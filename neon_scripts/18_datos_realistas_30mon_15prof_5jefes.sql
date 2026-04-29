-- ============================================================
-- INSERCIÓN DE DATOS REALISTAS: 30 monitores, 15 profesores, 5 jefes
-- ============================================================
-- Prerequisitos: scripts 1 al 3 ya ejecutados.
-- Extiende los datos existentes sin borrar nada.
--
-- Datos existentes que se respetan:
--   Profesores:  1001–1004
--   Jefes:       5001 (cubre programas 1, 2, 3)
--   Monitores:   2220001–2220005 (prospects)
--   Cursos:      1–16
--   Relaciones   course_professor id 1–9
-- ============================================================

SET search_path TO sigma, public;

-- ============================================================
-- 1. PROFESORES NUEVOS (1005–1015)  → 11 adicionales + 4 existentes = 15
-- ============================================================
INSERT INTO sigma.professor (id, name, password) VALUES
('1005', 'Dr. Miguel Herrera Ospina',       'prof123'),
('1006', 'Dra. Carmen López Arango',        'prof123'),
('1007', 'Dr. Andrés Morales Cifuentes',    'prof123'),
('1008', 'Dra. Beatriz Torres Salcedo',     'prof123'),
('1009', 'Dr. Eduardo Jiménez Pardo',       'prof123'),
('1010', 'Dra. Sandra Ruiz Quintero',       'prof123'),
('1011', 'Dr. Héctor Vargas Muñoz',         'prof123'),
('1012', 'Dra. Lucía Mendoza Peñaloza',     'prof123'),
('1013', 'Dr. Pablo García Montoya',        'prof123'),
('1014', 'Dra. Verónica Castro Reyes',      'prof123'),
('1015', 'Dr. Cristian Soto Valencia',      'prof123')
ON CONFLICT (id) DO NOTHING;

-- ============================================================
-- 2. ASIGNACIÓN PROFESOR–CURSO  (continúa desde id=10)
-- Cada profesor dicta 2–3 cursos. Algunos cursos tienen varios
-- profesores (secciones distintas), lo cual es realista.
-- ============================================================
INSERT INTO sigma.course_professor (id, course_id, professor_id) VALUES
-- Dr. Miguel Herrera (1005) — Ing. Sistemas
(10,  1,  '1005'),   -- Programación I
(11,  2,  '1005'),   -- Estructuras de Datos
(12,  5,  '1005'),   -- Inteligencia Artificial

-- Dra. Carmen López (1006) — Ing. Sistemas
(13,  3,  '1006'),   -- Bases de Datos
(14,  4,  '1006'),   -- Desarrollo Web

-- Dr. Andrés Morales (1007) — Ing. Industrial
(15,  6,  '1007'),   -- Estadística
(16,  8,  '1007'),   -- Gestión de Proyectos
(17,  7,  '1007'),   -- Investigación de Operaciones

-- Dra. Beatriz Torres (1008) — Ing. Bioquímica
(18,  9,  '1008'),   -- Diseño Experimental
(19, 10,  '1008'),   -- Química

-- Dr. Eduardo Jiménez (1009) — Matemáticas
(20, 11,  '1009'),   -- Cálculo de una variable
(21, 12,  '1009'),   -- Álgebra Lineal

-- Dra. Sandra Ruiz (1010) — Matemáticas / Física
(22, 11,  '1010'),   -- Cálculo (segunda sección)
(23, 13,  '1010'),   -- Física Mecánica

-- Dr. Héctor Vargas (1011) — Física
(24, 13,  '1011'),   -- Física Mecánica (sección 2)
(25, 14,  '1011'),   -- Electromagnetismo

-- Dra. Lucía Mendoza (1012) — Psicología
(26, 15,  '1012'),   -- Psicología General
(27, 16,  '1012'),   -- Psicología Social

-- Dr. Pablo García (1013) — Psicología
(28, 15,  '1013'),   -- Psicología General (sección 2)
(29, 16,  '1013'),   -- Psicología Social (sección 2)

-- Dra. Verónica Castro (1014) — Ing. Sistemas
(30,  1,  '1014'),   -- Programación I (sección 2)
(31,  2,  '1014'),   -- Estructuras de Datos (sección 2)

