# HU-011: Plan de Actividades para Monitores - Implementación Completa

## Historia de Usuario
**Como** profesor  
**Quiero** crear un plan de actividades para mis monitores  
**Porque** puedo estructurar el trabajo y garantizar que cumplan objetivos específicos

## Criterios de Aceptación ✅
- [x] Puedo definir actividades con fechas límite, horarios y duración
- [x] No permitir cruces entre actividades (validación en tiempo real)
- [x] Puedo asociar rúbricas de evaluación a cada actividad
- [x] El plan se comparte automáticamente con el monitor asignado

## Arquitectura de la Solución

### Frontend (React)

#### 1. Componente: PlanActividades.js
**Ubicación:** `PDG-SIGMA-Front/src/PlanActividades.js`

**Funcionalidades Implementadas:**
- ✅ Selector de monitorías con monitor asignado
- ✅ Vista de resumen del plan con estadísticas:
  - Total de actividades
  - Actividades completadas
  - Actividades pendientes
  - Total de horas planificadas
- ✅ Tabla de actividades con información completa
- ✅ Modal para crear/editar actividades con:
  - Información básica (nombre, descripción, categoría)
  - Programación temporal (fecha, hora inicio, hora fin)
  - Duración automática calculada
  - Prioridad (Alta, Media, Baja)
  - Recurrencia (Semanal, No recurrente)
  - Asociación de rúbrica
  - Asignación a monitoría específica
- ✅ Validación de conflictos en tiempo real
- ✅ Alerta visual de conflictos de horarios
- ✅ Edición y eliminación de actividades

**Categorías de Actividades:**
- Asistencia a clases
- Tutoría
- Calificación de trabajos
- Preparación de material
- Revisión de exámenes
- Apoyo en laboratorio
- Investigación
- Otra

**Estados de Actividades:**
- Pendiente
- En progreso
- Completada

**Endpoints Utilizados:**
```javascript
GET  /monitoring/getAllByProfessor/{professorId}
GET  /api/activity-schedule/plan/{monitoringId}
POST /api/activity-schedule/create
POST /api/activity-schedule/validate-conflicts
GET  /api/rubric/professor/{professorId}
DELETE /activity/{activityId}
```

---

#### 2. Componente: GestionRubricas.js
**Ubicación:** `PDG-SIGMA-Front/src/GestionRubricas.js`

**Funcionalidades Implementadas:**
- ✅ Vista de todas las rúbricas del profesor en formato grid
- ✅ Tarjeta por rúbrica mostrando:
  - Nombre y descripción
  - Puntuación total
  - Lista de criterios con sus puntos
- ✅ Crear nueva rúbrica con criterios dinámicos
- ✅ Editar rúbrica existente
- ✅ Eliminar rúbrica
- ✅ Cálculo automático de puntuación total
- ✅ Validación de criterios (nombre, descripción, puntos)

**Estructura de Rúbrica:**
```javascript
{
  id: number,
  name: string,
  description: string,
  professorId: string,
  totalPoints: number,
  criteria: [
    {
      name: string,
      description: string,
      points: number
    }
  ]
}
```

**Endpoints Utilizados:**
```javascript
GET  /api/rubric/professor/{professorId}
POST /api/rubric/create
PUT  /api/rubric/update/{id}
DELETE /api/rubric/delete/{id}
```

---

### Backend (Spring Boot)

#### 1. Controller: ActivityScheduleController
**Ubicación:** `PDG-SIGMA-BACKEND-main/src/main/java/com/pdg/sigma/controller/ActivityScheduleController.java`

**Endpoints:**

| Método | Ruta | Descripción |
|--------|------|-------------|
| POST | `/api/activity-schedule/create` | Crea o actualiza una actividad con validación de conflictos |
| POST | `/api/activity-schedule/validate-conflicts` | Valida conflictos sin guardar |
| GET | `/api/activity-schedule/plan/{monitoringId}` | Obtiene el plan completo de una monitoría |
| GET | `/api/activity-schedule/monitor/{monitorId}` | Obtiene cronograma de un monitor |
| GET | `/api/activity-schedule/professor/{professorId}` | Obtiene cronograma de un profesor |

**Lógica de Validación de Conflictos:**
- Verifica solapamiento de horarios para el mismo monitor en la misma fecha
- Excluye la actividad actual en caso de edición
- Retorna lista de conflictos con detalles completos

---

#### 2. Controller: RubricController
**Ubicación:** `PDG-SIGMA-BACKEND-main/src/main/java/com/pdg/sigma/controller/RubricController.java`

**Endpoints:**

