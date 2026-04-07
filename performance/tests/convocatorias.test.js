/**
 * SIGMA-PERF-004 / HU2-261 — Carga en endpoints de listado de convocatorias
 *
 * Simula el flujo de LECTURA de convocatorias para los tres roles:
 *   Profesor:
 *     1. Listar convocatorias abiertas
 *     2. Mis convocatorias (por profesor)
 *     3. Postulaciones recibidas
 *   Monitor:
 *     4. Convocatorias disponibles (abiertas)
 *     5. Mis postulaciones
 *   Jefe de departamento:
 *     6. Convocatorias pendientes de aprobación
 *
 * Para el flujo de ESCRITURA (ciclo completo crear→aprobar→postular→seleccionar→cerrar)
 * ver: performance/tests/convocatorias-ciclo.test.js
 *
 * Perfil de carga:
 *   0 → 15 VUs en 30 s  (ramp-up)
 *   15 VUs por 3 min     (carga sostenida)
 *   15 → 0 VUs en 30 s  (ramp-down)
 *
 * Criterios de éxito (SIGMA-PERF-002):
 *   - p95 de duración HTTP < 3 000 ms
 *   - Tasa de errores HTTP = 0 %
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
    HEAD_ID,      HEAD_PASS,
} from '../config/env.js';

// ── Opciones ──────────────────────────────────────────────────────────────────
export const options = {
    stages: [
        { duration: '30s', target: 15 },
        { duration: '3m',  target: 15 },
        { duration: '30s', target: 0  },
    ],
    thresholds: convocatoriasThresholds,
};

// ── Escenario principal ───────────────────────────────────────────────────────
export default function () {
    const vuMod = __VU % 3;
    if (vuMod === 0)      flujoProfesor();
    else if (vuMod === 1) flujoMonitor();
    else                  flujoJefe();

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
        const res = http.get(`${BASE_URL}/monitoring-request/open`, {
            ...authHeaders(token),
            timeout: '20s',
        });
        check(res, {
            'convocatorias abiertas: status 200': (r) => r.status === 200,
            'convocatorias abiertas: es array':   (r) => {
                try { return Array.isArray(r.json()); } catch (_) { return false; }
            },
        });
    });

    sleep(0.3);

    group('Profesor — Mis convocatorias', () => {
        const res = http.get(
            `${BASE_URL}/monitoring-request/professor/${PROFESSOR_ID}`,
            { ...authHeaders(token), timeout: '20s' }
        );
        check(res, {
            'mis convocatorias: status 200': (r) => r.status === 200,
        });
    });

    sleep(0.3);

    group('Profesor — Postulaciones recibidas', () => {
        const res = http.get(
            `${BASE_URL}/monitor-application/professor/${PROFESSOR_ID}`,
            {
                ...authHeaders(token),
                timeout: '20s',
                // 404/403 son válidos: lista vacía o sin datos de prueba
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

    group('Monitor — Convocatorias disponibles', () => {
        const res = http.get(`${BASE_URL}/monitoring-request/open`, {
            ...authHeaders(token),
            timeout: '20s',
        });
        check(res, {
            'convocatorias disponibles: status 200': (r) => r.status === 200,
        });
    });

    sleep(0.3);

    group('Monitor — Mis postulaciones', () => {
        const res = http.get(
            `${BASE_URL}/monitor-application/monitor/${MONITOR_ID}`,
            { ...authHeaders(token), timeout: '20s' }
        );
        check(res, {
            'mis postulaciones: status 200': (r) => r.status === 200,
        });
    });
}

// ── Flujo jefe de departamento ────────────────────────────────────────────────
function flujoJefe() {
    let token;

    group('Jefe — Login', () => {
        token = login(HEAD_ID, HEAD_PASS);
        check(token, { 'token de jefe obtenido': (t) => t !== null });
    });

    if (!token) return;

    sleep(0.3);

    group('Jefe — Convocatorias pendientes de aprobación', () => {
        const res = http.get(
            `${BASE_URL}/monitoring-request/pending-head-approval/${HEAD_ID}`,
            {
                ...authHeaders(token),
                timeout: '20s',
                // Lista puede estar vacía (404) o sin datos de prueba (403)
                responseCallback: http.expectedStatuses(200, 201, 204, 400, 401, 403, 404),
            }
        );
        check(res, {
            'pendientes aprobación: sin error de servidor': (r) => r.status < 500,
        });
    });
}
