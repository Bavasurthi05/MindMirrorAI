"""Lightweight, explainable analysis utilities.

These use transparent lexicon/heuristic baselines rather than large models so the
service runs quickly without downloading multi-gigabyte weights. The interfaces are
designed so a Transformer-backed implementation can be dropped in later.
"""
from __future__ import annotations

import re
from collections import Counter
from typing import Dict, List, Optional, Tuple

from . import ml_models
from .seed_data import EMOTION_TO_STATE

POSITIVE_WORDS = {
    "calm", "happy", "grateful", "hopeful", "relaxed", "energized", "content",
    "peaceful", "confident", "joy", "joyful", "proud", "rested", "clear", "focused",
    "motivated", "supported", "balanced", "better", "good", "great",
}

NEGATIVE_WORDS = {
    "anxious", "sad", "angry", "stressed", "tired", "exhausted", "overwhelmed",
    "worried", "lonely", "afraid", "frustrated", "hopeless", "restless", "tense",
    "drained", "upset", "bad", "worse", "fear", "panic", "numb",
}

EMOTION_LEXICON: Dict[str, set[str]] = {
    "admiration": {"admire", "impressed", "respect", "inspired"},
    "amusement": {"amused", "funny", "laugh", "hilarious"},
    "anger": {"angry", "furious", "mad", "irate"},
    "annoyance": {"annoyed", "irritated", "bothered", "fed up"},
    "approval": {"approve", "agree", "support", "valid"},
    "caring": {"care", "concerned", "compassion", "protective"},
    "confusion": {"confused", "unsure", "lost", "puzzled"},
    "curiosity": {"curious", "wondering", "interested", "intrigued"},
    "desire": {"want", "longing", "eager", "craving"},
    "disappointment": {"disappointed", "let down", "discouraged", "deflated"},
    "disapproval": {"disapprove", "critical", "against", "unacceptable"},
    "disgust": {"disgusted", "gross", "repulsed", "sickened"},
    "embarrassment": {"embarrassed", "awkward", "ashamed", "self-conscious"},
    "excitement": {"excited", "thrilled", "hyped", "pumped"},
    "fear": {"anxious", "worried", "afraid", "scared", "panic", "nervous", "tense"},
    "gratitude": {"grateful", "thankful", "appreciative", "blessed"},
    "grief": {"grieving", "heartbroken", "mourning", "devastated"},
    "joy": {"happy", "joy", "joyful", "delighted", "cheerful"},
    "love": {"love", "adore", "affection", "cared"},
    "nervousness": {"nervous", "jittery", "on edge", "uneasy"},
    "optimism": {"hopeful", "optimistic", "positive", "encouraged"},
    "pride": {"proud", "accomplished", "confident", "satisfied"},
    "realization": {"realized", "clicked", "understood", "aware"},
    "relief": {"relieved", "calmer", "at ease", "unburdened"},
    "remorse": {"sorry", "regret", "guilty", "remorseful"},
    "sadness": {"sad", "lonely", "hopeless", "down", "empty", "numb", "cry"},
    "surprise": {"surprised", "shocked", "stunned", "unexpected"},
    "neutral": {"neutral", "fine", "steady", "okay"},
    # Legacy auxiliary emotions retained for smoother fallback behavior.
    "calm": {"calm", "relaxed", "peaceful", "rested", "balanced", "settled"},
    "fatigue": {"tired", "exhausted", "drained", "restless", "overwhelmed"},
}

TRIGGER_LEXICON: Dict[str, set[str]] = {
    "Workload": {"work", "deadline", "workload", "meeting", "project", "boss", "overtime"},
    "Sleep": {"sleep", "insomnia", "tired", "rest", "awake", "nap"},
    "Social": {"friend", "family", "argument", "conflict", "lonely", "social", "relationship"},
    "Health": {"pain", "sick", "illness", "health", "headache", "appetite"},
    "Finance": {"money", "bills", "debt", "rent", "financial", "budget"},
    "Routine": {"routine", "schedule", "habit", "chores", "disorganized"},
}

_TOKEN_RE = re.compile(r"[a-zA-Z']+")


def _tokenize(text: str) -> List[str]:
    return [token.lower() for token in _TOKEN_RE.findall(text)]


def analyze_sentiment(text: str) -> Tuple[str, float, List[Tuple[str, float]]]:
    tokens = _tokenize(text)
    contributions: List[Tuple[str, float]] = []
    score = 0
    for token in tokens:
        if token in POSITIVE_WORDS:
            score += 1
            contributions.append((token, 1.0))
        elif token in NEGATIVE_WORDS:
            score -= 1
            contributions.append((token, -1.0))

    total = max(len(tokens), 1)
    normalized = max(-1.0, min(1.0, score / (total ** 0.5)))
    if normalized > 0.15:
        label = "positive"
    elif normalized < -0.15:
        label = "negative"
    else:
        label = "neutral"

    contributions.sort(key=lambda item: abs(item[1]), reverse=True)
    return label, round(normalized, 3), contributions[:8]


