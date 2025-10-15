-- Script para crear las bases de datos necesarias
-- Ejecuta este script en PostgreSQL (pgAdmin o psql)

CREATE DATABASE sigma_db
    WITH 
    OWNER = postgres
    ENCODING = 'UTF8';

CREATE DATABASE banner_db
    WITH 
    OWNER = postgres
    ENCODING = 'UTF8';

-- Verificar que se crearon correctamente
SELECT 'Bases de datos creadas exitosamente!' as mensaje;

