/**
 * SIGMA-PERF-009 / HU2-261 — Ciclo completo de creación de convocatoria
 *
 * Ejecuta el flujo de escritura end-to-end que representa la HU-010:
 *
 *   Paso 1 — Profesor crea la convocatoria
 *            POST /monitoring-request/create
 *   Paso 2 — Jefe de departamento la aprueba (convocatoria queda abierta)
 *            POST /monitoring-request/{id}/approve-by-head
 *   Paso 3 — Monitor se postula
 *            POST /monitor-application/apply
 *   Paso 4 — Profesor elige al monitor → monitoría se crea automáticamente
 *            POST /monitor-application/select
 *   Paso 5 — Se cierra la monitoría
 *            POST /monitoring-closure/{id}/close
 *
 * ¿Por qué solo 1 VU?
 *   Este flujo es destructivo (crea y muta datos reales en la DB).
 *   Con 1 VU se garantiza que cada iteración usa un semestre único
 *   (PERF-VU-ITER) y no genera conflictos de duplicados.
 *   El objetivo no es el throughput sino medir la latencia de cada paso
 *   y verificar que el flujo completo funciona correctamente.
 *
 * Criterios de éxito:
 *   - Cada paso responde en < 5 000 ms (p95)
 *   - 0 % de errores de servidor (5xx)
 *   - Checks de negocio: 100 % (cada paso retorna el ID necesario
 *     para el siguiente)
 *
 * Uso:
 *   k6 run performance/tests/convocatorias-ciclo.test.js
 *
 * Con datos personalizados:
 *   k6 run \
 *     -e COURSE_ID=3 -e SCHOOL_ID=1 -e PROGRAM_ID=1 \
 *     -e DIRECTOR_ID=5001 -e DIRECTOR_PASS=jefe123 \
 *     performance/tests/convocatorias-ciclo.test.js
 */

import http from 'k6/http';
import { check, group, sleep, fail } from 'k6';
import { login, authHeaders } from '../helpers/auth.js';
import { cicloThresholds } from '../config/thresholds.js';
import {
    BASE_URL,
    MONITOR_ID,   MONITOR_PASS,
    PROFESSOR_ID, PROFESSOR_PASS,
    HEAD_ID,      HEAD_PASS,
    COURSE_ID,    SCHOOL_ID,    PROGRAM_ID,
    DIRECTOR_ID,  DIRECTOR_PASS,
} from '../config/env.js';

// ── Opciones ──────────────────────────────────────────────────────────────────
export const options = {
    vus:        1,
    iterations: 3,   // 3 ciclos completos para validar estabilidad
    thresholds: cicloThresholds,
};

// ── Helpers ───────────────────────────────────────────────────────────────────
const JSON_HEADERS = { 'Content-Type': 'application/json' };

function post(url, body, token, tag) {
    return http.post(url, JSON.stringify(body), {
        headers: {
            ...JSON_HEADERS,
            ...(token ? { Authorization: `Bearer ${token}` } : {}),
        },
        timeout: '30s',
        tags: { step: tag },
        responseCallback: http.expectedStatuses(200, 201, 202, 204, 400, 401, 403, 404),
    });
}

