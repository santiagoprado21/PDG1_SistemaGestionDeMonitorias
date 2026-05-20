# Diseño de Pruebas: `ChatServiceImpl`

Este documento describe los casos de prueba diseñados para verificar la funcionalidad del servicio de mensajería implementado mediante `ChatServiceImpl`. Los tests unitarios correspondientes se encuentran en la clase `ChatServiceImplTest`.

**Objetivo:**  
Asegurar que `ChatServiceImpl` gestione correctamente el envío de mensajes, la construcción automática de conversaciones, la recuperación de mensajes, la resolución de destinatarios y la comunicación entre profesores, monitores y jefes de departamento.

**Alcance:**  
Pruebas unitarias sobre los métodos principales de `ChatServiceImpl`, utilizando mocks configurados con `MockitoExtension`. Se valida la interacción entre repositorios, servicios auxiliares y lógica de negocio relacionada con conversaciones y mensajes.

**Estrategia:**  
Pruebas unitarias enfocadas en los flujos funcionales principales del sistema de chat, verificando:
- creación automática de conversaciones,
- persistencia de mensajes,
- resolución de destinatarios,
- manejo de referencias a actividades,
- validaciones de mensajes,
- recuperación de conversaciones,
- y manejo de errores internos.

Las pruebas utilizan aserciones (`assertThat`, `assertThatThrownBy`) para validar respuestas, excepciones y consistencia de datos.

---

# Casos de Prueba

A continuación, se detallan los casos de prueba diseñados e implementados en `ChatServiceImplTest`:

---

## 1. Creación automática de conversación profesor-monitor

Estos casos verifican que el sistema construya automáticamente una conversación entre profesor y monitor cuando no existe previamente.

| ID Caso | Nombre del Test (Método) | Descripción | Precondiciones | Pasos | Resultado Esperado | Estado |
| :-- | :-- | :-- | :-- | :-- | :-- | :-- |
| CHATS-001 | `sendMessage_professorWithoutConversation_buildsConversationAndPersists` | Verifica que un profesor pueda iniciar una conversación con un monitor y que el mensaje sea persistido correctamente. | Existe un monitor válido registrado en el sistema. | 1. Crear payload del mensaje sin `conversationId`. 2. Enviar mensaje mediante `sendMessage`. 3. Validar persistencia y generación automática de conversación. | El sistema debe crear la conversación `prof-PROF1__mon-MON1`, normalizar el rol a `professor`, limpiar el texto del mensaje y persistir correctamente el mensaje. | OK |

---

## 2. Resolución automática del destinatario desde conversationId

Estos casos verifican que el sistema pueda determinar automáticamente el destinatario cuando no es enviado explícitamente.

| ID Caso | Nombre del Test (Método) | Descripción | Precondiciones | Pasos | Resultado Esperado | Estado |
| :-- | :-- | :-- | :-- | :-- | :-- | :-- |
| CHATS-002 | `sendMessage_monitorWithoutReceiver_resolvesReceiverFromConversationId` | Verifica que un monitor pueda enviar mensajes utilizando únicamente el `conversationId`. | Existe una conversación válida entre profesor y monitor. | 1. Crear payload sin `receiverId`. 2. Enviar mensaje mediante `sendMessage`. 3. Validar destinatario resuelto automáticamente. | El sistema debe identificar automáticamente al profesor como destinatario (`PROF2`) y conservar correctamente el `conversationId`. | OK |

---

## 3. Conservación de referencias a actividades

Estos casos verifican que los mensajes asociados a actividades mantengan correctamente la referencia correspondiente.

| ID Caso | Nombre del Test (Método) | Descripción | Precondiciones | Pasos | Resultado Esperado | Estado |
| :-- | :-- | :-- | :-- | :-- | :-- | :-- |
| CHATS-003 | `sendMessage_withActivityId_keepsActivityReference` | Verifica que un mensaje conserve correctamente el identificador de actividad asociado. | Existe una conversación válida entre profesor y monitor. | 1. Crear mensaje con `activityId`. 2. Enviar mensaje mediante `sendMessage`. 3. Validar datos retornados. | El mensaje debe conservar el `activityId` igual a `88` y mantener el contenido enviado. | OK |

---

## 4. Validación de mensajes vacíos

Estos casos verifican que el sistema rechace mensajes sin contenido ni archivos adjuntos.

| ID Caso | Nombre del Test (Método) | Descripción | Precondiciones | Pasos | Resultado Esperado | Estado |
| :-- | :-- | :-- | :-- | :-- | :-- | :-- |
| CHATS-004 | `sendMessage_withoutTextAndFiles_throwsValidationError` | Verifica que el sistema valide correctamente mensajes vacíos. | No aplica. | 1. Crear payload sin texto válido y sin archivos adjuntos. 2. Ejecutar `sendMessage`. | El sistema debe lanzar una excepción indicando que el mensaje requiere texto o al menos un archivo adjunto. | OK |

---

## 5. Manejo de errores al recuperar mensajes

Estos casos verifican que el sistema maneje correctamente excepciones del repositorio de mensajes.

| ID Caso | Nombre del Test (Método) | Descripción | Precondiciones | Pasos | Resultado Esperado | Estado |
| :-- | :-- | :-- | :-- | :-- | :-- | :-- |
| CHATS-005 | `getMessages_whenRepositoryThrows_returnsEmptyList` | Verifica el manejo seguro de errores durante la recuperación de mensajes. | El repositorio de mensajes genera una excepción. | 1. Simular excepción del repositorio. 2. Ejecutar `getMessages`. | El sistema debe retornar una lista vacía sin propagar la excepción. | OK |

