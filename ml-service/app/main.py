from fastapi import FastAPI

from . import ml_models
from .analysis import (
    analyze_sentiment,
    classify_emotion,
    detect_triggers,
    predict_mental_state,
    predict_mood,
)
from .schemas import (
    DetectedTrigger,
    FeatureReason,
    JournalAnalysisRequest,
    JournalAnalysisResponse,
    ModelInfo,
    ModelMetricsResponse,
    MoodPredictionRequest,
    MoodPredictionResponse,
    SocialAnalysisRequest,
    TokenContribution,
    TriggerDetectionRequest,
    TriggerDetectionResponse,
    WeeklyInsightsRequest,
    WeeklyInsightsResponse,
)

app = FastAPI(title="Mental Health ML Service", version="0.3.0")


@app.get("/health")
def health_check():
    return {"status": "ok"}


def _build_analysis(text: str) -> JournalAnalysisResponse:
    sentiment, sentiment_score, contributions = analyze_sentiment(text)
    emotion, emotion_scores = classify_emotion(text)
    state = predict_mental_state(text)
    return JournalAnalysisResponse(
        sentiment=sentiment,
        sentiment_score=sentiment_score,
        emotion=emotion,
        emotion_scores=emotion_scores,
        explanation=[TokenContribution(token=token, weight=weight) for token, weight in contributions],
        prediction=state["label"],
        prediction_confidence=state["confidence"],
        prediction_probabilities=state["probabilities"],
        reasons=[FeatureReason(**reason) for reason in state["reasons"]],
        model_backend=state["backend"],
    )


@app.post("/analyze/journal", response_model=JournalAnalysisResponse)
def analyze_journal(request: JournalAnalysisRequest) -> JournalAnalysisResponse:
    return _build_analysis(request.text)


@app.post("/analyze/social", response_model=JournalAnalysisResponse)
def analyze_social(request: SocialAnalysisRequest) -> JournalAnalysisResponse:
    return _build_analysis(request.text)


@app.get("/models/metrics", response_model=ModelMetricsResponse)
def model_metrics() -> ModelMetricsResponse:
    metrics = ml_models.get_metrics()
    if not metrics:
        return ModelMetricsResponse(
            available=ml_models.is_available(),
            backend="random_forest" if ml_models.is_available() else "heuristic",
        )
    models = {
        key: ModelInfo(**value) for key, value in metrics.get("models", {}).items()
    }
    return ModelMetricsResponse(
        available=ml_models.is_available(),
        backend="random_forest" if ml_models.is_available() else "heuristic",
        labels=metrics.get("labels", []),
        train_size=metrics.get("train_size", 0),
        test_size=metrics.get("test_size", 0),
        models=models,
    )


@app.post("/predict/mood", response_model=MoodPredictionResponse)
def predict_mood_endpoint(request: MoodPredictionRequest) -> MoodPredictionResponse:
    predicted, trend, confidence, rationale = predict_mood(request.recent_scores)
    return MoodPredictionResponse(
        predicted_score=predicted,
        trend=trend,
        confidence=confidence,
        rationale=rationale,
    )


@app.post("/detect/triggers", response_model=TriggerDetectionResponse)
def detect_triggers_endpoint(request: TriggerDetectionRequest) -> TriggerDetectionResponse:
    detected = detect_triggers(request.text)
    triggers = [
        DetectedTrigger(category=category, matched_terms=terms, intensity=min(10, 3 + 2 * len(terms)))
        for category, terms in detected
    ]
    return TriggerDetectionResponse(triggers=triggers)


@app.post("/insights/weekly", response_model=WeeklyInsightsResponse)
def weekly_insights(request: WeeklyInsightsRequest) -> WeeklyInsightsResponse:
    scores = request.mood_scores
    avg_mood = sum(scores) / len(scores) if scores else 50.0
    highlights: list[str] = []

    highlights.append(
        f"You recorded {len(scores)} mood check-in(s) with an average of {avg_mood:.0f}/100."
    )
    highlights.append(
        f"You wrote {request.journal_count} journal entr(y/ies) this week."
    )
    if request.trigger_count:
        highlights.append(
            f"{request.trigger_count} trigger(s) logged at an average intensity of "
            f"{request.average_trigger_intensity:.1f}/10."
        )

    if avg_mood >= 70:
        focus_area = "Maintain momentum"
    elif avg_mood >= 50:
        focus_area = "Steady the routine"
    else:
        focus_area = "Prioritize rest and support"

    wellbeing_index = int(max(0, min(100, avg_mood - (request.average_trigger_intensity or 0) * 2)))

    return WeeklyInsightsResponse(
        highlights=highlights,
        focus_area=focus_area,
        wellbeing_index=wellbeing_index,
    )
