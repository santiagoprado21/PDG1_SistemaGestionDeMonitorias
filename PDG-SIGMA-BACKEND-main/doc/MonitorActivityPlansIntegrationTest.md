# Diseño de Pruebas: `MonitorActivityPlansIntegrationTest`

Este documento describe los casos de prueba diseñados para verificar la funcionalidad relacionada con la visualización de planes de actividades del monitor implementados mediante el endpoint `/api/activity-schedule/monitor/{monitorId}/all-plans`. Los tests de integración correspondientes se encuentran en la clase `MonitorActivityPlansIntegrationTest`.

**Objetivo:**  
Asegurar que el sistema permita a los monitores consultar correctamente sus planes de actividades, incluyendo estadísticas, actividades asociadas y validaciones de comportamiento para distintos escenarios funcionales de la HU-017.

**Alcance:**  
Pruebas de integración sobre el endpoint de consulta de planes de actividades para monitores utilizando `@SpringBootTest`, `MockMvc` y perfil `test`. Se valida la interacción entre entidades como `Monitor`, `Monitoring`, `Activity`, `Professor`, `Course`, `Program` y `School`.

**Estrategia:**  
Pruebas de integración orientadas a flujos funcionales completos de la HU-017, verificando:
- consulta de planes de actividades,
- cálculo de estadísticas,
- inclusión de información detallada,
- combinación de actividades desde múltiples fuentes,
- y prevención de duplicados.

Las pruebas utilizan aserciones HTTP (`status`, `jsonPath`) para validar respuestas REST y consistencia de datos.

---

# Casos de Prueba

A continuación, se detallan los casos de prueba diseñados e implementados en `MonitorActivityPlansIntegrationTest`:

---

## 1. Obtención Exitosa de Planes de Actividades

Estos casos verifican que un monitor pueda consultar correctamente sus planes de actividades registrados.

| ID Caso | Nombre del Test (Método) | Descripción | Precondiciones | Pasos | Resultado Esperado | Estado |
| :-- | :-- | :-- | :-- | :-- | :-- | :-- |
| MAP-001 | `testGetMonitorActivityPlans_Success` | Verifica que el sistema retorne correctamente los planes de actividades asociados al monitor. | Existen `Monitor`, `Professor`, `Monitoring` y actividades registradas asociadas al monitor. | 1. Crear actividades asociadas a monitorías. 2. Ejecutar petición GET al endpoint. 3. Validar respuesta. | El sistema debe responder con `200 OK` y retornar al menos un plan con información de curso y profesor asociada. | OK |

---

## 2. Inclusión Correcta de Estadísticas

Estos casos verifican que las estadísticas de actividades sean calculadas correctamente.

| ID Caso | Nombre del Test (Método) | Descripción | Precondiciones | Pasos | Resultado Esperado | Estado |
| :-- | :-- | :-- | :-- | :-- | :-- | :-- |
| MAP-002 | `testGetMonitorActivityPlans_IncludesCorrectStatistics` | Verifica el cálculo correcto de actividades pendientes, completadas y horas totales. | Existen múltiples actividades asociadas a una monitoría con distintos estados. | 1. Crear actividades con estados `PENDIENTE`, `COMPLETADO` y `COMPLETADOT`. 2. Ejecutar consulta del plan. 3. Validar estadísticas retornadas. | El sistema debe retornar estadísticas correctas de total de actividades, pendientes, completadas y horas acumuladas. | OK |

---

## 3. Consulta Vacía para Monitor sin Actividades

Estos casos verifican el comportamiento cuando un monitor no posee actividades registradas.

| ID Caso | Nombre del Test (Método) | Descripción | Precondiciones | Pasos | Resultado Esperado | Estado |
| :-- | :-- | :-- | :-- | :-- | :-- | :-- |
| MAP-003 | `testGetMonitorActivityPlans_EmptyForMonitorWithoutActivities` | Verifica que el sistema retorne una lista vacía cuando el monitor no tiene actividades. | Existe un `Monitor` válido sin actividades asociadas. | 1. Ejecutar petición GET del monitor sin actividades. 2. Validar respuesta. | El sistema debe responder `200 OK` y retornar una lista vacía. | OK |

---

## 4. Consulta para Monitor Inexistente

Estos casos verifican el manejo de errores cuando el monitor consultado no existe.

