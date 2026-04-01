/**
 * SIGMA-PERF-008 — Escenario de carga sostenida (mezcla de flujos)
 *
 * Simula una carga realista del sistema con todos los roles activos
 * en paralelo, usando k6 scenarios para distribuir la carga.
 *
 * Escenarios paralelos:
 * ┌──────────────────┬───────┬──────────┬──────────────────────────────────┐
 * │ Escenario        │ VUs   │ Duración │ Descripción                      │
 * ├──────────────────┼───────┼──────────┼──────────────────────────────────┤
 * │ loginLoad        │  5    │ 5 min    │ Carga en /auth/login              │
 * │ convocatoriasLoad│  8    │ 5 min    │ Lectura y consulta convocatorias  │
 * │ actividadesLoad  │  8    │ 5 min    │ Plan de actividades y reportes    │
 * │ cierreLoad       │  4    │ 5 min    │ Flujo de cierre (lectura)         │
 * └──────────────────┴───────┴──────────┴──────────────────────────────────┘
 * Total máximo: 25 VUs concurrentes
 *
 * Criterios de éxito (SIGMA-PERF-002 — relajados para carga mixta):
 *   - p95 global          < 3 000 ms
 *   - p95 flow:login      < 2 000 ms
 *   - p95 flow:actividades< 2 500 ms
 *   - p95 flow:reporte    < 5 000 ms
 *   - p95 flow:cierre     < 3 000 ms
 *   - Tasa de errores     < 1 %
 *   - Checks              >= 99 %
 *
 * Uso:
 *   k6 run performance/scenarios/load.test.js
 *   k6 run --out json=results/load-$(date +%Y%m%d).json performance/scenarios/load.test.js
 */

import http from 'k6/http';
import { check, group, sleep } from 'k6';
import { login, authHeaders } from '../helpers/auth.js';
import { loadThresholds } from '../config/thresholds.js';
import {
    BASE_URL,
    MONITOR_ID,   MONITOR_PASS,
    PROFESSOR_ID, PROFESSOR_PASS,
    HEAD_ID,      HEAD_PASS,
} from '../config/env.js';

// ── Opciones ──────────────────────────────────────────────────────────────────
export const options = {
    scenarios: {
        loginLoad: {
            executor:  'constant-vus',
            vus:       5,
            duration:  '5m',
            exec:      'loginFlow',
            startTime: '0s',
        },
        convocatoriasLoad: {
            executor:  'constant-vus',
            vus:       8,
            duration:  '5m',
            exec:      'convocatoriasFlow',
            startTime: '0s',
        },
        actividadesLoad: {
            executor:  'constant-vus',
            vus:       8,
            duration:  '5m',
            exec:      'actividadesFlow',
            startTime: '0s',
        },
        cierreLoad: {
            executor:  'constant-vus',
            vus:       4,
            duration:  '5m',
            exec:      'cierreFlow',
            startTime: '0s',
        },
    },
    thresholds: loadThresholds,
};

// ── Flujo login ───────────────────────────────────────────────────────────────
export function loginFlow() {
    const roles = [
        { id: MONITOR_ID,   pass: MONITOR_PASS,   name: 'monitor'  },
        { id: PROFESSOR_ID, pass: PROFESSOR_PASS, name: 'profesor' },
        { id: HEAD_ID,      pass: HEAD_PASS,      name: 'jefe'     },
    ];
    const { id, pass, name } = roles[__VU % roles.length];

    group(`Login — ${name}`, () => {
        const payload = JSON.stringify({ userId: id, password: pass });
        const res = http.post(`${BASE_URL}/auth/login`, payload, {
            headers: { 'Content-Type': 'application/json' },
            tags:    { flow: 'login' },
        });
        check(res, {
            [`${name}: login 200`]:    (r) => r.status === 200,
            [`${name}: tiene token`]:  (r) => {
                try { return !!r.json('token'); } catch (_) { return false; }
            },
        });
    });

    sleep(1);
}

