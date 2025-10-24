-- ================================================
-- CREAR SCHEMAS EN NEON POSTGRESQL
-- ================================================
-- En Neon, es mejor usar SCHEMAS en lugar de múltiples bases de datos
-- Esto facilita la gestión y es más eficiente
--
-- EJECUTAR EN: SQL Editor de Neon (https://console.neon.tech)
-- ================================================

-- Crear schema para BANNER (sistema externo simulado)
CREATE SCHEMA IF NOT EXISTS banner;

-- Crear schema para SIGMA (aplicación principal)
CREATE SCHEMA IF NOT EXISTS sigma;

-- Verificar que se crearon correctamente
SELECT schema_name 
FROM information_schema.schemata 
WHERE schema_name IN ('banner', 'sigma');

-- Establecer search_path para facilitar consultas futuras
-- (Puedes quitar esto si prefieres especificar el schema en cada query)
-- ALTER DATABASE neondb SET search_path TO sigma, banner, public;

-- Mensaje de confirmación
SELECT '✅ Schemas "banner" y "sigma" creados exitosamente!' as resultado;
SELECT 'Ahora ejecuta: 2_datos_banner.sql' as siguiente_paso;

-- ================================================
-- NOTAS IMPORTANTES:
-- ================================================
-- 1. El schema "banner" contendrá las tablas del sistema externo
-- 2. El schema "sigma" contendrá las tablas de la aplicación principal
-- 3. En tu código Java, deberás especificar el schema en las entidades:
--    @Table(name = "nombre_tabla", schema = "sigma")
-- 4. O configurar el default schema en application-cloud.properties:
--    spring.jpa.properties.hibernate.default_schema=sigma
-- ================================================

