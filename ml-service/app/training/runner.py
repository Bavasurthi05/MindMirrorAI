"""Runs a retraining job: build corpus -> train -> evaluate -> gate -> register.

Registering is never the same as deploying. A finished run leaves a new version on disk
with its metrics and gate verdict; making it live is a separate, explicit promotion.
"""
from __future__ import annotations

import logging
import threading
import uuid
from datetime import datetime, timezone
from typing import Dict, List, Optional

from ..seed_data import DATASET_PROFILE, EMOTION_LABELS, LABELS
from . import corpus as corpus_module
from . import quality, registry

logger = logging.getLogger(__name__)

_jobs: Dict[str, Dict] = {}
_jobs_lock = threading.Lock()

MAX_TRACKED_JOBS = 50


def _set_job(job_id: str, **fields) -> None:
    with _jobs_lock:
        job = _jobs.setdefault(job_id, {"job_id": job_id})
        job.update(fields)
        # Keep the map bounded; the service is long-lived and jobs are small but unbounded.
        if len(_jobs) > MAX_TRACKED_JOBS:
            oldest = sorted(_jobs.values(), key=lambda item: item.get("started_at", ""))[0]
            _jobs.pop(oldest["job_id"], None)


def get_job(job_id: str) -> Optional[Dict]:
    with _jobs_lock:
        job = _jobs.get(job_id)
        return dict(job) if job else None


def list_jobs() -> List[Dict]:
    with _jobs_lock:
        return sorted(_jobs.values(), key=lambda item: item.get("started_at", ""), reverse=True)


def start(user_rows: Optional[List[Dict]] = None, max_per_user: int = corpus_module.DEFAULT_MAX_PER_USER) -> str:
    """Kick off a training run on a background thread; returns the job id to poll."""
    job_id = uuid.uuid4().hex[:12]
    _set_job(
        job_id,
        status="RUNNING",
        started_at=datetime.now(timezone.utc).isoformat(),
        message="Building corpus",
    )

    thread = threading.Thread(
        target=_run_safely, args=(job_id, user_rows or [], max_per_user), daemon=True
    )
    thread.start()
    return job_id


def _run_safely(job_id: str, user_rows: List[Dict], max_per_user: int) -> None:
    try:
        result = run(job_id, user_rows, max_per_user)
        _set_job(
            job_id,
            status="COMPLETED",
            finished_at=datetime.now(timezone.utc).isoformat(),
            message="Training complete",
            **result,
        )
    except Exception as exc:  # pragma: no cover - defensive: a job must never kill the service
        logger.exception("Training job %s failed", job_id)
        _set_job(
            job_id,
            status="FAILED",
            finished_at=datetime.now(timezone.utc).isoformat(),
            message=str(exc),
        )


def run(job_id: str, user_rows: List[Dict], max_per_user: int) -> Dict:
    from sklearn.ensemble import RandomForestClassifier
    from sklearn.feature_extraction.text import TfidfVectorizer
    from sklearn.linear_model import LogisticRegression
    from sklearn.metrics import accuracy_score, f1_score
    from sklearn.pipeline import Pipeline

    from ..preprocessing import preprocess

    texts, labels, weights, manifest = corpus_module.build(user_rows, max_per_user)
    _set_job(job_id, message=f"Training on {len(texts)} examples", corpus=manifest)

    holdout_texts, holdout_labels = corpus_module.holdout_from_seed()

    def vectorizer() -> TfidfVectorizer:
        return TfidfVectorizer(preprocessor=preprocess, ngram_range=(1, 2), min_df=1, sublinear_tf=True)

    def forest() -> RandomForestClassifier:
        if len(texts) >= 40000:
            return RandomForestClassifier(
                n_estimators=180, max_depth=50, min_samples_leaf=2, max_features="sqrt",
                class_weight="balanced_subsample", random_state=42, n_jobs=-1,
            )
        return RandomForestClassifier(
            n_estimators=300, class_weight="balanced", random_state=42, n_jobs=-1
        )

    rf_pipeline = Pipeline([("tfidf", vectorizer()), ("clf", forest())])
    baseline_pipeline = Pipeline([
        ("tfidf", vectorizer()),
        ("clf", LogisticRegression(max_iter=1000, class_weight="balanced")),
    ])

    # Sample weights let user corrections carry more influence than agreements.
    rf_pipeline.fit(texts, labels, clf__sample_weight=weights)
    baseline_pipeline.fit(texts, labels)

    _set_job(job_id, message="Evaluating against the frozen holdout")
    rf_predictions = list(rf_pipeline.predict(holdout_texts))
    baseline_predictions = list(baseline_pipeline.predict(holdout_texts))

    per_class = f1_score(holdout_labels, rf_predictions, average=None, labels=LABELS, zero_division=0)
    per_class_f1 = {label: round(float(score), 4) for label, score in zip(LABELS, per_class)}

    version = registry.next_version()
    metrics = {
        "version": version,
        "trained_at": datetime.now(timezone.utc).isoformat(),
        "labels": LABELS,
        "emotion_labels": EMOTION_LABELS,
        "train_size": len(texts),
        "test_size": len(holdout_texts),
        "dataset_profile": DATASET_PROFILE,
        "per_class_f1": per_class_f1,
        "models": {
            "random_forest": {
                "name": "TF-IDF + Random Forest",
                "accuracy": round(float(accuracy_score(holdout_labels, rf_predictions)), 4),
                "f1_macro": round(float(f1_score(holdout_labels, rf_predictions, average="macro", zero_division=0)), 4),
                "deployed": False,
            },
            "logistic_regression": {
                "name": "TF-IDF + Logistic Regression (baseline)",
                "accuracy": round(float(accuracy_score(holdout_labels, baseline_predictions)), 4),
                "f1_macro": round(float(f1_score(holdout_labels, baseline_predictions, average="macro", zero_division=0)), 4),
                "deployed": False,
            },
        },
    }

    # Compare against whatever is live today, on the same holdout.
    active_metrics = _active_metrics()
    shift = None
    active_pipeline = _load_active_pipeline()
    if active_pipeline is not None:
        try:
            active_predictions = list(active_pipeline.predict(holdout_texts))
            shift = quality.distribution_shift(rf_predictions, active_predictions)
        except Exception:  # pragma: no cover - a broken active model must not block training
            logger.warning("Could not score the active model for distribution comparison")

    _set_job(job_id, message="Applying the promotion gate")
    gate = quality.evaluate(metrics, active_metrics, manifest, LABELS, shift)

    registry.write_artifacts(version, rf_pipeline, metrics, manifest)
    entry = registry.register(version, metrics, manifest, gate)

    logger.info(
        "Training job %s produced %s (gate %s)",
        job_id, version, "passed" if gate["passed"] else "failed: " + ", ".join(gate["failed"]),
    )
    return {"version": version, "metrics": metrics, "gate": gate, "entry": entry}


def _active_metrics() -> Optional[Dict]:
    import json

    path = registry.active_metrics_path()
    if path is None or not path.exists():
        return None
    try:
        return json.loads(path.read_text(encoding="utf-8"))
    except (OSError, json.JSONDecodeError):  # pragma: no cover
        return None


def _load_active_pipeline():
    path = registry.active_pipeline_path()
    if path is None or not path.exists():
        return None
    try:
        import joblib

        return joblib.load(path)
    except Exception:  # pragma: no cover
        return None
