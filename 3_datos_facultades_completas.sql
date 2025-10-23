-- ================================================
-- PASO 3: DATOS COMPLETOS DE FACULTADES, PROGRAMAS Y CURSOS
-- ================================================
-- Ejecuta este script en AMBAS bases de datos: sigma_db Y banner_db
-- (Las estructuras académicas deben estar sincronizadas)

-- ================================================
-- 1. INSERTAR FACULTADES
-- ================================================

INSERT INTO public.school (name) VALUES
('Barberi de Ingeniería Diseño y Ciencias Aplicadas'),
('Ciencias de la Salud'),
('Negocios y Economía Isaac Gilinski'),
('Ciencias Humanas'),
('Decanatura de Innovación'),
('Educación y Fortalecimiento del PEI')
ON CONFLICT DO NOTHING;

-- ================================================
-- 2. INSERTAR PROGRAMAS
-- ================================================

-- Programas de Barberi de Ingeniería, Diseño y Ciencias Aplicadas
INSERT INTO public.program (name, school_id) VALUES
('Ingeniería de Sistemas', (SELECT id FROM public.school WHERE name='Barberi de Ingeniería Diseño y Ciencias Aplicadas')),
('Ingeniería Civil', (SELECT id FROM public.school WHERE name='Barberi de Ingeniería Diseño y Ciencias Aplicadas')),
('Diseño Industrial', (SELECT id FROM public.school WHERE name='Barberi de Ingeniería Diseño y Ciencias Aplicadas'))
ON CONFLICT DO NOTHING;

-- Programas de Ciencias de la Salud
INSERT INTO public.program (name, school_id) VALUES
('Medicina', (SELECT id FROM public.school WHERE name='Ciencias de la Salud')),
('Enfermería', (SELECT id FROM public.school WHERE name='Ciencias de la Salud')),
('Psicología', (SELECT id FROM public.school WHERE name='Ciencias de la Salud'))
ON CONFLICT DO NOTHING;

-- Programas de Negocios y Economía Isaac Gilinski
INSERT INTO public.program (name, school_id) VALUES
('Administración de Empresas', (SELECT id FROM public.school WHERE name='Negocios y Economía Isaac Gilinski')),
('Economía', (SELECT id FROM public.school WHERE name='Negocios y Economía Isaac Gilinski'))
ON CONFLICT DO NOTHING;

-- Programas de Ciencias Humanas
INSERT INTO public.program (name, school_id) VALUES
('Filosofía', (SELECT id FROM public.school WHERE name='Ciencias Humanas')),
('Literatura', (SELECT id FROM public.school WHERE name='Ciencias Humanas')),
('Historia', (SELECT id FROM public.school WHERE name='Ciencias Humanas'))
ON CONFLICT DO NOTHING;

-- Programas de Decanatura de Innovación
INSERT INTO public.program (name, school_id) VALUES
('Emprendimiento', (SELECT id FROM public.school WHERE name='Decanatura de Innovación')),
('Innovación Social', (SELECT id FROM public.school WHERE name='Decanatura de Innovación'))
ON CONFLICT DO NOTHING;

-- Programas de Educación y Fortalecimiento del PEI
INSERT INTO public.program (name, school_id) VALUES
('Pedagogía', (SELECT id FROM public.school WHERE name='Educación y Fortalecimiento del PEI')),
('Licenciatura en Educación', (SELECT id FROM public.school WHERE name='Educación y Fortalecimiento del PEI'))
ON CONFLICT DO NOTHING;

-- ================================================
-- 3. INSERTAR CURSOS
-- ================================================

-- Cursos de Ingeniería de Sistemas
INSERT INTO public.course (name, program_id) VALUES
('Estructuras de Datos', (SELECT id FROM public.program WHERE name='Ingeniería de Sistemas')),
('Bases de Datos', (SELECT id FROM public.program WHERE name='Ingeniería de Sistemas')),
('Algoritmos', (SELECT id FROM public.program WHERE name='Ingeniería de Sistemas')),
('Programación Orientada a Objetos', (SELECT id FROM public.program WHERE name='Ingeniería de Sistemas'))
ON CONFLICT DO NOTHING;

-- Cursos de Ingeniería Civil
INSERT INTO public.course (name, program_id) VALUES
('Mecánica de Suelos', (SELECT id FROM public.program WHERE name='Ingeniería Civil'))
ON CONFLICT DO NOTHING;

-- Cursos de Diseño Industrial
INSERT INTO public.course (name, program_id) VALUES
('Diseño Asistido por Computador', (SELECT id FROM public.program WHERE name='Diseño Industrial'))
ON CONFLICT DO NOTHING;

-- Cursos de Medicina
INSERT INTO public.course (name, program_id) VALUES
('Anatomía Humana', (SELECT id FROM public.program WHERE name='Medicina')),
('Fisiología', (SELECT id FROM public.program WHERE name='Medicina'))
ON CONFLICT DO NOTHING;

