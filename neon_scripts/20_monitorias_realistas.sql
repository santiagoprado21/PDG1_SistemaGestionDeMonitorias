-- ============================================================
-- DATOS REALISTAS DE MONITORÍAS (flujo completo HU-010)
-- ============================================================
-- Prerequisitos: scripts 1–19 ya ejecutados.
--
-- Genera 20 monitorias distribuidas en tres períodos:
--   2025-1 → 8 CERRADAS  (historial completo)
--   2025-2 → 10 APROBADAS (activas, en curso)
--   2026-1 → 2 PENDIENTE_APROBACION (recién creadas)
--
-- Por cada monitoria se crean:
--   1 convocatoria (monitoring_request)
--   3 postulaciones (1 SELECCIONADO + 2 NO_SELECCIONADO)
--   1 registro monitoring
--   1 registro monitoring_monitor
--   4 actividades (estados según el período)
-- ============================================================

SET search_path TO sigma, public;

-- ============================================================
-- 1. CONVOCATORIAS (monitoring_request) IDs 1–20
-- ============================================================
INSERT INTO sigma.monitoring_request
  (id, professor_id, course_id, school_id, program_id,
   requested_hours, justification, semester, start_date, finish_date,
   required_average_grade, required_course_grade, hourly_rate, status, created_at)
VALUES
-- ── 2025-1 ── (→ CERRADAS) ───────────────────────────────
(1,  '1001', 1,  1, 1, 40,
 'Se requiere apoyo en Programación I por alto volumen de estudiantes. El monitor reforzará conceptos básicos y resolverá dudas en horarios extracurriculares.',
 '2025-1', '2025-02-03', '2025-06-20', 4.0, 4.2, 20000, 'APROBADA', '2025-01-20 08:00:00'),

(2,  '1002', 3,  1, 1, 36,
 'Bases de Datos requiere monitor para acompañar prácticas de laboratorio y revisión de proyectos finales del semestre.',
 '2025-1', '2025-02-03', '2025-06-20', 4.0, 4.3, 20000, 'APROBADA', '2025-01-20 09:00:00'),

(3,  '1003', 6,  1, 2, 40,
 'Estadística exige acompañamiento adicional para resolución de talleres y preparación de exámenes parciales.',
 '2025-1', '2025-02-03', '2025-06-20', 4.0, 4.0, 18000, 'APROBADA', '2025-01-21 08:00:00'),

(4,  '1004', 11, 2, 4, 44,
 'Cálculo de una Variable requiere monitor para refuerzo de derivadas e integrales, con alta demanda de asesorías.',
 '2025-1', '2025-02-03', '2025-06-20', 4.1, 4.2, 18000, 'APROBADA', '2025-01-21 09:00:00'),

(5,  '1005', 2,  1, 1, 40,
 'Estructuras de Datos necesita monitor para apoyar en implementaciones de árboles, grafos y algoritmos de ordenamiento.',
 '2025-1', '2025-02-03', '2025-06-20', 4.2, 4.3, 20000, 'APROBADA', '2025-01-22 08:00:00'),

(6,  '1006', 4,  1, 1, 36,
 'Desarrollo Web requiere monitor para soporte técnico en proyectos de React y Node.js de los estudiantes.',
 '2025-1', '2025-02-03', '2025-06-20', 4.0, 4.2, 20000, 'APROBADA', '2025-01-22 09:00:00'),

(7,  '1007', 8,  1, 2, 32,
 'Gestión de Proyectos necesita monitor para acompañamiento en elaboración de planes de trabajo y metodologías ágiles.',
 '2025-1', '2025-02-03', '2025-06-20', 3.9, 4.0, 18000, 'APROBADA', '2025-01-23 08:00:00'),

(8,  '1008', 9,  1, 3, 40,
 'Diseño Experimental requiere monitor para apoyo en diseño de experimentos y análisis estadístico de resultados.',
 '2025-1', '2025-02-03', '2025-06-20', 4.0, 4.1, 18000, 'APROBADA', '2025-01-23 09:00:00'),

-- ── 2025-2 ── (→ APROBADAS, en curso) ────────────────────
(9,  '1001', 2,  1, 1, 40,
 'Estructuras de Datos requiere monitor para apoyar implementaciones y optimizar el desempeño académico del grupo.',
 '2025-2', '2025-07-14', '2025-11-28', 4.2, 4.3, 21000, 'APROBADA', '2025-06-30 08:00:00'),

(10, '1002', 5,  1, 1, 36,
 'Inteligencia Artificial exige acompañamiento en proyectos de ML, implementación de algoritmos y análisis de datasets.',
 '2025-2', '2025-07-14', '2025-11-28', 4.3, 4.5, 22000, 'APROBADA', '2025-06-30 09:00:00'),

(11, '1003', 7,  1, 2, 40,
 'Investigación de Operaciones requiere monitor para talleres de programación lineal, simplex y teoría de colas.',
 '2025-2', '2025-07-14', '2025-11-28', 4.0, 4.1, 18000, 'APROBADA', '2025-07-01 08:00:00'),

(12, '1004', 12, 2, 4, 44,
 'Álgebra Lineal requiere monitor para refuerzo en espacios vectoriales, transformaciones y álgebra matricial.',
 '2025-2', '2025-07-14', '2025-11-28', 4.1, 4.2, 18000, 'APROBADA', '2025-07-01 09:00:00'),

(13, '1005', 1,  1, 1, 40,
 'Programación I requiere monitor para apoyar a estudiantes de primer semestre con fundamentos y resolución de errores.',
 '2025-2', '2025-07-14', '2025-11-28', 4.0, 4.2, 20000, 'APROBADA', '2025-07-02 08:00:00'),

(14, '1006', 3,  1, 1, 36,
 'Bases de Datos requiere monitor para acompañar diseño de esquemas, consultas SQL y normalización.',
 '2025-2', '2025-07-14', '2025-11-28', 4.0, 4.2, 20000, 'APROBADA', '2025-07-02 09:00:00'),

(15, '1007', 6,  1, 2, 40,
 'Estadística necesita monitor para apoyar talleres de probabilidad, distribuciones y pruebas de hipótesis.',
 '2025-2', '2025-07-14', '2025-11-28', 4.0, 4.0, 18000, 'APROBADA', '2025-07-03 08:00:00'),

(16, '1008', 10, 1, 3, 40,
 'Química requiere monitor para acompañar prácticas de laboratorio, seguridad química y análisis de resultados.',
 '2025-2', '2025-07-14', '2025-11-28', 4.0, 4.1, 18000, 'APROBADA', '2025-07-03 09:00:00'),

(17, '1009', 11, 2, 4, 44,
 'Cálculo de una Variable requiere monitor para acompañamiento en límites, derivadas e integrales con énfasis en aplicaciones.',
 '2025-2', '2025-07-14', '2025-11-28', 4.1, 4.3, 18000, 'APROBADA', '2025-07-04 08:00:00'),

