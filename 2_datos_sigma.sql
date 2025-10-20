-- ================================================
-- PASO 2: USUARIOS PARA SIGMA_DB
-- ================================================
-- Ejecuta este script en la base de datos: sigma_db

-- Tabla: professor (Profesores)
-- Los profesores también deben existir en sigma_db para poder crear monitorías
INSERT INTO professor (id, name, password) VALUES
('1001', 'Dr. Roberto Castillo', 'prof123'),
('1002', 'Dra. Patricia Méndez', 'prof123'),
('1003', 'Dr. Fernando Ríos', 'prof123'),
('1004', 'Dra. Isabel Vargas', 'prof123')
ON CONFLICT (id) DO NOTHING;

-- Tabla: department_head (Jefe de Departamento)
INSERT INTO department_head (id, name, password) VALUES
('5001', 'Dr. Alejandro Ramírez', 'jefe123')
ON CONFLICT (id) DO NOTHING;

-- Tabla: prospect (Estudiantes)
-- Los estudiantes también deben existir en sigma_db para poder postularse
INSERT INTO prospect (id, code, name, last_name, email, password, semester, grade_average, grade_course) VALUES
('2220001', '2220001', 'Juan', 'Pérez García', 'juan.perez@ejemplo.com', '123456', 5, 4.2, 4.5),
('2220002', '2220002', 'María', 'López Martínez', 'maria.lopez@ejemplo.com', '123456', 6, 4.5, 4.8),
('2220003', '2220003', 'Carlos', 'Rodríguez Silva', 'carlos.rodriguez@ejemplo.com', '123456', 4, 4.0, 4.2),
('2220004', '2220004', 'Ana', 'González Torres', 'ana.gonzalez@ejemplo.com', '123456', 7, 4.7, 4.9),
('2220005', '2220005', 'Luis', 'Sánchez Morales', 'luis.sanchez@ejemplo.com', '123456', 5, 4.3, 4.4)
ON CONFLICT (id) DO NOTHING;

-- ================================================
-- MENSAJE DE CONFIRMACIÓN
-- ================================================
SELECT '✅ ¡Usuarios de prueba creados exitosamente!' as mensaje;
SELECT '' as espacio;
SELECT '========================================' as separador;
SELECT 'CREDENCIALES PARA PROBAR LA APLICACIÓN' as titulo;
SELECT '========================================' as separador;
SELECT '' as espacio;
SELECT 'ESTUDIANTES (pueden postularse como monitores):' as tipo;
SELECT '  Usuario: 2220001 | Contraseña: 123456' as credencial1;
SELECT '  Usuario: 2220002 | Contraseña: 123456' as credencial2;
SELECT '  Usuario: 2220003 | Contraseña: 123456' as credencial3;
SELECT '  Usuario: 2220004 | Contraseña: 123456' as credencial4;
SELECT '  Usuario: 2220005 | Contraseña: 123456' as credencial5;
SELECT '' as espacio2;
SELECT 'PROFESORES (pueden crear monitorías):' as tipo2;
SELECT '  Usuario: 1001 | Contraseña: prof123' as credencial6;
SELECT '  Usuario: 1002 | Contraseña: prof123' as credencial7;
SELECT '  Usuario: 1003 | Contraseña: prof123' as credencial8;
SELECT '  Usuario: 1004 | Contraseña: prof123' as credencial9;
SELECT '' as espacio3;
SELECT 'JEFE DE DEPARTAMENTO (acceso completo):' as tipo3;
SELECT '  Usuario: 5001 | Contraseña: jefe123' as credencial10;
SELECT '' as espacio4;
SELECT '========================================' as separador2;
SELECT '¡Ya puedes iniciar sesión en la aplicación!' as final;

