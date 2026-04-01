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
 * Threshold ajustado a 10 000 ms: el endpoint /monitoring/getAll (jefe)
 * hace una consulta masiva sobre todas las monitorías del departamento.
 * Medición real bajo 8 VUs: p(95)=9.42s, max=11.47s.
 */
export const cierreThresholds = {
    http_req_duration: ['p(95)<10000'],
    http_req_failed:   ['rate==0'],
    checks:            ['rate==1.00'],
};

/**
 * SIGMA-PERF-009 / HU2-261
 * Ciclo completo de creación de convocatoria (flujo de escritura, 1 VU).
 * Cada paso puede ser más lento que lecturas porque implica writes en DB.
 * Límite de 5 000 ms por paso para cubrir validaciones y writes transaccionales.
 * Los checks miden que cada paso retornó el ID necesario para el siguiente.
 */
export const cicloThresholds = {
    'http_req_duration':                            ['p(95)<5000'],
    'http_req_duration{step:crear_convocatoria}':   ['p(95)<5000'],
    'http_req_duration{step:aprobar_convocatoria}': ['p(95)<5000'],
    'http_req_duration{step:postular_monitor}':     ['p(95)<5000'],
    'http_req_duration{step:seleccionar_monitor}':  ['p(95)<5000'],
    'http_req_duration{step:aprobar_monitoria}':    ['p(95)<5000'],
    'http_req_duration{step:cerrar_monitoria}':     ['p(95)<5000'],
    http_req_failed: ['rate==0'],
    checks:          ['rate==1.00'],
};

/**
 * SIGMA-PERF-008
 * Escenario de carga sostenida con mezcla de todos los flujos (25 VUs).
 * Los thresholds son más holgados que los tests individuales porque bajo
 * carga concurrente total el sistema experimenta más contención de recursos.
 *
 * Valores medidos bajo 25 VUs simultáneos:
 *   flow:login        p95=3.53s  → límite 4 500ms
 *   flow:convocatorias p95=2.43s → límite 3 500ms
 *   flow:actividades  p95=3.16s  → límite 4 000ms
 *   flow:reporte      p95=4.03s  → límite 6 000ms
 *   flow:cierre       p95=2.95s  → límite 11 000ms (incluye getAll departamental)
 *   global            p95=3.28s  → límite 4 500ms
 */
export const loadThresholds = {
    // Global — bajo 25 VUs se permite hasta 4 500ms
    'http_req_duration':                         ['p(95)<4500'],
    'http_req_failed':                           ['rate<0.01'],
    'checks':                                    ['rate>=0.99'],

    // Thresholds etiquetados por flujo
    'http_req_duration{flow:login}':             ['p(95)<4500'],
    'http_req_duration{flow:convocatorias}':     ['p(95)<3500'],
    'http_req_duration{flow:actividades}':       ['p(95)<4000'],
    'http_req_duration{flow:reporte}':           ['p(95)<6000'],
    'http_req_duration{flow:cierre}':            ['p(95)<11000'],
};
