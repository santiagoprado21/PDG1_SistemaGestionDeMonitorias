# BulkMonitoringImportTest - Casos de Prueba

## Clase
`BulkMonitoringImportTest`

## Objetivo
Validar la importación masiva de monitorías mediante archivos CSV y Excel, asegurando:
- Creación correcta de monitorías.
- Validación de encabezados.
- Validación de semestres.
- Validación de datos obligatorios.
- Manejo de duplicados.
- Compatibilidad de formatos.

---

# Casos de Prueba

## 1. Importar CSV válido y crear monitorías exitosamente

### Método
`acceptsCsv_creates_all_rows()`

### Objetivo
Verificar que un archivo CSV válido cree correctamente todas las monitorías.

### Flujo
1. Crear estructura académica de prueba.
2. Generar archivo CSV válido con dos filas.
3. Ejecutar importación.
4. Verificar creación exitosa.

### Resultado Esperado
- Se crean 2 monitorías.
- No existen filas omitidas.
- El repositorio contiene 2 registros.

---

## 2. Rechazar CSV con encabezado inválido

### Método
`rejectsCsv_with_invalid_header()`

### Objetivo
Validar que el sistema rechace archivos con encabezados incorrectos.

### Flujo
1. Crear CSV con encabezado alterado.
2. Ejecutar importación.

### Resultado Esperado
- Se lanza excepción.
- El mensaje contiene “incompatibilidad”.

---

## 3. Importar Excel válido exitosamente

### Método
`acceptsExcel_creates_all_rows()`

### Objetivo
Validar la importación correcta de archivos Excel `.xlsx`.

### Flujo
1. Crear workbook Excel válido.
2. Agregar dos monitorías.
3. Ejecutar importación.

### Resultado Esperado
- Se crean 2 monitorías.
- No existen omitidas.

---

## 4. Aceptar CSV con contentType MS Excel

### Método
`acceptsCsv_with_msexcel_contentType_creates_all()`

### Objetivo
Verificar compatibilidad con `application/vnd.ms-excel`.

### Flujo
1. Crear CSV válido.
2. Asignar contentType MS Excel.
3. Ejecutar importación.

### Resultado Esperado
- Se crean correctamente las monitorías.
- No existen omitidas.

---

## 5. Aceptar encabezados con espacios extra

### Método
`acceptsCsv_with_header_trailing_spaces()`

### Objetivo
Validar tolerancia a espacios en encabezados.

### Flujo
1. Crear CSV con espacios en encabezado.
2. Ejecutar importación.

### Resultado Esperado
- Importación exitosa.
- Se crea la monitoría.

---

## 6. Rechazar semestre con año incorrecto

### Método
`rejectsCsv_with_wrong_year_in_semester()`

### Objetivo
Validar que el año del semestre coincida con el actual.

### Flujo
1. Crear CSV con período inválido.
2. Ejecutar importación.

### Resultado Esperado
- Se lanza excepción.
- Mensaje contiene “año actual”.

---

## 7. Rechazar semestre inválido según mes

### Método
`rejectsCsv_with_invalid_semester_for_month()`

### Objetivo
Verificar consistencia entre semestre y fecha.

### Flujo
1. Crear CSV con semestre inconsistente.
2. Ejecutar importación.

### Resultado Esperado
- Se lanza excepción.
- Mensaje contiene “semestre actual”.

---

## 8. Rechazar CSV con campos vacíos

### Método
`rejectsCsv_with_blank_field()`

### Objetivo
Validar obligatoriedad de campos.

### Flujo
1. Crear CSV con valor vacío.
2. Ejecutar importación.

### Resultado Esperado
- Se lanza excepción.
- Mensaje contiene “incompatibilidad”.

---

## 9. Omitir monitoría duplicada

### Método
`csv_duplicate_course_is_omitted_for_same_prof_and_semester()`

### Objetivo
Verificar que no se dupliquen monitorías del mismo profesor y semestre.

### Flujo
1. Importar monitoría válida.
2. Repetir importación.
3. Validar omisión.

### Resultado Esperado
- Segunda importación omitida.
- Mensaje indica duplicado existente.

---

## 10. Rechazar Excel con encabezado inválido

### Método
`rejectsExcel_with_invalid_header()`

### Objetivo
Validar encabezados incorrectos en archivos Excel.

### Flujo
1. Crear Excel con encabezado inválido.
2. Ejecutar importación.

### Resultado Esperado
- Se lanza excepción.
- Mensaje contiene “incompatibilidad”.

---

# Cobertura Validada

- Importación CSV.
- Importación Excel.
- Validación de encabezados.
- Validación de formatos.
- Validación de períodos académicos.
- Validación de campos obligatorios.
- Prevención de duplicados.
- Compatibilidad de content types.
- Persistencia de monitorías.