(18, '1010', 13, 2, 5, 40,
 'Física Mecánica requiere monitor para refuerzo en cinemática, dinámica y resolución de problemas experimentales.',
 '2025-2', '2025-07-14', '2025-11-28', 4.0, 4.2, 18000, 'APROBADA', '2025-07-04 09:00:00'),

-- ── 2026-1 ── (→ PENDIENTE_APROBACION) ───────────────────
(19, '1011', 14, 2, 5, 40,
 'Electromagnetismo requiere monitor para apoyar el estudio de campos eléctricos, magnéticos y ondas electromagnéticas.',
 '2026-1', '2026-02-02', '2026-06-19', 4.0, 4.2, 20000, 'MONITOR_SELECCIONADO', '2026-01-19 08:00:00'),

(20, '1012', 15, 3, 6, 36,
 'Psicología General requiere monitor para refuerzo en fundamentos del comportamiento humano y teorías psicológicas.',
 '2026-1', '2026-02-02', '2026-06-19', 4.0, 4.0, 18000, 'MONITOR_SELECCIONADO', '2026-01-19 09:00:00')

ON CONFLICT DO NOTHING;

-- ============================================================
-- 2. POSTULACIONES (monitor_application)
--    3 por convocatoria → 1 SELECCIONADO + 2 NO_SELECCIONADO
-- ============================================================
INSERT INTO sigma.monitor_application
  (id, monitoring_request_id, monitor_id, status, motivation_letter, application_date)
