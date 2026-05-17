# ChatControllerTest - Casos de Prueba

## Clase
`ChatControllerTest`

## Objetivo
Validar el funcionamiento del controlador de chat, asegurando:
- Obtención de conversaciones.
- Envío de mensajes.
- Envío de mensajes con archivos adjuntos.
- Descarga de archivos adjuntos.
- Manejo correcto de errores HTTP.

---

# Casos de Prueba

## 1. Obtener conversaciones exitosamente

### Método
`getConversations_returnsOk()`

### Objetivo
Verificar que un usuario pueda obtener correctamente sus conversaciones.

### Flujo
1. Simular conversaciones existentes.
2. Ejecutar petición GET.
3. Validar respuesta.

### Endpoint
`GET /chat/conversations/{userId}/{role}`

### Resultado Esperado
- Estado HTTP `200 OK`.
- Retorna lista de conversaciones.
- El ID de conversación coincide con el esperado.

---

## 2. Enviar mensaje exitosamente

### Método
`sendMessage_returnsCreated()`

### Objetivo
Validar el envío de mensajes simples sin archivos adjuntos.

### Flujo
1. Crear payload del mensaje.
2. Simular creación del mensaje.
3. Ejecutar petición POST.

### Endpoint
`POST /chat/messages`

### Resultado Esperado
- Estado HTTP `201 Created`.
- Retorna el mensaje creado.
- El ID del mensaje es correcto.
- La conversación corresponde al payload enviado.

---

## 3. Enviar mensaje con archivos adjuntos

### Método
`sendMessageWithAttachments_returnsCreated()`

### Objetivo
Verificar el envío de mensajes junto con archivos adjuntos.

### Flujo
1. Crear payload multipart.
2. Adjuntar archivo.
3. Ejecutar petición multipart.

### Endpoint
`POST /chat/messages/with-attachments`

### Resultado Esperado
- Estado HTTP `201 Created`.
- El mensaje se registra exitosamente.
- El archivo adjunto es procesado correctamente.

---

## 4. Descargar adjunto inexistente

### Método
`downloadAttachment_notFound_returns404()`

### Objetivo
Validar manejo de errores al solicitar archivos inexistentes.

### Flujo
1. Solicitar descarga de adjunto inexistente.
2. Simular excepción del servicio.

### Endpoint
`GET /chat/attachments/{attachmentId}`

### Resultado Esperado
- Estado HTTP `404 Not Found`.
- Mensaje: `"Adjunto no encontrado"`.

---

## 5. Descargar archivo adjunto exitosamente

### Método
`downloadAttachment_returnsFile()`

### Objetivo
Verificar la descarga correcta de archivos adjuntos.

### Flujo
1. Simular archivo almacenado.
2. Ejecutar petición GET.
3. Validar contenido retornado.

### Endpoint
`GET /chat/attachments/{attachmentId}`

### Resultado Esperado
- Estado HTTP `200 OK`.
- Content-Type correcto (`text/plain`).
- Archivo retornado exitosamente.

---

# Cobertura Validada

- Obtención de conversaciones.
- Envío de mensajes.
- Manejo de multipart/form-data.
- Descarga de archivos.
- Manejo de errores HTTP.
- Serialización JSON.
- Validación de endpoints REST.
