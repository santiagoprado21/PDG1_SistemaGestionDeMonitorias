-- ============================================================================
-- HU-011: Datos de prueba para rúbricas y actividades con horarios
-- Fecha: 2025-11-07
-- ============================================================================

SET search_path TO sigma;

DO $$
BEGIN
    RAISE NOTICE '📝 Insertando datos de prueba para HU-011...';
END $$;

-- ============================================================================
-- 1. INSERTAR RÚBRICAS DE EJEMPLO
-- ============================================================================

DO $$
DECLARE
    v_professor_id VARCHAR(20);
BEGIN
    -- Obtener el primer profesor disponible en la BD
    SELECT id INTO v_professor_id FROM professor;
    
    -- Verificar si existe al menos un profesor
    IF v_professor_id IS NULL THEN
        RAISE NOTICE '⚠️  No hay profesores en la BD. No se pueden insertar rúbricas.';
        RETURN;
    END IF;
    
    RAISE NOTICE 'Usando profesor ID: % para crear rúbricas', v_professor_id;

    -- Rúbrica 1: Evaluación de Asistencia a Clases
    INSERT INTO rubric (name, description, total_points, criteria, created_by)
    VALUES (
        'Evaluación de Asistencia a Clases',
        'Rúbrica para evaluar la asistencia y participación del monitor en las clases',
        100,
        '[
            {
                "criterion": "Puntualidad",
                "points": 25,
                "description": "Llega a tiempo a todas las sesiones programadas"
            },
            {
                "criterion": "Participación Activa",
                "points": 30,
                "description": "Participa activamente ayudando a los estudiantes"
            },
            {
                "criterion": "Preparación de Material",
                "points": 25,
                "description": "Tiene preparado el material necesario para la sesión"
            },
            {
                "criterion": "Comunicación Efectiva",
                "points": 20,
                "description": "Se comunica de forma clara con estudiantes y profesor"
            }
        ]'::jsonb,
        v_professor_id
    ) ON CONFLICT DO NOTHING;

    -- Rúbrica 2: Evaluación de Calificación de Trabajos
    INSERT INTO rubric (name, description, total_points, criteria, created_by)
    VALUES (
        'Evaluación de Calificación de Trabajos',
        'Criterios para evaluar la calidad de las calificaciones realizadas por el monitor',
        100,
        '[
            {
                "criterion": "Precisión en Calificación",
                "points": 40,
                "description": "Califica de acuerdo a la rúbrica del profesor"
            },
            {
                "criterion": "Retroalimentación Constructiva",
                "points": 30,
                "description": "Proporciona comentarios útiles a los estudiantes"
            },
            {
                "criterion": "Tiempo de Respuesta",
                "points": 20,
                "description": "Entrega calificaciones en el tiempo acordado"
            },
            {
                "criterion": "Organización",
                "points": 10,
                "description": "Mantiene registro ordenado de las calificaciones"
            }
        ]'::jsonb,
        v_professor_id
    ) ON CONFLICT DO NOTHING;

    -- Rúbrica 3: Evaluación de Tutoría Individual
    INSERT INTO rubric (name, description, total_points, criteria, created_by)
    VALUES (
        'Evaluación de Tutoría Individual',
        'Rúbrica para evaluar la efectividad de las tutorías individuales',
        100,
        '[
            {
                "criterion": "Identificación de Necesidades",
                "points": 25,
                "description": "Identifica correctamente las necesidades del estudiante"
            },
            {
                "criterion": "Explicación Clara",
                "points": 35,
                "description": "Explica los conceptos de forma comprensible"
            },
            {
                "criterion": "Paciencia y Empatía",
                "points": 20,
                "description": "Muestra paciencia y empatía con el estudiante"
            },
            {
                "criterion": "Seguimiento",
                "points": 20,
                "description": "Realiza seguimiento del progreso del estudiante"
            }
        ]'::jsonb,
        v_professor_id
    ) ON CONFLICT DO NOTHING;

    -- Rúbrica 4: Preparación de Material Didáctico
    INSERT INTO rubric (name, description, total_points, criteria, created_by)
    VALUES (
        'Preparación de Material Didáctico',
        'Evaluación de la calidad del material preparado por el monitor',
        100,
        '[
            {
                "criterion": "Relevancia del Contenido",
                "points": 30,
                "description": "El material es relevante para el tema del curso"
            },
            {
                "criterion": "Claridad y Presentación",
                "points": 25,
                "description": "El material está bien presentado y es fácil de entender"
            },
            {
                "criterion": "Creatividad",
                "points": 20,
                "description": "Usa métodos creativos para explicar conceptos"
            },
            {
                "criterion": "Ejemplos Prácticos",
                "points": 25,
                "description": "Incluye ejemplos prácticos y casos de uso"
            }
        ]'::jsonb,
        v_professor_id
    ) ON CONFLICT DO NOTHING;

    RAISE NOTICE '✓ % rúbricas insertadas', (SELECT COUNT(*) FROM rubric);
