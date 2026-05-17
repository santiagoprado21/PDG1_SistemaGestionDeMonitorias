# Diseño de Pruebas: `SupervisorEvaluationService`

Este documento describe los casos de prueba diseñados para verificar la funcionalidad relacionada con la evaluación de supervisores implementada mediante `SupervisorEvaluationServiceImpl`. Los tests unitarios correspondientes se encuentran en la clase `SupervisorEvaluationServiceTest`.

**Objetivo:**  
Asegurar que `SupervisorEvaluationServiceImpl` gestione correctamente la creación de evaluaciones de supervisión, el cálculo de puntajes de desempeño, las validaciones de rango y el almacenamiento de evaluaciones asociadas a monitorías y monitores.

**Alcance:**  
Pruebas unitarias sobre los métodos principales de `SupervisorEvaluationServiceImpl`, utilizando dependencias simuladas (`Mockito`) para validar el comportamiento de negocio sin necesidad de conectarse a una base de datos real.

**Estrategia:**  
Pruebas unitarias basadas en flujos funcionales completos de evaluación de supervisión, verificando:

- creación de evaluaciones,
- validación de rangos permitidos,
- cálculo de puntajes,
- clasificación del desempeño,
- y persistencia de información.

Las pruebas utilizan aserciones (`assertEquals`, `assertNotNull`, `assertThrows`) para validar resultados esperados y manejo de excepciones.

---

# Casos de Prueba

A continuación, se detallan los casos de prueba diseñados e implementados en `SupervisorEvaluationServiceTest`:

---

## 1. Creación Exitosa de Evaluación

Estos casos verifican que el sistema cree correctamente una evaluación de supervisor cuando toda la información es válida.

| ID Caso | Nombre del Test (Método) | Descripción | Precondiciones | Pasos | Resultado Esperado | Estado |
| :-- | :-- | :-- | :-- | :-- | :-- | :-- |
| SES-001 | `createEvaluation_successful` | Verifica que una evaluación de supervisor se cree exitosamente y se almacene correctamente. | Existen entidades válidas de `Monitoring`, `Monitor` y `Professor`. No existe una evaluación previa asociada a la misma monitoría y monitor. | 1. Crear objeto `SupervisorEvaluationRequest` con puntuaciones válidas. 2. Configurar mocks de repositorios. 3. Ejecutar `createEvaluation`. 4. Capturar y validar la evaluación guardada. | El sistema debe retornar una evaluación válida con ID generado, puntaje total calculado correctamente, nivel de desempeño `"EXCELENTE"` y comentarios almacenados. La evaluación debe persistirse correctamente en el repositorio. | OK |

---

## 2. Validación de Puntajes Fuera de Rango

Estos casos verifican que el sistema rechace evaluaciones cuando alguno de los puntajes se encuentra fuera del rango permitido.

| ID Caso | Nombre del Test (Método) | Descripción | Precondiciones | Pasos | Resultado Esperado | Estado |
| :-- | :-- | :-- | :-- | :-- | :-- | :-- |
| SES-002 | `createEvaluation_rejectsScoreOutOfRange` | Verifica que el sistema lance una excepción cuando una puntuación supera el rango permitido entre 1 y 7. | Existe una solicitud de evaluación con valores inválidos. | 1. Crear objeto `SupervisorEvaluationRequest` con puntuación inválida (`8`). 2. Ejecutar `createEvaluation`. 3. Capturar la excepción generada. | El sistema debe lanzar una excepción indicando que el valor debe encontrarse entre 1 y 7. El mensaje debe contener información relacionada con el criterio evaluado. | OK |

---

# Configuración de Datos de Prueba

Antes de ejecutar cada caso de prueba, se realiza la configuración de entidades simuladas (`mock`) mediante Mockito:

- `Professor`
- `Monitor`
- `Monitoring`
- `SupervisorEvaluation`

Los repositorios son simulados utilizando anotaciones `@Mock`, permitiendo aislar completamente la lógica del servicio durante la ejecución de las pruebas.

---

# Dependencias Utilizadas

Las pruebas integran los siguientes componentes:

| Componente | Tipo |
| :-- | :-- |
| `SupervisorEvaluationServiceImpl` | Servicio principal |
| `SupervisorEvaluationRepository` | Repositorio |
| `MonitoringRepository` | Repositorio |
| `MonitoringMonitorRepository` | Repositorio |
| `MonitorRepository` | Repositorio |
| `ProfessorRepository` | Repositorio |
| `MockitoExtension` | Framework de pruebas |

---

# Notas

- Las pruebas utilizan `@ExtendWith(MockitoExtension.class)` para habilitar la integración con Mockito.
- Los repositorios son simulados mediante `@Mock`.
- El servicio bajo prueba es inyectado mediante `@InjectMocks`.
- Se utiliza `ArgumentCaptor` para validar los datos persistidos en el repositorio.
- Las validaciones de rango verifican que los valores estén comprendidos entre 1 y 7.
- El cálculo del desempeño depende directamente de la lógica implementada en `SupervisorEvaluationServiceImpl`.
- Los casos implementados corresponden al flujo funcional completo de evaluación de supervisores.
