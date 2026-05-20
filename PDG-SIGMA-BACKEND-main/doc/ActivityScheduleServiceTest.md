# Diseño de Pruebas: `ActivityScheduleServiceImpl`

Este documento describe los casos de prueba diseñados para verificar la funcionalidad de `ActivityScheduleServiceImpl`, enfocándose en la creación de actividades programadas, validación de conflictos de horario y gestión de planes de actividades para monitores. Los tests unitarios correspondientes se encuentran en la clase `ActivityScheduleServiceTest`.

**Objetivo:**  
Asegurar que `ActivityScheduleServiceImpl` gestione correctamente:
- la creación de actividades con y sin horario,
- la validación de conflictos de agenda,
- la generación de planes de actividades,
- y la recuperación de planes asociados a monitores.

**Alcance:**  
Pruebas unitarias de los métodos públicos de `ActivityScheduleServiceImpl`, utilizando Mocks para los repositorios (`ActivityRepository`, `MonitoringRepository`, `MonitorRepository`, `ProfessorRepository`, `RubricRepository`).

No se prueban interacciones reales con base de datos.

**Estrategia:**  
Pruebas de caja blanca y caja negra orientadas a validar:
- reglas de negocio,
- conflictos de horarios,
- consolidación de actividades,
- manejo de excepciones,
- y comportamiento de la lógica de planificación académica.

Las pruebas utilizan Mockito para simulación de dependencias y JUnit para validación de resultados esperados.

---

# Casos de Prueba

A continuación, se detallan los casos de prueba diseñados e implementados en `ActivityScheduleServiceTest`:

---

## 1. Creación de Actividades

Estos casos verifican la creación de actividades académicas con y sin horarios específicos.

| ID Caso | Nombre del Test (Método) | Descripción | Precondiciones (Mocks) | Pasos | Resultado Esperado | Estado |
| :-- | :-- | :-- | :-- | :-- | :-- | :-- |
| ASS-001 | `testCreateActivityWithoutSchedule` | Verifica la creación de una actividad sin horario específico. | `monitoringRepository.findById`, `professorRepository.findById` y `monitorRepository.findById` retornan entidades válidas. `activityRepository.save` persiste la actividad. | 1. Crear `ActivityScheduleDTO` sin horario. 2. Ejecutar `saveActivityWithSchedule`. 3. Validar resultado. | La actividad debe crearse correctamente y guardarse en el repositorio. | OK |
| ASS-002 | `testCreateActivityWithSchedule` | Verifica la creación de una actividad con horario específico válido. | Repositorios retornan entidades válidas. `activityRepository.findScheduleConflicts` retorna lista vacía. | 1. Crear actividad con hora inicio y fin. 2. Ejecutar `saveActivityWithSchedule`. 3. Verificar datos retornados. | La actividad debe guardarse correctamente sin conflictos. | OK |

---

## 2. Validación de Conflictos de Horario

Estos casos verifican el manejo de conflictos entre actividades programadas.

| ID Caso | Nombre del Test (Método) | Descripción | Precondiciones (Mocks) | Pasos | Resultado Esperado | Estado |
| :-- | :-- | :-- | :-- | :-- | :-- | :-- |
| ASS-003 | `testDetectScheduleConflicts` | Verifica que el sistema detecte actividades con horarios solapados. | `activityRepository.findScheduleConflicts` retorna actividades en conflicto. | 1. Crear actividad con horario. 2. Ejecutar `validateScheduleConflicts`. | Debe retornarse una lista con conflictos detectados. | OK |
| ASS-004 | `testInvalidTimeRange` | Verifica que no se permitan horarios donde la hora final sea menor que la inicial. | `monitorRepository.findById` retorna monitor válido. | 1. Crear actividad con rango horario inválido. 2. Ejecutar validación. | Debe lanzarse una excepción indicando error en el rango horario. | OK |
| ASS-005 | `testRejectActivityWithConflict` | Verifica que el sistema rechace actividades con conflictos de horario. | `activityRepository.findScheduleConflicts` retorna conflictos existentes. | 1. Crear actividad con horario conflictivo. 2. Ejecutar guardado. | Debe lanzarse una excepción y no ejecutarse `save`. | OK |

---

## 3. Generación de Planes de Actividades

Estos casos verifican la generación y consolidación de planes de actividades asociados a monitorías.

