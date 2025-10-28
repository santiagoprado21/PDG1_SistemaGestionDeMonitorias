# 🧪 Documentación de Pruebas - HU-004: Generación de Archivo SIMON

## 📋 Resumen

Esta documentación describe las pruebas implementadas para la funcionalidad de generación del archivo SIMON, que permite exportar las monitorías aprobadas en formato Excel para el sistema SIMON.

---

## 📁 Archivos de Prueba

### 1. **SimonFileServiceImplTest.java**
Pruebas unitarias del servicio de generación de archivos SIMON.

**Ubicación:** `src/test/java/com/pdg/sigma/SimonFileServiceImplTest.java`

**Tipo:** Pruebas unitarias con Mockito

**Dependencias mockeadas:**
- `MonitoringMonitorRepository`
- `SimonFileGenerationRepository`

### 2. **SimonFileControllerTest.java**
Pruebas de integración del controlador REST de SIMON.

**Ubicación:** `src/test/java/com/pdg/sigma/SimonFileControllerTest.java`

**Tipo:** Pruebas de integración con MockMvc

**Dependencias mockeadas:**
- `SimonFileService`

---

## 🧪 Casos de Prueba

### SimonFileServiceImplTest (10 casos)

#### ✅ Caso 1: Generación exitosa con monitorías aprobadas
- **Método:** `generateSimonFile_WithApprovedMonitorings_ShouldGenerateFileSuccessfully()`
- **Propósito:** Verifica que el archivo se genera correctamente cuando hay monitorías aprobadas
- **Verificaciones:**
  - El workbook no es nulo
  - La hoja se crea con el nombre correcto
  - Se guarda el registro en la base de datos
  - El registro contiene los datos correctos (usuario, semestre, cantidad)

#### ❌ Caso 2: Error sin monitorías aprobadas
- **Método:** `generateSimonFile_WithNoApprovedMonitorings_ShouldThrowException()`
- **Propósito:** Verifica que se lance una excepción cuando no hay monitorías
- **Verificaciones:**
  - Se lanza `IllegalStateException`
  - El mensaje de error es correcto
  - No se intenta guardar ningún registro

#### 📊 Caso 3: Estructura del Excel
- **Método:** `generateSimonFile_ShouldHaveCorrectExcelStructure()`
- **Propósito:** Verifica que el archivo Excel tenga la estructura correcta
- **Verificaciones:**
  - Headers en el orden correcto (17 columnas)
  - Datos mapeados correctamente en cada celda
  - Formato de datos apropiado

#### 🔄 Caso 4: Mapeo a DTO
- **Método:** `getApprovedMonitoringsForSimon_ShouldReturnCorrectDTOs()`
- **Propósito:** Verifica que los datos se mapean correctamente al DTO
- **Verificaciones:**
  - Todos los campos del DTO están correctamente poblados
  - Formato de fechas es correcto (dd/MM/yyyy)
  - Cálculo de semanas es preciso

#### 📝 Caso 5: Múltiples monitorías
- **Método:** `getApprovedMonitoringsForSimon_WithMultipleMonitorings_ShouldReturnAllDTOs()`
- **Propósito:** Verifica el procesamiento de múltiples registros
- **Verificaciones:**
  - Se retornan todos los DTOs
  - Cada DTO tiene los datos correctos

#### 📜 Caso 6: Historial de generaciones
- **Método:** `getGenerationHistory_ShouldReturnAllGenerations()`
- **Propósito:** Verifica la obtención del historial completo
- **Verificaciones:**
  - Se obtienen todos los registros
  - Los datos son correctos

#### 🔍 Caso 7: Filtrado por semestre
- **Método:** `getGenerationHistoryBySemester_ShouldReturnFilteredGenerations()`
- **Propósito:** Verifica el filtrado por semestre
- **Verificaciones:**
  - Solo se obtienen registros del semestre especificado
  - Todos los registros pertenecen al semestre correcto

#### 📅 Caso 8: Formato de fechas
- **Método:** `getApprovedMonitoringsForSimon_ShouldFormatDatesCorrectly()`
- **Propósito:** Verifica el formato correcto de las fechas
- **Verificaciones:**
  - Fechas en formato dd/MM/yyyy

#### 🔧 Caso 9: Parámetros personalizados
- **Método:** `generateSimonFile_WithDifferentParameters_ShouldUseCorrectValues()`
- **Propósito:** Verifica el uso de diferentes parámetros
- **Verificaciones:**
  - Los parámetros se usan correctamente
  - El nombre del archivo incluye el semestre

