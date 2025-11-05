# HU-010: Guía Completa de Pruebas 🧪

## Índice
1. [Preparación del Entorno](#1-preparación-del-entorno)
2. [Migración de Base de Datos](#2-migración-de-base-de-datos)
3. [Iniciar el Backend](#3-iniciar-el-backend)
4. [Pruebas del Flujo Completo](#4-pruebas-del-flujo-completo)
5. [Verificación en Base de Datos](#5-verificación-en-base-de-datos)
6. [Casos de Prueba Adicionales](#6-casos-de-prueba-adicionales)
7. [Troubleshooting](#7-troubleshooting)

---

## 1. Preparación del Entorno

### 1.1. Verificar rama actual
```bash
cd C:\dev\SwMonitorias
git branch --show-current
```
**Debe mostrar:** `feature/HU-010-postulacion-monitorias`

### 1.2. Asegurar últimos cambios
```bash
git pull origin feature/HU-010-postulacion-monitorias
```

### 1.3. Herramientas necesarias
- ✅ Postman o Thunder Client (VS Code)
- ✅ Cliente SQL (DBeaver, pgAdmin, o psql)
- ✅ Backend corriendo en puerto 8080
- ✅ Acceso a base de datos Neon

---

## 2. Migración de Base de Datos

### 2.1. Conectar a Neon PostgreSQL

**Opción A: Neon SQL Editor (MÁS FÁCIL) ⭐ RECOMENDADO**
1. Ir a: https://console.neon.tech
2. Login con tu cuenta
3. Seleccionar tu proyecto
4. Click en **"SQL Editor"** en el menú izquierdo
5. ¡Listo! Ya puedes copiar y pegar el script SQL


### 2.2. Ejecutar script de migración

**Desde Neon SQL Editor (MÁS FÁCIL):** ⭐
1. Abrir el archivo `C:\dev\SwMonitorias\neon_scripts\6_hu010_create_new_tables.sql` en tu editor
2. Copiar **todo** el contenido del archivo (Ctrl+A, Ctrl+C)
3. Pegar en el SQL Editor de Neon
4. Click en **"Run"** o presionar **Ctrl+Enter**
5. Esperar a que termine (verás mensajes de "NOTICE")

### 2.3. Verificar tablas creadas

**En Neon SQL Editor, ejecutar:**

```sql
SET search_path TO sigma;

-- Verificar tablas
SELECT table_name 
FROM information_schema.tables 
WHERE table_schema = 'sigma' 
  AND table_name IN ('monitoring_request', 'monitor_application')
ORDER BY table_name;
```

**Resultado esperado:**
```
table_name
------------------
monitor_application
monitoring_request
```

**Luego verificar columnas:**
```sql
SELECT column_name, data_type 
FROM information_schema.columns 
WHERE table_schema = 'sigma' 
  AND table_name = 'monitoring'
  AND column_name IN ('monitoring_request_id', 'assigned_monitor_id', 'approval_status')
ORDER BY column_name;
```

**Resultado esperado:**
```
column_name              | data_type
-------------------------|------------------
approval_status          | character varying
assigned_monitor_id      | character varying
monitoring_request_id    | bigint
```

✅ Si ves estos resultados, la migración fue exitosa.

💡 **Tip:** Neon SQL Editor muestra los resultados en una tabla bonita debajo del editor.

---

## 3. Iniciar el Backend

### 3.1. Compilar proyecto
```bash
cd C:\dev\SwMonitorias\PDG-SIGMA-BACKEND-main
mvnw clean install -DskipTests
```

### 3.2. Iniciar con perfil cloud
```bash
mvnw spring-boot:run -Dspring-boot.run.profiles=cloud
```

**O desde tu IDE:** Ejecutar `SigmaApplication.java` con perfil `cloud`

### 3.3. Verificar que inició correctamente

**Buscar en logs:**
```
Started SigmaApplication in X.XXX seconds
Tomcat started on port(s): 8080
```

### 3.4. Probar endpoint de salud
```bash
curl http://localhost:8080/actuator/health
```

---

## 4. Pruebas del Flujo Completo

### 📋 Datos de prueba necesarios

Antes de empezar, necesitas IDs reales de tu base de datos. Ejecuta estas consultas:

```sql
SET search_path TO sigma;

-- Obtener un profesor
SELECT id, name FROM professor LIMIT 1;
-- Anotar: PROF_ID

-- Obtener un curso
SELECT id, name FROM course LIMIT 1;
-- Anotar: COURSE_ID

-- Obtener una facultad
SELECT id, name FROM school LIMIT 1;
-- Anotar: SCHOOL_ID

-- Obtener un programa
SELECT id, name FROM program LIMIT 1;
-- Anotar: PROGRAM_ID

-- Obtener 3 estudiantes (monitors o prospects)
SELECT id_monitor, name, last_name FROM monitor LIMIT 3;
-- Si no hay, usar prospects:
SELECT id, name, last_name FROM prospect LIMIT 3;
-- Anotar: STUDENT_1_ID, STUDENT_2_ID, STUDENT_3_ID

-- Obtener un jefe de departamento
SELECT id, name FROM department_head LIMIT 1;
-- Anotar: DEPT_HEAD_ID
```

---

## PASO 1️⃣: Profesor Crea Convocatoria

### Request
```http
POST http://localhost:8080/monitoring-request/create
Content-Type: application/json

{
  "professorId": "PROF_ID",
  "courseId": COURSE_ID,
  "schoolId": SCHOOL_ID,
  "programId": PROGRAM_ID,
  "requestedHours": 40,
  "justification": "Se requiere monitor para Cálculo I debido al alto número de estudiantes inscritos. El monitor apoyará con tutorías individuales y resolución de ejercicios prácticos.",
  "semester": "2025-1",
  "startDate": "2025-02-01",
  "finishDate": "2025-06-30",
  "requiredAverageGrade": 4.0,
  "requiredCourseGrade": 4.5,
  "hourlyRate": 20000.0
}
```

### Response esperada (200)
```json
{
  "id": 1,
  "professorId": "PROF_ID",
  "professorName": "Nombre Profesor",
  "courseId": COURSE_ID,
  "courseName": "Cálculo I",
  "status": "CONVOCATORIA_ABIERTA",
  "requestedHours": 40,
  "justification": "Se requiere monitor...",
  "semester": "2025-1",
  "createdAt": "2025-11-04T..."
}
```

**Anotar:** `REQUEST_ID` = el `id` de la respuesta

### Verificar en BD
```sql
SELECT id, course_id, status, requested_hours, justification 
FROM monitoring_request 
WHERE id = 1;
```

✅ **Estado debe ser:** `CONVOCATORIA_ABIERTA`

---

## PASO 2️⃣: Listar Convocatorias Abiertas

### Request
```http
GET http://localhost:8080/monitoring-request/open
```

### Response esperada (200)
```json
[
  {
    "id": 1,
    "courseName": "Cálculo I",
    "professorName": "Nombre Profesor",
    "status": "CONVOCATORIA_ABIERTA",
    "requestedHours": 40,
    "semester": "2025-1",
    "applicationCount": 0
  }
]
```

✅ La convocatoria aparece en la lista

---

## PASO 3️⃣: Estudiante 1 se Postula

### Request
```http
POST http://localhost:8080/monitor-application/apply
Content-Type: application/json

{
  "monitoringRequestId": REQUEST_ID,
  "monitorId": "STUDENT_1_ID",
  "motivationLetter": "Me gustaría ser monitor de Cálculo I porque obtuve excelentes calificaciones en el curso y me apasiona ayudar a otros estudiantes a comprender mejor las matemáticas."
}
```

### Response esperada (201)
```json
{
  "id": 1,
  "monitoringRequestId": REQUEST_ID,
  "monitorId": "STUDENT_1_ID",
  "monitorName": "Estudiante Uno",
  "status": "POSTULADO",
  "applicationDate": "2025-11-04T...",
  "motivationLetter": "Me gustaría ser monitor..."
}
```

**Anotar:** `APPLICATION_1_ID` = el `id` de la respuesta

---

## PASO 4️⃣: Estudiante 2 se Postula

### Request
```http
POST http://localhost:8080/monitor-application/apply
Content-Type: application/json

{
  "monitoringRequestId": REQUEST_ID,
  "monitorId": "STUDENT_2_ID",
  "motivationLetter": "Tengo experiencia dando tutorías a compañeros y un promedio excelente. Me encantaría contribuir al aprendizaje de otros estudiantes."
}
```

**Anotar:** `APPLICATION_2_ID` = el `id` de la respuesta

---

## PASO 5️⃣: Estudiante 3 se Postula

### Request
```http
POST http://localhost:8080/monitor-application/apply
Content-Type: application/json

{
  "monitoringRequestId": REQUEST_ID,
  "monitorId": "STUDENT_3_ID",
  "motivationLetter": "Estoy muy interesado en esta monitoría. Tengo habilidades de comunicación y paciencia para explicar conceptos complejos."
}
```

**Anotar:** `APPLICATION_3_ID` = el `id` de la respuesta

---

## PASO 6️⃣: Profesor Ve Postulantes

### Request
```http
GET http://localhost:8080/monitor-application/request/REQUEST_ID
```

### Response esperada (200)
```json
[
  {
    "id": 1,
    "monitorId": "STUDENT_1_ID",
    "monitorName": "Estudiante Uno",
    "status": "POSTULADO",
    "motivationLetter": "Me gustaría ser monitor..."
  },
  {
    "id": 2,
    "monitorId": "STUDENT_2_ID",
    "monitorName": "Estudiante Dos",
    "status": "POSTULADO",
    "motivationLetter": "Tengo experiencia..."
  },
  {
    "id": 3,
    "monitorId": "STUDENT_3_ID",
    "monitorName": "Estudiante Tres",
    "status": "POSTULADO",
    "motivationLetter": "Estoy muy interesado..."
  }
]
```

✅ **Verificar:** 3 postulantes, todos con estado `POSTULADO`

---

## PASO 7️⃣: Profesor Selecciona Monitor

### Request
```http
POST http://localhost:8080/monitor-application/select
Content-Type: application/json

{
  "applicationId": APPLICATION_1_ID,
  "professorId": "PROF_ID",
  "notes": "Seleccionado por su excelente promedio y experiencia en tutorías"
}
```

### Response esperada (200)
```json
{
  "message": "Monitor seleccionado exitosamente. La monitoría ha sido enviada para aprobación del jefe de departamento."
}
```

### Verificar en BD
```sql
-- Ver estado de postulaciones
SELECT id, monitor_id, status 
FROM monitor_application 
WHERE monitoring_request_id = REQUEST_ID;

-- Ver estado de convocatoria
SELECT id, status 
FROM monitoring_request 
WHERE id = REQUEST_ID;

-- Ver monitoría creada
SELECT id, assigned_monitor_id, approval_status, justification
FROM monitoring 
WHERE monitoring_request_id = REQUEST_ID;
```

✅ **Verificar:**
- Postulación 1: `SELECCIONADO`
- Postulaciones 2 y 3: `NO_SELECCIONADO`
- Convocatoria: `PENDIENTE_APROBACION`
- Monitoría creada con: `approval_status = PENDIENTE_APROBACION`

**Anotar:** `MONITORING_ID` = el `id` de la monitoría creada

---

## PASO 8️⃣: Jefe de Departamento Ve Pendientes

### Request
```http
GET http://localhost:8080/monitoring/pending-approval
```

### Response esperada (200)
```json
[
  {
    "id": MONITORING_ID,
    "courseName": "Cálculo I",
    "professorName": "Nombre Profesor",
    "assignedMonitorName": "Estudiante Uno",
    "approvalStatus": "PENDIENTE_APROBACION",
    "justification": "Se requiere monitor...",
    "estimatedHours": 40
  }
]
```

✅ La monitoría aparece en pendientes

---

## PASO 9️⃣: Jefe de Departamento Aprueba

### Request
```http
POST http://localhost:8080/monitoring/approve/MONITORING_ID
Content-Type: application/json

{
  "departmentHeadId": "DEPT_HEAD_ID",
  "comment": "Aprobado. La justificación es válida y el monitor seleccionado cumple con los requisitos."
}
```

### Response esperada (200)
```json
{
  "message": "Monitoría aprobada exitosamente",
  "monitoringId": MONITORING_ID
}
```

### Verificar en BD
```sql
-- Ver estado final de monitoría
SELECT id, approval_status, approved_by, approval_comment, approval_date
FROM monitoring 
WHERE id = MONITORING_ID;

-- Ver estado final de convocatoria
SELECT id, status, updated_at
FROM monitoring_request 
WHERE id = REQUEST_ID;
```

✅ **Verificar:**
- Monitoría: `approval_status = APROBADA`
- Convocatoria: `status = APROBADA`
- Campos `approved_by`, `approval_comment`, `approval_date` poblados

---

## 🎉 FLUJO COMPLETO EXITOSO

Si llegaste aquí y todos los estados coinciden, ¡el flujo completo funciona correctamente!

---

## 5. Verificación en Base de Datos

### 5.1. Estado final de todas las entidades

```sql
SET search_path TO sigma;

-- 1. Convocatoria
SELECT 
    id,
    status,
    requested_hours,
    LEFT(justification, 50) as justification_preview
FROM monitoring_request
WHERE id = REQUEST_ID;

-- 2. Postulaciones
SELECT 
    id,
    monitor_id,
    status,
    LEFT(motivation_letter, 50) as motivation_preview
FROM monitor_application
WHERE monitoring_request_id = REQUEST_ID
ORDER BY id;

-- 3. Monitoría
SELECT 
    id,
    assigned_monitor_id,
    approval_status,
    approved_by,
    approval_date,
    LEFT(justification, 50) as justification_preview
FROM monitoring
WHERE id = MONITORING_ID;
```

### 5.2. Vista completa del flujo

```sql
SELECT 
    mr.id as convocatoria_id,
    c.name as curso,
    p.name as profesor,
    mr.status as estado_convocatoria,
    COUNT(ma.id) as num_postulantes,
    m.id as monitoria_id,
    mon.name || ' ' || mon.last_name as monitor_seleccionado,
    m.approval_status as estado_aprobacion
FROM monitoring_request mr
LEFT JOIN course c ON c.id = mr.course_id
LEFT JOIN professor p ON p.id = mr.professor_id
LEFT JOIN monitor_application ma ON ma.monitoring_request_id = mr.id
LEFT JOIN monitoring m ON m.monitoring_request_id = mr.id
LEFT JOIN monitor mon ON mon.code = m.assigned_monitor_id
WHERE mr.id = REQUEST_ID
GROUP BY mr.id, c.name, p.name, mr.status, m.id, mon.name, mon.last_name, m.approval_status;
```

---

## 6. Casos de Prueba Adicionales

### 6.1. Probar Rechazo en lugar de Aprobación

**Crear otra convocatoria y repetir pasos 1-7, luego:**

```http
POST http://localhost:8080/monitoring/reject/MONITORING_ID_2
Content-Type: application/json

{
  "departmentHeadId": "DEPT_HEAD_ID",
  "comment": "Rechazado. El presupuesto del programa ya está comprometido para este semestre."
}
```

✅ Verificar: `approval_status = RECHAZADA` y `status = RECHAZADA`

---

### 6.2. Probar Cancelación de Convocatoria

**Crear convocatoria sin postulantes:**

```http
POST http://localhost:8080/monitoring-request/REQUEST_ID_3/cancel
Content-Type: application/json

{
  "professorId": "PROF_ID"
}
```

✅ Verificar: `status = CANCELADA`

---

### 6.3. Probar Validaciones

#### No duplicar postulación
```http
POST http://localhost:8080/monitor-application/apply
Content-Type: application/json

{
  "monitoringRequestId": REQUEST_ID,
  "monitorId": "STUDENT_1_ID",
  "motivationLetter": "Intento de duplicado"
}
```
❌ Esperado: Error 400 "Ya te has postulado a esta convocatoria"

---

#### Postularse a convocatoria cerrada
```http
POST http://localhost:8080/monitor-application/apply
Content-Type: application/json

{
  "monitoringRequestId": REQUEST_ID,
  "monitorId": "STUDENT_4_ID",
  "motivationLetter": "Intentando postular a convocatoria ya cerrada"
}
```
❌ Esperado: Error 400 "La convocatoria no está abierta para postulaciones"

---

#### Profesor sin permiso
```http
POST http://localhost:8080/monitoring-request/create
Content-Type: application/json

{
  "professorId": "OTRO_PROF_ID",
  "courseId": CURSO_NO_ASIGNADO,
  ...
}
```
❌ Esperado: Error 400 "El profesor no tiene permiso para crear convocatoria en este curso"

---

### 6.4. Probar Endpoints de Consulta

```http
# Ver convocatorias de un profesor
GET http://localhost:8080/monitoring-request/professor/PROF_ID

# Ver postulaciones de un estudiante
GET http://localhost:8080/monitor-application/monitor/STUDENT_1_ID

# Verificar si ya se postuló
GET http://localhost:8080/monitor-application/check-applied/REQUEST_ID/STUDENT_1_ID

# Ver convocatorias disponibles para un estudiante
GET http://localhost:8080/monitor-application/available/STUDENT_1_ID/PROGRAM_ID
```

---

## 7. Troubleshooting

### ❌ Error: "Tabla no existe"
**Solución:** Ejecutar script de migración (Paso 2.2)

### ❌ Error: "Profesor no encontrado"
**Solución:** Verificar que el `professorId` existe en la tabla `professor`

### ❌ Error: "No cumples con los requisitos"
**Solución:** Verificar que el estudiante existe en `prospect` o `monitor` con notas adecuadas

### ❌ Error: "No hay presupuesto disponible"
**Solución:** Verificar tabla `department_budget` o reducir `requestedHours`

### ❌ Error de conexión al backend
**Solución:** 
1. Verificar que el backend esté corriendo: `curl http://localhost:8080/actuator/health`
2. Ver logs del backend
3. Verificar que el perfil `cloud` esté activo

### ❌ Error de conexión a BD
**Solución:**
1. Verificar credenciales en `application-cloud.properties`
2. Verificar conectividad: `ping ep-wispy-firefly-aeueocfk-pooler.c-2.us-east-2.aws.neon.tech`
3. Verificar que SSL está habilitado

---

## 📝 Checklist Final

- [ ] Migración SQL ejecutada
- [ ] Tablas verificadas
- [ ] Backend corriendo
- [ ] Profesor crea convocatoria ✅
- [ ] Estudiantes se postulan (x3) ✅
- [ ] Profesor ve postulantes ✅
- [ ] Profesor selecciona monitor ✅
- [ ] Sistema crea monitoría automáticamente ✅
- [ ] Jefe ve pendientes ✅
- [ ] Jefe aprueba monitoría ✅
- [ ] Estados finales correctos ✅
- [ ] Validaciones funcionan ✅

---

## 🎯 Siguiente Paso: Integrar con Frontend

Una vez que todas las pruebas backend funcionen, el siguiente paso es integrar con el frontend para crear las interfaces de usuario.

**Pantallas necesarias:**
1. Crear convocatoria (Profesor)
2. Ver y postularse (Estudiante)
3. Seleccionar monitor (Profesor)
4. Aprobar/Rechazar (Jefe Departamento)

---

¡Éxito con las pruebas! 🚀

