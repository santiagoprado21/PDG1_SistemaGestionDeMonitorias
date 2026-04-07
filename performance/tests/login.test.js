/**
 * SIGMA-PERF-003 / HU2-257 — Latencia y tasa de éxito de /auth/login
 *
 * Mide la latencia y la tasa de éxito del endpoint de autenticación bajo
 * carga concurrente, simulando los tres roles del sistema (monitor,
 * profesor, jefe de departamento) en round-robin.
 *
 * Perfil de carga:
 *   0 → 10 VUs en 30 s  (ramp-up)
 *   10 VUs por 3 min     (carga sostenida)
 *   10 → 0 VUs en 30 s  (ramp-down)
 * Total: ~4 min
 *
 * Métricas capturadas:
 *   - http_req_duration global (p50, p90, p95, max)
 *   - http_req_duration desglosada por rol  {role:monitor|profesor|jefe}
 *   - http_req_failed  (tasa de errores HTTP)
 *   - checks           (tasa de éxito de negocio: status 200 + token)
 *
 * Criterios de éxito (thresholds):
 *   - p95 global       < 2 500 ms
 *   - p95 {role:monitor}  < 2 500 ms
 *   - p95 {role:profesor} < 2 500 ms
 *   - p95 {role:jefe}     < 2 500 ms
 *   - http_req_failed  == 0 %
 *   - checks           == 100 %
 *
 * Gestión de credenciales:
 *   Ver sección "Gestión de credenciales de prueba" en performance/README.md
 *
 * Uso básico:
 *   k6 run performance/tests/login.test.js
 *
 * Uso con credenciales personalizadas:
 *   k6 run \
 *     -e MONITOR_ID=2220004   -e MONITOR_PASS=123456 \
 *     -e PROFESSOR_ID=1002    -e PROFESSOR_PASS=prof123 \
 *     -e HEAD_ID=5001         -e HEAD_PASS=jefe123 \
 *     performance/tests/login.test.js
 */

import http from 'k6/http';
import { check, group, sleep } from 'k6';
import { loginThresholds } from '../config/thresholds.js';
import {
    BASE_URL,
    MONITOR_ID,    MONITOR_PASS,
    PROFESSOR_ID,  PROFESSOR_PASS,
    HEAD_ID,       HEAD_PASS,
} from '../config/env.js';

// ── Opciones ──────────────────────────────────────────────────────────────────
export const options = {
    stages: [
        { duration: '30s', target: 10 },  // ramp-up
        { duration: '3m',  target: 10 },  // carga sostenida
        { duration: '30s', target: 0  },  // ramp-down
    ],
    thresholds: {
        ...loginThresholds,
        // Desglose por rol — permite detectar si un rol específico es más lento
        'http_req_duration{role:monitor}':  ['p(95)<2500'],
        'http_req_duration{role:profesor}': ['p(95)<2500'],
        'http_req_duration{role:jefe}':     ['p(95)<2500'],
    },
};

// ── Helper de login ───────────────────────────────────────────────────────────
function attemptLogin(userId, password, role) {
    const payload = JSON.stringify({ userId, password });
    const params  = {
        headers: { 'Content-Type': 'application/json' },
        timeout: '20s',
        tags:    { role },
    };

    const res = http.post(`${BASE_URL}/auth/login`, payload, params);

    // Los checks validan correctitud (status + token).
    // El tiempo se controla con el threshold p95, no con checks individuales.
    check(res, {
        [`[${role}] login status 200`]:   (r) => r.status === 200,
        [`[${role}] responde con token`]: (r) => {
            try { return !!r.json('token'); } catch (_) { return false; }
        },
    });

    return res.status === 200 ? res.json('token') : null;
}

// ── Escenario principal ───────────────────────────────────────────────────────
export default function () {
    // Distribuir la carga entre los tres roles en round-robin
    const vuIndex = __VU % 3;

    if (vuIndex === 0) {
        group('Login monitor', () => {
            attemptLogin(MONITOR_ID, MONITOR_PASS, 'monitor');
        });
    } else if (vuIndex === 1) {
        group('Login profesor', () => {
            attemptLogin(PROFESSOR_ID, PROFESSOR_PASS, 'profesor');
        });
    } else {
        group('Login jefe de departamento', () => {
            attemptLogin(HEAD_ID, HEAD_PASS, 'jefe');
        });
    }

    // Pausa realista entre intentos (simula tiempo de lectura del formulario)
    sleep(1);
}
