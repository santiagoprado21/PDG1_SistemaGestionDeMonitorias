# Diseño de Pruebas: `SimonFileController`

Este documento describe los casos de prueba diseñados para verificar la funcionalidad relacionada con la generación y gestión de archivos SIMON implementada mediante `SimonFileController`. Los tests de integración correspondientes se encuentran en la clase `SimonFileControllerTest`.

**Objetivo:**  
Asegurar que `SimonFileController` gestione correctamente la generación de archivos Excel SIMON, la visualización previa de monitorías aprobadas y la consulta del historial de generaciones realizadas.

**Alcance:**  
Pruebas de integración sobre los endpoints principales de `SimonFileController`, utilizando `MockMvc` y dependencias simuladas con Mockito (`@MockBean`). Se valida la interacción con `SimonFileService` y la correcta respuesta HTTP de los endpoints.

**Estrategia:**  
Pruebas de integración basadas en flujos funcionales completos de generación de archivos SIMON (HU-004), verificando:
- generación de archivos Excel,
- validación de parámetros,
- manejo de errores,
- visualización previa de datos,
- y consulta de historial de generaciones.

Las pruebas utilizan aserciones (`assertEquals`, `assertTrue`, `assertFalse`) y validaciones HTTP (`status`, `jsonPath`, `header`, `contentType`) para verificar respuestas esperadas y manejo adecuado de excepciones.

---

# Casos de Prueba

A continuación, se detallan los casos de prueba diseñados e implementados en `SimonFileControllerTest`:

---

## 1. Generación de Archivo Excel SIMON

Estos casos verifican la generación correcta del archivo Excel descargable.

| ID Caso | Nombre del Test (Método) | Descripción | Precondiciones | Pasos | Resultado Esperado | Estado |
| :-- | :-- | :-- | :-- | :-- | :-- | :-- |
| SIM-001 | `generateSimonFile_WithApprovedMonitorings_ShouldReturnExcelFile` | Verifica la generación exitosa del archivo Excel SIMON. | Existen monitorías aprobadas disponibles para exportación. | 1. Invocar endpoint `/simon/generate`. 2. Enviar parámetros `generatedBy` y `semester`. 3. Validar respuesta HTTP y headers. | El sistema debe retornar un archivo `.xlsx` descargable con status `200 OK` y headers válidos. | OK |

| SIM-002 | `generateSimonFile_WithDefaultParameters_ShouldUseDefaults` | Verifica el uso de parámetros por defecto cuando no se envían valores. | El servicio permite generación con valores predeterminados. | 1. Invocar endpoint `/simon/generate` sin parámetros. 2. Validar respuesta HTTP. | El sistema debe utilizar valores por defecto correctamente y generar el archivo. | OK |

| SIM-003 | `generateSimonFile_WithNoMonitorings_ShouldReturn204` | Verifica la respuesta cuando no existen monitorías aprobadas. | No existen monitorías aprobadas disponibles. | 1. Invocar endpoint `/simon/generate`. 2. Simular excepción `IllegalStateException`. | El sistema debe retornar `204 NO CONTENT`. | OK |

| SIM-004 | `generateSimonFile_WithIOException_ShouldReturn500` | Verifica el manejo de errores internos durante la generación del archivo. | Ocurre un error de escritura del archivo Excel. | 1. Simular `IOException`. 2. Invocar endpoint `/simon/generate`. | El sistema debe retornar `500 INTERNAL SERVER ERROR`. | OK |

| SIM-005 | `generateSimonFile_ShouldIncludeSemesterAndTimestampInFilename` | Verifica que el nombre del archivo incluya semestre y timestamp. | Existe generación válida del archivo SIMON. | 1. Invocar endpoint con parámetro `semester`. 2. Validar `Content-Disposition`. | El nombre del archivo debe cumplir el patrón `SIMON_<semester>_<timestamp>.xlsx`. | OK |

| SIM-006 | `generateSimonFile_ShouldIncludeContentLength` | Verifica que la respuesta incluya el header `Content-Length`. | Existe generación válida del archivo Excel. | 1. Invocar endpoint `/simon/generate`. 2. Validar headers HTTP. | La respuesta debe contener `Content-Length`. | OK |

| SIM-007 | `generateSimonFile_WithCustomParameters_ShouldUseProvidedValues` | Verifica el uso de parámetros personalizados enviados por el usuario. | Se proporcionan valores personalizados para usuario y semestre. | 1. Invocar endpoint con parámetros personalizados. 2. Validar llamada al servicio. | El sistema debe utilizar los parámetros enviados correctamente. | OK |

---

## 2. Preview de Datos SIMON

Estos casos verifican la visualización previa de las monitorías aprobadas antes de generar el archivo.

| ID Caso | Nombre del Test (Método) | Descripción | Precondiciones | Pasos | Resultado Esperado | Estado |
| :-- | :-- | :-- | :-- | :-- | :-- | :-- |
| SIM-008 | `previewSimonData_WithMonitorings_ShouldReturnData` | Verifica el preview exitoso con monitorías disponibles. | Existen monitorías aprobadas registradas. | 1. Invocar endpoint `/simon/preview`. 2. Validar JSON retornado. | El sistema debe retornar monitorías, metadata y `canGenerate = true`. | OK |

