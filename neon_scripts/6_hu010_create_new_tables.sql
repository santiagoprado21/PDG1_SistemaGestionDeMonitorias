-- ================================================================
-- HU-010: Scripts de migración para separación de postulación y monitoría
-- ================================================================
-- Descripción:
--   Crea las nuevas tablas para el flujo de HU-010 donde:
--   1. Profesor crea convocatoria (monitoring_request)
--   2. Estudiantes se postulan (monitor_application)
--   3. Profesor selecciona 1 estudiante
--   4. Se crea monitoría con monitor asignado
--   5. Jefe de dpto aprueba paquete completo
-- ================================================================

-- Establecer el schema correcto
SET search_path TO sigma;

-- ================================================================
-- TABLA 1: monitoring_request (Convocatorias de Monitoría)
-- ================================================================
-- Representa las postulaciones/convocatorias creadas por profesores
-- Estado inicial: CONVOCATORIA_ABIERTA
-- ================================================================

CREATE TABLE IF NOT EXISTS monitoring_request (
    id BIGSERIAL PRIMARY KEY,
    
    -- Relaciones con entidades existentes
    professor_id VARCHAR(20) NOT NULL REFERENCES professor(id),
    course_id BIGINT NOT NULL REFERENCES course(id),
    school_id BIGINT NOT NULL REFERENCES school(id),
    program_id BIGINT NOT NULL REFERENCES program(id),
    
    -- Detalles de la solicitud (HU-010)
    requested_hours INTEGER NOT NULL CHECK (requested_hours > 0),
    justification TEXT NOT NULL CHECK (char_length(justification) > 0),
    semester VARCHAR(8) NOT NULL,
    start_date DATE NOT NULL,
    finish_date DATE NOT NULL,
    
    -- Requisitos para postulantes
    required_average_grade DOUBLE PRECISION DEFAULT 4.0,
    required_course_grade DOUBLE PRECISION DEFAULT 4.0,
    hourly_rate DOUBLE PRECISION,
    
    -- Estado de la convocatoria
    status VARCHAR(30) NOT NULL DEFAULT 'CONVOCATORIA_ABIERTA' 
        CHECK (status IN ('CONVOCATORIA_ABIERTA', 'MONITOR_SELECCIONADO', 
                          'PENDIENTE_APROBACION', 'APROBADA', 'RECHAZADA', 'CANCELADA')),
    
    -- Auditoría
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    
    -- Constraints
    CONSTRAINT valid_dates CHECK (start_date <= finish_date),
    CONSTRAINT unique_professor_course_semester UNIQUE (professor_id, course_id, semester)
);

-- Índices para mejorar performance
CREATE INDEX idx_monitoring_request_status ON monitoring_request(status);
CREATE INDEX idx_monitoring_request_professor ON monitoring_request(professor_id);
CREATE INDEX idx_monitoring_request_program ON monitoring_request(program_id);
CREATE INDEX idx_monitoring_request_semester ON monitoring_request(semester);
CREATE INDEX idx_monitoring_request_created_at ON monitoring_request(created_at);

-- Comentarios
COMMENT ON TABLE monitoring_request IS 'Convocatorias de monitoría creadas por profesores (HU-010)';
COMMENT ON COLUMN monitoring_request.status IS 'Estados: CONVOCATORIA_ABIERTA -> MONITOR_SELECCIONADO -> PENDIENTE_APROBACION -> APROBADA/RECHAZADA';
COMMENT ON COLUMN monitoring_request.justification IS 'Justificación del profesor para solicitar la monitoría (HU-010)';

-- ================================================================
-- TABLA 2: monitor_application (Postulaciones de Estudiantes)
-- ================================================================
-- Representa las postulaciones de estudiantes a las convocatorias
-- Los estudiantes se postulan a monitoring_request, NO a monitoring
-- ================================================================

CREATE TABLE IF NOT EXISTS monitor_application (
    id BIGSERIAL PRIMARY KEY,
    
    -- Relaciones
    monitoring_request_id BIGINT NOT NULL REFERENCES monitoring_request(id) ON DELETE CASCADE,
    monitor_id VARCHAR(20) NOT NULL REFERENCES monitor(code),
    
    -- Estado de la postulación
    status VARCHAR(20) NOT NULL DEFAULT 'POSTULADO'
        CHECK (status IN ('POSTULADO', 'SELECCIONADO', 'NO_SELECCIONADO')),
    
    -- Información de la postulación
    application_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    motivation_letter TEXT,
    notes TEXT,
    updated_at TIMESTAMP,
    
    -- Constraints
    CONSTRAINT unique_request_monitor UNIQUE (monitoring_request_id, monitor_id)
);

-- Índices
CREATE INDEX idx_monitor_application_request ON monitor_application(monitoring_request_id);
CREATE INDEX idx_monitor_application_monitor ON monitor_application(monitor_id);
CREATE INDEX idx_monitor_application_status ON monitor_application(status);

