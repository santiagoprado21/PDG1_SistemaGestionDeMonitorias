-- ================================================================
-- Script para obtener IDs necesarios para las pruebas de HU-010
-- ================================================================
-- Ejecutar este script ANTES de hacer las pruebas con Postman
-- Copiar los IDs resultantes a las variables de Postman
-- ================================================================

SET search_path TO sigma;

\echo '================================================================'
\echo 'IDs NECESARIOS PARA PRUEBAS DE HU-010'
\echo '================================================================'
\echo ''

-- ================================================================
-- 1. PROFESOR
-- ================================================================
\echo '--- 1. PROFESOR (professorId) ---'
SELECT 
    id as "ID",
    name || ' ' || last_name as "Nombre Completo",
    email
FROM professor 
WHERE id IS NOT NULL
LIMIT 5;

\echo ''
\echo 'Copiar el ID del profesor que quieras usar'
\echo 'Ejemplo: PROF001'
\echo ''

-- ================================================================
-- 2. CURSO
-- ================================================================
\echo '--- 2. CURSO (courseId) ---'
SELECT 
    id as "ID",
    name as "Nombre del Curso",
    code as "Código"
FROM course 
WHERE id IS NOT NULL
LIMIT 5;

\echo ''
\echo 'Copiar el ID del curso'
\echo 'Ejemplo: 1'
\echo ''

-- ================================================================
-- 3. FACULTAD (SCHOOL)
-- ================================================================
\echo '--- 3. FACULTAD (schoolId) ---'
SELECT 
    id as "ID",
    name as "Nombre Facultad"
FROM school 
WHERE id IS NOT NULL
LIMIT 5;

\echo ''
\echo 'Copiar el ID de la facultad'
\echo 'Ejemplo: 1'
\echo ''

-- ================================================================
-- 4. PROGRAMA
-- ================================================================
\echo '--- 4. PROGRAMA (programId) ---'
SELECT 
    id as "ID",
    name as "Nombre Programa"
FROM program 
WHERE id IS NOT NULL
LIMIT 5;

\echo ''
\echo 'Copiar el ID del programa'
\echo 'Ejemplo: 1'
\echo ''

-- ================================================================
-- 5. ESTUDIANTES (MONITORS)
-- ================================================================
\echo '--- 5. ESTUDIANTES (student1Id, student2Id, student3Id) ---'
\echo 'Opción A: Desde tabla monitor'
SELECT 
    code as "Código (ID)",
    id_monitor as "ID Monitor",
    name || ' ' || last_name as "Nombre Completo",
    grade_average as "Promedio"
FROM monitor 
WHERE code IS NOT NULL
ORDER BY grade_average DESC
LIMIT 5;

\echo ''
\echo 'Si la tabla monitor está vacía, usar tabla prospect:'
SELECT 
    id as "ID (Código)",
    code as "Código Estudiante",
    name || ' ' || last_name as "Nombre Completo",
    grade_average as "Promedio"
FROM prospect 
WHERE id IS NOT NULL
ORDER BY grade_average DESC
LIMIT 5;

\echo ''
\echo 'Copiar 3 códigos de estudiantes diferentes'
\echo 'Ejemplo: 2020111001, 2020111002, 2020111003'
\echo ''

-- ================================================================
-- 6. JEFE DE DEPARTAMENTO
-- ================================================================
\echo '--- 6. JEFE DE DEPARTAMENTO (deptHeadId) ---'
SELECT 
    dh.id as "ID",
    dh.name || ' ' || dh.last_name as "Nombre Completo",
    p.name as "Programa Asociado"
FROM department_head dh
LEFT JOIN head_program hp ON hp.department_head_id = dh.id
LEFT JOIN program p ON p.id = hp.program_id
WHERE dh.id IS NOT NULL
LIMIT 5;

\echo ''
\echo 'Copiar el ID del jefe de departamento'
\echo 'Ejemplo: DEPT001'
\echo ''

-- ================================================================
-- VERIFICAR RELACIONES (Opcional pero recomendado)
-- ================================================================
\echo '================================================================'
\echo 'VERIFICACIÓN DE RELACIONES'
\echo '================================================================'
\echo ''

-- Verificar que el profesor esté asignado a algún curso
\echo '--- Cursos asignados al profesor ---'
\echo 'Reemplazar PROF001 con el professorId que elegiste:'
SELECT 
    cp.professor_id,
    c.id as course_id,
    c.name as course_name
FROM course_professor cp
JOIN course c ON c.id = cp.course_id
WHERE cp.professor_id = 'PROF001'  -- REEMPLAZAR AQUÍ
LIMIT 5;

\echo ''
\echo 'Si no aparece nada, el profesor no está asignado a cursos.'
\echo 'Necesitas elegir otro profesor o asignar el curso.'
\echo ''

-- Verificar presupuesto disponible
\echo '--- Presupuesto disponible ---'
\echo 'Reemplazar 1 con el programId que elegiste:'
SELECT 
    db.program_id,
    p.name as program_name,
    db.semester,
    db.total_hours as presupuesto_total
FROM department_budget db
JOIN program p ON p.id = db.program_id
WHERE db.program_id = 1  -- REEMPLAZAR AQUÍ
  AND db.semester = '2025-1'
LIMIT 5;

\echo ''
\echo 'Si no aparece nada, no hay presupuesto configurado.'
\echo 'Las validaciones de presupuesto se omitirán.'
\echo ''

-- ================================================================
-- RESUMEN PARA COPIAR
-- ================================================================
\echo '================================================================'
\echo 'RESUMEN - COPIAR ESTOS VALORES A POSTMAN'
\echo '================================================================'
\echo ''
\echo 'Variables de Postman:'
\echo '  professorId:  <ID del profesor elegido>'
\echo '  courseId:     <ID del curso elegido>'
\echo '  schoolId:     <ID de la facultad elegida>'
\echo '  programId:    <ID del programa elegido>'
\echo '  student1Id:   <Código del estudiante 1>'
\echo '  student2Id:   <Código del estudiante 2>'
\echo '  student3Id:   <Código del estudiante 3>'
\echo '  deptHeadId:   <ID del jefe de departamento>'
\echo ''
\echo 'Otras variables se llenan automáticamente al ejecutar requests.'
\echo ''
\echo '================================================================'

-- ================================================================
-- SCRIPT RÁPIDO PARA CREAR DATOS SI NO EXISTEN
-- ================================================================
\echo ''
\echo 'Si no tienes datos suficientes, puedes ejecutar:'
\echo '  \\i 7_hu010_test_data.sql'
\echo '(Pero recuerda ajustar los IDs en ese archivo primero)'
\echo ''