VALUES
-- Convocatoria 1 (Prog I, 2025-1)
(1,  1, '2220006','SELECCIONADO',   'Tengo sólidos conocimientos en Programación I y experiencia dando tutorías a compañeros. Puedo apoyar en horarios flexibles.','2025-01-25 10:00:00'),
(2,  1, '2220026','NO_SELECCIONADO','Obtuve una nota sobresaliente en Programación I y quisiera compartir mi experiencia con los nuevos estudiantes.','2025-01-25 11:00:00'),
(3,  1, '2220027','NO_SELECCIONADO','Me apasiona la programación y tengo paciencia para explicar conceptos complejos de forma sencilla.','2025-01-26 09:00:00'),
-- Convocatoria 2 (BD, 2025-1)
(4,  2, '2220007','SELECCIONADO',   'Tengo experiencia en diseño de bases de datos relacionales y proyectos con PostgreSQL y MySQL.','2025-01-25 10:00:00'),
(5,  2, '2220028','NO_SELECCIONADO','Aprobé Bases de Datos con 4.6 y puedo acompañar las prácticas de laboratorio del curso.','2025-01-25 11:00:00'),
(6,  2, '2220029','NO_SELECCIONADO','Tengo conocimientos en SQL avanzado y modelado de datos. Me gustaría aportar al grupo.','2025-01-26 09:00:00'),
-- Convocatoria 3 (Estadística, 2025-1)
(7,  3, '2220008','SELECCIONADO',   'Aprobé Estadística con un 4.5 y he dado asesorías informales a compañeros durante el semestre.','2025-01-26 10:00:00'),
(8,  3, '2220030','NO_SELECCIONADO','Tengo habilidades en análisis estadístico y uso de herramientas como R y Excel para datos.','2025-01-26 11:00:00'),
(9,  3, '2220031','NO_SELECCIONADO','Me interesan los métodos cuantitativos y tengo buenas bases en probabilidad y estadística.','2025-01-27 09:00:00'),
-- Convocatoria 4 (Cálculo, 2025-1)
(10, 4, '2220009','SELECCIONADO',   'Obtuve 4.8 en Cálculo de una Variable. Disfruto enseñar y tengo paciencia para explicar paso a paso.','2025-01-26 10:00:00'),
(11, 4, '2220032','NO_SELECCIONADO','Tengo fuertes habilidades matemáticas y puedo apoyar en la resolución de ejercicios y preparación de exámenes.','2025-01-26 11:00:00'),
(12, 4, '2220033','NO_SELECCIONADO','Me destaqué en Cálculo y creo que puedo ser un apoyo valioso para quienes tienen dificultades.','2025-01-27 09:00:00'),
-- Convocatoria 5 (Estructuras, 2025-1)
(13, 5, '2220010','SELECCIONADO',   'Tengo experiencia implementando árboles, grafos y tablas hash. Puedo ayudar con proyectos prácticos.','2025-01-27 10:00:00'),
(14, 5, '2220034','NO_SELECCIONADO','Me gusta el análisis de algoritmos y tengo conocimiento en Java y Python para las implementaciones del curso.','2025-01-27 11:00:00'),
(15, 5, '2220035','NO_SELECCIONADO','Realicé proyectos de estructuras de datos avanzadas y puedo orientar a compañeros en las entregas.','2025-01-28 09:00:00'),
-- Convocatoria 6 (Dev Web, 2025-1)
(16, 6, '2220011','SELECCIONADO',   'Tengo proyectos reales con React y Node.js. Puedo acompañar desde fundamentos hasta despliegue.','2025-01-27 10:00:00'),
(17, 6, '2220026','NO_SELECCIONADO','Desarrollé una aplicación web completa como proyecto personal y quiero compartir ese conocimiento.','2025-01-27 11:00:00'),
(18, 6, '2220027','NO_SELECCIONADO','Me especializo en frontend y puedo ayudar con HTML, CSS, JavaScript y librerías modernas.','2025-01-28 09:00:00'),
-- Convocatoria 7 (Gestión Proy, 2025-1)
(19, 7, '2220012','SELECCIONADO',   'Tengo certificación en Scrum y experiencia liderando proyectos académicos con metodologías ágiles.','2025-01-28 10:00:00'),
(20, 7, '2220028','NO_SELECCIONADO','Aprobé Gestión de Proyectos con 4.4 y quisiera apoyar en la planificación y seguimiento de proyectos.','2025-01-28 11:00:00'),
(21, 7, '2220029','NO_SELECCIONADO','Tengo conocimientos en MS Project y Trello, y me interesa la gestión de equipos de trabajo.','2025-01-29 09:00:00'),
-- Convocatoria 8 (Diseño Exp, 2025-1)
(22, 8, '2220013','SELECCIONADO',   'Tengo formación en diseño de experimentos DOE y análisis de varianza. Aprobé con 4.5 el curso.','2025-01-28 10:00:00'),
(23, 8, '2220030','NO_SELECCIONADO','Me interesa el diseño experimental aplicado a la ingeniería bioquímica y tengo buenas bases estadísticas.','2025-01-28 11:00:00'),
(24, 8, '2220031','NO_SELECCIONADO','Realicé proyectos de diseño factorial en el laboratorio y puedo orientar a compañeros en el análisis.','2025-01-29 09:00:00'),
-- Convocatoria 9 (Estructuras, 2025-2)
(25, 9, '2220014','SELECCIONADO',   'Completé Estructuras de Datos con 4.7 y tengo experiencia en implementaciones en C++ y Java.','2025-07-05 10:00:00'),
(26, 9, '2220032','NO_SELECCIONADO','Me gustan los algoritmos y las estructuras de datos. Quiero contribuir a mejorar el rendimiento del grupo.','2025-07-05 11:00:00'),
(27, 9, '2220033','NO_SELECCIONADO','Tengo proyectos en GitHub con implementaciones de árboles AVL, B+ y grafos ponderados.','2025-07-06 09:00:00'),
-- Convocatoria 10 (IA, 2025-2)
(28, 10,'2220015','SELECCIONADO',   'Tengo experiencia en Python para ML, uso de scikit-learn y redes neuronales básicas con Keras.','2025-07-05 10:00:00'),
(29, 10,'2220034','NO_SELECCIONADO','Realicé proyectos de clasificación con random forests y me interesa el aprendizaje automático profundo.','2025-07-05 11:00:00'),
(30, 10,'2220035','NO_SELECCIONADO','Tengo conocimiento en análisis de datos y visualización. Puedo apoyar en la parte experimental del curso.','2025-07-06 09:00:00'),
-- Convocatoria 11 (Inv Oper, 2025-2)
(31, 11,'2220016','SELECCIONADO',   'Aprobé Investigación de Operaciones con 4.8 y domino el método simplex, transporte y asignación.','2025-07-06 10:00:00'),
(32, 11,'2220026','NO_SELECCIONADO','Tengo habilidades en modelación matemática y programación lineal entera. Me interesa apoyar en talleres.','2025-07-06 11:00:00'),
(33, 11,'2220027','NO_SELECCIONADO','Uso solver de Excel y AMPL para optimización. Puedo ayudar en la formulación y solución de problemas.','2025-07-07 09:00:00'),
-- Convocatoria 12 (Álgebra, 2025-2)
(34, 12,'2220017','SELECCIONADO',   'Obtuve 4.9 en Álgebra Lineal y tengo habilidad para explicar transformaciones y álgebra matricial de forma visual.','2025-07-06 10:00:00'),
(35, 12,'2220028','NO_SELECCIONADO','Me gustan las matemáticas abstractas y puedo apoyar en demostraciones y resolución de ejercicios.','2025-07-06 11:00:00'),
(36, 12,'2220029','NO_SELECCIONADO','Tengo conocimiento en MATLAB para álgebra lineal computacional y puedo ayudar en la parte aplicada.','2025-07-07 09:00:00'),
-- Convocatoria 13 (Prog I, 2025-2)
(37, 13,'2220018','SELECCIONADO',   'Tengo paciencia para enseñar programación desde cero. Ayudé informalmente a varios compañeros este semestre.','2025-07-07 10:00:00'),
(38, 13,'2220030','NO_SELECCIONADO','Me gusta explicar algoritmos básicos y tengo experiencia con Python y Java para primeros cursos.','2025-07-07 11:00:00'),
(39, 13,'2220031','NO_SELECCIONADO','Aprobé Programación I con 4.4 y creo que puedo ser un buen referente para estudiantes de primer año.','2025-07-08 09:00:00'),
-- Convocatoria 14 (BD, 2025-2)
(40, 14,'2220019','SELECCIONADO',   'Tengo experiencia en bases de datos relacionales, NoSQL y ORMs. Puedo apoyar tanto la teoría como la práctica.','2025-07-07 10:00:00'),
(41, 14,'2220032','NO_SELECCIONADO','Me especializo en optimización de consultas y modelado de datos. Quiero apoyar los laboratorios del curso.','2025-07-07 11:00:00'),
(42, 14,'2220033','NO_SELECCIONADO','Tengo proyectos con PostgreSQL y MongoDB. Puedo apoyar en diseño de esquemas y mejores prácticas.','2025-07-08 09:00:00'),
-- Convocatoria 15 (Estadística, 2025-2)
(43, 15,'2220020','SELECCIONADO',   'Aprobé Estadística con 4.5 y uso R y Python para análisis de datos. Puedo apoyar los talleres prácticos.','2025-07-08 10:00:00'),
(44, 15,'2220034','NO_SELECCIONADO','Tengo formación en estadística inferencial y manejo de herramientas estadísticas para análisis de datos.','2025-07-08 11:00:00'),
(45, 15,'2220035','NO_SELECCIONADO','Me interesa la estadística aplicada y puedo ayudar en la interpretación de resultados y visualización.','2025-07-09 09:00:00'),
-- Convocatoria 16 (Química, 2025-2)
(46, 16,'2220021','SELECCIONADO',   'Obtuve 4.7 en Química y tengo experiencia en análisis cuantitativo y manejo seguro de materiales de laboratorio.','2025-07-08 10:00:00'),
(47, 16,'2220026','NO_SELECCIONADO','Me gusta la química experimental y puedo apoyar en el diseño y análisis de las prácticas de laboratorio.','2025-07-08 11:00:00'),
(48, 16,'2220027','NO_SELECCIONADO','Tengo conocimiento en nomenclatura, reacciones y análisis químico. Puedo acompañar los laboratorios del curso.','2025-07-09 09:00:00'),
-- Convocatoria 17 (Cálculo, 2025-2)
(49, 17,'2220022','SELECCIONADO',   'Aprobé Cálculo con 4.8 y disfruto enseñar. Tengo paciencia para explicar desde lo básico hasta lo avanzado.','2025-07-09 10:00:00'),
(50, 17,'2220028','NO_SELECCIONADO','Tengo habilidades matemáticas sólidas y puedo apoyar a estudiantes con dificultades en cálculo diferencial.','2025-07-09 11:00:00'),
(51, 17,'2220029','NO_SELECCIONADO','Uso herramientas como GeoGebra y Wolfram Alpha para visualizar conceptos de cálculo. Puedo compartirlas.','2025-07-10 09:00:00'),
-- Convocatoria 18 (Física, 2025-2)
(52, 18,'2220023','SELECCIONADO',   'Obtuve 4.6 en Física Mecánica y tengo habilidad para conectar la teoría con aplicaciones del mundo real.','2025-07-09 10:00:00'),
(53, 18,'2220030','NO_SELECCIONADO','Me gusta resolver problemas de cinemática y dinámica. Puedo apoyar a compañeros que tienen dificultades.','2025-07-09 11:00:00'),
(54, 18,'2220031','NO_SELECCIONADO','Tengo experiencia en simulaciones de física con PhET y puedo orientar las prácticas experimentales.','2025-07-10 09:00:00'),
-- Convocatoria 19 (Electromagn, 2026-1)
(55, 19,'2220024','SELECCIONADO',   'Aprobé Electromagnetismo con 4.7 y domino campos, potencial eléctrico y circuitos de corriente alterna.','2026-01-22 10:00:00'),
(56, 19,'2220032','NO_SELECCIONADO','Me interesa la física de ondas y campos. Puedo apoyar en la resolución de problemas del Maxwell.','2026-01-22 11:00:00'),
(57, 19,'2220033','NO_SELECCIONADO','Tengo conocimiento en simulación de circuitos y electromagnética con LTspice y FEMM.','2026-01-23 09:00:00'),
-- Convocatoria 20 (Psicología, 2026-1)
(58, 20,'2220025','SELECCIONADO',   'Aprobé Psicología General con 4.5 y tengo habilidades de comunicación para acompañar el aprendizaje.','2026-01-22 10:00:00'),
(59, 20,'2220034','NO_SELECCIONADO','Me interesa la psicología del aprendizaje y puedo apoyar a los compañeros en la comprensión de teorías.','2026-01-22 11:00:00'),
(60, 20,'2220035','NO_SELECCIONADO','Tengo buenas habilidades de síntesis y puedo elaborar resúmenes y guías de estudio para el curso.','2026-01-23 09:00:00')