#### 📏 Caso 10: Número de filas
- **Método:** `generateSimonFile_ShouldHaveCorrectNumberOfRows()`
- **Propósito:** Verifica que el archivo tenga el número correcto de filas
- **Verificaciones:**
  - 1 fila de headers + N filas de datos
  - El conteo en el registro es correcto

---

### SimonFileControllerTest (17 casos)

#### 🟢 Caso 1: Generación exitosa
- **Método:** `generateSimonFile_WithApprovedMonitorings_ShouldReturnExcelFile()`
- **Endpoint:** `GET /simon/generate`
- **Verificaciones:**
  - Status 200 OK
  - Headers de descarga configurados
  - Content-Type correcto
  - Nombre de archivo con timestamp

#### 🔄 Caso 2: Parámetros por defecto
- **Método:** `generateSimonFile_WithDefaultParameters_ShouldUseDefaults()`
- **Endpoint:** `GET /simon/generate`
- **Verificaciones:**
  - Usa "coordinador" como generatedBy
  - Usa "2024-2" como semester

#### ⚠️ Caso 3: Sin monitorías
- **Método:** `generateSimonFile_WithNoMonitorings_ShouldReturn204()`
- **Endpoint:** `GET /simon/generate`
- **Verificaciones:**
  - Status 204 NO CONTENT

#### 💥 Caso 4: Error interno
- **Método:** `generateSimonFile_WithIOException_ShouldReturn500()`
- **Endpoint:** `GET /simon/generate`
- **Verificaciones:**
  - Status 500 INTERNAL SERVER ERROR

#### 👀 Caso 5: Preview exitoso
- **Método:** `previewSimonData_WithMonitorings_ShouldReturnData()`
- **Endpoint:** `GET /simon/preview`
- **Verificaciones:**
  - Status 200 OK
  - JSON con totalMonitorings, canGenerate, monitorings
  - Datos correctos de las monitorías

#### 🚫 Caso 6: Preview sin datos
- **Método:** `previewSimonData_WithNoMonitorings_ShouldIndicateCannotGenerate()`
- **Endpoint:** `GET /simon/preview`
- **Verificaciones:**
  - canGenerate = false
  - totalMonitorings = 0

#### 📋 Caso 7: Preview múltiple
- **Método:** `previewSimonData_WithMultipleMonitorings_ShouldReturnAll()`
- **Endpoint:** `GET /simon/preview`
- **Verificaciones:**
  - Retorna todas las monitorías
  - Datos completos de cada una

#### ❌ Caso 8: Error en preview
- **Método:** `previewSimonData_WithException_ShouldReturn500()`
- **Endpoint:** `GET /simon/preview`
- **Verificaciones:**
  - Status 500
  - Mensaje de error en respuesta

#### 📚 Caso 9: Historial completo
- **Método:** `getGenerationHistory_ShouldReturnAllGenerations()`
- **Endpoint:** `GET /simon/history`
- **Verificaciones:**
  - Status 200 OK
  - Lista de generaciones
  - Datos correctos de cada registro

#### 📭 Caso 10: Historial vacío
- **Método:** `getGenerationHistory_WithNoHistory_ShouldReturnEmptyList()`
- **Endpoint:** `GET /simon/history`
- **Verificaciones:**
  - Status 200 OK
  - Lista vacía

#### 🔴 Caso 11: Error en historial
- **Método:** `getGenerationHistory_WithException_ShouldReturn500()`
- **Endpoint:** `GET /simon/history`
- **Verificaciones:**
  - Status 500

#### 📅 Caso 12: Historial por semestre
- **Método:** `getGenerationHistoryBySemester_ShouldReturnFilteredGenerations()`
- **Endpoint:** `GET /simon/history/{semester}`
- **Verificaciones:**
  - Registros filtrados por semestre
  - Todos pertenecen al semestre solicitado

#### 📂 Caso 13: Historial semestre vacío
- **Método:** `getGenerationHistoryBySemester_WithNoResults_ShouldReturnEmptyList()`
- **Endpoint:** `GET /simon/history/{semester}`
- **Verificaciones:**
  - Lista vacía para semestre sin registros

#### 🛑 Caso 14: Error historial por semestre
- **Método:** `getGenerationHistoryBySemester_WithException_ShouldReturn500()`
- **Endpoint:** `GET /simon/history/{semester}`
- **Verificaciones:**
  - Status 500