-- Dr. Cristian Soto (1015) — Ing. Bioquímica / Industrial
(32,  9,  '1015'),   -- Diseño Experimental (sección 2)
(33,  6,  '1015')    -- Estadística (sección 2)
ON CONFLICT (id) DO NOTHING;

-- ============================================================
-- 3. JEFES DE DEPARTAMENTO NUEVOS (5002–5005)
--    + 5001 existente = 5 jefes
-- ============================================================
INSERT INTO sigma.department_head (id, name, password) VALUES
('5002', 'Dra. Marina Acosta Bernal',    'jefe123'),
('5003', 'Dr. Tomás Beltrán Cárdenas',   'jefe123'),
('5004', 'Dra. Gloria Peña Londoño',     'jefe123'),
('5005', 'Dr. Augusto Salazar Nieto',    'jefe123')
ON CONFLICT (id) DO NOTHING;

-- Asignación jefe–programa (continúa desde id=4)
INSERT INTO sigma.head_professor (id, department_head_id, program_id) VALUES
-- Dra. Marina Acosta (5002) → Matemáticas y Física
(4, '5002', 4),
(5, '5002', 5),

-- Dr. Tomás Beltrán (5003) → Psicología
(6, '5003', 6),

-- Dra. Gloria Peña (5004) → Ing. Bioquímica (comparte con 5001)
(7, '5004', 3),

-- Dr. Augusto Salazar (5005) → Ing. Sistemas e Industrial
(8, '5005', 1),
(9, '5005', 2)
ON CONFLICT (id) DO NOTHING;

-- ============================================================
-- 4. MONITORES — 30 registros
--    prospect: credenciales de login
--    monitor:  rol activo en el sistema
-- ============================================================

-- 4a. Insertar en prospect (para login)
INSERT INTO sigma.prospect (id, code, name, last_name, email, password, semester, grade_average, grade_course) VALUES
('2220006', '2220006', 'Sofía',     'Ramírez Herrera',  'sofia.ramirez@icesi.edu.co',    '123456', 6, 4.3, 4.6),
('2220007', '2220007', 'Diego',     'Castañeda López',  'diego.castaneda@icesi.edu.co',  '123456', 7, 4.5, 4.7),
('2220008', '2220008', 'Valentina', 'Moreno Arias',     'valentina.moreno@icesi.edu.co', '123456', 5, 4.1, 4.4),
('2220009', '2220009', 'Sebastián', 'Giraldo Torres',   'sebastian.giraldo@icesi.edu.co','123456', 8, 4.6, 4.8),
('2220010', '2220010', 'Isabela',   'Vargas Pinto',     'isabela.vargas@icesi.edu.co',   '123456', 6, 4.4, 4.5),
('2220011', '2220011', 'Nicolás',   'Ospina Cano',      'nicolas.ospina@icesi.edu.co',   '123456', 7, 4.2, 4.5),
('2220012', '2220012', 'Camila',    'Pedraza Ruiz',     'camila.pedraza@icesi.edu.co',   '123456', 5, 4.0, 4.3),
('2220013', '2220013', 'Mateo',     'Salinas Duque',    'mateo.salinas@icesi.edu.co',    '123456', 6, 4.5, 4.7),
('2220014', '2220014', 'Paula',     'Reyes Guzmán',     'paula.reyes@icesi.edu.co',      '123456', 7, 4.3, 4.6),
('2220015', '2220015', 'Andrés',    'Villamizar Peña',  'andres.villamizar@icesi.edu.co','123456', 8, 4.7, 4.9),
('2220016', '2220016', 'Laura',     'Cárdenas Mejía',   'laura.cardenas@icesi.edu.co',   '123456', 6, 4.1, 4.4),
('2220017', '2220017', 'Felipe',    'Montoya Arango',   'felipe.montoya@icesi.edu.co',   '123456', 5, 4.4, 4.6),
('2220018', '2220018', 'Daniela',   'Ríos Echeverry',   'daniela.rios@icesi.edu.co',     '123456', 7, 4.2, 4.5),
('2220019', '2220019', 'Julián',    'Palomino Vera',    'julian.palomino@icesi.edu.co',  '123456', 6, 4.6, 4.8),
('2220020', '2220020', 'Sara',      'Bermúdez Cortés',  'sara.bermudez@icesi.edu.co',    '123456', 5, 4.3, 4.5),
('2220021', '2220021', 'Miguel',    'Sandoval Gómez',   'miguel.sandoval@icesi.edu.co',  '123456', 8, 4.5, 4.7),
('2220022', '2220022', 'Natalia',   'Forero Medina',    'natalia.forero@icesi.edu.co',   '123456', 6, 4.0, 4.3),
('2220023', '2220023', 'Samuel',    'Hurtado Zúñiga',   'samuel.hurtado@icesi.edu.co',   '123456', 7, 4.4, 4.6),
('2220024', '2220024', 'Mariana',   'Quintero Lozano',  'mariana.quintero@icesi.edu.co', '123456', 5, 4.2, 4.4),
('2220025', '2220025', 'Daniel',    'Castaño Patiño',   'daniel.castano@icesi.edu.co',   '123456', 6, 4.6, 4.8),
('2220026', '2220026', 'Alejandra', 'Buitrago Nieto',   'alejandra.buitrago@icesi.edu.co','123456',7, 4.3, 4.5),
('2220027', '2220027', 'Tomás',     'Acevedo Cifuentes','tomas.acevedo@icesi.edu.co',    '123456', 8, 4.5, 4.7),
('2220028', '2220028', 'Manuela',   'Zapata Rincón',    'manuela.zapata@icesi.edu.co',   '123456', 6, 4.1, 4.4),
('2220029', '2220029', 'Esteban',   'Londoño Muñoz',    'esteban.londono@icesi.edu.co',  '123456', 7, 4.4, 4.6),
('2220030', '2220030', 'Gabriela',  'Correa Jiménez',   'gabriela.correa@icesi.edu.co',  '123456', 5, 4.2, 4.5),
('2220031', '2220031', 'Santiago',  'Peña Guerrero',    'santiago.pena@icesi.edu.co',    '123456', 8, 4.7, 4.9),
('2220032', '2220032', 'Valeria',   'Arango Betancur',  'valeria.arango@icesi.edu.co',   '123456', 6, 4.3, 4.5),
('2220033', '2220033', 'Óscar',     'Calderón Meza',    'oscar.calderon@icesi.edu.co',   '123456', 7, 4.5, 4.7),
('2220034', '2220034', 'Luciana',   'Escobar Pineda',   'luciana.escobar@icesi.edu.co',  '123456', 5, 4.0, 4.3),
('2220035', '2220035', 'Simón',     'Bedoya Flórez',    'simon.bedoya@icesi.edu.co',     '123456', 6, 4.4, 4.6)
ON CONFLICT (id) DO NOTHING;