ON CONFLICT DO NOTHING;

-- ============================================================
-- 3. MONITORÍAS (monitoring)  IDs 1–20
-- ============================================================
INSERT INTO sigma.monitoring
  (id, school_id, program_id, course_id, start_date, finish_date,
   average_grade, course_grade, semester, professor_id,
   estimated_hours, hourly_rate,
   monitoring_request_id, assigned_monitor_id,
   approval_status, justification,
   approved_by, approval_date,
   -- cierre (solo 2025-1)
   closed_by, closure_date, compliance_percentage,
   completed_activities, total_activities, actual_hours)
VALUES
-- ── 2025-1 CERRADAS ──────────────────────────────────────
(1,  1,1,1,  '2025-02-03','2025-06-20', 4.3,4.6,'2025-1','1001', 40,20000, 1, '2220006','CERRADA',
 'Se requiere apoyo en Programación I por alto volumen de estudiantes.',
 '5001','2025-02-10 10:00:00',
 '5001','2025-06-25 16:00:00', 90, 18,20,38),

(2,  1,1,3,  '2025-02-03','2025-06-20', 4.5,4.7,'2025-1','1002', 36,20000, 2, '2220007','CERRADA',
 'Bases de Datos requiere monitor para acompañar prácticas de laboratorio.',
 '5001','2025-02-10 10:00:00',
 '5001','2025-06-25 16:00:00', 95, 19,20,35),

(3,  1,2,6,  '2025-02-03','2025-06-20', 4.1,4.4,'2025-1','1003', 40,18000, 3, '2220008','CERRADA',
 'Estadística exige acompañamiento adicional para talleres y exámenes.',
 '5001','2025-02-10 10:00:00',
 '5001','2025-06-25 16:00:00', 85, 17,20,34),

(4,  2,4,11, '2025-02-03','2025-06-20', 4.6,4.8,'2025-1','1004', 44,18000, 4, '2220009','CERRADA',
 'Cálculo de una Variable requiere monitor para refuerzo de derivadas e integrales.',
 '5001','2025-02-10 10:00:00',
 '5001','2025-06-25 16:00:00', 100,20,20,44),

(5,  1,1,2,  '2025-02-03','2025-06-20', 4.4,4.5,'2025-1','1005', 40,20000, 5, '2220010','CERRADA',
 'Estructuras de Datos necesita monitor para implementaciones de árboles y grafos.',
 '5002','2025-02-12 09:00:00',
 '5002','2025-06-26 15:00:00', 80, 16,20,32),

(6,  1,1,4,  '2025-02-03','2025-06-20', 4.2,4.5,'2025-1','1006', 36,20000, 6, '2220011','CERRADA',
 'Desarrollo Web requiere monitor para soporte técnico en proyectos.',
 '5002','2025-02-12 09:00:00',
 '5002','2025-06-26 15:00:00', 85, 17,20,31),

(7,  1,2,8,  '2025-02-03','2025-06-20', 4.0,4.3,'2025-1','1007', 32,18000, 7, '2220012','CERRADA',
 'Gestión de Proyectos necesita monitor para metodologías ágiles.',
 '5001','2025-02-11 11:00:00',
 '5001','2025-06-27 14:00:00', 75, 15,20,24),

(8,  1,3,9,  '2025-02-03','2025-06-20', 4.2,4.5,'2025-1','1008', 40,18000, 8, '2220013','CERRADA',
 'Diseño Experimental requiere monitor para análisis estadístico.',
 '5004','2025-02-13 08:00:00',
 '5004','2025-06-27 14:00:00', 90, 18,20,37),

-- ── 2025-2 APROBADAS ─────────────────────────────────────
(9,  1,1,2,  '2025-07-14','2025-11-28', 4.5,4.7,'2025-2','1001', 40,21000, 9,  '2220014','APROBADA',
 'Estructuras de Datos requiere monitor para apoyar implementaciones.',
 '5001','2025-07-21 10:00:00', NULL,NULL,NULL,NULL,NULL,NULL),

(10, 1,1,5,  '2025-07-14','2025-11-28', 4.7,4.9,'2025-2','1002', 36,22000, 10, '2220015','APROBADA',
 'Inteligencia Artificial exige acompañamiento en proyectos de ML.',
 '5001','2025-07-21 10:00:00', NULL,NULL,NULL,NULL,NULL,NULL),

(11, 1,2,7,  '2025-07-14','2025-11-28', 4.6,4.8,'2025-2','1003', 40,18000, 11, '2220016','APROBADA',
 'Investigación de Operaciones requiere monitor para talleres de optimización.',
 '5001','2025-07-22 09:00:00', NULL,NULL,NULL,NULL,NULL,NULL),

(12, 2,4,12, '2025-07-14','2025-11-28', 4.9,4.9,'2025-2','1004', 44,18000, 12, '2220017','APROBADA',
 'Álgebra Lineal requiere monitor para espacios vectoriales y transformaciones.',
 '5002','2025-07-22 09:00:00', NULL,NULL,NULL,NULL,NULL,NULL),

(13, 1,1,1,  '2025-07-14','2025-11-28', 4.4,4.6,'2025-2','1005', 40,20000, 13, '2220018','APROBADA',
 'Programación I requiere monitor para apoyar a estudiantes de primer semestre.',
 '5002','2025-07-23 10:00:00', NULL,NULL,NULL,NULL,NULL,NULL),

(14, 1,1,3,  '2025-07-14','2025-11-28', 4.3,4.5,'2025-2','1006', 36,20000, 14, '2220019','APROBADA',
 'Bases de Datos requiere monitor para acompañar diseño de esquemas y SQL.',
 '5002','2025-07-23 10:00:00', NULL,NULL,NULL,NULL,NULL,NULL),

(15, 1,2,6,  '2025-07-14','2025-11-28', 4.3,4.5,'2025-2','1007', 40,18000, 15, '2220020','APROBADA',
 'Estadística necesita monitor para talleres de probabilidad y pruebas.',
 '5001','2025-07-24 11:00:00', NULL,NULL,NULL,NULL,NULL,NULL),

