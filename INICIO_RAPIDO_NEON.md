# 🚀 INICIO RÁPIDO - MIGRACIÓN A NEON

Esta es la guía resumida y práctica. Para más detalles, ver `GUIA_MIGRACION_NEON.md`.

---

## 📝 PASO 1: Crear Cuenta en Neon (5 minutos)

1. Ve a: **https://neon.tech**
2. Click en **"Sign Up"**
3. Regístrate con GitHub (más rápido) o Google
4. Verificar email si es necesario

---

## 📊 PASO 2: Crear Proyecto (2 minutos)

1. En el dashboard de Neon, click **"Create Project"**
2. Configura:
   - **Project Name**: `SwMonitorias`
   - **Region**: `US East (Ohio)` o el más cercano
   - **PostgreSQL Version**: `16` (última)
   - **Plan**: `Free` ✅

3. Click **"Create Project"**

4. **IMPORTANTE**: Copia y guarda las credenciales que aparecen:

```
Connection String:
postgresql://username:password@ep-xxx-xxx-xxx.us-east-2.aws.neon.tech/neondb?sslmode=require

'postgresql://neondb_owner:npg_RucBQ6Vo3ZsF@ep-wispy-firefly-aeueocfk-pooler.c-2.us-east-2.aws.neon.tech/neondb?sslmode=require&channel_binding=require'


Host: ep-xxx-xxx-xxx.us-east-2.aws.neon.tech
Database: neondb
User: username
Password: **********************
Port: 5432
```

---

## 💾 PASO 3: Ejecutar Scripts SQL (10 minutos)

1. En Neon, ve al **"SQL Editor"** (menú lateral izquierdo)

2. Ejecuta los scripts EN ORDEN:

### Script 1: Crear Schemas
```sql
-- Copia y pega el contenido de: neon_scripts/1_create_schemas.sql
```
✅ Deberías ver: "Schemas banner y sigma creados exitosamente!"

### Script 2: Datos Banner
```sql
-- Copia y pega el contenido de: neon_scripts/2_datos_banner.sql
```
✅ Deberías ver: "Datos creados exitosamente en schema BANNER!"

### Script 3: Datos Sigma
```sql
-- Copia y pega el contenido de: neon_scripts/3_datos_sigma.sql
```
✅ Deberías ver: "Schema SIGMA creado exitosamente con todas las tablas!"

---

## ⚙️ PASO 4: Configurar Backend (3 minutos)

### 4.1 Actualizar application-cloud.properties

Abre: `PDG-SIGMA-BACKEND-main/src/main/resources/application-cloud.properties`

Reemplaza con TUS credenciales de Neon:

```properties
# CONEXIÓN A NEON - REEMPLAZA CON TUS CREDENCIALES
spring.datasource.url=jdbc:postgresql://TU-HOST-NEON.neon.tech:5432/neondb?sslmode=require
spring.datasource.username=TU-USUARIO-NEON
spring.datasource.password=TU-PASSWORD-NEON
```

**Ejemplo real** (usa TUS credenciales, no estas):
```properties
spring.datasource.url=jdbc:postgresql://ep-cool-morning-12345678.us-east-2.aws.neon.tech:5432/neondb?sslmode=require
spring.datasource.username=alex_user_987
spring.datasource.password=npg_AbC123XyZ456
```

### 4.2 Activar Perfil Cloud

Abre: `PDG-SIGMA-BACKEND-main/src/main/resources/application.properties`

Cambia esta línea:

```properties
# spring.profiles.active=local
spring.profiles.active=cloud
```

---

## 🧪 PASO 5: Probar Conexión (2 minutos)

### 5.1 Iniciar Backend

```bash
cd PDG-SIGMA-BACKEND-main
mvnw spring-boot:run
```

O en PowerShell Windows:
```powershell
cd PDG-SIGMA-BACKEND-main
.\mvnw.cmd spring-boot:run
```

### 5.2 Verificar Logs

Deberías ver algo como:
```
Started SigmaApplication in 8.234 seconds
```

**SIN** errores de conexión a base de datos.

### 5.3 Probar Login

Si el frontend está corriendo, prueba iniciar sesión con:

**Estudiante:**
- Usuario: `2220001`
- Contraseña: `123456`

**Profesor:**
- Usuario: `1001`
- Contraseña: `prof123`

**Jefe de Departamento:**
- Usuario: `5001`
- Contraseña: `jefe123`

---

## 👥 PASO 6: Compartir con el Equipo

### 6.1 Compartir Credenciales de Neon

Comparte con tus compañeros (por WhatsApp, Telegram, etc.):
- Host
- Usuario
- Contraseña
- Database (neondb)

### 6.2 Cada Miembro del Equipo Debe:

1. Actualizar su archivo `application-cloud.properties` con las credenciales
2. Cambiar a `spring.profiles.active=cloud` en `application.properties`
3. Reiniciar su backend

### 6.3 ¡Listo!

✅ Los 3 trabajan con la misma base de datos en la nube
✅ Cambios visibles para todos en tiempo real
✅ No más sincronización de datos locales

---

## 🎯 VERIFICACIÓN RÁPIDA

- [ ] Cuenta creada en Neon
- [ ] Proyecto creado
- [ ] Credenciales guardadas
- [ ] Script 1 ejecutado (schemas creados)
- [ ] Script 2 ejecutado (datos banner)
- [ ] Script 3 ejecutado (datos sigma)
- [ ] `application-cloud.properties` actualizado
- [ ] Perfil `cloud` activado
- [ ] Backend inicia sin errores
- [ ] Puedes hacer login
- [ ] Equipo tiene las credenciales

---

## ❌ PROBLEMAS COMUNES

### "Connection refused" o "timeout"
- Verifica que copiaste el host completo
- Asegúrate de incluir `?sslmode=require` al final de la URL

### "Authentication failed"
- Copia exactamente usuario y contraseña desde Neon
- No debe haber espacios extras

### "Schema does not exist"
- Verifica que ejecutaste los 3 scripts SQL en orden

### Backend busca tablas en "public"
- Verifica que `spring.jpa.properties.hibernate.default_schema=sigma` esté en `application-cloud.properties`

---

## 📚 MÁS INFORMACIÓN

- Guía completa: `GUIA_MIGRACION_NEON.md`
- Configuración schemas Java: `neon_scripts/4_configurar_java_schemas.md`
- Dashboard Neon: https://console.neon.tech
- Documentación Neon: https://neon.tech/docs

---

## ✅ SIGUIENTE PASO

Una vez la migración esté completa y los 3 miembros puedan conectarse:

**Continuar con HU-004: Generación de archivo para SIMON**

¡Éxito! 🎉

