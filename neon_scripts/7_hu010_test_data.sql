-- ================================================================
-- HU-010: Datos de prueba para testear el nuevo flujo
-- ================================================================
-- Este script inserta datos de ejemplo para probar:
-- 1. Crear convocatorias
-- 2. Postulaciones de estudiantes
-- 3. Selección de monitor
-- 4. Aprobación de monitoría
-- ================================================================

SET search_path TO sigma;

-- ================================================================
-- NOTA: Ajusta los IDs según los datos existentes en tu base de datos
-- Estos son valores de ejemplo que debes reemplazar
-- ================================================================

-- ================================================================
-- 1. CREAR CONVOCATORIAS DE PRUEBA
-- ================================================================
-- IMPORTANTE: Reemplaza los IDs con valores reales de tu BD:
-- - professor_id: ID de profesor existente
-- - course_id: ID de curso existente
-- - school_id: ID de facultad existente
-- - program_id: ID de programa existente

-- Convocatoria 1: Cálculo I - Semestre 2025-1
INSERT INTO monitoring_request (
    professor_id,
    course_id,
    school_id,
    program_id,
    requested_hours,
    justification,
    semester,
    start_date,
    finish_date,
    required_average_grade,
    required_course_grade,
    hourly_rate,
    status
) VALUES (
    'PROF001',                              -- Reemplazar con ID real
    1,                                      -- Reemplazar con ID real
    1,                                      -- Reemplazar con ID real
    1,                                      -- Reemplazar con ID real
    40,
    'Se requiere monitor para Cálculo I debido al alto número de estudiantes inscritos y la necesidad de refuerzo en temas fundamentales. El monitor apoyará con tutorías y resolución de dudas.',
    '2025-1',
    '2025-02-01',
    '2025-06-30',
    4.0,
    4.5,
    20000.0,
    'CONVOCATORIA_ABIERTA'
);

-- Convocatoria 2: Programación Orientada a Objetos - Semestre 2025-1
INSERT INTO monitoring_request (
    professor_id,
    course_id,
    school_id,
    program_id,
    requested_hours,
    justification,
    semester,
    start_date,
    finish_date,
    required_average_grade,
    required_course_grade,
    hourly_rate,
    status
) VALUES (
    'PROF002',                              -- Reemplazar con ID real
    2,                                      -- Reemplazar con ID real
    1,                                      -- Reemplazar con ID real
    1,                                      -- Reemplazar con ID real
    35,
    'Monitor necesario para apoyo en laboratorios de programación y resolución de proyectos prácticos.',
    '2025-1',
    '2025-02-01',
    '2025-06-30',
    4.2,
    4.5,
    22000.0,
    'CONVOCATORIA_ABIERTA'
);

-- ================================================================
-- 2. POSTULACIONES DE ESTUDIANTES
-- ================================================================
-- IMPORTANTE: Reemplaza monitor_id con códigos reales de estudiantes
-- Los monitores deben existir en la tabla monitor o prospect

-- Estudiante 1 se postula a Convocatoria 1
INSERT INTO monitor_application (
    monitoring_request_id,
    monitor_id,
    status,
    motivation_letter
) VALUES (
    1,                                      -- ID de convocatoria creada arriba
    '2020111001',                          -- Reemplazar con código real
    'POSTULADO',
    'Me gustaría apoyar en el curso de Cálculo I porque tengo buena experiencia con el contenido y me apasiona ayudar a otros estudiantes a comprender mejor las matemáticas.'
);

-- Estudiante 2 se postula a Convocatoria 1
INSERT INTO monitor_application (
    monitoring_request_id,
    monitor_id,
    status,
    motivation_letter
) VALUES (
    1,                                      -- ID de convocatoria creada arriba
    '2020111002',                          -- Reemplazar con código real
    'POSTULADO',
    'Estoy muy interesado en esta monitoría porque obtuve excelentes calificaciones en Cálculo I y tengo experiencia dando tutorías a compañeros.'
);

-- Estudiante 3 se postula a Convocatoria 1
INSERT INTO monitor_application (
    monitoring_request_id,
    monitor_id,
    status,
    motivation_letter
) VALUES (
    1,                                      -- ID de convocatoria creada arriba
    '2020111003',                          -- Reemplazar con código real
    'POSTULADO',
    'Me encantaría ser monitor de Cálculo I. Tengo habilidades de comunicación y paciencia para explicar conceptos complejos.'
);

-- Estudiante 1 se postula también a Convocatoria 2
INSERT INTO monitor_application (
    monitoring_request_id,
    monitor_id,
    status,
    motivation_letter
) VALUES (
    2,                                      -- ID de convocatoria 2
    '2020111001',                          -- Reemplazar con código real
    'POSTULADO',
    'Tengo sólidos conocimientos en POO y he trabajado en varios proyectos personales. Me gustaría compartir mi experiencia con otros estudiantes.'
);

-- ================================================================
-- 3. EJEMPLO DE SELECCIÓN DE MONITOR (Simular flujo completo)
-- ================================================================
-- Este bloque simula lo que haría el servicio cuando un profesor
-- selecciona un monitor
-- NOTA: Normalmente esto lo hace el backend, pero aquí lo mostramos
-- para fines de prueba