| SIM-009 | `previewSimonData_WithNoMonitorings_ShouldIndicateCannotGenerate` | Verifica el comportamiento cuando no existen monitorías disponibles. | No existen monitorías aprobadas registradas. | 1. Invocar endpoint `/simon/preview`. 2. Validar JSON retornado. | El sistema debe retornar `canGenerate = false` y lista vacía. | OK |

| SIM-010 | `previewSimonData_WithMultipleMonitorings_ShouldReturnAll` | Verifica la visualización correcta de múltiples monitorías. | Existen múltiples monitorías aprobadas registradas. | 1. Simular múltiples DTOs. 2. Invocar endpoint `/simon/preview`. | El sistema debe retornar todas las monitorías correctamente. | OK |

| SIM-011 | `previewSimonData_WithException_ShouldReturn500` | Verifica el manejo de errores durante el preview de datos. | Ocurre un error interno en el servicio. | 1. Simular excepción `RuntimeException`. 2. Invocar endpoint `/simon/preview`. | El sistema debe retornar `500 INTERNAL SERVER ERROR` y mensaje de error. | OK |

---

## 3. Historial de Generaciones SIMON

Estos casos verifican la consulta del historial de archivos SIMON generados.

| ID Caso | Nombre del Test (Método) | Descripción | Precondiciones | Pasos | Resultado Esperado | Estado |
| :-- | :-- | :-- | :-- | :-- | :-- | :-- |
| SIM-012 | `getGenerationHistory_ShouldReturnAllGenerations` | Verifica la obtención completa del historial de generaciones. | Existen registros de generaciones SIMON. | 1. Invocar endpoint `/simon/history`. 2. Validar respuesta JSON. | El sistema debe retornar todas las generaciones registradas. | OK |

| SIM-013 | `getGenerationHistory_WithNoHistory_ShouldReturnEmptyList` | Verifica el comportamiento cuando no existe historial. | No existen registros de generaciones. | 1. Invocar endpoint `/simon/history`. | El sistema debe retornar una lista vacía con status `200 OK`. | OK |

| SIM-014 | `getGenerationHistory_WithException_ShouldReturn500` | Verifica el manejo de errores al consultar historial completo. | Ocurre un error interno del servicio. | 1. Simular excepción. 2. Invocar endpoint `/simon/history`. | El sistema debe retornar `500 INTERNAL SERVER ERROR`. | OK |

| SIM-015 | `getGenerationHistoryBySemester_ShouldReturnFilteredGenerations` | Verifica la obtención del historial filtrado por semestre. | Existen generaciones asociadas a un semestre específico. | 1. Invocar endpoint `/simon/history/{semester}`. 2. Validar respuesta JSON. | El sistema debe retornar únicamente generaciones del semestre consultado. | OK |

| SIM-016 | `getGenerationHistoryBySemester_WithNoResults_ShouldReturnEmptyList` | Verifica el comportamiento cuando no existen resultados para un semestre. | No existen generaciones asociadas al semestre consultado. | 1. Invocar endpoint `/simon/history/{semester}`. | El sistema debe retornar lista vacía con status `200 OK`. | OK |

| SIM-017 | `getGenerationHistoryBySemester_WithException_ShouldReturn500` | Verifica el manejo de errores al consultar historial filtrado. | Ocurre un error interno en el servicio. | 1. Simular excepción. 2. Invocar endpoint `/simon/history/{semester}`. | El sistema debe retornar `500 INTERNAL SERVER ERROR`. | OK |

---

# Configuración de Datos de Prueba

Antes de ejecutar cada caso de prueba, se realiza la configuración automática de datos simulados mediante el método `setUp()`:

- `SimonMonitoringDTO`
- `Workbook`
- `SimonFileGeneration`

Los datos son simulados utilizando Mockito y objetos de prueba (`XSSFWorkbook`) para representar archivos Excel generados por el sistema.

---

# Dependencias Utilizadas

Las pruebas integran los siguientes componentes:

| Componente | Tipo |
| :-- | :-- |
| `SimonFileController` | Controlador principal |
| `SimonFileService` | Servicio Mock |
| `MockMvc` | Framework de pruebas HTTP |
| `Workbook` | Generación de archivos Excel |
| `ObjectMapper` | Serialización JSON |

---

# Notas

- Las pruebas están configuradas utilizando `@WebMvcTest` y `MockMvc`.
- La autenticación se simula mediante `@WithMockUser`.
- Se valida la correcta configuración de headers HTTP para descargas de archivos.
- Se verifican respuestas JSON utilizando `jsonPath`.
- Los casos incluyen tanto flujos exitosos como manejo de excepciones.
- Los escenarios implementados corresponden al flujo funcional de generación de archivos SIMON definido para la HU-004.