def classify_emotion(text: str) -> Tuple[str, Dict[str, float]]:
    tokens = _tokenize(text)
    counts: Counter[str] = Counter()
    for token in tokens:
        for emotion, lexicon in EMOTION_LEXICON.items():
            if token in lexicon:
                counts[emotion] += 1

    if not counts:
        return "neutral", {"neutral": 1.0}

    total = sum(counts.values())
    scores = {emotion: round(count / total, 3) for emotion, count in counts.items()}
    dominant = max(scores, key=scores.get)
    return dominant, scores


def detect_triggers(text: str) -> List[Tuple[str, List[str]]]:
    tokens = set(_tokenize(text))
    detected: List[Tuple[str, List[str]]] = []
    for category, lexicon in TRIGGER_LEXICON.items():
        matches = sorted(tokens & lexicon)
        if matches:
            detected.append((category, matches))
    return detected


def predict_mood(recent_scores: List[int]) -> Tuple[float, str, float, str]:
    if not recent_scores:
        return 50.0, "unknown", 0.2, "No recent mood data available; showing a neutral baseline."

    window = recent_scores[-7:]
    avg = sum(window) / len(window)

    if len(window) >= 2:
        first_half = window[: len(window) // 2]
        second_half = window[len(window) // 2:]
        delta = (sum(second_half) / len(second_half)) - (sum(first_half) / len(first_half))
    else:
        delta = 0.0

    predicted = max(0.0, min(100.0, avg + delta))
    if delta > 3:
        trend = "improving"
    elif delta < -3:
        trend = "declining"
    else:
        trend = "steady"

    confidence = round(min(0.95, 0.4 + 0.1 * len(window)), 2)
    rationale = (
        f"Projected from a {len(window)}-point average of {avg:.0f} with a "
        f"{'positive' if delta >= 0 else 'negative'} short-term trend."
    )
    return round(predicted, 1), trend, confidence, rationale


# --- Mental-health state prediction (model-backed with heuristic fallback) -------------

# Maps sentiment/emotion signals to a state label when no trained model is present.
_HEURISTIC_STATE_BY_EMOTION: Dict[str, str] = {
    **EMOTION_TO_STATE,
    "calm": "normal",
    "fatigue": "stress",
}


def _heuristic_state(text: str) -> Dict:
    """Transparent fallback used when the trained model is unavailable."""
    sentiment, sentiment_score, contributions = analyze_sentiment(text)
    emotion, emotion_scores = classify_emotion(text)

    label = _HEURISTIC_STATE_BY_EMOTION.get(emotion, "normal")
    if label == "normal" and sentiment == "negative":
        label = "stress"

    # Confidence from the strength of the dominant emotion / sentiment signal.
    dominant = max(emotion_scores.values()) if emotion_scores else 0.0
    confidence = round(min(0.9, 0.4 + 0.4 * dominant + 0.1 * abs(sentiment_score)), 3)

    signed = [(token, weight) for token, weight in contributions]
    return {
        "label": label,
        "confidence": confidence,
        "probabilities": {label: confidence},
        "contributions": signed,
        "backend": "heuristic",
    }


def _to_percentage_breakdown(contributions: List[Tuple[str, float]]) -> List[Dict]:
    """Normalize signed feature weights into a percentage reason breakdown."""
    total = sum(abs(weight) for _, weight in contributions)
    breakdown: List[Dict] = []
    for feature, weight in contributions:
        pct = round(100 * abs(weight) / total, 1) if total > 0 else 0.0
        breakdown.append({"feature": feature, "weight": round(float(weight), 4), "percentage": pct})
    return breakdown


def predict_mental_state(text: str) -> Dict:
    """Predict mental-health state with confidence and an explainable reason breakdown.

    Uses the trained TF-IDF + Random Forest model with SHAP explanations when available,
    otherwise falls back to the transparent heuristic baseline.
    """
    result = ml_models.predict(text)
    backend = "random_forest"
    if result is None:
        result = _heuristic_state(text)
        backend = result.get("backend", "heuristic")

    return {
        "label": result["label"],
        "confidence": result["confidence"],
        "probabilities": result.get("probabilities", {}),
        "reasons": _to_percentage_breakdown(result.get("contributions", [])),
        "backend": backend,
    }