(16, 1,3,10, '2025-07-14','2025-11-28', 4.5,4.7,'2025-2','1008', 40,18000, 16, '2220021','APROBADA',
 'Química requiere monitor para prácticas de laboratorio y seguridad.',
 '5004','2025-07-24 11:00:00', NULL,NULL,NULL,NULL,NULL,NULL),

(17, 2,4,11, '2025-07-14','2025-11-28', 4.8,4.8,'2025-2','1009', 44,18000, 17, '2220022','APROBADA',
 'Cálculo requiere monitor para límites, derivadas e integrales con aplicaciones.',
 '5002','2025-07-25 08:00:00', NULL,NULL,NULL,NULL,NULL,NULL),

(18, 2,5,13, '2025-07-14','2025-11-28', 4.6,4.6,'2025-2','1010', 40,18000, 18, '2220023','APROBADA',
 'Física Mecánica requiere monitor para cinemática y dinámica.',
 '5002','2025-07-25 08:00:00', NULL,NULL,NULL,NULL,NULL,NULL),

-- ── 2026-1 PENDIENTE_APROBACION ──────────────────────────
(19, 2,5,14, '2026-02-02','2026-06-19', 4.7,4.7,'2026-1','1011', 40,20000, 19, '2220024','PENDIENTE_APROBACION',
 'Electromagnetismo requiere monitor para campos eléctricos y magnéticos.',
 NULL,NULL, NULL,NULL,NULL,NULL,NULL,NULL),

(20, 3,6,15, '2026-02-02','2026-06-19', 4.5,4.5,'2026-1','1012', 36,18000, 20, '2220025','PENDIENTE_APROBACION',
 'Psicología General requiere monitor para fundamentos del comportamiento humano.',
 NULL,NULL, NULL,NULL,NULL,NULL,NULL,NULL)

ON CONFLICT DO NOTHING;

-- ============================================================
-- 4. RELACIÓN MONITORING–MONITOR (monitoring_monitor)
-- ============================================================
INSERT INTO sigma.monitoring_monitor
  (id, estado_seleccion, comentario_decision, fecha_decision, decidido_por, monitoring_id, monitor_id)
VALUES
(1,  'seleccionado','Mejor promedio del grupo','2025-02-08 14:00:00','1001',1, '2220006'),
(2,  'seleccionado','Excelente carta de motivación','2025-02-08 14:00:00','1002',2, '2220007'),
(3,  'seleccionado','Experiencia previa relevante','2025-02-08 14:00:00','1003',3, '2220008'),
(4,  'seleccionado','Mejor promedio en el curso','2025-02-08 14:00:00','1004',4, '2220009'),
(5,  'seleccionado','Conocimiento técnico destacado','2025-02-09 09:00:00','1005',5, '2220010'),
(6,  'seleccionado','Proyectos prácticos demostrados','2025-02-09 09:00:00','1006',6, '2220011'),
(7,  'seleccionado','Certificación en metodologías ágiles','2025-02-09 10:00:00','1007',7, '2220012'),
(8,  'seleccionado','Formación específica en DOE','2025-02-09 10:00:00','1008',8, '2220013'),
(9,  'seleccionado','Mejor perfil para el curso','2025-07-18 10:00:00','1001',9, '2220014'),
(10, 'seleccionado','Experiencia en ML y Python','2025-07-18 10:00:00','1002',10,'2220015'),
(11, 'seleccionado','Dominio del método simplex','2025-07-18 10:00:00','1003',11,'2220016'),
(12, 'seleccionado','Calificación perfecta en el curso','2025-07-18 10:00:00','1004',12,'2220017'),
(13, 'seleccionado','Paciencia y habilidad de comunicación','2025-07-19 09:00:00','1005',13,'2220018'),
(14, 'seleccionado','Experiencia en SQL y NoSQL','2025-07-19 09:00:00','1006',14,'2220019'),
(15, 'seleccionado','Manejo de herramientas estadísticas','2025-07-19 09:00:00','1007',15,'2220020'),
(16, 'seleccionado','Conocimiento en seguridad de laboratorio','2025-07-19 10:00:00','1008',16,'2220021'),
(17, 'seleccionado','Calificación más alta del grupo','2025-07-20 09:00:00','1009',17,'2220022'),
(18, 'seleccionado','Capacidad de conexión teoría-práctica','2025-07-20 09:00:00','1010',18,'2220023'),
(19, 'seleccionado','Dominio de electromagnetismo','2026-01-28 10:00:00','1011',19,'2220024'),
(20, 'seleccionado','Habilidades comunicativas destacadas','2026-01-28 10:00:00','1012',20,'2220025')

ON CONFLICT DO NOTHING;

-- ============================================================
-- 5. ACTIVIDADES (4 por monitoría = 80 actividades)
--    Estados: CERRADA→COMPLETADA | APROBADA→mix | PENDIENTE→PENDIENTE
-- ============================================================
INSERT INTO sigma.activity
  (id, name, creation_date, finish_date, role_creator, role_responsable,
   category, description, monitoring_id, professor_id, monitor_id, state, semester)
