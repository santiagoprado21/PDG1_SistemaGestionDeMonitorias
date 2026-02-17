# HU-010: Modificación del Flujo de Aprobación

## 📋 Resumen de Cambios

Se modificó el flujo de aprobación de monitorías para que **el Jefe de Departamento apruebe ANTES** de que se abran las postulaciones, en lugar de aprobar DESPUÉS de que el profesor selecciona un monitor.

## 🔄 Comparación de Flujos

### ❌ Flujo ANTERIOR (Incorrecto)

```
1. PROFESOR crea convocatoria → CONVOCATORIA_ABIERTA
2. ESTUDIANTES se postulan
3. PROFESOR selecciona monitor → MONITOR_SELECCIONADO
4. SISTEMA crea Monitoring → PENDIENTE_APROBACION
5. JEFE aprueba/rechaza → APROBADA / RECHAZADA ❌ (AL FINAL)
```

### ✅ Flujo NUEVO (Correcto)

```
1. PROFESOR crea convocatoria → PENDIENTE_APROBACION_JEFE ⭐
2. JEFE revisa/modifica/aprueba → CONVOCATORIA_ABIERTA
3. ESTUDIANTES se postulan (solo si jefe aprobó)
4. PROFESOR selecciona monitor → MONITOR_SELECCIONADO
5. SISTEMA crea Monitoring → APROBADA (automático)
```

## 🆕 Nuevas Funcionalidades

### 1. El Jefe puede MODIFICAR la convocatoria

Antes de aprobar, el jefe puede modificar:
- ✏️ **Horas solicitadas** (ajustar presupuesto)
- ✏️ **Fechas** de inicio y fin
- ✏️ **Justificación** (mejorar o corregir)
- ✏️ **Requisitos de promedio** y nota del curso
- ✏️ **Tarifa por hora**

### 2. Nuevo Estado: PENDIENTE_APROBACION_JEFE

- Las convocatorias creadas por profesores inician en este estado
- No son visibles para estudiantes hasta que el jefe apruebe
- El jefe las ve en su panel de aprobación

## 🔧 Cambios Técnicos Implementados

### Backend (Java/Spring Boot)

#### 1. **Modelo** (`MonitoringRequest.java`)
```java
// Nuevos campos agregados:
private String approvedByHead;           // ID del jefe que aprobó
private String headComment;              // Comentario del jefe
private LocalDateTime headApprovalDate;  // Fecha de aprobación

// Estado inicial modificado:
private RequestStatus status = RequestStatus.PENDIENTE_APROBACION_JEFE;
```

#### 2. **Enum** (`RequestStatus.java`)
```java
PENDIENTE_APROBACION_JEFE,  // Nuevo estado al inicio
CONVOCATORIA_ABIERTA,       // Ahora significa "aprobada por jefe"
MONITOR_SELECCIONADO,
PENDIENTE_APROBACION,       // Deprecated (para compatibilidad)
APROBADA,
RECHAZADA,
CANCELADA
```

#### 3. **Service** (`MonitoringRequestService.java`)
Nuevos métodos:
- `findPendingHeadApproval(String departmentHeadId)`
- `approveByHead(Long requestId, String departmentHeadId, String comment)`
- `rejectByHead(Long requestId, String departmentHeadId, String comment)`
- `modifyAndApproveByHead(Long requestId, MonitoringRequestDTO modifications, String departmentHeadId, String comment)`

#### 4. **Controller** (`MonitoringRequestController.java`)
Nuevos endpoints:
- `GET /monitoring-request/pending-head-approval/{departmentHeadId}`
- `POST /monitoring-request/{id}/approve-by-head`
- `POST /monitoring-request/{id}/reject-by-head`
- `PUT /monitoring-request/{id}/modify-by-head`

#### 5. **DTO** (`MonitoringRequestDTO.java`)
```java
// Nuevos campos para el frontend:
private String approvedByHead;
private String headComment;
private LocalDateTime headApprovalDate;
```

### Frontend (React)

#### 1. **Componente** (`AprobarMonitoriasHU010.js`)

**Cambios principales:**

1. **Carga de datos** - Cambiado de monitorías a convocatorias:
```javascript
// ANTES:
fetch(`${BACKEND_URL}/monitoring/pending-approval`)

// AHORA:
fetch(`${BACKEND_URL}/monitoring-request/pending-head-approval/${departmentHeadId}`)
```

2. **Nuevo estado para modificación:**
```javascript
const [modifiedData, setModifiedData] = useState({
    requestedHours: '',
    justification: '',
    startDate: '',
    finishDate: '',
    requiredAverageGrade: '',
    requiredCourseGrade: '',
    hourlyRate: ''
});
```

3. **Tres acciones posibles:**
- `approve` - Aprobar sin cambios
- `modify` - Modificar y aprobar
- `reject` - Rechazar

