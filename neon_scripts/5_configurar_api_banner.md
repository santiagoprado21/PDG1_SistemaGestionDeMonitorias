# Configuración del API Banner para Neon

## ¿Qué es el API Banner?

El API Banner (`API-Banner-main`) es un servicio separado que simula el sistema institucional BANNER. 
Este API se conecta al schema `banner` donde están los datos de estudiantes, profesores, cursos, etc.

## Configuración

### 1. Actualizar application-cloud.properties

Abre: `API-Banner-main/src/main/resources/application-cloud.properties`

Ya está actualizado con la plantilla de Neon, solo reemplaza con TUS credenciales:

```properties
# CONEXIÓN A NEON - REEMPLAZA CON TUS CREDENCIALES
spring.datasource.url=jdbc:postgresql://TU-HOST-NEON.neon.tech:5432/neondb?sslmode=require&currentSchema=banner
spring.datasource.username=TU-USUARIO-NEON
spring.datasource.password=TU-PASSWORD-NEON
```

**IMPORTANTE**: Nota el `&currentSchema=banner` al final de la URL. Esto hace que este API use el schema `banner`.

### 2. Activar Perfil Cloud

Abre: `API-Banner-main/src/main/resources/application.properties`

Cambia:
```properties
# spring.profiles.active=local
spring.profiles.active=cloud
```

### 3. Verificar Puertos

El API Banner corre en un puerto diferente al backend principal:

- **Backend Principal (Sigma)**: Puerto `5433`
- **API Banner**: Puerto `5431`

Esto permite correr ambos simultáneamente.

## Probar el API Banner

### Iniciar el API

```bash
cd API-Banner-main
mvnw spring-boot:run
```

O en PowerShell Windows:
```powershell
cd API-Banner-main
.\mvnw.cmd spring-boot:run
```

### Verificar Logs

Deberías ver:
```
Started BannerApplication in X seconds
```

### Probar Endpoints

Si el API está corriendo, puedes probar endpoints como:

```
GET http://localhost:5431/api/prospects
GET http://localhost:5431/api/professors
GET http://localhost:5431/api/courses
```

(Ajusta según los endpoints reales de tu API)

## Arquitectura

```
┌─────────────────────────────────────────┐
│         NEON POSTGRESQL                 │
│                                         │
│  ┌─────────────┐    ┌──────────────┐  │
│  │   Schema    │    │   Schema     │  │
│  │   BANNER    │    │   SIGMA      │  │
│  │             │    │              │  │
│  │ - school    │    │ - school     │  │
│  │ - program   │    │ - program    │  │
│  │ - course    │    │ - course     │  │
│  │ - prospect  │    │ - prospect   │  │
│  │ - professor │    │ - professor  │  │
│  │ ...         │    │ - monitoring │  │
│  │             │    │ - activity   │  │
│  │             │    │ ...          │  │
│  └──────▲──────┘    └──────▲───────┘  │
│         │                   │          │
└─────────┼───────────────────┼──────────┘
          │                   │
    ┌─────┴──────┐    ┌──────┴───────┐
    │ API Banner │    │   Backend    │
    │  :5431     │    │   Sigma      │
    └────────────┘    │   :5433      │
                      └──────────────┘
```

## Diferencias con Local

### Antes (Local):
- 2 bases de datos: `banner_db` y `sigma_db`
- Cada una en PostgreSQL local
- Cada desarrollador tiene sus propios datos

### Ahora (Neon):
- 1 base de datos: `neondb`
- 2 schemas: `banner` y `sigma`
- Todos comparten los mismos datos en la nube

## Ventajas

✅ Una sola instancia de base de datos
✅ Datos compartidos entre el equipo
✅ Más fácil de mantener
✅ No necesitas correr PostgreSQL localmente

## Solución de Problemas

### Error: "Port 5431 already in use"
Otro servicio está usando el puerto. Cambia el puerto en `application-cloud.properties`:
```properties
server.port=5430
```

### Error: "Schema banner does not exist"
No ejecutaste el script `2_datos_banner.sql`. Ejecútalo en Neon SQL Editor.

### Error: "Cannot resolve table"
El API está buscando tablas en el schema incorrecto. Verifica:
```properties
spring.jpa.properties.hibernate.default_schema=banner
```

### API Banner no responde
1. Verifica que esté corriendo (revisa los logs)
2. Verifica el puerto correcto
3. Verifica que no haya errores de conexión a BD

## ¿Es Opcional el API Banner?

Depende de tu arquitectura:

- **Si tu backend Sigma se conecta directamente a las tablas**: El API Banner puede ser opcional
- **Si usas el API Banner como servicio REST**: Necesitas correrlo

Para verificar, busca en el código del backend si hay llamadas HTTP al API Banner:
```java
// Buscar referencias como:
WebClient, RestTemplate, @FeignClient
// que apunten a localhost:5431 o al API Banner
```

Si encuentras referencias, necesitas el API Banner corriendo.

## Checklist

- [ ] `application-cloud.properties` del API Banner actualizado
- [ ] Credenciales de Neon configuradas (mismas del backend)
- [ ] Schema `banner` especificado en la URL
- [ ] Perfil `cloud` activado
- [ ] API Banner inicia sin errores
- [ ] Endpoints responden correctamente

---

**Nota**: Si no usas el API Banner en tu proyecto, puedes ignorar esta configuración y solo trabajar con el backend principal (Sigma).

