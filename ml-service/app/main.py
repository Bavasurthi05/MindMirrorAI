import logging
import os

from fastapi import Depends, FastAPI, Header, HTTPException, status

from . import ml_models
from .analysis import (
    analyze_sentiment,
    classify_emotion,
    detect_triggers,
    predict_mental_state,
    predict_mood,
)
from .seed_data import DATASET_PROFILE
from .training import registry, runner
from .schemas import (
    BatchAnalysisRequest,
    BatchAnalysisResponse,
    BatchAnalysisResult,
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
    ModelVersionInfo,
    ModelVersionsResponse,
    PromoteRequest,
    PromoteResponse,
    TrainJobResponse,
    TrainRunRequest,
    WeeklyInsightsRequest,
    WeeklyInsightsResponse,
)

app = FastAPI(title="Mental Health ML Service", version="0.4.0")
logger = logging.getLogger(__name__)

# Training and promotion are privileged: they change what every user's predictions come
# from. The service is internal-only, but this stops any process that can reach it from
# silently swapping the deployed model.
TRAINING_TOKEN = os.getenv("ML_TRAINING_TOKEN", "").strip()


def require_training_token(x_training_token: str | None = Header(default=None)) -> None:
    if not TRAINING_TOKEN:
        # Unset means local/dev: allow, but make the exposure obvious in the logs.
        logger.warning("ML_TRAINING_TOKEN is not set — training endpoints are unauthenticated")
        return
    if x_training_token != TRAINING_TOKEN:
        raise HTTPException(status_code=status.HTTP_401_UNAUTHORIZED, detail="Invalid training token")


def _dataset_runtime_summary() -> dict:
    training_size = int(DATASET_PROFILE.get("training_samples_used", 0))
    source_size = int(DATASET_PROFILE.get("source_dataset_size", 0))
    mode = "full" if source_size > 0 and training_size >= source_size else "reduced"
    return {
        "mode": mode,
        "training_samples": training_size,
        "source_samples": source_size,
        "emotion_label_count": len(DATASET_PROFILE.get("emotion_labels", [])),
    }


@app.on_event("startup")
def log_dataset_profile() -> None:
    dataset = _dataset_runtime_summary()
    logger.info(
        "ML dataset profile loaded: mode=%s training_samples=%s source_samples=%s labels=%s",
        dataset["mode"],
        dataset["training_samples"],
        dataset["source_samples"],
        dataset["emotion_label_count"],
    )


@app.get("/health")
def health_check():
    return {
        "status": "ok",
        "model_version": ml_models.get_model_version(),
        "model_available": ml_models.is_available(),
        "dataset": _dataset_runtime_summary(),
    }


def _trigger_models(text: str) -> list[DetectedTrigger]:
    return [
        DetectedTrigger(category=category, matched_terms=terms, intensity=min(10, 3 + 2 * len(terms)))
        for category, terms in detect_triggers(text)
    ]


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
        triggers=_trigger_models(text),
        model_version=ml_models.get_model_version(),
    )


@app.post("/analyze/journal", response_model=JournalAnalysisResponse)
def analyze_journal(request: JournalAnalysisRequest) -> JournalAnalysisResponse:
    return _build_analysis(request.text)


@app.post("/analyze/social", response_model=JournalAnalysisResponse)
def analyze_social(request: SocialAnalysisRequest) -> JournalAnalysisResponse:
    return _build_analysis(request.text)


@app.post("/analyze/batch", response_model=BatchAnalysisResponse)
def analyze_batch(request: BatchAnalysisRequest) -> BatchAnalysisResponse:
    """Analyze up to 50 texts in one round trip.

    A failure on one item never fails the batch: that item carries an `error`
    and the rest still return results.
    """
    results: list[BatchAnalysisResult] = []
    for item in request.items:
        try:
            results.append(
                BatchAnalysisResult(reference=item.reference, analysis=_build_analysis(item.text))
            )
        except Exception as exc:  # pragma: no cover - defensive per-item isolation
            logger.exception("Batch analysis failed for reference=%s", item.reference)
            results.append(BatchAnalysisResult(reference=item.reference, error=str(exc)))
    return BatchAnalysisResponse(results=results)