#### 📝 Caso 15: Nombre de archivo con semestre
- **Método:** `generateSimonFile_ShouldIncludeSemesterAndTimestampInFilename()`
- **Endpoint:** `GET /simon/generate`
- **Verificaciones:**
  - Nombre sigue patrón SIMON_{semester}_{timestamp}.xlsx

#### 📦 Caso 16: Content-Length presente
- **Método:** `generateSimonFile_ShouldIncludeContentLength()`
- **Endpoint:** `GET /simon/generate`
- **Verificaciones:**
  - Header Content-Length existe

#### ⚙️ Caso 17: Parámetros personalizados
- **Método:** `generateSimonFile_WithCustomParameters_ShouldUseProvidedValues()`
- **Endpoint:** `GET /simon/generate`
- **Verificaciones:**
  - Usa los parámetros proporcionados

---

## 🚀 Cómo Ejecutar las Pruebas

### Todas las pruebas del proyecto
```bash
cd PDG-SIGMA-BACKEND-main
.\mvnw.cmd test
```

### Solo pruebas de SIMON
```bash
# Pruebas del servicio
.\mvnw.cmd test -Dtest=SimonFileServiceImplTest

# Pruebas del controlador
.\mvnw.cmd test -Dtest=SimonFileControllerTest

# Ambas
.\mvnw.cmd test -Dtest=Simon*Test
```

### Con Maven instalado
```bash
mvn test -Dtest=SimonFileServiceImplTest
mvn test -Dtest=SimonFileControllerTest
```

---

## 📊 Cobertura de Pruebas

### Servicio (SimonFileServiceImpl)
- ✅ `generateSimonFile()` - 100% cubierto
- ✅ `getApprovedMonitoringsForSimon()` - 100% cubierto
- ✅ `getGenerationHistory()` - 100% cubierto
- ✅ `getGenerationHistoryBySemester()` - 100% cubierto
- ✅ Métodos privados (indirectamente cubiertos)

### Controlador (SimonFileController)
- ✅ `GET /simon/generate` - 100% cubierto
- ✅ `GET /simon/preview` - 100% cubierto
- ✅ `GET /simon/history` - 100% cubierto
- ✅ `GET /simon/history/{semester}` - 100% cubierto

### Casos de uso cubiertos
- ✅ Generación exitosa
- ✅ Manejo de errores (sin datos, IOException, errores de BD)
- ✅ Validación de estructura del Excel
- ✅ Mapeo de datos
- ✅ Preview de datos
- ✅ Historial de generaciones
- ✅ Filtrado por semestre
- ✅ Formato de fechas
- ✅ Cálculo de semanas
- ✅ Parámetros por defecto y personalizados

---

## 🔍 Verificaciones Importantes

### Datos de prueba
Los tests usan datos mockeados que simulan:
- Monitor con código 2220001
- Curso "Programación Orientada a Objetos" (ID: 9704)
- Profesor "Dr. Carlos Rodríguez"
- Monitoría aprobada de 13 semanas
- 30 horas totales

### Formato del archivo Excel
- 17 columnas con headers específicos
- Datos alineados correctamente
- Formato de fecha dd/MM/yyyy
- Campos calculados (semanas, horas)

### Endpoints REST
Todos los endpoints están probados con:
- Casos exitosos
- Casos de error
- Validación de códigos HTTP
- Validación de estructura JSON
- Validación de headers HTTP

---

## 📝 Notas para Desarrolladores

### Mantenimiento
Si se modifica la funcionalidad de SIMON, actualizar:
1. Los tests correspondientes
2. Esta documentación
3. Los datos de prueba si cambia el esquema

### Agregar nuevos tests
1. Seguir la convención de nombres: `metodo_Escenario_ResultadoEsperado()`
2. Usar `@Test` y `@WithMockUser` cuando sea necesario
3. Documentar el propósito del test
4. Incluir secciones: Arrange, Act, Assert

### Datos de prueba comunes
Los datos están en el método `setUp()` de cada clase de test.
Si necesitas datos adicionales, créalos en el test específico.

---

## ✅ Checklist antes del PR

- [x] Todas las pruebas pasan
- [x] No hay errores de linter
- [x] Cobertura de código adecuada
- [x] Documentación actualizada
- [x] Casos de error cubiertos
- [x] Validaciones de formato incluidas

---

## 📞 Soporte

Para preguntas sobre las pruebas:
1. Revisar esta documentación
2. Ver comentarios en el código de los tests
3. Consultar con el equipo de desarrollo

---

**Última actualización:** Octubre 2024  
**Versión:** 1.0  
**Autor:** Equipo PDG-SIGMA

