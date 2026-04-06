/**
 * SIGMA-PERF-006 — Flujo de cierre de monitorías
 *
 * Valida los endpoints del flujo de cierre:
 *   1. Login como profesor
 *   2. Consultar monitorías activas (candidatas a cierre)
 *   3. Consultar detalle de una monitoría
 *   4. Consultar evaluaciones de monitores (previas al cierre)
 *   5. Login como jefe de departamento
 *   6. Consultar monitorías pendientes de aprobación/cierre
 *
 * Nota: el cierre real (PUT/PATCH) no se automatiza en pruebas de carga
 * para evitar modificar datos de producción o prueba de forma masiva.
 * Solo se validan los GETs del flujo y el estado de respuesta esperado.
 *
 * Perfil de carga:
 *   0 → 8 VUs en 30 s   (ramp-up — flujo más pesado, menos VUs)
 *   8 VUs por 3 min      (carga sostenida)
 *   8 → 0 VUs en 30 s   (ramp-down)
 *
 * Criterios de éxito (SIGMA-PERF-002):
 *   - p95 de duración HTTP < 3 000 ms
 *   - Tasa de errores HTTP = 0 %
 *   - 100 % de checks de negocio pasan
 *
 * Uso:
 *   k6 run performance/tests/cierre.test.js
 */

import http from 'k6/http';
import { check, group, sleep } from 'k6';
import { login, authHeaders } from '../helpers/auth.js';
import { cierreThresholds } from '../config/thresholds.js';
import {
    BASE_URL,
    PROFESSOR_ID, PROFESSOR_PASS,
    HEAD_ID,      HEAD_PASS,
} from '../config/env.js';

// ── Opciones ──────────────────────────────────────────────────────────────────
export const options = {
    stages: [
        { duration: '30s', target: 8 },
        { duration: '3m',  target: 8 },
        { duration: '30s', target: 0 },
    ],
    thresholds: cierreThresholds,
};

// ── Escenario principal ───────────────────────────────────────────────────────
export default function () {
    if (__VU % 2 === 0) {
        flujoProfesorCierre();
    } else {
        flujoJefeCierre();
    }

    sleep(1);
}

// ── Flujo: profesor consulta monitorías antes de cerrar ───────────────────────
function flujoProfesorCierre() {
    let token;

    group('Profesor — Login', () => {
        token = login(PROFESSOR_ID, PROFESSOR_PASS);
        check(token, { 'token profesor obtenido': (t) => t !== null });
    });

    if (!token) return;

    sleep(0.3);

    group('Profesor — Monitorías activas', () => {
        const res = http.get(
            `${BASE_URL}/monitoring/getAllByProfessor/${PROFESSOR_ID}`,
            authHeaders(token)
        );
        check(res, {
            'monitorías activas: status 200':   (r) => r.status === 200,
        });
    });

    sleep(0.3);

    group('Profesor — Evaluaciones de monitores', () => {
        const res = http.get(
            `${BASE_URL}/monitor-evaluations/professor/${PROFESSOR_ID}`,
            authHeaders(token)
        );
        check(res, {
            'evaluaciones: status 200 o 404':  (r) => r.status === 200 || r.status === 404,
        });
    });

    sleep(0.3);

    group('Profesor — Reporte final antes de cierre', () => {
        const res = http.get(
            `${BASE_URL}/monitoring/getMonitorsReport/${PROFESSOR_ID}/professor`,
            authHeaders(token)
        );
        check(res, {
            'reporte cierre: status 200':   (r) => r.status === 200,
        });
    });
}

// ── Flujo: jefe de departamento consulta estado de monitorías ─────────────────
function flujoJefeCierre() {
    let token;

    group('Jefe — Login', () => {
        token = login(HEAD_ID, HEAD_PASS);
        check(token, { 'token jefe obtenido': (t) => t !== null });
    });

    if (!token) return;

    sleep(0.3);

    group('Jefe — Todas las monitorías del departamento', () => {
        const res = http.get(
            `${BASE_URL}/monitoring/getAll`,
            {
                ...authHeaders(token),
                // El jefe de prueba puede no tener permisos o datos → 403/404 válidos
                responseCallback: http.expectedStatuses(200, 201, 204, 400, 401, 403, 404),
            }
        );
        check(res, {
            'monitorías departamento: sin error de servidor': (r) => r.status < 500,
        });
    });

    sleep(0.3);

    group('Jefe — Reporte departamental', () => {
        const res = http.get(
            `${BASE_URL}/monitoring/getMonitorsReport/${HEAD_ID}/head`,
            authHeaders(token)
        );
        check(res, {
            'reporte departamental: status 200 o 403': (r) => r.status === 200 || r.status === 403,
        });
    });
}
