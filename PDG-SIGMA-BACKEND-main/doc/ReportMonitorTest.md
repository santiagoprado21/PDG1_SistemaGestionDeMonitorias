# Diseño de Pruebas: `ReportMonitorTest` - Reportes de Monitores

Este documento describe los casos de prueba diseñados para verificar la funcionalidad relacionada con la generación de reportes de monitores implementada mediante `MonitoringServiceImpl`. Los tests correspondientes se encuentran en la clase `ReportMonitorTest`.

**Objetivo:**  
Asegurar que `MonitoringServiceImpl` gestione correctamente la generación de reportes académicos de monitores, incluyendo validaciones de usuarios inexistentes y consolidación de métricas de actividades.

**Alcance:**  
Pruebas unitarias sobre el método `getReportMonitors`, utilizando mocks configurados mediante `Mockito` para validar la lógica de generación de reportes según el rol consultado.

**Estrategia:**  
Pruebas orientadas a verificar:
- generación correcta de reportes de monitores,
- conteo de actividades según estado,
- validación de jefes inexistentes,
- consolidación de información académica y administrativa.

Las pruebas utilizan `JUnit 5`, `Mockito` y aserciones (`assertEquals`, `assertThrows`) para validar comportamiento esperado y manejo de excepciones.

---

# Casos de Prueba

A continuación, se detallan los casos de prueba implementados en `ReportMonitorTest`:

---

## 1. Generación Exitosa de Reporte de Monitores para Profesor

Estos casos verifican que el sistema genere correctamente reportes de monitores asociados a las monitorías de un profesor.

| ID Caso | Nombre del Test (Método) | Descripción | Precondiciones | Pasos | Resultado Esperado | Estado |
| :-- | :-- | :-- | :-- | :-- | :-- | :-- |
| RMT-001 | `testGetReportMonitorsWithProfessor` | Verifica que el sistema genere correctamente el reporte de monitores asociados a un profesor. | Existe un profesor válido con monitorías, monitores y actividades asociadas. | 1. Simular profesor, monitoría, monitor y actividades. 2. Ejecutar `getReportMonitors`. 3. Validar métricas y datos retornados. | El sistema debe retornar correctamente información del monitor, curso, programa, profesor, semestre y métricas de actividades pendientes, completadas y atrasadas. | OK |

---

## 2. Jefe de Departamento No Encontrado

Estos casos verifican el comportamiento cuando se intenta generar un reporte utilizando un jefe inexistente.

| ID Caso | Nombre del Test (Método) | Descripción | Precondiciones | Pasos | Resultado Esperado | Estado |
| :-- | :-- | :-- | :-- | :-- | :-- | :-- |
| RMT-002 | `testGetReportMonitorsWithDepartmentHeadNotFound` | Verifica que el sistema maneje correctamente la consulta de un jefe inexistente. | No existe un jefe asociado al ID consultado. | 1. Simular respuesta vacía del repositorio. 2. Ejecutar `getReportMonitors`. 3. Capturar excepción generada. | El sistema debe lanzar una excepción con el mensaje `"No existe un jefe con este Id"`. | OK |

---

# Configuración de Datos de Prueba

Antes de ejecutar las pruebas, se configuran entidades mock relacionadas con el flujo académico:

- `Professor`
- `Monitoring`
- `Course`
- `Program`
- `Monitor`
- `MonitoringMonitor`
- `Activity`

Las actividades utilizadas poseen distintos estados:
- `PENDIENTE`
- `COMPLETADO`
- `COMPLETADOT`

Las dependencias son simuladas utilizando `Mockito` para aislar la lógica del servicio.

---

# Dependencias Utilizadas

Las pruebas integran los siguientes componentes:

| Componente | Tipo |
| :-- | :-- |
| `MonitoringServiceImpl` | Servicio principal |
| `MonitoringRepository` | Repositorio mockeado |
| `ProfessorRepository` | Repositorio mockeado |
| `MonitoringMonitorRepository` | Repositorio mockeado |
| `ActivityRepository` | Repositorio mockeado |
| `DepartmentHeadRepository` | Repositorio mockeado |
| `HeadProgramRepository` | Repositorio mockeado |
| `CourseRepository` | Repositorio mockeado |
| `CourseProfessorRepository` | Repositorio mockeado |
| `Mockito` | Framework de mocks |
| `JUnit 5` | Framework de pruebas |

---

# Notas

- Las pruebas utilizan `@ExtendWith(MockitoExtension.class)` para habilitar soporte de Mockito.
- El método `getReportMonitors` genera reportes dependiendo del rol recibido como parámetro.
- Se validan correctamente métricas de actividades:
  - pendientes,
  - completadas,
  - atrasadas.
- Las excepciones son verificadas utilizando `assertThrows`.
- Los casos implementados cubren escenarios positivos y negativos del flujo de reportes académicos de monitores.
