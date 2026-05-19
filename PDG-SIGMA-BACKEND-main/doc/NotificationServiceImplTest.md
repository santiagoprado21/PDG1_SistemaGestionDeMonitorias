# Diseño de Pruebas: `NotificationServiceImpl`

Este documento describe los casos de prueba diseñados para verificar la funcionalidad relacionada con la gestión de notificaciones académicas implementadas mediante `NotificationServiceImpl`. Los tests correspondientes se encuentran en la clase `NotificationServiceImplTest`.

**Objetivo:**  
Asegurar que `NotificationServiceImpl` gestione correctamente la creación de notificaciones, el respeto de preferencias configuradas por el profesor, la prevención de duplicados y el marcado de notificaciones como leídas.

**Alcance:**  
Pruebas de integración utilizando `@DataJpaTest` sobre la lógica principal del servicio `NotificationServiceImpl`, validando la interacción entre:
- `NotificationRepository`
- `NotificationPreferenceRepository`
- `NotificationPreferenceServiceImpl`
- `Activity`
- `Professor`
- `Monitor`
- `Notification`

**Estrategia:**  
Pruebas de integración orientadas a validar:
- creación de notificaciones de progreso,
- aplicación de preferencias de notificación,
- prevención de duplicados en notificaciones vencidas,
- y actualización del conteo de notificaciones no leídas.

Las pruebas utilizan aserciones de AssertJ (`assertThat`) para validar resultados esperados y comportamiento del sistema.

---

# Casos de Prueba

A continuación, se detallan los casos de prueba diseñados e implementados en `NotificationServiceImplTest`:

---

## 1. Creación de Notificaciones de Progreso

Estos casos verifican que el sistema genere correctamente notificaciones de actualización de progreso cuando las preferencias están habilitadas.

| ID Caso | Nombre del Test (Método) | Descripción | Precondiciones | Pasos | Resultado Esperado | Estado |
| :-- | :-- | :-- | :-- | :-- | :-- | :-- |
| NSI-001 | `createsProgressNotificationWhenEnabled` | Verifica que el sistema cree una notificación de progreso cuando la preferencia correspondiente está habilitada. | Existe una actividad válida asociada a un profesor con preferencias activas. | 1. Crear actividad base. 2. Ejecutar `notifyProgressUpdate`. 3. Consultar notificaciones almacenadas. | El sistema debe registrar una notificación de tipo `PROGRESS_UPDATE`. | OK |

---

## 2. Validación de Preferencias Deshabilitadas

Estos casos verifican que las preferencias configuradas por el profesor controlen correctamente la generación de nuevas notificaciones.

| ID Caso | Nombre del Test (Método) | Descripción | Precondiciones | Pasos | Resultado Esperado | Estado |
| :-- | :-- | :-- | :-- | :-- | :-- | :-- |
| NSI-002 | `respectsDisabledCompletedPreference` | Verifica que el sistema no genere notificaciones de actividades completadas cuando la preferencia correspondiente está deshabilitada. | Existe una preferencia registrada con `enableCompleted = false`. | 1. Deshabilitar preferencia `enableCompleted`. 2. Guardar preferencias actualizadas. 3. Ejecutar `notifyCompleted`. 4. Consultar notificaciones registradas. | El sistema no debe registrar nuevas notificaciones cuando la preferencia está deshabilitada. | OK |

---

## 3. Prevención de Notificaciones Duplicadas

Estos casos verifican que el sistema evite registrar notificaciones duplicadas para actividades vencidas.

| ID Caso | Nombre del Test (Método) | Descripción | Precondiciones | Pasos | Resultado Esperado | Estado |
| :-- | :-- | :-- | :-- | :-- | :-- | :-- |
| NSI-003 | `overdueNotificationDeduplicates` | Verifica que el sistema no genere múltiples notificaciones vencidas para la misma actividad. | Existe una actividad válida registrada. | 1. Ejecutar `notifyOverdue` por primera vez. 2. Ejecutar nuevamente `notifyOverdue` para la misma actividad. 3. Consultar notificaciones almacenadas. | El sistema debe registrar únicamente una notificación de tipo `OVERDUE`. | OK |

---

## 4. Marcado Masivo de Notificaciones como Leídas

Estos casos verifican que el sistema actualice correctamente el estado de lectura y el conteo de notificaciones pendientes.

| ID Caso | Nombre del Test (Método) | Descripción | Precondiciones | Pasos | Resultado Esperado | Estado |
| :-- | :-- | :-- | :-- | :-- | :-- | :-- |
| NSI-004 | `markAllAsReadClearsUnreadCount` | Verifica que el sistema marque todas las notificaciones como leídas y actualice el conteo de pendientes. | Existen múltiples notificaciones no leídas registradas para el profesor. | 1. Crear notificaciones de progreso y completadas. 2. Consultar conteo inicial de pendientes. 3. Ejecutar `markAllAsRead`. 4. Consultar nuevamente el conteo. | El sistema debe retornar un conteo inicial de 2 notificaciones y posteriormente 0 después del marcado masivo. | OK |

---

# Configuración de Datos de Prueba

Antes de ejecutar cada caso de prueba, se realiza la creación automática de entidades necesarias mediante el método `setup()`:

- `Professor`
- `Monitor`
- `Activity`
- `NotificationPreference`

Estas entidades son persistidas utilizando los repositorios configurados en el entorno de pruebas para garantizar independencia y aislamiento entre tests.

---

# Dependencias Utilizadas

Las pruebas integran los siguientes componentes:

| Componente | Tipo |
| :-- | :-- |
| `NotificationServiceImpl` | Servicio principal |
| `NotificationPreferenceServiceImpl` | Servicio auxiliar |
| `NotificationRepository` | Repositorio |
| `NotificationPreferenceRepository` | Repositorio |
| `Activity` | Entidad de dominio |
| `Professor` | Entidad de dominio |
| `Monitor` | Entidad de dominio |
| `NotificationPreference` | Entidad de dominio |
| `AssertJ` | Framework de aserciones |

---

# Notas

- Las pruebas utilizan `@DataJpaTest` para cargar el contexto relacionado con persistencia JPA.
- El perfil `test` es activado mediante `@ActiveProfiles("test")`.
- Los servicios `NotificationServiceImpl` y `NotificationPreferenceServiceImpl` son cargados mediante `@Import`.
- Las operaciones de persistencia son ejecutadas sobre una base de datos de pruebas aislada.
- Las validaciones se realizan utilizando AssertJ mediante `assertThat`.
- Los casos implementados validan tanto lógica de negocio como persistencia de notificaciones académicas.
