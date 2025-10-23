-- ================================================
-- INSTRUCCIONES DE EJECUCIÓN
-- ================================================

# ORDEN DE EJECUCIÓN DE LOS SCRIPTS SQL

Ejecuta los scripts en este orden exacto:

## 1️⃣ CREAR LAS BASES DE DATOS (solo primera vez)
```sql
-- Ejecuta en PostgreSQL (conexión a 'postgres')
CREATE DATABASE sigma_db;
CREATE DATABASE banner_db;
```

## 2️⃣ CREAR TABLAS Y USUARIOS BÁSICOS

### En banner_db:
```bash
# Ejecuta: 1_datos_banner.sql
# Crea: prospect, professor, department_head con usuarios demo
```

### En sigma_db:
```bash
# Ejecuta: 2_datos_sigma.sql
# Crea: tablas académicas + replica usuarios de banner_db
```

## 3️⃣ INSERTAR TODAS LAS FACULTADES Y CURSOS

### En AMBAS bases de datos (banner_db Y sigma_db):
```bash
# Ejecuta: 3_datos_facultades_completas.sql
# Inserta:
# - 6 Facultades
# - 16 Programas académicos
# - 21 Cursos
# - 20 Estudiantes adicionales (solo banner_db)
# - Matrícula de 15-20 estudiantes por curso (solo banner_db)
```

## 4️⃣ VERIFICACIÓN

Ejecuta estas consultas para verificar:

### En sigma_db:
```sql
-- Ver facultades
SELECT * FROM school;

-- Ver programas por facultad
SELECT s.name as facultad, p.name as programa
FROM school s
JOIN program p ON s.id = p.school_id
ORDER BY s.name, p.name;

-- Ver cursos por programa
SELECT p.name as programa, c.name as curso
FROM program p
JOIN course c ON p.id = c.program_id
ORDER BY p.name, c.name;
```

### En banner_db:
```sql
-- Ver matrícula por curso
SELECT c.name as curso, COUNT(sc.student_id) as estudiantes
FROM course c
LEFT JOIN student_course sc ON c.id = sc.course_id
GROUP BY c.name
ORDER BY estudiantes DESC;
```

## 5️⃣ LISTO PARA PROBAR

Ahora puedes:
✅ Iniciar sesión como jefe de departamento (5001 / jefe123)
✅ Cargar cualquiera de los 6 CSV creados
✅ El sistema validará que cada curso tiene 15+ estudiantes
✅ Se crearán las monitorías automáticamente

## 📁 ARCHIVOS CSV DISPONIBLES:

1. monitorias_barberi_ingenieria.csv (6 cursos)
2. monitorias_ciencias_salud.csv (4 cursos)
3. monitorias_negocios_economia.csv (4 cursos)
4. monitorias_ciencias_humanas.csv (3 cursos)
5. monitorias_innovacion.csv (2 cursos)
6. monitorias_educacion.csv (2 cursos)
7. monitorias_demo.csv (21 cursos - TODOS juntos)

## ⚠️ NOTAS IMPORTANTES:

1. **El script 3_datos_facultades_completas.sql debe ejecutarse en AMBAS bases de datos**
   - En banner_db: Crea la estructura académica completa
   - En sigma_db: Replica la estructura para operar independientemente

2. **La sección de estudiantes (paso 5 y 6) solo aplica para banner_db**
   - sigma_db NO necesita tabla prospect ni student_course
   - Solo banner_db maneja matrícula para validar el criterio de 15+

3. **Si ya ejecutaste los scripts anteriores (1_datos_banner.sql y 2_datos_sigma.sql)**
   - Puedes ejecutar directamente el script 3 sin problemas
   - Los `ON CONFLICT DO NOTHING` evitan duplicados

4. **Para resetear todo (opcional):**
```sql
-- En sigma_db:
DROP TABLE IF EXISTS monitoring CASCADE;
DROP TABLE IF EXISTS course_professor CASCADE;
DROP TABLE IF EXISTS head_professor CASCADE;
DROP TABLE IF EXISTS course CASCADE;
DROP TABLE IF EXISTS program CASCADE;
DROP TABLE IF EXISTS school CASCADE;
DROP TABLE IF EXISTS professor CASCADE;
DROP TABLE IF EXISTS department_head CASCADE;

-- En banner_db:
DROP TABLE IF EXISTS student_course CASCADE;
DROP TABLE IF EXISTS course_professor CASCADE;
DROP TABLE IF EXISTS head_program CASCADE;
DROP TABLE IF EXISTS course CASCADE;
DROP TABLE IF EXISTS program CASCADE;
DROP TABLE IF EXISTS school CASCADE;
DROP TABLE IF EXISTS prospect CASCADE;
DROP TABLE IF EXISTS professor CASCADE;
DROP TABLE IF EXISTS department_head CASCADE;

-- Luego volver a ejecutar los 3 scripts en orden
```

## 🎯 RESUMEN RÁPIDO:

```
1. crear_bases_datos.sql         → Crea sigma_db y banner_db
2. 1_datos_banner.sql            → En banner_db (usuarios base)
3. 2_datos_sigma.sql             → En sigma_db (usuarios base + tablas)
4. 3_datos_facultades_completas.sql → En AMBAS (facultades, programas, cursos, estudiantes)
5. ✅ Listo para usar la aplicación
```
