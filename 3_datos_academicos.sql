-- ================================================
-- DATOS ACADÉMICOS DE PRUEBA
-- ================================================
-- Ejecuta este script en la base de datos: banner_db
-- Luego ejecuta 4_datos_academicos_sigma.sql en sigma_db

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
-- ESTUDIANTES EN CURSOS (student_course)
-- ================================================
-- Inscribir estudiantes en algunos cursos
INSERT INTO student_course (id, student_id, course_id) VALUES
-- Estudiante 2220001
(1, '2220001', 1),
(2, '2220001', 3),
(3, '2220001', 6),

-- Estudiante 2220002
(4, '2220002', 2),
(5, '2220002', 4),
(6, '2220002', 7),

-- Estudiante 2220003
(7, '2220003', 1),
(8, '2220003', 2),
(9, '2220003', 3),

-- Estudiante 2220004
(10, '2220004', 11),
(11, '2220004', 12),

-- Estudiante 2220005
(12, '2220005', 4),
(13, '2220005', 5)
ON CONFLICT (id) DO NOTHING;

-- ================================================
-- Mensaje de confirmación
-- ================================================
SELECT '✅ Datos académicos creados en banner_db!' as resultado;
SELECT 'Total de escuelas: ' || COUNT(*) FROM school;
SELECT 'Total de programas: ' || COUNT(*) FROM program;
SELECT 'Total de cursos: ' || COUNT(*) FROM course;
SELECT 'Total de relaciones profesor-curso: ' || COUNT(*) FROM course_professor;

