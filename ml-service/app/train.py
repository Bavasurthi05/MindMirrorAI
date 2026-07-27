"""Train and persist the TF-IDF + Random Forest mental-health text classifier.

Implements the design document's Machine Learning module:
  Text Cleaning -> Tokenization -> TF-IDF -> Random Forest -> Accuracy Comparison

Also trains a Logistic Regression baseline and writes a side-by-side accuracy report
so the Admin "Model Accuracy" view can surface a real comparison.

Usage:
    python -m app.train        # from the ml-service/ directory
Artifacts written to ml-service/models/:
    - pipeline.joblib   (TF-IDF + RandomForest, the deployed model)
    - metrics.json      (accuracy comparison + metadata)
"""
from __future__ import annotations

import json
from pathlib import Path

from sklearn.ensemble import RandomForestClassifier
from sklearn.feature_extraction.text import TfidfVectorizer
from sklearn.linear_model import LogisticRegression
from sklearn.metrics import accuracy_score, f1_score
from sklearn.model_selection import train_test_split
from sklearn.pipeline import Pipeline

from .preprocessing import preprocess
from .seed_data import LABELS, SAMPLES

MODELS_DIR = Path(__file__).resolve().parent.parent / "models"
PIPELINE_PATH = MODELS_DIR / "pipeline.joblib"
METRICS_PATH = MODELS_DIR / "metrics.json"


def _build_vectorizer() -> TfidfVectorizer:
    return TfidfVectorizer(
        preprocessor=preprocess,
        ngram_range=(1, 2),
        min_df=1,
        sublinear_tf=True,
    )


def train() -> dict:
    import joblib

    MODELS_DIR.mkdir(parents=True, exist_ok=True)

    texts = [text for text, _ in SAMPLES]
    labels = [label for _, label in SAMPLES]

    x_train, x_test, y_train, y_test = train_test_split(
        texts, labels, test_size=0.25, random_state=42, stratify=labels
    )

    rf_pipeline = Pipeline([
        ("tfidf", _build_vectorizer()),
        ("clf", RandomForestClassifier(n_estimators=300, random_state=42, class_weight="balanced")),
    ])
    baseline_pipeline = Pipeline([
        ("tfidf", _build_vectorizer()),
        ("clf", LogisticRegression(max_iter=1000, class_weight="balanced")),
    ])

    rf_pipeline.fit(x_train, y_train)
    baseline_pipeline.fit(x_train, y_train)

    rf_pred = rf_pipeline.predict(x_test)
    base_pred = baseline_pipeline.predict(x_test)

    metrics = {
        "labels": LABELS,
        "train_size": len(x_train),
        "test_size": len(x_test),
        "models": {
            "random_forest": {
                "name": "TF-IDF + Random Forest",
                "accuracy": round(float(accuracy_score(y_test, rf_pred)), 4),
                "f1_macro": round(float(f1_score(y_test, rf_pred, average="macro")), 4),
                "deployed": True,
            },
            "logistic_regression": {
                "name": "TF-IDF + Logistic Regression (baseline)",
                "accuracy": round(float(accuracy_score(y_test, base_pred)), 4),
                "f1_macro": round(float(f1_score(y_test, base_pred, average="macro")), 4),
                "deployed": False,
            },
        },
    }

    # Refit the deployed model on the full dataset for best downstream predictions.
    rf_pipeline.fit(texts, labels)
    joblib.dump(rf_pipeline, PIPELINE_PATH)
    METRICS_PATH.write_text(json.dumps(metrics, indent=2), encoding="utf-8")

    return metrics


if __name__ == "__main__":
    result = train()
    print(json.dumps(result, indent=2))
    print(f"\nSaved model to {PIPELINE_PATH}")
    print(f"Saved metrics to {METRICS_PATH}")
