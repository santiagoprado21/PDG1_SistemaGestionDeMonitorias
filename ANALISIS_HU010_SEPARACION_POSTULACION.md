# Análisis HU-010: Separación de Postulación y Monitoría

## 📋 Historia de Usuario
**HU-010: Crear postulación de monitorias por parte de los profesores**

> Como profesor
> Quiero postular monitorias de la lista de cursos elegibles
> Para solicitar apoyo académico para mis estudiantes

**Criterios de Aceptación:**
- Veo solo los cursos elegibles para monitoria de mi departamento
- Puedo especificar las horas solicitadas y justificación
- Recibo confirmación de mi postulación por medio de un mensaje en pantalla
- Filtrado automático por departamento y por bloque

---

## 🔍 Estructura Actual del Sistema

### Entidades Principales

#### 1. `Monitoring` (Monitoría)
- Representa una monitoría **YA CREADA y ACTIVA**
- Creada directamente por el profesor
- Contiene: curso, programa, facultad, fechas, profesor, requisitos académicos
- Tiene presupuesto: `estimatedHours`, `hourlyRate`

#### 2. `Monitor` (Estudiante Monitor)
- Representa el perfil de un estudiante que puede/es monitor
- Información: código, nombre, semestre, promedios, email

#### 3. `MonitoringMonitor` (Tabla de Relación)
**⚠️ PROBLEMA: Esta tabla cumple MÚLTIPLES roles actualmente:**

```java
@Entity
@Table(name = "monitoring_monitor")
public class MonitoringMonitor {
    @Id
    private Long id;
    
    @ManyToOne
    private Monitoring monitoring;
    
    @ManyToOne
    private Monitor monitor;
    
    // Estados: "no seleccionado", "seleccionado", "aprobado", "rechazado"
    private String estadoSeleccion = "no seleccionado";
    
    private String comentarioDecision;
    private LocalDateTime fechaDecision;
    private String decididoPor;
}
```

**Roles actuales de MonitoringMonitor:**
1. ✅ Representa la **postulación de un estudiante** a una monitoría
2. ✅ Representa la **asignación de un monitor** a una monitoría
3. ✅ Guarda el **estado del proceso de selección**

---

## 🔄 Flujo Actual del Sistema

```
1. PROFESOR crea Monitoring (monitoría)
   ↓
2. Monitoring está INMEDIATAMENTE disponible para postulaciones de estudiantes
   ↓
3. ESTUDIANTES se postulan → Se crea MonitoringMonitor con estado "no seleccionado"
   ↓
4. PROFESOR revisa y marca como "seleccionado" al candidato que prefiere
   ↓
5. JEFE DE DEPARTAMENTO aprueba/rechaza → Estado cambia a "aprobado"/"rechazado"
   ↓
6. Monitor asignado trabaja en la monitoría
```

**Problemas identificados:**
- ❌ No hay un proceso de **postulación previa de la monitoría** por el profesor
- ❌ El profesor no puede **justificar la necesidad** de la monitoría
- ❌ No hay **aprobación institucional** antes de abrir la convocatoria
- ❌ La misma tabla maneja postulación de monitores Y asignación final
- ❌ No se valida presupuesto antes de crear la monitoría

---

## 💡 Propuesta de Separación (FLUJO CORRECTO)

### Nuevo Flujo Propuesto

