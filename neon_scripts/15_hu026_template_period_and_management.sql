ALTER TABLE IF EXISTS sigma.monitor_survey_template
    ADD COLUMN IF NOT EXISTS created_for_semester VARCHAR(20);

UPDATE sigma.monitor_survey_template
SET created_for_semester = COALESCE(created_for_semester, 'N/A')
WHERE created_for_semester IS NULL;
