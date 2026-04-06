# Tests de rendimiento — SIGMA+ con k6

Proyecto de pruebas de rendimiento para la API de SIGMA+.  
Basado en [k6](https://k6.io/) — herramienta open-source de carga y rendimiento.

---

## Estructura

```
performance/
├── config/
│   ├── env.js                  # Variables de entorno (BASE_URL, credenciales)
│   └── thresholds.js           # SLAs centralizados (SIGMA-PERF-002)
├── helpers/
│   └── auth.js                 # Helper de login y headers de autorización
├── smoke/
│   └── smoke.test.js           # Smoke: 1 iteración por flujo crítico (< 2 min)
├── tests/
│   ├── login.test.js           # SIGMA-PERF-003: carga en /auth/login
│   ├── convocatorias.test.js   # SIGMA-PERF-004: flujo de convocatorias
│   ├── actividades.test.js     # SIGMA-PERF-005: plan de actividades y reportes
│   └── cierre.test.js          # SIGMA-PERF-006: cierre de monitorías
├── scenarios/
│   └── load.test.js            # SIGMA-PERF-008: carga sostenida (mezcla de flujos)
└── README.md
```

---

## Requisitos

- **k6** instalado localmente.
- Los tres backends corriendo:
  - `PDG-SIGMA-BACKEND-main` en **`http://localhost:5433`**
  - `API-Banner-main` en **`http://localhost:5435`**
  - Frontend (opcional para smoke) en **`http://localhost:3000`**


## Variables de entorno

Las credenciales y la URL base se pueden pasar como variables `-e` sin modificar el código.

| Variable        | Por defecto              | Descripción                            |
|-----------------|--------------------------|----------------------------------------|
| `BASE_URL`      | `http://localhost:5433`  | URL base del backend SIGMA             |
| `MONITOR_ID`    | `2220004`                | ID del monitor de prueba               |
| `MONITOR_PASS`  | `123456`                 | Contraseña del monitor de prueba       |
| `PROFESSOR_ID`  | `1002`                   | ID del profesor de prueba              |
| `PROFESSOR_PASS`| `prof123`                | Contraseña del profesor de prueba      |
| `HEAD_ID`       | `5001`                   | ID del jefe de departamento de prueba  |
| `HEAD_PASS`     | `jefe123`                | Contraseña del jefe de prueba          |

> **Nota de seguridad:** nunca subas credenciales reales al repositorio.  
> Úsalas solo con `-e` en la terminal o desde variables del entorno CI/CD.

---

## Umbrales de rendimiento (SLA — SIGMA-PERF-002)

Los SLAs están definidos en **`config/thresholds.js`** y son la única fuente de verdad.  
Todos los scripts importan sus thresholds desde ese módulo.

### Tabla de SLAs por tipo de endpoint

| Tipo de endpoint        | p95 máximo   | Error rate | Checks mínimo | Script               |
|-------------------------|--------------|------------|---------------|----------------------|
| Login (todos los roles) | 2 000 ms     | 0 %        | 100 %         | `tests/login.test.js`         |
| Lecturas simples        | 2 000 ms     | 0 %        | 100 %         | `smoke/smoke.test.js`         |
| Convocatorias           | 3 000 ms     | 0 %        | 100 %         | `tests/convocatorias.test.js` |
| Plan de actividades     | 2 500 ms     | 0 %        | 100 %         | `tests/actividades.test.js`   |
| Reportes                | 5 000 ms     | 0 %        | 100 %         | `tests/actividades.test.js`   |
| Cierre de monitorías    | 10 000 ms    | 0 %        | 100 %         | `tests/cierre.test.js`        |
| Carga sostenida (mix)   | 4 500 ms     | < 1 %      | ≥ 99 %        | `scenarios/load.test.js` (15 VUs) |

### Thresholds etiquetados en el escenario de carga

El escenario `load.test.js` usa **tags** (`flow:login`, `flow:convocatorias`, etc.)
para aplicar límites diferenciados dentro del mismo script:

| Tag                        | p95 máximo |
|----------------------------|------------|
| `flow:login`               | 2 000 ms   |
| `flow:convocatorias`       | 3 000 ms   |
| `flow:actividades`         | 2 500 ms   |
| `flow:reporte`             | 5 000 ms   |
| `flow:cierre`              | 10 000 ms  |

### Justificación de valores

- **Login (2 s):** operación crítica de entrada; usuarios abandonan si supera 2 s.
- **Lecturas simples (2 s):** listados de datos ya cargados en base de datos.
- **Convocatorias (3 s):** consultas con JOINs a múltiples tablas.
- **Actividades (2.5 s):** consultas paginadas de planes de trabajo.
- **Reportes (5 s):** agregaciones complejas sobre todas las monitorías; valor más holgado aceptado por el equipo.
- **Carga mix (3 s, < 1 % error):** bajo concurrencia alta se permite una degradación mínima.

---

## Perfiles de carga por script

| Script                        | VUs | Etapas                              | Duración total |
|-------------------------------|-----|-------------------------------------|----------------|
| `smoke/smoke.test.js`         |  1  | 1 iteración                         | < 2 min        |
| `tests/login.test.js`         | 10  | ramp 30 s → 3 min sostenido → 30 s  | ~4 min         |
| `tests/convocatorias.test.js` | 15  | ramp 30 s → 3 min sostenido → 30 s  | ~4 min         |
| `tests/actividades.test.js`   | 10  | ramp 30 s → 3 min sostenido → 30 s  | ~4 min         |
| `tests/cierre.test.js`        |  8  | ramp 30 s → 3 min sostenido → 30 s  | ~4 min         |
| `scenarios/load.test.js`      | 25  | 4 escenarios paralelos × 5 min      | ~5 min         |

---

## Cómo ejecutar los tests

Todos los comandos se corren desde la raíz del repositorio o desde `performance/`.

### Smoke (verificación rápida — < 2 min)
```bash
k6 run performance/smoke/smoke.test.js
```

Con credenciales personalizadas:
```bash
k6 run \
  -e BASE_URL=http://localhost:5433 \
  -e PROFESSOR_ID=P001 -e PROFESSOR_PASS=miPass \
  -e MONITOR_ID=A00381698 -e MONITOR_PASS=miPass \
  performance/smoke/smoke.test.js
```

### Tests individuales por flujo
```bash
# Login (SIGMA-PERF-003)
k6 run performance/tests/login.test.js

# Convocatorias (SIGMA-PERF-004)
k6 run performance/tests/convocatorias.test.js

# Plan de actividades y reportes (SIGMA-PERF-005)
k6 run performance/tests/actividades.test.js

# Cierre de monitorías (SIGMA-PERF-006)
k6 run performance/tests/cierre.test.js
```

### Carga sostenida — todos los flujos en paralelo (SIGMA-PERF-008)
```bash
k6 run performance/scenarios/load.test.js
```

### Exportar resultados a JSON
```bash
k6 run --out json=performance/results/smoke-$(date +%Y%m%d-%H%M%S).json \
  performance/smoke/smoke.test.js
```

---

## Interpretar resultados

Cuando el test termina, k6 muestra un resumen en consola. Los campos clave son:

```
http_req_duration............: avg=120ms  p(90)=200ms  p(95)=250ms
http_req_failed..............: 0.00%
checks.......................: 100.00%
```

- **`http_req_duration p(95)`** — el 95 % de las peticiones tardaron menos de ese valor.
- **`http_req_failed`** — porcentaje de peticiones con error HTTP (4xx/5xx).
- **`checks`** — porcentaje de validaciones de negocio (`check()`) que pasaron.

Si algún threshold se incumple, la salida muestra `FAILED` en rojo y el proceso
retorna código de salida ≠ 0 — ideal para integrar en pipelines CI/CD.

```
✓ http_req_duration.........: p(95)=1850ms  < 2000ms  ✓
✗ http_req_failed...........: 1.20%         >= 0%      ✗  ← FALLO
```

---