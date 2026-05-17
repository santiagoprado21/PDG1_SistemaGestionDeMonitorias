# Diseño de Pruebas: `ChatFlowIntegrationTest`

Este documento describe los casos de prueba diseñados para verificar el flujo de mensajería implementado mediante el módulo de chat del sistema. Los tests de integración correspondientes se encuentran en la clase `ChatFlowIntegrationTest`.

**Objetivo:**  
Asegurar que el sistema permita el envío, almacenamiento y recuperación correcta de mensajes entre diferentes roles académicos, garantizando la persistencia del historial de conversación y la asociación adecuada con actividades académicas.

**Alcance:**  
Pruebas de integración sobre los endpoints REST del módulo de chat utilizando `MockMvc` en un entorno completo de Spring Boot (`@SpringBootTest`) bajo el perfil `test`.

Se valida la interacción entre:
- Controladores REST,
- Servicios de chat,
- Persistencia de mensajes,
- Recuperación de conversaciones,
- y relaciones entre usuarios y actividades.

**Estrategia:**  
Pruebas de integración basadas en flujos funcionales completos de mensajería, verificando:
- envío de mensajes,
- persistencia del historial,
- recuperación cronológica de conversaciones,
- comunicación bidireccional entre roles,
- y asociación de mensajes con actividades.

Las pruebas utilizan aserciones (`status`, `jsonPath`, `hasSize`, `is`) para validar respuestas HTTP, estructura JSON y persistencia de datos.

---

# Casos de Prueba

A continuación, se detallan los casos de prueba diseñados e implementados en `ChatFlowIntegrationTest`:

---

## 1. Persistencia de Historial con Referencia a Actividad

Estos casos verifican que los mensajes enviados entre profesor y monitor se almacenen correctamente y mantengan la referencia a la actividad asociada.

| ID Caso | Nombre del Test (Método) | Descripción | Precondiciones | Pasos | Resultado Esperado | Estado |
| :-- | :-- | :-- | :-- | :-- | :-- | :-- |
| CFI-001 | `sendAndRetrieveMessages_keepsHistoryAndActivityReference` | Verifica el envío y recuperación de mensajes entre profesor y monitor manteniendo el historial y la referencia de actividad. | El sistema de chat se encuentra disponible y configurado en entorno de pruebas. | 1. Crear conversación entre profesor y monitor. 2. Enviar mensaje del profesor asociado a actividad. 3. Enviar respuesta del monitor. 4. Consultar historial mediante `GET /chat/messages/{conversationId}`. | El sistema debe almacenar ambos mensajes, conservar el orden cronológico y mantener el `activityId` asociado a cada mensaje. | OK |

---

## 2. Comunicación Bidireccional entre Jefe de Departamento y Profesor

Estos casos verifican la correcta persistencia de conversaciones bidireccionales entre jefe de departamento y profesor.

| ID Caso | Nombre del Test (Método) | Descripción | Precondiciones | Pasos | Resultado Esperado | Estado |
| :-- | :-- | :-- | :-- | :-- | :-- | :-- |
| CFI-002 | `sendAndRetrieveMessages_departmentHeadAndProfessor_keepsBidirectionalHistory` | Verifica la persistencia y recuperación de mensajes entre jefe de departamento y profesor. | El sistema de chat debe encontrarse operativo y configurado correctamente. | 1. Crear conversación entre jefe de departamento y profesor. 2. Enviar mensaje inicial desde jefe de departamento. 3. Enviar respuesta desde profesor. 4. Recuperar historial de conversación. | El historial debe contener ambos mensajes, conservando el orden y el rol correspondiente de cada remitente. | OK |

---

# Configuración de Datos de Prueba

Antes de ejecutar cada caso de prueba, se configura automáticamente el entorno de integración utilizando:

- `@SpringBootTest`
- `@AutoConfigureMockMvc(addFilters = false)`
- Perfil `test`

Además:
- Se mockea `JavaMailSender` para evitar envío real de correos.
- Se mockea `JwtAuthenticationFilter` para deshabilitar autenticación JWT durante las pruebas.

---

# Dependencias Utilizadas

Las pruebas integran los siguientes componentes:

| Componente | Tipo |
| :-- | :-- |
| `MockMvc` | Cliente de pruebas HTTP |
| `ObjectMapper` | Serialización JSON |
| `ChatController` | Controlador REST |
| `ChatService` | Servicio de mensajería |
| `JwtAuthenticationFilter` | Seguridad mockeada |
| `JavaMailSender` | Dependencia simulada |

---

# Endpoints Probados

| Endpoint | Método | Descripción |
| :-- | :-- | :-- |
| `/chat/messages` | POST | Envía mensajes dentro de una conversación |
| `/chat/messages/{conversationId}` | GET | Recupera historial de mensajes |

---

# Validaciones Realizadas

Las pruebas validan:

- Código HTTP correcto (`201 Created`, `200 OK`)
- Persistencia del historial de conversación
- Integridad del `conversationId`
- Asociación correcta de `activityId`
- Persistencia de roles (`professor`, `monitor`, `jfedpto`)
- Recuperación cronológica de mensajes
- Integridad del contenido de mensajes

---

# Notas

- Las pruebas utilizan `MockMvc` para simular peticiones HTTP reales.
- Los filtros de seguridad se deshabilitan mediante `addFilters = false`.
- La autenticación JWT es simulada utilizando `@MockBean`.
- Los datos enviados se serializan mediante `ObjectMapper`.
- Los casos implementados validan flujos completos de integración del módulo de chat.
- Las pruebas verifican tanto persistencia como recuperación de datos dentro del mismo flujo funcional.
