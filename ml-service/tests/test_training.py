import json

import pytest

from app.training import corpus, quality, registry


@pytest.fixture
def temp_registry(tmp_path, monkeypatch):
    """Point the registry at a throwaway directory so tests never touch real models."""
    monkeypatch.setattr(registry, "MODELS_DIR", tmp_path)
    monkeypatch.setattr(registry, "VERSIONS_DIR", tmp_path / "versions")
    monkeypatch.setattr(registry, "REGISTRY_PATH", tmp_path / "registry.json")
    monkeypatch.setattr(registry, "LEGACY_PIPELINE_PATH", tmp_path / "pipeline.joblib")
    monkeypatch.setattr(registry, "LEGACY_METRICS_PATH", tmp_path / "metrics.json")
    return tmp_path


def _register(version, passed=True, accuracy=0.9):
    metrics = {"models": {"random_forest": {"accuracy": accuracy, "f1_macro": accuracy}}}
    gate = {"passed": passed, "checks": [], "failed": [] if passed else ["enough_user_examples"]}
    return registry.register(version, metrics, {"user_examples": 500}, gate)


def _write_artifact(version, tmp_path):
    target = tmp_path / "versions" / version
    target.mkdir(parents=True, exist_ok=True)
    (target / "pipeline.joblib").write_bytes(b"stub")
    (target / "metrics.json").write_text(json.dumps({"version": version}), encoding="utf-8")


# --- Registry -----------------------------------------------------------------------


def test_empty_registry_has_no_active_version(temp_registry):
    assert registry.active_version() is None
    assert registry.list_versions() == []


def test_versions_increment(temp_registry):
    assert registry.next_version() == "v1"
    _register("v1")
    assert registry.next_version() == "v2"


def test_registering_does_not_deploy(temp_registry):
    _register("v1")
    # Training must never silently replace the live model.
    assert registry.active_version() is None
    assert registry.get_version("v1")["promoted"] is False


def test_promote_activates_and_marks_the_entry(temp_registry):
    _register("v1")
    _write_artifact("v1", temp_registry)

    registry.promote("v1")

    assert registry.active_version() == "v1"
    assert registry.get_version("v1")["promoted"] is True
    assert registry.get_version("v1")["promoted_at"]


def test_promote_requires_an_artifact_on_disk(temp_registry):
    _register("v1")
    with pytest.raises(ValueError, match="no artifact"):
        registry.promote("v1")


def test_promote_rejects_an_unknown_version(temp_registry):
    with pytest.raises(ValueError, match="Unknown version"):
        registry.promote("v99")


def test_rollback_switches_back_and_unmarks_the_previous(temp_registry):
    for version in ("v1", "v2"):
        _register(version)
        _write_artifact(version, temp_registry)
    registry.promote("v2")

    registry.rollback("v1")

    assert registry.active_version() == "v1"
    assert registry.get_version("v2")["promoted"] is False


def test_active_paths_fall_back_to_the_legacy_artifact(temp_registry):
    (temp_registry / "pipeline.joblib").write_bytes(b"legacy")
    # An existing deployment keeps working after upgrading, without retraining first.
    assert registry.active_pipeline_path() == temp_registry / "pipeline.joblib"


def test_the_active_version_cannot_be_deleted(temp_registry):
    _register("v1")
    _write_artifact("v1", temp_registry)
    registry.promote("v1")

    with pytest.raises(ValueError, match="Cannot delete the active version"):
        registry.delete_version("v1")


# --- Corpus -------------------------------------------------------------------------


def test_user_rows_are_validated(temp_registry):
    rows = [
        {"text": "a" * 30, "label": "anxiety"},
        {"text": "too short", "label": "anxiety"},          # under the length floor
        {"text": "a" * 30, "label": "burnout"},             # not a model label
    ]
    assert len(corpus.parse_user_examples(rows)) == 1


