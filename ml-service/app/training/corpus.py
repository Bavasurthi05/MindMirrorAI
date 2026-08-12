"""Builds a training corpus from the synthetic seed plus user-contributed examples.

Two rules shape everything here:

* **The seed stays.** It is the anchor that keeps a retrained model from drifting off
  whatever the most recent batch of user corrections happened to emphasise.
* **User corrections outweigh agreements.** A user saying "no, this was anxiety" is a
  directly supervised label; a thumbs-up merely confirms what the model already said.
"""
from __future__ import annotations

import hashlib
import re
from collections import Counter
from typing import Dict, Iterable, List, Optional, Tuple

from ..seed_data import LABELS, SAMPLES

# Weight applied when the same text appears from different sources.
SOURCE_WEIGHTS: Dict[str, float] = {
    "user_correction": 1.0,
    "user_agreement": 0.6,
    "questionnaire": 0.4,
    "seed": 1.0,
}

MIN_TEXT_LENGTH = 20
DEFAULT_MAX_PER_USER = 200


class CorpusExample:
    __slots__ = ("text", "label", "source", "weight", "user_hash")

    def __init__(self, text: str, label: str, source: str, weight: float, user_hash: str = ""):
        self.text = text
        self.label = label
        self.source = source
        self.weight = weight
        self.user_hash = user_hash


def _normalize(text: str) -> str:
    return re.sub(r"\s+", " ", text.strip().lower())


def _fingerprint(text: str) -> str:
    return hashlib.sha256(_normalize(text).encode("utf-8")).hexdigest()


def parse_user_examples(rows: Iterable[Dict]) -> List[CorpusExample]:
    """Validate and normalize exported rows, dropping anything untrainable."""
    examples: List[CorpusExample] = []
    for row in rows:
        text = (row.get("text") or "").strip()
        label = (row.get("label") or "").strip().lower()
        if len(text) < MIN_TEXT_LENGTH or label not in LABELS:
            continue
        source = row.get("source") or "user_correction"
        weight = float(row.get("weight") or SOURCE_WEIGHTS.get(source, 0.5))
        examples.append(CorpusExample(text, label, source, weight, row.get("user_hash", "")))
    return examples


def cap_per_user(examples: List[CorpusExample], max_per_user: int = DEFAULT_MAX_PER_USER) -> List[CorpusExample]:
    """Stop one prolific user from dominating the corpus (and the resulting model)."""
    seen: Counter[str] = Counter()
    kept: List[CorpusExample] = []
    for example in examples:
        key = example.user_hash or "anonymous"
        if seen[key] >= max_per_user:
            continue
        seen[key] += 1
        kept.append(example)
    return kept


def deduplicate(examples: List[CorpusExample]) -> List[CorpusExample]:
    """Keep the highest-weight instance of each distinct text."""
    best: Dict[str, CorpusExample] = {}
    for example in examples:
        key = _fingerprint(example.text)
        current = best.get(key)
        if current is None or example.weight > current.weight:
            best[key] = example
    return list(best.values())


def seed_examples() -> List[CorpusExample]:
    return [CorpusExample(text, label, "seed", SOURCE_WEIGHTS["seed"]) for text, label in SAMPLES]


def build(
    user_rows: Optional[Iterable[Dict]] = None,
    max_per_user: int = DEFAULT_MAX_PER_USER,
) -> Tuple[List[str], List[str], List[float], Dict]:
    """Merge seed + user data into (texts, labels, weights, manifest)."""
    users = parse_user_examples(user_rows or [])
    users = cap_per_user(users, max_per_user)
    users = deduplicate(users)

    seed = seed_examples()
    # Seed texts that a user example duplicates are dropped in favour of the real one.
    user_keys = {_fingerprint(example.text) for example in users}
    seed = [example for example in seed if _fingerprint(example.text) not in user_keys]

    combined = seed + users
    label_counts = Counter(example.label for example in combined)
    user_label_counts = Counter(example.label for example in users)

    manifest = {
        "total_examples": len(combined),
        "seed_examples": len(seed),
        "user_examples": len(users),
        "label_counts": dict(label_counts),
        "user_label_counts": dict(user_label_counts),
        "user_sources": dict(Counter(example.source for example in users)),
        "max_per_user": max_per_user,
        "min_text_length": MIN_TEXT_LENGTH,
    }

    return (
        [example.text for example in combined],
        [example.label for example in combined],
        [example.weight for example in combined],
        manifest,
    )


def holdout_from_seed(test_size: float = 0.25, random_state: int = 42) -> Tuple[List[str], List[str]]:
    """A frozen benchmark drawn only from the seed corpus.

    Deliberately excludes user data: the benchmark must not move underneath us between
    retrains, or successive versions become incomparable and the promotion gate is
    measuring two different things.
    """
    from sklearn.model_selection import train_test_split

    texts = [text for text, _ in SAMPLES]
    labels = [label for _, label in SAMPLES]
    _, holdout_texts, _, holdout_labels = train_test_split(
        texts, labels, test_size=test_size, random_state=random_state, stratify=labels
    )
    return holdout_texts, holdout_labels
