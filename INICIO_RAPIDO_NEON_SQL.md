# ⚡ Inicio Rápido - Ejecutar Scripts en Neon SQL Editor

## 🎯 La Forma MÁS FÁCIL (3 minutos)

No necesitas instalar **nada**. Todo desde el navegador web.

---

## PASO 1: Abrir Neon SQL Editor (30 segundos)

1. Ir a: **https://console.neon.tech**
2. **Login** con tu cuenta
3. Seleccionar tu proyecto (debería aparecer automáticamente)
4. En el menú lateral izquierdo, click en **"SQL Editor"**

![Captura de pantalla conceptual]
```
┌─────────────────────────────────────────┐
│  🏠 Dashboard                           │
│  📊 Databases                           │
│  ⚙️  Settings                           │
│  📝 SQL Editor  ← CLICK AQUÍ           │
└─────────────────────────────────────────┘
```

✅ Ya estás listo para ejecutar SQL!

---

## PASO 2: Ejecutar Migración SQL (1 minuto)

### 2.1. Abrir el script
En tu computadora, abrir el archivo:
```
C:\dev\SwMonitorias\neon_scripts\6_hu010_create_new_tables.sql
```

### 2.2. Copiar TODO el contenido
- Dentro del archivo, presionar: **Ctrl + A** (seleccionar todo)
- Luego: **Ctrl + C** (copiar)

### 2.3. Pegar en Neon SQL Editor
- Volver a Neon en el navegador
- Click en el área del editor (donde dice "Enter a query")
- Presionar: **Ctrl + V** (pegar)

### 2.4. Ejecutar
- Click en el botón verde **"Run"** (arriba a la derecha)
- O presionar: **Ctrl + Enter**

### 2.5. Esperar confirmación
Verás mensajes como:
```
NOTICE: ✓ Tabla monitoring_request creada
NOTICE: ✓ Tabla monitor_application creada
NOTICE: ✓ Columna monitoring.monitoring_request_id agregada
...
NOTICE: Migración completada exitosamente!
```

✅ ¡Migración completa!

---

## PASO 3: Obtener IDs para Pruebas (1 minuto)

### 3.1. Limpiar el editor
- En Neon SQL Editor, seleccionar todo: **Ctrl + A**
- Borrar: **Delete**

### 3.2. Abrir script de IDs
En tu computadora, abrir:
```
C:\dev\SwMonitorias\neon_scripts\8_obtener_ids_para_pruebas.sql
```

### 3.3. Copiar y pegar
- En el archivo: **Ctrl + A** → **Ctrl + C**
- En Neon: **Ctrl + V**

### 3.4. Ejecutar
- Click en **"Run"** o **Ctrl + Enter**

### 3.5. Ver resultados
Verás **tablas** con datos como:

**Tabla 1: PROFESOR**
```
| ID      | Nombre Completo    | email              |
|---------|--------------------|--------------------|
| PROF001 | Juan Pérez García  | juan@example.com   |
| PROF002 | María López Silva  | maria@example.com  |
```

**Tabla 2: CURSO**
```
| ID | Nombre del Curso | Código   |
|----|------------------|----------|
| 1  | Cálculo I        | CALC-101 |
| 2  | POO              | PROG-201 |
```

**... y así sucesivamente**

### 3.6. Copiar los IDs
Copiar los valores de las columnas "ID" de cada tabla:
- `professorId`: Ejemplo `PROF001`
- `courseId`: Ejemplo `1`
- `schoolId`: Ejemplo `1`
- `programId`: Ejemplo `1`
- `student1Id`: Ejemplo `2020111001`
- `student2Id`: Ejemplo `2020111002`
- `student3Id`: Ejemplo `2020111003`
- `deptHeadId`: Ejemplo `DEPT001`

✅ Ya tienes todos los IDs necesarios!

---

## PASO 4: Verificar que Todo Funciona (30 segundos)

### En Neon SQL Editor, pegar y ejecutar:

```sql
SET search_path TO sigma;

-- Ver tablas creadas
SELECT table_name 
FROM information_schema.tables 
WHERE table_schema = 'sigma' 
  AND table_name IN ('monitoring_request', 'monitor_application')
ORDER BY table_name;
```

### Deberías ver:
```
| table_name           |
|----------------------|
| monitor_application  |
| monitoring_request   |
```

✅ **¡PERFECTO! La migración está lista.**

---

## 💡 Tips de Neon SQL Editor

### ✨ Características útiles:
- **Auto-completado**: Empieza a escribir y sugiere tablas/columnas
- **Múltiples queries**: Separa con `;` y ejecuta varios a la vez
- **Historial**: Click en "History" para ver queries anteriores
- **Formato**: El editor colorea la sintaxis SQL
- **Resultados en tabla**: Los resultados se ven en una tabla bonita
- **Exportar**: Puedes exportar los resultados a CSV

### ⌨️ Atajos de teclado:
- **Ctrl + Enter**: Ejecutar query
- **Ctrl + A**: Seleccionar todo
- **Ctrl + /**: Comentar/descomentar línea
- **Ctrl + K**: Formatear SQL (en algunos editores)

### 🔍 Comandos útiles:

**Ver todas las tablas del schema sigma:**
```sql
SET search_path TO sigma;
SELECT table_name FROM information_schema.tables 
WHERE table_schema = 'sigma' 
ORDER BY table_name;
```

**Ver estructura de una tabla:**
```sql
SELECT column_name, data_type, is_nullable
FROM information_schema.columns
WHERE table_schema = 'sigma' AND table_name = 'monitoring_request'
ORDER BY ordinal_position;
```

**Contar registros:**
```sql
SELECT 
    'monitoring_request' as tabla, COUNT(*) as registros 
FROM monitoring_request
UNION ALL
SELECT 
    'monitor_application', COUNT(*) 
FROM monitor_application;
```

---

## 🚨 Errores Comunes

### ❌ "schema sigma does not exist"
**Solución:** Agregar al inicio del script:
```sql
SET search_path TO sigma;
```

### ❌ "relation does not exist"
**Solución:** Asegurarte de ejecutar primero el script de migración completo.

### ❌ "column already exists"
**Solución:** Ya ejecutaste el script antes. Si quieres re-ejecutar:
```sql
-- Ejecutar primero el rollback
\i C:/dev/SwMonitorias/neon_scripts/6_hu010_rollback.sql
-- Luego volver a ejecutar la migración
```

### ❌ Query muy largo, timeout
**Solución:** En Neon, aumentar el timeout:
- Settings → Compute → Autoscaling → Max timeout

---

## 📋 Checklist

- [ ] Abrí Neon SQL Editor
- [ ] Ejecuté `6_hu010_create_new_tables.sql`
- [ ] Vi mensajes de confirmación
- [ ] Ejecuté `8_obtener_ids_para_pruebas.sql`
- [ ] Copié los 8 IDs necesarios
- [ ] Verifiqué que las tablas existen

---

## ➡️ Siguiente Paso

Ahora que la base de datos está lista, continúa con:

**`HU010_GUIA_PRUEBAS.md`** → Sección 3: "Iniciar el Backend"

O salta directo a probar los endpoints con Postman! 🚀

---

## 🎉 Ventajas de Neon SQL Editor

✅ No requiere instalar nada  
✅ Funciona desde el navegador  
✅ Interfaz visual clara  
✅ Auto-guarda el historial  
✅ Resultados en formato tabla  
✅ Copiar/pegar fácil  
✅ Acceso desde cualquier lugar  

**¡Es la forma más rápida de probar scripts SQL!** ⚡