| ID Caso | Nombre del Test (Método) | Descripción | Precondiciones (Mocks) | Pasos | Resultado Esperado | Estado |
| :-- | :-- | :-- | :-- | :-- | :-- | :-- |
| ASS-006 | `testGetActivityPlan` | Verifica la obtención de un plan de actividades para una monitoría. | `monitoringRepository.findById` retorna monitoría válida. `activityRepository.findByMonitoringOrderedBySchedule` retorna actividades. | 1. Obtener plan mediante `getActivityPlan`. 2. Validar métricas calculadas. | El plan debe contener actividades, horas totales y conteos correctos de estados. | OK |

---

# HU-017: Vista Monitor - Plan de Actividades

Estos casos verifican la funcionalidad relacionada con la visualización de planes de actividades para monitores.

---

## 4. Obtención de Planes para Monitores

| ID Caso | Nombre del Test (Método) | Descripción | Precondiciones (Mocks) | Pasos | Resultado Esperado | Estado |
| :-- | :-- | :-- | :-- | :-- | :-- | :-- |
| ASS-007 | `testGetMonitorActivityPlans_NoMonitorings` | Verifica que un monitor sin monitorías asignadas obtenga una lista vacía. | `monitoringRepository.findMonitoringsDirectlyAssignedToMonitorWithStatusSelected` retorna lista vacía. | 1. Obtener planes del monitor. | Debe retornarse lista vacía sin errores. | OK |
| ASS-008 | `testGetMonitorActivityPlans_MultipleMonitorings` | Verifica la obtención de múltiples planes de actividades para diferentes monitorías. | Repositorios retornan múltiples monitorías y actividades asociadas. | 1. Obtener planes del monitor. 2. Validar contenido de cada plan. | Deben retornarse múltiples planes con métricas correctas. | OK |
| ASS-009 | `testGetMonitorActivityPlans_NoDuplicates` | Verifica que no existan planes duplicados cuando una monitoría aparece desde distintas fuentes. | Monitorías y actividades contienen referencias repetidas. | 1. Obtener planes. 2. Verificar cantidad retornada. | Debe retornarse un único plan por monitoría. | OK |
| ASS-010 | `testGetMonitorActivityPlans_ActivitiesWithoutMonitoring` | Verifica que actividades sin monitoría asociada no se incluyan en los planes. | `activityRepository.findByMonitor` retorna actividad sin monitoría. | 1. Obtener planes del monitor. | Las actividades huérfanas no deben incluirse en el resultado. | OK |

---

## 5. Manejo de Errores

Estos casos verifican el comportamiento del servicio ante errores o entidades inexistentes.

| ID Caso | Nombre del Test (Método) | Descripción | Precondiciones (Mocks) | Pasos | Resultado Esperado | Estado |
| :-- | :-- | :-- | :-- | :-- | :-- | :-- |
| ASS-011 | `testGetMonitorActivityPlans_MonitorNotFound` | Verifica que se lance excepción cuando el monitor no existe. | `monitorRepository.findById` retorna `Optional.empty()`. | 1. Solicitar planes con monitor inexistente. | Debe lanzarse excepción indicando que el monitor no fue encontrado. | OK |
| ASS-012 | `testGetMonitorActivityPlans_HandleIndividualPlanErrors` | Verifica que errores al obtener un plan individual no detengan el procesamiento general. | Una monitoría falla y otra retorna correctamente. | 1. Obtener planes del monitor. 2. Validar continuidad del flujo. | El sistema debe continuar procesando las monitorías válidas sin interrumpir el flujo completo. | OK |

---

# Configuración de Datos de Prueba

Antes de ejecutar cada caso de prueba, se inicializan entidades simuladas mediante `setUp()`:

- `Professor`
- `Monitor`
- `Monitoring`
- `Program`
- `Course`

Estas entidades son utilizadas para simular escenarios completos de planificación académica.

---

# Dependencias Utilizadas

Las pruebas utilizan los siguientes componentes simulados mediante Mockito:

| Componente | Tipo |
| :-- | :-- |
| `ActivityRepository` | Repository Mock |
| `MonitoringRepository` | Repository Mock |
| `MonitorRepository` | Repository Mock |
| `ProfessorRepository` | Repository Mock |
| `RubricRepository` | Repository Mock |

---

# Notas

- Las pruebas utilizan `MockitoAnnotations.openMocks(this)` para inicializar automáticamente los mocks.
- La validación de conflictos depende del método `findScheduleConflicts`.
- Los cálculos de métricas incluyen:
  - total de actividades,
  - actividades pendientes,
  - actividades completadas,
  - horas acumuladas.
- Los casos HU-017 validan escenarios de agregación de monitorías y actividades sin duplicados.
- Se verifica explícitamente el uso o no uso de métodos del repositorio mediante `verify()` y `never()`.
