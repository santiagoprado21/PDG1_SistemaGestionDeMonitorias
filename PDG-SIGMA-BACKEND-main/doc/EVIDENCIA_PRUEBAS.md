# Evidencia de pruebas HU: Carga masiva de cursos elegibles

Este documento resume cómo ejecutar y evidenciar las pruebas automatizadas de la HU de carga masiva, con la regla de **sin mínimo de 15 estudiantes** y con salida de pruebas limpia (warnings reducidos).

## Cambios clave para salida limpia
- Se eliminó `commons-logging` del classpath para evitar conflicto con `spring-jcl`.
- Se removió `hibernate.dialect` explícito para H2 en perfil `test` (detección automática).
- Se puso `spring.jpa.open-in-view=false` en `application-test.properties`.
- Se silenció el log de Spring Security en pruebas: `logging.level.org.springframework.security=ERROR`.
- Se añadió `-XX:+EnableDynamicAgentLoading` en Surefire para ocultar el warning del agente de ByteBuddy.

Archivos modificados:
- `pom.xml`
- `src/main/resources/application-test.properties`

## Cómo ejecutar

- Solo la HU (clase focalizada):
```powershell
./mvnw -Dtest=BulkMonitoringImportTest test
```

- Todo el backend:
```powershell
./mvnw clean test
```

## Dónde ver los resultados
Tras la ejecución, revisa los reportes en:
```
PDG-SIGMA-BACKEND-main/target/surefire-reports/
```
Archivos relevantes:
- `com.pdg.sigma.BulkMonitoringImportTest.txt`
- `com.pdg.sigma.BulkMonitoringImportTest.xml`

En el `.txt` verás una línea con el resumen, por ejemplo:
```
Tests run: N, Failures: 0, Errors: 0, Skipped: 0
```
Y en consola, el comando sin `-q` mostrará `BUILD SUCCESS`.

## Qué valida la prueba
`BulkMonitoringImportTest` verifica que:
- Se acepta CSV y Excel con encabezados correctos.
- No se exige el mínimo de 15 estudiantes; se crean monitorías aunque el curso tenga menos estudiantes.
- Se evita crear más de una monitoría por profesor en una misma importación (resguardo por restricción de BD), dejando el resto en “Omitidas”.
- Se valida formato de encabezados, periodo/año y campos obligatorios.
- Se omite el curso si ya existe una monitoría previa para ese curso.

## Evidencia sugerida para presentar
Incluye en tu entrega:
- Captura de consola con `BUILD SUCCESS` de la HU y/o del suite completo.
- Copia/fragmento del archivo `target/surefire-reports/com.pdg.sigma.BulkMonitoringImportTest.txt` con `Failures: 0, Errors: 0`.
- Este archivo `doc/EVIDENCIA_PRUEBAS.md` explicando los cambios y pasos de ejecución.
