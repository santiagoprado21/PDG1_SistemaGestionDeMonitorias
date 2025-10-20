-- ================================================
-- DATOS ACADÉMICOS DE PRUEBA PARA SIGMA_DB
-- ================================================
-- Ejecuta este script en la base de datos: sigma_db
-- DESPUÉS de ejecutar 3_datos_academicos.sql en banner_db

-- ================================================
-- ESCUELAS/FACULTADES (school)
-- ================================================
INSERT INTO school (id, name) VALUES
(1, 'Facultad de Ingeniería'),
(2, 'Facultad de Ciencias'),
(3, 'Facultad de Humanidades')
ON CONFLICT (id) DO NOTHING;

-- ================================================
-- PROGRAMAS (program)
-- ================================================
INSERT INTO program (id, name, school_id) VALUES
(1, 'Ingeniería de Sistemas', 1),
(2, 'Ingeniería Industrial', 1),
(3, 'Ingeniería Electrónica', 1),
(4, 'Matemáticas', 2),
(5, 'Física', 2),
(6, 'Psicología', 3)
ON CONFLICT (id) DO NOTHING;

-- ================================================
-- CURSOS (course)
-- ================================================
-- Cursos de Ingeniería de Sistemas
INSERT INTO course (id, name, program_id) VALUES
(1, 'Programación I', 1),
(2, 'Estructuras de Datos', 1),
(3, 'Bases de Datos', 1),
(4, 'Desarrollo Web', 1),
(5, 'Inteligencia Artificial', 1),

-- Cursos de Ingeniería Industrial
(6, 'Estadística', 2),
(7, 'Investigación de Operaciones', 2),
(8, 'Gestión de Proyectos', 2),

-- Cursos de Ingeniería Electrónica
(9, 'Circuitos Eléctricos', 3),
(10, 'Sistemas Digitales', 3),

-- Cursos de Matemáticas
(11, 'Cálculo Diferencial', 4),
(12, 'Álgebra Lineal', 4),

-- Cursos de Física
(13, 'Física Mecánica', 5),
(14, 'Electromagnetismo', 5),

-- Cursos de Psicología
(15, 'Psicología General', 6),
(16, 'Psicología Social', 6)
ON CONFLICT (id) DO NOTHING;

-- ================================================
-- RELACIONES PROFESOR-CURSO (course_professor)
-- ================================================
-- Asignar cursos al Profesor 1001 (Dr. Roberto Castillo)
INSERT INTO course_professor (id, professor_id, course_id) VALUES
(1, '1001', 1),  -- Programación I
(2, '1001', 2),  -- Estructuras de Datos
(3, '1001', 4)   -- Desarrollo Web
ON CONFLICT (id) DO NOTHING;

-- Asignar cursos al Profesor 1002 (Dra. Patricia Méndez)
INSERT INTO course_professor (id, professor_id, course_id) VALUES
(4, '1002', 3),  -- Bases de Datos
(5, '1002', 5)   -- Inteligencia Artificial
ON CONFLICT (id) DO NOTHING;

-- Asignar cursos al Profesor 1003 (Dr. Fernando Ríos)
INSERT INTO course_professor (id, professor_id, course_id) VALUES
(6, '1003', 6),  -- Estadística
(7, '1003', 7)   -- Investigación de Operaciones
ON CONFLICT (id) DO NOTHING;

-- Asignar cursos al Profesor 1004 (Dra. Isabel Vargas)
INSERT INTO course_professor (id, professor_id, course_id) VALUES
(8, '1004', 11), -- Cálculo Diferencial
(9, '1004', 12)  -- Álgebra Lineal
ON CONFLICT (id) DO NOTHING;

-- ================================================
-- RELACIÓN JEFE-PROGRAMA (head_professor)
-- ================================================
-- Asignar programas al Jefe de Departamento 5001
INSERT INTO head_professor (id, department_head_id, program_id) VALUES
(1, '5001', 1),  -- Ingeniería de Sistemas
(2, '5001', 2),  -- Ingeniería Industrial
(3, '5001', 3)   -- Ingeniería Electrónica
ON CONFLICT (id) DO NOTHING;

-- ================================================
-- Mensaje de confirmación
-- ================================================
SELECT '✅ Datos académicos creados en sigma_db!' as resultado;
SELECT '' as espacio;
SELECT '📊 RESUMEN DE DATOS CREADOS:' as titulo;
SELECT '' as espacio2;
SELECT 'Facultades: ' || (SELECT COUNT(*) FROM school) as total_facultades;
SELECT 'Programas: ' || (SELECT COUNT(*) FROM program) as total_programas;
SELECT 'Cursos: ' || (SELECT COUNT(*) FROM course) as total_cursos;
SELECT 'Profesores con cursos: ' || (SELECT COUNT(DISTINCT professor_id) FROM course_professor) as profesores_asignados;
SELECT '' as espacio3;
SELECT '🎓 CURSOS ASIGNADOS POR PROFESOR:' as seccion;
SELECT 'Dr. Roberto Castillo (1001): Programación I, Estructuras de Datos, Desarrollo Web' as profesor1;
SELECT 'Dra. Patricia Méndez (1002): Bases de Datos, Inteligencia Artificial' as profesor2;
SELECT 'Dr. Fernando Ríos (1003): Estadística, Investigación de Operaciones' as profesor3;
SELECT 'Dra. Isabel Vargas (1004): Cálculo Diferencial, Álgebra Lineal' as profesor4;
SELECT '' as espacio4;
SELECT '👔 Jefe de Departamento (5001) gestiona:' as jefe_info;
SELECT '   - Ingeniería de Sistemas' as prog1;
SELECT '   - Ingeniería Industrial' as prog2;
SELECT '   - Ingeniería Electrónica' as prog3;

