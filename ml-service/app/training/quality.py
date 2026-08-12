"""Promotion gate.

A newly trained model only becomes the live one if it clears every check below, measured
on the frozen seed holdout so successive versions stay comparable. Without this, a
retrain on a thin or skewed batch of user corrections could quietly replace a working
model with a worse one.
"""
from __future__ import annotations

from typing import Dict, List, Optional

# Small regressions are tolerated (noise), meaningful ones are not.
MAX_ACCURACY_REGRESSION = 0.01
MAX_F1_REGRESSION = 0.01
MIN_PER_CLASS_F1 = 0.50
MIN_USER_EXAMPLES = 200
MIN_USER_EXAMPLES_PER_CLASS = 30
MAX_DISTRIBUTION_SHIFT = 0.15


class GateCheck:
    __slots__ = ("name", "passed", "detail")

    def __init__(self, name: str, passed: bool, detail: str):
        self.name = name
        self.passed = passed
        self.detail = detail

    def to_dict(self) -> Dict:
        return {"name": self.name, "passed": self.passed, "detail": self.detail}


def evaluate(
    candidate_metrics: Dict,
    active_metrics: Optional[Dict],
    corpus_manifest: Dict,
    labels: List[str],
    distribution_shift: Optional[float] = None,
) -> Dict:
    """Run every gate condition and return a structured verdict."""
    checks: List[GateCheck] = []

    candidate = candidate_metrics.get("models", {}).get("random_forest", {})
    candidate_accuracy = float(candidate.get("accuracy", 0.0))
    candidate_f1 = float(candidate.get("f1_macro", 0.0))

    if active_metrics:
        active = active_metrics.get("models", {}).get("random_forest", {})
        active_accuracy = float(active.get("accuracy", 0.0))
        active_f1 = float(active.get("f1_macro", 0.0))

        checks.append(GateCheck(
            "accuracy_not_regressed",
            candidate_accuracy >= active_accuracy - MAX_ACCURACY_REGRESSION,
            f"candidate {candidate_accuracy:.4f} vs active {active_accuracy:.4f} "
            f"(tolerance {MAX_ACCURACY_REGRESSION})",
        ))
        checks.append(GateCheck(
            "f1_not_regressed",
            candidate_f1 >= active_f1 - MAX_F1_REGRESSION,
            f"candidate {candidate_f1:.4f} vs active {active_f1:.4f} "
            f"(tolerance {MAX_F1_REGRESSION})",
        ))
    else:
        # Nothing deployed yet: there is no baseline to regress against.
        checks.append(GateCheck("accuracy_not_regressed", True, "no active model to compare against"))
        checks.append(GateCheck("f1_not_regressed", True, "no active model to compare against"))

    per_class = candidate_metrics.get("per_class_f1", {})
    weak = {label: score for label, score in per_class.items() if float(score) < MIN_PER_CLASS_F1}
    checks.append(GateCheck(
        "per_class_f1_floor",
        not weak,
        "all classes above floor" if not weak
        else f"below {MIN_PER_CLASS_F1}: " + ", ".join(f"{k}={v:.2f}" for k, v in weak.items()),
    ))

    user_examples = int(corpus_manifest.get("user_examples", 0))
    checks.append(GateCheck(
        "enough_user_examples",
        user_examples >= MIN_USER_EXAMPLES,
        f"{user_examples}/{MIN_USER_EXAMPLES} user examples",
    ))

    user_label_counts = corpus_manifest.get("user_label_counts", {})
    thin = [
        label for label in labels
        if int(user_label_counts.get(label, 0)) < MIN_USER_EXAMPLES_PER_CLASS
    ]
    checks.append(GateCheck(
        "user_examples_per_class",
        not thin,
        "all classes represented" if not thin
        else f"under {MIN_USER_EXAMPLES_PER_CLASS} for: " + ", ".join(thin),
    ))

    if distribution_shift is None:
        checks.append(GateCheck("distribution_stable", True, "no active model to compare against"))
    else:
        checks.append(GateCheck(
            "distribution_stable",
            distribution_shift <= MAX_DISTRIBUTION_SHIFT,
            f"max class shift {distribution_shift:.3f} (limit {MAX_DISTRIBUTION_SHIFT})",
        ))

    passed = all(check.passed for check in checks)
    return {
        "passed": passed,
        "checks": [check.to_dict() for check in checks],
        "failed": [check.name for check in checks if not check.passed],
    }


def distribution_shift(candidate_predictions: List[str], active_predictions: List[str]) -> float:
    """Largest per-class change in predicted share between two models.

    Catches a model that has collapsed toward one class even while its headline accuracy
    still looks acceptable.
    """
    if not candidate_predictions or not active_predictions:
        return 0.0

    labels = set(candidate_predictions) | set(active_predictions)
    shift = 0.0
    for label in labels:
        candidate_share = candidate_predictions.count(label) / len(candidate_predictions)
        active_share = active_predictions.count(label) / len(active_predictions)
        shift = max(shift, abs(candidate_share - active_share))
    return shift