```
1. PROFESOR crea MonitoringRequest (POSTULACIÓN de monitoría)
   - Selecciona curso de su departamento
   - Especifica: horas solicitadas, justificación, periodo, requisitos
   - Sistema valida: permisos, presupuesto disponible, periodo académico
   - Estado: CONVOCATORIA_ABIERTA
   ↓
2. MonitoringRequest está INMEDIATAMENTE disponible para postulaciones
   - Los ESTUDIANTES pueden ver y postularse
   - NO se ha creado aún la Monitoring oficial
   ↓
3. ESTUDIANTES se postulan a la MonitoringRequest
   - Se crea MonitorApplication (postulación de estudiante)
   - Múltiples estudiantes pueden postularse
   - Estado de cada postulación: POSTULADO
   ↓
4. PROFESOR revisa postulantes y ELIGE UNO
   - Marca una MonitorApplication como "SELECCIONADA"
   - MonitoringRequest pasa a estado: MONITOR_SELECCIONADO
   ↓
5. Sistema CREA automáticamente Monitoring + Asignación
   - Se crea la Monitoring oficial (monitoría activa)
   - Se vincula con el estudiante seleccionado
   - MonitoringRequest pasa a estado: PENDIENTE_APROBACION
   ↓
6. JEFE DE DEPARTAMENTO revisa y aprueba/rechaza el "paquete completo"
   - Revisa: Monitoría + Monitor asignado + Justificación
   - Aprueba → Monitoring pasa a "APROBADA" (ya puede trabajar)
   - Rechaza → Monitoring pasa a "RECHAZADA" (proceso termina)
   ↓
7. Monitor asignado trabaja en la monitoría aprobada
```

**⚠️ Diferencia clave con arquitecturas tradicionales:**
- Los estudiantes se postulan a la "convocatoria" (MonitoringRequest)
- El jefe aprueba la "monitoría completa" (Monitoring con monitor ya asignado)
- Hay separación clara entre el proceso de postulación y la monitoría oficial

---

## 🗄️ Estructura de Datos Propuesta

### Nueva Entidad: `MonitoringRequest` (Convocatoria/Postulación de Monitoría)

```java
@Entity
@Table(name = "monitoring_request")
public class MonitoringRequest {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    // Información del profesor que postula
    @ManyToOne
    @JoinColumn(name = "professor_id", nullable = false)
    private Professor professor;
    
    // Curso para el cual solicita la monitoría
    @ManyToOne
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;
    
    @ManyToOne
    @JoinColumn(name = "school_id", nullable = false)
    private School school;
    
    @ManyToOne
    @JoinColumn(name = "program_id", nullable = false)
    private Program program;
    
    // Detalles de la solicitud (HU-010)
    @Column(name = "requested_hours", nullable = false)
    private Integer requestedHours;
    
    @Column(name = "justification", nullable = false, columnDefinition = "TEXT")
    private String justification;
    
    @Column(name = "semester", nullable = false)
    private String semester;
    
    @Column(name = "start_date", nullable = false)
    private Date startDate;
    
    @Column(name = "finish_date", nullable = false)
    private Date finishDate;
    
    // Requisitos académicos para los postulantes
    @Column(name = "required_average_grade")
    private Double requiredAverageGrade;
    
    @Column(name = "required_course_grade")
    private Double requiredCourseGrade;
    
    // Estado de la convocatoria/postulación
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private RequestStatus status;
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    
    // Relación con postulaciones de estudiantes
    @OneToMany(mappedBy = "monitoringRequest", cascade = CascadeType.ALL)
    private List<MonitorApplication> studentApplications;
    
    // Relación con la monitoría oficial creada (cuando se selecciona monitor)
    @OneToOne(mappedBy = "originatingRequest")
    private Monitoring createdMonitoring;
}

enum RequestStatus {
    CONVOCATORIA_ABIERTA,    // Recién creada, esperando postulantes
    MONITOR_SELECCIONADO,    // Profesor ya eligió un estudiante
    PENDIENTE_APROBACION,    // Enviada al jefe de dpto con monitor
    APROBADA,                // Jefe de dpto aprobó (monitoría activa)
    RECHAZADA,               // Jefe de dpto rechazó
    CANCELADA                // Profesor canceló antes de completar
}
```

### Entidad Modificada: `Monitoring` (Monitoría Oficial Activa)

