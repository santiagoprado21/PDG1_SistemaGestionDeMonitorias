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
 *   0 → 15 VUs en 30 s  (ramp-up — cubre todos los profesores nuevos)
 *   15 VUs por 3 min     (carga sostenida)
 *   15 → 0 VUs en 30 s  (ramp-down)
 *
 * Uso:
 *   k6 run performance/tests/reportes.test.js
 *   k6 run -e BASE_URL=https://mi-backend.onrender.com performance/tests/reportes.test.js
 *
 * Rotación de usuarios:
 *   Cada VU usa un profesor y monitor distinto según su número (__VU),
 *   cubriendo los 15 profesores (1001–1015) y 30 monitores (2220006–2220035).
 */

import http from 'k6/http';
import { check, group, sleep } from 'k6';
import { login, authHeaders } from '../helpers/auth.js';
import { reportesThresholds } from '../config/thresholds.js';
import { BASE_URL } from '../config/env.js';

// ── Pool de usuarios realistas (scripts 18–20) ─────────────────────────────────
// Solo profesores con monitorias reales en el sistema (script 20)
const PROFESORES = [
    '1001','1002','1003','1004','1005',
    '1006','1007','1008','1009','1010',
];
const MONITORES = [
    '2220006','2220007','2220008','2220009','2220010',
    '2220011','2220012','2220013','2220014','2220015',
    '2220016','2220017','2220018','2220019','2220020',
    '2220021','2220022','2220023',
];
const PROF_PASS   = 'prof123';
const MONITOR_PASS = '123456';

// ── Opciones ───────────────────────────────────────────────────────────────────
export const options = {
    stages: [
        { duration: '30s', target: 8 },
        { duration: '3m',  target: 8 },
        { duration: '30s', target: 0 },
    ],
    thresholds: reportesThresholds,
};

// ── Selección de usuario por VU ────────────────────────────────────────────────
// Cada VU rota sobre el pool para distribuir la carga entre todos los usuarios
function profId()    { return PROFESORES[(__VU - 1) % PROFESORES.length]; }
function monitorId() { return MONITORES[(__VU - 1)  % MONITORES.length];  }

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

    sleep(2);
}

// ── Flujo 1: Rúbricas y plan de actividades (profesor) ────────────────────────
function flujoRubricasYPlan() {
    const pid = profId();
    let token;

    group('Profesor — Login', () => {
        token = login(pid, PROF_PASS);
        check(token, { 'login profesor: ok': (t) => t !== null });
    });

    if (!token) return;
    sleep(0.3);

    group('Profesor — Rúbricas asignadas', () => {
        const res = http.get(
            `${BASE_URL}/rubric/professor/${pid}`,
            {
                ...authHeaders(token),
                tags: { endpoint: 'rubricas' },
                responseCallback: http.expectedStatuses(200, 201, 204, 400, 401, 403, 404),
            }
        );
        check(res, { 'rúbricas por profesor: sin error de servidor': (r) => r.status < 500 });
    });

    sleep(0.3);

    group('Profesor — Plan de actividades', () => {
        const res = http.get(
            `${BASE_URL}/activity/findAll/${pid}/professor`,
            {
                ...authHeaders(token),
                tags: { endpoint: 'plan_actividades' },
                responseCallback: http.expectedStatuses(200, 201, 204, 400, 401, 403, 404),
            }
        );
        check(res, { 'plan actividades profesor: sin error de servidor': (r) => r.status < 500 });
    });
}