| ID Caso | Nombre del Test (Método) | Descripción | Precondiciones | Pasos | Resultado Esperado | Estado |
| :-- | :-- | :-- | :-- | :-- | :-- | :-- |
| MAP-004 | `testGetMonitorActivityPlans_NonExistentMonitor` | Verifica el manejo de error para un monitor inexistente. | No existe un monitor con el identificador consultado. | 1. Ejecutar petición GET con un monitor inexistente. 2. Validar respuesta de error. | El sistema debe responder con `500 Internal Server Error` y mensaje indicando error. | OK |

---

## 5. Inclusión Completa de Información de Actividades

Estos casos verifican que las actividades retornadas incluyan toda la información requerida.

| ID Caso | Nombre del Test (Método) | Descripción | Precondiciones | Pasos | Resultado Esperado | Estado |
| :-- | :-- | :-- | :-- | :-- | :-- | :-- |
| MAP-005 | `testGetMonitorActivityPlans_IncludesCompleteActivityInfo` | Verifica que cada actividad incluya nombre, descripción, categoría, prioridad y estado. | Existe una actividad registrada con información detallada. | 1. Crear actividad con descripción, categoría y prioridad. 2. Ejecutar consulta. 3. Validar campos retornados. | El sistema debe retornar correctamente todos los atributos de la actividad. | OK |

---

## 6. Combinación de Actividades Directas y Asignadas

Estos casos verifican que el sistema combine actividades provenientes de múltiples fuentes.

| ID Caso | Nombre del Test (Método) | Descripción | Precondiciones | Pasos | Resultado Esperado | Estado |
| :-- | :-- | :-- | :-- | :-- | :-- | :-- |
| MAP-006 | `testGetMonitorActivityPlans_CombinesAssignedAndDirectActivities` | Verifica que el sistema combine monitorías asignadas y actividades directamente asociadas al monitor. | Existe un monitor con actividades directas y monitorías asociadas. | 1. Crear actividad asignada directamente al monitor. 2. Ejecutar consulta del plan. 3. Validar inclusión de actividad directa. | El sistema debe retornar correctamente actividades provenientes de ambas fuentes. | OK |

---

## 7. Prevención de Duplicados

Estos casos verifican que el sistema no retorne monitorías duplicadas cuando provienen de múltiples relaciones.

| ID Caso | Nombre del Test (Método) | Descripción | Precondiciones | Pasos | Resultado Esperado | Estado |
| :-- | :-- | :-- | :-- | :-- | :-- | :-- |
| MAP-007 | `testGetMonitorActivityPlans_NoDuplicates` | Verifica que no existan planes duplicados en la respuesta final. | El monitor aparece tanto en `monitoringMonitors` como en actividades directas. | 1. Crear actividades relacionadas desde ambas fuentes. 2. Ejecutar consulta. 3. Validar cantidad de planes retornados. | El sistema no debe generar duplicados en la lista de planes retornados. | OK |

---

# Configuración de Datos de Prueba

Antes de ejecutar cada caso de prueba, se realiza la creación automática de entidades necesarias mediante el método `setUp()`:

- `Professor`
- `Monitor`
- `School`
- `Program`
- `Course`
- `Monitoring`
- `MonitoringMonitor`

Estas entidades son persistidas utilizando los repositorios configurados en el entorno de pruebas para garantizar independencia y aislamiento entre tests.

---

# Dependencias Utilizadas

Las pruebas integran los siguientes componentes:

| Componente | Tipo |
| :-- | :-- |
| `MockMvc` | Cliente HTTP de pruebas |
| `MonitorRepository` | Repositorio |
| `MonitoringRepository` | Repositorio |
| `ActivityRepository` | Repositorio |
| `ProfessorRepository` | Repositorio |
| `CourseRepository` | Repositorio |
| `ProgramRepository` | Repositorio |
| `SchoolRepository` | Repositorio |

---

# Notas

- Las pruebas están configuradas con `@Transactional`, por lo que los datos generados se revierten automáticamente al finalizar cada ejecución.
- Se utiliza `MockMvc` para validar respuestas HTTP y estructuras JSON.
- El perfil `test` es activado mediante `@ActiveProfiles("test")`.
- Las pruebas utilizan autenticación simulada con `@WithMockUser(roles = "MONITOR")`.
- Los casos implementados validan el flujo funcional completo de la historia de usuario HU-017.
