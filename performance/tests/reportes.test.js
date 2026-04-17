/**
 * SIGMA-PERF-010 / HU2-265 — Rendimiento de plan de actividades, rúbricas y reportes
 *
 * Mide la latencia y tasa de éxito de los endpoints más usados y pesados
 * relacionados con el seguimiento académico de monitorías.
 *
 * Endpoints cubiertos:
 *   1. GET /rubric/professor/{professorId}                         — Rúbricas por profesor
 *   2. GET /activity/findAll/{professorId}/professor               — Plan de actividades (profesor)
 *   3. GET /activity/findAll/{monitorId}/monitor                   — Plan de actividades (monitor)
 *   4. GET /monitoring/getMonitorsReport/{professorId}/professor    — Reporte de monitores
 *   5. GET /monitoring/getProfessorReport/{professorId}            — Reporte de profesor
 *   6. GET /monitoring/getCategoriesReport/professor/{professorId} — Reporte de categorías
 *   7. GET /monitoring/getAttendanceReport/professor/{professorId} — Reporte de asistencia
 *
 * Umbrales diferenciados (SIGMA-PERF-010):
 *   - Rúbricas y plan de actividades  → p95 < 3 000 ms
 *   - Reportes de monitorías/profesor → p95 < 5 000 ms
 *   - Reporte de categorías           → p95 < 5 000 ms
 *   - Reporte de asistencia           → p95 < 6 000 ms  (join masivo)
 *
 * Perfil de carga:
 *   0 → 10 VUs en 30 s  (ramp-up)
 *   10 VUs por 3 min     (carga sostenida)
 *   10 → 0 VUs en 30 s  (ramp-down)
 *
 * Uso:
 *   k6 run performance/tests/reportes.test.js
 *   k6 run -e PROFESSOR_ID=1002 -e MONITOR_ID=2220004 performance/tests/reportes.test.js
 */

import http from 'k6/http';
import { check, group, sleep } from 'k6';
import { login, authHeaders } from '../helpers/auth.js';
import { reportesThresholds } from '../config/thresholds.js';
import {
    BASE_URL,
    PROFESSOR_ID, PROFESSOR_PASS,
    MONITOR_ID,   MONITOR_PASS,
    HEAD_ID,      HEAD_PASS,
} from '../config/env.js';

// ── Opciones ───────────────────────────────────────────────────────────────────
export const options = {
    stages: [
        { duration: '30s', target: 10 },
        { duration: '3m',  target: 10 },
        { duration: '30s', target: 0  },
    ],
    thresholds: reportesThresholds,
};

// ── Distribución de flujos por VU ──────────────────────────────────────────────
// VU % 4 === 0 → rúbricas + plan profesor
// VU % 4 === 1 → plan monitor + reporte monitores
// VU % 4 === 2 → reporte profesor + reporte categorías
// VU % 4 === 3 → reporte asistencia
export default function () {
    const mod = __VU % 4;

    if      (mod === 0) flujoRubricasYPlan();
    else if (mod === 1) flujoPlanMonitorYReporte();
    else if (mod === 2) flujoReporteProfesorYCategorias();
    else                flujoReporteAsistencia();

    sleep(1);
}

// ── Flujo 1: Rúbricas y plan de actividades (profesor) ────────────────────────
function flujoRubricasYPlan() {
    let token;

    group('Profesor — Login', () => {
        token = login(PROFESSOR_ID, PROFESSOR_PASS);
        check(token, { 'login profesor: ok': (t) => t !== null });
    });

    if (!token) return;
    sleep(0.3);

    group('Profesor — Rúbricas asignadas', () => {
        const res = http.get(
            `${BASE_URL}/rubric/professor/${PROFESSOR_ID}`,
            {
                ...authHeaders(token),
                tags: { endpoint: 'rubricas' },
                responseCallback: http.expectedStatuses(200, 204, 404),
            }
        );
        check(res, {
            'rúbricas por profesor: sin error de servidor': (r) => r.status < 500,
        });
    });

    sleep(0.3);

    group('Profesor — Plan de actividades', () => {
        const res = http.get(
            `${BASE_URL}/activity/findAll/${PROFESSOR_ID}/professor`,
            {
                ...authHeaders(token),
                tags: { endpoint: 'plan_actividades' },
            }
        );
        check(res, {
            'plan actividades profesor: status 200': (r) => r.status === 200,
        });
    });
}

