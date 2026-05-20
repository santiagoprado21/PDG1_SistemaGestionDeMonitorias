# Diseño de Pruebas: `NotificationPreferenceRepository`

Este documento describe los casos de prueba diseñados para verificar la funcionalidad relacionada con la persistencia y gestión de preferencias de notificación implementadas mediante `NotificationPreferenceRepository`. Los tests correspondientes se encuentran en la clase `NotificationPreferenceRepositoryTest`.

**Objetivo:**  
Asegurar que `NotificationPreferenceRepository` gestione correctamente la búsqueda, creación y actualización de preferencias de notificación asociadas a profesores.

**Alcance:**  
Pruebas de persistencia utilizando `@DataJpaTest` sobre las operaciones principales del repositorio `NotificationPreferenceRepository`, validando el comportamiento de la entidad `NotificationPreference` en entorno de pruebas configurado con el perfil `test`.

**Estrategia:**  
Pruebas de integración orientadas a validar:
- búsqueda de preferencias inexistentes,
- persistencia de configuraciones por defecto,
- actualización de preferencias,
- y recuperación correcta de datos almacenados.

Las pruebas utilizan aserciones de AssertJ (`assertThat`) para validar resultados esperados y consistencia de los datos persistidos.

---

# Casos de Prueba

A continuación, se detallan los casos de prueba diseñados e implementados en `NotificationPreferenceRepositoryTest`:

---

## 1. Búsqueda de Preferencias Inexistentes

Estos casos verifican que el repositorio retorne correctamente un resultado vacío cuando no existen preferencias asociadas a un profesor.

| ID Caso | Nombre del Test (Método) | Descripción | Precondiciones | Pasos | Resultado Esperado | Estado |
| :-- | :-- | :-- | :-- | :-- | :-- | :-- |
| NPR-001 | `findByProfessorIdEmptyWhenNotExists` | Verifica que el repositorio retorne vacío cuando no existen preferencias registradas para el profesor indicado. | No existen registros asociados al identificador `"NOPE"` en la base de datos de pruebas. | 1. Ejecutar `findByProfessorId("NOPE")`. 2. Validar el resultado obtenido. | El sistema debe retornar un `Optional` vacío sin generar errores. | OK |

---

## 2. Creación y Actualización de Preferencias

Estos casos verifican que las preferencias de notificación puedan persistirse y actualizarse correctamente.

| ID Caso | Nombre del Test (Método) | Descripción | Precondiciones | Pasos | Resultado Esperado | Estado |
| :-- | :-- | :-- | :-- | :-- | :-- | :-- |
| NPR-002 | `saveAndUpdatePreferences` | Verifica el guardado inicial de preferencias con valores por defecto y su posterior actualización. | Existe acceso al repositorio de preferencias configurado en entorno de pruebas. | 1. Crear instancia `NotificationPreference` para `"PROF_X"`. 2. Guardar entidad. 3. Consultar preferencias registradas. 4. Validar valores por defecto. 5. Modificar preferencias `enableOverdue` y `enableSound`. 6. Guardar cambios. 7. Consultar nuevamente preferencias actualizadas. | El sistema debe persistir correctamente los valores por defecto (`true`) y posteriormente reflejar las actualizaciones realizadas (`false`) en los campos modificados. | OK |

---

# Configuración de Datos de Prueba

Antes de ejecutar cada caso de prueba, el entorno de persistencia es inicializado automáticamente mediante `@DataJpaTest`.

Las entidades utilizadas incluyen:

- `NotificationPreference`

Los datos son almacenados en una base de datos de prueba aislada para garantizar independencia entre ejecuciones.

---

# Dependencias Utilizadas

Las pruebas integran los siguientes componentes:

| Componente | Tipo |
| :-- | :-- |
| `NotificationPreferenceRepository` | Repositorio |
| `NotificationPreference` | Entidad de dominio |
| `AssertJ` | Framework de aserciones |
| `@DataJpaTest` | Configuración de pruebas JPA |

---

# Notas

- Las pruebas utilizan `@DataJpaTest` para cargar únicamente el contexto relacionado con persistencia JPA.
- El perfil `test` es activado mediante `@ActiveProfiles("test")`.
- Las operaciones de persistencia son ejecutadas sobre una base de datos de pruebas aislada.
- Las validaciones se realizan utilizando AssertJ mediante `assertThat`.
- Los casos implementados verifican tanto persistencia inicial como actualización de configuraciones de notificación.
