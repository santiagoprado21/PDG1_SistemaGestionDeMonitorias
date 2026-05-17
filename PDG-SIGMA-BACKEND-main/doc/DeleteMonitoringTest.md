# Diseño de Pruebas: `DeleteMonitoring`

Este documento describe los casos de prueba diseñados para verificar la funcionalidad de eliminación de monitorías implementada en `MonitoringServiceImpl`. Los tests unitarios correspondientes se encuentran en la clase `DeleteMonitoringTest`.

**Objetivo:**  
Asegurar que `MonitoringServiceImpl` gestione correctamente la eliminación de monitorías, validando la existencia de registros y la presencia de monitores asociados antes de ejecutar la eliminación.

**Alcance:**  
Pruebas unitarias sobre el método `deleteMonitoring`, utilizando mocks configurados con `MockitoExtension`. Se valida la interacción entre `MonitoringRepository` y `MonitoringMonitorRepository`.

**Estrategia:**  
Pruebas unitarias enfocadas en los diferentes escenarios de eliminación de monitorías, verificando:
- inexistencia de la monitoría,
- existencia de monitores asociados,
- y eliminación exitosa cuando no existen relaciones activas.

Las pruebas utilizan aserciones (`assertTrue`, `assertFalse`) y verificaciones de Mockito (`verify`, `never`) para validar el comportamiento esperado.

---

# Casos de Prueba

A continuación, se detallan los casos de prueba diseñados e implementados en `DeleteMonitoringTest`:

---

## 1. Eliminación de monitoría inexistente

Estos casos verifican que el sistema no intente eliminar una monitoría que no existe en el repositorio.

| ID Caso | Nombre del Test (Método) | Descripción | Precondiciones | Pasos | Resultado Esperado | Estado |
| :-- | :-- | :-- | :-- | :-- | :-- | :-- |
| DELMON-001 | `deleteMonitoring_shouldReturnFalse_whenMonitoringNotFound` | Verifica que el sistema retorne `false` cuando la monitoría no existe. | No existe una monitoría registrada con el ID solicitado. | 1. Simular búsqueda vacía en `MonitoringRepository`. 2. Ejecutar `deleteMonitoring`. 3. Validar resultado retornado. | El sistema debe retornar `false` y no debe ejecutar la eliminación en el repositorio. | OK |

---

## 2. Eliminación de monitoría con monitores asociados

Estos casos verifican que el sistema impida eliminar monitorías que tienen monitores relacionados.

| ID Caso | Nombre del Test (Método) | Descripción | Precondiciones | Pasos | Resultado Esperado | Estado |
| :-- | :-- | :-- | :-- | :-- | :-- | :-- |
| DELMON-002 | `deleteMonitoring_shouldReturnFalse_whenMonitoringHasMonitors` | Verifica que una monitoría no pueda eliminarse si tiene monitores asociados. | Existe una monitoría con registros asociados en `MonitoringMonitorRepository`. | 1. Simular monitoría existente. 2. Simular lista de monitores asociados. 3. Ejecutar `deleteMonitoring`. | El sistema debe retornar `false` y no debe eliminar la monitoría. | OK |

---

## 3. Eliminación exitosa de monitoría sin monitores asociados

Estos casos verifican que una monitoría pueda eliminarse correctamente cuando no existen relaciones activas.

| ID Caso | Nombre del Test (Método) | Descripción | Precondiciones | Pasos | Resultado Esperado | Estado |
| :-- | :-- | :-- | :-- | :-- | :-- | :-- |
| DELMON-003 | `deleteMonitoring_shouldReturnTrue_whenMonitoringExistsAndHasNoMonitors` | Verifica la eliminación exitosa de una monitoría sin monitores asociados. | Existe una monitoría válida sin relaciones activas en `MonitoringMonitorRepository`. | 1. Simular monitoría existente. 2. Simular lista vacía de monitores asociados. 3. Ejecutar `deleteMonitoring`. | El sistema debe retornar `true` y ejecutar correctamente la eliminación de la monitoría. | OK |

---

# Configuración de Datos de Prueba

Las pruebas utilizan mocks configurados mediante `MockitoExtension` para simular el comportamiento de las dependencias externas:

- `MonitoringRepository`
- `MonitoringMonitorRepository`

La lógica de negocio es validada utilizando simulaciones controladas mediante `Mockito.when()`.

---

# Dependencias Utilizadas

Las pruebas integran los siguientes componentes:

| Componente | Tipo |
| :-- | :-- |
| `MonitoringServiceImpl` | Servicio principal |
| `MonitoringRepository` | Repositorio |
| `MonitoringMonitorRepository` | Repositorio |

---

# Notas

- Las pruebas están configuradas con `@ExtendWith(MockitoExtension.class)` para habilitar mocks unitarios.
- Se utiliza `@InjectMocks` para inyectar automáticamente las dependencias simuladas en `MonitoringServiceImpl`.
- Se valida explícitamente que el método `delete()` no sea ejecutado en escenarios inválidos mediante `Mockito.verify(..., never())`.
- Las pruebas cubren tanto escenarios negativos como positivos del flujo de eliminación.
- Los casos implementados corresponden al flujo funcional de eliminación segura de monitorías.
