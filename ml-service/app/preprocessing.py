"""Text preprocessing utilities shared by the heuristic and model backends.

Kept dependency-light (pure standard library) so the service starts even when the
optional ML stack (scikit-learn / transformers) is not installed. The functions here
implement the design document's "Text Cleaning" and "Tokenization" steps.
"""
from __future__ import annotations

import re
from typing import List

# Minimal English stopword list — enough to reduce noise for TF-IDF without pulling
# in a heavyweight NLP dependency.
STOPWORDS = {
    "a", "an", "and", "are", "as", "at", "be", "been", "but", "by", "for", "from",
    "had", "has", "have", "he", "her", "his", "i", "in", "is", "it", "its", "me",
    "my", "of", "on", "or", "our", "so", "that", "the", "their", "them", "they",
    "this", "to", "was", "we", "were", "with", "you", "your", "am", "im", "ive",
    "just", "really", "very", "too", "also", "there", "here", "then", "than",
}

_URL_RE = re.compile(r"https?://\S+|www\.\S+")
_MENTION_RE = re.compile(r"[@#]\w+")
_NON_ALPHA_RE = re.compile(r"[^a-z']+")
_TOKEN_RE = re.compile(r"[a-zA-Z']+")


def clean_text(text: str) -> str:
    """Lowercase, strip URLs / @mentions / #hashtags, and collapse non-alpha noise."""
    lowered = text.lower()
    lowered = _URL_RE.sub(" ", lowered)
    lowered = _MENTION_RE.sub(" ", lowered)
    lowered = _NON_ALPHA_RE.sub(" ", lowered)
    return re.sub(r"\s+", " ", lowered).strip()


def tokenize(text: str, remove_stopwords: bool = False) -> List[str]:
    """Split raw text into lowercase word tokens."""
    tokens = [token.lower() for token in _TOKEN_RE.findall(text)]
    if remove_stopwords:
        tokens = [token for token in tokens if token not in STOPWORDS]
    return tokens


def preprocess(text: str) -> str:
    """Full cleaning + stopword removal, returned as a normalized string for TF-IDF."""
    cleaned = clean_text(text)
    tokens = [token for token in cleaned.split() if token not in STOPWORDS and len(token) > 1]
    return " ".join(tokens)
