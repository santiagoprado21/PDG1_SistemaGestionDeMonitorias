# Diseño de Pruebas: `ActivityProgressServiceImpl`

Este documento describe los casos de prueba diseñados para verificar la funcionalidad de `ActivityProgressServiceImpl`, enfocándose en el registro de avances de actividades, validación de permisos, almacenamiento de evidencias y actualización automática de estados. Los tests unitarios correspondientes se encuentran en la clase `ActivityProgressServiceImplTest`.

**Objetivo:**  
Asegurar que `ActivityProgressServiceImpl` gestione correctamente el registro de progreso de actividades, la validación de usuarios autorizados (`Monitor` y `Professor`), el almacenamiento de evidencias, la actualización de estados de las actividades y el envío de notificaciones correspondientes.

**Alcance:**  
Pruebas unitarias de los métodos públicos de `ActivityProgressServiceImpl`, utilizando Mocks para las dependencias (`Repositories`, `NotificationService` y `ActivityEvidenceStorageService`). No se realizan interacciones reales con base de datos ni almacenamiento físico de archivos.

**Estrategia:**  
Pruebas de caja blanca y caja negra orientadas a:
- validación de reglas de negocio,
- control de acceso según roles,
- actualización de estados de actividades,
- manejo de evidencias,
- y verificación de notificaciones automáticas.

Las pruebas utilizan Mockito para simular dependencias y AssertJ/JUnit para validar resultados esperados y manejo de excepciones.

---

# Casos de Prueba

A continuación, se detallan los casos de prueba diseñados e implementados en `ActivityProgressServiceImplTest`:

---

## 1. Registro de Progreso con Evidencias

Estos casos verifican que un monitor asignado pueda registrar avances incluyendo archivos de evidencia.

| ID Caso | Nombre del Test (Método) | Descripción | Precondiciones (Mocks) | Pasos | Resultado Esperado | Estado |
| :-- | :-- | :-- | :-- | :-- | :-- | :-- |
| APR-001 | `registerProgress_asignedMonitorWithEvidence_updatesActivityAndNotifiesProgressUpdate` | Verifica que un monitor asignado pueda registrar progreso con evidencia adjunta y actualizar correctamente la actividad. | `activityRepository.findById` devuelve `Activity`. `monitorRepository.findByIdMonitor` devuelve `Monitor`. `progressRepository.save` retorna progreso persistido. `evidenceStorageService.store` almacena archivo correctamente. | 1. Crear `ActivityProgressRequestDTO`. 2. Simular archivo `MultipartFile`. 3. Registrar progreso mediante `registerProgress`. 4. Verificar actualización de actividad y notificaciones. | La actividad debe actualizar porcentaje, comentario, usuario responsable y estado `EN_PROGRESO`. La evidencia debe almacenarse correctamente y debe ejecutarse `notifyProgressUpdate`. | OK |

---

## 2. Finalización de Actividad dentro del Plazo

Estos casos verifican que una actividad completada antes de la fecha límite cambie correctamente a estado `COMPLETADO`.

| ID Caso | Nombre del Test (Método) | Descripción | Precondiciones (Mocks) | Pasos | Resultado Esperado | Estado |
| :-- | :-- | :-- | :-- | :-- | :-- | :-- |
| APR-002 | `registerProgress_monitorCompletesBeforeDeadline_setsCompletedStateAndNotifiesCompleted` | Verifica que una actividad finalizada antes de la fecha límite cambie al estado `COMPLETADO`. | `activityRepository.findById` devuelve `Activity` vigente. `monitorRepository.findByIdMonitor` devuelve `Monitor`. | 1. Crear payload con progreso al 100%. 2. Registrar progreso. 3. Verificar actualización de estado. | La actividad debe cambiar a estado `COMPLETADO`, registrar fecha de entrega y ejecutar `notifyCompleted`. | OK |

---

## 3. Finalización de Actividad Fuera del Plazo

Estos casos verifican el comportamiento cuando una actividad se entrega después de la fecha límite.

