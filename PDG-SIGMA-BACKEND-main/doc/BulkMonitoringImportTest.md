# Diseño de Pruebas: `BulkMonitoringImportService`

Este documento describe los casos de prueba diseñados para verificar la funcionalidad de importación masiva de monitorías implementada mediante `MonitoringServiceImpl`. Los tests de integración correspondientes se encuentran en la clase `BulkMonitoringImportTest`.

**Objetivo:**  
Asegurar que `MonitoringServiceImpl` procese correctamente archivos CSV y Excel para la creación masiva de monitorías, validando encabezados, períodos académicos, campos obligatorios, compatibilidad de formatos y prevención de duplicados.

**Alcance:**  
Pruebas de integración sobre el método `processListMonitor`, utilizando repositorios reales configurados en entorno de pruebas (`@SpringBootTest`, perfil `test`). Se valida la interacción entre entidades como `School`, `Program`, `Course`, `Professor`, `StudentCourse` y `Monitoring`.

**Estrategia:**  
Pruebas de integración basadas en flujos funcionales completos de negocio relacionados con importación masiva de monitorías, verificando:

- importación de archivos CSV,
- importación de archivos Excel,
- validación de encabezados,
- validación de períodos académicos,
- validación de campos obligatorios,
- compatibilidad de formatos,
- y prevención de registros duplicados.

Las pruebas utilizan aserciones (`assertEquals`, `assertTrue`, `assertFalse`, `assertThrows`) para validar resultados esperados y manejo de excepciones.

---

# Casos de Prueba

A continuación, se detallan los casos de prueba diseñados e implementados en `BulkMonitoringImportTest`:

---

## 1. Importación Exitosa de CSV

Estos casos verifican que el sistema procese correctamente archivos CSV válidos y cree las monitorías correspondientes.

| ID Caso | Nombre del Test (Método) | Descripción | Precondiciones | Pasos | Resultado Esperado | Estado |
| :-- | :-- | :-- | :-- | :-- | :-- | :-- |
| BMI-001 | `acceptsCsv_creates_all_rows` | Verifica la creación exitosa de monitorías desde un archivo CSV válido. | Existen `School`, `Program`, `Course` y `Professor` válidos registrados en la base de datos de prueba. | 1. Crear archivo CSV válido con dos registros. 2. Ejecutar `processListMonitor`. 3. Validar respuesta del servicio. 4. Verificar cantidad de registros en repositorio. | El sistema debe crear correctamente las 2 monitorías indicadas y no generar registros omitidos. | OK |

---

## 2. Validación de Encabezados CSV

Estos casos verifican que el sistema rechace archivos CSV con encabezados incompatibles.

| ID Caso | Nombre del Test (Método) | Descripción | Precondiciones | Pasos | Resultado Esperado | Estado |
| :-- | :-- | :-- | :-- | :-- | :-- | :-- |
| BMI-002 | `rejectsCsv_with_invalid_header` | Verifica que el sistema detecte encabezados inválidos en archivos CSV. | Existe configuración académica válida en la base de datos de prueba. | 1. Crear archivo CSV con encabezado alterado. 2. Ejecutar `processListMonitor`. | El sistema debe lanzar una excepción indicando incompatibilidad en el formato del archivo. | OK |

---

## 3. Importación Exitosa de Excel

Estos casos verifican el procesamiento correcto de archivos Excel `.xlsx`.

| ID Caso | Nombre del Test (Método) | Descripción | Precondiciones | Pasos | Resultado Esperado | Estado |
| :-- | :-- | :-- | :-- | :-- | :-- | :-- |
| BMI-003 | `acceptsExcel_creates_all_rows` | Verifica la creación correcta de monitorías desde archivos Excel válidos. | Existen entidades académicas válidas registradas. | 1. Crear workbook Excel válido. 2. Agregar dos registros de monitorías. 3. Ejecutar `processListMonitor`. | El sistema debe crear correctamente las 2 monitorías y no generar omitidas. | OK |

---

## 4. Compatibilidad de Content-Type

Estos casos verifican compatibilidad con distintos tipos MIME para archivos CSV.

| ID Caso | Nombre del Test (Método) | Descripción | Precondiciones | Pasos | Resultado Esperado | Estado |
| :-- | :-- | :-- | :-- | :-- | :-- | :-- |
| BMI-004 | `acceptsCsv_with_msexcel_contentType_creates_all` | Verifica compatibilidad con `application/vnd.ms-excel` para archivos CSV. | Existe configuración académica válida. | 1. Crear archivo CSV válido. 2. Configurar contentType `application/vnd.ms-excel`. 3. Ejecutar importación. | El sistema debe procesar correctamente el archivo y crear las monitorías esperadas. | OK |

---

## 5. Tolerancia a Espacios en Encabezados

Estos casos verifican que el sistema permita encabezados con espacios adicionales.

| ID Caso | Nombre del Test (Método) | Descripción | Precondiciones | Pasos | Resultado Esperado | Estado |
| :-- | :-- | :-- | :-- | :-- | :-- | :-- |
| BMI-005 | `acceptsCsv_with_header_trailing_spaces` | Verifica tolerancia a espacios extra en encabezados CSV. | Existe configuración académica válida. | 1. Crear archivo CSV con espacios adicionales en encabezados. 2. Ejecutar `processListMonitor`. | El sistema debe procesar correctamente el archivo y crear la monitoría correspondiente. | OK |

