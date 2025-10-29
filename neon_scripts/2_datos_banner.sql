-- ================================================
-- DATOS PARA SCHEMA BANNER EN NEON
-- ================================================
-- EJECUTAR EN: SQL Editor de Neon
-- IMPORTANTE: Ejecuta primero el script 1_create_schemas.sql
-- ================================================

-- Asegurarnos de estar en el schema correcto
SET search_path TO banner, public;

-- ================================================
-- ESCUELAS/FACULTADES (school)
-- ================================================
CREATE TABLE IF NOT EXISTS banner.school (
    id BIGINT PRIMARY KEY,
    name VARCHAR(255) NOT NULL
);

INSERT INTO banner.school (id, name) VALUES
(1, 'Facultad de Ingeniería'),
(2, 'Facultad de Ciencias'),
(3, 'Facultad de Humanidades')
ON CONFLICT (id) DO NOTHING;

-- ================================================
-- PROGRAMAS (program)
-- ================================================
CREATE TABLE IF NOT EXISTS banner.program (
    id BIGINT PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    school_id BIGINT,
    FOREIGN KEY (school_id) REFERENCES banner.school(id)
);

INSERT INTO banner.program (id, name, school_id) VALUES
(1, 'Ingeniería de Sistemas', 1),
(2, 'Ingeniería Industrial', 1),
(3, 'Ingeniería Bioquímica', 1),
(4, 'Matemáticas', 2),
(5, 'Física', 2),
(6, 'Psicología', 3)
ON CONFLICT (id) DO NOTHING;

-- ================================================
-- CURSOS (course)
-- ================================================
CREATE TABLE IF NOT EXISTS banner.course (
    id BIGINT PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    program_id BIGINT,
    FOREIGN KEY (program_id) REFERENCES banner.program(id)
);

INSERT INTO banner.course (id, name, program_id) VALUES
-- Cursos de Ingeniería de Sistemas
(1, 'Programación I', 1),
(2, 'Estructuras de Datos', 1),
(3, 'Bases de Datos', 1),
(4, 'Desarrollo Web', 1),
(5, 'Inteligencia Artificial', 1),

-- Cursos de Ingeniería Industrial
(6, 'Estadística', 2),
(7, 'Investigación de Operaciones', 2),
(8, 'Gestión de Proyectos', 2),

-- Cursos de Ingeniería Bioquímica
(9, 'Diseño experimental', 3),
(10, 'Química', 3),

-- Cursos de Matemáticas
(11, 'Cálculo de una variable', 4),
(12, 'Álgebra Lineal', 4),

-- Cursos de Física
(13, 'Física Mecánica', 5),
(14, 'Electromagnetismo', 5),

-- Cursos de Psicología
(15, 'Psicología General', 6),
(16, 'Psicología Social', 6)
ON CONFLICT (id) DO NOTHING;

-- ================================================
-- USUARIOS - ESTUDIANTES (prospect)
-- ================================================
CREATE TABLE IF NOT EXISTS banner.prospect (
    id VARCHAR(50) PRIMARY KEY,
    code VARCHAR(50) NOT NULL UNIQUE,
    name VARCHAR(255) NOT NULL,
    last_name VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    semester INTEGER,
    grade_average DECIMAL(3,2),
    grade_course DECIMAL(3,2)
);

INSERT INTO banner.prospect (id, code, name, last_name, email, password, semester, grade_average, grade_course) VALUES
('2220001', '2220001', 'Juan', 'Pérez García', 'juan.perez@ejemplo.com', '123456', 5, 4.2, 4.5),
('2220002', '2220002', 'María', 'López Martínez', 'maria.lopez@ejemplo.com', '123456', 6, 4.5, 4.8),
('2220003', '2220003', 'Carlos', 'Rodríguez Silva', 'carlos.rodriguez@ejemplo.com', '123456', 4, 4.0, 4.2),
('2220004', '2220004', 'Ana', 'González Torres', 'ana.gonzalez@ejemplo.com', '123456', 7, 4.7, 4.9),
('2220005', '2220005', 'Luis', 'Sánchez Morales', 'luis.sanchez@ejemplo.com', '123456', 5, 4.3, 4.4)
ON CONFLICT (id) DO NOTHING;