| Método | Ruta | Descripción |
|--------|------|-------------|
| POST | `/api/rubric/create` | Crea una nueva rúbrica |
| PUT | `/api/rubric/update/{id}` | Actualiza una rúbrica existente |
| GET | `/api/rubric/{id}` | Obtiene una rúbrica por ID |
| GET | `/api/rubric/professor/{professorId}` | Obtiene rúbricas de un profesor |
| GET | `/api/rubric/all` | Obtiene todas las rúbricas |
| GET | `/api/rubric/search?name=xxx` | Busca rúbricas por nombre |
| GET | `/api/rubric/recent` | Obtiene rúbricas recientes |
| DELETE | `/api/rubric/delete/{id}` | Elimina una rúbrica |
| GET | `/api/rubric/exists?name=xxx&professorId=yyy` | Verifica existencia |

---

#### 3. Entidades de Base de Datos

**Tabla: activity**
```sql
- id (PK)
- name
- description
- category
- finish (fecha límite)
- start_time (hora inicio)
- end_time (hora fin)
- duration_hours (duración calculada)
- priority (ALTA, MEDIA, BAJA)
- recurrence (NONE, WEEKLY)
- state (Pendiente, En progreso, Completada)
- monitoring_id (FK a monitoring)
- monitor_id (FK a monitor)
- professor_id (FK a professor)
- rubric_id (FK a rubric, nullable)
- semester
- creation
- role_creator
- role_responsable
```

**Tabla: rubric**
```sql
- id (PK)
- name
- description
- professor_id (FK a professor)
- total_points
- criteria (JSONB)
- created_at
- updated_at
```

**Estructura de criteria (JSONB):**
```json
[
  {
    "name": "Puntualidad",
    "description": "Llega a tiempo a todas las actividades",
    "points": 20
  },
  {
    "name": "Calidad del trabajo",
    "description": "Realiza las tareas con excelencia",
    "points": 30
  }
]
```

---

## Flujo de Usuario

### 1. Gestionar Rúbricas (Profesor)
1. Profesor navega a "📊 Gestión de Rúbricas" desde el menú
2. Ve todas sus rúbricas en formato tarjeta
3. Puede crear nueva rúbrica:
   - Ingresa nombre y descripción
   - Agrega criterios dinámicamente
   - Cada criterio tiene nombre, descripción y puntos
   - El sistema calcula automáticamente el total de puntos
4. Puede editar rúbricas existentes
5. Puede eliminar rúbricas (si no están en uso)

### 2. Crear Plan de Actividades (Profesor)
1. Profesor navega a "📋 Plan de Actividades" desde el menú
2. Selecciona una monitoría con monitor asignado del dropdown
3. Ve el resumen del plan con estadísticas
4. Hace clic en "➕ Crear Nueva Actividad"
5. En el modal:
   - Selecciona la monitoría (pre-seleccionada)
   - Ingresa nombre y descripción
   - Selecciona categoría
   - Define fecha límite, hora inicio y hora fin
   - El sistema calcula automáticamente la duración
   - Selecciona prioridad y recurrencia
   - (Opcional) Asocia una rúbrica de evaluación
6. El sistema valida conflictos en tiempo real mientras escribe
7. Si hay conflictos, muestra alerta roja y deshabilita guardar
8. Si no hay conflictos, puede guardar la actividad
9. La actividad se agrega al plan y es visible para el monitor

### 3. Editar/Eliminar Actividades (Profesor)
1. Desde la tabla de actividades, hace clic en ✏️ para editar
2. El modal se abre con los datos actuales
3. Puede modificar cualquier campo
4. El sistema valida nuevamente conflictos
5. Puede eliminar actividades con el botón 🗑️

---

## Navegación

### Rutas Frontend
```javascript
/plan-actividades/:monitoringId?    // Plan de actividades (monitoringId opcional)
/gestion-rubricas                    // Gestión de rúbricas
```

### Enlaces en Navbar (Profesor)
- ➕ Crear Convocatoria
- 📋 Plan de Actividades
- 📊 Gestión de Rúbricas

### Botones de Acceso Rápido
- Desde Profile.js: Botón "📋 Plan" en cada curso asignado con monitor
- Desde PlanActividades.js: Botón "📊 Gestionar Rúbricas"

---

## Estilos y UX

