-- HU-027 / HU-021
-- Normaliza periodos legacy al formato canonico AAAA-1 o AAAA-2.
-- Ejecutar despues de 16_hu027_professor_survey_bank.sql.

-- 1) Monitoring request: usar start_date cuando exista; en caso contrario usar periodo actual.
WITH current_period AS (
    SELECT to_char(CURRENT_DATE, 'YYYY') || '-' ||
           CASE WHEN EXTRACT(MONTH FROM CURRENT_DATE) <= 6 THEN '1' ELSE '2' END AS period
)
UPDATE sigma.monitoring_request mr
SET semester = COALESCE(
    CASE
        WHEN mr.start_date IS NOT NULL THEN
            to_char(mr.start_date::date, 'YYYY') || '-' ||
            CASE WHEN EXTRACT(MONTH FROM mr.start_date::date) <= 6 THEN '1' ELSE '2' END
    END,
    cp.period
)
FROM current_period cp
WHERE mr.semester IS NULL
   OR btrim(mr.semester) !~ '^\d{4}-[12]$';

-- 2) Monitoring: si tiene request origen valido, heredar periodo de la request.
DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = 'sigma'
          AND table_name = 'monitoring'
          AND column_name = 'monitoring_request_id'
    ) THEN
        EXECUTE $sql$
            UPDATE sigma.monitoring m
            SET semester = btrim(mr.semester)
            FROM sigma.monitoring_request mr
            WHERE m.monitoring_request_id = mr.id
              AND (m.semester IS NULL OR btrim(m.semester) !~ '^\d{4}-[12]$')
              AND mr.semester IS NOT NULL
              AND btrim(mr.semester) ~ '^\d{4}-[12]$'
        $sql$;
    END IF;
END $$;

-- 3) Monitoring: completar invalidos restantes con start_date o periodo actual.
WITH current_period AS (
    SELECT to_char(CURRENT_DATE, 'YYYY') || '-' ||
           CASE WHEN EXTRACT(MONTH FROM CURRENT_DATE) <= 6 THEN '1' ELSE '2' END AS period
)
UPDATE sigma.monitoring m
SET semester = COALESCE(
    CASE
        WHEN m.start_date IS NOT NULL THEN
            to_char(m.start_date::date, 'YYYY') || '-' ||
            CASE WHEN EXTRACT(MONTH FROM m.start_date::date) <= 6 THEN '1' ELSE '2' END
    END,
    cp.period
)
FROM current_period cp
WHERE m.semester IS NULL
   OR btrim(m.semester) !~ '^\d{4}-[12]$';

-- 4) Supervisor evaluation: alinear con monitoring.semester; fallback por created_at o periodo actual.
WITH current_period AS (
    SELECT to_char(CURRENT_DATE, 'YYYY') || '-' ||
           CASE WHEN EXTRACT(MONTH FROM CURRENT_DATE) <= 6 THEN '1' ELSE '2' END AS period
)
UPDATE sigma.supervisor_evaluation se
SET semester = COALESCE(
    CASE
        WHEN m.semester IS NOT NULL AND btrim(m.semester) ~ '^\d{4}-[12]$' THEN btrim(m.semester)
    END,
    CASE
        WHEN se.created_at IS NOT NULL THEN
            to_char(se.created_at::date, 'YYYY') || '-' ||
            CASE WHEN EXTRACT(MONTH FROM se.created_at::date) <= 6 THEN '1' ELSE '2' END
    END,
    cp.period
)
FROM sigma.monitoring m
CROSS JOIN current_period cp
WHERE se.monitoring_id = m.id
  AND (se.semester IS NULL OR btrim(se.semester) !~ '^\d{4}-[12]$');

-- 5) Tablas HU-027: asegurar periodos canonicos.
WITH current_period AS (
    SELECT to_char(CURRENT_DATE, 'YYYY') || '-' ||
           CASE WHEN EXTRACT(MONTH FROM CURRENT_DATE) <= 6 THEN '1' ELSE '2' END AS period
)
UPDATE sigma.professor_survey_template t
SET created_for_semester = cp.period
FROM current_period cp
WHERE t.created_for_semester IS NULL
   OR btrim(t.created_for_semester) !~ '^\d{4}-[12]$';

WITH current_period AS (
    SELECT to_char(CURRENT_DATE, 'YYYY') || '-' ||
           CASE WHEN EXTRACT(MONTH FROM CURRENT_DATE) <= 6 THEN '1' ELSE '2' END AS period
)
UPDATE sigma.professor_survey_semester_config c
SET semester = cp.period
FROM current_period cp
WHERE (c.semester IS NULL OR btrim(c.semester) !~ '^\d{4}-[12]$')
  AND NOT EXISTS (
      SELECT 1
      FROM sigma.professor_survey_semester_config c2
      WHERE c2.semester = cp.period
        AND c2.id <> c.id
  );

-- 6) Constraints para prevenir nuevos periodos invalidos.
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'ck_monitoring_request_period_format') THEN
        ALTER TABLE sigma.monitoring_request
            ADD CONSTRAINT ck_monitoring_request_period_format
            CHECK (semester ~ '^\d{4}-[12]$') NOT VALID;
    END IF;

    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'ck_monitoring_period_format') THEN
        ALTER TABLE sigma.monitoring
            ADD CONSTRAINT ck_monitoring_period_format
            CHECK (semester ~ '^\d{4}-[12]$') NOT VALID;
    END IF;

    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'ck_supervisor_evaluation_period_format') THEN
        ALTER TABLE sigma.supervisor_evaluation
            ADD CONSTRAINT ck_supervisor_evaluation_period_format
            CHECK (semester IS NULL OR semester ~ '^\d{4}-[12]$') NOT VALID;
    END IF;

    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'ck_prof_survey_sem_cfg_period_format') THEN
        ALTER TABLE sigma.professor_survey_semester_config
            ADD CONSTRAINT ck_prof_survey_sem_cfg_period_format
            CHECK (semester ~ '^\d{4}-[12]$') NOT VALID;
    END IF;

    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'ck_prof_survey_template_period_format') THEN
        ALTER TABLE sigma.professor_survey_template
            ADD CONSTRAINT ck_prof_survey_template_period_format
            CHECK (created_for_semester IS NULL OR created_for_semester ~ '^\d{4}-[12]$') NOT VALID;
    END IF;
END $$;

-- 7) Verificacion rapida post-migracion.
SELECT 'monitoring_request_invalid' AS check_name, COUNT(*) AS total
FROM sigma.monitoring_request
WHERE semester IS NULL OR btrim(semester) !~ '^\d{4}-[12]$'

UNION ALL

SELECT 'monitoring_invalid' AS check_name, COUNT(*) AS total
FROM sigma.monitoring
WHERE semester IS NULL OR btrim(semester) !~ '^\d{4}-[12]$'

UNION ALL

SELECT 'supervisor_eval_invalid' AS check_name, COUNT(*) AS total
FROM sigma.supervisor_evaluation
WHERE semester IS NOT NULL AND btrim(semester) !~ '^\d{4}-[12]$'

UNION ALL

SELECT 'prof_template_invalid' AS check_name, COUNT(*) AS total
FROM sigma.professor_survey_template
WHERE created_for_semester IS NOT NULL AND btrim(created_for_semester) !~ '^\d{4}-[12]$'

UNION ALL

SELECT 'prof_sem_cfg_invalid' AS check_name, COUNT(*) AS total
FROM sigma.professor_survey_semester_config
WHERE semester IS NULL OR btrim(semester) !~ '^\d{4}-[12]$';