-- ================================================
-- USUARIOS - PROFESORES (professor)
-- ================================================
CREATE TABLE IF NOT EXISTS banner.professor (
    id VARCHAR(50) PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    password VARCHAR(255) NOT NULL
);

INSERT INTO banner.professor (id, name, password) VALUES
('1001', 'Dr. Roberto Castillo', 'prof123'),
('1002', 'Dra. Patricia Méndez', 'prof123'),
('1003', 'Dr. Fernando Ríos', 'prof123'),
('1004', 'Dra. Isabel Vargas', 'prof123')
ON CONFLICT (id) DO NOTHING;

-- ================================================
-- USUARIOS - JEFE DE DEPARTAMENTO (department_head)
-- ================================================
CREATE TABLE IF NOT EXISTS banner.department_head (
    id VARCHAR(50) PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    password VARCHAR(255) NOT NULL
);

INSERT INTO banner.department_head (id, name, password) VALUES
('5001', 'Dr. Alejandro Ramírez', 'jefe123')
ON CONFLICT (id) DO NOTHING;

-- ================================================
-- RELACIONES PROFESOR-CURSO (course_professor)
-- ================================================
CREATE TABLE IF NOT EXISTS banner.course_professor (
    id BIGINT PRIMARY KEY,
    professor_id VARCHAR(50),
    course_id BIGINT,
    FOREIGN KEY (professor_id) REFERENCES banner.professor(id),
    FOREIGN KEY (course_id) REFERENCES banner.course(id)
);

INSERT INTO banner.course_professor (id, professor_id, course_id) VALUES
-- Dr. Roberto Castillo (1001)
(1, '1001', 1),  -- Programación I
(2, '1001', 2),  -- Estructuras de Datos
(3, '1001', 4),  -- Desarrollo Web

-- Dra. Patricia Méndez (1002)
(4, '1002', 3),  -- Bases de Datos
(5, '1002', 5),  -- Inteligencia Artificial

-- Dr. Fernando Ríos (1003)
(6, '1003', 6),  -- Estadística
(7, '1003', 7),  -- Investigación de Operaciones

-- Dra. Isabel Vargas (1004)
(8, '1004', 11), -- Cálculo Diferencial
(9, '1004', 12)  -- Álgebra Lineal
ON CONFLICT (id) DO NOTHING;

-- ================================================
-- ESTUDIANTES EN CURSOS (student_course)
-- ================================================
CREATE TABLE IF NOT EXISTS banner.student_course (
    id BIGINT PRIMARY KEY,
    student_id VARCHAR(50),
    course_id BIGINT,
    FOREIGN KEY (student_id) REFERENCES banner.prospect(id),
    FOREIGN KEY (course_id) REFERENCES banner.course(id)
);

INSERT INTO banner.student_course (id, student_id, course_id) VALUES
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
-- VERIFICACIÓN Y MENSAJE FINAL
-- ================================================
SELECT '✅ Datos creados exitosamente en schema BANNER!' as resultado;
SELECT '' as espacio;
SELECT '📊 RESUMEN DE DATOS CREADOS EN BANNER:' as titulo;
SELECT 'Facultades: ' || (SELECT COUNT(*) FROM banner.school) as total1;
SELECT 'Programas: ' || (SELECT COUNT(*) FROM banner.program) as total2;
SELECT 'Cursos: ' || (SELECT COUNT(*) FROM banner.course) as total3;
SELECT 'Estudiantes: ' || (SELECT COUNT(*) FROM banner.prospect) as total4;
SELECT 'Profesores: ' || (SELECT COUNT(*) FROM banner.professor) as total5;
SELECT 'Jefes de Departamento: ' || (SELECT COUNT(*) FROM banner.department_head) as total6;
SELECT 'Relaciones Profesor-Curso: ' || (SELECT COUNT(*) FROM banner.course_professor) as total7;
SELECT 'Estudiantes en Cursos: ' || (SELECT COUNT(*) FROM banner.student_course) as total8;
SELECT '' as espacio2;
SELECT 'Ahora ejecuta: 3_datos_sigma.sql' as siguiente_paso;

