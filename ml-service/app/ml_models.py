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
_load_attempted = False


def _load_pipeline():
    global _pipeline, _load_attempted
    with _lock:
        if _load_attempted:
            return _pipeline
        _load_attempted = True
        try:
            import joblib  # noqa: WPS433 (optional dependency)

            if PIPELINE_PATH.exists():
                _pipeline = joblib.load(PIPELINE_PATH)
        except Exception:  # pragma: no cover - defensive: any load failure -> heuristic
            _pipeline = None
        return _pipeline


def is_available() -> bool:
    return _load_pipeline() is not None


def get_metrics() -> Optional[dict]:
    try:
        if METRICS_PATH.exists():
            return json.loads(METRICS_PATH.read_text(encoding="utf-8"))
    except Exception:  # pragma: no cover
        return None
    return None


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