4. **Modal con campos editables:**
- Cuando `modalAction === 'modify'` se muestran inputs editables
- Cuando `modalAction === 'approve'` o `'reject'` solo se muestra información

#### 2. **Estilos** (`AprobarMonitoriasHU010.css`)

Nuevos estilos agregados:
- `.btn-modify-hu010` - Botón azul para modificar
- `.input-modify-hu010` - Inputs editables en el modal
- `.textarea-modify-hu010` - Textarea editable para justificación
- `.info-box-hu010` - Box informativo azul (similar al warning)
- `.btn-confirm-modify-hu010` - Botón de confirmación para modificar

### Base de Datos

#### Script de Migración (`12_hu010_modificacion_flujo_aprobacion.sql`)

```sql
-- Nuevas columnas agregadas
ALTER TABLE sigma.monitoring_request 
ADD COLUMN IF NOT EXISTS approved_by_head VARCHAR(20),
ADD COLUMN IF NOT EXISTS head_comment TEXT,
ADD COLUMN IF NOT EXISTS head_approval_date TIMESTAMP;

-- Ampliar columna status para el nuevo estado más largo
ALTER TABLE sigma.monitoring_request 
ALTER COLUMN status TYPE VARCHAR(35);
```

## 📝 Cómo Usar el Nuevo Flujo

### Como PROFESOR:

1. Crear convocatoria con todos los detalles (igual que antes)
2. La convocatoria queda en estado `PENDIENTE_APROBACION_JEFE`
3. **Esperar a que el jefe apruebe** ⏳
4. Cuando el jefe apruebe, recién ahí los estudiantes podrán postularse
5. Revisar postulaciones y seleccionar monitor (igual que antes)

### Como JEFE DE DEPARTAMENTO:

1. Ver convocatorias pendientes en "Aprobar Convocatorias de Monitoría"
2. Revisar detalles de cada convocatoria:
   - Curso, profesor, programa
   - Horas solicitadas
   - Período (fechas)
   - Justificación del profesor

3. **Decidir una acción:**

   **Opción A: Aprobar sin cambios** ✅
   - Click en botón "✓ Aprobar"
   - Escribir comentario
   - Confirmar
   - → La convocatoria queda ABIERTA para postulaciones

   **Opción B: Modificar y aprobar** ✏️
   - Click en botón "✏️ Modificar"
   - Editar los campos que necesites (horas, fechas, justificación, etc.)
   - Escribir comentario explicando los cambios
   - Confirmar
   - → Los cambios se aplican Y la convocatoria queda ABIERTA

   **Opción C: Rechazar** ❌
   - Click en botón "✗ Rechazar"
   - Escribir motivo del rechazo
   - Confirmar
   - → La convocatoria queda RECHAZADA (el profesor debe crear otra)

### Como ESTUDIANTE:

1. Ver convocatorias disponibles (igual que antes)
2. **IMPORTANTE:** Solo verás convocatorias que ya fueron aprobadas por el jefe
3. Postularse a las que te interesen (igual que antes)

## 🧪 Testing

### Pruebas Manuales Recomendadas

#### Test 1: Flujo Completo Exitoso
1. Login como Profesor (ej: `1001 / prof123`)
2. Crear una nueva convocatoria
3. Verificar que queda en estado `PENDIENTE_APROBACION_JEFE`
4. Login como Estudiante (ej: `2220001 / 123456`)
5. Verificar que NO aparece en convocatorias abiertas
6. Login como Jefe (ej: `5001 / jefe123`)
7. Aprobar la convocatoria
8. Login como Estudiante nuevamente
9. Verificar que AHORA SÍ aparece en convocatorias abiertas
10. Postularse exitosamente

#### Test 2: Modificación por Jefe
1. Profesor crea convocatoria con 40 horas
2. Jefe revisa y decide que son muchas horas
3. Jefe click en "Modificar"
4. Cambia horas a 30
5. Agrega comentario: "Reducidas horas por presupuesto"
6. Aprueba
7. Verificar que la convocatoria tiene 30 horas (no 40)

#### Test 3: Rechazo
1. Profesor crea convocatoria mal justificada
2. Jefe la rechaza con comentario
3. Verificar que NO aparece para estudiantes
4. Profesor debe crear nueva convocatoria mejorada

### Endpoints para Testing con Postman/Curl

#### Obtener convocatorias pendientes
```bash
GET http://localhost:8080/monitoring-request/pending-head-approval/5001
Authorization: Bearer {token}
```

#### Aprobar convocatoria
```bash
POST http://localhost:8080/monitoring-request/{id}/approve-by-head
Content-Type: application/json
Authorization: Bearer {token}

{
  "departmentHeadId": "5001",
  "comment": "Aprobada. Todo en orden."
}
```

#### Modificar y aprobar
```bash
PUT http://localhost:8080/monitoring-request/{id}/modify-by-head
Content-Type: application/json
Authorization: Bearer {token}

{
  "departmentHeadId": "5001",
  "comment": "Reducidas horas por presupuesto",
  "requestedHours": 30,
  "justification": "Justificación modificada...",
  "startDate": "2025-02-01",
  "finishDate": "2025-06-30"
}
```

