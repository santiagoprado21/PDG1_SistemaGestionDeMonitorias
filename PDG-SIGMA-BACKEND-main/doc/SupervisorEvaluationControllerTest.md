# Diseño de Pruebas: `SupervisorEvaluationController`

Este documento describe los casos de prueba diseñados para verificar la funcionalidad relacionada con la gestión de evaluaciones de supervisión implementadas mediante `SupervisorEvaluationController`. Los tests correspondientes se encuentran en la clase `SupervisorEvaluationControllerTest`.

**Objetivo:**  
Asegurar que `SupervisorEvaluationController` gestione correctamente la creación de evaluaciones, la consulta de asignaciones para monitores y la recuperación de evaluaciones para coordinadores, validando además restricciones de autorización y respuestas HTTP esperadas.

**Alcance:**  
Pruebas de controlador utilizando `@WebMvcTest`, `MockMvc` y mocks de servicios (`SupervisorEvaluationService`). Se valida el comportamiento de los endpoints REST asociados a evaluaciones de supervisión en un entorno controlado de pruebas.

**Estrategia:**  
Pruebas de integración ligera enfocadas en:
- creación de evaluaciones,
- consulta de asignaciones de evaluación,
- validación de permisos,
- y recuperación de evaluaciones para coordinadores.

Las pruebas utilizan aserciones sobre:
- códigos HTTP,
- estructura JSON,
- contenido de respuestas,
- y validación de roles.

---

# Casos de Prueba

A continuación, se detallan los casos de prueba diseñados e implementados en `SupervisorEvaluationControllerTest`:

---

## 1. Creación de Evaluaciones

Estos casos verifican que el sistema permita registrar correctamente evaluaciones de supervisión realizadas por monitores.

| ID Caso | Nombre del Test (Método) | Descripción | Precondiciones | Pasos | Resultado Esperado | Estado |
| :-- | :-- | :-- | :-- | :-- | :-- | :-- |
| SEC-001 | `createEvaluation_returnsCreatedResponse` | Verifica la creación exitosa de una evaluación de supervisión. | Existe un monitor válido identificado como `MON-10`. | 1. Construir `SupervisorEvaluationRequest` con puntajes válidos. 2. Simular respuesta del servicio. 3. Ejecutar petición `POST /supervisor-evaluations`. | El sistema debe retornar HTTP 201 y un JSON con `evaluationId = 80` y `performanceLevel = "EXCELENTE"`. | OK |

---

## 2. Consulta de Asignaciones de Evaluación

Estos casos verifican que un monitor pueda consultar correctamente las evaluaciones pendientes o asignadas.

| ID Caso | Nombre del Test (Método) | Descripción | Precondiciones | Pasos | Resultado Esperado | Estado |
| :-- | :-- | :-- | :-- | :-- | :-- | :-- |
| SEC-002 | `getAssignments_returnsList` | Verifica la consulta exitosa de asignaciones de evaluación para un monitor. | Existe un monitor con identificador `MON-10` y al menos una evaluación asignada. | 1. Simular respuesta del servicio con lista de asignaciones. 2. Ejecutar petición `GET /supervisor-evaluations/monitor/{id}/assignments`. | El sistema debe retornar HTTP 200 y una lista JSON con una asignación en estado `PENDIENTE`. | OK |

---

## 3. Validación de Permisos para Coordinador

Estos casos verifican que solo usuarios autorizados puedan acceder al listado global de evaluaciones.

| ID Caso | Nombre del Test (Método) | Descripción | Precondiciones | Pasos | Resultado Esperado | Estado |
| :-- | :-- | :-- | :-- | :-- | :-- | :-- |
| SEC-003 | `getEvaluationsForCoordinator_forbiddenWhenRoleNotCoordinator` | Verifica que usuarios sin permisos de coordinador no puedan acceder a las evaluaciones globales. | El usuario posee rol `monitor`. | 1. Ejecutar petición `GET /supervisor-evaluations/coordinator`. 2. Enviar atributo `role = monitor`. | El sistema debe retornar HTTP 403 con mensaje `"No está autorizado"`. | OK |

---

## 4. Consulta de Evaluaciones para Coordinador

Estos casos verifican que un coordinador autorizado pueda consultar correctamente las evaluaciones registradas.

| ID Caso | Nombre del Test (Método) | Descripción | Precondiciones | Pasos | Resultado Esperado | Estado |
| :-- | :-- | :-- | :-- | :-- | :-- | :-- |
| SEC-004 | `getEvaluationsForCoordinator_returnsData` | Verifica la consulta exitosa de evaluaciones para coordinadores. | El usuario posee rol autorizado (`jfedpto`). | 1. Simular respuesta del servicio con evaluaciones registradas. 2. Ejecutar petición `GET /supervisor-evaluations/coordinator`. | El sistema debe retornar HTTP 200 y una lista JSON con evaluaciones registradas. | OK |

---

# Configuración de Datos de Prueba

Antes de ejecutar cada caso de prueba, se configuran los siguientes componentes:

- `MockMvc`
- `ObjectMapper`
- `SupervisorEvaluationService` (mock)
- `JwtAuthenticationFilter` (mock)

Las respuestas del servicio son simuladas mediante `Mockito.when(...)` para garantizar independencia y aislamiento entre pruebas.

---

# Dependencias Utilizadas

Las pruebas integran los siguientes componentes:

| Componente | Tipo |
| :-- | :-- |
| `SupervisorEvaluationController` | Controlador principal |
| `SupervisorEvaluationService` | Servicio mockeado |
| `MockMvc` | Framework de pruebas HTTP |
| `ObjectMapper` | Serialización JSON |
| `JwtAuthenticationFilter` | Filtro de autenticación mockeado |

---

# Notas

- Las pruebas utilizan `@WebMvcTest` para cargar únicamente el contexto web necesario.
- La seguridad automática se encuentra deshabilitada mediante `excludeAutoConfiguration`.
- Los filtros de seguridad son omitidos utilizando `@AutoConfigureMockMvc(addFilters = false)`.
- Las respuestas JSON se validan mediante `jsonPath`.
- Los casos implementados corresponden al flujo funcional relacionado con evaluaciones de supervisión académica.
