"""Pydantic request/response schemas for the ML service."""
from typing import Dict, List, Optional

from pydantic import BaseModel, Field


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


class JournalAnalysisResponse(BaseModel):
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


class MoodPredictionRequest(BaseModel):
    recent_scores: List[int] = Field(default_factory=list, description="Recent mood scores (0-100)")


class MoodPredictionResponse(BaseModel):
    predicted_score: float
    trend: str
    confidence: float
    rationale: str


class TriggerDetectionRequest(BaseModel):
    text: str = Field(..., min_length=1)


class DetectedTrigger(BaseModel):
    category: str
    matched_terms: List[str]
    intensity: int


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
    labels: List[str] = Field(default_factory=list)
    train_size: int = 0
    test_size: int = 0
    models: Dict[str, ModelInfo] = Field(default_factory=dict)
