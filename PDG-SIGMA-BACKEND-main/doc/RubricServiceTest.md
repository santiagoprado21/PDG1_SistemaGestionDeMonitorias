# Diseño de Pruebas: `RubricServiceImpl`

Este documento describe los casos de prueba diseñados para verificar la funcionalidad relacionada con la gestión de rúbricas de evaluación implementadas mediante `RubricServiceImpl`. Los tests unitarios correspondientes se encuentran en la clase `RubricServiceTest`.

**Objetivo:**  
Asegurar que `RubricServiceImpl` gestione correctamente la creación, actualización, consulta, validación y eliminación de rúbricas de evaluación utilizadas en el sistema de monitorías académicas.

**Alcance:**  
Pruebas unitarias sobre los métodos principales de `RubricServiceImpl`, utilizando dependencias simuladas con Mockito (`@Mock`, `@InjectMocks`). Se valida la interacción con los componentes `RubricRepository`, `ProfessorRepository` y `ObjectMapper`.

**Estrategia:**  
Pruebas unitarias basadas en los flujos funcionales de gestión de rúbricas (HU-011), verificando:
- creación de rúbricas,
- actualización de rúbricas,
- consulta por profesor e identificador,
- validación de puntajes,
- eliminación de registros,
- y validación de existencia por nombre.

Las pruebas utilizan aserciones (`assertEquals`, `assertNotNull`, `assertThrows`, `assertTrue`) y verificaciones de Mockito (`verify`, `times`, `never`) para validar el comportamiento esperado y el manejo de excepciones.

---

# Casos de Prueba

A continuación, se detallan los casos de prueba diseñados e implementados en `RubricServiceTest`:

---

## 1. Creación de Rúbricas

Estos casos verifican que el sistema permita crear correctamente nuevas rúbricas de evaluación.

| ID Caso | Nombre del Test (Método) | Descripción | Precondiciones | Pasos | Resultado Esperado | Estado |
| :-- | :-- | :-- | :-- | :-- | :-- | :-- |
| RUB-001 | `testCreateRubric` | Verifica la creación exitosa de una rúbrica con criterios válidos. | Existe un `Professor` válido registrado. | 1. Construir `CreateRubricRequest` con criterios y puntajes válidos. 2. Simular serialización JSON de criterios. 3. Ejecutar `createRubric`. | El sistema debe crear la rúbrica correctamente y retornar un `RubricDTO` con los datos esperados. | OK |

---

## 2. Actualización de Rúbricas

Estos casos verifican que una rúbrica existente pueda ser actualizada correctamente.

| ID Caso | Nombre del Test (Método) | Descripción | Precondiciones | Pasos | Resultado Esperado | Estado |
| :-- | :-- | :-- | :-- | :-- | :-- | :-- |
| RUB-002 | `testUpdateRubric` | Verifica la actualización exitosa de una rúbrica existente. | Existe una rúbrica registrada previamente. | 1. Consultar la rúbrica por ID. 2. Modificar información de nombre y criterios. 3. Ejecutar `updateRubric`. | El sistema debe actualizar la información correctamente y persistir los cambios. | OK |

---

## 3. Consulta de Rúbricas por Profesor

Estos casos verifican que el sistema retorne correctamente las rúbricas asociadas a un profesor.

| ID Caso | Nombre del Test (Método) | Descripción | Precondiciones | Pasos | Resultado Esperado | Estado |
| :-- | :-- | :-- | :-- | :-- | :-- | :-- |
| RUB-003 | `testGetRubricsByProfessor` | Verifica la obtención de todas las rúbricas asociadas a un profesor. | Existen múltiples rúbricas creadas por el profesor. | 1. Simular múltiples registros de rúbricas. 2. Ejecutar `getRubricsByProfessor`. | El sistema debe retornar la lista completa de rúbricas asociadas al profesor. | OK |

---

## 4. Eliminación de Rúbricas

Estos casos verifican la eliminación correcta de rúbricas existentes.

| ID Caso | Nombre del Test (Método) | Descripción | Precondiciones | Pasos | Resultado Esperado | Estado |
| :-- | :-- | :-- | :-- | :-- | :-- | :-- |
| RUB-004 | `testDeleteRubric` | Verifica la eliminación correcta de una rúbrica existente. | Existe una rúbrica registrada en la base de datos. | 1. Verificar existencia por ID. 2. Ejecutar `deleteRubric`. | La rúbrica debe eliminarse correctamente del repositorio. | OK |

---

## 5. Eliminación de Rúbricas Inexistentes

Estos casos verifican el manejo de errores al intentar eliminar rúbricas inexistentes.