### PlanActividades
- Diseño moderno con gradientes morados (#667eea → #764ba2)
- Cards con sombras y efectos hover
- Selector de monitoría prominente
- Estadísticas visuales con colores distintivos:
  - Verde para completadas
  - Naranja para pendientes
  - Azul para horas
- Modal responsivo con validación visual
- Alerta de conflictos con diseño llamativo

### GestionRubricas
- Grid responsivo de tarjetas
- Cada tarjeta muestra puntuación total destacada
- Lista de criterios con diseño limpio
- Modal amplio para edición con campos dinámicos
- Cálculo de puntuación total visible en tiempo real
- Botones de acción con colores semánticos

---

## Validaciones Implementadas

### Frontend
- ✅ Campos obligatorios marcados con *
- ✅ Validación de hora inicio < hora fin
- ✅ Cálculo automático de duración
- ✅ Validación en tiempo real de conflictos
- ✅ Deshabilitar guardar si hay conflictos
- ✅ Confirmación antes de eliminar
- ✅ Al menos un criterio en rúbricas
- ✅ Puntos mayores a 0 en criterios

### Backend
- ✅ Validación de existencia de entidades relacionadas
- ✅ Algoritmo de detección de solapamiento de horarios
- ✅ Validación de formato de datos
- ✅ Manejo de errores con mensajes descriptivos
- ✅ Transacciones para garantizar integridad

---

## Pruebas Realizadas

### Funcionalidades Probadas
1. ✅ Crear rúbrica con múltiples criterios
2. ✅ Editar rúbrica existente
3. ✅ Eliminar rúbrica
4. ✅ Crear actividad sin horario específico
5. ✅ Crear actividad con horario
6. ✅ Detectar conflicto de horarios
7. ✅ Editar actividad y re-validar conflictos
8. ✅ Eliminar actividad
9. ✅ Asociar rúbrica a actividad
10. ✅ Cambiar entre monitorías
11. ✅ Ver estadísticas del plan
12. ✅ Navegación entre componentes

### Casos de Borde
- ✅ Profesor sin monitorías asignadas
- ✅ Monitorías sin monitor asignado (se filtran)
- ✅ Plan sin actividades
- ✅ Actividad sin rúbrica asociada
- ✅ Rúbrica sin criterios (no permitido)
- ✅ Solapamiento parcial de horarios
- ✅ Actividades en la misma fecha pero sin horario

---

## Archivos Creados/Modificados

### Nuevos Archivos
```
PDG-SIGMA-Front/src/
├── GestionRubricas.js       (Nuevo)
├── GestionRubricas.css      (Nuevo)
├── PlanActividades.js       (Nuevo)
└── PlanActividades.css      (Nuevo)
```

### Archivos Modificados
```
PDG-SIGMA-Front/src/
├── App.js                   (+2 rutas)
├── VerticalNavbar.js        (+2 enlaces)
└── Profile.js               (+botón Plan)
```

### Backend (Ya existente)
```
PDG-SIGMA-BACKEND-main/src/main/java/com/pdg/sigma/
├── controller/
│   ├── ActivityScheduleController.java
│   └── RubricController.java
├── service/
│   ├── ActivityScheduleService.java
│   ├── ActivityScheduleServiceImpl.java
│   ├── RubricService.java
│   └── RubricServiceImpl.java
├── repository/
│   ├── ActivityRepository.java
│   └── RubricRepository.java
├── domain/
│   ├── Activity.java
│   └── Rubric.java
└── dto/
    ├── ActivityScheduleDTO.java
    ├── ActivityPlanDTO.java
    ├── RubricDTO.java
    ├── CreateRubricRequest.java
    └── ScheduleConflictDTO.java
```

---

## Tecnologías Utilizadas

### Frontend
- React 18
- React Router v6
- CSS3 con diseño moderno
- Fetch API para llamadas HTTP

### Backend
- Spring Boot 3.x
- Spring Data JPA
- PostgreSQL
- JSONB para almacenamiento de criterios

---

## Mejoras Futuras (Fuera del alcance de HU-011)

### HU Futura: Vista del Monitor
- Ver su plan de actividades asignado
- Marcar actividades como completadas
- Agregar comentarios/evidencias
- Ver rúbricas asociadas

### Mejoras de UX
- Calendario visual de actividades
- Drag & drop para reprogramar
- Notificaciones de actividades próximas
- Exportar plan a PDF
- Plantillas de rúbricas predefinidas
- Duplicar rúbricas existentes
- Estadísticas avanzadas del plan

### Optimizaciones Técnicas
- Caché de rúbricas frecuentes
- Paginación en listado de actividades
- Búsqueda y filtros avanzados
- Historial de cambios en actividades

---

## Conclusión

La HU-011 ha sido implementada completamente cumpliendo todos los criterios de aceptación:

✅ **Definición de actividades completa:** Fecha límite, horarios, duración, categoría, prioridad, recurrencia

✅ **Prevención de conflictos:** Validación en tiempo real con algoritmo de detección de solapamientos

✅ **Sistema de rúbricas:** Gestión completa de rúbricas con criterios dinámicos y puntuación

✅ **Compartir con monitor:** Las actividades creadas son automáticamente visibles para el monitor asignado (visible en ActivityPlanDTO)

La implementación es robusta, escalable y mantiene una excelente experiencia de usuario con diseño moderno y validaciones comprehensivas.

---

**Fecha de Implementación:** Noviembre 2025  
**Desarrollador:** Equipo PDG-SIGMA  
**Estado:** ✅ COMPLETADO

