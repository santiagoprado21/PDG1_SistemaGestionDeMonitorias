# Tests de rendimiento — SIGMA+ con k6

Proyecto de pruebas de rendimiento para la API de SIGMA+.  
Basado en [k6](https://k6.io/) — herramienta open-source de carga y rendimiento.

---

## Estructura

```
performance/
├── config/
│   └── env.js                  # Variables de entorno (BASE_URL, credenciales)
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

| Variable        | Por defecto              | Descripción                            |
|-----------------|--------------------------|----------------------------------------|
| `BASE_URL`      | `http://localhost:5433`  | URL base del backend SIGMA             |
| `MONITOR_ID`    | `A00000000`              | ID del monitor de prueba               |
| `MONITOR_PASS`  | `password123`            | Contraseña del monitor de prueba       |
| `PROFESSOR_ID`  | `P0000000`               | ID del profesor de prueba              |
| `PROFESSOR_PASS`| `password123`            | Contraseña del profesor de prueba      |
| `HEAD_ID`       | `J0000000`               | ID del jefe de departamento de prueba  |
| `HEAD_PASS`     | `password123`            | Contraseña del jefe de prueba          |

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
# Login (SIGMA-PERF-003)
k6 run tests/login.test.js

# Convocatorias (SIGMA-PERF-004)
k6 run tests/convocatorias.test.js

# Plan de actividades y reportes (SIGMA-PERF-005)
k6 run tests/actividades.test.js

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

| Flujo / Endpoint            | Script                     | p95 máximo  | Error rate | Checks   |
|-----------------------------|----------------------------|-------------|------------|----------|
| Login (`/auth/login`)       | `tests/login.test.js`      | 2 500 ms    | 0 %        | 100 %    |
| Lecturas simples            | `smoke/smoke.test.js`      | 3 000 ms    | 0 %        | 100 %    |
| Convocatorias               | `tests/convocatorias.test.js` | 3 000 ms | 0 %        | 100 %    |
| Plan de actividades         | `tests/actividades.test.js`| 2 500 ms    | 0 %        | 100 %    |
| Reporte de monitores        | `tests/actividades.test.js`| 5 000 ms    | 0 %        | 100 %    |
| Cierre de monitorías        | `tests/cierre.test.js`     | 10 000 ms   | 0 %        | 100 %    |
| Carga sostenida (15 VUs)    | `scenarios/load.test.js`   | 4 500 ms    | < 1 %      | ≥ 99 %   |

### Thresholds por rol — Login (HU2-257)

El script `login.test.js` también mide latencia desglosada por rol, útil para detectar
si un tipo de usuario específico experimenta degradación:

| Rol                  | p95 máximo |
|----------------------|------------|
| `{role:monitor}`     | 2 500 ms   |
| `{role:profesor}`    | 2 500 ms   |
| `{role:jefe}`        | 2 500 ms   |

### Justificación de valores

- **Login 2 500 ms**: el endpoint hace una consulta a PostgreSQL en Neon (cloud). En pruebas
  reales con 10 VUs se midió p95 ≈ 2.19 s. Se estableció 2 500 ms como límite con margen
  de 15 % sobre el máximo medido.
- **Cierre 10 000 ms**: el endpoint `/monitoring/getAll` recupera todas las monitorías del
  departamento. Con 8 VUs se midió p95 ≈ 9.42 s. Valor de corte: 10 000 ms.
- **Carga mixta 4 500 ms**: bajo 15 VUs simultáneos (todos los flujos en paralelo), el
  p95 global medido fue 2.36 s. El límite de 4 500 ms da margen para variaciones de red.

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

## Flujo recomendado antes de un deploy

1. Asegurarte de que los tres backends están corriendo.
2. Correr el smoke:
   ```bash
   k6 run -e PROFESSOR_ID=xxx -e PROFESSOR_PASS=yyy smoke/smoke.test.js
   ```
3. Si todos los checks pasan y no hay umbrales rotos → el ambiente está listo.
4. Para validación completa antes de releases importantes, correr también `scenarios/load.test.js`.