```java
@Entity
@Table(name = "monitoring")
public class Monitoring {
    // ... campos existentes (school, program, course, dates, professor, etc.) ...
    
    // NUEVA: Relación con la convocatoria que la originó
    @OneToOne
    @JoinColumn(name = "monitoring_request_id")
    private MonitoringRequest originatingRequest;
    
    // NUEVA: Estado de aprobación por jefe de departamento
    @Enumerated(EnumType.STRING)
    @Column(name = "approval_status", nullable = false)
    private MonitoringApprovalStatus approvalStatus;
    
    // NUEVA: Monitor asignado desde la creación
    @ManyToOne
    @JoinColumn(name = "assigned_monitor_id", nullable = false)
    private Monitor assignedMonitor;
    
    // Auditoría de aprobación del jefe
    @Column(name = "approved_by")
    private String approvedBy; // ID del jefe de departamento
    
    @Column(name = "approval_comment", columnDefinition = "TEXT")
    private String approvalComment;
    
    @Column(name = "approval_date")
    private LocalDateTime approvalDate;
}

enum MonitoringApprovalStatus {
    PENDIENTE_APROBACION,    // Recién creada con monitor, esperando jefe
    APROBADA,                // Jefe aprobó, puede funcionar
    RECHAZADA                // Jefe rechazó
}
```

### Nueva Entidad: `MonitorApplication` (Postulación de Estudiante)

```java
@Entity
@Table(name = "monitor_application")
public class MonitorApplication {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    // IMPORTANTE: Se postula a la REQUEST (convocatoria), NO a la Monitoring
    @ManyToOne
    @JoinColumn(name = "monitoring_request_id", nullable = false)
    private MonitoringRequest monitoringRequest;
    
    @ManyToOne
    @JoinColumn(name = "monitor_id", nullable = false)
    private Monitor monitor;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private ApplicationStatus status = ApplicationStatus.POSTULADO;
    
    @Column(name = "application_date", nullable = false)
    private LocalDateTime applicationDate;
    
    // Información adicional de la postulación (opcional)
    @Column(name = "motivation_letter", columnDefinition = "TEXT")
    private String motivationLetter;
}

enum ApplicationStatus {
    POSTULADO,      // Estudiante se postuló a la convocatoria
    SELECCIONADO,   // Profesor lo eligió (solo uno puede tener este estado)
    NO_SELECCIONADO // Los demás quedan así cuando se elige a otro
}
```

---

## 🔄 Migración de Datos

### Opción 1: Migración Completa
- Mantener `monitoring_monitor` para monitorías históricas
- Nuevas postulaciones usan el nuevo sistema
- Requiere lógica dual durante transición

### Opción 2: Renombrar y Extender (Recomendado)
1. Renombrar `monitoring_monitor` → `monitor_application`
2. Crear nueva tabla `monitoring_application`
3. Para monitorías existentes: Crear automáticamente `monitoring_application` con estado "APROBADA"
4. Asociar `monitoring` existentes con sus `monitoring_application` generadas

```sql
-- Script de migración
-- 1. Crear nueva tabla monitoring_application
CREATE TABLE monitoring_application (
    id BIGSERIAL PRIMARY KEY,
    professor_id VARCHAR(20) NOT NULL,
    course_id INTEGER NOT NULL,
    school_id INTEGER NOT NULL,
    program_id INTEGER NOT NULL,
    requested_hours INTEGER NOT NULL,
    justification TEXT NOT NULL,
    semester VARCHAR(8) NOT NULL,
    start_date DATE NOT NULL,
    finish_date DATE NOT NULL,
    required_average_grade DOUBLE PRECISION,
    required_course_grade DOUBLE PRECISION,
    status VARCHAR(20) NOT NULL DEFAULT 'APROBADA',
    reviewed_by VARCHAR(20),
    review_comment TEXT,
    review_date TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    monitoring_id BIGINT UNIQUE,
    FOREIGN KEY (professor_id) REFERENCES professor(id),
    FOREIGN KEY (course_id) REFERENCES course(id),
    FOREIGN KEY (school_id) REFERENCES school(id),
    FOREIGN KEY (program_id) REFERENCES program(id)
);

-- 2. Generar monitoring_application para cada monitoring existente
INSERT INTO monitoring_application (
    professor_id, course_id, school_id, program_id,
    requested_hours, justification, semester, start_date, finish_date,
    required_average_grade, required_course_grade,
    status, created_at, monitoring_id
)
SELECT 
    m.professor_id,
    m.course_id,
    m.school_id,
    m.program_id,
    COALESCE(m.estimated_hours, 40) as requested_hours,
    'Migrado automáticamente desde monitoría existente' as justification,
    m.semester,
    m.start_date,
    m.finish_date,
    m.average_grade,
    m.course_grade,
    'APROBADA' as status,
    NOW() as created_at,
    m.id as monitoring_id
FROM monitoring m;

-- 3. Renombrar tabla monitoring_monitor
ALTER TABLE monitoring_monitor RENAME TO monitor_application;

-- 4. Ajustar columnas de monitor_application
ALTER TABLE monitor_application 
    ADD COLUMN application_date TIMESTAMP DEFAULT NOW();

-- 5. Mapear estados antiguos a nuevos
UPDATE monitor_application 
SET status = CASE 
    WHEN estado_seleccion = 'seleccionado' THEN 'SELECCIONADO'
    WHEN estado_seleccion = 'aprobado' THEN 'APROBADO'
    WHEN estado_seleccion = 'rechazado' THEN 'RECHAZADO'
    ELSE 'POSTULADO'
END;

ALTER TABLE monitor_application DROP COLUMN estado_seleccion;
```