END $$;

-- ============================================================================
-- 2. ACTUALIZAR ACTIVIDADES EXISTENTES CON HORARIOS
-- ============================================================================

-- Nota: Vamos a actualizar algunas actividades existentes con horarios de ejemplo
-- Solo si hay actividades en la BD

DO $$
DECLARE
    v_activity_count INTEGER;
    v_rubric_asistencia BIGINT;
    v_rubric_calificacion BIGINT;
    v_rubric_tutoria BIGINT;
BEGIN
    -- Contar actividades existentes
    SELECT COUNT(*) INTO v_activity_count FROM activity;
    
    IF v_activity_count > 0 THEN
        -- Obtener IDs de rúbricas (SELECT INTO ya toma solo la primera fila)
        SELECT id INTO v_rubric_asistencia FROM rubric WHERE name = 'Evaluación de Asistencia a Clases';
        SELECT id INTO v_rubric_calificacion FROM rubric WHERE name = 'Evaluación de Calificación de Trabajos';
        SELECT id INTO v_rubric_tutoria FROM rubric WHERE name = 'Evaluación de Tutoría Individual';
        
        -- Actualizar algunas actividades con horarios (ejemplo)
        -- Actividades de tipo "Asistencia a clases" - Lunes y Miércoles 8:00-10:00
        UPDATE activity 
        SET 
            start_time = '08:00:00',
            end_time = '10:00:00',
            duration_hours = 2.0,
            recurrence = 'WEEKLY',
            priority = 'ALTA',
            rubric_id = v_rubric_asistencia
        WHERE id IN (
            SELECT id FROM activity 
            WHERE category = 'Asistencia a clases'
            AND start_time IS NULL
            LIMIT 3
        );
        
        -- Actividades de tipo "Calificación" - Viernes 14:00-17:00
        UPDATE activity 
        SET 
            start_time = '14:00:00',
            end_time = '17:00:00',
            duration_hours = 3.0,
            recurrence = 'WEEKLY',
            priority = 'MEDIA',
            rubric_id = v_rubric_calificacion
        WHERE id IN (
            SELECT id FROM activity
            WHERE category = 'Calificación de trabajos/talleres'
            AND start_time IS NULL
            LIMIT 2
        );
        
        -- Actividades de tipo "Tutoría" - Martes y Jueves 15:00-17:00
        UPDATE activity 
        SET 
            start_time = '15:00:00',
            end_time = '17:00:00',
            duration_hours = 2.0,
            recurrence = 'WEEKLY',
            priority = 'ALTA',
            rubric_id = v_rubric_tutoria
        WHERE id IN (
            SELECT id FROM activity
            WHERE category = 'Tutoría'
            AND start_time IS NULL
            LIMIT 2
        );
        
        RAISE NOTICE '✓ Actividades existentes actualizadas con horarios';
    ELSE
        RAISE NOTICE 'ℹ️  No hay actividades existentes para actualizar';
    END IF;
END $$;

-- ============================================================================
-- 3. INSERTAR ACTIVIDADES DE EJEMPLO CON HORARIOS COMPLETOS
-- ============================================================================

-- Obtener IDs necesarios
DO $$
DECLARE
    v_monitoring_id INTEGER;
    v_professor_id VARCHAR(20);
    v_monitor_code VARCHAR(20);
    v_rubric_asistencia BIGINT;
    v_rubric_tutoria BIGINT;
    v_rubric_material BIGINT;