-- Cursos de Enfermería
INSERT INTO public.course (name, program_id) VALUES
('Cuidado Crítico', (SELECT id FROM public.program WHERE name='Enfermería'))
ON CONFLICT DO NOTHING;

-- Cursos de Psicología
INSERT INTO public.course (name, program_id) VALUES
('Psicología Clínica', (SELECT id FROM public.program WHERE name='Psicología'))
ON CONFLICT DO NOTHING;

-- Cursos de Administración de Empresas
INSERT INTO public.course (name, program_id) VALUES
('Contabilidad Financiera', (SELECT id FROM public.program WHERE name='Administración de Empresas')),
('Gestión Estratégica', (SELECT id FROM public.program WHERE name='Administración de Empresas'))
ON CONFLICT DO NOTHING;

-- Cursos de Economía
INSERT INTO public.course (name, program_id) VALUES
('Microeconomía', (SELECT id FROM public.program WHERE name='Economía')),
('Macroeconomía', (SELECT id FROM public.program WHERE name='Economía'))
ON CONFLICT DO NOTHING;

-- Cursos de Filosofía
INSERT INTO public.course (name, program_id) VALUES
('Ética y Moral', (SELECT id FROM public.program WHERE name='Filosofía'))
ON CONFLICT DO NOTHING;

-- Cursos de Literatura
INSERT INTO public.course (name, program_id) VALUES
('Análisis Literario', (SELECT id FROM public.program WHERE name='Literatura'))
ON CONFLICT DO NOTHING;

-- Cursos de Historia
INSERT INTO public.course (name, program_id) VALUES
('Historia de Colombia', (SELECT id FROM public.program WHERE name='Historia'))
ON CONFLICT DO NOTHING;

-- Cursos de Emprendimiento
INSERT INTO public.course (name, program_id) VALUES
('Modelos de Negocio', (SELECT id FROM public.program WHERE name='Emprendimiento'))
ON CONFLICT DO NOTHING;

-- Cursos de Innovación Social
INSERT INTO public.course (name, program_id) VALUES
('Diseño de Proyectos Sociales', (SELECT id FROM public.program WHERE name='Innovación Social'))
ON CONFLICT DO NOTHING;

-- Cursos de Pedagogía
INSERT INTO public.course (name, program_id) VALUES
('Didáctica General', (SELECT id FROM public.program WHERE name='Pedagogía'))
ON CONFLICT DO NOTHING;

-- Cursos de Licenciatura en Educación
INSERT INTO public.course (name, program_id) VALUES
('Evaluación Educativa', (SELECT id FROM public.program WHERE name='Licenciatura en Educación'))
ON CONFLICT DO NOTHING;

-- ================================================
-- 4. ASIGNAR PROFESORES A CURSOS
-- ================================================
-- Asignamos el profesor '1001' (Dr. Roberto Castillo) a todos los cursos como ejemplo
-- Puedes modificar esto para asignar diferentes profesores

INSERT INTO public.course_professor (course_id, professor_id)
SELECT c.id, '1001'
FROM public.course c
WHERE c.name IN (
    'Estructuras de Datos',
    'Bases de Datos',
    'Algoritmos',
    'Programación Orientada a Objetos',
    'Mecánica de Suelos',
    'Diseño Asistido por Computador',
    'Anatomía Humana',
    'Fisiología',
    'Cuidado Crítico',
    'Psicología Clínica',
    'Contabilidad Financiera',
    'Gestión Estratégica',
    'Microeconomía',
    'Macroeconomía',
    'Ética y Moral',
    'Análisis Literario',
    'Historia de Colombia',
    'Modelos de Negocio',
    'Diseño de Proyectos Sociales',
    'Didáctica General',
    'Evaluación Educativa'
)
ON CONFLICT ON CONSTRAINT uq_course_prof DO NOTHING;

-- ================================================
-- 5. CREAR ESTUDIANTES DEMO (PARA VALIDACIÓN 15+)
-- ================================================
-- Solo en banner_db (estudiantes ficticios para cada curso)

