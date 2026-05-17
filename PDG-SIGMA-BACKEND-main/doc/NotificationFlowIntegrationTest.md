# Diseño de Pruebas: `NotificationService` y `NotificationController`

Este documento describe los casos de prueba diseñados para verificar la funcionalidad relacionada con la gestión de notificaciones académicas implementadas mediante `NotificationService` y expuestas a través de `NotificationController`. Los tests de integración correspondientes se encuentran en la clase `NotificationFlowIntegrationTest`.

**Objetivo:**  
Asegurar que el sistema gestione correctamente la creación de notificaciones, consulta de notificaciones no leídas, conteo de pendientes, marcado de lectura y validación de preferencias de notificación asociadas a profesores.

**Alcance:**  
Pruebas de integración sobre el flujo completo de notificaciones utilizando `@SpringBootTest` y `MockMvc`, validando la interacción entre:
- `NotificationServiceImpl`
- `NotificationPreferenceRepository`
- `Activity`
- `Professor`
- `Monitor`
- `NotificationPreference`

**Estrategia:**  
Pruebas de integración basadas en el flujo funcional completo de notificaciones académicas, verificando:
- creación automática de notificaciones,
- consulta de notificaciones no leídas,
- conteo de pendientes,
- marcado masivo como leídas,
- y aplicación de preferencias configuradas por el usuario.

Las pruebas utilizan aserciones (`assertEquals`, `assertTrue`, `assertFalse`, `jsonPath`, `status`) para validar resultados esperados y comportamiento del sistema.

---

# Casos de Prueba

A continuación, se detallan los casos de prueba diseñados e implementados en `NotificationFlowIntegrationTest`:

---

## 1. Flujo Completo de Notificaciones

Estos casos verifican el flujo integral de creación, consulta y actualización de notificaciones asociadas a actividades académicas.

| ID Caso | Nombre del Test (Método) | Descripción | Precondiciones | Pasos | Resultado Esperado | Estado |
| :-- | :-- | :-- | :-- | :-- | :-- | :-- |
| NTF-001 | `endToEndFlow_create_list_count_markAll_read` | Verifica el flujo completo de creación, consulta, conteo y marcado de notificaciones como leídas. | Existen preferencias de notificación configuradas para el profesor y una actividad válida asociada a monitor y profesor. | 1. Crear notificación de actualización de progreso. 2. Crear notificación de actividad completada. 3. Consultar conteo de notificaciones. 4. Consultar listado de notificaciones no leídas. 5. Marcar todas como leídas. 6. Verificar nuevo conteo. | El sistema debe registrar correctamente las notificaciones, retornar conteo igual a 2 inicialmente, listar ambas notificaciones y posteriormente retornar conteo 0 después del marcado como leído. | OK |

---

## 2. Validación de Preferencias de Notificación

Estos casos verifican que las preferencias configuradas por el profesor controlen correctamente la generación de nuevas notificaciones.

| ID Caso | Nombre del Test (Método) | Descripción | Precondiciones | Pasos | Resultado Esperado | Estado |
| :-- | :-- | :-- | :-- | :-- | :-- | :-- |
| NTF-002 | `endToEndFlow_create_list_count_markAll_read` | Verifica que las preferencias deshabilitadas impidan la generación de nuevas notificaciones de progreso. | Existe una preferencia registrada para el profesor con notificaciones de progreso habilitadas inicialmente. | 1. Deshabilitar `enableProgressUpdate`. 2. Guardar preferencias actualizadas. 3. Intentar generar una nueva notificación de progreso. 4. Consultar conteo de notificaciones pendientes. | El sistema no debe registrar nuevas notificaciones de progreso cuando la preferencia está deshabilitada. El conteo debe permanecer en 0. | OK |

---

# Configuración de Datos de Prueba

Antes de ejecutar cada caso de prueba, se realiza la creación automática de entidades necesarias mediante el método `init()`:

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
| `NotificationPreferenceRepository` | Repositorio |
| `MockMvc` | Framework de pruebas HTTP |
| `JavaMailSender` | Dependencia mockeada |
| `Activity` | Entidad de dominio |
| `Professor` | Entidad de dominio |
| `Monitor` | Entidad de dominio |
| `NotificationPreference` | Entidad de dominio |

---

# Notas

- Las pruebas utilizan `@SpringBootTest` para cargar el contexto completo de Spring.
- Se utiliza `@AutoConfigureMockMvc(addFilters = false)` para deshabilitar filtros de seguridad durante las pruebas.
- El perfil `test` es activado mediante `@ActiveProfiles("test")`.
- `JavaMailSender` es mockeado mediante `@MockBean` para evitar el envío real de correos electrónicos.
- Las preferencias de notificación son verificadas directamente desde `NotificationPreferenceRepository`.
- Los casos implementados validan el flujo funcional completo relacionado con gestión de notificaciones académicas.