---

## 📁 Cambios en el Código

### Nuevos Controladores

```java
@RestController
@RequestMapping("/monitoring-application")
public class MonitoringApplicationController {
    
    // Profesor crea postulación
    @PostMapping("/create")
    public ResponseEntity<?> createApplication(@RequestBody MonitoringApplicationDTO dto);
    
    // Jefe de dpto lista postulaciones pendientes de su departamento
    @GetMapping("/pending/{departmentHeadId}")
    public ResponseEntity<List<MonitoringApplication>> getPendingApplications(@PathVariable String departmentHeadId);
    
    // Jefe de dpto aprueba postulación
    @PostMapping("/approve/{applicationId}")
    public ResponseEntity<?> approveApplication(@PathVariable Long applicationId, @RequestBody ApprovalRequest request);
    
    // Jefe de dpto rechaza postulación
    @PostMapping("/reject/{applicationId}")
    public ResponseEntity<?> rejectApplication(@PathVariable Long applicationId, @RequestBody RejectionRequest request);
    
    // Profesor consulta sus postulaciones
    @GetMapping("/by-professor/{professorId}")
    public ResponseEntity<List<MonitoringApplication>> getApplicationsByProfessor(@PathVariable String professorId);
}
```

### Servicios Modificados

```java
@Service
public class MonitoringApplicationService {
    
    public MonitoringApplication createApplication(MonitoringApplicationDTO dto) throws Exception {
        // 1. Validar que el profesor pertenece al departamento del curso
        // 2. Validar que hay presupuesto disponible
        // 3. Validar periodo académico
        // 4. Crear MonitoringApplication con estado PENDIENTE
        // 5. Enviar notificación al jefe de departamento
    }
    
    public void approveApplication(Long applicationId, ApprovalRequest request) throws Exception {
        // 1. Cambiar estado a APROBADA
        // 2. Crear Monitoring automáticamente
        // 3. Vincular MonitoringApplication con Monitoring creada
        // 4. Enviar notificación al profesor
    }
    
    public void rejectApplication(Long applicationId, RejectionRequest request) throws Exception {
        // 1. Cambiar estado a RECHAZADA
        // 2. Guardar comentario de rechazo
        // 3. Enviar notificación al profesor
    }
}

@Service
public class MonitoringService {
    
    // MODIFICAR: Ya no se crea directamente, solo desde MonitoringApplication aprobada
    @Deprecated
    public Monitoring save(MonitoringDTO entity) throws Exception {
        throw new UnsupportedOperationException(
            "Use MonitoringApplicationService para crear nuevas monitorías"
        );
    }
    
    // Nuevo método interno (package-private)
    Monitoring createFromApprovedApplication(MonitoringApplication application) {
        // Crear Monitoring basada en la application aprobada
    }
}
```

---

## ✅ Ventajas de la Separación

1. **Claridad Conceptual**: Cada entidad tiene una responsabilidad única
2. **Proceso Controlado**: El jefe de departamento aprueba antes de abrir convocatoria
3. **Trazabilidad**: Se puede rastrear desde la postulación hasta la monitoría final
4. **Validación Temprana**: Presupuesto y requisitos se validan antes de crear monitoría
5. **Cumplimiento de HU-010**: El profesor ahora "postula" explícitamente con justificación
6. **Separación de Flujos**: 
   - Flujo de postulación de monitoría (profesor → jefe)
   - Flujo de postulación de estudiantes (estudiante → profesor → jefe)

