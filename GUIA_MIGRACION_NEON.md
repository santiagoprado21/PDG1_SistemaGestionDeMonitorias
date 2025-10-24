# 🚀 GUÍA DE MIGRACIÓN A NEON (PostgreSQL en la Nube)

## 📋 ÍNDICE
1. [Registro en Neon](#paso-1-registro-en-neon)
2. [Crear Proyecto y Bases de Datos](#paso-2-crear-proyecto-en-neon)
3. [Ejecutar Scripts SQL](#paso-3-ejecutar-scripts-sql)
4. [Configurar Backend](#paso-4-configurar-backend)
5. [Probar Conexión](#paso-5-probar-conexión)
6. [Compartir con el Equipo](#paso-6-compartir-con-el-equipo)

---

## PASO 1: Registro en Neon

### 1.1 Crear Cuenta
1. Ve a: **https://neon.tech**
2. Click en **"Sign Up"** o **"Get Started"**
3. Regístrate con:
   - ✅ GitHub (recomendado - más rápido)
   - ✅ Google
   - ✅ Email

### 1.2 Verificar Cuenta
- Verifica tu email si usaste ese método
- Acepta los términos de servicio

---

## PASO 2: Crear Proyecto en Neon

### 2.1 Crear Nuevo Proyecto
1. Una vez dentro, click en **"Create Project"** o **"New Project"**
2. Configura:
   - **Project Name**: `SwMonitorias` (o el nombre que prefieras)
   - **Region**: Selecciona `US East` o la más cercana a Colombia/tu ubicación
   - **PostgreSQL Version**: `16` (o la más reciente)
   - **Plan**: `Free` (3GB de almacenamiento)

3. Click en **"Create Project"**

### 2.2 Obtener Credenciales
Una vez creado el proyecto, verás algo como esto:

```
Connection String:
postgresql://username:password@ep-xxx-xxx.us-east-2.aws.neon.tech/neondb?sslmode=require

Host: ep-xxx-xxx.us-east-2.aws.neon.tech
Database: neondb
User: username
Password: **********************
Port: 5432
```

**⚠️ IMPORTANTE**: 
- Guarda estas credenciales en un lugar seguro
- Las necesitarás más adelante
- No las compartas públicamente

---

## PASO 3: Crear Bases de Datos

### 3.1 Acceder al SQL Editor de Neon
1. En tu proyecto de Neon, ve a **"SQL Editor"** (menú lateral izquierdo)
2. O usa el botón **"Query"** 

### 3.2 Crear las Bases de Datos

**IMPORTANTE**: Neon usa un enfoque diferente. En lugar de crear múltiples bases de datos, 
usaremos **SCHEMAS** dentro de la misma base de datos.

Ejecuta este script en el SQL Editor:

```sql
-- Ver archivo: neon_scripts/1_create_schemas.sql
```

Copia y pega el contenido del archivo `neon_scripts/1_create_schemas.sql` y ejecútalo.

### 3.3 Poblar BANNER Schema
```sql
-- Ver archivo: neon_scripts/2_datos_banner.sql
```

### 3.4 Poblar SIGMA Schema
```sql
-- Ver archivo: neon_scripts/3_datos_sigma.sql
```

---

## PASO 4: Configurar Backend

### 4.1 Actualizar application-cloud.properties

1. Abre el archivo: `PDG-SIGMA-BACKEND-main/src/main/resources/application-cloud.properties`

2. Reemplaza con tus credenciales de Neon:

```properties
spring.datasource.url=jdbc:postgresql://TU-HOST-NEON:5432/neondb?sslmode=require
spring.datasource.username=TU-USUARIO-NEON
spring.datasource.password=TU-PASSWORD-NEON
```

Ejemplo real:
```properties
spring.datasource.url=jdbc:postgresql://ep-cool-morning-12345.us-east-2.aws.neon.tech:5432/neondb?sslmode=require
spring.datasource.username=alex_user_123
spring.datasource.password=npg_S3cr3tP4ssw0rd123xyz
```

### 4.2 Activar Perfil Cloud

Abre: `PDG-SIGMA-BACKEND-main/src/main/resources/application.properties`

Cambia:
```properties
# spring.profiles.active=local
spring.profiles.active=cloud
```

---

## PASO 5: Probar Conexión

### 5.1 Desde el Backend
```bash
cd PDG-SIGMA-BACKEND-main
mvn spring-boot:run
```

Deberías ver en la consola:
```
Started SigmaApplication in X seconds
```

Sin errores de conexión.

### 5.2 Verificar en Neon Dashboard
1. Ve a tu proyecto en Neon
2. Click en **"Monitoring"** o **"Usage"**
3. Deberías ver conexiones activas

---

## PASO 6: Compartir con el Equipo

### 6.1 Compartir Credenciales de Forma Segura
**NO** subas las credenciales a Git. Comparte con tu equipo por:
- ✅ WhatsApp/Telegram (mensaje temporal)
- ✅ Google Drive compartido (archivo protegido)
- ✅ Variables de entorno

### 6.2 Cada Miembro del Equipo Debe:
1. Actualizar su `application-cloud.properties` con las credenciales
2. Cambiar su `application.properties` a `spring.profiles.active=cloud`
3. Reiniciar su backend

### 6.3 Ventajas
✅ Los 3 miembros trabajan con los mismos datos
✅ Cambios en BD visibles para todos inmediatamente
✅ No más problemas de "funciona en mi máquina"

---

## 📊 VERIFICACIÓN FINAL

### Checklist
- [ ] Proyecto creado en Neon
- [ ] Schemas `banner` y `sigma` creados
- [ ] Datos de prueba cargados
- [ ] `application-cloud.properties` actualizado
- [ ] Perfil `cloud` activado en `application.properties`
- [ ] Backend inicia sin errores
- [ ] Equipo puede conectarse

---

## 🆘 PROBLEMAS COMUNES

### Error: "Connection refused"
- Verifica que el host y puerto sean correctos
- Asegúrate de incluir `?sslmode=require`

### Error: "Authentication failed"
- Verifica usuario y contraseña
- Copia exactamente desde Neon (sin espacios extras)

### Error: "Schema not found"
- Verifica que ejecutaste los scripts de creación de schemas
- Asegúrate de ejecutarlos en orden (1, 2, 3)

### Backend busca tabla en schema 'public'
- Revisa que las entidades tengan `@Table(schema = "sigma")` o `@Table(schema = "banner")`
- Ver archivos en `neon_scripts/4_verificar_schemas_java.md`

---

## 📞 SOPORTE

- **Documentación Neon**: https://neon.tech/docs
- **Dashboard Neon**: https://console.neon.tech
- **Comunidad**: https://discord.gg/neon

---

## ✅ SIGUIENTE PASO

Una vez completada la migración, puedes continuar con:
- **HU-004: Generación de archivo para SIMON**

¡Éxito! 🚀

