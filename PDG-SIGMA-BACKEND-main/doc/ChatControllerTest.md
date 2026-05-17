# Diseño de Pruebas: `ChatController`

Este documento describe los casos de prueba diseñados para verificar la funcionalidad del controlador de chat implementado mediante `ChatController`. Los tests correspondientes se encuentran en la clase `ChatControllerTest`.

**Objetivo:**  
Asegurar que `ChatController` gestione correctamente las operaciones relacionadas con conversaciones, envío de mensajes, manejo de archivos adjuntos y descarga de recursos, validando respuestas HTTP y manejo adecuado de errores.

**Alcance:**  
Pruebas web del controlador utilizando `@WebMvcTest`, configuradas sobre `ChatController` y utilizando mocks para dependencias externas como `ChatService` y `JwtAuthenticationFilter`. Se valida el comportamiento de los endpoints REST asociados al módulo de chat.

**Estrategia:**  
Pruebas funcionales del controlador enfocadas en:

- obtención de conversaciones,
- envío de mensajes,
- envío de mensajes con archivos adjuntos,
- descarga de archivos,
- y manejo de errores HTTP.

Las pruebas utilizan `MockMvc` para simular peticiones HTTP y aserciones (`status`, `jsonPath`, `content`) para validar respuestas esperadas.

---

# Casos de Prueba

A continuación, se detallan los casos de prueba diseñados e implementados en `ChatControllerTest`:

---

## 1. Obtención de Conversaciones

Estos casos verifican que el sistema retorne correctamente las conversaciones asociadas a un usuario.

| ID Caso | Nombre del Test (Método) | Descripción | Precondiciones | Pasos | Resultado Esperado | Estado |
| :-- | :-- | :-- | :-- | :-- | :-- | :-- |
| CHT-001 | `getConversations_returnsOk` | Verifica la obtención exitosa de conversaciones de un usuario. | Existe al menos una conversación simulada asociada al usuario. | 1. Simular conversaciones mediante `ChatService`. 2. Ejecutar petición GET sobre `/chat/conversations/{userId}/{role}`. 3. Validar respuesta JSON. | El sistema debe retornar estado `200 OK` y una lista de conversaciones con el identificador esperado. | OK |

---

## 2. Envío de Mensajes Simples

Estos casos verifican el envío correcto de mensajes sin archivos adjuntos.

| ID Caso | Nombre del Test (Método) | Descripción | Precondiciones | Pasos | Resultado Esperado | Estado |
| :-- | :-- | :-- | :-- | :-- | :-- | :-- |
| CHT-002 | `sendMessage_returnsCreated` | Verifica el envío exitoso de mensajes simples. | Existe una conversación válida entre usuarios. | 1. Construir `ChatMessageCreateDTO`. 2. Simular creación del mensaje mediante `ChatService`. 3. Ejecutar petición POST sobre `/chat/messages`. | El sistema debe retornar estado `201 Created` y devolver el mensaje creado con el ID y conversación correctos. | OK |

---

## 3. Envío de Mensajes con Archivos Adjuntos

Estos casos verifican el procesamiento correcto de mensajes multipart con archivos adjuntos.

| ID Caso | Nombre del Test (Método) | Descripción | Precondiciones | Pasos | Resultado Esperado | Estado |
| :-- | :-- | :-- | :-- | :-- | :-- | :-- |
| CHT-003 | `sendMessageWithAttachments_returnsCreated` | Verifica el envío exitoso de mensajes con archivos adjuntos. | Existe una conversación válida y un archivo de prueba disponible. | 1. Crear payload multipart con mensaje y archivo adjunto. 2. Simular creación del mensaje. 3. Ejecutar petición multipart sobre `/chat/messages/with-attachments`. | El sistema debe retornar estado `201 Created` y registrar correctamente el mensaje junto al archivo adjunto. | OK |

---

## 4. Descarga de Adjuntos Inexistentes

Estos casos verifican el manejo adecuado de errores al solicitar archivos inexistentes.

| ID Caso | Nombre del Test (Método) | Descripción | Precondiciones | Pasos | Resultado Esperado | Estado |
| :-- | :-- | :-- | :-- | :-- | :-- | :-- |
| CHT-004 | `downloadAttachment_notFound_returns404` | Verifica el manejo de error al descargar un adjunto inexistente. | El servicio de chat simula inexistencia del archivo solicitado. | 1. Solicitar descarga de un archivo inexistente. 2. Simular excepción desde `ChatService`. | El sistema debe retornar estado `404 Not Found` con el mensaje `"Adjunto no encontrado"`. | OK |

---

## 5. Descarga Exitosa de Adjuntos

Estos casos verifican la descarga correcta de archivos almacenados.

| ID Caso | Nombre del Test (Método) | Descripción | Precondiciones | Pasos | Resultado Esperado | Estado |
| :-- | :-- | :-- | :-- | :-- | :-- | :-- |
| CHT-005 | `downloadAttachment_returnsFile` | Verifica la descarga exitosa de archivos adjuntos almacenados. | Existe un archivo simulado disponible para descarga. | 1. Simular recurso descargable mediante `ChatService`. 2. Ejecutar petición GET sobre `/chat/attachments/{attachmentId}`. 3. Validar contenido retornado. | El sistema debe retornar estado `200 OK`, content-type correcto y el archivo solicitado. | OK |

---

# Configuración de Datos de Prueba

Antes de ejecutar cada caso de prueba, se configuran automáticamente los componentes necesarios mediante mocks y configuración de entorno:

- `ChatService`
- `JwtAuthenticationFilter`
- `MockMvc`
- `ObjectMapper`

Las dependencias son simuladas mediante `@MockBean` para aislar completamente el comportamiento del controlador.

---

# Dependencias Utilizadas

Las pruebas integran los siguientes componentes:

| Componente | Tipo |
| :-- | :-- |
| `ChatController` | Controlador principal |
| `ChatService` | Servicio mockeado |
| `JwtAuthenticationFilter` | Filtro mockeado |
| `MockMvc` | Framework de pruebas HTTP |
| `ObjectMapper` | Serialización JSON |

---

# Notas

- Las pruebas utilizan `@WebMvcTest` para aislar únicamente el comportamiento del controlador.
- La seguridad de Spring Security se encuentra deshabilitada mediante `excludeAutoConfiguration`.
- Se utiliza `@AutoConfigureMockMvc(addFilters = false)` para evitar ejecución de filtros reales durante las pruebas.
- Los archivos adjuntos son simulados mediante `MockMultipartFile`.
- Las respuestas HTTP son validadas utilizando `jsonPath`, `status` y `content`.
- Los casos implementados cubren el flujo funcional principal del módulo de chat.
