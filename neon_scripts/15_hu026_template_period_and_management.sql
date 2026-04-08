ALTER TABLE IF EXISTS sigma.monitor_survey_template
    ADD COLUMN IF NOT EXISTS created_for_semester VARCHAR(20);

DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = 'sigma'
          AND table_name = 'monitor_survey_template'
          AND column_name = 'created_for_semester'
    ) THEN
        UPDATE sigma.monitor_survey_template
        SET created_for_semester = COALESCE(created_for_semester, 'N/A')
        WHERE created_for_semester IS NULL;
    END IF;
END $$;