-- 4b. Insertar en monitor (rol activo)
INSERT INTO sigma.monitor (code, name, last_name, semester, grade_average, grade_course, email, id) VALUES
('2220006', 'Sofía',     'Ramírez Herrera',   6, 4.3, 4.6, 'sofia.ramirez@icesi.edu.co',     '2220006'),
('2220007', 'Diego',     'Castañeda López',   7, 4.5, 4.7, 'diego.castaneda@icesi.edu.co',   '2220007'),
('2220008', 'Valentina', 'Moreno Arias',      5, 4.1, 4.4, 'valentina.moreno@icesi.edu.co',  '2220008'),
('2220009', 'Sebastián', 'Giraldo Torres',    8, 4.6, 4.8, 'sebastian.giraldo@icesi.edu.co', '2220009'),
('2220010', 'Isabela',   'Vargas Pinto',      6, 4.4, 4.5, 'isabela.vargas@icesi.edu.co',    '2220010'),
('2220011', 'Nicolás',   'Ospina Cano',       7, 4.2, 4.5, 'nicolas.ospina@icesi.edu.co',    '2220011'),
('2220012', 'Camila',    'Pedraza Ruiz',      5, 4.0, 4.3, 'camila.pedraza@icesi.edu.co',    '2220012'),
('2220013', 'Mateo',     'Salinas Duque',     6, 4.5, 4.7, 'mateo.salinas@icesi.edu.co',     '2220013'),
('2220014', 'Paula',     'Reyes Guzmán',      7, 4.3, 4.6, 'paula.reyes@icesi.edu.co',       '2220014'),
('2220015', 'Andrés',    'Villamizar Peña',   8, 4.7, 4.9, 'andres.villamizar@icesi.edu.co', '2220015'),
('2220016', 'Laura',     'Cárdenas Mejía',    6, 4.1, 4.4, 'laura.cardenas@icesi.edu.co',    '2220016'),
('2220017', 'Felipe',    'Montoya Arango',    5, 4.4, 4.6, 'felipe.montoya@icesi.edu.co',    '2220017'),
('2220018', 'Daniela',   'Ríos Echeverry',    7, 4.2, 4.5, 'daniela.rios@icesi.edu.co',      '2220018'),
('2220019', 'Julián',    'Palomino Vera',     6, 4.6, 4.8, 'julian.palomino@icesi.edu.co',   '2220019'),
('2220020', 'Sara',      'Bermúdez Cortés',   5, 4.3, 4.5, 'sara.bermudez@icesi.edu.co',     '2220020'),
('2220021', 'Miguel',    'Sandoval Gómez',    8, 4.5, 4.7, 'miguel.sandoval@icesi.edu.co',   '2220021'),
('2220022', 'Natalia',   'Forero Medina',     6, 4.0, 4.3, 'natalia.forero@icesi.edu.co',    '2220022'),
('2220023', 'Samuel',    'Hurtado Zúñiga',    7, 4.4, 4.6, 'samuel.hurtado@icesi.edu.co',    '2220023'),
('2220024', 'Mariana',   'Quintero Lozano',   5, 4.2, 4.4, 'mariana.quintero@icesi.edu.co',  '2220024'),
('2220025', 'Daniel',    'Castaño Patiño',    6, 4.6, 4.8, 'daniel.castano@icesi.edu.co',    '2220025'),
('2220026', 'Alejandra', 'Buitrago Nieto',    7, 4.3, 4.5, 'alejandra.buitrago@icesi.edu.co','2220026'),
('2220027', 'Tomás',     'Acevedo Cifuentes', 8, 4.5, 4.7, 'tomas.acevedo@icesi.edu.co',     '2220027'),
('2220028', 'Manuela',   'Zapata Rincón',     6, 4.1, 4.4, 'manuela.zapata@icesi.edu.co',    '2220028'),
('2220029', 'Esteban',   'Londoño Muñoz',     7, 4.4, 4.6, 'esteban.londono@icesi.edu.co',   '2220029'),
('2220030', 'Gabriela',  'Correa Jiménez',    5, 4.2, 4.5, 'gabriela.correa@icesi.edu.co',   '2220030'),
('2220031', 'Santiago',  'Peña Guerrero',     8, 4.7, 4.9, 'santiago.pena@icesi.edu.co',     '2220031'),
('2220032', 'Valeria',   'Arango Betancur',   6, 4.3, 4.5, 'valeria.arango@icesi.edu.co',    '2220032'),
('2220033', 'Óscar',     'Calderón Meza',     7, 4.5, 4.7, 'oscar.calderon@icesi.edu.co',    '2220033'),
('2220034', 'Luciana',   'Escobar Pineda',    5, 4.0, 4.3, 'luciana.escobar@icesi.edu.co',   '2220034'),
('2220035', 'Simón',     'Bedoya Flórez',     6, 4.4, 4.6, 'simon.bedoya@icesi.edu.co',      '2220035')
ON CONFLICT (code) DO NOTHING;

-- ============================================================
-- 5. VERIFICACIÓN FINAL
-- ============================================================
SELECT '✅ Datos realistas insertados correctamente' AS resultado;
SELECT 'Profesores totales:      ' || COUNT(*) FROM sigma.professor;
SELECT 'Jefes totales:           ' || COUNT(*) FROM sigma.department_head;
SELECT 'Monitores totales:       ' || COUNT(*) FROM sigma.monitor;
SELECT 'Prospects totales:       ' || COUNT(*) FROM sigma.prospect;
SELECT 'Rel. profesor-curso:     ' || COUNT(*) FROM sigma.course_professor;
SELECT 'Rel. jefe-programa:      ' || COUNT(*) FROM sigma.head_professor;

-- ============================================================
-- CREDENCIALES PARA LOGIN
-- ============================================================
-- Todos los monitores:  2220006–2220035  /  123456
-- Todos los profesores: 1005–1015        /  prof123
-- Todos los jefes:      5002–5005        /  jefe123
-- (los anteriores 1001-1004, 5001, 2220001-2220005 siguen vigentes)
-- ============================================================
