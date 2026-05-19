# Diseño de Pruebas: DepartmentHeadController

Este documento describe los casos de prueba diseñados para verificar la funcionalidad del controlador `DepartmentHeadController`, enfocados en la aprobación y rechazo de postulaciones a monitorías, así como en la consulta de postulaciones pendientes. Los tests unitarios correspondientes se encuentran en la clase `ApproveApplicationsTest`.

**Objetivo:**  
Asegurar que `DepartmentHeadController` gestione correctamente las solicitudes de aprobación y rechazo de postulaciones, valide los roles autorizados y permita consultar las postulaciones pendientes de revisión.

**Alcance:**  
Pruebas unitarias del controlador `DepartmentHeadController` utilizando `MockMvc` y Mocks de los servicios (`DepartmentHeadServiceImpl`, `MonitoringMonitorServiceImpl`). No se prueban interacciones reales con base de datos ni lógica interna de persistencia.

**Estrategia:**  
Pruebas de caja negra enfocadas en los endpoints REST del controlador. Se validan respuestas HTTP, mensajes esperados, restricciones de seguridad y llamadas a los servicios correspondientes utilizando Mockito.

---

# Casos de Prueba

A continuación, se detallan los casos de prueba diseñados e implementados en `ApproveApplicationsTest`:

---

## 1. Aprobación de Postulaciones

Estos casos validan el proceso de aprobación de postulaciones por parte del jefe de departamento.

| ID Caso | Nombre del Test (Método) | Descripción | Precondiciones (Mocks) | Pasos | Resultado Esperado | Estado |
| :-- | :-- | :-- | :-- | :-- | :-- | :-- |
| APP-001 | `testApproveApplication_Success` | Verifica la aprobación exitosa de una postulación. | `monitoringMonitorService.approveApplication` ejecuta correctamente sin excepciones. | 1. Crear `ApproveApplicationRequest`. 2. Realizar petición POST a `/department-head/approve`. 3. Validar respuesta HTTP. | El endpoint responde con estado `200 OK` y mensaje `"Postulación aprobada exitosamente"`. | OK |
| APP-002 | `testApproveApplication_AccessDenied_InvalidRole` | Verifica que un usuario con rol inválido no pueda aprobar postulaciones. | Usuario autenticado con rol distinto de `jfedpto`. | 1. Crear request. 2. Realizar petición POST con rol inválido. | El endpoint responde con estado `403 Forbidden` y mensaje de acceso denegado. | OK |
| APP-003 | `testApproveApplication_AlreadyProcessed` | Verifica el manejo de error cuando la postulación ya fue procesada previamente. | `monitoringMonitorService.approveApplication` lanza excepción. | 1. Configurar Mock con excepción. 2. Ejecutar petición POST. | El endpoint responde con estado `400 Bad Request` y mensaje de error correspondiente. | OK |

---

## 2. Rechazo de Postulaciones

Estos casos validan el proceso de rechazo de postulaciones.

| ID Caso | Nombre del Test (Método) | Descripción | Precondiciones (Mocks) | Pasos | Resultado Esperado | Estado |
| :-- | :-- | :-- | :-- | :-- | :-- | :-- |
| APP-004 | `testRejectApplication_Success` | Verifica el rechazo exitoso de una postulación. | `monitoringMonitorService.rejectApplication` ejecuta correctamente sin excepciones. | 1. Crear `ApproveApplicationRequest`. 2. Realizar petición POST a `/department-head/reject`. | El endpoint responde con estado `200 OK` y mensaje `"Postulación rechazada exitosamente"`. | OK |

---

## 3. Consulta de Postulaciones Pendientes

Estos casos validan la obtención de postulaciones pendientes para revisión por parte del jefe de departamento.

| ID Caso | Nombre del Test (Método) | Descripción | Precondiciones (Mocks) | Pasos | Resultado Esperado | Estado |
| :-- | :-- | :-- | :-- | :-- | :-- | :-- |
| APP-005 | `testGetPendingApplications_Success` | Verifica la consulta exitosa de postulaciones pendientes. | `departmentHeadServiceImpl.getPendingApplications` devuelve lista de postulaciones pendientes. | 1. Configurar lista mock de `PendingApplicationDTO`. 2. Ejecutar petición GET a `/department-head/{id}/pending-applications`. | El endpoint responde con estado `200 OK` y retorna un arreglo JSON con la información correcta de las postulaciones pendientes. | OK |

---

# Notas

- Las pruebas utilizan `MockMvc` para simular peticiones HTTP al controlador.
- Se emplea `@WithMockUser` para simular usuarios autenticados con diferentes roles.
- Los casos cubren validaciones de seguridad, respuestas HTTP exitosas y manejo de excepciones.
- Mockito se utiliza para simular el comportamiento de los servicios dependientes y aislar la lógica del controlador.
