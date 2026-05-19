# Diseño de Pruebas: `MonitoringConvocatoriaFlowTest`

Este documento describe los casos de prueba diseñados para verificar el flujo completo de convocatorias de monitoría implementado mediante los controladores `MonitoringRequestController` y `MonitorApplicationController`. Los tests correspondientes se encuentran en la clase `MonitoringConvocatoriaFlowTest`.

**Objetivo:**  
Asegurar que el sistema gestione correctamente la creación de convocatorias, postulaciones de estudiantes, selección de monitores, consultas de convocatorias y validaciones asociadas al flujo de monitorías.

**Alcance:**  
Pruebas de integración sobre los endpoints principales relacionados con convocatorias de monitoría y postulaciones, utilizando `MockMvc` y servicios simulados (`@MockBean`) en un entorno `@WebMvcTest`.

Se valida la interacción entre:
- `MonitoringRequest`
- `MonitorApplication`
- `Professor`
- `Monitor`
- `MonitoringRequestService`
- `MonitorApplicationService`

**Estrategia:**  
Pruebas de integración enfocadas en el flujo funcional completo de convocatorias (HU-010), verificando:
- creación de convocatorias,
- postulación de estudiantes,
- selección de monitores,
- consulta de convocatorias,
- validaciones de reglas de negocio,
- y manejo correcto de errores HTTP.

Las pruebas utilizan aserciones sobre:
- estados HTTP,
- respuestas JSON,
- ejecución de servicios,
- y validaciones de contenido.

---

# Casos de Prueba

A continuación, se detallan los casos de prueba diseñados e implementados en `MonitoringConvocatoriaFlowTest`:

---

## 1. Creación de Convocatoria

Estos casos verifican que un profesor pueda crear correctamente una convocatoria de monitoría.

| ID Caso | Nombre del Test (Método) | Descripción | Precondiciones | Pasos | Resultado Esperado | Estado |
| :-- | :-- | :-- | :-- | :-- | :-- | :-- |
| MCF-001 | `testCreateConvocatoria_Success` | Verifica la creación exitosa de una convocatoria de monitoría. | Existe un profesor autenticado con rol `professor`. | 1. Crear DTO de convocatoria. 2. Simular creación mediante `createConvocatoria`. 3. Ejecutar petición POST `/monitoring-request/create`. | El sistema debe retornar `201 Created` y ejecutar correctamente el servicio de creación. | OK |

---

## 2. Postulación a Convocatoria

Estos casos verifican que un estudiante pueda postularse correctamente a una convocatoria.

| ID Caso | Nombre del Test (Método) | Descripción | Precondiciones | Pasos | Resultado Esperado | Estado |
| :-- | :-- | :-- | :-- | :-- | :-- | :-- |
| MCF-002 | `testApplyToConvocatoria_Success` | Verifica el registro exitoso de una postulación a convocatoria. | Existe una convocatoria abierta y un estudiante autenticado con rol `student`. | 1. Crear DTO de postulación. 2. Simular aplicación mediante `applyToConvocatoria`. 3. Ejecutar petición POST `/monitor-application/apply`. | El sistema debe retornar `201 Created` y registrar correctamente la postulación. | OK |

---

## 3. Selección de Monitor

Estos casos verifican que un profesor pueda seleccionar un monitor postulante.

| ID Caso | Nombre del Test (Método) | Descripción | Precondiciones | Pasos | Resultado Esperado | Estado |
| :-- | :-- | :-- | :-- | :-- | :-- | :-- |
| MCF-003 | `testSelectMonitor_Success` | Verifica la selección exitosa de un monitor postulante. | Existe una convocatoria con postulaciones registradas. | 1. Crear request de selección. 2. Ejecutar petición POST `/monitor-application/select`. 3. Validar respuesta. | El sistema debe retornar `200 OK` y un mensaje de confirmación. | OK |

---

## 4. Consulta de Convocatorias Abiertas

Estos casos verifican la obtención de convocatorias abiertas disponibles para estudiantes.

| ID Caso | Nombre del Test (Método) | Descripción | Precondiciones | Pasos | Resultado Esperado | Estado |
| :-- | :-- | :-- | :-- | :-- | :-- | :-- |
| MCF-004 | `testGetOpenConvocatorias_Success` | Verifica la consulta de convocatorias abiertas. | Existen convocatorias activas registradas. | 1. Simular convocatorias abiertas. 2. Ejecutar petición GET `/monitoring-request/open`. | El sistema debe retornar `200 OK` y un arreglo de convocatorias. | OK |

---

## 5. Consulta de Postulaciones por Convocatoria

Estos casos verifican la consulta de postulaciones asociadas a una convocatoria específica.

| ID Caso | Nombre del Test (Método) | Descripción | Precondiciones | Pasos | Resultado Esperado | Estado |
| :-- | :-- | :-- | :-- | :-- | :-- | :-- |
| MCF-005 | `testGetApplicationsByRequest_Success` | Verifica la obtención de postulantes de una convocatoria. | Existe una convocatoria con postulaciones registradas. | 1. Simular lista de postulaciones. 2. Ejecutar petición GET `/monitor-application/request/{requestId}`. | El sistema debe retornar `200 OK` y un arreglo de postulaciones. | OK |

