from app.analysis import analyze_sentiment, classify_emotion, detect_triggers, predict_mood


def test_positive_sentiment():
    label, score, _ = analyze_sentiment("I feel calm, grateful and hopeful today")
    assert label == "positive"
    assert score > 0


def test_negative_sentiment():
    label, score, _ = analyze_sentiment("I am anxious, exhausted and overwhelmed")
    assert label == "negative"
    assert score < 0


def test_emotion_classification():
    emotion, scores = classify_emotion("I am so anxious and worried")
    assert emotion == "fear"
    assert scores


def test_trigger_detection():
    detected = detect_triggers("The workload and a looming deadline kept me awake")
    categories = {category for category, _ in detected}
    assert "Workload" in categories


def test_mood_prediction_trend():
    predicted, trend, confidence, _ = predict_mood([40, 45, 55, 60, 70])
    assert trend == "improving"
    assert 0 <= predicted <= 100
    assert 0 < confidence <= 1
