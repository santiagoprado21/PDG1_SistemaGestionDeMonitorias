# Diseño de Pruebas: AttendanceServiceImpl

Este documento describe los casos de prueba diseñados para verificar la funcionalidad de la clase `AttendanceServiceImpl`, enfocándose en la gestión de asistencias de estudiantes a actividades. Los tests unitarios correspondientes se encuentran en la clase `AttendanceServiceImplTest`.

**Objetivo:**  
Asegurar que `AttendanceServiceImpl` maneje correctamente el registro, consulta, actualización y eliminación de asistencias, así como la validación de entidades relacionadas (`Activity` y `Student`) y el manejo adecuado de errores.

**Alcance:**  
Pruebas unitarias de los métodos públicos de `AttendanceServiceImpl`, utilizando Mocks para las dependencias (`AttendanceRepository`, `ActivityRepository`, `StudentRepository`). No se prueban interacciones reales con la base de datos.

**Estrategia:**  
Pruebas de caja blanca basadas en la lógica del servicio y pruebas de caja negra enfocadas en los requerimientos funcionales. Se utilizan aserciones (`assertEquals`, `assertTrue`, `assertFalse`, `assertThrows`) y verificación de interacciones con Mockito.

---

# Casos de Prueba

A continuación, se detallan los casos de prueba diseñados e implementados en `AttendanceServiceImplTest`:

---

## 1. Registro de Asistencia (`save`)

Estos casos verifican el registro de asistencias y las validaciones necesarias antes de persistir la información.

| ID Caso | Nombre del Test (Método) | Descripción | Precondiciones (Mocks) | Pasos | Resultado Esperado | Estado |
| :-- | :-- | :-- | :-- | :-- | :-- | :-- |
| ATT-001 | `testSave_Success` | Verifica el registro exitoso de una asistencia. | `activityRepository.findById` devuelve `Activity`. `studentRepository.findById` devuelve `Student`. `attendanceRepository.save` devuelve asistencia guardada. | 1. Crear `Attendance`. 2. Configurar Mocks. 3. Ejecutar `attendanceService.save()`. | Se devuelve una asistencia válida con la actividad y estudiante correctos. | OK |
| ATT-002 | `testSave_ActivityNotFound` | Verifica que se lance excepción si la actividad no existe. | `activityRepository.findById` devuelve `Optional.empty()`. | 1. Crear `Attendance`. 2. Ejecutar `attendanceService.save()`. | Se lanza `RuntimeException` con mensaje `"Actividad no encontrada con ID: 1"`. | OK |
| ATT-003 | `testSave_StudentNotFound` | Verifica que se lance excepción si el estudiante no existe. | `activityRepository.findById` devuelve `Activity`. `studentRepository.findById` devuelve `Optional.empty()`. | 1. Crear `Attendance`. 2. Ejecutar `attendanceService.save()`. | Se lanza `RuntimeException` con mensaje `"Estudiante no encontrado con ID: S123"`. | OK |

---

## 2. Consulta de Asistencias

Estos casos verifican las búsquedas y consultas de asistencias registradas.

| ID Caso | Nombre del Test (Método) | Descripción | Precondiciones (Mocks) | Pasos | Resultado Esperado | Estado |
| :-- | :-- | :-- | :-- | :-- | :-- | :-- |
| ATT-004 | `testFindByActivity` | Verifica la búsqueda de asistencias por actividad. | `attendanceRepository.findByActivityId` devuelve lista de asistencias. | Ejecutar `attendanceService.findByActivity(activityId)`. | Retorna lista de asistencias con tamaño esperado. | OK |
| ATT-005 | `testFindByActivityAndStudent_Found` | Verifica la búsqueda de asistencia por actividad y estudiante cuando existe. | `attendanceRepository.findByActivityIdAndStudentCode` devuelve `Optional.of(attendance)`. | Ejecutar `attendanceService.findByActivityAndStudent(activityId, studentId)`. | Retorna `Optional` con la asistencia encontrada. | OK |
| ATT-006 | `testFindByActivityAndStudent_NotFound` | Verifica la búsqueda de asistencia por actividad y estudiante cuando no existe. | `attendanceRepository.findByActivityIdAndStudentCode` devuelve `Optional.empty()`. | Ejecutar `attendanceService.findByActivityAndStudent(activityId, studentId)`. | Retorna `Optional.empty()`. | OK |
| ATT-007 | `testFindAll` | Verifica la consulta de todas las asistencias registradas. | `attendanceRepository.findAll` devuelve lista de asistencias. | Ejecutar `attendanceService.findAll()`. | Retorna lista completa de asistencias. | OK |
| ATT-008 | `testFindById_Found` | Verifica la búsqueda de asistencia por ID cuando existe. | `attendanceRepository.findById` devuelve `Optional.of(attendance)`. | Ejecutar `attendanceService.findById(id)`. | Retorna `Optional` con la asistencia encontrada. | OK |
| ATT-009 | `testFindById_NotFound` | Verifica la búsqueda de asistencia por ID cuando no existe. | `attendanceRepository.findById` devuelve `Optional.empty()`. | Ejecutar `attendanceService.findById(id)`. | Retorna `Optional.empty()`. | OK |

---

## 3. Actualización de Asistencia (`update`)

Estos casos verifican la actualización de registros de asistencia.

| ID Caso | Nombre del Test (Método) | Descripción | Precondiciones (Mocks) | Pasos | Resultado Esperado | Estado |
| :-- | :-- | :-- | :-- | :-- | :-- | :-- |
| ATT-010 | `testUpdate` | Verifica la actualización exitosa de una asistencia. | `attendanceRepository.save` devuelve asistencia actualizada. | 1. Crear asistencia con ID. 2. Ejecutar `attendanceService.update()`. | Retorna la asistencia actualizada correctamente. | OK |

---

## 4. Eliminación de Asistencia (`delete` y `deleteById`)

Estos casos verifican la eliminación de asistencias registradas.

| ID Caso | Nombre del Test (Método) | Descripción | Precondiciones (Mocks) | Pasos | Resultado Esperado | Estado |
| :-- | :-- | :-- | :-- | :-- | :-- | :-- |
| ATT-011 | `testDelete` | Verifica la eliminación de una asistencia mediante entidad. | - | Ejecutar `attendanceService.delete(attendance)`. | Se invoca `attendanceRepository.delete()` una vez. | OK |
| ATT-012 | `testDeleteById` | Verifica la eliminación de una asistencia mediante ID. | - | Ejecutar `attendanceService.deleteById(id)`. | Se invoca `attendanceRepository.deleteById(id)` una vez. | OK |

---

## 5. Conteo de Asistencias (`count`)

Estos casos verifican la obtención del número total de asistencias registradas.

| ID Caso | Nombre del Test (Método) | Descripción | Precondiciones (Mocks) | Pasos | Resultado Esperado | Estado |
| :-- | :-- | :-- | :-- | :-- | :-- | :-- |
| ATT-013 | `testCount` | Verifica el conteo total de asistencias registradas. | `attendanceRepository.count` devuelve `5L`. | Ejecutar `attendanceService.count()`. | Retorna el valor `5L`. | OK |

---

# Notas

- Las pruebas utilizan Mockito para simular el comportamiento de los repositorios y aislar la lógica de negocio del servicio.
- Los casos cubren operaciones CRUD completas sobre la entidad `Attendance`.
- Se validan escenarios exitosos y de error mediante excepciones controladas.
- Se emplean aserciones de JUnit y verificaciones de interacción (`verify`) para comprobar el comportamiento esperado del servicio.
