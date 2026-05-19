# Diseño de Pruebas: `DepartmentBudgetFeature`

Este documento describe los casos de prueba diseñados para verificar la funcionalidad relacionada con la gestión presupuestal de monitorías implementada mediante `MonitoringServiceImpl` y `DepartmentBudgetController`. Los tests de integración correspondientes se encuentran en la clase `DepartmentBudgetFeatureTest`.

**Objetivo:**  
Asegurar que el sistema gestione correctamente la asignación de horas y tarifas a monitorías, el control de presupuesto disponible por departamento y semestre, la actualización dinámica del presupuesto y las validaciones de negocio asociadas.

**Alcance:**  
Pruebas de integración sobre la funcionalidad presupuestal utilizando repositorios reales configurados en entorno de pruebas (`@SpringBootTest`, perfil `test`). Se valida la interacción entre entidades como `Monitoring`, `DepartmentBudget`, `Professor`, `Course`, `Program` y `School`.

**Estrategia:**  
Pruebas de integración basadas en flujos funcionales completos de negocio relacionados con presupuesto de monitorías, verificando:

- asignación de horas y valor hora,
- cálculo derivado de costos,
- control de límites presupuestales,
- actualización dinámica del presupuesto restante,
- validación de valores inválidos,
- preservación de datos existentes,
- y manejo de errores desde el controlador.

Las pruebas utilizan aserciones (`assertEquals`, `assertThrows`, `assertTrue`, `assertFalse`, `assertNotNull`) para validar resultados esperados y manejo de excepciones.

---

# Casos de Prueba

A continuación, se detallan los casos de prueba diseñados e implementados en `DepartmentBudgetFeatureTest`:

---

## 1. Asignación Correcta de Horas y Valor Hora

Estos casos verifican que el sistema permita definir horas estimadas y valor por hora, calculando correctamente el costo derivado.

| ID Caso | Nombre del Test (Método) | Descripción | Precondiciones | Pasos | Resultado Esperado | Estado |
| :-- | :-- | :-- | :-- | :-- | :-- | :-- |
| DBF-001 | `can_define_hours_and_rate_and_cost_is_derived` | Verifica que el sistema permita asignar horas y tarifa a una monitoría y calcular correctamente el costo total derivado. | Existe un presupuesto configurado para el programa y semestre. Existe una monitoría válida registrada. | 1. Configurar presupuesto disponible. 2. Crear monitoría. 3. Actualizar horas y valor hora. 4. Calcular costo derivado. | La monitoría debe almacenar correctamente las horas y tarifa. El costo derivado debe corresponder a `horas × valorHora`. | OK |

---

## 2. Validación de Exceso Presupuestal

Estos casos verifican que el sistema impida asignar horas que excedan el presupuesto restante.

| ID Caso | Nombre del Test (Método) | Descripción | Precondiciones | Pasos | Resultado Esperado | Estado |
| :-- | :-- | :-- | :-- | :-- | :-- | :-- |
| DBF-002 | `rejects_assigning_hours_over_remaining_budget` | Verifica que el sistema rechace asignaciones de horas que superen el presupuesto disponible del semestre. | Existe un presupuesto limitado configurado y una monitoría ya consume parte del presupuesto. | 1. Configurar presupuesto de 10 horas. 2. Asignar 8 horas a una monitoría. 3. Intentar asignar 5 horas a otra monitoría. | El sistema debe lanzar excepción indicando que no hay suficientes horas disponibles. | OK |

---

## 3. Actualización Dinámica del Presupuesto Restante

Estos casos verifican que el presupuesto restante se actualice automáticamente después de modificaciones.

| ID Caso | Nombre del Test (Método) | Descripción | Precondiciones | Pasos | Resultado Esperado | Estado |
| :-- | :-- | :-- | :-- | :-- | :-- | :-- |
| DBF-003 | `remaining_budget_updates_in_real_time` | Verifica que el sistema actualice dinámicamente las horas usadas y restantes del presupuesto. | Existe un presupuesto configurado y una monitoría válida. | 1. Configurar presupuesto de 20 horas. 2. Asignar 6 horas. 3. Consultar presupuesto. 4. Actualizar a 10 horas. 5. Consultar nuevamente. | El sistema debe reflejar correctamente las horas usadas y restantes después de cada actualización. | OK |

---

## 4. Validación de Valores Negativos

Estos casos verifican que el sistema rechace valores negativos para horas y valor hora.

| ID Caso | Nombre del Test (Método) | Descripción | Precondiciones | Pasos | Resultado Esperado | Estado |
| :-- | :-- | :-- | :-- | :-- | :-- | :-- |
| DBF-004 | `rejects_negative_values_for_hours_and_rate` | Verifica que el sistema no permita registrar horas ni tarifas negativas. | Existe una monitoría válida y presupuesto configurado. | 1. Intentar asignar horas negativas. 2. Intentar asignar tarifa negativa. | El sistema debe lanzar excepciones relacionadas con validación de horas y valor hora. | OK |

