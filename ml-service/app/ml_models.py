"""Model backend: loads the trained TF-IDF + Random Forest pipeline and produces
predictions with SHAP-based feature explanations.

Everything here degrades gracefully:
  * If the trained artifact or the ML stack is missing, `is_available()` returns False
    and callers fall back to the transparent heuristic baseline in `analysis.py`.
  * SHAP is optional; when unavailable we fall back to Random Forest feature
    importances restricted to the tokens present in the input.
"""
from __future__ import annotations

import json
import threading
from pathlib import Path
from typing import Dict, List, Optional, Tuple

MODELS_DIR = Path(__file__).resolve().parent.parent / "models"
PIPELINE_PATH = MODELS_DIR / "pipeline.joblib"
METRICS_PATH = MODELS_DIR / "metrics.json"

_lock = threading.Lock()
_pipeline = None
_loaded_path: Optional[Path] = None
_load_attempted = False


def _resolve_pipeline_path() -> Optional[Path]:
    """Artifact the registry says is live, falling back to the pre-registry file."""
    from .training import registry

    return registry.active_pipeline_path()


def _load_pipeline():
    """Load (or reload) the active pipeline.

    Reloads automatically when the registry's active version changes, so promoting or
    rolling back a model takes effect without restarting the service.
    """
    global _pipeline, _loaded_path, _load_attempted
    with _lock:
        path = _resolve_pipeline_path()
        if _load_attempted and path == _loaded_path:
            return _pipeline

        _load_attempted = True
        _loaded_path = path
        _pipeline = None
        if path is None or not path.exists():
            return None
        try:
            import joblib  # noqa: WPS433 (optional dependency)

            _pipeline = joblib.load(path)
        except Exception:  # pragma: no cover - defensive: any load failure -> heuristic
            _pipeline = None
        return _pipeline


def reload() -> bool:
    """Force a reload; called after a promotion or rollback."""
    global _load_attempted, _loaded_path
    with _lock:
        _load_attempted = False
        _loaded_path = None
    return _load_pipeline() is not None


def is_available() -> bool:
    return _load_pipeline() is not None


def get_metrics() -> Optional[dict]:
    from .training import registry

    path = registry.active_metrics_path()
    try:
        if path is not None and path.exists():
            return json.loads(path.read_text(encoding="utf-8"))
    except Exception:  # pragma: no cover
        return None
    return None


HEURISTIC_VERSION = "heuristic-v1"


def get_model_version() -> str:
    """Stable identifier for whatever produced a prediction.

    Every stored analysis records this, so a later corpus/model change can be
    traced back to the predictions it produced. Prefers the version written by
    the trainer; falls back to the artifact's modification time.
    """
    if not is_available():
        return HEURISTIC_VERSION

    from .training import registry

    active = registry.active_version()
    if active:
        return active

    metrics = get_metrics() or {}
    version = metrics.get("version")
    if isinstance(version, str) and version:
        return version

    try:
        stamp = int(PIPELINE_PATH.stat().st_mtime)
        return f"random_forest-{stamp}"
    except OSError:  # pragma: no cover - defensive
        return "random_forest-unknown"


def _feature_contributions(pipeline, text: str, predicted_index: int,
                           top_k: int = 8) -> List[Tuple[str, float]]:
    """Return (feature, signed_weight) pairs explaining the prediction.

    Prefers SHAP; falls back to RF feature importances over present tokens.
    """
    vectorizer = pipeline.named_steps["tfidf"]
    classifier = pipeline.named_steps["clf"]
    feature_names = vectorizer.get_feature_names_out()
    vector = vectorizer.transform([text])
    present = vector.nonzero()[1]
    if len(present) == 0:
        return []

    # Attempt SHAP first.
    try:
        import shap  # noqa: WPS433 (optional dependency)

        dense = vector.toarray()
        explainer = shap.TreeExplainer(classifier)
        shap_values = explainer.shap_values(dense)
        # shap_values shape handling across versions: list per class or 3D array.
        if isinstance(shap_values, list):
            class_values = shap_values[predicted_index][0]
        else:
            arr = shap_values
            class_values = arr[0, :, predicted_index] if arr.ndim == 3 else arr[0]
        contributions = [
            (feature_names[idx], float(class_values[idx]))
            for idx in present
        ]
    except Exception:
        importances = classifier.feature_importances_
        contributions = [
            (feature_names[idx], float(importances[idx]))
            for idx in present
        ]

    contributions.sort(key=lambda item: abs(item[1]), reverse=True)
    return contributions[:top_k]


def predict(text: str) -> Optional[Dict]:
    """Predict mental-health state with confidence and feature explanations.

    Returns None when the model backend is unavailable.
    """
    pipeline = _load_pipeline()
    if pipeline is None:
        return None

    try:
        probabilities = pipeline.predict_proba([text])[0]
        classes = list(pipeline.named_steps["clf"].classes_)
        predicted_index = int(probabilities.argmax())
        label = classes[predicted_index]
        confidence = round(float(probabilities[predicted_index]), 3)
        proba_map = {cls: round(float(p), 3) for cls, p in zip(classes, probabilities)}
        contributions = _feature_contributions(pipeline, text, predicted_index)
    except Exception:  # pragma: no cover - defensive fallback
        return None

    return {
        "label": label,
        "confidence": confidence,
        "probabilities": proba_map,
        "contributions": contributions,
    }
