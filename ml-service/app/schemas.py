"""Pydantic request/response schemas for the ML service."""
from typing import Dict, List, Optional

from pydantic import BaseModel, ConfigDict, Field

# `model_backend` / `model_version` are part of the public API contract; opt out of
# Pydantic's protected "model_" namespace so they don't emit shadowing warnings.
_ALLOW_MODEL_PREFIX = ConfigDict(protected_namespaces=())


class JournalAnalysisRequest(BaseModel):
    text: str = Field(..., min_length=1, description="Journal entry text to analyze")


class SocialAnalysisRequest(BaseModel):
    text: str = Field(..., min_length=1, description="Social media post text to analyze")
    source: Optional[str] = Field(default="social", description="Origin of the text")


class TokenContribution(BaseModel):
    token: str
    weight: float


class FeatureReason(BaseModel):
    feature: str
    weight: float
    percentage: float


class DetectedTrigger(BaseModel):
    category: str
    matched_terms: List[str]
    intensity: int


class JournalAnalysisResponse(BaseModel):
    model_config = _ALLOW_MODEL_PREFIX

    sentiment: str
    sentiment_score: float
    emotion: str
    emotion_scores: dict[str, float]
    explanation: List[TokenContribution]
    prediction: str
    prediction_confidence: float
    prediction_probabilities: Dict[str, float]
    reasons: List[FeatureReason]
    model_backend: str
    # Triggers travel with the analysis so callers need one round trip, not two.
    triggers: List[DetectedTrigger] = Field(default_factory=list)
    model_version: str = "unknown"


class BatchAnalysisItem(BaseModel):
    """One text in a batch request, tagged with a caller-supplied reference."""

    reference: str = Field(..., description="Caller's id, echoed back so results can be matched")
    text: str = Field(..., min_length=1)


class BatchAnalysisRequest(BaseModel):
    items: List[BatchAnalysisItem] = Field(..., min_length=1, max_length=50)


class BatchAnalysisResult(BaseModel):
    reference: str
    analysis: Optional[JournalAnalysisResponse] = None
    error: Optional[str] = None


class BatchAnalysisResponse(BaseModel):
    results: List[BatchAnalysisResult]


class MoodPredictionRequest(BaseModel):
    recent_scores: List[int] = Field(default_factory=list, description="Recent mood scores (0-100)")


class MoodPredictionResponse(BaseModel):
    predicted_score: float
    trend: str
    confidence: float
    rationale: str


class TriggerDetectionRequest(BaseModel):
    text: str = Field(..., min_length=1)


class TriggerDetectionResponse(BaseModel):
    triggers: List[DetectedTrigger]


class WeeklyInsightsRequest(BaseModel):
    mood_scores: List[int] = Field(default_factory=list)
    journal_count: int = 0
    trigger_count: int = 0
    average_trigger_intensity: Optional[float] = 0.0


class WeeklyInsightsResponse(BaseModel):
    highlights: List[str]
    focus_area: str
    wellbeing_index: int


class ModelInfo(BaseModel):
    name: str
    accuracy: float
    f1_macro: float
    deployed: bool


class ModelMetricsResponse(BaseModel):
    available: bool
    backend: str
    version: str = "unknown"
    labels: List[str] = Field(default_factory=list)
    emotion_labels: List[str] = Field(default_factory=list)
    train_size: int = 0
    test_size: int = 0
    dataset_profile: Dict[str, object] = Field(default_factory=dict)
    models: Dict[str, ModelInfo] = Field(default_factory=dict)


# --- Training / registry -------------------------------------------------------------


class TrainingExample(BaseModel):
    """One exported, already pseudonymized and PII-scrubbed training row."""

    text: str
    label: str
    source: str = "user_correction"
    weight: Optional[float] = None
    user_hash: Optional[str] = None


class TrainRunRequest(BaseModel):
    examples: List[TrainingExample] = Field(default_factory=list)
    max_per_user: int = Field(default=200, ge=1, le=10000)


class TrainJobResponse(BaseModel):
    model_config = _ALLOW_MODEL_PREFIX

    job_id: str
    status: str
    message: Optional[str] = None
    started_at: Optional[str] = None
    finished_at: Optional[str] = None
    version: Optional[str] = None
    gate: Optional[Dict[str, object]] = None
    corpus: Optional[Dict[str, object]] = None
    metrics: Optional[Dict[str, object]] = None


class ModelVersionInfo(BaseModel):
    model_config = _ALLOW_MODEL_PREFIX

    version: str
    created_at: Optional[str] = None
    promoted: bool = False
    promoted_at: Optional[str] = None
    metrics: Dict[str, object] = Field(default_factory=dict)
    corpus: Dict[str, object] = Field(default_factory=dict)
    gate: Dict[str, object] = Field(default_factory=dict)


class ModelVersionsResponse(BaseModel):
    model_config = _ALLOW_MODEL_PREFIX

    active: Optional[str] = None
    versions: List[ModelVersionInfo] = Field(default_factory=list)


class PromoteRequest(BaseModel):
    version: str
    """Set when an admin knowingly deploys a version that failed the gate."""
    force: bool = False


class PromoteResponse(BaseModel):
    model_config = _ALLOW_MODEL_PREFIX

    active: str
    reloaded: bool
    gate_passed: bool
    forced: bool