def test_duplicates_keep_the_highest_weight():
    examples = corpus.parse_user_examples([
        {"text": "the same reflection text here", "label": "stress", "weight": 0.6},
        {"text": "The Same Reflection Text Here  ", "label": "stress", "weight": 1.0},
    ])
    deduped = corpus.deduplicate(examples)
    assert len(deduped) == 1
    assert deduped[0].weight == 1.0


def test_one_user_cannot_dominate_the_corpus():
    rows = [
        {"text": f"a long enough reflection number {i}", "label": "stress", "user_hash": "heavy"}
        for i in range(50)
    ]
    capped = corpus.cap_per_user(corpus.parse_user_examples(rows), max_per_user=10)
    assert len(capped) == 10


def test_seed_always_anchors_the_corpus():
    texts, labels, weights, manifest = corpus.build([])
    assert manifest["seed_examples"] > 0
    assert manifest["user_examples"] == 0
    assert len(texts) == len(labels) == len(weights)


def test_user_examples_are_merged_with_the_seed():
    rows = [
        {"text": f"a genuinely distinct user reflection {i}", "label": "anxiety", "user_hash": "u1"}
        for i in range(5)
    ]
    _, _, _, manifest = corpus.build(rows)
    assert manifest["user_examples"] == 5
    assert manifest["user_label_counts"]["anxiety"] == 5


# --- Quality gate -------------------------------------------------------------------


def _metrics(accuracy=0.9, f1=0.9, per_class=None):
    return {
        "models": {"random_forest": {"accuracy": accuracy, "f1_macro": f1}},
        "per_class_f1": per_class or {"normal": 0.9, "stress": 0.9, "anxiety": 0.9, "depression": 0.9},
    }


def _manifest(user_examples=500, per_class=50):
    return {
        "user_examples": user_examples,
        "user_label_counts": {label: per_class for label in LABELS},
    }


LABELS = ["normal", "stress", "anxiety", "depression"]


def test_gate_passes_a_healthy_candidate():
    result = quality.evaluate(_metrics(), _metrics(0.89, 0.89), _manifest(), LABELS, 0.02)
    assert result["passed"] is True
    assert result["failed"] == []


def test_gate_blocks_an_accuracy_regression():
    result = quality.evaluate(_metrics(0.80), _metrics(0.90), _manifest(), LABELS, 0.0)
    assert result["passed"] is False
    assert "accuracy_not_regressed" in result["failed"]


def test_gate_tolerates_noise_level_movement():
    result = quality.evaluate(_metrics(0.895), _metrics(0.90), _manifest(), LABELS, 0.0)
    assert "accuracy_not_regressed" not in result["failed"]


def test_gate_blocks_a_collapsed_class():
    result = quality.evaluate(
        _metrics(per_class={"normal": 0.9, "stress": 0.9, "anxiety": 0.9, "depression": 0.1}),
        _metrics(0.89, 0.89), _manifest(), LABELS, 0.0,
    )
    assert "per_class_f1_floor" in result["failed"]


def test_gate_blocks_thin_data():
    result = quality.evaluate(_metrics(), _metrics(), _manifest(user_examples=10, per_class=2), LABELS, 0.0)
    assert "enough_user_examples" in result["failed"]
    assert "user_examples_per_class" in result["failed"]


def test_gate_blocks_a_large_distribution_shift():
    result = quality.evaluate(_metrics(), _metrics(0.89, 0.89), _manifest(), LABELS, 0.40)
    assert "distribution_stable" in result["failed"]


def test_gate_skips_comparisons_when_nothing_is_deployed():
    result = quality.evaluate(_metrics(), None, _manifest(), LABELS, None)
    assert result["passed"] is True


def test_distribution_shift_detects_a_collapse_to_one_class():
    active = ["normal", "stress", "anxiety", "depression"]
    collapsed = ["stress", "stress", "stress", "stress"]
    assert quality.distribution_shift(collapsed, active) == pytest.approx(0.75)


def test_distribution_shift_is_zero_for_identical_predictions():
    predictions = ["normal", "stress", "anxiety"]
    assert quality.distribution_shift(predictions, predictions) == 0.0
