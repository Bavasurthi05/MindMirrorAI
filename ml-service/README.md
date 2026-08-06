# ML Service

FastAPI-based machine learning service for the Mental Health Analytics Platform.

## Dataset

- Uses a GoEmotions-style synthetic corpus with 27 emotion labels.
- Source corpus size is approximately 58k comments.
- For compatibility with the app's prediction contract, emotion labels are mapped to:
   `normal`, `stress`, `anxiety`, `depression`.
- Control training volume with `ML_SYNTHETIC_TRAINING_SAMPLES` (default `58320`).
   For faster local iteration, use `12000`.

## Setup

1. Create and activate a virtual environment.
2. Install dependencies:
   pip install -r requirements.txt
3. Start the service:
   uvicorn app.main:app --reload --host 0.0.0.0 --port 8000
