-- HU-498: Agregar estado ANULADA al constraint de approval_status en monitoring
-- El soft-delete cambia el estado a ANULADA en lugar de borrar el registro de la BD.

ALTER TABLE sigma.monitoring
DROP CONSTRAINT IF EXISTS monitoring_approval_status_check;

ALTER TABLE sigma.monitoring
ADD CONSTRAINT monitoring_approval_status_check
CHECK (approval_status IN ('PENDIENTE_APROBACION', 'APROBADA', 'CERRADA', 'RECHAZADA', 'ANULADA'));

-- Verificar que el constraint quedó bien
SELECT con.conname, pg_get_constraintdef(con.oid)
FROM pg_constraint con
JOIN pg_class rel ON rel.oid = con.conrelid
JOIN pg_namespace nsp ON nsp.oid = rel.relnamespace
WHERE nsp.nspname = 'sigma'
  AND rel.relname = 'monitoring'
  AND con.conname = 'monitoring_approval_status_check';
