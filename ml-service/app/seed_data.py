"""GoEmotions-like synthetic dataset for the mental-health text classifier.

The app keeps its existing 4-state prediction targets (normal/stress/anxiety/
depression), but the training corpus is generated from a richer 27-emotion label
space inspired by Google's GoEmotions taxonomy.

Why synthetic?
  - keeps repository size small,
  - allows deterministic, configurable dataset volume,
  - gives broad emotion coverage for demo/testing workflows.

Set `ML_SYNTHETIC_TRAINING_SAMPLES` to control training sample count.
Default is 12,000 for faster local training, while the generated source corpus
contains ~58k comments.
"""
from __future__ import annotations

import os
import random
from typing import Dict, List, Tuple

LABELS: List[str] = ["normal", "stress", "anxiety", "depression"]

EMOTION_LABELS: List[str] = [
    "admiration",
    "amusement",
    "anger",
    "annoyance",
    "approval",
    "caring",
    "confusion",
    "curiosity",
    "desire",
    "disappointment",
    "disapproval",
    "disgust",
    "embarrassment",
    "excitement",
    "fear",
    "gratitude",
    "grief",
    "joy",
    "love",
    "nervousness",
    "optimism",
    "pride",
    "realization",
    "relief",
    "remorse",
    "sadness",
    "surprise",
    "neutral",
]

EMOTION_TO_STATE: Dict[str, str] = {
    "admiration": "normal",
    "amusement": "normal",
    "anger": "stress",
    "annoyance": "stress",
    "approval": "normal",
    "caring": "normal",
    "confusion": "anxiety",
    "curiosity": "normal",
    "desire": "normal",
    "disappointment": "depression",
    "disapproval": "stress",
    "disgust": "stress",
    "embarrassment": "anxiety",
    "excitement": "normal",
    "fear": "anxiety",
    "gratitude": "normal",
    "grief": "depression",
    "joy": "normal",
    "love": "normal",
    "nervousness": "anxiety",
    "optimism": "normal",
    "pride": "normal",
    "realization": "normal",
    "relief": "normal",
    "remorse": "depression",
    "sadness": "depression",
    "surprise": "anxiety",
    "neutral": "normal",
}

_TOPICS = [
    "about work",
    "after talking with my family",
    "after therapy",
    "during class",
    "this morning",
    "tonight",
    "about tomorrow",
    "after that message",
    "about my health",
    "while planning the week",
]

_OPENERS = [
    "I feel",
    "Today I feel",
    "Honestly I feel",
    "Right now I feel",
    "Lately I feel",
    "At the moment I feel",
]

_INTENSIFIERS = [
    "a little",
    "pretty",
    "really",
    "very",
    "deeply",
]

_EMOTION_TERMS: Dict[str, List[str]] = {
    "admiration": ["admiration", "respect", "inspired", "impressed"],
    "amusement": ["amused", "entertained", "laughing", "cracking up"],
    "anger": ["angry", "furious", "mad", "irate"],
    "annoyance": ["annoyed", "irritated", "bothered", "fed up"],
    "approval": ["approve", "agree", "supportive", "on board"],
    "caring": ["caring", "concerned", "protective", "compassionate"],
    "confusion": ["confused", "unsure", "lost", "puzzled"],
    "curiosity": ["curious", "interested", "intrigued", "wondering"],
    "desire": ["wanting", "longing", "eager", "craving"],
    "disappointment": ["disappointed", "let down", "discouraged", "deflated"],
    "disapproval": ["disapprove", "against this", "critical", "not okay with it"],
    "disgust": ["disgusted", "grossed out", "repulsed", "sickened"],
    "embarrassment": ["embarrassed", "awkward", "ashamed", "self-conscious"],
    "excitement": ["excited", "thrilled", "hyped", "pumped"],
    "fear": ["scared", "afraid", "fearful", "terrified"],
    "gratitude": ["grateful", "thankful", "appreciative", "blessed"],
    "grief": ["grieving", "heartbroken", "mourning", "devastated"],
    "joy": ["joyful", "happy", "delighted", "cheerful"],
    "love": ["loved", "affectionate", "connected", "adored"],
    "nervousness": ["nervous", "anxious", "on edge", "jittery"],
    "optimism": ["optimistic", "hopeful", "positive", "encouraged"],
    "pride": ["proud", "accomplished", "confident", "satisfied"],
    "realization": ["realized", "aware now", "it clicked", "it makes sense now"],
    "relief": ["relieved", "at ease", "calmer", "unburdened"],
    "remorse": ["remorseful", "guilty", "regretful", "sorry"],
    "sadness": ["sad", "down", "empty", "lonely"],
    "surprise": ["surprised", "shocked", "caught off guard", "stunned"],
    "neutral": ["neutral", "okay", "fine", "steady"],
}

