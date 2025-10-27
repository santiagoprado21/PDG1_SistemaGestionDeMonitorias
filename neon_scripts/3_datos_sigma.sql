-- ================================================
-- DATOS PARA SCHEMA SIGMA EN NEON
-- ================================================
-- EJECUTAR EN: SQL Editor de Neon
-- IMPORTANTE: Ejecuta primero 1_create_schemas.sql y 2_datos_banner.sql
-- ================================================

-- Asegurarnos de estar en el schema correcto
SET search_path TO sigma, public;

-- ================================================
-- TABLAS ACADÉMICAS
-- ================================================

-- ESCUELAS/FACULTADES (school)
CREATE TABLE IF NOT EXISTS sigma.school (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL
);

INSERT INTO sigma.school (id, name) VALUES
(1, 'Facultad de Ingeniería'),
(2, 'Facultad de Ciencias'),
(3, 'Facultad de Humanidades')
ON CONFLICT (id) DO NOTHING;

-- PROGRAMAS (program)
CREATE TABLE IF NOT EXISTS sigma.program (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    school_id BIGINT NOT NULL,
    FOREIGN KEY (school_id) REFERENCES sigma.school(id)
);

INSERT INTO sigma.program (id, name, school_id) VALUES
(1, 'Ingeniería de Sistemas', 1),
(2, 'Ingeniería Industrial', 1),
(3, 'Ingeniería Bioquímica', 1),
(4, 'Matemáticas', 2),
(5, 'Física', 2),
(6, 'Psicología', 3)
ON CONFLICT (id) DO NOTHING;

-- CURSOS (course)
CREATE TABLE IF NOT EXISTS sigma.course (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    program_id BIGINT NOT NULL,
    FOREIGN KEY (program_id) REFERENCES sigma.program(id)
);

INSERT INTO sigma.course (id, name, program_id) VALUES
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
-- TABLAS DE USUARIOS
-- ================================================

-- ESTUDIANTES/ASPIRANTES (prospect)
CREATE TABLE IF NOT EXISTS sigma.prospect (
    id VARCHAR(100) PRIMARY KEY,
    code VARCHAR(70) NOT NULL UNIQUE,
    name VARCHAR(70) NOT NULL,
    last_name VARCHAR(70) NOT NULL,
    email VARCHAR(100) NOT NULL UNIQUE,
    password VARCHAR(100) NOT NULL,
    semester INTEGER NOT NULL,
    grade_average DECIMAL(3,2),
    grade_course DECIMAL(3,2)
);

INSERT INTO sigma.prospect (id, code, name, last_name, email, password, semester, grade_average, grade_course) VALUES
('2220001', '2220001', 'Juan', 'Pérez García', 'juan.perez@ejemplo.com', '123456', 5, 4.2, 4.5),
('2220002', '2220002', 'María', 'López Martínez', 'maria.lopez@ejemplo.com', '123456', 6, 4.5, 4.8),
('2220003', '2220003', 'Carlos', 'Rodríguez Silva', 'carlos.rodriguez@ejemplo.com', '123456', 4, 4.0, 4.2),
('2220004', '2220004', 'Ana', 'González Torres', 'ana.gonzalez@ejemplo.com', '123456', 7, 4.7, 4.9),
('2220005', '2220005', 'Luis', 'Sánchez Morales', 'luis.sanchez@ejemplo.com', '123456', 5, 4.3, 4.4)
ON CONFLICT (id) DO NOTHING;

-- PROFESORES (professor)
CREATE TABLE IF NOT EXISTS sigma.professor (
    id VARCHAR(255) PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    password VARCHAR(255) NOT NULL
);

INSERT INTO sigma.professor (id, name, password) VALUES
('1001', 'Dr. Roberto Castillo', 'prof123'),
('1002', 'Dra. Patricia Méndez', 'prof123'),
('1003', 'Dr. Fernando Ríos', 'prof123'),
('1004', 'Dra. Isabel Vargas', 'prof123')
ON CONFLICT (id) DO NOTHING;

-- JEFES DE DEPARTAMENTO (department_head)
CREATE TABLE IF NOT EXISTS sigma.department_head (
    id VARCHAR(255) PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    password VARCHAR(255) NOT NULL
);

