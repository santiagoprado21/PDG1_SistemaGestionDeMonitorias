# Diseño de Pruebas: `ActivityScheduleService`

Este documento describe los casos de prueba diseñados para verificar la funcionalidad relacionada con la planificación de actividades académicas y validación de conflictos de horario implementados mediante `ActivityScheduleService`. Los tests de integración correspondientes se encuentran en la clase `ActivityPlanIntegrationTest`.

**Objetivo:**  
Asegurar que `ActivityScheduleService` gestione correctamente la creación de actividades programadas, la validación de conflictos de horario, la generación de planes de actividades y el cálculo de métricas asociadas a las monitorías.

**Alcance:**  
Pruebas de integración sobre los métodos principales de `ActivityScheduleService`, utilizando repositorios reales configurados en entorno de pruebas (`@SpringBootTest`, perfil `test`). Se valida la interacción entre entidades como `Activity`, `Monitoring`, `Professor`, `Monitor`, `Course`, `Program` y `School`.

**Estrategia:**  
Pruebas de integración basadas en flujos funcionales completos de negocio (HU-011), verificando:
- creación de actividades,
- detección de conflictos horarios,
- consolidación de planes de actividades,
- y recuperación de información agregada.

Las pruebas utilizan aserciones (`assertEquals`, `assertNotNull`, `assertThrows`, `assertTrue`, `assertFalse`) para validar resultados esperados y manejo de excepciones.

---

# Casos de Prueba

A continuación, se detallan los casos de prueba diseñados e implementados en `ActivityPlanIntegrationTest`:

---

## 1. Validación de Conflictos de Horario

Estos casos verifican que el sistema detecte correctamente solapamientos de horario entre actividades asociadas a una monitoría.

| ID Caso | Nombre del Test (Método) | Descripción | Precondiciones | Pasos | Resultado Esperado | Estado |
| :-- | :-- | :-- | :-- | :-- | :-- | :-- |
| APS-001 | `testConflictDetection` | Verifica que el sistema detecte conflictos de horario entre actividades solapadas. | Existen `Professor`, `Monitor`, `Monitoring`, `Course`, `Program` y `School` válidos registrados en la base de datos de prueba. | 1. Crear primera actividad con horario de 14:00 a 16:00. 2. Crear segunda actividad con horario de 15:00 a 17:00 en la misma fecha. 3. Validar conflictos mediante `validateScheduleConflicts`. 4. Intentar guardar la segunda actividad. | El sistema debe detectar al menos un conflicto horario. La creación de la segunda actividad debe lanzar una excepción con mensaje relacionado a conflicto. | OK |

---

## 2. Creación de Actividades sin Conflictos

Estos casos verifican que múltiples actividades puedan registrarse correctamente cuando no existen solapamientos horarios.

| ID Caso | Nombre del Test (Método) | Descripción | Precondiciones | Pasos | Resultado Esperado | Estado |
| :-- | :-- | :-- | :-- | :-- | :-- | :-- |
| APS-002 | `testMultipleActivitiesWithoutConflicts` | Verifica la creación exitosa de múltiples actividades sin conflictos de horario. | Existen entidades válidas de `Professor`, `Monitor` y `Monitoring`. | 1. Crear tres actividades en fechas distintas. 2. Guardar cada actividad mediante `saveActivityWithSchedule`. 3. Obtener el plan de actividades con `getActivityPlan`. | El plan debe contener 3 actividades, 6 horas totales, 3 actividades pendientes y 0 completadas. | OK |

---

## 3. Obtención de Plan Vacío

Estos casos verifican el comportamiento del sistema cuando una monitoría aún no posee actividades registradas.

| ID Caso | Nombre del Test (Método) | Descripción | Precondiciones | Pasos | Resultado Esperado | Estado |
| :-- | :-- | :-- | :-- | :-- | :-- | :-- |
| APS-003 | `testEmptyActivityPlan` | Verifica que el sistema retorne un plan vacío cuando no existen actividades asociadas a la monitoría. | Existe una `Monitoring` válida sin actividades asociadas. | 1. Obtener plan de actividades mediante `getActivityPlan`. 2. Validar métricas retornadas. | El plan debe existir y contener 0 actividades totales, 0 actividades pendientes, 0 actividades completadas y 0 horas totales. La lista de actividades debe estar vacía. | OK |

---

# Configuración de Datos de Prueba

Antes de ejecutar cada caso de prueba, se realiza la creación automática de entidades necesarias mediante el método `setUp()`:

- `Professor`
- `Monitor`
- `School`
- `Program`
- `Course`
- `Monitoring`

Estas entidades son persistidas utilizando los repositorios configurados en el entorno de pruebas para garantizar independencia y aislamiento entre tests.

---

# Dependencias Utilizadas

Las pruebas integran los siguientes componentes:

| Componente | Tipo |
| :-- | :-- |
| `ActivityScheduleService` | Servicio principal |
| `MonitoringRepository` | Repositorio |
| `ProfessorRepository` | Repositorio |
| `MonitorRepository` | Repositorio |
| `CourseRepository` | Repositorio |
| `ProgramRepository` | Repositorio |
| `SchoolRepository` | Repositorio |

---

# Notas

- Las pruebas están configuradas con `@Transactional`, por lo que los datos generados se revierten automáticamente al finalizar cada ejecución.
- Se utiliza el perfil `test` mediante `@ActiveProfiles("test")`.
- Las validaciones de conflicto dependen directamente de la lógica implementada en `validateScheduleConflicts`.
- Los métodos incluyen validaciones condicionales para evitar fallos si alguna dependencia no está disponible (`null checks`).
- Los casos implementados corresponden al flujo funcional completo de la historia de usuario HU-011.
