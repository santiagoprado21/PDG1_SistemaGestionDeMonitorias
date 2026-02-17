-- HU-018: Registro de progreso de actividades
SET search_path TO sigma;

CREATE TABLE IF NOT EXISTS activity_progress (
    id SERIAL PRIMARY KEY,
    activity_id INTEGER NOT NULL REFERENCES sigma.activity(id) ON DELETE CASCADE,
    progress_percentage INTEGER NOT NULL CHECK (progress_percentage BETWEEN 0 AND 100),
    progress_comment VARCHAR(500),
    evidence_path VARCHAR(512),
    evidence_name VARCHAR(255),
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    created_by VARCHAR(100),
    created_by_role VARCHAR(20),
    created_by_name VARCHAR(150)
);

ALTER TABLE sigma.activity ADD COLUMN IF NOT EXISTS progress_percentage INTEGER DEFAULT 0;
ALTER TABLE sigma.activity ADD COLUMN IF NOT EXISTS progress_comment VARCHAR(500);
ALTER TABLE sigma.activity ADD COLUMN IF NOT EXISTS progress_updated_at TIMESTAMP;
ALTER TABLE sigma.activity ADD COLUMN IF NOT EXISTS progress_updated_by VARCHAR(100);
ALTER TABLE sigma.activity ADD COLUMN IF NOT EXISTS progress_updated_by_role VARCHAR(20);
ALTER TABLE sigma.activity ADD COLUMN IF NOT EXISTS progress_updated_by_name VARCHAR(150);
ALTER TABLE sigma.activity ADD COLUMN IF NOT EXISTS progress_evidence_path VARCHAR(512);
ALTER TABLE sigma.activity ADD COLUMN IF NOT EXISTS progress_evidence_name VARCHAR(255);