-- Comentarios
COMMENT ON TABLE monitor_application IS 'Postulaciones de estudiantes a convocatorias de monitoría (HU-010)';
COMMENT ON COLUMN monitor_application.status IS 'POSTULADO: Se postuló, SELECCIONADO: Profesor lo eligió, NO_SELECCIONADO: No fue elegido';
COMMENT ON CONSTRAINT unique_request_monitor ON monitor_application IS 'Un estudiante solo puede postularse una vez por convocatoria';

-- ================================================================
-- TABLA 3: Modificaciones a monitoring (Monitoría Oficial)
-- ================================================================
-- Agregar nuevas columnas para integrar con el nuevo flujo
-- ================================================================

-- Columna para relacionar con la convocatoria que la originó
ALTER TABLE monitoring 
ADD COLUMN IF NOT EXISTS monitoring_request_id BIGINT REFERENCES monitoring_request(id);

-- Columna para el monitor asignado desde la creación (nuevo flujo)
ALTER TABLE monitoring 
ADD COLUMN IF NOT EXISTS assigned_monitor_id VARCHAR(20) REFERENCES monitor(code);

-- Estado de aprobación por el jefe de departamento
ALTER TABLE monitoring 
ADD COLUMN IF NOT EXISTS approval_status VARCHAR(30)
    CHECK (approval_status IN ('PENDIENTE_APROBACION', 'APROBADA', 'RECHAZADA'));

-- Justificación (copiada de monitoring_request)
ALTER TABLE monitoring 
ADD COLUMN IF NOT EXISTS justification TEXT;

-- Auditoría de aprobación
ALTER TABLE monitoring 
ADD COLUMN IF NOT EXISTS approved_by VARCHAR(20);

ALTER TABLE monitoring 
ADD COLUMN IF NOT EXISTS approval_comment TEXT;

ALTER TABLE monitoring 
ADD COLUMN IF NOT EXISTS approval_date TIMESTAMP;

-- Índices para las nuevas columnas
CREATE INDEX IF NOT EXISTS idx_monitoring_request_id ON monitoring(monitoring_request_id);
CREATE INDEX IF NOT EXISTS idx_monitoring_assigned_monitor ON monitoring(assigned_monitor_id);
CREATE INDEX IF NOT EXISTS idx_monitoring_approval_status ON monitoring(approval_status);

-- Comentarios
COMMENT ON COLUMN monitoring.monitoring_request_id IS 'Referencia a la convocatoria que originó esta monitoría (NULL para monitorías del flujo antiguo)';
COMMENT ON COLUMN monitoring.assigned_monitor_id IS 'Monitor asignado desde la creación (nuevo flujo HU-010)';
COMMENT ON COLUMN monitoring.approval_status IS 'Estado de aprobación por jefe de departamento (nuevo flujo)';
COMMENT ON COLUMN monitoring.justification IS 'Justificación de la monitoría (copiada de monitoring_request)';

-- ================================================================
-- DATOS INICIALES / MIGRACIÓN
-- ================================================================

-- Trigger para actualizar updated_at automáticamente
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

-- Aplicar trigger a monitoring_request
DROP TRIGGER IF EXISTS update_monitoring_request_updated_at ON monitoring_request;
CREATE TRIGGER update_monitoring_request_updated_at 
    BEFORE UPDATE ON monitoring_request 
    FOR EACH ROW 
    EXECUTE FUNCTION update_updated_at_column();

-- Aplicar trigger a monitor_application
DROP TRIGGER IF EXISTS update_monitor_application_updated_at ON monitor_application;
CREATE TRIGGER update_monitor_application_updated_at 
    BEFORE UPDATE ON monitor_application 
    FOR EACH ROW 
    EXECUTE FUNCTION update_updated_at_column();

-- ================================================================
-- VERIFICACIÓN
-- ================================================================

-- Verificar que las tablas se crearon correctamente
DO $$
BEGIN
    RAISE NOTICE 'Verificando tablas creadas...';
    
    IF EXISTS (SELECT 1 FROM information_schema.tables 
               WHERE table_schema = 'sigma' AND table_name = 'monitoring_request') THEN
        RAISE NOTICE '✓ Tabla monitoring_request creada';
    END IF;
    
    IF EXISTS (SELECT 1 FROM information_schema.tables 
               WHERE table_schema = 'sigma' AND table_name = 'monitor_application') THEN
        RAISE NOTICE '✓ Tabla monitor_application creada';
    END IF;
    
    IF EXISTS (SELECT 1 FROM information_schema.columns 
               WHERE table_schema = 'sigma' 
               AND table_name = 'monitoring' 
               AND column_name = 'monitoring_request_id') THEN
        RAISE NOTICE '✓ Columna monitoring.monitoring_request_id agregada';
    END IF;
    
    IF EXISTS (SELECT 1 FROM information_schema.columns 
               WHERE table_schema = 'sigma' 
               AND table_name = 'monitoring' 
               AND column_name = 'approval_status') THEN
        RAISE NOTICE '✓ Columna monitoring.approval_status agregada';
    END IF;
    
    RAISE NOTICE 'Migración completada exitosamente!';
END $$;