-- Comentado porque normalmente lo hace el servicio:
/*
-- 3.1: Marcar la postulación seleccionada
UPDATE monitor_application 
SET status = 'SELECCIONADO',
    notes = 'Seleccionado por su excelente promedio y experiencia'
WHERE id = 1;  -- ID de la postulación seleccionada

-- 3.2: Marcar las demás como NO_SELECCIONADO
UPDATE monitor_application 
SET status = 'NO_SELECCIONADO'
WHERE monitoring_request_id = 1 
  AND id != 1;

-- 3.3: Actualizar estado de la convocatoria
UPDATE monitoring_request 
SET status = 'MONITOR_SELECCIONADO',
    updated_at = CURRENT_TIMESTAMP
WHERE id = 1;

-- 3.4: Crear la monitoría oficial (el servicio hace esto automáticamente)
INSERT INTO monitoring (
    school_id,
    program_id,
    course_id,
    start,
    finish,
    average_grade,
    course_grade,
    semester,
    professor_id,
    estimated_hours,
    hourly_rate,
    monitoring_request_id,
    assigned_monitor_id,
    approval_status,
    justification
)
SELECT 
    mr.school_id,
    mr.program_id,
    mr.course_id,
    mr.start_date,
    mr.finish_date,
    mr.required_average_grade,
    mr.required_course_grade,
    mr.semester,
    mr.professor_id,
    mr.requested_hours,
    mr.hourly_rate,
    mr.id,
    ma.monitor_id,
    'PENDIENTE_APROBACION',
    mr.justification
FROM monitoring_request mr
JOIN monitor_application ma ON ma.monitoring_request_id = mr.id
WHERE mr.id = 1 
  AND ma.status = 'SELECCIONADO';

-- 3.5: Actualizar convocatoria a PENDIENTE_APROBACION
UPDATE monitoring_request 
SET status = 'PENDIENTE_APROBACION',
    updated_at = CURRENT_TIMESTAMP
WHERE id = 1;
*/

-- ================================================================
-- VERIFICACIÓN DE DATOS DE PRUEBA
-- ================================================================
DO $$
DECLARE
    convocatorias_count INTEGER;
    postulaciones_count INTEGER;
BEGIN
    RAISE NOTICE '=== VERIFICACIÓN DE DATOS DE PRUEBA ===';
    
    -- Contar convocatorias
    SELECT COUNT(*) INTO convocatorias_count FROM monitoring_request;
    RAISE NOTICE 'Convocatorias creadas: %', convocatorias_count;
    
    -- Contar postulaciones
    SELECT COUNT(*) INTO postulaciones_count FROM monitor_application;
    RAISE NOTICE 'Postulaciones creadas: %', postulaciones_count;
    
    -- Mostrar convocatorias abiertas
    RAISE NOTICE '--- Convocatorias Abiertas ---';
    FOR i IN (
        SELECT 
            mr.id,
            mr.semester,
            c.name as course_name,
            COUNT(ma.id) as num_postulantes
        FROM monitoring_request mr
        LEFT JOIN course c ON c.id = mr.course_id
        LEFT JOIN monitor_application ma ON ma.monitoring_request_id = mr.id
        WHERE mr.status = 'CONVOCATORIA_ABIERTA'
        GROUP BY mr.id, mr.semester, c.name
    ) LOOP
        RAISE NOTICE 'ID: %, Curso: %, Semestre: %, Postulantes: %', 
            i.id, i.course_name, i.semester, i.num_postulantes;
    END LOOP;
    
    RAISE NOTICE '======================================';
END $$;

-- ================================================================
-- CONSULTAS ÚTILES PARA VERIFICAR
-- ================================================================

-- Ver todas las convocatorias con postulantes
/*
SELECT 
    mr.id,
    c.name as curso,
    p.name as profesor,
    mr.semester,
    mr.status,
    mr.requested_hours,
    COUNT(ma.id) as num_postulantes
FROM monitoring_request mr
LEFT JOIN course c ON c.id = mr.course_id
LEFT JOIN professor p ON p.id = mr.professor_id
LEFT JOIN monitor_application ma ON ma.monitoring_request_id = mr.id
GROUP BY mr.id, c.name, p.name, mr.semester, mr.status, mr.requested_hours
ORDER BY mr.created_at DESC;
*/

-- Ver postulantes de una convocatoria específica
/*
SELECT 
    ma.id,
    m.name || ' ' || m.last_name as estudiante,
    ma.status,
    ma.application_date,
    LEFT(ma.motivation_letter, 100) as motivacion
FROM monitor_application ma
JOIN monitor m ON m.code = ma.monitor_id
WHERE ma.monitoring_request_id = 1
ORDER BY ma.application_date;
*/

-- Ver monitorías pendientes de aprobación
/*
SELECT 
    m.id,
    c.name as curso,
    p.name as profesor,
    mon.name || ' ' || mon.last_name as monitor_asignado,
    m.approval_status,
    LEFT(m.justification, 100) as justificacion
FROM monitoring m
JOIN course c ON c.id = m.course_id
JOIN professor p ON p.id = m.professor_id
LEFT JOIN monitor mon ON mon.code = m.assigned_monitor_id
WHERE m.approval_status = 'PENDIENTE_APROBACION'
ORDER BY m.id DESC;
*/

