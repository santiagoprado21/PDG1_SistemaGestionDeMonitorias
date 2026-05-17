# Diseño de Pruebas: `MonitorEvaluationControllerTest`

Este documento describe los casos de prueba diseñados para verificar la funcionalidad relacionada con la gestión de evaluaciones de monitores implementada mediante `MonitorEvaluationController`. Los tests correspondientes se encuentran en la clase `MonitorEvaluationControllerTest`.

**Objetivo:**  
Asegurar que el controlador de evaluaciones de monitores gestione correctamente la creación, actualización, consulta y confirmación de evaluaciones, así como el manejo adecuado de respuestas HTTP y errores asociados.

**Alcance:**  
Pruebas unitarias del controlador REST utilizando `@WebMvcTest`, `MockMvc` y servicios simulados mediante Mockito. Se validan endpoints relacionados con:
- creación de evaluaciones,
- actualización de evaluaciones,
- consulta de asignaciones,
- confirmación de evaluaciones,
- y recuperación de evaluaciones de monitores.

**Estrategia:**  
Pruebas enfocadas en validar:
- códigos HTTP correctos,
- serialización/deserialización JSON,
- respuestas exitosas,
- manejo de errores,
- y estructura de datos retornados por el controlador.

Las pruebas utilizan aserciones HTTP (`status`, `jsonPath`) y simulación de dependencias mediante `Mockito`.

---

# Casos de Prueba

A continuación, se detallan los casos de prueba diseñados e implementados en `MonitorEvaluationControllerTest`:

---

## 1. Creación Exitosa de Evaluación

Estos casos verifican que una evaluación pueda registrarse correctamente.

| ID Caso | Nombre del Test (Método) | Descripción | Precondiciones | Pasos | Resultado Esperado | Estado |
| :-- | :-- | :-- | :-- | :-- | :-- | :-- |
| MEC-001 | `createEvaluation_returnsCreatedResponse` | Verifica la creación exitosa de una evaluación de monitor. | Existe un profesor, monitor y monitoría válidos. | 1. Construir payload de evaluación. 2. Simular respuesta del servicio. 3. Ejecutar petición POST. | El sistema debe responder con `201 Created` y retornar la evaluación creada con información del monitor y nivel de desempeño. | OK |

---

## 2. Prevención de Evaluaciones Duplicadas

Estos casos verifican que el sistema no permita registrar evaluaciones duplicadas.

| ID Caso | Nombre del Test (Método) | Descripción | Precondiciones | Pasos | Resultado Esperado | Estado |
| :-- | :-- | :-- | :-- | :-- | :-- | :-- |
| MEC-002 | `createEvaluation_conflictWhenAlreadyExists` | Verifica que el sistema rechace evaluaciones duplicadas. | Ya existe una evaluación registrada para el monitor y monitoría indicados. | 1. Construir payload duplicado. 2. Simular excepción del servicio. 3. Ejecutar petición POST. | El sistema debe responder con `409 Conflict` y retornar mensaje indicando que ya existe una evaluación. | OK |

---

## 3. Actualización Exitosa de Evaluación

Estos casos verifican que una evaluación pueda actualizarse correctamente.

| ID Caso | Nombre del Test (Método) | Descripción | Precondiciones | Pasos | Resultado Esperado | Estado |
| :-- | :-- | :-- | :-- | :-- | :-- | :-- |
| MEC-003 | `updateEvaluation_returnsOk` | Verifica la actualización exitosa de una evaluación existente. | Existe una evaluación previamente registrada. | 1. Construir payload actualizado. 2. Simular actualización en el servicio. 3. Ejecutar petición PUT. | El sistema debe responder con `200 OK` y retornar la evaluación actualizada. | OK |

---

## 4. Obtención de Asignaciones de Evaluación

Estos casos verifican la consulta de asignaciones pendientes o realizadas para un profesor.

