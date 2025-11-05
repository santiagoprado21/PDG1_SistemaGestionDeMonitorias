# 🎯 Guía de Pruebas Frontend HU-010

## ✅ Componentes Implementados

### 1. **SeleccionarMonitor.js** (Profesor)
**Ruta:** `/seleccionar-monitor/:requestId`

**Funcionalidad:**
- El profesor ve todos los postulantes de una convocatoria específica
- Puede ver la carta de motivación de cada postulante
- Selecciona UN monitor
- Al seleccionar, automáticamente:
  - Los demás postulantes son marcados como "NO_SELECCIONADO"
  - Se crea una monitoría con el monitor asignado
  - La monitoría pasa a estado "PENDIENTE_APROBACION"
  - Se envía al jefe de departamento para su aprobación

**Características:**
- ✅ Vista de tabla con postulantes
- ✅ Modal de confirmación con todos los detalles
- ✅ Notas adicionales opcionales
- ✅ Advertencia sobre la acción irreversible
- ✅ Redirección automática después de seleccionar

---

### 2. **AprobarMonitoriasHU010.js** (Jefe de Departamento)
**Ruta:** `/aprobar-monitorias-hu010`

**Funcionalidad:**
- El jefe de departamento ve todas las monitorías con estado "PENDIENTE_APROBACION"
- Puede aprobar o rechazar cada monitoría
- Debe ingresar un comentario obligatorio
- Al aprobar/rechazar:
  - La monitoría cambia de estado a "APROBADA" o "RECHAZADA"
  - La convocatoria también cambia de estado
  - Se registra quién aprobó/rechazó y cuándo

**Características:**
- ✅ Tabla con todas las monitorías pendientes
- ✅ Filtro por curso
- ✅ Modal de confirmación con detalles completos
- ✅ Justificación del profesor visible
- ✅ Comentario obligatorio
- ✅ Advertencias claras sobre las consecuencias

---

## 🔄 Flujo Completo HU-010

```
┌─────────────────────────────────────────────────────────┐
│                   FLUJO COMPLETO HU-010                 │
└─────────────────────────────────────────────────────────┘

1️⃣ PROFESOR
   ↓ /crear-convocatoria
   ↓ Crea convocatoria con justificación
   ↓ Estado: CONVOCATORIA_ABIERTA
   │
   │
2️⃣ ESTUDIANTES
   ↓ /ver-convocatorias
   ↓ Ven convocatorias abiertas
   ↓ Se postulan con carta de motivación
   ↓ Estado: POSTULADO
   │
   │
3️⃣ PROFESOR
   ↓ /crear-convocatoria (ve sus convocatorias)
   ↓ Click en "Ver Postulantes"
   ↓ /seleccionar-monitor/:requestId
   ↓ Ve lista de postulantes
   ↓ Selecciona 1 monitor
   ↓ Estado Convocatoria: PENDIENTE_APROBACION
   ↓ Estado Postulación: SELECCIONADO / NO_SELECCIONADO
   ↓ Se crea Monitoring automáticamente
   │
   │
4️⃣ JEFE DE DEPARTAMENTO
   ↓ /aprobar-monitorias-hu010
   ↓ Ve monitorías pendientes
   ↓ Aprueba/Rechaza con comentario
   ↓ Estado Monitoring: APROBADA/RECHAZADA
   ↓ Estado Convocatoria: APROBADA/RECHAZADA
   │
   │
5️⃣ MONITORÍA ACTIVA ✅
```

---

## 🧪 Instrucciones de Prueba

### Paso 1: Iniciar el Backend
```bash
cd C:\dev\SwMonitorias\PDG-SIGMA-BACKEND-main
mvnw spring-boot:run -Dspring-boot.run.profiles=cloud
```

**Verificar:** Backend corriendo en `http://localhost:8080`

---

### Paso 2: Iniciar el Frontend
```bash
cd C:\dev\SwMonitorias\PDG-SIGMA-Front
npm start
```

**Verificar:** Frontend corriendo en `http://localhost:3000`

---

### Paso 3: Probar como PROFESOR

1. **Login como profesor**
   - Usuario: [tu usuario profesor]
   - Password: [tu password]

2. **Crear una convocatoria**
   - Ir a: **📢 Crear Convocatoria**
   - Llenar el formulario:
     - Facultad, Programa, Curso
     - Semestre (ej: "2025-1")
     - Horas solicitadas (ej: 40)
     - Fechas de inicio y fin
     - **Justificación** (mínimo 50 caracteres)
   - Click en "Crear Convocatoria"
   - ✅ Verificar: Mensaje de éxito
   - ✅ Verificar: La convocatoria aparece en la tabla inferior con estado "Abierta"

