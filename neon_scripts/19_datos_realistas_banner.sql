-- ============================================================
-- INSERCIÓN DE DATOS REALISTAS EN BANNER: 30 monitores, 15 profesores, 5 jefes
-- ============================================================
-- Prerequisito: script 18_datos_realistas_30mon_15prof_5jefes.sql ya ejecutado en sigma.
-- Este script replica los mismos usuarios en banner para que el login funcione.
-- ============================================================

SET search_path TO banner, public;

-- ============================================================
-- 1. PROFESORES NUEVOS (1005–1015)
-- ============================================================
INSERT INTO banner.professor (id, name, password) VALUES
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
-- 2. ASIGNACIÓN PROFESOR–CURSO EN BANNER (continúa desde id=10)
-- ============================================================
INSERT INTO banner.course_professor (id, professor_id, course_id) VALUES
(10,  '1005', 1),
(11,  '1005', 2),
(12,  '1005', 5),
(13,  '1006', 3),
(14,  '1006', 4),
(15,  '1007', 6),
(16,  '1007', 8),
(17,  '1007', 7),
(18,  '1008', 9),
(19,  '1008', 10),
(20,  '1009', 11),
(21,  '1009', 12),
(22,  '1010', 11),
(23,  '1010', 13),
(24,  '1011', 13),
(25,  '1011', 14),
(26,  '1012', 15),
(27,  '1012', 16),
(28,  '1013', 15),
(29,  '1013', 16),
(30,  '1014', 1),
(31,  '1014', 2),
(32,  '1015', 9),
(33,  '1015', 6)
ON CONFLICT (id) DO NOTHING;

-- ============================================================
-- 3. JEFES DE DEPARTAMENTO NUEVOS (5002–5005)
-- ============================================================
INSERT INTO banner.department_head (id, name, password) VALUES
('5002', 'Dra. Marina Acosta Bernal',    'jefe123'),
('5003', 'Dr. Tomás Beltrán Cárdenas',   'jefe123'),
('5004', 'Dra. Gloria Peña Londoño',     'jefe123'),
('5005', 'Dr. Augusto Salazar Nieto',    'jefe123')
ON CONFLICT (id) DO NOTHING;