-- Generar 20 estudiantes de ejemplo
INSERT INTO prospect (id, code, name, last_name, email, password, semester, grade_average, grade_course) VALUES
('2220006', '2220006', 'Pedro', 'Martínez López', 'pedro.martinez@ejemplo.com', '123456', 5, 4.1, 4.3),
('2220007', '2220007', 'Laura', 'Fernández Castro', 'laura.fernandez@ejemplo.com', '123456', 6, 4.4, 4.6),
('2220008', '2220008', 'Diego', 'Ramírez Soto', 'diego.ramirez@ejemplo.com', '123456', 4, 4.0, 4.1),
('2220009', '2220009', 'Camila', 'Torres Gómez', 'camila.torres@ejemplo.com', '123456', 7, 4.6, 4.8),
('2220010', '2220010', 'Andrés', 'Morales Ruiz', 'andres.morales@ejemplo.com', '123456', 5, 4.3, 4.5),
('2220011', '2220011', 'Valentina', 'Castro Díaz', 'valentina.castro@ejemplo.com', '123456', 6, 4.5, 4.7),
('2220012', '2220012', 'Santiago', 'Hernández Mejía', 'santiago.hernandez@ejemplo.com', '123456', 4, 4.2, 4.4),
('2220013', '2220013', 'Isabella', 'Vargas Ortiz', 'isabella.vargas@ejemplo.com', '123456', 7, 4.7, 4.9),
('2220014', '2220014', 'Mateo', 'Jiménez Cruz', 'mateo.jimenez@ejemplo.com', '123456', 5, 4.4, 4.6),
('2220015', '2220015', 'Sofía', 'Romero Silva', 'sofia.romero@ejemplo.com', '123456', 6, 4.6, 4.8),
('2220016', '2220016', 'Sebastián', 'Gutiérrez Rojas', 'sebastian.gutierrez@ejemplo.com', '123456', 4, 4.1, 4.2),
('2220017', '2220017', 'Mariana', 'Mendoza Paredes', 'mariana.mendoza@ejemplo.com', '123456', 7, 4.5, 4.7),
('2220018', '2220018', 'Nicolás', 'Salazar Vega', 'nicolas.salazar@ejemplo.com', '123456', 5, 4.3, 4.4),
('2220019', '2220019', 'Daniela', 'Acosta Reyes', 'daniela.acosta@ejemplo.com', '123456', 6, 4.4, 4.5),
('2220020', '2220020', 'Felipe', 'Navarro Campos', 'felipe.navarro@ejemplo.com', '123456', 4, 4.0, 4.1),
('2220021', '2220021', 'Gabriela', 'Parra Luna', 'gabriela.parra@ejemplo.com', '123456', 7, 4.6, 4.8),
('2220022', '2220022', 'Alejandro', 'Cortés Mora', 'alejandro.cortes@ejemplo.com', '123456', 5, 4.2, 4.3),
('2220023', '2220023', 'Carolina', 'Figueroa Arias', 'carolina.figueroa@ejemplo.com', '123456', 6, 4.5, 4.7),
('2220024', '2220024', 'Emilio', 'Duarte Sandoval', 'emilio.duarte@ejemplo.com', '123456', 4, 4.3, 4.4),
('2220025', '2220025', 'Natalia', 'Peña Chávez', 'natalia.pena@ejemplo.com', '123456', 7, 4.7, 4.9)
ON CONFLICT (id) DO NOTHING;

-- ================================================
-- 6. MATRICULAR ESTUDIANTES EN CURSOS (15+ por curso)
-- ================================================
-- Solo en banner_db
-- Matriculamos 15-20 estudiantes en cada curso para cumplir el criterio H1

-- Estructuras de Datos (18 estudiantes)
INSERT INTO student_course (student_id, course_id)
SELECT p.id, (SELECT id FROM course WHERE name='Estructuras de Datos')
FROM prospect p
WHERE p.id IN ('2220001','2220002','2220003','2220004','2220005','2220006','2220007','2220008','2220009','2220010','2220011','2220012','2220013','2220014','2220015','2220016','2220017','2220018')
ON CONFLICT DO NOTHING;

-- Bases de Datos (17 estudiantes)
INSERT INTO student_course (student_id, course_id)
SELECT p.id, (SELECT id FROM course WHERE name='Bases de Datos')
FROM prospect p
WHERE p.id IN ('2220002','2220003','2220004','2220005','2220006','2220007','2220008','2220009','2220010','2220011','2220012','2220013','2220014','2220015','2220016','2220017','2220018')
ON CONFLICT DO NOTHING;

-- Algoritmos (20 estudiantes)
INSERT INTO student_course (student_id, course_id)
SELECT p.id, (SELECT id FROM course WHERE name='Algoritmos')
FROM prospect p
WHERE p.id IN ('2220001','2220002','2220003','2220004','2220005','2220006','2220007','2220008','2220009','2220010','2220011','2220012','2220013','2220014','2220015','2220016','2220017','2220018','2220019','2220020')
ON CONFLICT DO NOTHING;

-- Programación Orientada a Objetos (16 estudiantes)
INSERT INTO student_course (student_id, course_id)
SELECT p.id, (SELECT id FROM course WHERE name='Programación Orientada a Objetos')
FROM prospect p
WHERE p.id IN ('2220003','2220004','2220005','2220006','2220007','2220008','2220009','2220010','2220011','2220012','2220013','2220014','2220015','2220016','2220017','2220018')
ON CONFLICT DO NOTHING;

