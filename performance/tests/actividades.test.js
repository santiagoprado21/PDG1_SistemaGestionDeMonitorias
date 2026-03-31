/**
 * SIGMA-PERF-005 — Plan de actividades y reportes
 *
 * Simula la consulta del plan de actividades (monitor y profesor)
 * y la generación del reporte de monitores, que es el endpoint
 * más costoso computacionalmente.
 *
 * Flujos cubiertos:
 *   1. Login profesor → monitorías asignadas → plan de actividades → reporte
 *   2. Login monitor  → actividades del monitor
 *
 * Perfil de carga:
 *   0 → 10 VUs en 30 s  (ramp-up)
 *   10 VUs por 3 min     (carga sostenida)
 *   10 → 0 VUs en 30 s  (ramp-down)
 *
 * Criterios de éxito (SIGMA-PERF-002):
 *   - p95 actividades  < 2 500 ms
 *   - p95 reportes     < 5 000 ms  (etiqueta `endpoint:reporte`)
 *   - Tasa de errores  = 0 %
 *   - 100 % de checks pasan
 *
 * Uso:
 *   k6 run performance/tests/actividades.test.js
 */

import http from 'k6/http';
import { check, group, sleep } from 'k6';
import { login, authHeaders } from '../helpers/auth.js';
import { actividadesThresholds } from '../config/thresholds.js';
import {
    BASE_URL,
    MONITOR_ID,   MONITOR_PASS,
    PROFESSOR_ID, PROFESSOR_PASS,
} from '../config/env.js';

// ── Opciones ──────────────────────────────────────────────────────────────────
export const options = {
    stages: [
        { duration: '30s', target: 10 },
        { duration: '3m',  target: 10 },
        { duration: '30s', target: 0  },
    ],
    thresholds: actividadesThresholds,
};

// ── Escenario principal ───────────────────────────────────────────────────────
export default function () {
    if (__VU % 3 === 0) {
        flujoReporte();
    } else if (__VU % 3 === 1) {
        flujoProfesorActividades();
    } else {
        flujoMonitorActividades();
    }

    sleep(1);
}

// ── Flujo: reporte de monitores (endpoint costoso) ────────────────────────────
function flujoReporte() {
    let token;

    group('Profesor — Login', () => {
        token = login(PROFESSOR_ID, PROFESSOR_PASS);
        check(token, { 'token para reporte obtenido': (t) => t !== null });
    });

    if (!token) return;

    sleep(0.3);

    group('Profesor — Reporte de monitores', () => {
        // Tag "endpoint:reporte" activa el threshold específico de 5 000 ms
        const res = http.get(
            `${BASE_URL}/monitoring/getMonitorsReport/${PROFESSOR_ID}/professor`,
            {
                ...authHeaders(token),
                tags: { endpoint: 'reporte' },
            }
        );
        check(res, {
            'reporte monitores: status 200':   (r) => r.status === 200,
        });
    });
}

// ── Flujo: actividades desde perspectiva del profesor ────────────────────────
function flujoProfesorActividades() {
    let token;

    group('Profesor — Login', () => {
        token = login(PROFESSOR_ID, PROFESSOR_PASS);
        check(token, { 'token profesor obtenido': (t) => t !== null });
    });

    if (!token) return;

    sleep(0.3);

    group('Profesor — Monitorías asignadas', () => {
        const res = http.get(
            `${BASE_URL}/monitoring/getAllByProfessor/${PROFESSOR_ID}`,
            authHeaders(token)
        );
        check(res, {
            'monitorías por profesor: status 200':   (r) => r.status === 200,
        });
    });

    sleep(0.3);

    group('Profesor — Actividades por rol', () => {
        const res = http.get(
            `${BASE_URL}/activity/findAll/${PROFESSOR_ID}/professor`,
            authHeaders(token)
        );
        check(res, {
            'actividades profesor: status 200':    (r) => r.status === 200,
        });
    });
}

// ── Flujo: actividades desde perspectiva del monitor ─────────────────────────
function flujoMonitorActividades() {
    let token;

    group('Monitor — Login', () => {
        token = login(MONITOR_ID, MONITOR_PASS);
        check(token, { 'token monitor obtenido': (t) => t !== null });
    });

    if (!token) return;

    sleep(0.3);

    group('Monitor — Mis actividades', () => {
        const res = http.get(
            `${BASE_URL}/activity/findAll/${MONITOR_ID}/monitor`,
            authHeaders(token)
        );
        check(res, {
            'mis actividades: status 200':    (r) => r.status === 200,
        });
    });

    sleep(0.3);

    group('Monitor — Monitorías del monitor', () => {
        const res = http.get(
            `${BASE_URL}/monitoring/getAllByMonitor/${MONITOR_ID}`,
            authHeaders(token)
        );
        check(res, {
            'monitorías monitor: status 200':    (r) => r.status === 200,
        });
    });
}
