-- ============================================================================
-- HU-011: Rollback - Revertir migración de rúbricas y horarios
-- USAR CON PRECAUCIÓN: Este script elimina datos
-- Fecha: 2025-11-07
-- ============================================================================

-- Cambiar al schema sigma
SET search_path TO sigma;

RAISE NOTICE '⚠️  INICIANDO ROLLBACK HU-011...';

-- ============================================================================
-- 1. ELIMINAR VISTA
-- ============================================================================

DROP VIEW IF EXISTS v_activity_schedule CASCADE;
RAISE NOTICE '✓ Vista v_activity_schedule eliminada';

-- ============================================================================
-- 2. ELIMINAR FUNCIÓN
-- ============================================================================

DROP FUNCTION IF EXISTS check_activity_time_conflict(VARCHAR, DATE, TIME, TIME, INTEGER) CASCADE;
RAISE NOTICE '✓ Función check_activity_time_conflict eliminada';

-- ============================================================================
-- 3. ELIMINAR TABLA DE CONFLICTOS
-- ============================================================================

DROP TABLE IF EXISTS activity_schedule_conflict CASCADE;
RAISE NOTICE '✓ Tabla activity_schedule_conflict eliminada';

-- ============================================================================
-- 4. ELIMINAR COLUMNAS DE ACTIVITY
-- ============================================================================

-- Eliminar foreign key primero
ALTER TABLE activity DROP CONSTRAINT IF EXISTS fk_activity_rubric;

-- Eliminar índices
DROP INDEX IF EXISTS idx_activity_start_time;
DROP INDEX IF EXISTS idx_activity_end_time;
DROP INDEX IF EXISTS idx_activity_finish_date;
DROP INDEX IF EXISTS idx_activity_rubric;
DROP INDEX IF EXISTS idx_activity_priority;
DROP INDEX IF EXISTS idx_activity_schedule;

RAISE NOTICE '✓ Índices de activity eliminados';

-- Eliminar columnas
ALTER TABLE activity DROP COLUMN IF EXISTS start_time;
ALTER TABLE activity DROP COLUMN IF EXISTS end_time;
ALTER TABLE activity DROP COLUMN IF EXISTS duration_hours;
ALTER TABLE activity DROP COLUMN IF EXISTS recurrence;
ALTER TABLE activity DROP COLUMN IF EXISTS priority;
ALTER TABLE activity DROP COLUMN IF EXISTS rubric_id;

RAISE NOTICE '✓ Columnas de horarios y rúbrica eliminadas de activity';

-- ============================================================================
-- 5. ELIMINAR TABLA RUBRIC
-- ============================================================================

-- Eliminar trigger primero
DROP TRIGGER IF EXISTS trigger_rubric_updated_at ON rubric;
DROP FUNCTION IF EXISTS update_rubric_updated_at() CASCADE;

-- Eliminar índices
DROP INDEX IF EXISTS idx_rubric_created_by;
DROP INDEX IF EXISTS idx_rubric_name;

-- Eliminar tabla
DROP TABLE IF EXISTS rubric CASCADE;

RAISE NOTICE '✓ Tabla rubric eliminada';

-- ============================================================================
-- 6. VERIFICACIÓN POST-ROLLBACK
-- ============================================================================

DO $$
DECLARE
    v_rubric_exists BOOLEAN;
    v_columns_exist INTEGER;
BEGIN
    -- Verificar que rubric no existe
    SELECT EXISTS (
        SELECT 1 FROM information_schema.tables 
        WHERE table_schema = 'sigma' AND table_name = 'rubric'
    ) INTO v_rubric_exists;
    
    -- Verificar que columnas fueron eliminadas
    SELECT COUNT(*) INTO v_columns_exist
    FROM information_schema.columns 
    WHERE table_schema = 'sigma' 
        AND table_name = 'activity'
        AND column_name IN ('start_time', 'end_time', 'duration_hours', 'rubric_id', 'priority');
    
    IF v_rubric_exists THEN
        RAISE EXCEPTION '❌ ERROR: Tabla rubric aún existe';
    END IF;
    
    IF v_columns_exist > 0 THEN
        RAISE EXCEPTION '❌ ERROR: Columnas aún existen en activity (encontradas: %)', v_columns_exist;
    END IF;
    
    RAISE NOTICE '=================================================================';
    RAISE NOTICE '✓ ROLLBACK HU-011 COMPLETADO EXITOSAMENTE';
    RAISE NOTICE '=================================================================';
    RAISE NOTICE 'Tabla rubric: ELIMINADA';
    RAISE NOTICE 'Columnas activity: ELIMINADAS';
    RAISE NOTICE 'Índices: ELIMINADOS';
    RAISE NOTICE 'Vista y funciones: ELIMINADAS';
    RAISE NOTICE '=================================================================';
END $$;

-- ============================================================================
-- FIN DEL ROLLBACK
-- ============================================================================