VALUES
-- ── Monitoría 1 (CERRADA) ────────────────────────────────
(1,  'Sesión de tutoría grupal','2025-02-10','2025-03-07','P','M','Tutoría','Sesión de tutoría para reforzar fundamentos de algoritmos y pseudocódigo con el grupo completo.',1,'1001','2220006','COMPLETADO','2025-1'),
(2,  'Revisión de trabajos parciales','2025-02-24','2025-03-21','P','M','Evaluación','Revisión y retroalimentación de los trabajos del primer parcial del curso.',1,'1001','2220006','COMPLETADO','2025-1'),
(3,  'Asesoría individualizada','2025-03-10','2025-04-04','M','M','Asesoría','Asesorías personalizadas a estudiantes con bajo rendimiento en ciclos y funciones.',1,NULL,'2220006','COMPLETADO','2025-1'),
(4,  'Preparación material de apoyo','2025-04-07','2025-05-02','M','M','Material','Elaboración de guía de ejercicios resueltos sobre arreglos y matrices para el examen final.',1,NULL,'2220006','COMPLETADO','2025-1'),
-- ── Monitoría 2 (CERRADA) ────────────────────────────────
(5,  'Sesión de tutoría grupal','2025-02-10','2025-03-07','P','M','Tutoría','Introducción a SQL: consultas básicas, filtros y joins para el laboratorio semanal.',2,'1002','2220007','COMPLETADO','2025-1'),
(6,  'Práctica de laboratorio','2025-02-24','2025-03-21','P','M','Laboratorio','Práctica guiada de diseño de esquemas ER y normalización hasta 3FN.',2,'1002','2220007','COMPLETADO','2025-1'),
(7,  'Asesoría individualizada','2025-03-17','2025-04-11','M','M','Asesoría','Asesorías sobre consultas avanzadas, subconsultas y optimización de índices.',2,NULL,'2220007','COMPLETADO','2025-1'),
(8,  'Revisión de proyecto final','2025-04-14','2025-05-09','P','M','Evaluación','Revisión de avances del proyecto integrador de bases de datos relacionales.',2,'1002','2220007','COMPLETADO','2025-1'),
-- ── Monitoría 3 (CERRADA) ────────────────────────────────
(9,  'Taller de probabilidad','2025-02-10','2025-03-07','P','M','Tutoría','Taller de probabilidad condicional, Bayes y distribuciones discretas y continuas.',3,'1003','2220008','COMPLETADO','2025-1'),
(10, 'Revisión de parcial','2025-03-03','2025-03-28','P','M','Evaluación','Revisión colectiva del primer parcial e identificación de errores frecuentes.',3,'1003','2220008','COMPLETADO','2025-1'),
(11, 'Asesoría en R','2025-03-17','2025-04-11','M','M','Laboratorio','Introducción al uso de R para análisis estadístico descriptivo e inferencial.',3,NULL,'2220008','COMPLETADO','2025-1'),
(12, 'Preparación material de apoyo','2025-04-07','2025-05-02','M','M','Material','Resumen de distribuciones de probabilidad y pruebas de hipótesis para examen final.',3,NULL,'2220008','COMPLETADO','2025-1'),
-- ── Monitoría 4 (CERRADA) ────────────────────────────────
(13, 'Taller de derivadas','2025-02-10','2025-03-07','P','M','Tutoría','Resolución de ejercicios de derivadas usando regla de la cadena, producto y cociente.',4,'1004','2220009','COMPLETADO','2025-1'),
(14, 'Asesoría en integrales','2025-03-03','2025-03-28','M','M','Asesoría','Sesión de asesoría sobre integración por sustitución, partes y fracciones parciales.',4,NULL,'2220009','COMPLETADO','2025-1'),
(15, 'Revisión de parcial','2025-03-24','2025-04-18','P','M','Evaluación','Revisión del segundo parcial de Cálculo con análisis de errores por tema.',4,'1004','2220009','COMPLETADO','2025-1'),
(16, 'Guía de series y límites','2025-04-14','2025-05-09','M','M','Material','Elaboración de guía completa de series, sucesiones y límites para preparación del final.',4,NULL,'2220009','COMPLETADO','2025-1'),
-- ── Monitoría 5 (CERRADA) ────────────────────────────────
(17, 'Taller de árboles','2025-02-10','2025-03-07','P','M','Laboratorio','Implementación y análisis de árboles binarios de búsqueda y árboles AVL en Java.',5,'1005','2220010','COMPLETADO','2025-1'),
(18, 'Asesoría en grafos','2025-03-03','2025-03-28','M','M','Asesoría','Asesoría sobre algoritmos de Dijkstra, Prim y Floyd-Warshall con casos de uso.',5,NULL,'2220010','COMPLETADO','2025-1'),
(19, 'Revisión de proyecto','2025-03-24','2025-04-18','P','M','Evaluación','Revisión de avances del proyecto de estructuras de datos del semestre.',5,'1005','2220010','COMPLETADO','2025-1'),
(20, 'Tablas hash y hashing','2025-04-14','2025-05-09','M','M','Material','Taller de implementación de tablas hash con manejo de colisiones por encadenamiento.',5,NULL,'2220010','COMPLETADO','2025-1'),
-- ── Monitoría 6 (CERRADA) ────────────────────────────────
(21, 'Introducción a React','2025-02-10','2025-03-07','P','M','Laboratorio','Sesión introductoria a React: componentes, props, estado y ciclo de vida.',6,'1006','2220011','COMPLETADO','2025-1'),
(22, 'Asesoría en Node.js','2025-03-03','2025-03-28','M','M','Asesoría','Asesoría sobre Express.js, rutas REST y conexión con bases de datos.',6,NULL,'2220011','COMPLETADO','2025-1'),
(23, 'Revisión de proyectos web','2025-03-24','2025-04-18','P','M','Evaluación','Revisión del proyecto integrador de desarrollo web full stack.',6,'1006','2220011','COMPLETADO','2025-1'),
(24, 'Despliegue en la nube','2025-04-14','2025-05-09','M','M','Material','Guía de despliegue de aplicaciones web en Vercel y Render con variables de entorno.',6,NULL,'2220011','COMPLETADO','2025-1'),
-- ── Monitoría 7 (CERRADA) ────────────────────────────────
(25, 'Taller de Scrum','2025-02-10','2025-03-07','P','M','Tutoría','Taller práctico sobre metodología Scrum: sprints, ceremonias y artefactos.',7,'1007','2220012','COMPLETADO','2025-1'),
(26, 'Revisión de planes','2025-03-03','2025-03-28','P','M','Evaluación','Revisión de planes de proyecto y cronogramas de los equipos de trabajo.',7,'1007','2220012','COMPLETADO','2025-1'),
(27, 'Asesoría en gestión de riesgos','2025-03-24','2025-04-18','M','M','Asesoría','Asesoría en identificación, análisis y mitigación de riesgos en proyectos.',7,NULL,'2220012','COMPLETADO','2025-1'),
(28, 'Cierre y lecciones aprendidas','2025-04-14','2025-05-09','M','M','Material','Guía de cierre de proyectos y documentación de lecciones aprendidas.',7,NULL,'2220012','COMPLETADO','2025-1'),
-- ── Monitoría 8 (CERRADA) ────────────────────────────────
(29, 'Diseño factorial DOE','2025-02-10','2025-03-07','P','M','Laboratorio','Sesión sobre diseño factorial completo y análisis de varianza ANOVA.',8,'1008','2220013','COMPLETADO','2025-1'),
(30, 'Asesoría en análisis estadístico','2025-03-03','2025-03-28','M','M','Asesoría','Asesoría sobre análisis de resultados experimentales con R y Minitab.',8,NULL,'2220013','COMPLETADO','2025-1'),
(31, 'Revisión de informes','2025-03-24','2025-04-18','P','M','Evaluación','Revisión y retroalimentación de los informes de laboratorio del semestre.',8,'1008','2220013','COMPLETADO','2025-1'),
(32, 'Diseño de superficie de respuesta','2025-04-14','2025-05-09','M','M','Material','Taller de diseño de superficie de respuesta y optimización de experimentos.',8,NULL,'2220013','COMPLETADO','2025-1'),
-- ── Monitoría 9 (APROBADA) ───────────────────────────────
(33, 'Taller de árboles AVL','2025-07-21','2025-08-15','P','M','Laboratorio','Implementación y rotaciones en árboles AVL y árboles rojo-negro.',9,'1001','2220014','COMPLETADO','2025-2'),
(34, 'Asesoría en grafos','2025-08-18','2025-09-12','M','M','Asesoría','Asesoría sobre algoritmos de camino mínimo y árbol de expansión mínima.',9,NULL,'2220014','COMPLETADO','2025-2'),
(35, 'Revisión primer parcial','2025-09-15','2025-10-10','P','M','Evaluación','Revisión colectiva del primer parcial e identificación de patrones de error.',9,'1001','2220014','EN_PROGRESO','2025-2'),
(36, 'Tablas hash avanzadas','2025-10-13','2025-11-07','M','M','Material','Taller de implementación de tablas hash con open addressing y perfect hashing.',9,NULL,'2220014','PENDIENTE','2025-2'),
-- ── Monitoría 10 (APROBADA) ──────────────────────────────
(37, 'Introducción a ML con sklearn','2025-07-21','2025-08-15','P','M','Laboratorio','Taller de clasificación con scikit-learn: regresión logística y árboles de decisión.',10,'1002','2220015','COMPLETADO','2025-2'),
(38, 'Redes neuronales con Keras','2025-08-18','2025-09-12','M','M','Laboratorio','Implementación de redes neuronales densas con Keras para clasificación de imágenes.',10,NULL,'2220015','COMPLETADO','2025-2'),
(39, 'Revisión de proyectos de ML','2025-09-15','2025-10-10','P','M','Evaluación','Revisión de avances de los proyectos de aprendizaje automático del semestre.',10,'1002','2220015','EN_PROGRESO','2025-2'),
(40, 'NLP y procesamiento de texto','2025-10-13','2025-11-07','M','M','Material','Asesoría en procesamiento de lenguaje natural con spaCy y transformers básicos.',10,NULL,'2220015','PENDIENTE','2025-2'),
-- ── Monitoría 11 (APROBADA) ──────────────────────────────
(41, 'Taller de programación lineal','2025-07-21','2025-08-15','P','M','Tutoría','Taller de formulación y solución de modelos de programación lineal con Simplex.',11,'1003','2220016','COMPLETADO','2025-2'),
(42, 'Transporte y asignación','2025-08-18','2025-09-12','M','M','Asesoría','Asesoría sobre modelos de transporte, asignación y el método de la esquina noroeste.',11,NULL,'2220016','COMPLETADO','2025-2'),
(43, 'Revisión de parcial','2025-09-15','2025-10-10','P','M','Evaluación','Revisión del primer parcial de Investigación de Operaciones con análisis de sensibilidad.',11,'1003','2220016','EN_PROGRESO','2025-2'),
(44, 'Teoría de colas y redes','2025-10-13','2025-11-07','M','M','Material','Guía de modelos de colas M/M/1, M/M/c y redes de Petri.',11,NULL,'2220016','PENDIENTE','2025-2'),
-- ── Monitoría 12 (APROBADA) ──────────────────────────────
(45, 'Taller de espacios vectoriales','2025-07-21','2025-08-15','P','M','Tutoría','Taller sobre subespacios, bases, dimensión y coordenadas en espacios vectoriales.',12,'1004','2220017','COMPLETADO','2025-2'),
(46, 'Transformaciones lineales','2025-08-18','2025-09-12','M','M','Asesoría','Asesoría sobre transformaciones lineales, núcleo, imagen y matrices de transformación.',12,NULL,'2220017','COMPLETADO','2025-2'),
(47, 'Valores y vectores propios','2025-09-15','2025-10-10','P','M','Evaluación','Taller de cálculo de eigenvalores y eigenvectores con aplicaciones en Google PageRank.',12,'1004','2220017','EN_PROGRESO','2025-2'),
(48, 'Descomposición espectral','2025-10-13','2025-11-07','M','M','Material','Guía de descomposición SVD y sus aplicaciones en compresión y sistemas de recomendación.',12,NULL,'2220017','PENDIENTE','2025-2'),
-- ── Monitoría 13 (APROBADA) ──────────────────────────────
(49, 'Fundamentos de algoritmos','2025-07-21','2025-08-15','P','M','Tutoría','Introducción a algoritmos, variables, tipos de datos, condicionales y bucles.',13,'1005','2220018','COMPLETADO','2025-2'),
(50, 'Funciones y recursión','2025-08-18','2025-09-12','P','M','Tutoría','Taller sobre definición de funciones, parámetros, retorno y recursión básica.',13,'1005','2220018','COMPLETADO','2025-2'),
(51, 'Arreglos y matrices','2025-09-15','2025-10-10','M','M','Asesoría','Asesoría sobre arreglos unidimensionales y bidimensionales con ejercicios prácticos.',13,NULL,'2220018','EN_PROGRESO','2025-2'),
(52, 'Proyecto integrador','2025-10-13','2025-11-07','P','M','Evaluación','Revisión y apoyo en el proyecto final de Programación I del semestre.',13,'1005','2220018','PENDIENTE','2025-2'),
-- ── Monitoría 14 (APROBADA) ──────────────────────────────
(53, 'Modelado ER y relacional','2025-07-21','2025-08-15','P','M','Laboratorio','Taller de diseño de diagramas ER, modelo relacional y mapeo entre ambos.',14,'1006','2220019','COMPLETADO','2025-2'),
(54, 'SQL avanzado','2025-08-18','2025-09-12','M','M','Laboratorio','Asesoría en subconsultas, CTEs, window functions y stored procedures.',14,NULL,'2220019','COMPLETADO','2025-2'),
(55, 'Revisión de proyecto BD','2025-09-15','2025-10-10','P','M','Evaluación','Revisión del proyecto integrador de diseño e implementación de base de datos.',14,'1006','2220019','EN_PROGRESO','2025-2'),
(56, 'Bases de datos NoSQL','2025-10-13','2025-11-07','M','M','Material','Introducción a MongoDB, Redis y Cassandra con comparación con RDBMS.',14,NULL,'2220019','PENDIENTE','2025-2'),
-- ── Monitoría 15 (APROBADA) ──────────────────────────────
(57, 'Probabilidad y distribuciones','2025-07-21','2025-08-15','P','M','Tutoría','Taller de distribuciones de probabilidad continua: normal, exponencial y uniforme.',15,'1007','2220020','COMPLETADO','2025-2'),
(58, 'Inferencia estadística','2025-08-18','2025-09-12','M','M','Asesoría','Asesoría sobre intervalos de confianza y pruebas de hipótesis para una y dos muestras.',15,NULL,'2220020','COMPLETADO','2025-2'),
(59, 'Regresión lineal','2025-09-15','2025-10-10','P','M','Laboratorio','Taller de regresión lineal simple y múltiple con análisis de residuos en R.',15,'1007','2220020','EN_PROGRESO','2025-2'),
(60, 'Control estadístico de procesos','2025-10-13','2025-11-07','M','M','Material','Guía de cartas de control Shewhart y análisis de capacidad de procesos.',15,NULL,'2220020','PENDIENTE','2025-2'),
-- ── Monitoría 16 (APROBADA) ──────────────────────────────
(61, 'Seguridad en laboratorio','2025-07-21','2025-08-15','P','M','Laboratorio','Sesión de protocolos de seguridad, EPP y manejo de residuos en el laboratorio.',16,'1008','2220021','COMPLETADO','2025-2'),
(62, 'Análisis cuantitativo','2025-08-18','2025-09-12','M','M','Laboratorio','Asesoría en técnicas de análisis volumétrico, gravimétrico y espectrofotométrico.',16,NULL,'2220021','COMPLETADO','2025-2'),
(63, 'Revisión de informes','2025-09-15','2025-10-10','P','M','Evaluación','Revisión y retroalimentación de los informes de laboratorio de química.',16,'1008','2220021','EN_PROGRESO','2025-2'),
(64, 'Cinética y equilibrio','2025-10-13','2025-11-07','M','M','Material','Guía de cinética química, equilibrio y cálculo de constantes de equilibrio.',16,NULL,'2220021','PENDIENTE','2025-2'),
-- ── Monitoría 17 (APROBADA) ──────────────────────────────
(65, 'Límites y continuidad','2025-07-21','2025-08-15','P','M','Tutoría','Taller de cálculo de límites por definición, regla de L''Hôpital y continuidad.',17,'1009','2220022','COMPLETADO','2025-2'),
(66, 'Técnicas de derivación','2025-08-18','2025-09-12','M','M','Asesoría','Asesoría en reglas de derivación, derivada implícita y aplicaciones de optimización.',17,NULL,'2220022','COMPLETADO','2025-2'),
(67, 'Técnicas de integración','2025-09-15','2025-10-10','P','M','Tutoría','Taller de integración por sustitución trigonométrica y fracciones parciales.',17,'1009','2220022','EN_PROGRESO','2025-2'),
(68, 'Series de Taylor y Maclaurin','2025-10-13','2025-11-07','M','M','Material','Guía de series de potencias, radio de convergencia y aplicaciones en física.',17,NULL,'2220022','PENDIENTE','2025-2'),
-- ── Monitoría 18 (APROBADA) ──────────────────────────────
(69, 'Cinemática y movimiento','2025-07-21','2025-08-15','P','M','Tutoría','Taller de cinemática: MRU, MRUA, caída libre y movimiento parabólico con ejemplos.',18,'1010','2220023','COMPLETADO','2025-2'),
(70, 'Leyes de Newton','2025-08-18','2025-09-12','M','M','Asesoría','Asesoría sobre las tres leyes de Newton, fuerzas de fricción y planos inclinados.',18,NULL,'2220023','COMPLETADO','2025-2'),
(71, 'Trabajo y energía','2025-09-15','2025-10-10','P','M','Laboratorio','Taller de trabajo, energía cinética y potencial, y conservación de energía.',18,'1010','2220023','EN_PROGRESO','2025-2'),
(72, 'Movimiento armónico y ondas','2025-10-13','2025-11-07','M','M','Material','Guía de movimiento armónico simple, péndulo y ondas mecánicas transversales.',18,NULL,'2220023','PENDIENTE','2025-2'),
-- ── Monitoría 19 (PENDIENTE_APROBACION) ──────────────────
(73, 'Electrostática: ley de Coulomb','2026-02-09','2026-03-06','P','M','Tutoría','Taller de fuerza eléctrica, campo eléctrico y potencial con distribuciones de carga.',19,'1011','2220024','PENDIENTE','2026-1'),
(74, 'Corriente y circuitos DC','2026-03-09','2026-04-03','P','M','Laboratorio','Introducción a circuitos de corriente continua, ley de Ohm y leyes de Kirchhoff.',19,'1011','2220024','PENDIENTE','2026-1'),
(75, 'Campo magnético','2026-04-06','2026-05-01','M','M','Asesoría','Asesoría sobre fuerzas magnéticas, inducción y ley de Faraday.',19,NULL,'2220024','PENDIENTE','2026-1'),
(76, 'Ondas electromagnéticas','2026-05-05','2026-05-29','M','M','Material','Guía de ecuaciones de Maxwell y propagación de ondas electromagnéticas.',19,NULL,'2220024','PENDIENTE','2026-1'),
-- ── Monitoría 20 (PENDIENTE_APROBACION) ──────────────────
(77, 'Introducción a la psicología','2026-02-09','2026-03-06','P','M','Tutoría','Sesión de repaso de historia de la psicología, escuelas y métodos de investigación.',20,'1012','2220025','PENDIENTE','2026-1'),
(78, 'Psicología del desarrollo','2026-03-09','2026-04-03','P','M','Tutoría','Taller sobre etapas del desarrollo humano según Piaget, Erikson y Vygotsky.',20,'1012','2220025','PENDIENTE','2026-1'),
(79, 'Psicología cognitiva','2026-04-06','2026-05-01','M','M','Asesoría','Asesoría sobre procesos cognitivos: memoria, percepción, atención y lenguaje.',20,NULL,'2220025','PENDIENTE','2026-1'),
(80, 'Psicología social','2026-05-05','2026-05-29','M','M','Material','Guía de influencia social, actitudes, cognición social y dinámica de grupos.',20,NULL,'2220025','PENDIENTE','2026-1')

