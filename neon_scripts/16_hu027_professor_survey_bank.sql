CREATE TABLE IF NOT EXISTS sigma.professor_survey_question (
    id SERIAL PRIMARY KEY,
    question_key VARCHAR(80) NOT NULL UNIQUE,
    statement TEXT NOT NULL,
    category VARCHAR(120) NOT NULL,
    bank_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS sigma.professor_survey_template (
    id SERIAL PRIMARY KEY,
    name VARCHAR(120) NOT NULL UNIQUE,
    description TEXT,
    created_for_semester VARCHAR(20),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS sigma.professor_survey_template_question (
    id SERIAL PRIMARY KEY,
    template_id BIGINT NOT NULL REFERENCES sigma.professor_survey_template(id) ON DELETE CASCADE,
    question_id BIGINT NOT NULL REFERENCES sigma.professor_survey_question(id) ON DELETE RESTRICT,
    display_order INTEGER NOT NULL,
    CONSTRAINT uk_professor_template_question UNIQUE (template_id, question_id)
);

CREATE TABLE IF NOT EXISTS sigma.professor_survey_semester_config (
    id SERIAL PRIMARY KEY,
    semester VARCHAR(20) NOT NULL UNIQUE,
    template_id BIGINT REFERENCES sigma.professor_survey_template(id),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS sigma.professor_survey_semester_question (
    id SERIAL PRIMARY KEY,
    semester_config_id BIGINT NOT NULL REFERENCES sigma.professor_survey_semester_config(id) ON DELETE CASCADE,
    question_id BIGINT NOT NULL REFERENCES sigma.professor_survey_question(id) ON DELETE RESTRICT,
    display_order INTEGER NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT uk_professor_semester_question UNIQUE (semester_config_id, question_id)
);

CREATE TABLE IF NOT EXISTS sigma.supervisor_evaluation_answer (
    id SERIAL PRIMARY KEY,
    evaluation_id BIGINT NOT NULL REFERENCES sigma.supervisor_evaluation(id) ON DELETE CASCADE,
    question_id BIGINT NOT NULL REFERENCES sigma.professor_survey_question(id) ON DELETE RESTRICT,
    display_order INTEGER NOT NULL,
    score INTEGER NOT NULL CHECK (score BETWEEN 1 AND 7),
    CONSTRAINT uk_supervisor_eval_answer UNIQUE (evaluation_id, question_id)
);

INSERT INTO sigma.professor_survey_question (question_key, statement, category, bank_active)
SELECT * FROM (
    VALUES
    ('guidance_clarity', 'El profesor proporciono instrucciones y objetivos claros para el desarrollo de mis actividades.', 'Orientacion y expectativas', TRUE),
    ('role_expectations', 'Las expectativas del profesor sobre mi rol y responsabilidades estuvieron bien definidas desde el inicio.', 'Orientacion y expectativas', TRUE),
    ('availability_disposition', 'El profesor mostro disposicion constante para atenderme cuando necesite resolver dudas o problemas.', 'Acompanamiento y disponibilidad', TRUE),
    ('support_timeliness', 'El acompanamiento brindado por el profesor fue suficiente y oportuno durante todo el semestre.', 'Acompanamiento y disponibilidad', TRUE),
    ('feedback_constructive', 'La retroalimentacion que recibi sobre mi trabajo fue constructiva y me ayudo a mejorar.', 'Retroalimentacion y evaluacion', TRUE),
    ('feedback_fairness', 'El profesor evaluo mi desempeno de manera justa y basada en los criterios acordados.', 'Retroalimentacion y evaluacion', TRUE),
    ('respectful_treatment', 'El trato del profesor hacia mi fue siempre respetuoso, profesional y cordial.', 'Relacion y clima', TRUE),
    ('trust_environment', 'El profesor fomento un ambiente de confianza que me permitio expresar mis ideas o dificultades.', 'Relacion y clima', TRUE)
) AS seed(question_key, statement, category, bank_active)
WHERE NOT EXISTS (
    SELECT 1 FROM sigma.professor_survey_question q WHERE q.question_key = seed.question_key
);

INSERT INTO sigma.professor_survey_template (name, description, created_for_semester)
WITH current_period AS (
    SELECT to_char(CURRENT_DATE, 'YYYY') || '-' ||
           CASE WHEN EXTRACT(MONTH FROM CURRENT_DATE) <= 6 THEN '1' ELSE '2' END AS period
)
SELECT 'Plantilla base HU-021', 'Plantilla inicial para evaluacion de profesores supervisores', cp.period
FROM current_period cp
WHERE NOT EXISTS (
    SELECT 1 FROM sigma.professor_survey_template WHERE name = 'Plantilla base HU-021'
);

WITH current_period AS (
    SELECT to_char(CURRENT_DATE, 'YYYY') || '-' ||
           CASE WHEN EXTRACT(MONTH FROM CURRENT_DATE) <= 6 THEN '1' ELSE '2' END AS period
)
UPDATE sigma.professor_survey_template t
SET created_for_semester = cp.period
FROM current_period cp
WHERE t.created_for_semester IS NULL
   OR t.created_for_semester !~ '^\\d{4}-[12]$';

WITH base_template AS (
    SELECT id FROM sigma.professor_survey_template WHERE name = 'Plantilla base HU-021' LIMIT 1
),
ordered_questions AS (
    SELECT id,
           ROW_NUMBER() OVER (
               ORDER BY CASE question_key
                   WHEN 'guidance_clarity' THEN 1
                   WHEN 'role_expectations' THEN 2
                   WHEN 'availability_disposition' THEN 3
                   WHEN 'support_timeliness' THEN 4
                   WHEN 'feedback_constructive' THEN 5
                   WHEN 'feedback_fairness' THEN 6
                   WHEN 'respectful_treatment' THEN 7
                   WHEN 'trust_environment' THEN 8
                   ELSE 999 END,
               id
           ) AS rn
    FROM sigma.professor_survey_question
)
INSERT INTO sigma.professor_survey_template_question (template_id, question_id, display_order)
SELECT bt.id, oq.id, oq.rn
FROM base_template bt
CROSS JOIN ordered_questions oq
WHERE NOT EXISTS (
    SELECT 1
    FROM sigma.professor_survey_template_question tq
    WHERE tq.template_id = bt.id AND tq.question_id = oq.id
);

WITH existing_config AS (
    SELECT id
    FROM sigma.professor_survey_semester_config
    LIMIT 1
),
default_semester AS (
    SELECT to_char(CURRENT_DATE, 'YYYY') || '-' ||
           CASE WHEN EXTRACT(MONTH FROM CURRENT_DATE) <= 6 THEN '1' ELSE '2' END AS semester
),
base_template AS (
    SELECT id FROM sigma.professor_survey_template WHERE name = 'Plantilla base HU-021' LIMIT 1
)
INSERT INTO sigma.professor_survey_semester_config (semester, template_id, active)
SELECT ds.semester, bt.id, TRUE
FROM default_semester ds
CROSS JOIN base_template bt
WHERE NOT EXISTS (SELECT 1 FROM existing_config)
  AND NOT EXISTS (
      SELECT 1 FROM sigma.professor_survey_semester_config c WHERE c.semester = ds.semester
  );

WITH current_period AS (
        SELECT to_char(CURRENT_DATE, 'YYYY') || '-' ||
                     CASE WHEN EXTRACT(MONTH FROM CURRENT_DATE) <= 6 THEN '1' ELSE '2' END AS period
)
UPDATE sigma.professor_survey_semester_config c
SET semester = cp.period
FROM current_period cp
WHERE (c.semester IS NULL OR c.semester !~ '^\\d{4}-[12]$')
    AND NOT EXISTS (
            SELECT 1
            FROM sigma.professor_survey_semester_config c2
            WHERE c2.semester = cp.period
                AND c2.id <> c.id
    );

WITH config_to_fill AS (
    SELECT c.id
    FROM sigma.professor_survey_semester_config c
    WHERE c.active = TRUE
    ORDER BY c.updated_at DESC, c.id DESC
    LIMIT 1
),
question_order AS (
    SELECT tq.question_id, tq.display_order
    FROM sigma.professor_survey_template_question tq
    JOIN sigma.professor_survey_semester_config c ON c.template_id = tq.template_id
    WHERE c.id = (SELECT id FROM config_to_fill)
)
INSERT INTO sigma.professor_survey_semester_question (semester_config_id, question_id, display_order, active)
SELECT (SELECT id FROM config_to_fill), qo.question_id, qo.display_order, TRUE
FROM question_order qo
WHERE (SELECT id FROM config_to_fill) IS NOT NULL
  AND NOT EXISTS (
      SELECT 1
      FROM sigma.professor_survey_semester_question sq
      WHERE sq.semester_config_id = (SELECT id FROM config_to_fill)
        AND sq.question_id = qo.question_id
  );
