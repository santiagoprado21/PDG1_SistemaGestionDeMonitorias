# Diseño de Pruebas: `MonitorEvaluationServiceImpl`

Este documento describe los casos de prueba diseñados para verificar la funcionalidad relacionada con la evaluación de monitores implementada mediante `MonitorEvaluationServiceImpl`. Los tests unitarios correspondientes se encuentran en la clase `MonitorEvaluationServiceTest`.

**Objetivo:**  
Asegurar que `MonitorEvaluationServiceImpl` gestione correctamente la creación, actualización, consulta y confirmación de evaluaciones de monitores, incluyendo validaciones de visibilidad, filtros de búsqueda y persistencia de información.

**Alcance:**  
Pruebas unitarias sobre los métodos principales de `MonitorEvaluationServiceImpl`, utilizando mocks (`Mockito`) para simular el comportamiento de los repositorios y dependencias asociadas. Se valida la interacción entre entidades como `MonitorEvaluation`, `Monitoring`, `Monitor`, `Professor`, `MonitoringMonitor`, `Course`, `Program` y `School`.

**Estrategia:**  
Pruebas unitarias enfocadas en flujos funcionales completos del módulo de evaluación de monitores, verificando:

- creación de evaluaciones,
- actualización de evaluaciones,
- confirmación de evaluaciones,
- obtención de asignaciones filtradas,
- y recuperación de evaluaciones visibles para monitores.

Las pruebas utilizan aserciones (`assertEquals`, `assertTrue`, `assertFalse`, `assertNotNull`) y verificaciones (`verify`) para validar resultados esperados y persistencia de datos.

---

# Casos de Prueba

A continuación, se detallan los casos de prueba diseñados e implementados en `MonitorEvaluationServiceTest`:

---

## 1. Creación Exitosa de Evaluación

Estos casos verifican que el sistema cree correctamente una evaluación de monitor con cálculo automático de puntaje y nivel de desempeño.

| ID Caso | Nombre del Test (Método) | Descripción | Precondiciones | Pasos | Resultado Esperado | Estado |
| :-- | :-- | :-- | :-- | :-- | :-- | :-- |
| MES-001 | `createEvaluation_successful` | Verifica la creación exitosa de una evaluación de monitor. | Existen `Professor`, `Monitor`, `Monitoring` y relación `MonitoringMonitor` válidos. | 1. Construir solicitud de evaluación. 2. Simular entidades requeridas. 3. Ejecutar `createEvaluation`. 4. Validar respuesta y persistencia. | La evaluación debe crearse correctamente con puntaje total 4.5, nivel “EXCELENTE” y persistirse con los comentarios correspondientes. | OK |

---

## 2. Obtención de Asignaciones Filtradas

Estos casos verifican que el sistema filtre correctamente las asignaciones de evaluación mediante parámetros de búsqueda.

| ID Caso | Nombre del Test (Método) | Descripción | Precondiciones | Pasos | Resultado Esperado | Estado |
| :-- | :-- | :-- | :-- | :-- | :-- | :-- |
| MES-002 | `getAssignments_filtersBySearch` | Verifica que las asignaciones se filtren correctamente por nombre del monitor. | Existe un `Professor` con monitorías y evaluaciones registradas. | 1. Crear monitoría y monitor asociado. 2. Registrar evaluación. 3. Ejecutar `getEvaluationAssignmentsForProfessor` con búsqueda “carla”. | El sistema debe retornar únicamente la asignación correspondiente a “Carla Gómez” con estado evaluado y promedio correcto. | OK |

---

## 3. Actualización de Evaluaciones

Estos casos verifican que el sistema permita actualizar evaluaciones y reinicie correctamente el estado de confirmación del monitor.

| ID Caso | Nombre del Test (Método) | Descripción | Precondiciones | Pasos | Resultado Esperado | Estado |
| :-- | :-- | :-- | :-- | :-- | :-- | :-- |
| MES-003 | `updateEvaluation_resetsAcknowledgementAndPersistsChanges` | Verifica que una evaluación actualizada reinicie la confirmación del monitor. | Existe una evaluación previamente confirmada. | 1. Crear evaluación inicial confirmada. 2. Ejecutar `updateEvaluation` con nuevos valores. 3. Validar persistencia y estado. | La evaluación debe actualizarse correctamente, recalcular el promedio, cambiar el nivel de desempeño y marcar la confirmación como falsa. | OK |

---

## 4. Confirmación de Evaluaciones

Estos casos verifican que un monitor pueda confirmar correctamente una evaluación visible.

| ID Caso | Nombre del Test (Método) | Descripción | Precondiciones | Pasos | Resultado Esperado | Estado |
| :-- | :-- | :-- | :-- | :-- | :-- | :-- |
| MES-004 | `acknowledgeEvaluation_marksEvaluationWhenVisible` | Verifica que el monitor pueda confirmar una evaluación visible. | Existe una evaluación visible asociada al monitor. | 1. Crear evaluación visible. 2. Ejecutar `acknowledgeEvaluation`. 3. Validar confirmación. | La evaluación debe marcarse como confirmada y registrar fecha de confirmación. | OK |

---

## 5. Obtención de Evaluaciones Visibles para el Monitor

Estos casos verifican que el sistema solo retorne evaluaciones visibles para el monitor.

| ID Caso | Nombre del Test (Método) | Descripción | Precondiciones | Pasos | Resultado Esperado | Estado |
| :-- | :-- | :-- | :-- | :-- | :-- | :-- |
| MES-005 | `getEvaluationsForMonitor_filtersInvisibleRecords` | Verifica que las evaluaciones ocultas no sean visibles para el monitor. | Existen evaluaciones visibles y ocultas asociadas al monitor. | 1. Crear evaluación visible y otra oculta. 2. Ejecutar `getEvaluationsForMonitor`. 3. Validar resultados. | El sistema debe retornar únicamente las evaluaciones visibles para el monitor. | OK |

---

# Configuración de Datos de Prueba

Antes de ejecutar cada caso de prueba, se realiza la creación o simulación de entidades necesarias mediante mocks y objetos de dominio:

- `Professor`
- `Monitor`
- `Monitoring`
- `MonitoringMonitor`
- `MonitorEvaluation`
- `Course`
- `Program`
- `School`

Las dependencias son simuladas utilizando `Mockito` para garantizar aislamiento entre pruebas unitarias.

---

# Dependencias Utilizadas

Las pruebas integran los siguientes componentes:

| Componente | Tipo |
| :-- | :-- |
| `MonitorEvaluationServiceImpl` | Servicio principal |
| `MonitorEvaluationRepository` | Repositorio |
| `MonitoringRepository` | Repositorio |
| `MonitoringMonitorRepository` | Repositorio |
| `ProfessorRepository` | Repositorio |
| `MonitorRepository` | Repositorio |

---

# Notas

- Las pruebas están implementadas utilizando `JUnit 5` y `MockitoExtension`.
- Se utilizan mocks para aislar completamente la lógica del servicio.
- Los puntajes y niveles de desempeño son calculados automáticamente mediante la lógica de negocio de `MonitorEvaluation`.
- Las evaluaciones visibles y ocultas son filtradas mediante la propiedad `visibleToMonitor`.
- Las actualizaciones reinician automáticamente el estado de confirmación del monitor.
- Los casos implementados validan los flujos principales del módulo de evaluación de monitores.
