-- ================================================================
-- HU-010: Script de ROLLBACK para revertir cambios
-- ================================================================
-- ADVERTENCIA: Este script elimina las tablas y columnas creadas
-- para HU-010. Usar solo si es necesario revertir la migración.
-- ================================================================

SET search_path TO sigma;

-- ================================================================
-- PASO 1: Eliminar triggers
-- ================================================================
DROP TRIGGER IF EXISTS update_monitoring_request_updated_at ON monitoring_request;
DROP TRIGGER IF EXISTS update_monitor_application_updated_at ON monitor_application;

-- ================================================================
-- PASO 2: Eliminar tabla monitor_application (tiene FK a monitoring_request)
-- ================================================================
DROP TABLE IF EXISTS monitor_application CASCADE;

-- ================================================================
-- PASO 3: Eliminar columnas agregadas a monitoring
-- ================================================================
ALTER TABLE monitoring DROP COLUMN IF EXISTS approval_date;
ALTER TABLE monitoring DROP COLUMN IF EXISTS approval_comment;
ALTER TABLE monitoring DROP COLUMN IF EXISTS approved_by;
ALTER TABLE monitoring DROP COLUMN IF EXISTS justification;
ALTER TABLE monitoring DROP COLUMN IF EXISTS approval_status;
ALTER TABLE monitoring DROP COLUMN IF EXISTS assigned_monitor_id;
ALTER TABLE monitoring DROP COLUMN IF EXISTS monitoring_request_id;

-- Eliminar índices relacionados
DROP INDEX IF EXISTS idx_monitoring_request_id;
DROP INDEX IF EXISTS idx_monitoring_assigned_monitor;
DROP INDEX IF EXISTS idx_monitoring_approval_status;

-- ================================================================
-- PASO 4: Eliminar tabla monitoring_request
-- ================================================================
DROP TABLE IF EXISTS monitoring_request CASCADE;

-- ================================================================
-- PASO 5: Eliminar función del trigger (si no se usa en otro lado)
-- ================================================================
-- NOTA: No eliminamos update_updated_at_column() porque puede
-- estar siendo usada por otras tablas del sistema.
-- Si estás seguro de que no se usa, descomenta la siguiente línea:
-- DROP FUNCTION IF EXISTS update_updated_at_column();

-- ================================================================
-- VERIFICACIÓN
-- ================================================================
DO $$
BEGIN
    RAISE NOTICE 'Verificando rollback...';
    
    IF NOT EXISTS (SELECT 1 FROM information_schema.tables 
                   WHERE table_schema = 'sigma' AND table_name = 'monitoring_request') THEN
        RAISE NOTICE '✓ Tabla monitoring_request eliminada';
    ELSE
        RAISE WARNING '✗ Tabla monitoring_request aún existe';
    END IF;
    
    IF NOT EXISTS (SELECT 1 FROM information_schema.tables 
                   WHERE table_schema = 'sigma' AND table_name = 'monitor_application') THEN
        RAISE NOTICE '✓ Tabla monitor_application eliminada';
    ELSE
        RAISE WARNING '✗ Tabla monitor_application aún existe';
    END IF;
    
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns 
                   WHERE table_schema = 'sigma' 
                   AND table_name = 'monitoring' 
                   AND column_name = 'monitoring_request_id') THEN
        RAISE NOTICE '✓ Columnas de monitoring eliminadas';
    ELSE
        RAISE WARNING '✗ Algunas columnas de monitoring aún existen';
    END IF;
    
    RAISE NOTICE 'Rollback completado!';
END $$;

