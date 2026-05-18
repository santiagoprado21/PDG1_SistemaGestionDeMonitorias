CREATE TABLE IF NOT EXISTS sigma.monitor_survey_integration_config (
    id BIGSERIAL PRIMARY KEY,
    apps_script_url TEXT,
    dashboard_url TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

INSERT INTO sigma.monitor_survey_integration_config (apps_script_url, dashboard_url)
SELECT NULL, NULL
WHERE NOT EXISTS (SELECT 1 FROM sigma.monitor_survey_integration_config);