INSERT INTO sigma.department_head (id, name, password) VALUES
('5001', 'Dr. Alejandro Ramírez', 'jefe123')
ON CONFLICT (id) DO NOTHING;

-- ================================================
-- TABLAS DE RELACIONES
-- ================================================

-- RELACIÓN PROFESOR-CURSO (course_professor)
CREATE TABLE IF NOT EXISTS sigma.course_professor (
    id SERIAL PRIMARY KEY,
    course_id BIGINT NOT NULL,
    professor_id VARCHAR(255) NOT NULL,
    FOREIGN KEY (course_id) REFERENCES sigma.course(id),
    FOREIGN KEY (professor_id) REFERENCES sigma.professor(id)
);

INSERT INTO sigma.course_professor (id, course_id, professor_id) VALUES
-- Dr. Roberto Castillo (1001)
(1, 1, '1001'),  -- Programación I
(2, 2, '1001'),  -- Estructuras de Datos
(3, 4, '1001'),  -- Desarrollo Web

-- Dra. Patricia Méndez (1002)
(4, 3, '1002'),  -- Bases de Datos
(5, 5, '1002'),  -- Inteligencia Artificial

-- Dr. Fernando Ríos (1003)
(6, 6, '1003'),  -- Estadística
(7, 7, '1003'),  -- Investigación de Operaciones

-- Dra. Isabel Vargas (1004)
(8, 11, '1004'), -- Cálculo Diferencial
(9, 12, '1004')  -- Álgebra Lineal
ON CONFLICT (id) DO NOTHING;

-- RELACIÓN JEFE-PROGRAMA (head_professor)
CREATE TABLE IF NOT EXISTS sigma.head_professor (
    id SERIAL PRIMARY KEY,
    department_head_id VARCHAR(255) NOT NULL,
    program_id BIGINT NOT NULL,
    FOREIGN KEY (department_head_id) REFERENCES sigma.department_head(id),
    FOREIGN KEY (program_id) REFERENCES sigma.program(id)
);

INSERT INTO sigma.head_professor (id, department_head_id, program_id) VALUES
(1, '5001', 1),  -- Ingeniería de Sistemas
(2, '5001', 2),  -- Ingeniería Industrial
(3, '5001', 3)   -- Ingeniería Electrónica
ON CONFLICT (id) DO NOTHING;

-- ESTUDIANTES EN CURSOS (student_course) - para asistencia
CREATE TABLE IF NOT EXISTS sigma.student_course (
    id BIGSERIAL PRIMARY KEY,
    course_id INTEGER NOT NULL,
    student_id VARCHAR(20) NOT NULL
);

-- ================================================
-- TABLAS DE MONITORIAS
-- ================================================

-- MONITORES (monitor)
CREATE TABLE IF NOT EXISTS sigma.monitor (
    code VARCHAR(20) PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    semester INTEGER NOT NULL,
    grade_average DECIMAL(3,2),
    grade_course DECIMAL(3,2),
    email VARCHAR(100) NOT NULL,
    id VARCHAR(100) NOT NULL
);

-- MONITORIAS (monitoring)
CREATE TABLE IF NOT EXISTS sigma.monitoring (
    id BIGSERIAL PRIMARY KEY,
    school_id BIGINT NOT NULL,
    program_id BIGINT NOT NULL,
    course_id BIGINT NOT NULL,
    start_date TIMESTAMP NOT NULL,
    finish_date TIMESTAMP NOT NULL,
    average_grade DECIMAL(3,2),
    course_grade DECIMAL(3,2),
    semester VARCHAR(255) NOT NULL,
    professor_id VARCHAR(255) NOT NULL,
    FOREIGN KEY (school_id) REFERENCES sigma.school(id),
    FOREIGN KEY (program_id) REFERENCES sigma.program(id),
    FOREIGN KEY (course_id) REFERENCES sigma.course(id),
    FOREIGN KEY (professor_id) REFERENCES sigma.professor(id)
);