---

## 6. Obtención de conversaciones para jefe de departamento

Estos casos verifican que un jefe de departamento pueda visualizar conversaciones asociadas a sus profesores.

| ID Caso | Nombre del Test (Método) | Descripción | Precondiciones | Pasos | Resultado Esperado | Estado |
| :-- | :-- | :-- | :-- | :-- | :-- | :-- |
| CHATS-006 | `getConversations_departmentHead_returnsProfessorConversations` | Verifica la generación de conversaciones entre jefe de departamento y profesores asociados. | Existe un profesor asociado al jefe de departamento. | 1. Simular profesores asociados al jefe. 2. Ejecutar `getConversations`. 3. Validar información retornada. | El sistema debe generar una conversación con ID `head-H5001__prof-P100` y título correspondiente al profesor. | OK |

---

## 7. Creación automática de conversación jefe-profesor

Estos casos verifican que el sistema genere automáticamente conversaciones entre jefes de departamento y profesores.

| ID Caso | Nombre del Test (Método) | Descripción | Precondiciones | Pasos | Resultado Esperado | Estado |
| :-- | :-- | :-- | :-- | :-- | :-- | :-- |
| CHATS-007 | `sendMessage_departmentHeadWithoutConversation_buildsHeadProfessorConversation` | Verifica que un jefe de departamento pueda iniciar conversación con un profesor. | Existe un profesor válido registrado en el sistema. | 1. Crear payload sin `conversationId`. 2. Enviar mensaje mediante `sendMessage`. 3. Validar conversación generada. | El sistema debe construir automáticamente la conversación `head-H5001__prof-P100` y persistir correctamente el mensaje. | OK |

---

## 8. Conversación profesor → jefe de departamento

Estos casos verifican que un profesor pueda iniciar conversaciones con jefes de departamento.

| ID Caso | Nombre del Test (Método) | Descripción | Precondiciones | Pasos | Resultado Esperado | Estado |
| :-- | :-- | :-- | :-- | :-- | :-- | :-- |
| CHATS-008 | `sendMessage_professorToDepartmentHeadWithoutConversation_buildsHeadProfessorConversation` | Verifica que un profesor pueda iniciar conversación con un jefe de departamento. | Existe un jefe de departamento válido registrado. | 1. Crear payload sin `conversationId`. 2. Enviar mensaje mediante `sendMessage`. 3. Validar conversación generada. | El sistema debe generar automáticamente la conversación `head-H6001__prof-P200`. | OK |

---

## 9. Resolución automática del jefe de departamento desde conversationId

Estos casos verifican que el sistema resuelva automáticamente al jefe de departamento como destinatario utilizando el `conversationId`.

| ID Caso | Nombre del Test (Método) | Descripción | Precondiciones | Pasos | Resultado Esperado | Estado |
| :-- | :-- | :-- | :-- | :-- | :-- | :-- |
| CHATS-009 | `sendMessage_professorFromHeadConversationWithoutReceiver_resolvesDepartmentHeadReceiver` | Verifica la resolución automática del jefe de departamento como destinatario. | Existe una conversación válida entre jefe y profesor. | 1. Crear payload sin `receiverId`. 2. Enviar mensaje mediante `sendMessage`. 3. Validar destinatario generado automáticamente. | El sistema debe identificar correctamente al jefe de departamento (`H9001`) como destinatario. | OK |

---

# Configuración de Datos de Prueba

Las pruebas utilizan mocks configurados mediante `MockitoExtension` para simular el comportamiento de las dependencias externas:

- `MonitorRepository`
- `ProfessorRepository`
- `DepartmentHeadRepository`
- `DepartmentHeadService`
- `ChatMessageRepository`
- `ChatAttachmentRepository`
- `ChatStorageService`
- `JdbcTemplate`

La persistencia de mensajes y consultas se simula utilizando `Mockito.when()` y `thenAnswer()` para controlar el comportamiento esperado durante cada flujo.

---

# Dependencias Utilizadas

Las pruebas integran los siguientes componentes:

| Componente | Tipo |
| :-- | :-- |
| `ChatServiceImpl` | Servicio principal |
| `ChatMessageRepository` | Repositorio |
| `ChatAttachmentRepository` | Repositorio |
| `MonitorRepository` | Repositorio |
| `ProfessorRepository` | Repositorio |
| `DepartmentHeadRepository` | Repositorio |
| `DepartmentHeadService` | Servicio |
| `ChatStorageService` | Servicio |
| `JdbcTemplate` | Componente JDBC |

---

# Notas

- Las pruebas están configuradas con `@ExtendWith(MockitoExtension.class)` para habilitar mocks unitarios.
- Se utiliza `@InjectMocks` para inyectar automáticamente las dependencias simuladas en `ChatServiceImpl`.
- La persistencia de mensajes es simulada utilizando respuestas dinámicas con `thenAnswer`.
- Los tests validan normalización de roles, construcción de IDs de conversación y resolución automática de destinatarios.
- Las pruebas cubren flujos entre profesores, monitores y jefes de departamento.
- Los casos implementados corresponden al flujo funcional del módulo de mensajería interna del sistema.
