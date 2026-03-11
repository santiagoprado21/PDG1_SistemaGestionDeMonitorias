/**
 * Variables de entorno para los tests de rendimiento de SIGMA+.
 * Se pueden sobreescribir en tiempo de ejecución con -e:
 *   k6 run -e BASE_URL=http://localhost:5433 -e MONITOR_ID=A00123 smoke/smoke.test.js
 */

export const BASE_URL      = __ENV.BASE_URL      || 'http://localhost:5433';

// Credenciales de prueba — monitor
export const MONITOR_ID    = __ENV.MONITOR_ID    || '2220004';
export const MONITOR_PASS  = __ENV.MONITOR_PASS  || '123456';

// Credenciales de prueba — profesor
export const PROFESSOR_ID  = __ENV.PROFESSOR_ID  || '1002';
export const PROFESSOR_PASS = __ENV.PROFESSOR_PASS || 'prof123';

// Credenciales de prueba — jefe de departamento
export const HEAD_ID       = __ENV.HEAD_ID       || '5001';
export const HEAD_PASS     = __ENV.HEAD_PASS     || 'jefe123';