ON CONFLICT DO NOTHING;

-- ============================================================
-- 6. ACTUALIZAR SECUENCIAS (para que próximos INSERTs no colisionen)
-- ============================================================
SELECT setval('sigma.monitoring_request_id_seq',        (SELECT MAX(id) FROM sigma.monitoring_request));
SELECT setval('sigma.monitor_application_id_seq',       (SELECT MAX(id) FROM sigma.monitor_application));
SELECT setval('sigma.monitoring_id_seq',                (SELECT MAX(id) FROM sigma.monitoring));
SELECT setval('sigma.monitoring_monitor_id_seq',        (SELECT MAX(id) FROM sigma.monitoring_monitor));
SELECT setval('sigma.activity_id_seq',                  (SELECT MAX(id) FROM sigma.activity));

-- ============================================================
-- 7. VERIFICACIÓN FINAL
-- ============================================================
SELECT '✅ Monitorias realistas insertadas correctamente' AS resultado;
SELECT 'Convocatorias:           ' || COUNT(*) FROM sigma.monitoring_request;
SELECT 'Postulaciones:           ' || COUNT(*) FROM sigma.monitor_application;
SELECT 'Monitorias:              ' || COUNT(*) FROM sigma.monitoring;
SELECT 'Relaciones mon-monitor:  ' || COUNT(*) FROM sigma.monitoring_monitor;
SELECT 'Actividades:             ' || COUNT(*) FROM sigma.activity;
SELECT '' AS espacio;
SELECT 'Por estado de monitoria:' AS detalle;
SELECT approval_status, COUNT(*) FROM sigma.monitoring GROUP BY approval_status;
SELECT '' AS espacio2;
SELECT 'Por estado de actividad:' AS detalle2;
SELECT state, COUNT(*) FROM sigma.activity GROUP BY state;
