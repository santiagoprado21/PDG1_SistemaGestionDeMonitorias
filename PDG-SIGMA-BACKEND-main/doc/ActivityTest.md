# Diseño de Pruebas: ActivityServiceImpl

Este documento describe los casos de prueba diseñados para verificar la funcionalidad de la clase `ActivityServiceImpl`, enfocándose en la lógica de negocio relacionada con la creación, actualización y asignación de roles en las actividades. Los tests unitarios correspondientes se encuentran en la clase `ActivityTest`.

**Objetivo:**  
Asegurar que `ActivityServiceImpl` maneja correctamente la asignación de roles entre `Monitor` y `Professor`, así como la actualización de actividades y el manejo adecuado de errores cuando una actividad no existe.

**Alcance:**  
Pruebas unitarias de los métodos públicos de `ActivityServiceImpl`, utilizando Mocks para las dependencias (`ActivityRepository`, `MonitorRepository`, `ProfessorRepository`, `MonitoringRepository`). No se prueban interacciones reales con la base de datos.

**Estrategia:**  
Pruebas de caja blanca basadas en la lógica interna del servicio y pruebas de caja negra basadas en los requerimientos funcionales. Se utilizan aserciones (`assertEquals`, `assertNull`, `assertThrows`) y verificación de excepciones para validar los resultados esperados.

---

# Casos de Prueba

A continuación, se detallan los casos de prueba diseñados e implementados en `ActivityTest`:

---

## 1. Asignación de Roles

Estos casos validan la asignación de diferentes combinaciones de `Monitor` y `Professor` a una entidad `Activity`.

| ID Caso | Nombre del Test (Método) | Descripción | Precondiciones | Pasos | Resultado Esperado | Estado |
| :-- | :-- | :-- | :-- | :-- | :-- | :-- |
| ACT-001 | `testAssignRoles_MonitorCreator_ProfessorResponsible` | Verifica que se puedan asignar un Monitor y un Profesor a una actividad. | - | 1. Crear `Activity`. 2. Crear `Monitor`. 3. Crear `Professor`. 4. Asignar `Monitor` y `Professor` a `Activity`. | La actividad debe tener asociados correctamente el `Monitor` y el `Professor` (`assertEquals`). | OK |
| ACT-002 | `testAssignRoles_ProfessorCreator_MonitorResponsible` | Verifica la asignación inversa de roles entre Profesor y Monitor. | - | 1. Crear `Activity`. 2. Crear `Professor`. 3. Crear `Monitor`. 4. Asignar ambos a la actividad. | La actividad debe tener asociados correctamente el `Professor` y el `Monitor` (`assertEquals`). | OK |
| ACT-003 | `testAssignRoles_MonitorCreator_MonitorResponsible` | Verifica que se pueda asignar únicamente un Monitor y dejar el Profesor como `null`. | - | 1. Crear `Activity`. 2. Crear `Monitor`. 3. Asignar `Monitor`. 4. Asignar `null` al `Professor`. | La actividad debe tener el `Monitor` correcto y el `Professor` debe ser `null` (`assertNull`). | OK |
| ACT-004 | `testAssignRoles_ProfessorCreator_ProfessorResponsible` | Verifica que se pueda asignar únicamente un Profesor y dejar el Monitor como `null`. | - | 1. Crear `Activity`. 2. Crear `Professor`. 3. Asignar `Professor`. 4. Asignar `null` al `Monitor`. | La actividad debe tener el `Professor` correcto y el `Monitor` debe ser `null` (`assertNull`). | OK |

---

## 2. Creación de Actividad (`save` con `NewActivityRequestDTO`)

Estos casos están presentes en el código, pero actualmente se encuentran comentados en `ActivityTest`.

| ID Caso | Nombre del Test (Método) | Descripción | Precondiciones (Mocks) | Pasos | Resultado Esperado | Estado |
| :-- | :-- | :-- | :-- | :-- | :-- | :-- |
| ACT-005 | `testSave_NewActivity` | Verifica la creación exitosa de una actividad con datos válidos. | `monitoringRepository.findById` devuelve `Monitoring`. `monitorRepository.findByIdMonitor` devuelve `Monitor`. `professorRepository.findById` devuelve `Professor`. `activityRepository.save` devuelve la actividad guardada. | 1. Crear `NewActivityRequestDTO`. 2. Configurar Mocks. 3. Ejecutar `activityService.save(dto)`. 4. Verificar resultado. | Se devuelve un `ActivityDTO` no nulo con los datos correctos de la actividad creada. | Comentado / OK |

---

## 3. Actualización de Actividad (`update` con `ActivityRequestDTO`)

Estos casos verifican la actualización de actividades existentes y el manejo de errores.

| ID Caso | Nombre del Test (Método) | Descripción | Precondiciones (Mocks) | Pasos | Resultado Esperado | Estado |
| :-- | :-- | :-- | :-- | :-- | :-- | :-- |
| ACT-006 | `testUpdate_ExistingActivity` | Verifica la actualización exitosa de una actividad existente con nuevos datos. | `activityRepository.findById` devuelve actividad existente. `monitoringRepository.findById` devuelve nueva monitoría. `monitorRepository.findByIdMonitor` devuelve monitor válido. `professorRepository.findById` devuelve profesor válido. `activityRepository.save` devuelve actividad actualizada. | 1. Crear `ActivityRequestDTO`. 2. Configurar Mocks. 3. Ejecutar `activityService.update(dto)`. 4. Validar datos actualizados. | Se devuelve un `ActivityDTO` actualizado correctamente (`assertEquals`). | Comentado / OK |
| ACT-007 | `testUpdate_ActivityNotFound` | Verifica que se lance una excepción al intentar actualizar una actividad inexistente. | `activityRepository.findById` devuelve `Optional.empty()`. | 1. Crear DTO con ID inexistente. 2. Configurar Mock. 3. Ejecutar `activityService.update(dto)`. | Se lanza una `Exception` al no encontrar la actividad (`assertThrows`). | OK |

---

# Notas

- Los casos marcados como **Comentado** existen en el código fuente, pero actualmente están comentados y requieren ser habilitados para su ejecución.
- Los casos marcados como **OK** corresponden a pruebas implementadas y funcionales.
- Las pruebas utilizan Mockito para simular el comportamiento de los repositorios y aislar la lógica de negocio del servicio.
