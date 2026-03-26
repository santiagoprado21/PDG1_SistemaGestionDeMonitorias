CREATE TABLE IF NOT EXISTS sigma.monitor_survey_question (
    id SERIAL PRIMARY KEY,
    question_key VARCHAR(80) NOT NULL UNIQUE,
    statement TEXT NOT NULL,
    category VARCHAR(120) NOT NULL,
    bank_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS sigma.monitor_survey_template (
    id SERIAL PRIMARY KEY,
    name VARCHAR(120) NOT NULL UNIQUE,
    description TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS sigma.monitor_survey_template_question (
    id SERIAL PRIMARY KEY,
    template_id BIGINT NOT NULL REFERENCES sigma.monitor_survey_template(id) ON DELETE CASCADE,
    question_id BIGINT NOT NULL REFERENCES sigma.monitor_survey_question(id) ON DELETE RESTRICT,
    display_order INTEGER NOT NULL,
    CONSTRAINT uk_template_question UNIQUE (template_id, question_id)
);

CREATE TABLE IF NOT EXISTS sigma.monitor_survey_semester_config (
    id SERIAL PRIMARY KEY,
    semester VARCHAR(20) NOT NULL UNIQUE,
    template_id BIGINT REFERENCES sigma.monitor_survey_template(id),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS sigma.monitor_survey_semester_question (
    id SERIAL PRIMARY KEY,
    semester_config_id BIGINT NOT NULL REFERENCES sigma.monitor_survey_semester_config(id) ON DELETE CASCADE,
    question_id BIGINT NOT NULL REFERENCES sigma.monitor_survey_question(id) ON DELETE RESTRICT,
    display_order INTEGER NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT uk_semester_question UNIQUE (semester_config_id, question_id)
);

CREATE TABLE IF NOT EXISTS sigma.monitor_survey_response (
    id SERIAL PRIMARY KEY,
    semester VARCHAR(20) NOT NULL,
    monitoring_id VARCHAR(50),
    monitor_code VARCHAR(50),
    monitor_name VARCHAR(255),
    positive_feedback TEXT,
    improvement_feedback TEXT,
    average_score NUMERIC(4,2),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS sigma.monitor_survey_response_answer (
    id SERIAL PRIMARY KEY,
    response_id BIGINT NOT NULL REFERENCES sigma.monitor_survey_response(id) ON DELETE CASCADE,
    question_id BIGINT NOT NULL REFERENCES sigma.monitor_survey_question(id) ON DELETE RESTRICT,
    score INTEGER NOT NULL CHECK (score BETWEEN 1 AND 7)
);

INSERT INTO sigma.monitor_survey_question (question_key, statement, category, bank_active)
SELECT * FROM (
    VALUES
    ('topic_mastery', 'El monitor demostró dominio de los temas tratados.', 'Apoyo Pedagógico', TRUE),
    ('explanation_clarity', 'Las explicaciones del monitor fueron claras y útiles.', 'Apoyo Pedagógico', TRUE),
    ('doubt_resolution', 'El monitor resolvió mis dudas de manera efectiva.', 'Apoyo Pedagógico', TRUE),
    ('schedule_compliance', 'El monitor cumplió con los horarios establecidos.', 'Disponibilidad y Puntualidad', TRUE),
    ('availability', 'Fue fácil contactar al monitor y asistir a sus sesiones.', 'Disponibilidad y Puntualidad', TRUE),
    ('respectful_attitude', 'El monitor tuvo una actitud respetuosa y paciente.', 'Actitud y Metodología', TRUE),
    ('learning_resources', 'El monitor usó recursos o ejemplos útiles.', 'Actitud y Metodología', TRUE),
    ('perceived_value', 'El apoyo del monitor fue fundamental para mi desempeño.', 'Percepción de Valor', TRUE),
    ('recommendation', 'Recomendaría a este monitor para futuros semestres.', 'Percepción de Valor', TRUE)
) AS seed(question_key, statement, category, bank_active)
WHERE NOT EXISTS (
    SELECT 1 FROM sigma.monitor_survey_question q WHERE q.question_key = seed.question_key
);

INSERT INTO sigma.monitor_survey_template (name, description)
SELECT 'Plantilla base HU-022', 'Plantilla inicial para evaluación de experiencia con monitores'
WHERE NOT EXISTS (
    SELECT 1 FROM sigma.monitor_survey_template WHERE name = 'Plantilla base HU-022'
);

WITH base_template AS (
    SELECT id FROM sigma.monitor_survey_template WHERE name = 'Plantilla base HU-022' LIMIT 1
), base_questions AS (
    SELECT id, ROW_NUMBER() OVER (ORDER BY id) AS rn
    FROM sigma.monitor_survey_question
)
INSERT INTO sigma.monitor_survey_template_question (template_id, question_id, display_order)
SELECT bt.id, bq.id, bq.rn
FROM base_template bt
CROSS JOIN base_questions bq
WHERE NOT EXISTS (
    SELECT 1
    FROM sigma.monitor_survey_template_question tq
    WHERE tq.template_id = bt.id AND tq.question_id = bq.id
);