// ── Flujo 2: Plan de actividades (monitor) + reporte de monitores ─────────────
function flujoPlanMonitorYReporte() {
    let tokenMonitor;
    let tokenProfesor;

    group('Monitor — Login', () => {
        tokenMonitor = login(MONITOR_ID, MONITOR_PASS);
        check(tokenMonitor, { 'login monitor: ok': (t) => t !== null });
    });

    if (!tokenMonitor) return;
    sleep(0.3);

    group('Monitor — Plan de actividades', () => {
        const res = http.get(
            `${BASE_URL}/activity/findAll/${MONITOR_ID}/monitor`,
            {
                ...authHeaders(tokenMonitor),
                tags: { endpoint: 'plan_actividades' },
            }
        );
        check(res, {
            'plan actividades monitor: status 200': (r) => r.status === 200,
        });
    });

    sleep(0.3);

    group('Profesor — Login para reporte', () => {
        tokenProfesor = login(PROFESSOR_ID, PROFESSOR_PASS);
        check(tokenProfesor, { 'login profesor para reporte: ok': (t) => t !== null });
    });

    if (!tokenProfesor) return;
    sleep(0.3);

    group('Profesor — Reporte de monitores', () => {
        const res = http.get(
            `${BASE_URL}/monitoring/getMonitorsReport/${PROFESSOR_ID}/professor`,
            {
                ...authHeaders(tokenProfesor),
                tags: { endpoint: 'reporte_monitores' },
                timeout: '20s',
            }
        );
        check(res, {
            'reporte monitores: status 200': (r) => r.status === 200,
        });
    });
}

// ── Flujo 3: Reporte de profesor y reporte de categorías ──────────────────────
function flujoReporteProfesorYCategorias() {
    let token;

    group('Profesor — Login', () => {
        token = login(PROFESSOR_ID, PROFESSOR_PASS);
        check(token, { 'login profesor: ok': (t) => t !== null });
    });

    if (!token) return;
    sleep(0.3);

    group('Profesor — Reporte de profesor', () => {
        const res = http.get(
            `${BASE_URL}/monitoring/getProfessorReport/${PROFESSOR_ID}`,
            {
                ...authHeaders(token),
                tags: { endpoint: 'reporte_profesor' },
                timeout: '20s',
                responseCallback: http.expectedStatuses(200, 204, 404),
            }
        );
        check(res, {
            'reporte profesor: sin error de servidor': (r) => r.status < 500,
        });
    });

    sleep(0.3);

    group('Profesor — Reporte de categorías', () => {
        const res = http.get(
            `${BASE_URL}/monitoring/getCategoriesReport/professor/${PROFESSOR_ID}`,
            {
                ...authHeaders(token),
                tags: { endpoint: 'reporte_categorias' },
                timeout: '20s',
                responseCallback: http.expectedStatuses(200, 204, 404),
            }
        );
        check(res, {
            'reporte categorías: sin error de servidor': (r) => r.status < 500,
        });
    });
}

// ── Flujo 4: Reporte de asistencia ────────────────────────────────────────────
function flujoReporteAsistencia() {
    let token;

    group('Profesor — Login para asistencia', () => {
        token = login(PROFESSOR_ID, PROFESSOR_PASS);
        check(token, { 'login para asistencia: ok': (t) => t !== null });
    });

    if (!token) return;
    sleep(0.3);

    group('Profesor — Reporte de asistencia', () => {
        const res = http.get(
            `${BASE_URL}/monitoring/getAttendanceReport/professor/${PROFESSOR_ID}`,
            {
                ...authHeaders(token),
                tags: { endpoint: 'reporte_asistencia' },
                timeout: '25s',
                responseCallback: http.expectedStatuses(200, 204, 404),
            }
        );
        check(res, {
            'reporte asistencia: sin error de servidor': (r) => r.status < 500,
        });
    });
}
