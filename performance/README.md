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

Los tests fallan automáticamente si no se cumplen estos criterios:

| Tipo de endpoint       | p95 máximo | Error rate |
|------------------------|------------|------------|
| Login                  | 2 000 ms   | 0 %        |
| Lecturas simples       | 2 000 ms   | 0 %        |
| Convocatorias          | 3 000 ms   | 0 %        |
| Plan de actividades    | 2 500 ms   | 0 %        |
| Reportes               | 5 000 ms   | 0 %        |
| Carga sostenida (mix)  | 3 000 ms   | < 1 %      |

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

## Flujo recomendado antes de un deploy

1. Asegurarte de que los tres backends están corriendo.
2. Correr el smoke:
   ```bash
   k6 run -e PROFESSOR_ID=xxx -e PROFESSOR_PASS=yyy smoke/smoke.test.js
   ```
3. Si todos los checks pasan y no hay umbrales rotos → el ambiente está listo.
4. Para validación completa antes de releases importantes, correr también `scenarios/load.test.js`.
