from fastapi.testclient import TestClient

from app.main import app

client = TestClient(app)


def test_health_reports_model_version_and_dataset():
    response = client.get("/health")
    assert response.status_code == 200
    body = response.json()
    assert body["status"] == "ok"
    assert body["model_version"]
    assert "mode" in body["dataset"]


def test_analyze_journal_includes_triggers_and_version():
    response = client.post(
        "/analyze/journal",
        json={"text": "The workload and a looming deadline kept me awake and anxious"},
    )
    assert response.status_code == 200
    body = response.json()
    assert body["prediction"] in {"normal", "stress", "anxiety", "depression"}
    assert body["model_version"]
    categories = {trigger["category"] for trigger in body["triggers"]}
    assert "Workload" in categories
    assert "Sleep" in categories


def test_batch_analysis_echoes_references():
    response = client.post(
        "/analyze/batch",
        json={
            "items": [
                {"reference": "a", "text": "I feel calm and grateful today"},
                {"reference": "b", "text": "I am exhausted and overwhelmed by work"},
            ]
        },
    )
    assert response.status_code == 200
    results = response.json()["results"]
    assert [item["reference"] for item in results] == ["a", "b"]
    assert all(item["error"] is None for item in results)
    assert all(item["analysis"]["sentiment"] for item in results)


def test_batch_rejects_more_than_fifty_items():
    items = [{"reference": str(i), "text": "hello"} for i in range(51)]
    response = client.post("/analyze/batch", json={"items": items})
    assert response.status_code == 422


def test_batch_analysis_matches_single_analysis():
    text = "I am anxious about the deadline"
    single = client.post("/analyze/journal", json={"text": text}).json()
    batch = client.post(
        "/analyze/batch", json={"items": [{"reference": "x", "text": text}]}
    ).json()["results"][0]["analysis"]
    assert single["prediction"] == batch["prediction"]
    assert single["sentiment"] == batch["sentiment"]
    assert single["triggers"] == batch["triggers"]