---

## 6. Validación de Año Académico

Estos casos verifican que el período académico corresponda al año actual.

| ID Caso | Nombre del Test (Método) | Descripción | Precondiciones | Pasos | Resultado Esperado | Estado |
| :-- | :-- | :-- | :-- | :-- | :-- | :-- |
| BMI-006 | `rejectsCsv_with_wrong_year_in_semester` | Verifica rechazo de períodos académicos con año incorrecto. | Existe configuración académica válida. | 1. Crear CSV con semestre de año distinto al actual. 2. Ejecutar importación. | El sistema debe lanzar excepción indicando inconsistencia con el año actual. | OK |

---

## 7. Validación de Semestre Según Fecha

Estos casos verifican consistencia entre fechas y semestre académico.

| ID Caso | Nombre del Test (Método) | Descripción | Precondiciones | Pasos | Resultado Esperado | Estado |
| :-- | :-- | :-- | :-- | :-- | :-- | :-- |
| BMI-007 | `rejectsCsv_with_invalid_semester_for_month` | Verifica rechazo de semestre inconsistente con el período actual. | Existe configuración académica válida. | 1. Crear CSV con semestre inválido para el mes configurado. 2. Ejecutar importación. | El sistema debe lanzar excepción indicando inconsistencia con el semestre actual. | OK |

---

## 8. Validación de Campos Obligatorios

Estos casos verifican que el sistema rechace registros con campos vacíos.

| ID Caso | Nombre del Test (Método) | Descripción | Precondiciones | Pasos | Resultado Esperado | Estado |
| :-- | :-- | :-- | :-- | :-- | :-- | :-- |
| BMI-008 | `rejectsCsv_with_blank_field` | Verifica validación de obligatoriedad de campos en CSV. | Existe configuración académica válida. | 1. Crear CSV con campo vacío. 2. Ejecutar importación. | El sistema debe lanzar excepción indicando incompatibilidad en los datos. | OK |

---

## 9. Prevención de Duplicados

Estos casos verifican que el sistema no permita crear monitorías duplicadas para el mismo profesor y semestre.

| ID Caso | Nombre del Test (Método) | Descripción | Precondiciones | Pasos | Resultado Esperado | Estado |
| :-- | :-- | :-- | :-- | :-- | :-- | :-- |
| BMI-009 | `csv_duplicate_course_is_omitted_for_same_prof_and_semester` | Verifica que monitorías duplicadas sean omitidas automáticamente. | Existe una monitoría previamente creada para el mismo curso, profesor y semestre. | 1. Importar monitoría válida. 2. Repetir importación del mismo archivo. 3. Validar respuesta del servicio. | El sistema debe omitir la segunda creación e indicar que la monitoría ya existía para el semestre correspondiente. | OK |

---

## 10. Validación de Encabezados Excel

Estos casos verifican que el sistema rechace archivos Excel con encabezados inválidos.

| ID Caso | Nombre del Test (Método) | Descripción | Precondiciones | Pasos | Resultado Esperado | Estado |
| :-- | :-- | :-- | :-- | :-- | :-- | :-- |
| BMI-010 | `rejectsExcel_with_invalid_header` | Verifica rechazo de encabezados inválidos en archivos Excel. | Existe configuración académica válida. | 1. Crear archivo Excel con encabezado alterado. 2. Ejecutar `processListMonitor`. | El sistema debe lanzar excepción indicando incompatibilidad en el archivo Excel. | OK |

---

# Configuración de Datos de Prueba

Antes de ejecutar cada caso de prueba, se realiza la creación automática de entidades necesarias mediante el método `setup()`:

- `School`
- `Program`
- `Course`
- `Professor`
- `StudentCourse`

Estas entidades son persistidas utilizando los repositorios configurados en el entorno de pruebas para garantizar independencia y aislamiento entre tests.

---

# Dependencias Utilizadas

Las pruebas integran los siguientes componentes:

| Componente | Tipo |
| :-- | :-- |
| `MonitoringServiceImpl` | Servicio principal |
| `MonitoringRepository` | Repositorio |
| `ProfessorRepository` | Repositorio |
| `CourseRepository` | Repositorio |
| `ProgramRepository` | Repositorio |
| `SchoolRepository` | Repositorio |
| `StudentCourseRepository` | Repositorio |
| `JavaMailSender` | Dependencia mockeada |

---

# Notas

- Las pruebas están configuradas con `@Transactional`, por lo que los datos generados se revierten automáticamente al finalizar cada ejecución.
- Se utiliza el perfil `test` mediante `@ActiveProfiles("test")`.
- Los archivos CSV y Excel son generados dinámicamente en memoria durante la ejecución de las pruebas.
- Las validaciones dependen directamente de la lógica implementada en `processListMonitor`.
- `JavaMailSender` se encuentra mockeado mediante `@MockBean` para evitar envío real de correos durante las pruebas.
- Los casos implementados validan el flujo funcional completo de importación masiva de monitorías.
- Compatibilidad de content types.
- Persistencia de monitorías.
