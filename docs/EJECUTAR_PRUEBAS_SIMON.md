# 🧪 Guía Rápida: Ejecutar Pruebas de SIMON

## 🚀 Ejecución Rápida

### PowerShell (Windows)

```powershell
# Navegar al directorio del backend
cd PDG-SIGMA-BACKEND-main

# Ejecutar pruebas del servicio
.\mvnw.cmd test -Dtest=SimonFileServiceImplTest

# Ejecutar pruebas del controlador
.\mvnw.cmd test -Dtest=SimonFileControllerTest

# Ejecutar todas las pruebas de SIMON
.\mvnw.cmd test -Dtest=Simon*Test
```

### Bash / Git Bash (Windows/Linux/Mac)

```bash
# Navegar al directorio del backend
cd PDG-SIGMA-BACKEND-main

# Ejecutar pruebas del servicio
./mvnw test -Dtest=SimonFileServiceImplTest

# Ejecutar pruebas del controlador
./mvnw test -Dtest=SimonFileControllerTest

# Ejecutar todas las pruebas de SIMON
./mvnw test -Dtest=Simon*Test
```

---

## 📊 Resultado Esperado

Si todo está bien, deberías ver algo como:

```
[INFO] -------------------------------------------------------
[INFO]  T E S T S
[INFO] -------------------------------------------------------
[INFO] Running com.pdg.sigma.SimonFileServiceImplTest
[INFO] Tests run: 10, Failures: 0, Errors: 0, Skipped: 0
[INFO] 
[INFO] Results:
[INFO] 
[INFO] Tests run: 10, Failures: 0, Errors: 0, Skipped: 0
[INFO]
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
```

---

## ❌ Solución de Problemas

### Error: "mvnw no reconocido"

**Problema:** PowerShell no encuentra el comando

**Solución:**
```powershell
# Asegúrate de estar en el directorio correcto
cd C:\dev\SwMonitorias\PDG-SIGMA-BACKEND-main

# Ejecuta con la ruta completa
.\mvnw.cmd test -Dtest=SimonFileServiceImplTest
```

### Error: "No tests were executed"

**Problema:** El nombre del test no coincide

**Solución:** Verifica que los archivos existan:
```powershell
dir src\test\java\com\pdg\sigma\Simon*Test.java
```

### Error: Fallan las pruebas

**Posibles causas:**
1. **Dependencias no descargadas:** Ejecuta `.\mvnw.cmd clean install`
2. **Cambios en el código:** Revisa que no haya modificaciones sin guardar
3. **Base de datos:** Las pruebas usan mocks, no deberían necesitar BD

---

## 🔍 Ver Resultados Detallados

Para ver más información durante la ejecución:

```powershell
.\mvnw.cmd test -Dtest=SimonFileServiceImplTest -X
```

El parámetro `-X` muestra logs de debug.

---

## 📝 Ejecutar Desde IDE

### IntelliJ IDEA
1. Abre el archivo `SimonFileServiceImplTest.java`
2. Click derecho en el archivo
3. Selecciona "Run 'SimonFileServiceImplTest'"

### VS Code
1. Instala la extensión "Java Test Runner"
2. Abre el archivo de test
3. Click en el botón "Run Test" que aparece sobre la clase

### Eclipse
1. Abre el archivo de test
2. Right-click → Run As → JUnit Test

---

## 📦 Ejecutar Todas las Pruebas del Proyecto

```powershell
# Todas las pruebas
.\mvnw.cmd test

# Todas las pruebas con reporte
.\mvnw.cmd test site
```

---

## ✅ Checklist Antes de Hacer el PR

```bash
# 1. Verificar que el código compila
.\mvnw.cmd clean compile

# 2. Ejecutar las pruebas de SIMON
.\mvnw.cmd test -Dtest=Simon*Test

# 3. Ejecutar todas las pruebas del proyecto (opcional pero recomendado)
.\mvnw.cmd test

# 4. Verificar el estado de Git
git status

# 5. Ver los cambios
git diff
```

---

## 🎯 Resultado de las Pruebas

### SimonFileServiceImplTest (10 tests)
- ✅ Generación exitosa con monitorías aprobadas
- ✅ Error cuando no hay monitorías aprobadas
- ✅ Estructura correcta del Excel
- ✅ Mapeo correcto a DTO
- ✅ Procesamiento de múltiples monitorías
- ✅ Historial de generaciones
- ✅ Filtrado por semestre
- ✅ Formato de fechas
- ✅ Parámetros personalizados
- ✅ Conteo correcto de filas

### SimonFileControllerTest (17 tests)
- ✅ Generación exitosa del archivo (GET /simon/generate)
- ✅ Parámetros por defecto
- ✅ Error 204 sin monitorías
- ✅ Error 500 en IOException
- ✅ Preview exitoso (GET /simon/preview)
- ✅ Preview sin datos
- ✅ Preview con múltiples monitorías
- ✅ Error en preview
- ✅ Historial completo (GET /simon/history)
- ✅ Historial vacío
- ✅ Error en historial
- ✅ Historial por semestre (GET /simon/history/{semester})
- ✅ Historial semestre sin resultados
- ✅ Error historial por semestre
- ✅ Nombre de archivo con timestamp
- ✅ Content-Length presente
- ✅ Parámetros personalizados

**Total: 27 pruebas**

---

## 📚 Más Información

Para más detalles sobre las pruebas, ver:
- `doc/SimonFileTestDesign.md` - Documentación completa de las pruebas
- Comentarios en los archivos de test

---

## 🎉 ¡Listo!

Si todas las pruebas pasan (27/27 ✅), estás listo para hacer el PR.

```bash
# Agregar los archivos de test
git add src/test/java/com/pdg/sigma/Simon*.java
git add doc/SimonFileTestDesign.md
git add EJECUTAR_PRUEBAS_SIMON.md

# Hacer commit
git commit -m "test: Agregar pruebas completas para HU-004 generación archivo SIMON"

# Ver el estado antes de push
git status
```