// ── Flujo convocatorias ───────────────────────────────────────────────────────
export function convocatoriasFlow() {
    let token;

    group('Convocatorias — Login', () => {
        token = login(PROFESSOR_ID, PROFESSOR_PASS);
    });

    if (!token) { sleep(1); return; }

    sleep(0.3);

    group('Convocatorias — Listado abierto', () => {
        const res = http.get(`${BASE_URL}/monitoring-request/open`, {
            ...authHeaders(token),
            tags: { flow: 'convocatorias' },
        });
        check(res, {
            'convocatorias: 200':      (r) => r.status === 200,
            'convocatorias: es array': (r) => {
                try { return Array.isArray(r.json()); } catch (_) { return false; }
            },
        });
    });

    sleep(0.3);

    group('Convocatorias — Por profesor', () => {
        const res = http.get(
            `${BASE_URL}/monitoring-request/professor/${PROFESSOR_ID}`,
            { ...authHeaders(token), tags: { flow: 'convocatorias' } }
        );
        check(res, { 'convocatorias por prof: 200': (r) => r.status === 200 });
    });

    sleep(0.3);

    group('Convocatorias — Postulaciones recibidas', () => {
        const res = http.get(
            `${BASE_URL}/monitor-application/professor/${PROFESSOR_ID}`,
            {
                ...authHeaders(token),
                tags: { flow: 'convocatorias' },
                responseCallback: http.expectedStatuses(200, 201, 204, 400, 401, 403, 404),
            }
        );
        check(res, { 'postulaciones: sin error de servidor': (r) => r.status < 500 });
    });

    sleep(1);
}

// ── Flujo actividades ─────────────────────────────────────────────────────────
export function actividadesFlow() {
    const useMonitor = __VU % 2 === 0;
    const userId     = useMonitor ? MONITOR_ID   : PROFESSOR_ID;
    const pass       = useMonitor ? MONITOR_PASS  : PROFESSOR_PASS;
    const role       = useMonitor ? 'monitor'     : 'professor';

    let token;

    group('Actividades — Login', () => {
        token = login(userId, pass);
    });

    if (!token) { sleep(1); return; }

    sleep(0.3);

    group('Actividades — Listado', () => {
        const res = http.get(
            `${BASE_URL}/activity/findAll/${userId}/${role}`,
            { ...authHeaders(token), tags: { flow: 'actividades' } }
        );
        check(res, {
            'actividades: 200': (r) => r.status === 200,
        });
    });

    sleep(0.3);

    // Solo el profesor consulta el reporte (endpoint costoso)
    if (!useMonitor) {
        group('Actividades — Reporte', () => {
            const res = http.get(
                `${BASE_URL}/monitoring/getMonitorsReport/${PROFESSOR_ID}/professor`,
                { ...authHeaders(token), tags: { flow: 'reporte' } }
            );
            check(res, {
                'reporte: 200': (r) => r.status === 200,
            });
        });
    }

    sleep(1);
}

// ── Flujo cierre ──────────────────────────────────────────────────────────────
export function cierreFlow() {
    const useHead = __VU % 2 === 0;
    const userId  = useHead ? HEAD_ID      : PROFESSOR_ID;
    const pass    = useHead ? HEAD_PASS    : PROFESSOR_PASS;
    const role    = useHead ? 'jefe'       : 'profesor';

    let token;

    group(`Cierre — Login ${role}`, () => {
        token = login(userId, pass);
    });

    if (!token) { sleep(1); return; }

    sleep(0.3);

    group('Cierre — Monitorías activas', () => {
        const endpoint = useHead
            ? `${BASE_URL}/monitoring/getAll`
            : `${BASE_URL}/monitoring/getAllByProfessor/${PROFESSOR_ID}`;

        const res = http.get(endpoint, {
            ...authHeaders(token),
            tags: { flow: 'cierre' },
            // getAll puede devolver 403/404 para el usuario de prueba
            responseCallback: http.expectedStatuses(200, 201, 204, 400, 401, 403, 404),
        });
        check(res, {
            'monitorías cierre: sin error de servidor': (r) => r.status < 500,
        });
    });

    sleep(0.3);

    group('Cierre — Evaluaciones', () => {
        const res = http.get(
            `${BASE_URL}/monitor-evaluations/professor/${PROFESSOR_ID}`,
            { ...authHeaders(token), tags: { flow: 'cierre' } }
        );
        check(res, {
            'evaluaciones cierre: 200 o 404': (r) => r.status === 200 || r.status === 404,
        });
    });

    sleep(1);
}
