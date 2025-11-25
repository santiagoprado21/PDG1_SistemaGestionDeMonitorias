-- HU-018: Registro de progreso de actividades
-- Tabla para guardar el historial de avances y evidencia de soporte
CREATE TABLE IF NOT EXISTS activity_progress (
    id SERIAL PRIMARY KEY,
    activity_id INTEGER NOT NULL REFERENCES activity(id) ON DELETE CASCADE,
    progress_percentage INTEGER NOT NULL CHECK (progress_percentage BETWEEN 0 AND 100),
    progress_comment VARCHAR(500),
    evidence_path VARCHAR(512),
    evidence_name VARCHAR(255),
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    created_by VARCHAR(100),
    created_by_role VARCHAR(20),
    created_by_name VARCHAR(150)
);

-- Campos de apoyo en actividad para el resumen del último progreso
ALTER TABLE activity ADD COLUMN IF NOT EXISTS progress_percentage INTEGER DEFAULT 0;
ALTER TABLE activity ADD COLUMN IF NOT EXISTS progress_comment VARCHAR(500);
ALTER TABLE activity ADD COLUMN IF NOT EXISTS progress_updated_at TIMESTAMP;
ALTER TABLE activity ADD COLUMN IF NOT EXISTS progress_updated_by VARCHAR(100);
ALTER TABLE activity ADD COLUMN IF NOT EXISTS progress_updated_by_role VARCHAR(20);
ALTER TABLE activity ADD COLUMN IF NOT EXISTS progress_updated_by_name VARCHAR(150);
ALTER TABLE activity ADD COLUMN IF NOT EXISTS progress_evidence_path VARCHAR(512);
ALTER TABLE activity ADD COLUMN IF NOT EXISTS progress_evidence_name VARCHAR(255);
