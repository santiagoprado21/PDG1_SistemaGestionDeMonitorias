# Diseño de Pruebas: `MonitorController` - Perfil del Monitor

Este documento describe los casos de prueba diseñados para verificar la funcionalidad relacionada con la consulta del perfil de monitor implementada mediante `MonitorController`. Los tests correspondientes se encuentran en la clase `MonitorProfileTest`.

**Objetivo:**  
Asegurar que `MonitorController` gestione correctamente la consulta del perfil de un monitor, retornando la información esperada cuando el monitor existe y manejando adecuadamente los errores cuando no existe.

**Alcance:**  
Pruebas web utilizando `@WebMvcTest` sobre el endpoint principal del controlador `MonitorController`, validando la interacción con `MonitorServiceImpl`.

**Estrategia:**  
Pruebas de integración web enfocadas en la historia de usuario relacionada con visualización de perfil de monitor, verificando:
- consulta exitosa del perfil,
- manejo de errores cuando el monitor no existe,
- validación de respuestas HTTP y contenido JSON.

Las pruebas utilizan `MockMvc`, `Mockito` y aserciones (`status`, `jsonPath`, `content`) para validar el comportamiento esperado del endpoint.

---

# Casos de Prueba

A continuación, se detallan los casos de prueba diseñados e implementados en `MonitorProfileTest`:

---

## 1. Consulta Exitosa del Perfil del Monitor

Estos casos verifican que el sistema retorne correctamente la información del perfil del monitor cuando el ID existe.

| ID Caso | Nombre del Test (Método) | Descripción | Precondiciones | Pasos | Resultado Esperado | Estado |
| :-- | :-- | :-- | :-- | :-- | :-- | :-- |
| MPT-001 | `testProfileMonitorFound` | Verifica que el endpoint retorne correctamente la información del perfil de un monitor existente. | Existe un monitor válido registrado en el sistema y el servicio retorna un `MonitorDTO` válido. | 1. Configurar mock del servicio `getProfile`. 2. Realizar petición GET a `/monitor/profile/{id}`. 3. Validar respuesta HTTP y contenido JSON. | El sistema debe responder con estado `200 OK` y retornar correctamente los campos `school`, `program`, `rol` y `name`. | OK |

---

## 2. Consulta de Perfil para Monitor Inexistente

Estos casos verifican el manejo de errores cuando se consulta un monitor inexistente.

| ID Caso | Nombre del Test (Método) | Descripción | Precondiciones | Pasos | Resultado Esperado | Estado |
| :-- | :-- | :-- | :-- | :-- | :-- | :-- |
| MPT-002 | `testProfileMonitorNotFound` | Verifica que el sistema retorne error cuando el monitor no existe. | El servicio `getProfile` lanza excepción indicando que el monitor no existe. | 1. Configurar mock del servicio para lanzar excepción. 2. Realizar petición GET a `/monitor/profile/{id}` con un ID inexistente. 3. Validar respuesta HTTP y mensaje retornado. | El sistema debe responder con estado `404 NOT FOUND` y retornar el mensaje `"No existe monitor con este ID"`. | OK |

---

# Configuración de Datos de Prueba

Antes de ejecutar cada caso de prueba:

- Se inicializa `MockMvc` mediante `@WebMvcTest`.
- Se mockea `MonitorServiceImpl` utilizando `@MockBean`.
- Se configura autenticación simulada mediante `@WithMockUser`.

Las respuestas del servicio son simuladas utilizando `Mockito.when(...)`.

---

# Dependencias Utilizadas

Las pruebas integran los siguientes componentes:

| Componente | Tipo |
| :-- | :-- |
| `MonitorController` | Controlador principal |
| `MonitorServiceImpl` | Servicio mockeado |
| `MockMvc` | Framework de pruebas web |
| `Mockito` | Framework de mocking |

---

# Notas

- Las pruebas utilizan `@WebMvcTest(MonitorController.class)` para cargar únicamente el contexto web necesario.
- La autenticación se simula mediante `@WithMockUser(roles = "monitor")`.
- El servicio `MonitorServiceImpl` es mockeado para aislar el comportamiento del controlador.
- Las validaciones incluyen tanto el estado HTTP como el contenido de la respuesta JSON o texto plano.
- Los casos implementados validan el flujo funcional de consulta de perfil del monitor.