---

## 🚧 Consideraciones de Implementación

### Fase 1: Backend
1. Crear entidad `MonitoringApplication`
2. Crear entidad `MonitorApplication` (renombrar `MonitoringMonitor`)
3. Crear servicios y controladores
4. Implementar validaciones de presupuesto
5. Script de migración de datos

### Fase 2: Frontend
1. Crear interfaz de postulación para profesores (HU-010)
2. Crear interfaz de aprobación para jefes de departamento
3. Modificar flujo de creación de monitorías (redirigir a postulación)
4. Actualizar vistas de estudiantes (solo ver monitorías aprobadas)

### Fase 3: Testing
1. Probar flujo completo de postulación
2. Probar validaciones de presupuesto
3. Probar migración de datos históricos
4. Pruebas de integración E2E

---

## 📊 Diagrama de Entidades (Propuesto - FLUJO CORRECTO)

```
┌─────────────────────────┐
│   Professor             │
└───────────┬─────────────┘
            │ 1. creates
            ↓
┌─────────────────────────────────────────────────────┐
│ MonitoringRequest (Convocatoria) ← NUEVA ENTIDAD   │
│ ──────────────────────────────────────────────────  │
│ - justification, requestedHours                     │
│ - status: CONVOCATORIA_ABIERTA                      │
│         → MONITOR_SELECCIONADO                      │
│         → PENDIENTE_APROBACION                      │
│         → APROBADA / RECHAZADA                      │
└───────────┬──────────────────────┬──────────────────┘
            │                      │
            │ 2. Students apply    │ 4. When monitor selected
            ↓                      │    creates Monitoring
┌─────────────────────────┐       │
│ MonitorApplication      │       │
│ (Postulación Estudiante)│       │
│ ──────────────────────  │       │
│ - status: POSTULADO     │       ↓
│         → SELECCIONADO  │  ┌─────────────────────────────────────┐
│         → NO_SELECCIONADO  │  Monitoring (Monitoría Oficial)     │
└──┬──────────────────────┘  │ ──────────────────────────────────  │
   │                         │ - course, dates, requirements       │
   │ 3. references           │ - assignedMonitor (desde creación)  │
   ↓                         │ - approvalStatus:                   │
┌─────────────────────────┐ │     PENDIENTE_APROBACION            │
│ Monitor (Student)       │ │   → APROBADA / RECHAZADA (Jefe)     │
└─────────────────────────┘ └─────────────────────────────────────┘
                                      ↑
                                      │ 5. Jefe de Dpto
                                      │    aprueba/rechaza
                                      │    el "paquete completo"
```

**Flujo Visual:**
1. Profesor crea `MonitoringRequest` (convocatoria abierta)
2. Estudiantes crean `MonitorApplication` (se postulan a la convocatoria)
3. Profesor elige 1 `MonitorApplication` → marca como SELECCIONADO
4. Sistema crea `Monitoring` con monitor ya asignado
5. Jefe de Dpto aprueba/rechaza la `Monitoring` completa

**⚠️ Separación clara:**
- `MonitoringRequest` = Proceso de convocatoria (antes de monitoría oficial)
- `MonitorApplication` = Postulaciones de estudiantes a la convocatoria
- `Monitoring` = Monitoría oficial con monitor asignado (espera aprobación jefe)

---

## 🎯 Resumen

**Problema actual:** 
- Monitorías se crean directamente sin proceso de postulación
- Estudiantes se postulan después de que la monitoría ya está creada
- No hay un proceso claro de convocatoria → selección → aprobación

**Solución propuesta (FLUJO CORRECTO):**
1. Introducir `MonitoringRequest` (convocatoria) donde:
   - Profesor postula con justificación y detalles
   - Estudiantes se postulan a la convocatoria (NO a la monitoría)
   - Profesor selecciona 1 estudiante de los postulantes
   
2. Sistema crea `Monitoring` automáticamente cuando se selecciona monitor:
   - La monitoría ya tiene monitor asignado desde su creación
   - Va directamente a aprobación del jefe de departamento
   