| ID Caso | Nombre del Test (Método) | Descripción | Precondiciones | Pasos | Resultado Esperado | Estado |
| :-- | :-- | :-- | :-- | :-- | :-- | :-- |
| RUB-005 | `testDeleteNonExistentRubric` | Verifica que el sistema lance excepción al intentar eliminar una rúbrica inexistente. | No existe una rúbrica con el ID consultado. | 1. Simular inexistencia de la rúbrica. 2. Ejecutar `deleteRubric`. | El sistema debe lanzar una excepción indicando que la rúbrica no fue encontrada. | OK |

---

## 6. Validación de Puntajes Totales

Estos casos verifican que la suma de criterios coincida con el puntaje total definido para la rúbrica.

| ID Caso | Nombre del Test (Método) | Descripción | Precondiciones | Pasos | Resultado Esperado | Estado |
| :-- | :-- | :-- | :-- | :-- | :-- | :-- |
| RUB-006 | `testValidateTotalPoints` | Verifica que la suma de puntos de los criterios coincida con el total definido. | Existe un `CreateRubricRequest` válido con criterios configurados. | 1. Crear criterios con puntajes acumulados. 2. Ejecutar `createRubric`. 3. Calcular suma de criterios retornados. | La suma de los criterios debe coincidir exactamente con el total de puntos configurado. | OK |

---

## 7. Consulta de Rúbricas por ID

Estos casos verifican la obtención correcta de una rúbrica específica.

| ID Caso | Nombre del Test (Método) | Descripción | Precondiciones | Pasos | Resultado Esperado | Estado |
| :-- | :-- | :-- | :-- | :-- | :-- | :-- |
| RUB-007 | `testGetRubricById` | Verifica la obtención de una rúbrica mediante su identificador. | Existe una rúbrica registrada con el ID consultado. | 1. Simular búsqueda por ID. 2. Ejecutar `getRubricById`. | El sistema debe retornar correctamente la información de la rúbrica. | OK |

---

## 8. Consulta de Rúbricas Inexistentes

Estos casos verifican el manejo de errores cuando una rúbrica no existe.

| ID Caso | Nombre del Test (Método) | Descripción | Precondiciones | Pasos | Resultado Esperado | Estado |
| :-- | :-- | :-- | :-- | :-- | :-- | :-- |
| RUB-008 | `testGetNonExistentRubric` | Verifica que el sistema lance excepción al consultar una rúbrica inexistente. | No existe una rúbrica registrada con el ID consultado. | 1. Simular respuesta vacía del repositorio. 2. Ejecutar `getRubricById`. | El sistema debe lanzar una excepción indicando que la rúbrica no fue encontrada. | OK |

---

## 9. Validación de Existencia por Nombre y Profesor

Estos casos verifican que el sistema pueda validar la existencia de rúbricas duplicadas por nombre y profesor.

| ID Caso | Nombre del Test (Método) | Descripción | Precondiciones | Pasos | Resultado Esperado | Estado |
| :-- | :-- | :-- | :-- | :-- | :-- | :-- |
| RUB-009 | `testExistsByNameAndProfessor` | Verifica que el sistema detecte la existencia de una rúbrica con el mismo nombre para un profesor. | Existe una rúbrica registrada previamente. | 1. Simular consulta por nombre y profesor. 2. Ejecutar `existsByNameAndProfessor`. | El sistema debe retornar `true` indicando que la rúbrica ya existe. | OK |

---

# Configuración de Datos de Prueba

Antes de ejecutar cada caso de prueba, se realiza la configuración automática de datos simulados mediante el método `setUp()`:

- `Professor`
- `CreateRubricRequest`
- `Rubric`
- `RubricCriterion`

Los datos se configuran utilizando Mockito para simular el comportamiento de los repositorios y dependencias auxiliares.

---

# Dependencias Utilizadas

Las pruebas integran los siguientes componentes:

| Componente | Tipo |
| :-- | :-- |
| `RubricServiceImpl` | Servicio principal |
| `RubricRepository` | Repositorio Mock |
| `ProfessorRepository` | Repositorio Mock |
| `ObjectMapper` | Dependencia Mock |

---

# Notas

- Las pruebas están configuradas utilizando `MockitoExtension` y anotaciones `@Mock` y `@InjectMocks`.
- Se valida tanto el flujo exitoso como el manejo de excepciones para operaciones inválidas.
- Las pruebas verifican serialización de criterios mediante `ObjectMapper`.
- Se utilizan verificaciones de interacción (`verify`) para confirmar llamadas correctas a los repositorios.
- Los casos implementados corresponden al flujo funcional de gestión de rúbricas definido para la HU-011.
