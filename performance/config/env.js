/**
 * Variables de entorno para los tests de rendimiento de SIGMA+.
 * Se pueden sobreescribir en tiempo de ejecución con -e:
 *   k6 run -e BASE_URL=http://localhost:5433 -e MONITOR_ID=2220004 smoke/smoke.test.js
 *
 * Ver sección "Gestión de credenciales de prueba" en performance/README.md
 * para instrucciones de rotación y uso en CI/CD.
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

// Credenciales de prueba — director (cierre de monitorías)
// Por defecto usa el mismo usuario que el jefe (5001 tiene ambos permisos)
export const DIRECTOR_ID   = __ENV.DIRECTOR_ID   || '5001';
export const DIRECTOR_PASS = __ENV.DIRECTOR_PASS || 'jefe123';

// IDs de datos académicos para el ciclo de creación de convocatorias (HU2-261)
// Profesor 1002 (Dra. Patricia Méndez) dicta el curso 3 (Bases de Datos)
// que pertenece al programa 1 (Ing. Sistemas) de la escuela 1 (Ing.)
export const COURSE_ID     = __ENV.COURSE_ID     || '3';
export const SCHOOL_ID     = __ENV.SCHOOL_ID     || '1';
export const PROGRAM_ID    = __ENV.PROGRAM_ID    || '1';