3. Jefe de departamento aprueba el "paquete completo":
   - Revisa monitoría + monitor + justificación
   - Una sola aprobación para todo

**Impacto:** 
- ✅ Cumple con HU-010 completamente (profesor postula con justificación)
- ✅ Separa TRES procesos claramente:
  - Proceso 1: Convocatoria (MonitoringRequest)
  - Proceso 2: Postulación estudiantes (MonitorApplication)
  - Proceso 3: Monitoría oficial (Monitoring con aprobación)
- ✅ Jefe aprueba al FINAL (monitoría + monitor juntos)
- ✅ Mejora control presupuestario
- ✅ Mantiene compatibilidad con datos históricos
- ⚠️ Requiere migración de datos y actualización del frontend

---

## 🤔 Preguntas para el Equipo

### Sobre el proceso de convocatoria:
1. ¿Puede el profesor editar/cancelar una `MonitoringRequest` después de crearla?
   - Si ya hay estudiantes postulados, ¿se les notifica?
   
2. ¿Cuánto tiempo debe estar abierta una convocatoria?
   - ¿Hay un mínimo/máximo de días?
   - ¿El profesor puede cerrarla anticipadamente?

3. ¿Qué pasa si ningún estudiante se postula?
   - ¿Se cancela automáticamente después de X días?
   - ¿El profesor recibe alerta?

### Sobre la selección de monitor:
4. ¿El profesor puede cambiar su selección antes de que el jefe apruebe?
   - Si ya seleccionó a Juan pero quiere cambiar a María
   
5. ¿Qué pasa con los estudiantes NO seleccionados?
   - ¿Reciben notificación de rechazo?
   - ¿Sus postulaciones quedan registradas para futuras convocatorias?

### Sobre la aprobación del jefe:
6. ¿El jefe puede rechazar y solicitar cambio de monitor?
   - O solo puede aprobar/rechazar todo el paquete?
   
7. ¿Si el jefe rechaza, el profesor puede volver a abrir convocatoria?
   - ¿Se mantiene la justificación original?
   
8. ¿El jefe puede modificar las horas asignadas al aprobar?
   - O debe aceptar lo que el profesor solicitó?

### Sobre el presupuesto:
9. ¿Validamos presupuesto al crear la convocatoria o al aprobar?
   - Validación temprana: evita convocatorias inviables
   - Validación tardía: más flexibilidad para el profesor
   
10. ¿Qué pasa si el presupuesto se agota mientras hay convocatorias abiertas?
    - ¿Se cancelan automáticamente?
    - ¿Se priorizan por fecha de creación?

### Técnicas:
11. ¿Migramos las `Monitoring` existentes como si vinieran de `MonitoringRequest` aprobadas?
12. ¿Mantenemos compatibilidad con el flujo antiguo durante un periodo de transición?

---

## 📱 Ejemplo del Flujo Completo (Con Casos de Uso)

### Caso 1: Flujo Exitoso Completo

**Paso 1: Profesor crea convocatoria**
```
Pantalla: "Crear Postulación de Monitoría"
Profesor Juan García selecciona:
- Curso: "Cálculo Diferencial"
- Horas solicitadas: 40 horas
- Justificación: "Hay 80 estudiantes inscritos y alto índice de pérdida"
- Fecha inicio: 2025-11-01
- Fecha fin: 2025-12-15
- Requisitos: Promedio ≥ 4.0, Nota curso ≥ 4.5

Sistema crea: MonitoringRequest #123 con estado CONVOCATORIA_ABIERTA
```

**Paso 2: Estudiantes se postulan**
```
Pantalla estudiante: "Convocatorias Abiertas"
María López ve:
- "Cálculo Diferencial - Prof. Juan García"
- "40 horas - Periodo 2025-11"
- Requisitos: ✅ Cumple

María hace clic en "Postularme"
Sistema crea: MonitorApplication #456
  - monitoringRequestId: 123
  - monitorId: maria_lopez
  - status: POSTULADO

También se postulan: Pedro Ruiz, Ana Torres, Carlos Díaz
```

