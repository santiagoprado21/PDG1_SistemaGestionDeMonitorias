/**
 * SIGMA-PERF-003 — Carga en /auth/login
 *
 * Valida que el endpoint de autenticación aguante carga concurrente
 * de los tres roles (monitor, profesor, jefe de departamento) y
 * cumpla el SLA definido en SIGMA-PERF-002.
 *
 * Perfil de carga:
 *   0 → 10 VUs en 30 s  (ramp-up)
 *   10 VUs por 3 min     (carga sostenida)
 *   10 → 0 VUs en 30 s  (ramp-down)
 *
 * Criterios de éxito (thresholds):
 *   - p95 de duración HTTP < 2 500 ms  (ajustado por latencia real de DB cloud)
 *   - Tasa de errores HTTP = 0 %
 *   - 100 % de checks de negocio pasan (estado 200 + token presente)
 *
 * Uso:
 *   k6 run performance/tests/login.test.js
 *   k6 run -e BASE_URL=http://localhost:5433 -e MONITOR_ID=A00381698 performance/tests/login.test.js
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
    thresholds: loginThresholds,
};

// ── Helpers ───────────────────────────────────────────────────────────────────
function attemptLogin(userId, password, role) {
    const payload = JSON.stringify({ userId, password });
    const params  = { headers: { 'Content-Type': 'application/json' } };
    const res     = http.post(`${BASE_URL}/auth/login`, payload, params);

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
    // Distribuir la carga entre los tres roles de forma round-robin
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

    // Pausa realista entre intentos de login (simula tiempo de lectura de UI)
    sleep(1);
}
