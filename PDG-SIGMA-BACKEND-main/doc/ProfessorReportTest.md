# Diseño de Pruebas: `MonitoringServiceImpl` - Reportes de Profesor

Este documento describe los casos de prueba diseñados para verificar la funcionalidad relacionada con la generación de reportes académicos de profesores implementada mediante `MonitoringServiceImpl`. Los tests correspondientes se encuentran en la clase `ProfessorReportTest`.

**Objetivo:**  
Asegurar que `MonitoringServiceImpl` gestione correctamente la generación de reportes de actividades asociadas a monitorías de profesores, incluyendo validaciones de profesores inexistentes y ausencia de monitorías registradas.

**Alcance:**  
Pruebas unitarias sobre el método `getProfessorReport`, utilizando mocks configurados mediante `Mockito` para validar la lógica de consolidación de actividades y generación de métricas académicas.

**Estrategia:**  
Pruebas enfocadas en:
- generación correcta de reportes,
- conteo de actividades según estado,
- validación de profesores inexistentes,
- validación de ausencia de monitorías.

Las pruebas utilizan `JUnit 5`, `Mockito` y aserciones (`assertEquals`, `assertThrows`) para validar comportamiento esperado y manejo de excepciones.

---

# Casos de Prueba

A continuación, se detallan los casos de prueba implementados en `ProfessorReportTest`:

---

## 1. Generación Exitosa del Reporte del Profesor

Estos casos verifican que el sistema genere correctamente un reporte consolidado de actividades asociadas a las monitorías de un profesor.

| ID Caso | Nombre del Test (Método) | Descripción | Precondiciones | Pasos | Resultado Esperado | Estado |
| :-- | :-- | :-- | :-- | :-- | :-- | :-- |
| PRT-001 | `testGetProfessorReport_Success` | Verifica que el sistema genere correctamente el reporte de actividades del profesor. | Existe un profesor válido con monitorías y actividades asociadas. | 1. Simular profesor, monitoría y actividades con distintos estados. 2. Ejecutar `getProfessorReport`. 3. Validar métricas generadas. | El sistema debe retornar un reporte con 1 actividad pendiente, 1 completada y 1 atrasada, incluyendo correctamente datos del profesor, curso y programa. | OK |

---

## 2. Profesor No Encontrado

Estos casos verifican el comportamiento cuando se intenta generar un reporte para un profesor inexistente.

| ID Caso | Nombre del Test (Método) | Descripción | Precondiciones | Pasos | Resultado Esperado | Estado |
| :-- | :-- | :-- | :-- | :-- | :-- | :-- |
| PRT-002 | `testGetProfessorReport_NoProfessorFound` | Verifica que el sistema maneje correctamente un profesor inexistente. | No existe un profesor asociado al ID consultado. | 1. Simular respuesta vacía del repositorio. 2. Ejecutar `getProfessorReport`. 3. Capturar excepción generada. | El sistema debe lanzar una excepción con el mensaje `"No existe professor con este id"`. | OK |

---

## 3. Profesor sin Monitorías

Estos casos verifican el comportamiento cuando un profesor no posee monitorías registradas.

| ID Caso | Nombre del Test (Método) | Descripción | Precondiciones | Pasos | Resultado Esperado | Estado |
| :-- | :-- | :-- | :-- | :-- | :-- | :-- |
| PRT-003 | `testGetProfessorReport_NoMonitorings` | Verifica que el sistema informe correctamente cuando un profesor no tiene monitorías creadas. | Existe un profesor válido sin monitorías asociadas. | 1. Simular profesor válido. 2. Simular lista vacía de monitorías. 3. Ejecutar `getProfessorReport`. | El sistema debe lanzar una excepción con el mensaje `"No hay monitorías creadas"`. | OK |

---

# Configuración de Datos de Prueba

Antes de ejecutar cada prueba, se configuran entidades mock mediante el método `setup()`:

- `Professor`
- `Program`
- `Course`
- `Monitoring`

Además, se crean actividades con diferentes estados:
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
| `ProfessorRepository` | Repositorio mockeado |
| `MonitoringRepository` | Repositorio mockeado |
| `ActivityRepository` | Repositorio mockeado |
| `Mockito` | Framework de mocks |
| `JUnit 5` | Framework de pruebas |

---

# Notas

- Las pruebas utilizan `@ExtendWith(MockitoExtension.class)` para habilitar soporte de Mockito.
- Se validan correctamente distintos estados de actividades para generar métricas del reporte.
- El cálculo del reporte depende de las actividades obtenidas mediante:
  - `findByProfessorAndRoleResponsable`
  - `findByProfessorAndRoleCreator`
- Los casos implementados cubren escenarios positivos y negativos del flujo de reportes académicos.
- Las excepciones son verificadas utilizando `assertThrows`.