**Paso 3: Profesor revisa y selecciona**
```
Pantalla profesor: "Mis Convocatorias"
Juan ve convocatoria #123:
- "Cálculo Diferencial"
- "4 postulantes"

Entra a ver detalle:
┌────────────────────────────────────────┐
│ María López - Código: 202012345        │
│ Promedio: 4.5 | Nota curso: 4.8       │
│ Semestre: 6                            │
│ [Seleccionar]                          │
├────────────────────────────────────────┤
│ Pedro Ruiz - Código: 202012346         │
│ Promedio: 4.2 | Nota curso: 4.3       │
│ [Seleccionar]                          │
└────────────────────────────────────────┘

Juan hace clic en [Seleccionar] para María

Sistema ejecuta:
1. MonitorApplication #456 → status: SELECCIONADO
2. Otras aplicaciones → status: NO_SELECCIONADO
3. MonitoringRequest #123 → status: MONITOR_SELECCIONADO
4. Crea Monitoring #789:
   - course: Cálculo Diferencial
   - professor: Juan García
   - assignedMonitor: María López
   - approvalStatus: PENDIENTE_APROBACION
   - originatingRequest: #123
5. MonitoringRequest #123 → status: PENDIENTE_APROBACION
```

**Paso 4: Jefe de Departamento aprueba**
```
Pantalla jefe: "Monitorías Pendientes de Aprobación"
Jefe ve:
┌────────────────────────────────────────────────────┐
│ Cálculo Diferencial                                │
│ Profesor: Juan García                              │
│ Monitor seleccionado: María López (Prom: 4.5)     │
│ Horas: 40 | Presupuesto disponible: 120h          │
│ Justificación: "Hay 80 estudiantes inscritos..."  │
│                                                    │
│ [Aprobar] [Rechazar]                               │
└────────────────────────────────────────────────────┘

Jefe hace clic en [Aprobar]

Sistema actualiza:
- Monitoring #789 → approvalStatus: APROBADA
- MonitoringRequest #123 → status: APROBADA
- Envía email a Juan y María
```

**Paso 5: Monitor comienza a trabajar**
```
Pantalla María: "Mis Monitorías"
María ve:
- ✅ Cálculo Diferencial - Prof. Juan García
- Estado: APROBADA - Puedes comenzar
- [Ver horario] [Registrar actividades]
```

---

### Caso 2: Rechazo por el Jefe

```
...mismo flujo hasta el Paso 4...

Jefe hace clic en [Rechazar]
Escribe comentario: "No hay presupuesto suficiente este mes"

Sistema actualiza:
- Monitoring #789 → approvalStatus: RECHAZADA
- MonitoringRequest #123 → status: RECHAZADA
- Envía email a Juan explicando el rechazo

Juan recibe notificación:
"Tu solicitud de monitoría para Cálculo Diferencial fue rechazada.
Motivo: No hay presupuesto suficiente este mes"
```

---

### Caso 3: Sin Postulantes

```
...Profesor crea convocatoria...
Pasan 7 días, nadie se postula

Sistema automático:
- Envía alerta a Juan: "Tu convocatoria no ha recibido postulantes"
- Opción 1: Juan extiende el plazo
- Opción 2: Juan cancela la convocatoria
- Opción 3: Después de 14 días se cancela automáticamente
```

---

## 🎨 Ventajas de Este Flujo

### Para el Profesor:
✅ Puede crear convocatoria inmediatamente (no espera aprobación previa)
✅ Ve todos los postulantes antes de elegir
✅ Tiene control sobre la selección del monitor
✅ Justifica la necesidad de la monitoría (HU-010 ✓)

### Para el Estudiante:
✅ Ve convocatorias disponibles claramente
✅ Puede postularse a múltiples convocatorias
✅ Sabe si fue seleccionado o no

### Para el Jefe de Departamento:
✅ Revisa "paquetes completos" (monitoría + monitor)
✅ Puede tomar decisiones informadas
✅ Control de presupuesto centralizado
✅ Una sola aprobación por monitoría

### Para el Sistema:
✅ Separación clara de responsabilidades
✅ Estados bien definidos
✅ Trazabilidad completa del proceso
✅ Fácil de auditar