// ── Flujo 2: Plan de actividades (monitor) + reporte de monitores ─────────────
function flujoPlanMonitorYReporte() {
    const mid = monitorId();
    const pid = profId();
    let tokenMonitor;
    let tokenProfesor;

    group('Monitor — Login', () => {
        tokenMonitor = login(mid, MONITOR_PASS);
        check(tokenMonitor, { 'login monitor: ok': (t) => t !== null });
    });

    if (!tokenMonitor) return;
    sleep(0.3);

    group('Monitor — Plan de actividades', () => {
        const res = http.get(
            `${BASE_URL}/activity/findAll/${mid}/monitor`,
            {
                ...authHeaders(tokenMonitor),
                tags: { endpoint: 'plan_actividades' },
                responseCallback: http.expectedStatuses(200, 201, 204, 400, 401, 403, 404),
            }
        );
        check(res, { 'plan actividades monitor: sin error de servidor': (r) => r.status < 500 });
    });

    sleep(0.3);

    group('Profesor — Login para reporte', () => {
        tokenProfesor = login(pid, PROF_PASS);
        check(tokenProfesor, { 'login profesor para reporte: ok': (t) => t !== null });
    });

    if (!tokenProfesor) return;
    sleep(0.3);

    group('Profesor — Reporte de monitores', () => {
        const res = http.get(
            `${BASE_URL}/monitoring/getMonitorsReport/${pid}/professor`,
            {
                ...authHeaders(tokenProfesor),
                tags: { endpoint: 'reporte_monitores' },
                timeout: '20s',
                responseCallback: http.expectedStatuses(200, 201, 204, 400, 401, 403, 404),
            }
        );
        if (res.status >= 500) console.error(`[reporte_monitores] prof=${pid} status=${res.status} body=${res.body.substring(0, 300)}`);
        check(res, { 'reporte monitores: sin error de servidor': (r) => r.status < 500 });
    });
}

// ── Flujo 3: Reporte de profesor y reporte de categorías ──────────────────────
function flujoReporteProfesorYCategorias() {
    const pid = profId();
    let token;

    group('Profesor — Login', () => {
        token = login(pid, PROF_PASS);
        check(token, { 'login profesor: ok': (t) => t !== null });
    });

    if (!token) return;
    sleep(0.3);

    group('Profesor — Reporte de profesor', () => {
        const res = http.get(
            `${BASE_URL}/monitoring/getProfessorReport/${pid}`,
            {
                ...authHeaders(token),
                tags: { endpoint: 'reporte_profesor' },
                timeout: '20s',
                responseCallback: http.expectedStatuses(200, 201, 204, 400, 401, 403, 404),
            }
        );
        if (res.status >= 500) console.error(`[reporte_profesor] prof=${pid} status=${res.status} body=${res.body.substring(0, 300)}`);
        check(res, { 'reporte profesor: sin error de servidor': (r) => r.status < 500 });
    });

    sleep(0.3);

    group('Profesor — Reporte de categorías', () => {
        const res = http.get(
            `${BASE_URL}/monitoring/getCategoriesReport/professor/${pid}`,
            {
                ...authHeaders(token),
                tags: { endpoint: 'reporte_categorias' },
                timeout: '20s',
                responseCallback: http.expectedStatuses(200, 201, 204, 400, 401, 403, 404),
            }
        );
        if (res.status >= 500) console.error(`[reporte_categorias] prof=${pid} status=${res.status} body=${res.body.substring(0, 300)}`);
        check(res, { 'reporte categorías: sin error de servidor': (r) => r.status < 500 });
    });
}

// ── Flujo 4: Reporte de asistencia ────────────────────────────────────────────
function flujoReporteAsistencia() {
    const pid = profId();
    let token;

    group('Profesor — Login para asistencia', () => {
        token = login(pid, PROF_PASS);
        check(token, { 'login para asistencia: ok': (t) => t !== null });
    });

    if (!token) return;
    sleep(0.3);

    group('Profesor — Reporte de asistencia', () => {
        const res = http.get(
            `${BASE_URL}/monitoring/getAttendanceReport/professor/${pid}`,
            {
                ...authHeaders(token),
                tags: { endpoint: 'reporte_asistencia' },
                timeout: '25s',
                responseCallback: http.expectedStatuses(200, 201, 204, 400, 401, 403, 404),
            }
        );
        if (res.status >= 500) console.error(`[reporte_asistencia] prof=${pid} status=${res.status} body=${res.body.substring(0, 300)}`);
        check(res, { 'reporte asistencia: sin error de servidor': (r) => r.status < 500 });
    });
}