_EMOTION_TEMPLATES = [
    "{opener} {intensifier} {term} {topic}.",
    "{opener} {term} {topic}, and it is hard to ignore.",
    "{topic}, I have been feeling {intensifier} {term}.",
    "I have this {term} feeling {topic}.",
    "{opener} {intensifier} {term}; it has stayed with me {topic}.",
    "{topic} I ended up feeling {term} and wrote this down.",
]

SOURCE_DATASET_SIZE = 58320
DEFAULT_TRAINING_SAMPLE_SIZE = SOURCE_DATASET_SIZE
MIN_TRAINING_SAMPLES = 2000


def _build_emotion_samples(target_size: int = SOURCE_DATASET_SIZE) -> List[Tuple[str, str]]:
    label_count = len(EMOTION_LABELS)
    per_emotion = max(1, target_size // label_count)
    remainder = max(0, target_size - (per_emotion * label_count))
    samples: List[Tuple[str, str]] = []

    for emotion_idx, emotion in enumerate(EMOTION_LABELS):
        terms = _EMOTION_TERMS[emotion]
        # Spread the remainder so total generated rows equals target_size.
        sample_count = per_emotion + (1 if emotion_idx < remainder else 0)
        for i in range(sample_count):
            template = _EMOTION_TEMPLATES[(emotion_idx + i) % len(_EMOTION_TEMPLATES)]
            text = template.format(
                opener=_OPENERS[(emotion_idx + i * 3) % len(_OPENERS)],
                intensifier=_INTENSIFIERS[(emotion_idx * 2 + i) % len(_INTENSIFIERS)],
                term=terms[(emotion_idx + i) % len(terms)],
                topic=_TOPICS[(emotion_idx * 3 + i * 2) % len(_TOPICS)],
            )
            samples.append((text, emotion))

    rng = random.Random(42)
    rng.shuffle(samples)
    return samples[:target_size]


def _to_training_samples(emotion_samples: List[Tuple[str, str]], training_size: int) -> List[Tuple[str, str]]:
    bounded_size = min(max(training_size, MIN_TRAINING_SAMPLES), len(emotion_samples))
    selected = emotion_samples[:bounded_size]
    return [(text, EMOTION_TO_STATE[emotion]) for text, emotion in selected]


def _training_sample_size_from_env() -> int:
    raw = os.getenv("ML_SYNTHETIC_TRAINING_SAMPLES", str(DEFAULT_TRAINING_SAMPLE_SIZE)).strip()
    try:
        return int(raw)
    except ValueError:
        return DEFAULT_TRAINING_SAMPLE_SIZE


EMOTION_SAMPLES: List[Tuple[str, str]] = _build_emotion_samples()
SAMPLES: List[Tuple[str, str]] = _to_training_samples(EMOTION_SAMPLES, _training_sample_size_from_env())

DATASET_PROFILE: Dict[str, object] = {
    "name": "GoEmotions-style synthetic dataset",
    "source": "Inspired by Google GoEmotions label taxonomy",
    "source_dataset_size": len(EMOTION_SAMPLES),
    "training_samples_used": len(SAMPLES),
    "emotion_labels": EMOTION_LABELS,
    "state_labels": LABELS,
}
