# Diseño de Pruebas: `SimonFileServiceImpl`

Este documento describe los casos de prueba diseñados para verificar la funcionalidad relacionada con la generación y gestión de archivos SIMON implementados mediante `SimonFileServiceImpl`. Los tests unitarios correspondientes se encuentran en la clase `SimonFileServiceImplTest`.

**Objetivo:**  
Asegurar que `SimonFileServiceImpl` gestione correctamente la generación de archivos Excel SIMON, el mapeo de monitorías aprobadas, la validación de información, el registro de historial y la obtención de datos asociados a las monitorías aprobadas.

**Alcance:**  
Pruebas unitarias sobre los métodos principales de `SimonFileServiceImpl`, utilizando mocks de repositorios mediante Mockito (`@ExtendWith(MockitoExtension.class)`). Se valida la interacción entre entidades como `Monitoring`, `Monitor`, `Professor`, `Course`, `MonitoringMonitor` y `SimonFileGeneration`.

**Estrategia:**  
Pruebas unitarias enfocadas en los flujos funcionales de generación de archivos SIMON (HU-004), verificando:

- generación de archivos Excel,
- validación de monitorías aprobadas,
- estructura y contenido del archivo generado,
- generación y filtrado del historial,
- mapeo correcto a DTOs,
- y validación de formatos y cantidades.

Las pruebas utilizan aserciones (`assertEquals`, `assertNotNull`, `assertThrows`, `assertTrue`) para validar resultados esperados y manejo de excepciones.

---

# Casos de Prueba

A continuación, se detallan los casos de prueba diseñados e implementados en `SimonFileServiceImplTest`:

---

## 1. Generación Exitosa del Archivo SIMON

Estos casos verifican que el sistema genere correctamente el archivo Excel cuando existen monitorías aprobadas.

| ID Caso | Nombre del Test (Método) | Descripción | Precondiciones | Pasos | Resultado Esperado | Estado |
| :-- | :-- | :-- | :-- | :-- | :-- | :-- |
| SFS-001 | `generateSimonFile_WithApprovedMonitorings_ShouldGenerateFileSuccessfully` | Verifica que el archivo SIMON se genere correctamente con monitorías aprobadas. | Existen monitorías aprobadas disponibles en el repositorio mockeado. | 1. Simular monitorías aprobadas. 2. Ejecutar `generateSimonFile`. 3. Validar workbook y hoja generada. 4. Verificar registro en historial. | El sistema debe generar correctamente el archivo Excel y registrar la generación en la base de datos. | OK |

---

## 2. Validación de Monitorías Disponibles

Estos casos verifican el comportamiento cuando no existen monitorías aprobadas.

| ID Caso | Nombre del Test (Método) | Descripción | Precondiciones | Pasos | Resultado Esperado | Estado |
| :-- | :-- | :-- | :-- | :-- | :-- | :-- |
| SFS-002 | `generateSimonFile_WithNoApprovedMonitorings_ShouldThrowException` | Verifica que el sistema lance excepción cuando no existen monitorías aprobadas. | El repositorio retorna una lista vacía. | 1. Simular lista vacía. 2. Ejecutar `generateSimonFile`. | El sistema debe lanzar `IllegalStateException` indicando que no hay monitorías aprobadas. | OK |

---

## 3. Validación de la Estructura del Excel

Estos casos verifican que el archivo Excel generado contenga correctamente encabezados y datos.

| ID Caso | Nombre del Test (Método) | Descripción | Precondiciones | Pasos | Resultado Esperado | Estado |
| :-- | :-- | :-- | :-- | :-- | :-- | :-- |
| SFS-003 | `generateSimonFile_ShouldHaveCorrectExcelStructure` | Verifica que el archivo Excel contenga la estructura y datos esperados. | Existen monitorías aprobadas válidas. | 1. Generar archivo SIMON. 2. Obtener hoja y filas. 3. Validar encabezados y datos. | El archivo debe contener correctamente todos los encabezados y la información de monitorías. | OK |

---

## 4. Obtención de Monitorías para SIMON

Estos casos verifican el correcto mapeo de monitorías aprobadas hacia DTOs.

| ID Caso | Nombre del Test (Método) | Descripción | Precondiciones | Pasos | Resultado Esperado | Estado |
| :-- | :-- | :-- | :-- | :-- | :-- | :-- |
| SFS-004 | `getApprovedMonitoringsForSimon_ShouldReturnCorrectDTOs` | Verifica el mapeo correcto de monitorías aprobadas hacia `SimonMonitoringDTO`. | Existen monitorías aprobadas configuradas. | 1. Ejecutar `getApprovedMonitoringsForSimon`. 2. Validar campos del DTO. | El DTO debe contener correctamente nombres, cursos, fechas y datos del monitor. | OK |

---

## 5. Procesamiento de Múltiples Monitorías

Estos casos verifican el manejo correcto de múltiples monitorías aprobadas.

