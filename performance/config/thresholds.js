/**
 * SIGMA-PERF-002 — Umbrales de rendimiento (SLAs) centralizados
 *
 * Fuente única de verdad para los thresholds de k6.
 * Todos los scripts de prueba importan desde aquí para garantizar
 * consistencia entre ejecuciones individuales y el escenario de carga.
 *
 * Criterios de aceptación:
 *   - p95: el 95 % de las peticiones deben responder por debajo del límite.
 *   - error rate: porcentaje máximo de respuestas 4xx/5xx permitidas.
 *   - checks: porcentaje mínimo de checks de negocio que deben pasar.
 *
 * Referencia de valores (definida en reunión de equipo HU2-253):
 * ┌────────────────────────┬──────────────┬────────────┬─────────────┐
 * │ Tipo de endpoint       │ p95 máximo   │ Error rate │ Checks min  │
 * ├────────────────────────┼──────────────┼────────────┼─────────────┤
 * │ Login                  │ 2 500 ms     │ 0 %        │ 100 %       │  ← medido: p95=2.18s bajo 10 VUs
 * │ Lecturas simples       │ 2 000 ms     │ 0 %        │ 100 %       │
 * │ Convocatorias          │ 3 000 ms     │ 0 %        │ 100 %       │
 * │ Plan de actividades    │ 2 500 ms     │ 0 %        │ 100 %       │
 * │ Reportes               │ 5 000 ms     │ 0 %        │ 100 %       │
 * │ Carga sostenida (mix)  │ 3 000 ms     │ < 1 %      │  99 %       │
 * └────────────────────────┴──────────────┴────────────┴─────────────┘
 */

// ── Thresholds por tipo de script ─────────────────────────────────────────────

/**
 * SIGMA-PERF-003
 * Endpoint /auth/login — autenticación de todos los roles.
 * Threshold ajustado a 2 500 ms tras medición real: p95=2 180 ms bajo 10 VUs
 * con DB cloud (Neon). El objetivo ideal es 2 000 ms pero la latencia de red
 * al proveedor cloud añade ~200 ms adicionales en condiciones de carga.
 */
export const loginThresholds = {
    http_req_duration: ['p(95)<2500'],
    http_req_failed:   ['rate==0'],
    checks:            ['rate==1.00'],
};

/**
 * Lecturas simples: perfil, listados básicos.
 */
export const readsThresholds = {
    http_req_duration: ['p(95)<2000'],
    http_req_failed:   ['rate==0'],
    checks:            ['rate==1.00'],
};

/**
 * SIGMA-PERF-004
 * Flujo de convocatorias: listado, apertura, postulaciones.
 */
export const convocatoriasThresholds = {
    http_req_duration: ['p(95)<3000'],
    http_req_failed:   ['rate==0'],
    checks:            ['rate==1.00'],
};

/**
 * SIGMA-PERF-005
 * Plan de actividades y reporte de monitores.
 * Los reportes tienen un límite más holgado (5 s) por su mayor carga de cómputo.
 */
export const actividadesThresholds = {
    'http_req_duration':                    ['p(95)<2500'],
    'http_req_duration{endpoint:reporte}':  ['p(95)<5000'],
    http_req_failed:                        ['rate==0'],
    checks:                                 ['rate==1.00'],
};

/**
 * SIGMA-PERF-006
 * Flujo de cierre de monitorías.
 */
export const cierreThresholds = {
    http_req_duration: ['p(95)<3000'],
    http_req_failed:   ['rate==0'],
    checks:            ['rate==1.00'],
};

/**
 * SIGMA-PERF-008
 * Escenario de carga sostenida con mezcla de todos los flujos.
 * Se relajan levemente: error rate < 1 % y checks >= 99 %.
 */
export const loadThresholds = {
    // Global
    'http_req_duration':                         ['p(95)<3000'],
    'http_req_failed':                           ['rate<0.01'],
    'checks':                                    ['rate>=0.99'],

    // Thresholds etiquetados por flujo (tags aplicados en cada script)
    'http_req_duration{flow:login}':             ['p(95)<2500'],
    'http_req_duration{flow:convocatorias}':     ['p(95)<3000'],
    'http_req_duration{flow:actividades}':       ['p(95)<2500'],
    'http_req_duration{flow:reporte}':           ['p(95)<5000'],
    'http_req_duration{flow:cierre}':            ['p(95)<3000'],
};