@app.get("/models/metrics", response_model=ModelMetricsResponse)
def model_metrics() -> ModelMetricsResponse:
    metrics = ml_models.get_metrics()
    if not metrics:
        return ModelMetricsResponse(
            available=ml_models.is_available(),
            backend="random_forest" if ml_models.is_available() else "heuristic",
            version=ml_models.get_model_version(),
        )
    models = {
        key: ModelInfo(**value) for key, value in metrics.get("models", {}).items()
    }
    return ModelMetricsResponse(
        available=ml_models.is_available(),
        backend="random_forest" if ml_models.is_available() else "heuristic",
        version=ml_models.get_model_version(),
        labels=metrics.get("labels", []),
        emotion_labels=metrics.get("emotion_labels", []),
        train_size=metrics.get("train_size", 0),
        test_size=metrics.get("test_size", 0),
        dataset_profile=metrics.get("dataset_profile", {}),
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
    return TriggerDetectionResponse(triggers=_trigger_models(request.text))


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


# --- Training and model registry -----------------------------------------------------


def _job_response(job: dict) -> TrainJobResponse:
    return TrainJobResponse(
        job_id=job.get("job_id", ""),
        status=job.get("status", "UNKNOWN"),
        message=job.get("message"),
        started_at=job.get("started_at"),
        finished_at=job.get("finished_at"),
        version=job.get("version"),
        gate=job.get("gate"),
        corpus=job.get("corpus"),
        metrics=job.get("metrics"),
    )


@app.post("/train/run", response_model=TrainJobResponse,
          dependencies=[Depends(require_training_token)])
def train_run(request: TrainRunRequest) -> TrainJobResponse:
    """Start a retraining run. Returns immediately with a job id to poll.

    Producing a new version never deploys it — see /models/promote.
    """
    rows = [example.model_dump() for example in request.examples]
    job_id = runner.start(rows, request.max_per_user)
    return _job_response(runner.get_job(job_id) or {"job_id": job_id, "status": "RUNNING"})


@app.get("/train/status/{job_id}", response_model=TrainJobResponse,
         dependencies=[Depends(require_training_token)])
def train_status(job_id: str) -> TrainJobResponse:
    job = runner.get_job(job_id)
    if job is None:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Unknown job")
    return _job_response(job)


@app.get("/models/versions", response_model=ModelVersionsResponse)
def model_versions() -> ModelVersionsResponse:
    return ModelVersionsResponse(
        active=registry.active_version(),
        versions=[ModelVersionInfo(**entry) for entry in registry.list_versions()],
    )


@app.get("/models/active", response_model=ModelVersionInfo)
def active_model() -> ModelVersionInfo:
    entry = registry.active_entry()
    if entry is None:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="No active version")
    return ModelVersionInfo(**entry)


@app.post("/models/promote", response_model=PromoteResponse,
          dependencies=[Depends(require_training_token)])
def promote_model(request: PromoteRequest) -> PromoteResponse:
    """Deploy a version. Refuses one that failed the gate unless explicitly forced."""
    entry = registry.get_version(request.version)
    if entry is None:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Unknown version")

    gate_passed = bool(entry.get("gate", {}).get("passed"))
    if not gate_passed and not request.force:
        failed = ", ".join(entry.get("gate", {}).get("failed", []))
        raise HTTPException(
            status_code=status.HTTP_409_CONFLICT,
            detail=f"Version {request.version} failed the quality gate ({failed}). "
                   "Re-send with force=true to deploy it anyway.",
        )

    try:
        registry.promote(request.version)
    except ValueError as exc:
        raise HTTPException(status_code=status.HTTP_400_BAD_REQUEST, detail=str(exc)) from exc

    reloaded = ml_models.reload()
    if not gate_passed:
        logger.warning("Version %s was force-promoted despite failing the gate", request.version)
    return PromoteResponse(
        active=request.version, reloaded=reloaded, gate_passed=gate_passed, forced=not gate_passed
    )


@app.post("/models/rollback", response_model=PromoteResponse,
          dependencies=[Depends(require_training_token)])
def rollback_model(request: PromoteRequest) -> PromoteResponse:
    """Return to a previously trained version. Never gated — this is the escape hatch."""
    try:
        registry.rollback(request.version)
    except ValueError as exc:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail=str(exc)) from exc

    entry = registry.get_version(request.version) or {}
    return PromoteResponse(
        active=request.version,
        reloaded=ml_models.reload(),
        gate_passed=bool(entry.get("gate", {}).get("passed")),
        forced=False,
    )
