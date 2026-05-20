# Diseño de Pruebas: `NotificationController`

Este documento describe los casos de prueba diseñados para verificar la funcionalidad relacionada con la gestión de notificaciones implementada mediante `NotificationController`. Los tests correspondientes se encuentran en la clase `NotificationControllerTest`.

**Objetivo:**  
Asegurar que `NotificationController` gestione correctamente la consulta, conteo, actualización y configuración de notificaciones para profesores dentro del sistema.

**Alcance:**  
Pruebas web utilizando `@WebMvcTest` sobre los endpoints principales del controlador `NotificationController`, validando la interacción con `NotificationService` y `NotificationPreferenceService`.

**Estrategia:**  
Pruebas de integración web enfocadas en la funcionalidad de notificaciones, verificando:
- consulta de notificaciones no leídas,
- conteo de notificaciones pendientes,
- marcado de notificaciones como leídas,
- actualización de preferencias de notificación,
- y validación de respuestas HTTP y contenido JSON.

Las pruebas utilizan `MockMvc`, `Mockito` y aserciones (`status`, `jsonPath`, `content`) para validar el comportamiento esperado de los endpoints.

---

# Casos de Prueba

A continuación, se detallan los casos de prueba diseñados e implementados en `NotificationControllerTest`:

---

## 1. Consulta de Notificaciones No Leídas

Estos casos verifican que el sistema retorne correctamente las notificaciones pendientes de un profesor.

| ID Caso | Nombre del Test (Método) | Descripción | Precondiciones | Pasos | Resultado Esperado | Estado |
| :-- | :-- | :-- | :-- | :-- | :-- | :-- |
| NCT-001 | `getUnread_returnsList` | Verifica que el endpoint retorne la lista de notificaciones no leídas de un profesor. | Existe al menos una notificación pendiente asociada al profesor. | 1. Configurar mock del servicio `getUnreadForProfessor`. 2. Realizar petición GET a `/notifications/unread/{professorId}`. 3. Validar respuesta HTTP y contenido JSON. | El sistema debe responder con estado `200 OK` y retornar una lista con las notificaciones pendientes del profesor. | OK |

---

## 2. Consulta de Cantidad de Notificaciones

Estos casos verifican que el sistema retorne correctamente el número de notificaciones no leídas.

| ID Caso | Nombre del Test (Método) | Descripción | Precondiciones | Pasos | Resultado Esperado | Estado |
| :-- | :-- | :-- | :-- | :-- | :-- | :-- |
| NCT-002 | `getCount_returnsNumber` | Verifica que el sistema retorne el número de notificaciones no leídas. | Existen notificaciones pendientes asociadas al profesor. | 1. Configurar mock del servicio `getUnreadCount`. 2. Realizar petición GET a `/notifications/count/{professorId}`. 3. Validar respuesta. | El sistema debe responder con estado `200 OK` y retornar el número correcto de notificaciones pendientes. | OK |

---

## 3. Marcado de Notificación Individual como Leída

Estos casos verifican que una notificación específica pueda marcarse como leída.

| ID Caso | Nombre del Test (Método) | Descripción | Precondiciones | Pasos | Resultado Esperado | Estado |
| :-- | :-- | :-- | :-- | :-- | :-- | :-- |
| NCT-003 | `markOne_marksAsRead` | Verifica que el sistema permita marcar una notificación individual como leída. | Existe una notificación válida registrada en el sistema. | 1. Realizar petición PUT a `/notifications/{id}/read`. 2. Validar respuesta HTTP. 3. Verificar invocación del servicio `markAsRead`. | El sistema debe responder con estado `200 OK` y ejecutar correctamente el marcado de la notificación. | OK |

---

## 4. Marcado de Todas las Notificaciones como Leídas

Estos casos verifican que todas las notificaciones de un profesor puedan marcarse como leídas.

| ID Caso | Nombre del Test (Método) | Descripción | Precondiciones | Pasos | Resultado Esperado | Estado |
| :-- | :-- | :-- | :-- | :-- | :-- | :-- |
| NCT-004 | `markAll_marksAllAsRead` | Verifica que el sistema permita marcar todas las notificaciones de un profesor como leídas. | Existen notificaciones pendientes asociadas al profesor. | 1. Realizar petición PUT a `/notifications/read-all/{professorId}`. 2. Validar respuesta HTTP. 3. Verificar invocación del servicio `markAllAsRead`. | El sistema debe responder con estado `200 OK` y ejecutar correctamente el marcado masivo de notificaciones. | OK |

---

## 5. Consulta de Preferencias de Notificación

Estos casos verifican que el sistema retorne correctamente las preferencias configuradas por un profesor.

| ID Caso | Nombre del Test (Método) | Descripción | Precondiciones | Pasos | Resultado Esperado | Estado |
| :-- | :-- | :-- | :-- | :-- | :-- | :-- |
| NCT-005 | `getPrefs_returnsDto` | Verifica que el sistema retorne las preferencias de notificación configuradas para un profesor. | Existen preferencias configuradas para el profesor. | 1. Configurar mock del servicio `getPreferences`. 2. Realizar petición GET a `/notifications/prefs/{professorId}`. 3. Validar respuesta JSON. | El sistema debe responder con estado `200 OK` y retornar correctamente las preferencias de notificación del profesor. | OK |

---

## 6. Actualización de Preferencias de Notificación

Estos casos verifican que el sistema permita actualizar correctamente las preferencias de notificación.

| ID Caso | Nombre del Test (Método) | Descripción | Precondiciones | Pasos | Resultado Esperado | Estado |
| :-- | :-- | :-- | :-- | :-- | :-- | :-- |
| NCT-006 | `updatePrefs_updatesAndReturnsDto` | Verifica que el sistema permita actualizar las preferencias de notificación de un profesor. | Existe un profesor válido con preferencias configuradas. | 1. Crear objeto `NotificationPreferenceDTO` con nuevas preferencias. 2. Configurar mock del servicio `updatePreferences`. 3. Realizar petición PUT a `/notifications/prefs/{professorId}`. 4. Validar respuesta JSON. | El sistema debe responder con estado `200 OK` y retornar las nuevas preferencias actualizadas correctamente. | OK |

---

# Configuración de Datos de Prueba

Antes de ejecutar cada caso de prueba:

- Se inicializa `MockMvc` mediante `@WebMvcTest`.
- Se mockean `NotificationService` y `NotificationPreferenceService` utilizando `@MockBean`.
- Se deshabilitan filtros de seguridad mediante `@AutoConfigureMockMvc(addFilters = false)`.
- Se mockea `JwtAuthenticationFilter` para aislar las pruebas del sistema de autenticación.

Las respuestas de los servicios son simuladas utilizando `Mockito.when(...)`.

---

# Dependencias Utilizadas

Las pruebas integran los siguientes componentes:

| Componente | Tipo |
| :-- | :-- |
| `NotificationController` | Controlador principal |
| `NotificationService` | Servicio mockeado |
| `NotificationPreferenceService` | Servicio mockeado |
| `MockMvc` | Framework de pruebas web |
| `Mockito` | Framework de mocking |

---

# Notas

- Las pruebas utilizan `@WebMvcTest(NotificationController.class)` para cargar únicamente el contexto web necesario.
- Los filtros de seguridad se encuentran deshabilitados para simplificar la ejecución de pruebas.
- El filtro JWT (`JwtAuthenticationFilter`) es mockeado para evitar dependencias externas de autenticación.
- Las validaciones incluyen tanto el estado HTTP como el contenido de la respuesta JSON.
- Los casos implementados cubren el flujo funcional principal de gestión de notificaciones y preferencias de usuario.
