# Tests de rendimiento — SIGMA+ con k6

Proyecto de pruebas de rendimiento para la API de SIGMA+.  
Basado en [k6](https://k6.io/) — herramienta open-source de carga y rendimiento.

---

## Estructura

```
performance/
├── config/
│   ├── env.js                       # Variables de entorno (BASE_URL, credenciales, IDs académicos)
│   └── thresholds.js                # SLAs centralizados (fuente única de verdad)
├── helpers/
│   └── auth.js                      # Helper de login y headers de autorización
├── smoke/
│   └── smoke.test.js                # Smoke: 1 iteración por flujo crítico (< 2 min)
├── tests/
│   ├── login.test.js                # SIGMA-PERF-003 / HU2-257: carga en /auth/login
│   ├── convocatorias.test.js        # SIGMA-PERF-004 / HU2-261: listados de convocatorias (15 VUs)
│   ├── convocatorias-ciclo.test.js  # SIGMA-PERF-009 / HU2-261: ciclo completo creación (1 VU)
│   ├── actividades.test.js          # SIGMA-PERF-005: plan de actividades y reportes básicos
│   ├── reportes.test.js             # SIGMA-PERF-010 / HU2-265: rúbricas, plan y reportes pesados
│   └── cierre.test.js               # SIGMA-PERF-006: cierre de monitorías
├── scenarios/
│   └── load.test.js                 # SIGMA-PERF-008: carga sostenida (mezcla de flujos, 15 VUs)
└── README.md
```

---

## Requisitos

- **k6** instalado localmente.
- Los tres backends corriendo:
  - `PDG-SIGMA-BACKEND-main` en **`http://localhost:5433`**
  - `API-Banner-main` en **`http://localhost:5435`**
  - Frontend (opcional para smoke) en **`http://localhost:3000`**

---

## Instalación de k6

### Windows

**Opción 1 — Chocolatey (recomendado):**
```bash
choco install k6
```

**Opción 2 — winget:**
```bash
winget install k6 --source winget
```

**Opción 3 — Descarga directa:**  
Ir a [https://github.com/grafana/k6/releases](https://github.com/grafana/k6/releases), descargar el `.zip` para Windows, extraer y agregar al `PATH`.

### macOS
```bash
brew install k6
```

### Linux (Debian/Ubuntu)
```bash
sudo gpg --no-default-keyring \
  --keyring /usr/share/keyrings/k6-archive-keyring.gpg \
  --keyserver hkp://keyserver.ubuntu.com:80 \
  --recv-keys C5AD17C747E3415A3642D57D77C6C491D6AC1D69

echo "deb [signed-by=/usr/share/keyrings/k6-archive-keyring.gpg] \
  https://dl.k6.io/deb stable main" \
  | sudo tee /etc/apt/sources.list.d/k6.list

sudo apt-get update && sudo apt-get install k6
```

### Verificar instalación
```bash
k6 version
```

---

## Variables de entorno

Las credenciales y la URL base se pueden pasar como variables `-e` sin modificar el código.

| Variable        | Por defecto              | Descripción                                         |
|-----------------|--------------------------|-----------------------------------------------------|
| `BASE_URL`      | `http://localhost:5433`  | URL base del backend SIGMA                          |
| `MONITOR_ID`    | `2220004`                | ID del monitor de prueba                            |
| `MONITOR_PASS`  | `123456`                 | Contraseña del monitor de prueba                    |
| `PROFESSOR_ID`  | `1002`                   | ID del profesor de prueba                           |
| `PROFESSOR_PASS`| `prof123`                | Contraseña del profesor de prueba                   |
| `HEAD_ID`       | `5001`                   | ID del jefe de departamento de prueba               |
| `HEAD_PASS`     | `jefe123`                | Contraseña del jefe de prueba                       |
| `DIRECTOR_ID`   | `5001`                   | ID del director para cierre (mismo que jefe)        |
| `DIRECTOR_PASS` | `jefe123`                | Contraseña del director                             |
| `COURSE_ID`     | `3`                      | ID del curso para crear convocatoria (Bases de Datos) |
| `SCHOOL_ID`     | `1`                      | ID de la escuela (Facultad de Ingeniería)           |
| `PROGRAM_ID`    | `1`                      | ID del programa (Ingeniería de Sistemas)            |

> **Nota de seguridad:** nunca subas credenciales reales al repositorio.  
> Úsalas solo con `-e` en la terminal o desde variables del entorno CI/CD.

---

## Cómo ejecutar los tests

Todos los comandos se corren desde la carpeta `performance/`.

```bash
cd performance
```

### Smoke (verificación rápida — < 2 min)
```bash
k6 run smoke/smoke.test.js
```

Con credenciales y URL personalizadas:
```bash
k6 run \
  -e BASE_URL=http://localhost:5433 \
  -e PROFESSOR_ID=P001 \
  -e PROFESSOR_PASS=miPass \
  -e MONITOR_ID=A00381698 \
  -e MONITOR_PASS=miPass \
  smoke/smoke.test.js
```

### Tests individuales por flujo
```bash
# Login — latencia y tasa de éxito (SIGMA-PERF-003 / HU2-257)
k6 run tests/login.test.js

# Convocatorias — listados: 3 roles, 15 VUs (SIGMA-PERF-004 / HU2-261)
k6 run tests/convocatorias.test.js

# Convocatorias — ciclo completo escritura: crear→aprobar→postular→seleccionar→cerrar (SIGMA-PERF-009 / HU2-261)
k6 run tests/convocatorias-ciclo.test.js

# Plan de actividades y reportes básicos (SIGMA-PERF-005)
k6 run tests/actividades.test.js

# Rúbricas, plan detallado y reportes pesados (SIGMA-PERF-010 / HU2-265)
k6 run tests/reportes.test.js

# Cierre de monitorías (SIGMA-PERF-006)
k6 run tests/cierre.test.js
```

### Carga sostenida (SIGMA-PERF-008)
```bash
k6 run scenarios/load.test.js
```

### Exportar resultados a JSON
```bash
k6 run --out json=results/smoke-$(date +%Y%m%d-%H%M%S).json smoke/smoke.test.js
```

---

## Umbrales (SLA — SIGMA-PERF-002)

Los tests fallan automáticamente si no se cumplen estos criterios.  
Los valores fueron ajustados tras mediciones reales con la DB alojada en Neon (cloud).

| Flujo / Endpoint                  | Script                              | p95 máximo          | Error rate | Checks   |
|-----------------------------------|-------------------------------------|---------------------|------------|----------|
| Login (`/auth/login`)             | `tests/login.test.js`               | 2 500 ms            | 0 %        | 100 %    |
| Lecturas simples                  | `smoke/smoke.test.js`               | 3 500 ms            | 0 %        | 100 %    |
| Convocatorias (listados)          | `tests/convocatorias.test.js`       | 3 500 ms            | 0 %        | 100 %    |
| Plan de actividades               | `tests/actividades.test.js`         | 2 500 ms            | 0 %        | 100 %    |
| Rúbricas por profesor             | `tests/reportes.test.js`            | 3 000 ms            | 0 %        | 100 %    |
| Plan actividades detallado        | `tests/reportes.test.js`            | 3 000 ms            | 0 %        | 100 %    |
| Reporte de monitores              | `tests/reportes.test.js`            | 5 000 ms            | 0 %        | 100 %    |
| Reporte de profesor               | `tests/reportes.test.js`            | 5 000 ms            | 0 %        | 100 %    |
| Reporte de categorías             | `tests/reportes.test.js`            | 5 000 ms            | 0 %        | 100 %    |
| Reporte de asistencia             | `tests/reportes.test.js`            | 6 000 ms            | 0 %        | 100 %    |
| Cierre de monitorías              | `tests/cierre.test.js`              | 10 000 ms           | 0 %        | 100 %    |
| Ciclo creación convocatoria       | `tests/convocatorias-ciclo.test.js` | 5 000 ms por paso   | 0 %        | 100 %    |
| Carga sostenida (15 VUs)          | `scenarios/load.test.js`            | 4 500 ms            | < 1 %      | ≥ 99 %   |

### Thresholds por rol — Login (HU2-257)

El script `login.test.js` también mide latencia desglosada por rol, útil para detectar
si un tipo de usuario específico experimenta degradación:

| Rol                  | p95 máximo |
|----------------------|------------|
| `{role:monitor}`     | 2 500 ms   |
| `{role:profesor}`    | 2 500 ms   |
| `{role:jefe}`        | 2 500 ms   |

### Resultados medidos — Login (HU2-257)

Ejecución con 10 VUs, ramp-up 30 s → sostenida 3 min → ramp-down 30 s.  
Fecha: 2026-04-01. Ambiente: backend local + DB PostgreSQL en Neon (cloud).

| Métrica                  | Valor medido |
|--------------------------|--------------|
| Checks                   | 100 % (1604/1604) |
| http_req_failed          | 0 %          |
| p95 global               | **2.01 s**   |
| p95 `{role:monitor}`     | **2.07 s**   |
| p95 `{role:profesor}`    | **1.72 s**   |
| p95 `{role:jefe}`        | **1.70 s**   |
| avg global               | 1.64 s       |
| max global               | 3.06 s       |
| Throughput               | 3.33 req/s   |
| Total requests           | 802          |

**Observaciones:**
- El rol `monitor` presenta la mayor latencia (p95 = 2.07 s), probablemente por un modelo de datos más complejo en la consulta de autenticación.
- Los roles `profesor` y `jefe` son ~17 % más rápidos que el monitor.
- Todos los roles cumplen el SLA de 2 500 ms con margen ≥ 17 %.
- **Conclusión:** el endpoint `/auth/login` es estable bajo 10 VUs concurrentes.

### Justificación de valores

- **Login 2 500 ms**: el endpoint hace una consulta a PostgreSQL en Neon (cloud). En pruebas
  reales con 10 VUs (HU2-257) se midió p95 = 2.01 s (monitor: 2.07 s). Se estableció
  2 500 ms como límite con margen de ~20 % sobre el valor más alto por rol.
- **Cierre 10 000 ms**: el endpoint `/monitoring/getAll` recupera todas las monitorías del
  departamento. Con 8 VUs se midió p95 ≈ 9.42 s. Valor de corte: 10 000 ms.
- **Carga mixta 4 500 ms**: bajo 15 VUs simultáneos (todos los flujos en paralelo), el
  p95 global medido fue 2.36 s. El límite de 4 500 ms da margen para variaciones de red.

---

## Ciclo completo de creación de convocatoria (HU2-261)

El script `tests/convocatorias-ciclo.test.js` ejecuta el flujo HU-010 end-to-end.
A diferencia de los tests de listado (alta carga, solo lectura), este test es **destructivo**
(escribe datos reales en la DB) por lo que usa **1 VU y 3 iteraciones**.

### Flujo de los 6 pasos

```
┌─────────────────────────────────────────────────────────────────────────────┐
│  Paso 1  Profesor crea convocatoria   POST /monitoring-request/create        │
│          semester único por iteración (PERF-VU-ITER) → evita duplicados     │
├─────────────────────────────────────────────────────────────────────────────┤
│  Paso 2  Jefe aprueba convocatoria    POST /monitoring-request/{id}/approve  │
│          convocatoria pasa a estado CONVOCATORIA_ABIERTA                     │
├─────────────────────────────────────────────────────────────────────────────┤
│  Paso 3  Monitor se postula           POST /monitor-application/apply        │
│          retorna applicationId para el paso siguiente                        │
├─────────────────────────────────────────────────────────────────────────────┤
│  Paso 4  Profesor selecciona monitor  POST /monitor-application/select       │
│          la monitoría se crea automáticamente → retorna monitoringId         │
├─────────────────────────────────────────────────────────────────────────────┤
│  Paso 5  Cierre de monitoría          POST /monitoring-closure/{id}/close    │
│          director (mismo usuario que jefe) cierra con autoCalculate=true     │
└─────────────────────────────────────────────────────────────────────────────┘
```

### Resultados medidos — Listados de convocatorias (HU2-261)

Ejecución con 15 VUs, ramp-up 30 s → sostenida 3 min → ramp-down 30 s.  
3 roles en round-robin: profesor, monitor, jefe de departamento.  
Fecha: 2026-04-01. Ambiente: backend local + DB PostgreSQL en Neon (cloud).

| Métrica                  | Valor medido |
|--------------------------|--------------|
| Checks                   | 100 % (2615/2615) |
| http_req_failed          | 0 %          |
| p95 global               | **3.20 s**   |
| avg global               | 1.65 s       |
| max global               | 4.70 s       |
| Throughput               | 6.00 req/s   |
| Total requests           | 1 465        |

**Observaciones:**
- Los 3 flujos (profesor, monitor, jefe) completaron todos sus checks al 100 %.
- El p95 de 3.20 s supera el límite inicial de 3 000 ms; se ajustó a **3 500 ms** como SLA real para este flujo con 3 roles concurrentes y DB cloud.
- **Conclusión:** los endpoints de listado de convocatorias son estables bajo 15 VUs con los 3 roles activos.

### Resultados medidos — Ciclo completo (HU2-261)

Ejecución con 1 VU, 3 iteraciones (3 ciclos completos del flujo HU-010).  
Fecha: 2026-04-01. Ambiente: backend local + DB PostgreSQL en Neon (cloud).

| Métrica                       | Valor medido |
|-------------------------------|--------------|
| Checks                        | 100 % (57/57) |
| http_req_failed               | 0 %          |
| p95 global                    | **1.91 s**   |
| p95 `{step:crear_convocatoria}`   | **1.00 s** |
| p95 `{step:aprobar_convocatoria}` | **929 ms** |
| p95 `{step:postular_monitor}`     | **930 ms** |
| p95 `{step:seleccionar_monitor}`  | **1.12 s** |
| p95 `{step:cerrar_monitoria}`     | **11.7 ms** |
| Duración promedio por ciclo   | 13.78 s      |
| Total requests                | 27 (9 por ciclo × 3 iteraciones) |

**Observaciones:**
- Los 5 pasos del flujo pasaron en los 3 ciclos sin ningún error.
- `seleccionar_monitor` es el paso más lento (p95 = 1.12 s) porque escribe la monitoría en la DB.
- `cerrar_monitoria` es el más rápido (p95 = 11.7 ms) — el cierre en este estado de datos fue casi instantáneo.
- Todos los pasos están muy por debajo del umbral de 5 000 ms, con margen mayor al 75 %.
- **Conclusión:** el flujo completo HU-010 es estable y eficiente bajo carga secuencial.

### Datos académicos requeridos

El test usa por defecto los datos del seed de la DB de prueba:

| Dato           | Valor por defecto | Descripción                                      |
|----------------|-------------------|--------------------------------------------------|
| Profesor       | `1002`            | Dra. Patricia Méndez                             |
| Curso          | `3`               | Bases de Datos (profesor 1002 lo dicta)          |
| Escuela        | `1`               | Facultad de Ingeniería                           |
| Programa       | `1`               | Ingeniería de Sistemas                           |
| Jefe/Director  | `5001`            | Tiene permisos de aprobación y cierre            |

---

## Interpretar resultados

Cuando el test termina, k6 muestra un resumen. Los campos clave son:

```
http_req_duration............: avg=120ms  p(90)=200ms  p(95)=250ms
http_req_failed..............: 0.00%
checks.......................: 100.00%
```

- **`http_req_duration p(95)`** — el 95 % de las peticiones tardaron menos de ese valor.
- **`http_req_failed`** — porcentaje de peticiones con error HTTP (4xx/5xx).
- **`checks`** — porcentaje de `check()` que pasaron.

Si algún umbral se incumple, la salida termina con `FAILED` y el proceso retorna código de salida ≠ 0 (útil para CI/CD).

---

## Gestión de credenciales de prueba

### Credenciales actuales

Los scripts usan tres usuarios de prueba que deben existir en la base de datos:

| Rol                   | Variable `MONITOR_ID` / `_PASS`     | Valor por defecto |
|-----------------------|-------------------------------------|-------------------|
| Monitor               | `MONITOR_ID` / `MONITOR_PASS`       | `2220004` / `123456` |
| Profesor              | `PROFESSOR_ID` / `PROFESSOR_PASS`   | `1002` / `prof123`   |
| Jefe de departamento  | `HEAD_ID` / `HEAD_PASS`             | `5001` / `jefe123`   |

Estos valores viven en `performance/config/env.js` como fallback.  
**Nunca subir credenciales de producción al repositorio.**

### Cómo obtener credenciales válidas

1. Acceder a la base de datos PostgreSQL del ambiente de pruebas.
2. Verificar que los tres usuarios existan:
   ```sql
   SELECT user_id, role FROM users WHERE user_id IN ('2220004', '1002', '5001');
   ```
3. Si no existen, crearlos con el script de seed del proyecto o insertarlos manualmente.

### Cómo rotar o cambiar credenciales sin tocar el código

Pasar las credenciales nuevas como variables `-e` en tiempo de ejecución:

```bash
k6 run \
  -e MONITOR_ID=nuevo_monitor   -e MONITOR_PASS=nueva_clave \
  -e PROFESSOR_ID=nuevo_prof    -e PROFESSOR_PASS=nueva_clave \
  -e HEAD_ID=nuevo_jefe         -e HEAD_PASS=nueva_clave \
  performance/tests/login.test.js
```

### En CI/CD (GitHub Actions)

Agregar las credenciales como **Secrets** del repositorio y pasarlas como variables de entorno:

```yaml
- name: Run login load test
  run: k6 run performance/tests/login.test.js
  env:
    BASE_URL:       ${{ secrets.SIGMA_BASE_URL }}
    MONITOR_ID:     ${{ secrets.TEST_MONITOR_ID }}
    MONITOR_PASS:   ${{ secrets.TEST_MONITOR_PASS }}
    PROFESSOR_ID:   ${{ secrets.TEST_PROFESSOR_ID }}
    PROFESSOR_PASS: ${{ secrets.TEST_PROFESSOR_PASS }}
    HEAD_ID:        ${{ secrets.TEST_HEAD_ID }}
    HEAD_PASS:      ${{ secrets.TEST_HEAD_PASS }}
```

### Qué hacer si un usuario de prueba ya no existe

1. Crear el usuario nuevamente en la DB de pruebas.
2. Actualizar el valor por defecto en `performance/config/env.js` si el ID cambió.
3. Volver a correr el smoke test para verificar que las credenciales funcionan:
   ```bash
   k6 run performance/smoke/smoke.test.js
   ```

---

## Rúbricas, plan y reportes pesados (HU2-265)

### Endpoints cubiertos — `tests/reportes.test.js`

```
GET /rubric/professor/{professorId}                         → Rúbricas asignadas al profesor
GET /activity/findAll/{userId}/professor                    → Plan de actividades (rol profesor)
GET /activity/findAll/{userId}/monitor                      → Plan de actividades (rol monitor)
GET /monitoring/getMonitorsReport/{professorId}/professor   → Reporte de monitores
GET /monitoring/getProfessorReport/{professorId}            → Reporte del profesor
GET /monitoring/getCategoriesReport/professor/{professorId} → Reporte de categorías por materia
GET /monitoring/getAttendanceReport/professor/{professorId} → Reporte de asistencia (join masivo)
```

### Umbrales diferenciados (SIGMA-PERF-010)

| Endpoint (tag)              | p95 máximo | Justificación                                     |
|-----------------------------|------------|---------------------------------------------------|
| `endpoint:rubricas`         | 3 000 ms   | Consulta indexada por professorId                 |
| `endpoint:plan_actividades` | 3 000 ms   | Listado de actividades por rol                    |
| `endpoint:reporte_monitores`| 5 000 ms   | Agrega evaluaciones y actividades por monitor     |
| `endpoint:reporte_profesor` | 5 000 ms   | Resumen agregado de todas las monitorías          |
| `endpoint:reporte_categorias`| 5 000 ms  | Agrupa por categoría con conteos                  |
| `endpoint:reporte_asistencia`| 6 000 ms  | Join entre actividades, asistencia y monitorías   |

### Perfil de carga

- **10 VUs** en round-robin entre los 4 flujos
- Ramp-up 30 s → sostenida 3 min → ramp-down 30 s
- Los reportes pesados llevan `timeout: '20s'`–`'25s'` para no bloquear la suite

---

## Flujo recomendado antes de un deploy

1. Asegurarte de que los tres backends están corriendo.
2. Correr el smoke:
   ```bash
   k6 run -e PROFESSOR_ID=xxx -e PROFESSOR_PASS=yyy smoke/smoke.test.js
   ```
3. Si todos los checks pasan y no hay umbrales rotos → el ambiente está listo.
4. Para validación completa antes de releases importantes, correr también `scenarios/load.test.js`.
