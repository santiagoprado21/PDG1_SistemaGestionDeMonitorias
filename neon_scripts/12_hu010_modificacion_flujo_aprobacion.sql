-- ================================================
-- MIGRACION HU-010: MODIFICACION DE FLUJO DE APROBACION
-- ================================================
-- El jefe de departamento ahora debe aprobar ANTES de las postulaciones
-- en lugar de aprobar DESPUES de que se selecciona el monitor
-- ================================================
-- EJECUTAR EN: SQL Editor de Neon (https://console.neon.tech)
-- ================================================

-- 1. Agregar nuevos campos a la tabla monitoring_request
ALTER TABLE sigma.monitoring_request 
ADD COLUMN IF NOT EXISTS approved_by_head VARCHAR(20),
ADD COLUMN IF NOT EXISTS head_comment TEXT,
ADD COLUMN IF NOT EXISTS head_approval_date TIMESTAMP;

-- 2. Modificar el tamanio de la columna status para permitir el nuevo estado mas largo
ALTER TABLE sigma.monitoring_request 
ALTER COLUMN status TYPE VARCHAR(35);

-- 3. ACTUALIZAR CHECK CONSTRAINT para incluir el nuevo estado
-- Primero eliminar el constraint existente
ALTER TABLE sigma.monitoring_request 
DROP CONSTRAINT IF EXISTS monitoring_request_status_check;

-- Crear el constraint actualizado con el nuevo estado
ALTER TABLE sigma.monitoring_request 
ADD CONSTRAINT monitoring_request_status_check 
CHECK (status IN (
    'PENDIENTE_APROBACION_JEFE',  -- NUEVO ESTADO
    'CONVOCATORIA_ABIERTA',
    'MONITOR_SELECCIONADO',
    'PENDIENTE_APROBACION',
    'APROBADA',
    'RECHAZADA',
    'CANCELADA'
));

-- 4. Comentarios explicativos
COMMENT ON COLUMN sigma.monitoring_request.approved_by_head IS 
'ID del jefe de departamento que aprobo o rechazo la convocatoria';

COMMENT ON COLUMN sigma.monitoring_request.head_comment IS 
'Comentario del jefe al aprobar, rechazar o modificar la convocatoria';

COMMENT ON COLUMN sigma.monitoring_request.head_approval_date IS 
'Fecha y hora en que el jefe aprobo o rechazo la convocatoria';

-- 5. Actualizar convocatorias existentes que estan CONVOCATORIA_ABIERTA 
-- para que mantengan su estado (ya fueron aprobadas implicitamente)
-- Las que estan en otros estados no se tocan

-- No necesitamos cambiar el estado de las existentes, el codigo manejara ambos flujos

-- 6. Verificar que los cambios se aplicaron correctamente
SELECT column_name, data_type, character_maximum_length 
FROM information_schema.columns 
WHERE table_schema = 'sigma' 
  AND table_name = 'monitoring_request'
  AND column_name IN ('status', 'approved_by_head', 'head_comment', 'head_approval_date')
ORDER BY column_name;

-- 7. Verificar que el constraint se actualizo
SELECT constraint_name, check_clause
FROM information_schema.check_constraints
WHERE constraint_schema = 'sigma'
  AND constraint_name = 'monitoring_request_status_check';

-- 8. Mensaje de confirmacion
SELECT 'Migracion completada exitosamente!' as resultado;
SELECT 'Ahora el flujo es: Profesor crea -> Jefe aprueba -> Estudiantes postulan -> Profesor selecciona' as nuevo_flujo;

-- ================================================
-- NOTAS IMPORTANTES:
-- ================================================
-- 1. NUEVO ESTADO: PENDIENTE_APROBACION_JEFE
--    - Las nuevas convocatorias creadas por profesores iniciaran en este estado
--    - El jefe debe aprobar ANTES de que se abran las postulaciones
--
-- 2. NUEVO FLUJO:
--    Profesor crea convocatoria -> PENDIENTE_APROBACION_JEFE
--    Jefe revisa/modifica/aprueba -> CONVOCATORIA_ABIERTA
--    Estudiantes postulan -> (sigue igual)
--    Profesor selecciona monitor -> MONITOR_SELECCIONADO
--    Sistema crea Monitoring -> APROBADA (ya no necesita segunda aprobacion)
--
-- 3. COMPATIBILIDAD:
--    - Las convocatorias existentes siguen funcionando normalmente
--    - El backend detecta automaticamente si es flujo antiguo o nuevo
--
-- 4. BACKEND:
--    - Ya esta actualizado con los nuevos endpoints
--    - /monitoring-request/pending-head-approval/{departmentHeadId}
--    - /monitoring-request/{id}/approve-by-head
--    - /monitoring-request/{id}/reject-by-head
--    - /monitoring-request/{id}/modify-by-head
--
-- 5. FRONTEND:
--    - AprobarMonitoriasHU010.js ya esta actualizado
--    - Muestra convocatorias en estado PENDIENTE_APROBACION_JEFE
--    - Permite aprobar, modificar o rechazar
-- ================================================