---

## 5. Límite Exacto de Presupuesto Permitido

Estos casos verifican que el sistema permita utilizar exactamente las horas restantes del presupuesto.

| ID Caso | Nombre del Test (Método) | Descripción | Precondiciones | Pasos | Resultado Esperado | Estado |
| :-- | :-- | :-- | :-- | :-- | :-- | :-- |
| DBF-005 | `boundary_equal_to_remaining_is_allowed` | Verifica que el sistema permita asignar exactamente las horas restantes del presupuesto disponible. | Existe un presupuesto parcialmente consumido. | 1. Configurar presupuesto de 10 horas. 2. Consumir 8 horas. 3. Asignar exactamente 2 horas restantes. | La asignación debe realizarse exitosamente y el presupuesto restante debe quedar en 0. | OK |

---

## 6. Actualización Parcial de Horas o Tarifa

Estos casos verifican que el sistema preserve valores existentes cuando solo se actualiza un campo.

| ID Caso | Nombre del Test (Método) | Descripción | Precondiciones | Pasos | Resultado Esperado | Estado |
| :-- | :-- | :-- | :-- | :-- | :-- | :-- |
| DBF-006 | `update_only_rate_or_only_hours_preserves_the_other_field` | Verifica que actualizar únicamente horas o tarifa preserve el valor previamente registrado del otro campo. | Existe una monitoría con horas y tarifa previamente configuradas. | 1. Configurar horas y tarifa iniciales. 2. Actualizar solo tarifa. 3. Actualizar solo horas. | El sistema debe mantener el valor no actualizado sin modificaciones. | OK |

---

## 7. Liberación de Presupuesto al Reducir Horas

Estos casos verifican que disminuir las horas asignadas libere presupuesto disponible automáticamente.

| ID Caso | Nombre del Test (Método) | Descripción | Precondiciones | Pasos | Resultado Esperado | Estado |
| :-- | :-- | :-- | :-- | :-- | :-- | :-- |
| DBF-007 | `decreasing_hours_frees_budget_and_is_reflected` | Verifica que reducir horas asignadas incremente automáticamente las horas restantes del presupuesto. | Existe una monitoría con horas previamente asignadas. | 1. Asignar 10 horas. 2. Consultar presupuesto restante. 3. Reducir a 5 horas. 4. Consultar nuevamente. | El presupuesto restante debe aumentar correctamente después de reducir las horas. | OK |

---

## 8. Manejo de Errores del Controlador de Presupuesto

Estos casos verifican las respuestas HTTP del controlador ante escenarios inválidos.

| ID Caso | Nombre del Test (Método) | Descripción | Precondiciones | Pasos | Resultado Esperado | Estado |
| :-- | :-- | :-- | :-- | :-- | :-- | :-- |
| DBF-008 | `budget_controller_returns_meaningful_errors` | Verifica que el controlador retorne códigos HTTP apropiados ante errores de consulta de presupuesto. | No existe presupuesto configurado o el programa consultado no existe. | 1. Consultar presupuesto inexistente. 2. Consultar programa inválido. | El sistema debe retornar `404` para presupuesto inexistente y `400` para programa inválido. | OK |

---

# Configuración de Datos de Prueba

Antes de ejecutar cada caso de prueba, se realiza la creación automática de entidades necesarias mediante el método `setup()`:

- `School`
- `Program`
- `Course`
- `Professor`
- `Monitoring`
- `DepartmentBudget`

Estas entidades son persistidas utilizando los repositorios configurados en el entorno de pruebas para garantizar independencia y aislamiento entre tests.

---

# Dependencias Utilizadas

Las pruebas integran los siguientes componentes:

| Componente | Tipo |
| :-- | :-- |
| `MonitoringServiceImpl` | Servicio principal |
| `DepartmentBudgetController` | Controlador |
| `MonitoringRepository` | Repositorio |
| `DepartmentBudgetRepository` | Repositorio |
| `ProfessorRepository` | Repositorio |
| `CourseRepository` | Repositorio |
| `ProgramRepository` | Repositorio |
| `SchoolRepository` | Repositorio |

---

# Notas

- Las pruebas están configuradas con `@Transactional`, por lo que los datos generados se revierten automáticamente al finalizar cada ejecución.
- Se utiliza el perfil `test` mediante `@ActiveProfiles("test")`.
- El envío de correos se encuentra desacoplado mediante `@MockBean JavaMailSender`.
- Las validaciones presupuestales dependen directamente de la lógica implementada en `updateMonitoringBudget`.
- Los cálculos de costo se realizan de manera derivada a partir de horas estimadas y valor por hora.
- Los casos implementados cubren validaciones de límites, consistencia presupuestal y actualización dinámica de recursos.