---

## 6. Validación de Justificación Obligatoria

Estos casos verifican que una convocatoria no pueda ser creada sin justificación.

| ID Caso | Nombre del Test (Método) | Descripción | Precondiciones | Pasos | Resultado Esperado | Estado |
| :-- | :-- | :-- | :-- | :-- | :-- | :-- |
| MCF-006 | `testCreateConvocatoria_FailsWithoutJustification` | Verifica la validación de justificación obligatoria. | Existe un profesor autenticado. | 1. Crear DTO sin justificación. 2. Ejecutar petición POST `/monitoring-request/create`. | El sistema debe retornar `400 Bad Request` con mensaje `"La justificación es requerida"`. | OK |

---

## 7. Validación de Carta de Motivación

Estos casos verifican que la carta de motivación cumpla la longitud mínima requerida.

| ID Caso | Nombre del Test (Método) | Descripción | Precondiciones | Pasos | Resultado Esperado | Estado |
| :-- | :-- | :-- | :-- | :-- | :-- | :-- |
| MCF-007 | `testApplyToConvocatoria_FailsWithShortMotivationLetter` | Verifica la validación de longitud mínima de carta de motivación. | Existe una convocatoria activa. | 1. Crear DTO con carta corta. 2. Ejecutar petición POST `/monitor-application/apply`. | El sistema debe retornar `400 Bad Request` con mensaje relacionado a longitud mínima. | OK |

---

## 8. Consulta de Convocatorias del Profesor

Estos casos verifican la obtención de convocatorias asociadas a un profesor.

| ID Caso | Nombre del Test (Método) | Descripción | Precondiciones | Pasos | Resultado Esperado | Estado |
| :-- | :-- | :-- | :-- | :-- | :-- | :-- |
| MCF-008 | `testGetMyRequests_Success` | Verifica la consulta de convocatorias creadas por un profesor. | Existe un profesor con convocatorias registradas. | 1. Simular convocatorias asociadas al profesor. 2. Ejecutar petición GET `/monitoring-request/professor/{professorId}`. | El sistema debe retornar `200 OK` y un arreglo de convocatorias. | OK |

---

## 9. Consulta de Postulaciones del Estudiante

Estos casos verifican la obtención de postulaciones realizadas por un estudiante.

| ID Caso | Nombre del Test (Método) | Descripción | Precondiciones | Pasos | Resultado Esperado | Estado |
| :-- | :-- | :-- | :-- | :-- | :-- | :-- |
| MCF-009 | `testGetMyApplications_Success` | Verifica la consulta de postulaciones realizadas por un monitor. | Existe un monitor con postulaciones registradas. | 1. Simular postulaciones asociadas al monitor. 2. Ejecutar petición GET `/monitor-application/monitor/{monitorId}`. | El sistema debe retornar `200 OK` y un arreglo de postulaciones. | OK |

---

## 10. Cancelación de Convocatoria

Estos casos verifican que un profesor pueda cancelar correctamente una convocatoria activa.

| ID Caso | Nombre del Test (Método) | Descripción | Precondiciones | Pasos | Resultado Esperado | Estado |
| :-- | :-- | :-- | :-- | :-- | :-- | :-- |
| MCF-010 | `testCancelConvocatoria_Success` | Verifica la cancelación exitosa de una convocatoria. | Existe una convocatoria activa perteneciente al profesor. | 1. Enviar petición POST `/monitoring-request/{id}/cancel`. 2. Validar respuesta. | El sistema debe retornar `200 OK` y mensaje `"Convocatoria cancelada exitosamente"`. | OK |

---

# Configuración de Datos de Prueba

Antes de ejecutar cada caso de prueba, se configuran objetos simulados (`MockBean`) para:
- `MonitoringRequestService`
- `MonitorApplicationService`

También se inicializan:
- `MonitoringRequest`
- `MonitorApplication`
- DTOs de convocatorias y postulaciones.

Las peticiones HTTP se ejecutan mediante `MockMvc` utilizando usuarios autenticados con `@WithMockUser`.

---

# Dependencias Utilizadas

Las pruebas integran los siguientes componentes:

| Componente | Tipo |
| :-- | :-- |
| `MonitoringRequestController` | Controlador |
| `MonitorApplicationController` | Controlador |
| `MonitoringRequestService` | Servicio |
| `MonitorApplicationService` | Servicio |
| `MockMvc` | Herramienta de pruebas HTTP |
| `ObjectMapper` | Serialización JSON |

---

# Notas

- Las pruebas utilizan `@WebMvcTest` para cargar únicamente la capa web.
- Se utilizan servicios simulados mediante `@MockBean`.
- La autenticación se simula mediante `@WithMockUser`.
- Las pruebas validan estados HTTP, estructuras JSON y ejecución de servicios.
- Los endpoints probados corresponden al flujo funcional completo de la historia de usuario HU-010.