| ID Caso | Nombre del Test (Método) | Descripción | Precondiciones | Pasos | Resultado Esperado | Estado |
| :-- | :-- | :-- | :-- | :-- | :-- | :-- |
| MEC-004 | `getAssignments_returnsList` | Verifica la consulta de asignaciones de evaluación asociadas a un profesor. | Existen asignaciones registradas para el profesor. | 1. Simular lista de asignaciones. 2. Ejecutar petición GET con parámetro de búsqueda. 3. Validar respuesta. | El sistema debe responder con `200 OK` y retornar la lista de asignaciones correctamente. | OK |

---

## 5. Restricción de Confirmación de Evaluaciones

Estos casos verifican que un monitor no pueda confirmar evaluaciones que no le pertenecen.

| ID Caso | Nombre del Test (Método) | Descripción | Precondiciones | Pasos | Resultado Esperado | Estado |
| :-- | :-- | :-- | :-- | :-- | :-- | :-- |
| MEC-005 | `acknowledgeEvaluation_forbiddenWhenDoesNotBelong` | Verifica el manejo de acceso no autorizado al confirmar una evaluación. | La evaluación no pertenece al monitor indicado. | 1. Ejecutar petición PATCH de confirmación. 2. Simular excepción del servicio. | El sistema debe responder con `403 Forbidden` y retornar mensaje de error correspondiente. | OK |

---

## 6. Consulta de Evaluación Inexistente

Estos casos verifican el comportamiento cuando se consulta una evaluación inexistente.

| ID Caso | Nombre del Test (Método) | Descripción | Precondiciones | Pasos | Resultado Esperado | Estado |
| :-- | :-- | :-- | :-- | :-- | :-- | :-- |
| MEC-006 | `getEvaluation_notFoundWhenMissing` | Verifica el manejo de consultas sobre evaluaciones inexistentes. | No existe una evaluación con el identificador consultado. | 1. Ejecutar petición GET con ID inexistente. 2. Validar respuesta. | El sistema debe responder con `404 Not Found` y mensaje indicando que la evaluación no fue encontrada. | OK |

---

## 7. Obtención de Evaluaciones del Monitor

Estos casos verifican la consulta de evaluaciones asociadas a un monitor.

| ID Caso | Nombre del Test (Método) | Descripción | Precondiciones | Pasos | Resultado Esperado | Estado |
| :-- | :-- | :-- | :-- | :-- | :-- | :-- |
| MEC-007 | `getEvaluationsForMonitor_returnsData` | Verifica la recuperación de evaluaciones asociadas a un monitor. | Existen evaluaciones registradas para el monitor. | 1. Simular lista de evaluaciones. 2. Ejecutar petición GET del monitor. 3. Validar respuesta. | El sistema debe responder con `200 OK` y retornar las evaluaciones del monitor correctamente. | OK |

---

# Configuración de Datos de Prueba

Las pruebas utilizan dependencias simuladas mediante Mockito para aislar el comportamiento del controlador:

- `MonitorEvaluationService`
- `JwtAuthenticationFilter`

El entorno de pruebas utiliza `MockMvc` para ejecutar solicitudes HTTP simuladas y validar respuestas REST.

---

# Dependencias Utilizadas

Las pruebas integran los siguientes componentes:

| Componente | Tipo |
| :-- | :-- |
| `MonitorEvaluationController` | Controlador principal |
| `MonitorEvaluationService` | Servicio mockeado |
| `MockMvc` | Cliente HTTP de pruebas |
| `ObjectMapper` | Serialización JSON |
| `JwtAuthenticationFilter` | Filtro de autenticación mockeado |

---

# Notas

- Las pruebas utilizan `@WebMvcTest` para cargar únicamente el contexto web necesario.
- Los filtros de seguridad son deshabilitados mediante `@AutoConfigureMockMvc(addFilters = false)`.
- Se utilizan respuestas simuladas con Mockito para controlar escenarios exitosos y de error.
- Las validaciones principales se realizan mediante `jsonPath` y códigos HTTP.
- Los casos implementados cubren el flujo funcional de creación, consulta y actualización de evaluaciones de monitores.