BEGIN
    -- Obtener monitoring_id de una monitoría existente
    SELECT id INTO v_monitoring_id FROM monitoring WHERE approval_status = 'APROBADA';
    
    -- Si no hay monitorías aprobadas, usar cualquiera
    IF v_monitoring_id IS NULL THEN
        SELECT id INTO v_monitoring_id FROM monitoring;
    END IF;
    
    -- Obtener profesor
    SELECT id INTO v_professor_id FROM professor;
    
    -- Obtener monitor (code es la clave en la relación con activity)
    SELECT code INTO v_monitor_code FROM monitor;
    
    -- Obtener rúbricas
    SELECT id INTO v_rubric_asistencia FROM rubric WHERE name = 'Evaluación de Asistencia a Clases';
    SELECT id INTO v_rubric_tutoria FROM rubric WHERE name = 'Evaluación de Tutoría Individual';
    SELECT id INTO v_rubric_material FROM rubric WHERE name = 'Preparación de Material Didáctico';
    
    IF v_monitoring_id IS NOT NULL AND v_professor_id IS NOT NULL THEN
        -- Actividad 1: Asistencia a clase - Lunes
        INSERT INTO activity (
            name, description, category, 
            creation_date, finish_date,
            start_time, end_time, duration_hours,
            recurrence, priority,
            role_creator, role_responsable,
            monitoring_id, professor_id, monitor_id,
            state, semester,
            rubric_id
        ) VALUES (
            'Apoyo en Clase de Programación I',
            'Asistir y apoyar al profesor durante la clase magistral, ayudando a estudiantes con dudas puntuales',
            'Asistencia a clases',
            CURRENT_DATE,
            CURRENT_DATE + INTERVAL '7 days', -- Próximo lunes
            '08:00:00', '10:00:00', 2.0,
            'WEEKLY', 'ALTA',
            'profesor', 'monitor',
            v_monitoring_id, v_professor_id, v_monitor_code,
            'PENDIENTE', '2025-1',
            v_rubric_asistencia
        );
        
        -- Actividad 2: Tutoría - Martes
        INSERT INTO activity (
            name, description, category,
            creation, finish_date,
            start_time, end_time, duration_hours,
            recurrence, priority,
            role_creator, role_responsable,
            monitoring_id, professor_id, monitor_id,
            state, semester,
            rubric_id
        ) VALUES (
            'Tutoría Grupal - Estructuras de Datos',
            'Sesión de tutoría grupal para resolver dudas sobre árboles binarios y recorridos',
            'Tutoría',
            CURRENT_DATE,
            CURRENT_DATE + INTERVAL '8 days', -- Próximo martes
            '15:00:00', '17:00:00', 2.0,
            'WEEKLY', 'ALTA',
            'profesor', 'monitor',
            v_monitoring_id, v_professor_id, v_monitor_code,
            'PENDIENTE', '2025-1',
            v_rubric_tutoria
        );
        
        -- Actividad 3: Preparación de Material - Miércoles
        INSERT INTO activity (
            name, description, category,
            creation, finish_date,
            start_time, end_time, duration_hours,
            recurrence, priority,
            role_creator, role_responsable,
            monitoring_id, professor_id, monitor_id,
            state, semester,
            rubric_id
        ) VALUES (
            'Preparación de Taller de Grafos',
            'Diseñar y preparar ejercicios prácticos sobre algoritmos de grafos (BFS y DFS)',
            'Preparación de material didáctico',
            CURRENT_DATE,
            CURRENT_DATE + INTERVAL '9 days', -- Próximo miércoles
            '10:00:00', '13:00:00', 3.0,
            'NONE', 'MEDIA',
            'profesor', 'monitor',
            v_monitoring_id, v_professor_id, v_monitor_code,
            'PENDIENTE', '2025-1',
            v_rubric_material
        );
        
        -- Actividad 4: Asistencia a clase - Jueves (CONFLICTO INTENCIONAL para pruebas)
        INSERT INTO activity (
            name, description, category,
            creation, finish_date,
            start_time, end_time, duration_hours,
            recurrence, priority,
            role_creator, role_responsable,
            monitoring_id, professor_id, monitor_id,
            state, semester
        ) VALUES (
            'Apoyo en Laboratorio de Programación',
            'Supervisar y ayudar a estudiantes durante la práctica de laboratorio',
            'Asistencia a clases',
            CURRENT_DATE,
            CURRENT_DATE + INTERVAL '8 days', -- Mismo día que tutoría (conflicto!)
            '16:00:00', '18:00:00', 2.0,
            'WEEKLY', 'ALTA',
            'profesor', 'monitor',
            v_monitoring_id, v_professor_id, v_monitor_code,
            'PENDIENTE', '2025-1',
            NULL
        );
        
        RAISE NOTICE '✓ 4 actividades de ejemplo insertadas con horarios';
    ELSE
        RAISE NOTICE 'ℹ️  No se pueden insertar actividades de ejemplo (faltan datos base)';
    END IF;
END $$;

-- ============================================================================
-- 4. PROBAR FUNCIÓN DE DETECCIÓN DE CONFLICTOS
-- ============================================================================