3. **Esperar postulantes**
   - La convocatoria ahora está visible para los estudiantes
   - Espera a que los estudiantes se postulen (Paso 4)

4. **Seleccionar monitor** (después de que haya postulantes)
   - En la tabla de "Mis Convocatorias"
   - Click en **"Ver Postulantes"** en la convocatoria abierta
   - Te redirige a: `/seleccionar-monitor/:requestId`
   - ✅ Verificar: Ves la lista de postulantes
   - ✅ Verificar: Puedes ver sus cartas de motivación
   - Click en **"Seleccionar"** en el postulante que elijas
   - Se abre un modal con:
     - Datos del postulante
     - Carta de motivación completa
     - Campo para notas adicionales (opcional)
     - Advertencia sobre la acción
   - Click en **"Confirmar Selección"**
   - ✅ Verificar: Mensaje de éxito indicando que se envió al jefe de departamento
   - ✅ Verificar: Redirección automática a "Mis Convocatorias"
   - ✅ Verificar: El estado de la convocatoria cambió a "Pendiente Aprobación"

---

### Paso 4: Probar como ESTUDIANTE

1. **Login como estudiante/monitor**
   - Usuario: [tu usuario estudiante]
   - Password: [tu password]

2. **Ver convocatorias abiertas**
   - Ir a: **Convocatorias Abiertas**
   - ✅ Verificar: Ves las convocatorias abiertas
   - ✅ Verificar: Puedes filtrar por programa

3. **Postularse a una convocatoria**
   - Click en **"Postularse"** en cualquier convocatoria
   - Se abre un modal
   - Escribir carta de motivación (mínimo 50 caracteres)
   - Click en **"Enviar Postulación"**
   - ✅ Verificar: Mensaje de éxito
   - ✅ Verificar: El botón ahora dice "✓ Ya te postulaste"

4. **Repetir con 2-3 estudiantes diferentes**
   - Para que el profesor tenga opciones para seleccionar
   - Logout y login con diferentes cuentas de estudiantes

---

### Paso 5: Probar como JEFE DE DEPARTAMENTO

1. **Login como jefe de departamento**
   - Usuario: [tu usuario jefe]
   - Password: [tu password]

2. **Ver monitorías pendientes**
   - Ir a: **✓ Aprobar Monitorías (HU-010)**
   - ✅ Verificar: Ves las monitorías en estado "PENDIENTE_APROBACION"
   - ✅ Verificar: Ves:
     - Curso, Programa, Profesor
     - **Monitor Asignado** (el que seleccionó el profesor)
     - Horas estimadas
     - Período (fechas)
     - Justificación del profesor

3. **Aprobar una monitoría**
   - Click en **"✓ Aprobar"**
   - Se abre un modal con:
     - Todos los detalles de la monitoría
     - Justificación completa del profesor
     - Campo para comentario (obligatorio)
   - Ingresar comentario (ej: "Aprobada. La justificación es válida.")
   - Click en **"Confirmar Aprobación"**
   - ✅ Verificar: Mensaje de éxito
   - ✅ Verificar: La monitoría desaparece de la lista de pendientes
   - ✅ Verificar: En el backend, el estado es "APROBADA"

4. **Rechazar una monitoría** (opcional)
   - Click en **"✗ Rechazar"**
   - Se abre un modal con advertencia
   - Ingresar comentario (ej: "Rechazada. Presupuesto comprometido.")
   - Click en **"Confirmar Rechazo"**
   - ✅ Verificar: Mensaje de éxito
   - ✅ Verificar: La monitoría desaparece de pendientes

---

## 🔍 Verificaciones en Base de Datos

Después de completar el flujo, puedes verificar en la base de datos:

```sql
-- Ver estado de convocatoria
SELECT id, status, requested_hours
FROM monitoring_request 
WHERE id = [REQUEST_ID];

-- Ver postulaciones
SELECT id, monitor_id, status
FROM monitor_application
WHERE monitoring_request_id = [REQUEST_ID];

-- Ver monitoría creada
SELECT id, assigned_monitor_id, approval_status, approved_by, approval_comment
FROM monitoring
WHERE monitoring_request_id = [REQUEST_ID];
```

**Estados esperados después del flujo completo:**
- `monitoring_request.status` = "APROBADA"
- Una `monitor_application` con `status` = "SELECCIONADO"
- Las demás `monitor_application` con `status` = "NO_SELECCIONADO"
- `monitoring.approval_status` = "APROBADA"
- `monitoring.assigned_monitor_id` = [código del monitor seleccionado]
- `monitoring.approved_by` = [ID del jefe de departamento]
- `monitoring.approval_comment` = [comentario ingresado]

---

## 📱 Navegación en el Frontend

### Profesor:
- **Navbar lateral:**
  - 📢 Crear Convocatoria
  - Mis postulantes (flujo viejo)