#### Rechazar convocatoria
```bash
POST http://localhost:8080/monitoring-request/{id}/reject-by-head
Content-Type: application/json
Authorization: Bearer {token}

{
  "departmentHeadId": "5001",
  "comment": "Rechazada. Justificación insuficiente."
}
```

## 📊 Migración de Datos

### Para Base de Datos Existente

Ejecutar el script de migración:

```sql
-- En Neon SQL Editor o psql
\i neon_scripts/12_hu010_modificacion_flujo_aprobacion.sql
```

O copiar y pegar el contenido del archivo en el SQL Editor de Neon.

### Convocatorias Existentes

Las convocatorias que ya existen en la base de datos:
- ✅ Siguen funcionando normalmente
- ✅ No necesitan migración de datos
- ✅ El backend detecta automáticamente si usan flujo antiguo o nuevo
- ℹ️ Solo las NUEVAS convocatorias usan el nuevo flujo

## 🐛 Troubleshooting

### Problema: "No aparecen convocatorias para el jefe"

**Solución:**
1. Verificar que el jefe tenga programas asignados en `head_program`
2. Verificar que haya convocatorias en estado `PENDIENTE_APROBACION_JEFE`
3. Revisar console del navegador para errores

### Problema: "Error al modificar: presupuesto excedido"

**Solución:**
- Las horas modificadas no pueden exceder el presupuesto disponible
- Reducir las horas solicitadas
- O el administrador debe aumentar el presupuesto del programa

### Problema: "Estudiantes siguen viendo convocatorias no aprobadas"

**Solución:**
- Revisar query en frontend: debe filtrar por `status === 'CONVOCATORIA_ABIERTA'`
- Verificar que el backend endpoint `/monitoring-request/open` filtre correctamente

## 📚 Archivos Modificados

### Backend
- `PDG-SIGMA-BACKEND-main/src/main/java/com/pdg/sigma/domain/RequestStatus.java`
- `PDG-SIGMA-BACKEND-main/src/main/java/com/pdg/sigma/domain/MonitoringRequest.java`
- `PDG-SIGMA-BACKEND-main/src/main/java/com/pdg/sigma/service/MonitoringRequestService.java`
- `PDG-SIGMA-BACKEND-main/src/main/java/com/pdg/sigma/service/MonitoringRequestServiceImpl.java`
- `PDG-SIGMA-BACKEND-main/src/main/java/com/pdg/sigma/controller/MonitoringRequestController.java`
- `PDG-SIGMA-BACKEND-main/src/main/java/com/pdg/sigma/dto/MonitoringRequestDTO.java`

### Frontend
- `PDG-SIGMA-Front/src/AprobarMonitoriasHU010.js`
- `PDG-SIGMA-Front/src/AprobarMonitoriasHU010.css`

### Base de Datos
- `neon_scripts/12_hu010_modificacion_flujo_aprobacion.sql`

### Documentación
- `neon_scripts/README_HU010_NUEVO_FLUJO.md` (este archivo)

## 📅 Changelog

**Versión 2.0 - Modificación de Flujo de Aprobación**
- Agregado estado `PENDIENTE_APROBACION_JEFE`
- Jefe aprueba ANTES de postulaciones (no después)
- Jefe puede modificar convocatorias antes de aprobar
- Nuevos endpoints para aprobación inicial
- UI actualizada con botón "Modificar" y campos editables
- Script de migración SQL incluido

## ✅ Checklist de Implementación

- [x] Agregar nuevo estado al enum `RequestStatus`
- [x] Modificar modelo `MonitoringRequest` con nuevos campos
- [x] Implementar métodos en `MonitoringRequestService`
- [x] Crear endpoints en `MonitoringRequestController`
- [x] Actualizar DTO con nuevos campos
- [x] Modificar frontend `AprobarMonitoriasHU010.js`
- [x] Agregar estilos CSS para modificación
- [x] Crear script de migración SQL
- [x] Documentar cambios
- [ ] Ejecutar migración en base de datos de desarrollo
- [ ] Probar flujo completo end-to-end
- [ ] Ejecutar migración en base de datos de producción
- [ ] Capacitar usuarios (profesores y jefes)

## 🎯 Próximos Pasos

1. **Ejecutar la migración SQL** en la base de datos
2. **Reiniciar el backend** para cargar los cambios
3. **Probar el nuevo flujo** con usuarios de prueba
4. **Documentar en manual de usuario** las nuevas funcionalidades
5. **Capacitar al equipo** sobre el nuevo proceso

---

**Fecha de implementación:** 21 de enero de 2026  
**Desarrollador:** Sistema de Monitorías - HU-010  
**Estado:** ✅ Completado - Listo para testing
