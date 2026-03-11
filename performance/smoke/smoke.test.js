/**
 * SIGMA-PERF-007 / SIGMA-PERF-001 — Script Smoke
 *
 * Verifica en una sola pasada que los flujos críticos de SIGMA+ responden
 * correctamente (status 200, sin errores). Duración total < 2 minutos.
 *
 * Flujos cubiertos:
 *   1. Login (profesor)
 *   2. Login (monitor)
 *   3. Perfil de profesor
 *   4. Convocatorias abiertas
 *   5. Plan de actividades (por profesor)
 *   6. Reporte de monitores
 *
 * Uso:
 *   k6 run smoke/smoke.test.js
 *   k6 run -e BASE_URL=http://localhost:5433 -e PROFESSOR_ID=P001 -e PROFESSOR_PASS=pass smoke/smoke.test.js
 */

import http from 'k6/http';
import { check, group, sleep } from 'k6';
import { login, authHeaders } from '../helpers/auth.js';
import {
    BASE_URL,
    PROFESSOR_ID, PROFESSOR_PASS,
    MONITOR_ID,   MONITOR_PASS
} from '../config/env.js';

// ─── Opciones (SIGMA-PERF-001 + SIGMA-PERF-002) ───────────────────────────────
export const options = {
    vus:        1,
    iterations: 1,

    thresholds: {
        // 0% de peticiones deben fallar
        http_req_failed: ['rate==0'],
        // p95 de todas las peticiones < 2000 ms
        http_req_duration: ['p(95)<2000'],
        // Checks: todos deben pasar
        checks: ['rate==1.00']
    }
};

// ─── Escenario smoke ──────────────────────────────────────────────────────────
export default function () {

    // 1. Login profesor
    let profToken;
    group('1. Login profesor', () => {
        profToken = login(PROFESSOR_ID, PROFESSOR_PASS);
        check(profToken, { 'token de profesor obtenido': (t) => t !== null });
    });

    sleep(0.3);

    // 2. Login monitor
    let monToken;
    group('2. Login monitor', () => {
        monToken = login(MONITOR_ID, MONITOR_PASS);
        check(monToken, { 'token de monitor obtenido': (t) => t !== null });
    });

    sleep(0.3);

    // 3. Perfil de profesor
    group('3. Perfil profesor', () => {
        if (!profToken) return;
        const res = http.get(
            `${BASE_URL}/professor/profile/${PROFESSOR_ID}`,
            authHeaders(profToken)
        );
        check(res, {
            'perfil profesor status 200': (r) => r.status === 200,
            'perfil profesor contiene name': (r) => {
                try { return !!r.json('name'); } catch(_) { return false; }
            }
        });
    });

    sleep(0.3);

    // 4. Convocatorias abiertas
    group('4. Convocatorias abiertas', () => {
        if (!profToken) return;
        const res = http.get(
            `${BASE_URL}/monitoring-request/open`,
            authHeaders(profToken)
        );
        check(res, {
            'convocatorias abiertas status 200': (r) => r.status === 200,
            'convocatorias es array': (r) => {
                try { return Array.isArray(r.json()); } catch(_) { return false; }
            }
        });
    });

    sleep(0.3);

    // 5. Monitorias por profesor (base para plan de actividades)
    group('5. Monitorias por profesor', () => {
        if (!profToken) return;
        const res = http.get(
            `${BASE_URL}/monitoring/getAllByProfessor/${PROFESSOR_ID}`,
            authHeaders(profToken)
        );
        check(res, {
            'monitorias por profesor status 200': (r) => r.status === 200
        });
    });

    sleep(0.3);

    // 6. Reporte de monitores
    group('6. Reporte de monitores', () => {
        if (!profToken) return;
        const res = http.get(
            `${BASE_URL}/monitoring/getMonitorsReport/${PROFESSOR_ID}/professor`,
            authHeaders(profToken)
        );
        check(res, {
            'reporte monitores status 200': (r) => r.status === 200
        });
    });

    // 7. Mis postulaciones (monitor)
    group('7. Mis postulaciones (monitor)', () => {
        if (!monToken) return;
        const res = http.get(
            `${BASE_URL}/monitor-application/monitor/${MONITOR_ID}`,
            authHeaders(monToken)
        );
        check(res, {
            'mis postulaciones status 200': (r) => r.status === 200
        });
    });
}
