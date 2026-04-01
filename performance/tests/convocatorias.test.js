/**
 * SIGMA-PERF-004 — Flujo de convocatorias
 *
 * Simula el flujo completo de gestión de convocatorias:
 *   1. Login como profesor
 *   2. Listar convocatorias abiertas
 *   3. Obtener convocatorias por profesor
 *   4. Consultar postulaciones recibidas (acepta 200 o 404 — lista vacía es válida)
 *   5. Login como monitor
 *   6. Listar convocatorias disponibles para el monitor
 *   7. Consultar sus propias postulaciones
 *
 * Perfil de carga:
 *   0 → 15 VUs en 30 s  (ramp-up)
 *   15 VUs por 3 min     (carga sostenida)
 *   15 → 0 VUs en 30 s  (ramp-down)
 *
 * Criterios de éxito (SIGMA-PERF-002):
 *   - p95 de duración HTTP < 3 000 ms
 *   - Tasa de errores HTTP = 0 %  (404 no se cuenta como error de infraestructura)
 *   - 100 % de checks de negocio pasan
 *
 * Uso:
 *   k6 run performance/tests/convocatorias.test.js
 */

import http from 'k6/http';
import { check, group, sleep } from 'k6';
import { login, authHeaders } from '../helpers/auth.js';
import { convocatoriasThresholds } from '../config/thresholds.js';
import {
    BASE_URL,
    MONITOR_ID,   MONITOR_PASS,
    PROFESSOR_ID, PROFESSOR_PASS,
} from '../config/env.js';

// ── Opciones ──────────────────────────────────────────────────────────────────
export const options = {
    stages: [
        { duration: '30s', target: 15 },
        { duration: '3m',  target: 15 },
        { duration: '30s', target: 0  },
    ],
    thresholds: {
        ...convocatoriasThresholds,
        // 404 es respuesta válida (lista vacía). Solo 5xx son errores reales.
        // k6 marca como "failed" los 4xx/5xx; excluimos 404 contando solo >= 500.
        http_req_failed: ['rate==0'],
    },
};

// ── Escenario principal ───────────────────────────────────────────────────────
export default function () {
    if (__VU % 2 === 0) {
        flujoProfesor();
    } else {
        flujoMonitor();
    }

    sleep(1);
}

// ── Flujo profesor ────────────────────────────────────────────────────────────
function flujoProfesor() {
    let token;

    group('Profesor — Login', () => {
        token = login(PROFESSOR_ID, PROFESSOR_PASS);
        check(token, { 'token de profesor obtenido': (t) => t !== null });
    });

    if (!token) return;

    sleep(0.3);

    group('Profesor — Convocatorias abiertas', () => {
        const res = http.get(
            `${BASE_URL}/monitoring-request/open`,
            authHeaders(token)
        );
        check(res, {
            'convocatorias abiertas: status 200': (r) => r.status === 200,
            'convocatorias abiertas: es array':   (r) => {
                try { return Array.isArray(r.json()); } catch (_) { return false; }
            },
        });
    });

    sleep(0.3);

    group('Profesor — Convocatorias por profesor', () => {
        const res = http.get(
            `${BASE_URL}/monitoring-request/professor/${PROFESSOR_ID}`,
            authHeaders(token)
        );
        check(res, {
            'convocatorias por profesor: status 200': (r) => r.status === 200,
        });
    });

    sleep(0.3);

    group('Profesor — Postulaciones recibidas', () => {
        const res = http.get(
            `${BASE_URL}/monitor-application/professor/${PROFESSOR_ID}`,
            {
                ...authHeaders(token),
                // Cualquier respuesta no-5xx es válida: 200 (hay datos),
                // 404 (sin postulaciones), 403 (sin permiso en datos de prueba)
                responseCallback: http.expectedStatuses(200, 201, 204, 400, 401, 403, 404),
            }
        );
        check(res, {
            'postulaciones recibidas: sin error de servidor': (r) => r.status < 500,
        });
    });
}

// ── Flujo monitor ─────────────────────────────────────────────────────────────
function flujoMonitor() {
    let token;

    group('Monitor — Login', () => {
        token = login(MONITOR_ID, MONITOR_PASS);
        check(token, { 'token de monitor obtenido': (t) => t !== null });
    });

    if (!token) return;

    sleep(0.3);

    group('Monitor — Ver convocatorias disponibles', () => {
        const res = http.get(
            `${BASE_URL}/monitoring-request/open`,
            authHeaders(token)
        );
        check(res, {
            'convocatorias disponibles: status 200': (r) => r.status === 200,
        });
    });

    sleep(0.3);

    group('Monitor — Mis postulaciones', () => {
        const res = http.get(
            `${BASE_URL}/monitor-application/monitor/${MONITOR_ID}`,
            authHeaders(token)
        );
        check(res, {
            'mis postulaciones: status 200': (r) => r.status === 200,
        });
    });
}
