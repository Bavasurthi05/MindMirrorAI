from app.analysis import predict_mental_state
from app.preprocessing import clean_text, preprocess, tokenize


def test_clean_text_strips_urls_and_mentions():
    cleaned = clean_text("Check https://example.com @friend #mood I feel GREAT!")
    assert "http" not in cleaned
    assert "@friend" not in cleaned
    assert "#mood" not in cleaned
    assert "great" in cleaned


def test_preprocess_removes_stopwords():
    processed = preprocess("I am feeling very tired and the work is hard")
    assert "the" not in processed.split()
    assert "tired" in processed.split()


def test_tokenize_optionally_removes_stopwords():
    tokens = tokenize("I am tired", remove_stopwords=True)
    assert "tired" in tokens
    assert "the" not in tokens


def test_predict_mental_state_shape():
    result = predict_mental_state("I feel hopeless, empty and so sad with no motivation")
    assert result["label"] in {"normal", "stress", "anxiety", "depression"}
    assert 0.0 <= result["confidence"] <= 1.0
    assert isinstance(result["reasons"], list)
    assert result["backend"] in {"random_forest", "heuristic"}
    for reason in result["reasons"]:
        assert {"feature", "weight", "percentage"} <= set(reason.keys())
