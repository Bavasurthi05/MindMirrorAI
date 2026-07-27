"""Small, transparent seed dataset for the mental-health text classifier.

This is intentionally tiny and illustrative so the model trains in seconds and the
repository stays lightweight. Replace / extend `SAMPLES` with a real labelled corpus
(e.g. a curated export from `journal_entries`) for production-grade accuracy.

Labels follow the design document's prediction targets.
"""
from __future__ import annotations

from typing import List, Tuple

LABELS: List[str] = ["normal", "stress", "anxiety", "depression"]

# (text, label)
SAMPLES: List[Tuple[str, str]] = [
    # normal / balanced
    ("Had a productive day at work and enjoyed dinner with friends.", "normal"),
    ("Feeling calm and grateful, went for a relaxing walk this morning.", "normal"),
    ("Slept well and woke up energized and focused for the day.", "normal"),
    ("A balanced day, nothing stressful, felt content and settled.", "normal"),
    ("Spent quality time with family and felt happy and supported.", "normal"),
    ("Good workout, healthy meals, and a peaceful evening reading.", "normal"),
    ("I feel motivated and clear about my goals this week.", "normal"),
    ("Everything is going fine, steady mood and good energy.", "normal"),

    # stress
    ("So much work and tight deadlines, I feel completely overwhelmed.", "stress"),
    ("The project pressure is intense and I have no time to rest.", "stress"),
    ("Back to back meetings all day left me drained and tense.", "stress"),
    ("Too many tasks piling up, I am stressed and can't keep up.", "stress"),
    ("My boss keeps adding work and the deadline stress is crushing.", "stress"),
    ("Overloaded with responsibilities, feeling burnt out and exhausted.", "stress"),
    ("Constant pressure at work is making me irritable and restless.", "stress"),
    ("I am juggling too many things and feel stretched thin and stressed.", "stress"),

    # anxiety
    ("I feel anxious and worried, my heart races and I can't relax.", "anxiety"),
    ("Constant nervousness and fear about what might go wrong.", "anxiety"),
    ("I keep overthinking everything and feel scared and on edge.", "anxiety"),
    ("Panic keeps rising and I feel afraid without a clear reason.", "anxiety"),
    ("So worried about the future, my mind won't stop racing.", "anxiety"),
    ("Tense and jittery all day, dreading things that may happen.", "anxiety"),
    ("A wave of fear and worry hit me and my chest felt tight.", "anxiety"),
    ("I feel restless and nervous, anticipating the worst outcomes.", "anxiety"),

    # depression
    ("I feel hopeless and empty, nothing brings me joy anymore.", "depression"),
    ("So sad and lonely, I have no energy or motivation to do anything.", "depression"),
    ("Everything feels heavy and pointless, I just want to stay in bed.", "depression"),
    ("I feel worthless and numb, disconnected from everyone around me.", "depression"),
    ("Deep sadness lingers and I have lost interest in things I loved.", "depression"),
    ("I am exhausted, tearful, and can't see any hope ahead.", "depression"),
    ("Feeling down and defeated, like a dark cloud follows me.", "depression"),
    ("I feel isolated and empty, struggling to get through each day.", "depression"),
]
