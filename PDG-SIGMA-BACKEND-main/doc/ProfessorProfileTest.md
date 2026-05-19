# Diseño de Pruebas: `ProfessorController`

Este documento describe los casos de prueba diseñados para verificar la funcionalidad relacionada con la consulta del perfil de profesores implementada mediante `ProfessorController`. Los tests correspondientes se encuentran en la clase `ProfessorProfileTest`.

**Objetivo:**  
Asegurar que `ProfessorController` gestione correctamente la obtención de información del perfil de profesores, incluyendo validaciones para profesores inexistentes y profesores sin cursos asignados.

**Alcance:**  
Pruebas web de tipo controlador utilizando `@WebMvcTest`, validando respuestas HTTP, serialización JSON y manejo de excepciones del endpoint `/professor/profile/{id}`.

**Estrategia:**  
Pruebas orientadas a verificar:
- consulta exitosa del perfil,
- validación de profesor inexistente,
- manejo de profesores sin cursos asignados.

Las pruebas utilizan `MockMvc`, `Mockito` y aserciones HTTP (`status`, `jsonPath`, `content`) para validar comportamiento esperado.

---

# Casos de Prueba

A continuación, se detallan los casos de prueba implementados en `ProfessorProfileTest`:

---

## 1. Consulta Exitosa del Perfil del Profesor

Estos casos verifican que el sistema retorne correctamente la información del perfil de un profesor existente.

| ID Caso | Nombre del Test (Método) | Descripción | Precondiciones | Pasos | Resultado Esperado | Estado |
| :-- | :-- | :-- | :-- | :-- | :-- | :-- |
| PFT-001 | `testGetProfessorProfile_Success` | Verifica que el sistema retorne correctamente el perfil de un profesor existente. | Existe un profesor válido registrado en el sistema. | 1. Simular respuesta del servicio `getProfile`. 2. Realizar petición GET al endpoint `/professor/profile/{id}`. 3. Validar respuesta JSON. | El sistema debe responder HTTP 200 y retornar correctamente los campos `school`, `program`, `rol` y `name`. | OK |

---

## 2. Profesor No Encontrado

Estos casos verifican el comportamiento cuando se consulta un profesor inexistente.

| ID Caso | Nombre del Test (Método) | Descripción | Precondiciones | Pasos | Resultado Esperado | Estado |
| :-- | :-- | :-- | :-- | :-- | :-- | :-- |
| PFT-002 | `testGetProfessorProfile_NotFound` | Verifica que el sistema maneje correctamente la consulta de un profesor inexistente. | No existe un profesor asociado al ID consultado. | 1. Simular excepción en `getProfile`. 2. Realizar petición GET al endpoint `/professor/profile/{id}`. 3. Validar respuesta HTTP. | El sistema debe responder HTTP 404 y retornar el mensaje `"No existe profesor con este ID"`. | OK |

---

## 3. Profesor sin Cursos Asignados

Estos casos verifican el manejo de profesores que no poseen cursos asignados en el semestre actual.

| ID Caso | Nombre del Test (Método) | Descripción | Precondiciones | Pasos | Resultado Esperado | Estado |
| :-- | :-- | :-- | :-- | :-- | :-- | :-- |
| PFT-003 | `testGetProfessorProfile_NoCoursesAssigned` | Verifica que el sistema informe correctamente cuando un profesor no tiene cursos asignados. | Existe un profesor válido sin cursos asignados en el semestre. | 1. Simular excepción en `getProfile`. 2. Realizar petición GET al endpoint `/professor/profile/{id}`. 3. Validar respuesta HTTP y mensaje retornado. | El sistema debe responder HTTP 404 y retornar el mensaje `"No tiene asignado cursos para este semestre"`. | OK |

---

# Configuración de Datos de Prueba

Las pruebas utilizan mocks configurados mediante `Mockito` sobre el servicio `ProfessorServiceImpl`.

Se simulan distintos escenarios:
- profesor existente,
- profesor inexistente,
- profesor sin cursos asignados.

El entorno de pruebas utiliza:
- `@WebMvcTest`
- `MockMvc`
- `@WithMockUser`

para validar comportamiento del controlador de manera aislada.

---

# Dependencias Utilizadas

Las pruebas integran los siguientes componentes:

| Componente | Tipo |
| :-- | :-- |
| `ProfessorController` | Controlador principal |
| `ProfessorServiceImpl` | Servicio mockeado |
| `MockMvc` | Framework de pruebas web |
| `Mockito` | Framework de mocks |

---

# Notas

- Las pruebas utilizan autenticación simulada mediante `@WithMockUser`.
- Se valida tanto el contenido JSON como mensajes de error en texto plano.
- Los endpoints probados corresponden al flujo de consulta de perfil académico de profesores.
- Las respuestas HTTP esperadas incluyen:
  - `200 OK`
  - `404 NOT FOUND`
- Los casos implementados validan escenarios positivos y negativos del controlador.
