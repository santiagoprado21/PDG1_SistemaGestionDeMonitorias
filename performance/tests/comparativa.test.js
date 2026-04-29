/**
 * SIGMA-PERF — Comparativa: pocos usuarios vs datos realistas
 *
 * Demuestra la diferencia de comportamiento del sistema con datos limitados
 * (un solo profesor y monitor) vs datos realistas (10 profesores, 18 monitores).
 *
 * Escenario A — "antes" (pocos datos, usuario único):
 *   k6 run -e ESCENARIO=antes tests/comparativa.test.js
 *
 * Escenario B — "después" (datos realistas, pool diverso):
 *   k6 run -e ESCENARIO=despues tests/comparativa.test.js
 *
 * Con exportación de métricas para comparar lado a lado:
 *   k6 run -e ESCENARIO=antes   --summary-export results/antes.json   tests/comparativa.test.js
 *   k6 run -e ESCENARIO=despues --summary-export results/despues.json  tests/comparativa.test.js
 */

import http from 'k6/http';
import { check, group, sleep } from 'k6';
import { login, authHeaders } from '../helpers/auth.js';
import { BASE_URL } from '../config/env.js';

// ── Pools de usuarios ─────────────────────────────────────────────────────────
const POOL = {
    antes: {
        profesores:   ['1002'],
        monitores:    ['2220004'],
        profPass:     'prof123',
        monitorPass:  '123456',
        label:        'ANTES  (1 profesor / 1 monitor)',
    },
    despues: {
        profesores:   ['1001','1002','1003','1004','1005','1006','1007','1008','1009','1010','1011','1012'],
        monitores:    ['2220006','2220007','2220008','2220009','2220010',
                       '2220011','2220012','2220013','2220014','2220015',
                       '2220016','2220017','2220018','2220019','2220020',
                       '2220021','2220022','2220023','2220024','2220025'],
        profPass:     'prof123',
        monitorPass:  '123456',
        label:        'DESPUÉS (12 profesores / 20 monitores)',
    },
};

const escenario = __ENV.ESCENARIO === 'antes' ? 'antes' : 'despues';
const cfg       = POOL[escenario];

console.log(`\n▶ Escenario: ${cfg.label}\n`);

// ── Opciones ──────────────────────────────────────────────────────────────────
export const options = {
    stages: [
        { duration: '20s', target: 8 },
        { duration: '2m',  target: 8 },
        { duration: '10s', target: 0 },
    ],
    thresholds: {
        checks:          [{ threshold: 'rate==1.00', abortOnFail: false }],
        http_req_failed: [{ threshold: 'rate<0.01',  abortOnFail: false }],
        'http_req_duration{endpoint:plan_actividades}': ['p(95)<3000'],
        'http_req_duration{endpoint:reporte_monitores}': ['p(95)<5000'],
        'http_req_duration{endpoint:reporte_profesor}':  ['p(95)<5000'],
        'http_req_duration{endpoint:reporte_asistencia}':['p(95)<6000'],
    },
};

// ── Selección de usuario por VU ───────────────────────────────────────────────
function profId()    { return cfg.profesores[(__VU - 1) % cfg.profesores.length]; }
function monitorId() { return cfg.monitores[(__VU  - 1) % cfg.monitores.length];  }

// ── Flujo principal ───────────────────────────────────────────────────────────
export default function () {
    const mod = __VU % 4;
    if      (mod === 0) flujoPlan();
    else if (mod === 1) flujoReporteMonitores();
    else if (mod === 2) flujoReporteProfesor();
    else                flujoAsistencia();
    sleep(2);
}

function flujoPlan() {
    const pid = profId();
    let token;
    group('Login profesor', () => {
        token = login(pid, cfg.profPass);
        check(token, { 'login ok': (t) => t !== null });
    });
    if (!token) return;
    sleep(0.3);
    group('Plan actividades (profesor)', () => {
        const res = http.get(`${BASE_URL}/activity/findAll/${pid}/professor`, {
            ...authHeaders(token),
            tags: { endpoint: 'plan_actividades' },
            responseCallback: http.expectedStatuses(200, 201, 204, 400, 401, 403, 404),
        });
        check(res, { 'plan actividades: ok': (r) => r.status < 500 });
    });
}

function flujoReporteMonitores() {
    const pid = profId();
    let token;
    group('Login profesor (reporte monitores)', () => {
        token = login(pid, cfg.profPass);
        check(token, { 'login ok': (t) => t !== null });
    });
    if (!token) return;
    sleep(0.3);
    group('Reporte de monitores', () => {
        const res = http.get(`${BASE_URL}/monitoring/getMonitorsReport/${pid}/professor`, {
            ...authHeaders(token),
            tags: { endpoint: 'reporte_monitores' },
            timeout: '20s',
            responseCallback: http.expectedStatuses(200, 201, 204, 400, 401, 403, 404),
        });
        if (res.status >= 500) console.error(`[reporte_monitores] prof=${pid} status=${res.status}`);
        check(res, { 'reporte monitores: ok': (r) => r.status < 500 });
    });
}

function flujoReporteProfesor() {
    const pid = profId();
    let token;
    group('Login profesor (reporte profesor)', () => {
        token = login(pid, cfg.profPass);
        check(token, { 'login ok': (t) => t !== null });
    });
    if (!token) return;
    sleep(0.3);
    group('Reporte de profesor', () => {
        const res = http.get(`${BASE_URL}/monitoring/getProfessorReport/${pid}`, {
            ...authHeaders(token),
            tags: { endpoint: 'reporte_profesor' },
            timeout: '20s',
            responseCallback: http.expectedStatuses(200, 201, 204, 400, 401, 403, 404),
        });
        if (res.status >= 500) console.error(`[reporte_profesor] prof=${pid} status=${res.status}`);
        check(res, { 'reporte profesor: ok': (r) => r.status < 500 });
    });
}

function flujoAsistencia() {
    const pid = profId();
    let token;
    group('Login profesor (asistencia)', () => {
        token = login(pid, cfg.profPass);
        check(token, { 'login ok': (t) => t !== null });
    });
    if (!token) return;
    sleep(0.3);
    group('Reporte de asistencia', () => {
        const res = http.get(`${BASE_URL}/monitoring/getAttendanceReport/professor/${pid}`, {
            ...authHeaders(token),
            tags: { endpoint: 'reporte_asistencia' },
            timeout: '25s',
            responseCallback: http.expectedStatuses(200, 201, 204, 400, 401, 403, 404),
        });
        if (res.status >= 500) console.error(`[reporte_asistencia] prof=${pid} status=${res.status}`);
        check(res, { 'reporte asistencia: ok': (r) => r.status < 500 });
    });
}
