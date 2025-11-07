-- ============================================================================
-- Script: 11_fix_char_to_varchar.sql
-- Propósito: Convertir columnas CHAR(1) a VARCHAR(1) para compatibilidad con Hibernate
-- Fecha: 2025-11-06
-- ============================================================================

-- Asegurar que estamos en el esquema correcto
SET search_path TO sigma;

-- Convertir role_creator y role_responsable de CHAR(1) a VARCHAR(1)
ALTER TABLE sigma.activity 
    ALTER COLUMN role_creator TYPE VARCHAR(1);

ALTER TABLE sigma.activity 
    ALTER COLUMN role_responsable TYPE VARCHAR(1);

DO $$
BEGIN
    RAISE NOTICE '✓ Columnas role_creator y role_responsable convertidas a VARCHAR(1)';
END $$;

