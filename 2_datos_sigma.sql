-- ================================================
-- PASO 2: USUARIOS PARA SIGMA_DB
-- ================================================
-- Ejecuta este script en la base de datos: sigma_db


-- CREAR TABLAS PARA LOS INSERTS:
-- Tabla: professor
CREATE TABLE IF NOT EXISTS public.professor (
  id varchar(255) PRIMARY KEY,
  name varchar(100) NOT NULL,
  password varchar(255) NOT NULL
);

-- Tabla: department_head
CREATE TABLE IF NOT EXISTS public.department_head (
  id varchar(255) PRIMARY KEY,
  name varchar(255) NOT NULL,
  password varchar(255) NOT NULL
);

-- Facultades
CREATE TABLE IF NOT EXISTS public.school (
  id bigserial PRIMARY KEY,
  name varchar(100) NOT NULL
);

-- Programas
CREATE TABLE IF NOT EXISTS public.program (
  id bigserial PRIMARY KEY,
  name varchar(100) NOT NULL,
  school_id bigint NOT NULL REFERENCES public.school(id)
);

-- Cursos
CREATE TABLE IF NOT EXISTS public.course (
  id bigserial PRIMARY KEY,
  name varchar(100) NOT NULL,
  program_id bigint NOT NULL REFERENCES public.program(id)
);

-- Relación curso-profesor
CREATE TABLE IF NOT EXISTS public.course_professor (
  id serial PRIMARY KEY,
  course_id bigint NOT NULL REFERENCES public.course(id),
  professor_id varchar(255) NOT NULL REFERENCES public.professor(id),
  CONSTRAINT uq_course_prof UNIQUE (course_id, professor_id)
);

-- Asociación jefe de departamento -> programa
-- OJO: en SIGMA la entidad usa la tabla head_professor (nombre distinto al API Banner)
CREATE TABLE IF NOT EXISTS public.head_professor (
  id serial PRIMARY KEY,
  department_head_id varchar(255) NOT NULL REFERENCES public.department_head(id),
  program_id bigint NOT NULL REFERENCES public.program(id)
);


-- ================================================


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

-- 1. Facultades
INSERT INTO public.school (name)
VALUES ('Ingeniería')
ON CONFLICT DO NOTHING;

-- 2. Programas
INSERT INTO public.program (name, school_id)
VALUES ('Ingeniería de Sistemas', (SELECT id FROM public.school WHERE name='Ingeniería'))
ON CONFLICT DO NOTHING;

-- 3. Cursos
INSERT INTO public.course (name, program_id)
VALUES 
  ('Estructuras de Datos', (SELECT id FROM public.program WHERE name='Ingeniería de Sistemas')),
  ('Bases de Datos', (SELECT id FROM public.program WHERE name='Ingeniería de Sistemas'))
ON CONFLICT DO NOTHING;

-- 4. Relación curso-profesor (usa IDs que ya insertaste en professor)
INSERT INTO public.course_professor (course_id, professor_id)
SELECT c.id, '1001'
FROM public.course c
WHERE c.name IN ('Estructuras de Datos','Bases de Datos')
ON CONFLICT ON CONSTRAINT uq_course_prof DO NOTHING;

-- 5. Jefe de departamento asociado al programa (usa tu jefe '5001')
INSERT INTO public.head_professor (department_head_id, program_id)
VALUES ('5001', (SELECT id FROM public.program WHERE name='Ingeniería de Sistemas'))
ON CONFLICT DO NOTHING;

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

