-- ================================================
-- PASO 1: USUARIOS PARA BANNER_DB
-- ================================================
-- Ejecuta este script en la base de datos: banner_db

-- Tabla: prospect (Estudiantes/Aspirantes)
-- Estos estudiantes podrán postularse como monitores
INSERT INTO prospect (id, code, name, last_name, email, password, semester, grade_average, grade_course) VALUES
('2220001', '2220001', 'Juan', 'Pérez García', 'juan.perez@ejemplo.com', '123456', 5, 4.2, 4.5),
('2220002', '2220002', 'María', 'López Martínez', 'maria.lopez@ejemplo.com', '123456', 6, 4.5, 4.8),
('2220003', '2220003', 'Carlos', 'Rodríguez Silva', 'carlos.rodriguez@ejemplo.com', '123456', 4, 4.0, 4.2),
('2220004', '2220004', 'Ana', 'González Torres', 'ana.gonzalez@ejemplo.com', '123456', 7, 4.7, 4.9),
('2220005', '2220005', 'Luis', 'Sánchez Morales', 'luis.sanchez@ejemplo.com', '123456', 5, 4.3, 4.4)
ON CONFLICT (id) DO NOTHING;

-- Tabla: professor (Profesores)
-- Estos profesores pueden crear y gestionar monitorías
INSERT INTO professor (id, name, password) VALUES
('1001', 'Dr. Roberto Castillo', 'prof123'),
('1002', 'Dra. Patricia Méndez', 'prof123'),
('1003', 'Dr. Fernando Ríos', 'prof123'),
('1004', 'Dra. Isabel Vargas', 'prof123')
ON CONFLICT (id) DO NOTHING;

-- Tabla: department_head (Jefe de Departamento)
-- Puede ver y aprobar todas las monitorías
INSERT INTO department_head (id, name, password) VALUES
('5001', 'Dr. Alejandro Ramírez', 'jefe123')
ON CONFLICT (id) DO NOTHING;

-- Mensaje de confirmación
SELECT '✅ Usuarios creados en banner_db correctamente!' as resultado;
SELECT 'Ahora ejecuta el archivo: 2_datos_sigma.sql en la base de datos sigma_db' as siguiente_paso;

