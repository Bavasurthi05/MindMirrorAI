-- Move social-import analyses out of assessment_submissions.
--
-- SocialAccountService used to store analysis output as a fake questionnaire
-- submission (questionnaire_key = 'social-import:<provider>', raw post text in
-- `answers`). That polluted assessment history and skewed every wellness score
-- derived from the latest assessment. Those rows now belong in analysis_results.

INSERT INTO analysis_results (
    user_id, source_type, source_id, status, attempt_count,
    source_text, sentiment_score, prediction,
    model_backend, model_version, analyzed_at, created_at, updated_at
)
SELECT
    s.user_id,
    'SOCIAL',
    NULL,
    'OK',
    1,
    s.answers,
    s.total_score / 100.0,
    s.severity,
    'migrated',
    'unknown',
    s.submitted_at,
    s.created_at,
    s.updated_at
FROM assessment_submissions s
WHERE s.questionnaire_key LIKE 'social-import:%';

DELETE FROM assessment_submissions WHERE questionnaire_key LIKE 'social-import:%';
