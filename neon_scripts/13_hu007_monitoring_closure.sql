-- ================================================
-- HU-007: Cierre de Monitorías al Final del Semestre
-- ================================================
-- Fecha: 2026-01-21
-- Descripción: Agrega funcionalidad para cerrar monitorías
--              y generar reportes de cumplimiento
-- ================================================

-- 1. Modificar CHECK constraint para agregar estado CERRADA
-- Nota: approval_status es VARCHAR(30) con CHECK constraint, no un enum

-- Primero, eliminar el constraint existente
ALTER TABLE sigma.monitoring 
DROP CONSTRAINT IF EXISTS monitoring_approval_status_check;

-- Crear nuevo constraint con el estado adicional CERRADA
ALTER TABLE sigma.monitoring 
ADD CONSTRAINT monitoring_approval_status_check 
CHECK (approval_status IN ('PENDIENTE_APROBACION', 'APROBADA', 'CERRADA', 'RECHAZADA'));

-- 2. Agregar columnas de auditoría de cierre
ALTER TABLE sigma.monitoring
ADD COLUMN IF NOT EXISTS closed_by VARCHAR(20),
ADD COLUMN IF NOT EXISTS closure_comment TEXT,
ADD COLUMN IF NOT EXISTS closure_date TIMESTAMP;

COMMENT ON COLUMN sigma.monitoring.closed_by IS 'ID del director que cerró la monitoría';
COMMENT ON COLUMN sigma.monitoring.closure_comment IS 'Comentario del director sobre el cierre';
COMMENT ON COLUMN sigma.monitoring.closure_date IS 'Fecha y hora del cierre';

-- 3. Agregar columnas de métricas de cumplimiento
ALTER TABLE sigma.monitoring
ADD COLUMN IF NOT EXISTS compliance_percentage INTEGER,
ADD COLUMN IF NOT EXISTS completed_activities INTEGER,
ADD COLUMN IF NOT EXISTS total_activities INTEGER,
ADD COLUMN IF NOT EXISTS actual_hours INTEGER;

COMMENT ON COLUMN sigma.monitoring.compliance_percentage IS 'Porcentaje de cumplimiento (0-100)';
COMMENT ON COLUMN sigma.monitoring.completed_activities IS 'Total de actividades completadas';
COMMENT ON COLUMN sigma.monitoring.total_activities IS 'Total de actividades planificadas';
COMMENT ON COLUMN sigma.monitoring.actual_hours IS 'Horas trabajadas reales';

-- 4. Agregar índices para mejorar rendimiento de consultas
CREATE INDEX IF NOT EXISTS idx_monitoring_approval_status 
ON sigma.monitoring(approval_status);

CREATE INDEX IF NOT EXISTS idx_monitoring_semester 
ON sigma.monitoring(semester);

CREATE INDEX IF NOT EXISTS idx_monitoring_semester_status 
ON sigma.monitoring(semester, approval_status);

CREATE INDEX IF NOT EXISTS idx_monitoring_closure_date 
ON sigma.monitoring(closure_date);

-- ================================================
-- VALIDACIONES Y RESTRICCIONES
-- ================================================

-- Las monitorías cerradas no deben poder cambiar de estado
-- Esta lógica se maneja en el código Java, pero agregamos un comentario
COMMENT ON COLUMN sigma.monitoring.approval_status IS 
'Estado de la monitoría. Las monitorías en estado CERRADA no pueden modificarse.';

-- ================================================
-- DATOS DE EJEMPLO (OPCIONAL)
-- ================================================
-- Puedes descomentar esto para probar con datos de ejemplo:

/*
-- Ejemplo: Cerrar una monitoría de prueba
UPDATE sigma.monitoring 
SET approval_status = 'CERRADA',
    closed_by = 'admin',
    closure_comment = 'Cierre de prueba del semestre 2025-2',
    closure_date = NOW(),
    compliance_percentage = 85,
    completed_activities = 17,
    total_activities = 20,
    actual_hours = 40
WHERE id = 1 
AND approval_status = 'APROBADA';
*/

-- ================================================
-- VERIFICACIÓN
-- ================================================

-- Verificar que las columnas se agregaron correctamente
SELECT column_name, data_type, is_nullable
FROM information_schema.columns
WHERE table_schema = 'sigma'
  AND table_name = 'monitoring'
  AND column_name IN ('closed_by', 'closure_comment', 'closure_date', 
                      'compliance_percentage', 'completed_activities', 
                      'total_activities', 'actual_hours')
ORDER BY column_name;

-- Verificar el CHECK constraint actualizado
SELECT 
    con.conname AS constraint_name,
    pg_get_constraintdef(con.oid) AS constraint_definition
FROM pg_constraint con
JOIN pg_class rel ON rel.oid = con.conrelid
JOIN pg_namespace nsp ON nsp.oid = rel.relnamespace
WHERE nsp.nspname = 'sigma'
  AND rel.relname = 'monitoring'
  AND con.conname = 'monitoring_approval_status_check';

-- Verificar índices
SELECT indexname, indexdef
FROM pg_indexes
WHERE schemaname = 'sigma'
  AND tablename = 'monitoring'
  AND indexname LIKE 'idx_monitoring%'
ORDER BY indexname;

-- ================================================
-- ROLLBACK (SI ES NECESARIO)
-- ================================================
-- ADVERTENCIA: Ejecutar esto eliminará las columnas y datos de cierre

/*
-- Eliminar índices
DROP INDEX IF EXISTS sigma.idx_monitoring_approval_status;
DROP INDEX IF EXISTS sigma.idx_monitoring_semester;
DROP INDEX IF EXISTS sigma.idx_monitoring_semester_status;
DROP INDEX IF EXISTS sigma.idx_monitoring_closure_date;

-- Eliminar columnas
ALTER TABLE sigma.monitoring
DROP COLUMN IF EXISTS closed_by,
DROP COLUMN IF EXISTS closure_comment,
DROP COLUMN IF EXISTS closure_date,
DROP COLUMN IF EXISTS compliance_percentage,
DROP COLUMN IF EXISTS completed_activities,
DROP COLUMN IF EXISTS total_activities,
DROP COLUMN IF EXISTS actual_hours;

-- Restaurar constraint original (sin CERRADA)
ALTER TABLE sigma.monitoring 
DROP CONSTRAINT IF EXISTS monitoring_approval_status_check;

ALTER TABLE sigma.monitoring 
ADD CONSTRAINT monitoring_approval_status_check 
CHECK (approval_status IN ('PENDIENTE_APROBACION', 'APROBADA', 'RECHAZADA'));
*/

COMMENT ON TABLE sigma.monitoring IS 'Tabla de monitorías oficiales. HU-007: Soporta cierre al final del semestre con métricas de cumplimiento.';
