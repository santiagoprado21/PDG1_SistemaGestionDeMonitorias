-- ============================================================================
-- HU-011: Creación de plan de actividades para monitores
-- Script de migración: Crear tabla Rubric y extender Activity
-- Fecha: 2025-11-07
-- ============================================================================

-- Cambiar al schema sigma
SET search_path TO sigma;

-- ============================================================================
-- 1. CREAR TABLA RUBRIC (Rúbricas de evaluación)
-- ============================================================================

CREATE TABLE IF NOT EXISTS rubric (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    description TEXT,
    total_points INTEGER NOT NULL CHECK (total_points > 0),
    criteria JSONB NOT NULL, -- Array de criterios: [{"criterion": "...", "points": 10, "description": "..."}]
    created_by VARCHAR(20) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    -- Foreign Keys
    CONSTRAINT fk_rubric_professor FOREIGN KEY (created_by) REFERENCES professor(id) ON DELETE CASCADE
);

-- Comentarios
COMMENT ON TABLE rubric IS 'Rúbricas de evaluación para actividades';
COMMENT ON COLUMN rubric.id IS 'ID único de la rúbrica';
COMMENT ON COLUMN rubric.name IS 'Nombre de la rúbrica';
COMMENT ON COLUMN rubric.description IS 'Descripción general de la rúbrica';
COMMENT ON COLUMN rubric.total_points IS 'Puntos totales de la rúbrica';
COMMENT ON COLUMN rubric.criteria IS 'Criterios de evaluación en formato JSON';
COMMENT ON COLUMN rubric.created_by IS 'ID del profesor que creó la rúbrica';
COMMENT ON COLUMN rubric.created_at IS 'Fecha de creación';
COMMENT ON COLUMN rubric.updated_at IS 'Fecha de última modificación';

-- Índices para rendimiento
CREATE INDEX IF NOT EXISTS idx_rubric_created_by ON rubric(created_by);
CREATE INDEX IF NOT EXISTS idx_rubric_name ON rubric(name);

-- Trigger para actualizar updated_at automáticamente
CREATE OR REPLACE FUNCTION update_rubric_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trigger_rubric_updated_at ON rubric;
CREATE TRIGGER trigger_rubric_updated_at
    BEFORE UPDATE ON rubric
    FOR EACH ROW
    EXECUTE FUNCTION update_rubric_updated_at();

DO $$
BEGIN
    RAISE NOTICE '✓ Tabla rubric creada exitosamente';
END $$;

-- ============================================================================
-- 2. EXTENDER TABLA ACTIVITY (Agregar horarios, duración y rúbrica)
-- ============================================================================

-- 2.1 Agregar columnas de horario
ALTER TABLE activity
ADD COLUMN IF NOT EXISTS start_time TIME,
ADD COLUMN IF NOT EXISTS end_time TIME,
ADD COLUMN IF NOT EXISTS duration_hours DECIMAL(4,2) CHECK (duration_hours > 0),
ADD COLUMN IF NOT EXISTS recurrence VARCHAR(20), -- 'NONE', 'DAILY', 'WEEKLY'
ADD COLUMN IF NOT EXISTS priority VARCHAR(10) DEFAULT 'MEDIA', -- 'ALTA', 'MEDIA', 'BAJA'
ADD COLUMN IF NOT EXISTS rubric_id BIGINT;

-- Comentarios
COMMENT ON COLUMN activity.start_time IS 'Hora de inicio de la actividad';
COMMENT ON COLUMN activity.end_time IS 'Hora de fin de la actividad';
COMMENT ON COLUMN activity.duration_hours IS 'Duración estimada en horas';
COMMENT ON COLUMN activity.recurrence IS 'Recurrencia de la actividad (NONE, DAILY, WEEKLY)';
COMMENT ON COLUMN activity.priority IS 'Prioridad de la actividad (ALTA, MEDIA, BAJA)';
COMMENT ON COLUMN activity.rubric_id IS 'ID de la rúbrica asociada (opcional)';

-- 2.2 Agregar foreign key a rubric (si no existe)
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.table_constraints 
        WHERE constraint_name = 'fk_activity_rubric'
        AND table_name = 'activity'
    ) THEN
        ALTER TABLE activity
        ADD CONSTRAINT fk_activity_rubric 
        FOREIGN KEY (rubric_id) REFERENCES rubric(id) ON DELETE SET NULL;
    END IF;
END $$;

-- 2.3 Crear índices para optimización de consultas de horarios
CREATE INDEX IF NOT EXISTS idx_activity_start_time ON activity(start_time);
CREATE INDEX IF NOT EXISTS idx_activity_end_time ON activity(end_time);
CREATE INDEX IF NOT EXISTS idx_activity_finish_date ON activity(finish_date);
CREATE INDEX IF NOT EXISTS idx_activity_rubric ON activity(rubric_id);
CREATE INDEX IF NOT EXISTS idx_activity_priority ON activity(priority);

-- Índice compuesto para búsqueda de conflictos de horarios
DROP INDEX IF EXISTS idx_activity_schedule;
CREATE INDEX idx_activity_schedule 
ON activity(monitor_id, finish_date, start_time, end_time);

DO $$
BEGIN
    RAISE NOTICE '✓ Tabla activity extendida exitosamente con campos de horarios y rúbricas';
END $$;

-- ============================================================================
-- 3. CREAR TABLA DE CONFLICTOS DE HORARIOS (Log)
-- ============================================================================

CREATE TABLE IF NOT EXISTS activity_schedule_conflict (
    id BIGSERIAL PRIMARY KEY,
    activity_id_1 INTEGER REFERENCES activity(id) ON DELETE CASCADE,
    activity_id_2 INTEGER REFERENCES activity(id) ON DELETE CASCADE,
    conflict_date DATE NOT NULL,
    conflict_start_time TIME NOT NULL,
    conflict_end_time TIME NOT NULL,
    resolved BOOLEAN DEFAULT FALSE,
    detected_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    resolved_at TIMESTAMP,
    notes TEXT
);