DO $$
DECLARE
    v_monitor_code VARCHAR(20);
    v_conflict_date DATE;
    v_conflict RECORD;
    v_conflict_count INTEGER := 0;
BEGIN
    -- Obtener un monitor (code es la clave en la relación con activity)
    SELECT code INTO v_monitor_code FROM monitor;
    
    IF v_monitor_code IS NOT NULL THEN
        -- Buscar conflictos en los próximos 14 días
        v_conflict_date := CURRENT_DATE;
        
        RAISE NOTICE '=================================================================';
        RAISE NOTICE '🔍 PROBANDO DETECCIÓN DE CONFLICTOS DE HORARIOS';
        RAISE NOTICE '=================================================================';
        RAISE NOTICE 'Monitor: %', v_monitor_code;
        RAISE NOTICE 'Rango de fechas: % a %', v_conflict_date, v_conflict_date + INTERVAL '14 days';
        RAISE NOTICE '';
        
        FOR i IN 0..14 LOOP
            v_conflict_date := CURRENT_DATE + (i || ' days')::interval;
            
            FOR v_conflict IN 
                SELECT * FROM activity 
                WHERE monitor_id = v_monitor_code 
                    AND finish_date = v_conflict_date 
                    AND start_time IS NOT NULL
                ORDER BY start_time
            LOOP
                -- Buscar conflictos para esta actividad
                FOR v_conflict IN 
                    SELECT * FROM check_activity_time_conflict(
                        (SELECT id FROM monitor WHERE code = v_monitor_code),
                        v_conflict_date,
                        v_conflict.start_time,
                        v_conflict.end_time,
                        v_conflict.id
                    )
                LOOP
                    v_conflict_count := v_conflict_count + 1;
                    RAISE NOTICE '⚠️  CONFLICTO DETECTADO:';
                    RAISE NOTICE '   Fecha: %', v_conflict_date;
                    RAISE NOTICE '   Actividad en conflicto: % (ID: %)', v_conflict.conflicting_activity_name, v_conflict.conflicting_activity_id;
                    RAISE NOTICE '   Horario: % - %', v_conflict.conflicting_start_time, v_conflict.conflicting_end_time;
                    RAISE NOTICE '';
                END LOOP;
            END LOOP;
        END LOOP;
        
        IF v_conflict_count = 0 THEN
            RAISE NOTICE '✓ No se detectaron conflictos de horarios';
        ELSE
            RAISE NOTICE '⚠️  Se detectaron % conflicto(s) de horarios', v_conflict_count;
        END IF;
    END IF;
END $$;

-- ============================================================================
-- 5. VERIFICACIÓN FINAL
-- ============================================================================

DO $$
DECLARE
    v_rubric_count INTEGER;
    v_activities_with_schedule INTEGER;
    v_activities_with_rubric INTEGER;
BEGIN
    SELECT COUNT(*) INTO v_rubric_count FROM rubric;
    SELECT COUNT(*) INTO v_activities_with_schedule FROM activity WHERE start_time IS NOT NULL;
    SELECT COUNT(*) INTO v_activities_with_rubric FROM activity WHERE rubric_id IS NOT NULL;
    
    RAISE NOTICE '=================================================================';
    RAISE NOTICE '✓ DATOS DE PRUEBA HU-011 INSERTADOS';
    RAISE NOTICE '=================================================================';
    RAISE NOTICE 'Rúbricas creadas: %', v_rubric_count;
    RAISE NOTICE 'Actividades con horarios: %', v_activities_with_schedule;
    RAISE NOTICE 'Actividades con rúbrica: %', v_activities_with_rubric;
    RAISE NOTICE '=================================================================';
    RAISE NOTICE '';
    RAISE NOTICE '📊 Consultas útiles para verificar:';
    RAISE NOTICE '';
    RAISE NOTICE '-- Ver todas las rúbricas:';
    RAISE NOTICE 'SELECT id, name, total_points FROM rubric;';
    RAISE NOTICE '';
    RAISE NOTICE '-- Ver actividades con horarios:';
    RAISE NOTICE 'SELECT * FROM v_activity_schedule;';
    RAISE NOTICE '';
    RAISE NOTICE '-- Ver cronograma de un monitor:';
    RAISE NOTICE 'SELECT activity_date, start_time, end_time, name, category FROM v_activity_schedule WHERE monitor_id = ''TU_MONITOR_ID'' ORDER BY activity_date, start_time;';
    RAISE NOTICE '=================================================================';
END $$;

-- ============================================================================
-- FIN DEL SCRIPT
-- ============================================================================