-- ============================================================
-- 4. MONITORES/PROSPECTS (2220006–2220035)
-- ============================================================
INSERT INTO banner.prospect (id, code, name, last_name, email, password, semester, grade_average, grade_course) VALUES
('2220006', '2220006', 'Sofía',     'Ramírez Herrera',   'sofia.ramirez@icesi.edu.co',     '123456', 6, 4.3, 4.6),
('2220007', '2220007', 'Diego',     'Castañeda López',   'diego.castaneda@icesi.edu.co',   '123456', 7, 4.5, 4.7),
('2220008', '2220008', 'Valentina', 'Moreno Arias',      'valentina.moreno@icesi.edu.co',  '123456', 5, 4.1, 4.4),
('2220009', '2220009', 'Sebastián', 'Giraldo Torres',    'sebastian.giraldo@icesi.edu.co', '123456', 8, 4.6, 4.8),
('2220010', '2220010', 'Isabela',   'Vargas Pinto',      'isabela.vargas@icesi.edu.co',    '123456', 6, 4.4, 4.5),
('2220011', '2220011', 'Nicolás',   'Ospina Cano',       'nicolas.ospina@icesi.edu.co',    '123456', 7, 4.2, 4.5),
('2220012', '2220012', 'Camila',    'Pedraza Ruiz',      'camila.pedraza@icesi.edu.co',    '123456', 5, 4.0, 4.3),
('2220013', '2220013', 'Mateo',     'Salinas Duque',     'mateo.salinas@icesi.edu.co',     '123456', 6, 4.5, 4.7),
('2220014', '2220014', 'Paula',     'Reyes Guzmán',      'paula.reyes@icesi.edu.co',       '123456', 7, 4.3, 4.6),
('2220015', '2220015', 'Andrés',    'Villamizar Peña',   'andres.villamizar@icesi.edu.co', '123456', 8, 4.7, 4.9),
('2220016', '2220016', 'Laura',     'Cárdenas Mejía',    'laura.cardenas@icesi.edu.co',    '123456', 6, 4.1, 4.4),
('2220017', '2220017', 'Felipe',    'Montoya Arango',    'felipe.montoya@icesi.edu.co',    '123456', 5, 4.4, 4.6),
('2220018', '2220018', 'Daniela',   'Ríos Echeverry',    'daniela.rios@icesi.edu.co',      '123456', 7, 4.2, 4.5),
('2220019', '2220019', 'Julián',    'Palomino Vera',     'julian.palomino@icesi.edu.co',   '123456', 6, 4.6, 4.8),
('2220020', '2220020', 'Sara',      'Bermúdez Cortés',   'sara.bermudez@icesi.edu.co',     '123456', 5, 4.3, 4.5),
('2220021', '2220021', 'Miguel',    'Sandoval Gómez',    'miguel.sandoval@icesi.edu.co',   '123456', 8, 4.5, 4.7),
('2220022', '2220022', 'Natalia',   'Forero Medina',     'natalia.forero@icesi.edu.co',    '123456', 6, 4.0, 4.3),
('2220023', '2220023', 'Samuel',    'Hurtado Zúñiga',    'samuel.hurtado@icesi.edu.co',    '123456', 7, 4.4, 4.6),
('2220024', '2220024', 'Mariana',   'Quintero Lozano',   'mariana.quintero@icesi.edu.co',  '123456', 5, 4.2, 4.4),
('2220025', '2220025', 'Daniel',    'Castaño Patiño',    'daniel.castano@icesi.edu.co',    '123456', 6, 4.6, 4.8),
('2220026', '2220026', 'Alejandra', 'Buitrago Nieto',    'alejandra.buitrago@icesi.edu.co','123456', 7, 4.3, 4.5),
('2220027', '2220027', 'Tomás',     'Acevedo Cifuentes', 'tomas.acevedo@icesi.edu.co',     '123456', 8, 4.5, 4.7),
('2220028', '2220028', 'Manuela',   'Zapata Rincón',     'manuela.zapata@icesi.edu.co',    '123456', 6, 4.1, 4.4),
('2220029', '2220029', 'Esteban',   'Londoño Muñoz',     'esteban.londono@icesi.edu.co',   '123456', 7, 4.4, 4.6),
('2220030', '2220030', 'Gabriela',  'Correa Jiménez',    'gabriela.correa@icesi.edu.co',   '123456', 5, 4.2, 4.5),
('2220031', '2220031', 'Santiago',  'Peña Guerrero',     'santiago.pena@icesi.edu.co',     '123456', 8, 4.7, 4.9),
('2220032', '2220032', 'Valeria',   'Arango Betancur',   'valeria.arango@icesi.edu.co',    '123456', 6, 4.3, 4.5),
('2220033', '2220033', 'Óscar',     'Calderón Meza',     'oscar.calderon@icesi.edu.co',    '123456', 7, 4.5, 4.7),
('2220034', '2220034', 'Luciana',   'Escobar Pineda',    'luciana.escobar@icesi.edu.co',   '123456', 5, 4.0, 4.3),
('2220035', '2220035', 'Simón',     'Bedoya Flórez',     'simon.bedoya@icesi.edu.co',      '123456', 6, 4.4, 4.6)
ON CONFLICT (id) DO NOTHING;

-- ============================================================
-- 5. VERIFICACIÓN FINAL
-- ============================================================
SELECT '✅ Datos realistas insertados en BANNER correctamente' AS resultado;
SELECT 'Profesores en banner:    ' || COUNT(*) FROM banner.professor;
SELECT 'Jefes en banner:         ' || COUNT(*) FROM banner.department_head;
SELECT 'Prospects en banner:     ' || COUNT(*) FROM banner.prospect;
SELECT 'Rel. profesor-curso:     ' || COUNT(*) FROM banner.course_professor;