- **Flujo:**
  - Crear Convocatoria → Ver Postulantes → Seleccionar Monitor

### Estudiante:
- **Navbar lateral:**
  - Convocatorias Abiertas
- **Flujo:**
  - Ver Convocatorias → Postularse

### Jefe de Departamento:
- **Navbar lateral:**
  - ✓ Aprobar Monitorías (HU-010) ← **NUEVO**
  - Aprobar Postulaciones (flujo viejo)
- **Flujo:**
  - Ver Monitorías Pendientes → Aprobar/Rechazar

---

## 🎨 Características Visuales

### SeleccionarMonitor:
- 🔵 Tabla moderna con degradado morado en el header
- 📝 Preview de la carta de motivación en la tabla
- 💬 Modal con carta de motivación completa
- ⚠️ Warning box amarillo con advertencias
- ✅ Botón verde para confirmar selección
- 🔙 Botón "Volver" con efecto hover

### AprobarMonitoriasHU010:
- 🔵 Tabla moderna con degradado morado en el header
- 📊 Filtro por curso
- 📈 Contador de monitorías pendientes
- 💬 Modal con todos los detalles
- 📄 Justificación del profesor en un box especial
- ⚠️ Warning box para rechazos
- ✅ Botón verde (aprobar) / ❌ Botón rojo (rechazar)
- 📝 Textarea obligatorio para comentarios

---

## 🐛 Posibles Errores y Soluciones

### Error: "No hay convocatorias para mostrar"
**Solución:** El profesor debe crear convocatorias primero

### Error: "No hay postulantes"
**Solución:** Los estudiantes deben postularse primero

### Error: "No hay monitorías pendientes"
**Solución:** El profesor debe seleccionar un monitor primero

### Error: Backend no responde
**Solución:** 
1. Verificar que el backend esté corriendo
2. Verificar que la base de datos esté accesible
3. Verificar el token de autorización

### Error: "Unauthorized"
**Solución:**
1. Logout y login nuevamente
2. Verificar que el token esté en localStorage
3. Verificar que el rol sea correcto

---

## 📊 Endpoints Utilizados

### SeleccionarMonitor:
- `GET /monitoring-request/{id}` - Obtener detalles de convocatoria
- `GET /monitor-application/request/{requestId}` - Listar postulantes
- `POST /monitor-application/select` - Seleccionar monitor

### AprobarMonitoriasHU010:
- `GET /monitoring/pending-approval` - Listar monitorías pendientes
- `POST /monitoring/approve/{id}` - Aprobar monitoría
- `POST /monitoring/reject/{id}` - Rechazar monitoría

---

## ✅ Checklist de Pruebas

- [ ] Backend corriendo sin errores
- [ ] Frontend corriendo sin errores
- [ ] Profesor puede crear convocatoria
- [ ] Convocatoria aparece en la lista del profesor
- [ ] Estudiante puede ver convocatorias abiertas
- [ ] Estudiante puede postularse con carta de motivación
- [ ] Estudiante ve "Ya te postulaste" después de postularse
- [ ] Profesor ve botón "Ver Postulantes" en convocatorias abiertas
- [ ] Profesor puede acceder a /seleccionar-monitor/:requestId
- [ ] Profesor ve lista de postulantes con sus datos
- [ ] Profesor puede ver cartas de motivación completas
- [ ] Profesor puede seleccionar un monitor
- [ ] Se muestra mensaje de éxito al seleccionar
- [ ] Convocatoria cambia a "Pendiente Aprobación"
- [ ] Jefe puede acceder a /aprobar-monitorias-hu010
- [ ] Jefe ve monitorías pendientes con monitor asignado
- [ ] Jefe puede ver justificación completa
- [ ] Jefe puede aprobar con comentario obligatorio
- [ ] Jefe puede rechazar con comentario obligatorio
- [ ] Se muestra mensaje de éxito al aprobar/rechazar
- [ ] Monitoría desaparece de la lista de pendientes
- [ ] Estados en BD son correctos

---

## 🚀 ¡Listo para Probar!

El frontend está completamente implementado y listo para probar. 

**Archivos creados:**
- ✅ `SeleccionarMonitor.js`
- ✅ `SeleccionarMonitor.css`
- ✅ `AprobarMonitoriasHU010.js`
- ✅ `AprobarMonitoriasHU010.css`

**Archivos modificados:**
- ✅ `App.js` (rutas agregadas)
- ✅ `VerticalNavbar.js` (enlace para jefe de departamento)

**Sin errores de linting** ✅

---

**¡Éxito con las pruebas!** 🎉

Si encuentras algún problema, revisa:
1. La consola del navegador (F12)
2. Los logs del backend
3. Los estados en la base de datos
4. Los tokens de autorización en localStorage