COMMENT ON TABLE activity_schedule_conflict IS 'Log de conflictos de horarios detectados entre actividades';

CREATE INDEX IF NOT EXISTS idx_conflict_date ON activity_schedule_conflict(conflict_date);
CREATE INDEX IF NOT EXISTS idx_conflict_resolved ON activity_schedule_conflict(resolved);

DO $$
BEGIN
    RAISE NOTICE '✓ Tabla activity_schedule_conflict creada exitosamente';
END $$;

-- ============================================================================
-- 4. FUNCIÓN PARA DETECTAR CONFLICTOS DE HORARIOS
-- ============================================================================

CREATE OR REPLACE FUNCTION check_activity_time_conflict(
    p_monitor_id VARCHAR(100),
    p_date DATE,
    p_start_time TIME,
    p_end_time TIME,
    p_activity_id INTEGER DEFAULT NULL
) RETURNS TABLE (
    conflicting_activity_id INTEGER,
    conflicting_activity_name VARCHAR(100),
    conflicting_start_time TIME,
    conflicting_end_time TIME
) AS $$
BEGIN
    RETURN QUERY
    SELECT 
        a.id,
        a.name,
        a.start_time,
        a.end_time
    FROM activity a
    WHERE a.monitor_id = (SELECT code FROM monitor WHERE id = p_monitor_id)
        AND a.finish_date = p_date
        AND a.start_time IS NOT NULL
        AND a.end_time IS NOT NULL
        AND (p_activity_id IS NULL OR a.id != p_activity_id) -- Excluir la actividad actual en ediciones
        AND (
            -- Solapamiento de horarios
            (a.start_time, a.end_time) OVERLAPS (p_start_time, p_end_time)
        );
END;
$$ LANGUAGE plpgsql;

COMMENT ON FUNCTION check_activity_time_conflict IS 'Detecta conflictos de horarios para un monitor en una fecha específica';

DO $$
BEGIN
    RAISE NOTICE '✓ Función check_activity_time_conflict creada exitosamente';
END $$;

-- ============================================================================
-- 5. VISTA PARA CRONOGRAMA DE ACTIVIDADES
-- ============================================================================

CREATE OR REPLACE VIEW v_activity_schedule AS
SELECT 
    a.id,
    a.name,
    a.description,
    a.category,
    a.priority,
    a.finish_date AS activity_date,
    a.start_time,
    a.end_time,
    a.duration_hours,
    a.state,
    a.monitoring_id,
    m.semester,
    c.name AS course_name,
    p.name AS professor_name,
    mon.name || ' ' || mon.last_name AS monitor_name,
    mon.id AS monitor_id,
    r.name AS rubric_name,
    r.total_points AS rubric_points
FROM activity a
LEFT JOIN monitoring m ON a.monitoring_id = m.id
LEFT JOIN course c ON m.course_id = c.id
LEFT JOIN professor p ON a.professor_id = p.id
LEFT JOIN monitor mon ON a.monitor_id = mon.code
LEFT JOIN rubric r ON a.rubric_id = r.id
WHERE a.start_time IS NOT NULL AND a.end_time IS NOT NULL
ORDER BY a.finish_date, a.start_time;

COMMENT ON VIEW v_activity_schedule IS 'Vista consolidada del cronograma de actividades con horarios';

DO $$
BEGIN
    RAISE NOTICE '✓ Vista v_activity_schedule creada exitosamente';
END $$;

-- ============================================================================
-- 6. VERIFICACIÓN POST-MIGRACIÓN
-- ============================================================================

DO $$
DECLARE
    v_rubric_count INTEGER;
    v_activity_columns INTEGER;
    v_indexes INTEGER;
BEGIN
    -- Verificar tabla rubric
    SELECT COUNT(*) INTO v_rubric_count
    FROM information_schema.tables 
    WHERE table_schema = 'sigma' AND table_name = 'rubric';
    
    IF v_rubric_count = 0 THEN
        RAISE EXCEPTION '❌ ERROR: Tabla rubric no fue creada';
    END IF;
    
    -- Verificar columnas nuevas en activity
    SELECT COUNT(*) INTO v_activity_columns
    FROM information_schema.columns 
    WHERE table_schema = 'sigma' 
        AND table_name = 'activity'
        AND column_name IN ('start_time', 'end_time', 'duration_hours', 'rubric_id', 'priority');
    
    IF v_activity_columns < 5 THEN
        RAISE EXCEPTION '❌ ERROR: Columnas no agregadas a activity (encontradas: %)', v_activity_columns;
    END IF;
    
    -- Verificar índices
    SELECT COUNT(*) INTO v_indexes
    FROM pg_indexes 
    WHERE schemaname = 'sigma' 
        AND tablename = 'activity'
        AND indexname LIKE 'idx_activity_%';
    
    RAISE NOTICE '=================================================================';
    RAISE NOTICE '✓ MIGRACIÓN HU-011 COMPLETADA EXITOSAMENTE';
    RAISE NOTICE '=================================================================';
    RAISE NOTICE 'Tabla rubric: CREADA';
    RAISE NOTICE 'Columnas activity: % agregadas', v_activity_columns;
    RAISE NOTICE 'Índices creados: %', v_indexes;
    RAISE NOTICE 'Vista v_activity_schedule: CREADA';
    RAISE NOTICE 'Función check_activity_time_conflict: CREADA';
    RAISE NOTICE '=================================================================';
END $$;

-- ============================================================================
-- FIN DEL SCRIPT
-- ============================================================================