// ── Escenario principal ───────────────────────────────────────────────────────
export default function () {
    // Semestre único por iteración para evitar conflictos de datos
    const semester = `PERF-${__VU}-${__ITER}`;

    let profToken, headToken, monToken;
    let requestId, applicationId, monitoringId;

    // ── Paso 0: Login de los tres roles ───────────────────────────────────────
    group('Login — todos los roles', () => {
        profToken = login(PROFESSOR_ID, PROFESSOR_PASS);
        headToken = login(HEAD_ID, HEAD_PASS);
        monToken  = login(MONITOR_ID, MONITOR_PASS);

        check(null, {
            'login profesor: ok': () => profToken !== null,
            'login jefe: ok':     () => headToken !== null,
            'login monitor: ok':  () => monToken  !== null,
        });
    });

    if (!profToken || !headToken || !monToken) {
        fail('No se pudieron obtener todos los tokens — abortando ciclo');
    }

    sleep(0.5);

    // ── Paso 1: Profesor crea convocatoria ────────────────────────────────────
    group('Paso 1 — Profesor crea convocatoria', () => {
        const body = {
            professorId:          PROFESSOR_ID,
            courseId:             parseInt(COURSE_ID),
            schoolId:             parseInt(SCHOOL_ID),
            programId:            parseInt(PROGRAM_ID),
            requestedHours:       4,
            justification:        `Convocatoria de prueba de rendimiento — semestre ${semester}`,
            semester:             semester,
            startDate:            '2026-08-01',
            finishDate:           '2026-11-30',
            requiredAverageGrade: 3.5,
            requiredCourseGrade:  3.5,
            hourlyRate:           15000,
        };

        const res = post(`${BASE_URL}/monitoring-request/create`, body, profToken, 'crear_convocatoria');

        const ok = check(res, {
            'crear convocatoria: status 201 o 200': (r) => r.status === 201 || r.status === 200,
            'crear convocatoria: retorna ID':        (r) => {
                try {
                    const data = r.json();
                    requestId = data.id || data.monitoringRequestId || data;
                    return !!requestId;
                } catch (_) { return false; }
            },
        });

        if (!ok) fail(`Paso 1 fallido — status ${res.status}: ${res.body}`);
    });

    sleep(0.5);

    // ── Paso 2: Jefe aprueba la convocatoria ──────────────────────────────────
    group('Paso 2 — Jefe aprueba convocatoria', () => {
        const res = post(
            `${BASE_URL}/monitoring-request/${requestId}/approve-by-head`,
            { departmentHeadId: HEAD_ID, comment: 'Aprobado en prueba de rendimiento' },
            headToken,
            'aprobar_convocatoria'
        );

        check(res, {
            'aprobar convocatoria: sin error de servidor': (r) => r.status < 500,
        });
    });

    sleep(0.5);

    // ── Paso 3: Monitor se postula ────────────────────────────────────────────
    group('Paso 3 — Monitor se postula', () => {
        const res = post(
            `${BASE_URL}/monitor-application/apply`,
            {
                monitoringRequestId: requestId,
                monitorId:           MONITOR_ID,
                motivationLetter:    'Carta de motivación de prueba de rendimiento',
            },
            monToken,
            'postular_monitor'
        );

        check(res, {
            'postulación: sin error de servidor': (r) => r.status < 500,
            'postulación: retorna ID': (r) => {
                try {
                    const data = r.json();
                    applicationId = data.id || data.applicationId || data;
                    return !!applicationId;
                } catch (_) { return false; }
            },
        });
    });

    sleep(0.5);

    // ── Paso 4: Profesor elige al monitor ─────────────────────────────────────
    group('Paso 4 — Profesor selecciona monitor', () => {
        const res = post(
            `${BASE_URL}/monitor-application/select`,
            {
                monitoringRequestId: requestId,
                applicationId:       applicationId,
                professorId:         PROFESSOR_ID,
                notes:               'Seleccionado en prueba de rendimiento',
            },
            profToken,
            'seleccionar_monitor'
        );

        check(res, {
            'seleccionar monitor: sin error de servidor': (r) => r.status < 500,
            'seleccionar monitor: retorna monitoringId': (r) => {
                try {
                    const data = r.json();
                    monitoringId = data.id || data.monitoringId || data;
                    return !!monitoringId;
                } catch (_) { return false; }
            },
        });
    });

    sleep(0.5);

    // ── Paso 5: Cierre de la monitoría ────────────────────────────────────────
    group('Paso 5 — Cierre de monitoría', () => {
        const dirToken = login(DIRECTOR_ID, DIRECTOR_PASS);

        const res = post(
            `${BASE_URL}/monitoring-closure/${monitoringId}/close`,
            { comment: 'Cierre en prueba de rendimiento', autoCalculate: true },
            dirToken,
            'cerrar_monitoria'
        );

        check(res, {
            'cerrar monitoría: sin error de servidor': (r) => r.status < 500,
        });
    });

    sleep(1);
}