-- RELACIÓN MONITORIA-MONITOR (monitoring_monitor)
CREATE TABLE IF NOT EXISTS sigma.monitoring_monitor (
    id BIGSERIAL PRIMARY KEY,
    estado_seleccion VARCHAR(255) NOT NULL DEFAULT 'no seleccionado',
    comentario_decision TEXT,
    fecha_decision TIMESTAMP,
    decidido_por VARCHAR(255),
    monitoring_id BIGINT NOT NULL,
    monitor_id VARCHAR(20) NOT NULL,
    FOREIGN KEY (monitoring_id) REFERENCES sigma.monitoring(id),
    FOREIGN KEY (monitor_id) REFERENCES sigma.monitor(code)
);

-- ACTIVIDADES (activity)
CREATE TABLE IF NOT EXISTS sigma.activity (
    id SERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    creation_date TIMESTAMP NOT NULL,
    finish_date TIMESTAMP NOT NULL,
    role_creator CHAR(1) NOT NULL,
    role_responsable CHAR(1) NOT NULL,
    category VARCHAR(30),
    description VARCHAR(255) NOT NULL,
    monitoring_id BIGINT NOT NULL,
    professor_id VARCHAR(255),
    monitor_id VARCHAR(20),
    state VARCHAR(50) NOT NULL,
    delivey_date TIMESTAMP,
    edited_date TIMESTAMP,
    semester VARCHAR(8),
    FOREIGN KEY (monitoring_id) REFERENCES sigma.monitoring(id),
    FOREIGN KEY (professor_id) REFERENCES sigma.professor(id),
    FOREIGN KEY (monitor_id) REFERENCES sigma.monitor(code)
);

-- CATEGORÍAS (category)
CREATE TABLE IF NOT EXISTS sigma.category (
    id SERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    course_id BIGINT NOT NULL,
    FOREIGN KEY (course_id) REFERENCES sigma.course(id)
);

-- ESTUDIANTES (student) - para asistencia
CREATE TABLE IF NOT EXISTS sigma.student (
    code VARCHAR(50) PRIMARY KEY,
    name VARCHAR(255) NOT NULL
);

-- ASISTENCIA (attendance)
CREATE TABLE IF NOT EXISTS sigma.attendance (
    id SERIAL PRIMARY KEY,
    activity_id INTEGER NOT NULL,
    student_id VARCHAR(50) NOT NULL,
    FOREIGN KEY (activity_id) REFERENCES sigma.activity(id),
    FOREIGN KEY (student_id) REFERENCES sigma.student(code)
);

-- ================================================
-- VERIFICACIÓN Y MENSAJE FINAL
-- ================================================
SELECT '✅ Schema SIGMA creado exitosamente con todas las tablas!' as resultado;
SELECT '' as espacio;
SELECT '📊 RESUMEN DE DATOS CREADOS EN SIGMA:' as titulo;
SELECT 'Facultades: ' || (SELECT COUNT(*) FROM sigma.school) as total1;
SELECT 'Programas: ' || (SELECT COUNT(*) FROM sigma.program) as total2;
SELECT 'Cursos: ' || (SELECT COUNT(*) FROM sigma.course) as total3;
SELECT 'Estudiantes/Aspirantes: ' || (SELECT COUNT(*) FROM sigma.prospect) as total4;
SELECT 'Profesores: ' || (SELECT COUNT(*) FROM sigma.professor) as total5;
SELECT 'Jefes de Departamento: ' || (SELECT COUNT(*) FROM sigma.department_head) as total6;
SELECT 'Relaciones Profesor-Curso: ' || (SELECT COUNT(*) FROM sigma.course_professor) as total7;
SELECT 'Relaciones Jefe-Programa: ' || (SELECT COUNT(*) FROM sigma.head_professor) as total8;
SELECT '' as espacio2;
SELECT '🎓 CREDENCIALES PARA LOGIN:' as seccion;
SELECT 'Estudiante: 2220001 / 123456' as cred1;
SELECT 'Profesor: 1001 / prof123' as cred2;
SELECT 'Jefe Depto: 5001 / jefe123' as cred3;
SELECT '' as espacio3;
SELECT '✅ ¡Base de datos lista para usar!' as final;
SELECT 'Ahora configura el backend con las credenciales de Neon' as siguiente_paso;