| ID Caso | Nombre del Test (Método) | Descripción | Precondiciones | Pasos | Resultado Esperado | Estado |
| :-- | :-- | :-- | :-- | :-- | :-- | :-- |
| SFS-005 | `getApprovedMonitoringsForSimon_WithMultipleMonitorings_ShouldReturnAllDTOs` | Verifica el procesamiento de múltiples monitorías aprobadas. | Existen varias monitorías aprobadas configuradas. | 1. Simular múltiples monitorías. 2. Ejecutar obtención de DTOs. | El sistema debe retornar correctamente todos los DTOs generados. | OK |

---

## 6. Obtención del Historial de Generaciones

Estos casos verifican la recuperación del historial de archivos SIMON generados.

| ID Caso | Nombre del Test (Método) | Descripción | Precondiciones | Pasos | Resultado Esperado | Estado |
| :-- | :-- | :-- | :-- | :-- | :-- | :-- |
| SFS-006 | `getGenerationHistory_ShouldReturnAllGenerations` | Verifica la obtención completa del historial de generaciones. | Existen registros de historial simulados. | 1. Simular historial. 2. Ejecutar `getGenerationHistory`. | El sistema debe retornar correctamente todas las generaciones registradas. | OK |

---

## 7. Filtrado de Historial por Semestre

Estos casos verifican la recuperación de historial filtrado por semestre académico.

| ID Caso | Nombre del Test (Método) | Descripción | Precondiciones | Pasos | Resultado Esperado | Estado |
| :-- | :-- | :-- | :-- | :-- | :-- | :-- |
| SFS-007 | `getGenerationHistoryBySemester_ShouldReturnFilteredGenerations` | Verifica el filtrado del historial por semestre. | Existen generaciones registradas para el semestre indicado. | 1. Simular historial filtrado. 2. Ejecutar búsqueda por semestre. | El sistema debe retornar únicamente las generaciones correspondientes al semestre solicitado. | OK |

---

## 8. Validación de Formato de Fechas

Estos casos verifican el correcto formato de fechas en los DTOs SIMON.

| ID Caso | Nombre del Test (Método) | Descripción | Precondiciones | Pasos | Resultado Esperado | Estado |
| :-- | :-- | :-- | :-- | :-- | :-- | :-- |
| SFS-008 | `getApprovedMonitoringsForSimon_ShouldFormatDatesCorrectly` | Verifica que las fechas sean formateadas correctamente. | Existen monitorías con fechas válidas configuradas. | 1. Configurar fechas de inicio y fin. 2. Ejecutar obtención de DTOs. | El sistema debe retornar fechas con formato `dd/MM/yyyy`. | OK |

---

## 9. Validación de Parámetros de Generación

Estos casos verifican que el sistema utilice correctamente parámetros personalizados.

| ID Caso | Nombre del Test (Método) | Descripción | Precondiciones | Pasos | Resultado Esperado | Estado |
| :-- | :-- | :-- | :-- | :-- | :-- | :-- |
| SFS-009 | `generateSimonFile_WithDifferentParameters_ShouldUseCorrectValues` | Verifica el uso correcto de parámetros personalizados de generación. | Existen monitorías aprobadas válidas. | 1. Ejecutar generación con usuario y semestre personalizados. 2. Verificar historial guardado. | El sistema debe registrar correctamente usuario y semestre personalizados. | OK |

---

## 10. Validación de Cantidad de Filas Generadas

Estos casos verifican que el archivo generado contenga el número correcto de filas.

| ID Caso | Nombre del Test (Método) | Descripción | Precondiciones | Pasos | Resultado Esperado | Estado |
| :-- | :-- | :-- | :-- | :-- | :-- | :-- |
| SFS-010 | `generateSimonFile_ShouldHaveCorrectNumberOfRows` | Verifica que el Excel generado contenga el número correcto de filas. | Existen múltiples monitorías aprobadas configuradas. | 1. Generar archivo con varias monitorías. 2. Validar cantidad de filas. | El archivo debe contener correctamente la fila de encabezados y las filas de datos esperadas. | OK |

---

# Configuración de Datos de Prueba

Antes de ejecutar cada caso de prueba, se realiza la configuración automática de entidades mock mediante el método `setUp()`:

- `Monitor`
- `Professor`
- `Course`
- `Monitoring`
- `MonitoringMonitor`

Estas entidades son configuradas utilizando Mockito para garantizar independencia y aislamiento entre pruebas.

---

# Dependencias Utilizadas

Las pruebas integran los siguientes componentes:

| Componente | Tipo |
| :-- | :-- |
| `SimonFileServiceImpl` | Servicio principal |
| `MonitoringMonitorRepository` | Repositorio mock |
| `SimonFileGenerationRepository` | Repositorio mock |
| `Workbook` | Apache POI |
| `Sheet` | Apache POI |

---

# Notas

- Las pruebas utilizan Mockito mediante `@ExtendWith(MockitoExtension.class)`.
- Se emplean `ArgumentCaptor` para validar entidades persistidas.
- La generación del archivo Excel utiliza Apache POI (`Workbook`, `Sheet`, `Row`, `Cell`).
- Las fechas son validadas utilizando `SimpleDateFormat`.
- Los casos implementados corresponden al flujo funcional completo de la historia de usuario HU-004.