-- Mecánica de Suelos (15 estudiantes)
INSERT INTO student_course (student_id, course_id)
SELECT p.id, (SELECT id FROM course WHERE name='Mecánica de Suelos')
FROM prospect p
WHERE p.id IN ('2220001','2220002','2220003','2220004','2220005','2220006','2220007','2220008','2220009','2220010','2220011','2220012','2220013','2220014','2220015')
ON CONFLICT DO NOTHING;

-- Diseño Asistido por Computador (19 estudiantes)
INSERT INTO student_course (student_id, course_id)
SELECT p.id, (SELECT id FROM course WHERE name='Diseño Asistido por Computador')
FROM prospect p
WHERE p.id IN ('2220002','2220003','2220004','2220005','2220006','2220007','2220008','2220009','2220010','2220011','2220012','2220013','2220014','2220015','2220016','2220017','2220018','2220019','2220020')
ON CONFLICT DO NOTHING;

-- Anatomía Humana (20 estudiantes)
INSERT INTO student_course (student_id, course_id)
SELECT p.id, (SELECT id FROM course WHERE name='Anatomía Humana')
FROM prospect p
WHERE p.id IN ('2220001','2220002','2220003','2220004','2220005','2220006','2220007','2220008','2220009','2220010','2220011','2220012','2220013','2220014','2220015','2220016','2220017','2220018','2220019','2220020')
ON CONFLICT DO NOTHING;

-- Fisiología (18 estudiantes)
INSERT INTO student_course (student_id, course_id)
SELECT p.id, (SELECT id FROM course WHERE name='Fisiología')
FROM prospect p
WHERE p.id IN ('2220001','2220002','2220003','2220004','2220005','2220006','2220007','2220008','2220009','2220010','2220011','2220012','2220013','2220014','2220015','2220016','2220017','2220018')
ON CONFLICT DO NOTHING;

-- Cuidado Crítico (17 estudiantes)
INSERT INTO student_course (student_id, course_id)
SELECT p.id, (SELECT id FROM course WHERE name='Cuidado Crítico')
FROM prospect p
WHERE p.id IN ('2220002','2220003','2220004','2220005','2220006','2220007','2220008','2220009','2220010','2220011','2220012','2220013','2220014','2220015','2220016','2220017','2220018')
ON CONFLICT DO NOTHING;

-- Psicología Clínica (16 estudiantes)
INSERT INTO student_course (student_id, course_id)
SELECT p.id, (SELECT id FROM course WHERE name='Psicología Clínica')
FROM prospect p
WHERE p.id IN ('2220003','2220004','2220005','2220006','2220007','2220008','2220009','2220010','2220011','2220012','2220013','2220014','2220015','2220016','2220017','2220018')
ON CONFLICT DO NOTHING;

-- Los demás cursos también con 15+ estudiantes
INSERT INTO student_course (student_id, course_id)
SELECT p.id, c.id
FROM prospect p, course c
WHERE c.name IN ('Contabilidad Financiera','Gestión Estratégica','Microeconomía','Macroeconomía','Ética y Moral','Análisis Literario','Historia de Colombia','Modelos de Negocio','Diseño de Proyectos Sociales','Didáctica General','Evaluación Educativa')
AND p.id IN ('2220001','2220002','2220003','2220004','2220005','2220006','2220007','2220008','2220009','2220010','2220011','2220012','2220013','2220014','2220015')
ON CONFLICT DO NOTHING;

-- ================================================
-- VERIFICACIÓN
-- ================================================

SELECT '✅ ¡Datos completos insertados exitosamente!' as mensaje;
SELECT '' as espacio;
SELECT '========================================' as separador;
SELECT 'RESUMEN DE DATOS INSERTADOS' as titulo;
SELECT '========================================' as separador;
SELECT '' as espacio;

-- Contar facultades
SELECT COUNT(*) as total_facultades, 'Facultades creadas' as descripcion FROM school;

-- Contar programas
SELECT COUNT(*) as total_programas, 'Programas creados' as descripcion FROM program;

-- Contar cursos
SELECT COUNT(*) as total_cursos, 'Cursos creados' as descripcion FROM course;

-- Contar estudiantes (solo banner_db)
SELECT COUNT(*) as total_estudiantes, 'Estudiantes registrados' as descripcion FROM prospect;

-- Mostrar matrícula por curso (solo banner_db)
SELECT c.name as curso, COUNT(sc.student_id) as estudiantes_matriculados
FROM course c
LEFT JOIN student_course sc ON c.id = sc.course_id
GROUP BY c.name
ORDER BY estudiantes_matriculados DESC;

SELECT '' as espacio2;
SELECT '========================================' as separador2;
SELECT '¡Ahora puedes cargar los CSV y crear monitorías!' as final;