| ID Caso | Nombre del Test (Método) | Descripción | Precondiciones (Mocks) | Pasos | Resultado Esperado | Estado |
| :-- | :-- | :-- | :-- | :-- | :-- | :-- |
| APR-003 | `registerProgress_monitorCompletesAfterDeadline_setsCompletedTState` | Verifica que una actividad entregada fuera de tiempo cambie al estado `COMPLETADOT`. | `activityRepository.findById` devuelve actividad vencida. `monitorRepository.findByIdMonitor` devuelve `Monitor`. | 1. Crear actividad con fecha límite pasada. 2. Registrar progreso al 100%. 3. Validar estado final. | La actividad debe cambiar a estado `COMPLETADOT` y ejecutarse `notifyCompleted`. | OK |

---

## 4. Validación de Permisos de Usuario

Estos casos verifican que usuarios no autorizados no puedan registrar progreso en actividades.

| ID Caso | Nombre del Test (Método) | Descripción | Precondiciones (Mocks) | Pasos | Resultado Esperado | Estado |
| :-- | :-- | :-- | :-- | :-- | :-- | :-- |
| APR-004 | `registerProgress_whenMonitorNotAssigned_throwsException` | Verifica que se lance una excepción cuando un monitor no asignado intenta registrar progreso. | `activityRepository.findById` devuelve `Activity`. Usuario no corresponde al monitor asignado. | 1. Crear payload con usuario no autorizado. 2. Ejecutar `registerProgress`. | Se debe lanzar una excepción indicando que el usuario no está autorizado. No debe guardarse progreso ni enviarse notificaciones. | OK |

---

## 5. Actualización de Progreso por Profesor

Estos casos verifican que un profesor asociado pueda registrar progreso sobre la actividad.

| ID Caso | Nombre del Test (Método) | Descripción | Precondiciones (Mocks) | Pasos | Resultado Esperado | Estado |
| :-- | :-- | :-- | :-- | :-- | :-- | :-- |
| APR-005 | `registerProgress_professorCanUpdateProgress` | Verifica que un profesor autorizado pueda actualizar el progreso de una actividad. | `activityRepository.findById` devuelve `Activity`. `professorRepository.findById` devuelve `Professor`. | 1. Crear payload con rol `professor`. 2. Registrar progreso. 3. Validar actualización de datos. | La actividad debe actualizar correctamente el porcentaje, usuario responsable y rol asociado. Debe ejecutarse `notifyProgressUpdate`. | OK |

---

# Configuración de Datos de Prueba

Antes de ejecutar cada caso de prueba, se generan entidades de prueba mediante el método `setUp()`:

- `Activity`
- `Monitor`
- `Professor`
- `Monitoring`
- `Program`
- `Course`
- `School`

Estas entidades permiten simular escenarios completos de seguimiento de actividades académicas.

---

# Dependencias Utilizadas

Las pruebas integran los siguientes componentes simulados mediante Mockito:

| Componente | Tipo |
| :-- | :-- |
| `ActivityRepository` | Repository Mock |
| `ActivityProgressRepository` | Repository Mock |
| `MonitorRepository` | Repository Mock |
| `ProfessorRepository` | Repository Mock |
| `ProspectRepository` | Repository Mock |
| `ActivityEvidenceStorageService` | Service Mock |
| `NotificationService` | Service Mock |

---

# Notas

- Las pruebas utilizan `MockitoExtension` para inicializar automáticamente los mocks.
- Se utiliza `Strictness.LENIENT` para evitar errores por stubbings no utilizados en ciertos escenarios.
- Los archivos de evidencia son simulados mediante `MultipartFile`.
- Las fechas de entrega se manipulan dinámicamente para validar actividades entregadas dentro y fuera del plazo.
- Se verifica explícitamente el envío o no envío de notificaciones utilizando `verify()` y `never()`.
- Los estados validados incluyen:
  - `PENDIENTE`
  - `EN_PROGRESO`
  - `COMPLETADO`
  - `COMPLETADOT`