# Diseño de Pruebas: `NotificationRepository`

Este documento describe los casos de prueba diseñados para verificar la funcionalidad relacionada con la persistencia y consulta de notificaciones implementadas mediante `NotificationRepository`. Los tests correspondientes se encuentran en la clase `NotificationRepositoryTest`.

**Objetivo:**  
Asegurar que `NotificationRepository` gestione correctamente la búsqueda de notificaciones no leídas, el ordenamiento por fecha de creación y la validación de existencia de notificaciones pendientes asociadas a actividades académicas.

**Alcance:**  
Pruebas de persistencia utilizando `@DataJpaTest` sobre las operaciones principales del repositorio `NotificationRepository`, validando el comportamiento de la entidad `Notification` y sus consultas derivadas en entorno de pruebas configurado con el perfil `test`.

**Estrategia:**  
Pruebas de integración orientadas a validar:
- filtrado de notificaciones no leídas,
- ordenamiento descendente por fecha,
- exclusión de registros de otros profesores,
- y verificación de existencia de notificaciones pendientes.

Las pruebas utilizan aserciones de AssertJ (`assertThat`) para validar resultados esperados y consistencia de los datos persistidos.

---

# Casos de Prueba

A continuación, se detallan los casos de prueba diseñados e implementados en `NotificationRepositoryTest`:

---

## 1. Consulta de Notificaciones No Leídas

Estos casos verifican que el repositorio retorne únicamente las notificaciones no leídas asociadas al profesor indicado y ordenadas correctamente por fecha de creación.

| ID Caso | Nombre del Test (Método) | Descripción | Precondiciones | Pasos | Resultado Esperado | Estado |
| :-- | :-- | :-- | :-- | :-- | :-- | :-- |
| NRR-001 | `findUnreadByProfessorReturnsOnlyUnreadSortedDesc` | Verifica que el repositorio filtre únicamente notificaciones no leídas del profesor indicado y las ordene de forma descendente por fecha de creación. | Existen múltiples notificaciones registradas con diferentes estados (`readFlag`) y profesores asociados. | 1. Crear notificaciones para el profesor `"P1"` con distintas fechas. 2. Marcar una notificación como leída. 3. Crear notificación adicional para otro profesor (`"P2"`). 4. Ejecutar `findByProfessorIdAndReadFlagFalseOrderByCreatedAtDesc("P1")`. 5. Validar resultados obtenidos. | El sistema debe retornar únicamente las notificaciones no leídas de `"P1"` y ordenarlas de la más reciente a la más antigua. Las notificaciones leídas o pertenecientes a otros profesores no deben incluirse. | OK |

---

## 2. Validación de Existencia de Notificaciones Pendientes

Estos casos verifican que el repositorio identifique correctamente si existe una notificación pendiente asociada a una actividad específica.

| ID Caso | Nombre del Test (Método) | Descripción | Precondiciones | Pasos | Resultado Esperado | Estado |
| :-- | :-- | :-- | :-- | :-- | :-- | :-- |
| NRR-002 | `existsByProfessorAndActivityIdOnlyCountsUnread` | Verifica que el método de existencia considere únicamente notificaciones no leídas. | Existe una notificación no leída asociada al profesor `"P3"` y actividad `100`. | 1. Crear notificación no leída para `"P3"` y actividad `100`. 2. Validar existencia mediante `existsByProfessorIdAndActivityIdAndReadFlagFalse`. 3. Marcar notificación como leída. 4. Guardar cambios. 5. Validar nuevamente existencia. | El sistema debe retornar `true` mientras la notificación esté pendiente y `false` después de marcarla como leída. | OK |

---

# Configuración de Datos de Prueba

Antes de ejecutar cada caso de prueba, el entorno de persistencia es inicializado automáticamente mediante `@DataJpaTest`.

Las entidades utilizadas incluyen:

- `Notification`
- `NotificationType`

Los datos son almacenados en una base de datos de prueba aislada para garantizar independencia entre ejecuciones.

---

# Dependencias Utilizadas

Las pruebas integran los siguientes componentes:

| Componente | Tipo |
| :-- | :-- |
| `NotificationRepository` | Repositorio |
| `Notification` | Entidad de dominio |
| `NotificationType` | Enumeración |
| `AssertJ` | Framework de aserciones |
| `@DataJpaTest` | Configuración de pruebas JPA |

---

# Notas

- Las pruebas utilizan `@DataJpaTest` para cargar únicamente el contexto relacionado con persistencia JPA.
- El perfil `test` es activado mediante `@ActiveProfiles("test")`.
- Las operaciones de persistencia son ejecutadas sobre una base de datos de pruebas aislada.
- Las validaciones se realizan utilizando AssertJ mediante `assertThat`.
- Las consultas probadas corresponden a métodos derivados de Spring Data JPA.
- Los casos implementados validan filtrado condicional, ordenamiento y existencia de registros pendientes.
