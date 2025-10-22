# 🚀 MIGRACIÓN A NEON - PUNTO DE ENTRADA

## ¿Qué es esto?

Tu proyecto ha sido configurado para usar **Neon PostgreSQL** en la nube, permitiendo que todo tu equipo trabaje con la misma base de datos sin necesidad de PostgreSQL local.

---

## 📚 DOCUMENTACIÓN DISPONIBLE

### 🏁 Para Empezar Rápido
**→ Lee primero: [`INICIO_RAPIDO_NEON.md`](INICIO_RAPIDO_NEON.md)**

Esta es la guía paso a paso resumida para poner todo en marcha en 20 minutos.

---

### 📖 Documentación Completa

1. **[GUIA_MIGRACION_NEON.md](GUIA_MIGRACION_NEON.md)**
   - Guía detallada con troubleshooting
   - Explicaciones completas de cada paso

2. **[RESUMEN_MIGRACION.md](RESUMEN_MIGRACION.md)**
   - Vista general de todos los cambios
   - Arquitectura antes vs después
   - Checklist completo

---

### 🗂️ Scripts SQL (carpeta `neon_scripts/`)

**Ejecutar EN ORDEN:**

1. **[1_create_schemas.sql](neon_scripts/1_create_schemas.sql)**
   - Crea los schemas `banner` y `sigma`

2. **[2_datos_banner.sql](neon_scripts/2_datos_banner.sql)**
   - Crea tablas y datos del schema `banner`

3. **[3_datos_sigma.sql](neon_scripts/3_datos_sigma.sql)**
   - Crea tablas y datos del schema `sigma`

---

### 🔧 Guías de Configuración

4. **[4_configurar_java_schemas.md](neon_scripts/4_configurar_java_schemas.md)**
   - Cómo funcionan los schemas en Java
   - Solución de problemas comunes

5. **[5_configurar_api_banner.md](neon_scripts/5_configurar_api_banner.md)**
   - Configuración específica del API Banner

---

## ⚡ INICIO RÁPIDO (TL;DR)

```bash
# 1. Crear cuenta en Neon
https://neon.tech

# 2. Crear proyecto "SwMonitorias"

# 3. Ejecutar scripts SQL en Neon SQL Editor (en orden 1, 2, 3)

# 4. Actualizar archivos de configuración con TUS credenciales:
PDG-SIGMA-BACKEND-main/src/main/resources/application-cloud.properties
API-Banner-main/src/main/resources/application-cloud.properties

# 5. Activar perfil cloud:
spring.profiles.active=cloud

# 6. Iniciar backend
cd PDG-SIGMA-BACKEND-main
mvnw spring-boot:run

# 7. ¡Listo! 🎉
```

---

## 🎯 ¿QUÉ HACER AHORA?

### Si eres el líder del equipo:
1. Lee [`INICIO_RAPIDO_NEON.md`](INICIO_RAPIDO_NEON.md)
2. Crea la cuenta en Neon
3. Ejecuta los scripts SQL
4. Configura tu backend local
5. Comparte las credenciales con tu equipo

### Si eres miembro del equipo:
1. Espera a que el líder comparta las credenciales
2. Actualiza tu `application-cloud.properties` local
3. Activa el perfil cloud
4. ¡Empieza a trabajar!

---

## 📦 ESTRUCTURA DE ARCHIVOS

```
SwMonitorias/
├── LEEME_MIGRACION_NEON.md          ← ESTÁS AQUÍ
├── INICIO_RAPIDO_NEON.md             ← EMPIEZA AQUÍ
├── GUIA_MIGRACION_NEON.md            ← Guía completa
├── RESUMEN_MIGRACION.md              ← Vista general
│
├── neon_scripts/
│   ├── 1_create_schemas.sql          ← Ejecutar primero
│   ├── 2_datos_banner.sql            ← Ejecutar segundo
│   ├── 3_datos_sigma.sql             ← Ejecutar tercero
│   ├── 4_configurar_java_schemas.md  ← Guía Java
│   └── 5_configurar_api_banner.md    ← Guía API Banner
│
├── PDG-SIGMA-BACKEND-main/
│   └── src/main/resources/
│       ├── application.properties             ← Cambiar a cloud
│       └── application-cloud.properties       ← Actualizar credenciales
│
└── API-Banner-main/
    └── src/main/resources/
        ├── application.properties             ← Cambiar a cloud
        └── application-cloud.properties       ← Actualizar credenciales
```

---

## ❓ PREGUNTAS FRECUENTES

**P: ¿Necesito instalar PostgreSQL localmente?**
R: ¡No! Neon está en la nube, no necesitas nada local.

**P: ¿Es gratis?**
R: Sí, Neon es gratis hasta 3GB. Suficiente para desarrollo.

**P: ¿Los 3 podemos usar la misma cuenta?**
R: Sí, todos usan las mismas credenciales de conexión.

**P: ¿Qué pasa si alguien modifica datos?**
R: Todos verán los cambios inmediatamente. Es compartido.

**P: ¿Y si algo sale mal?**
R: Neon hace backups automáticos. Además, tienes los scripts para recrear todo.

**P: ¿Necesito el API Banner?**
R: Depende de tu arquitectura. Lee [`5_configurar_api_banner.md`](neon_scripts/5_configurar_api_banner.md).

---

## 🆘 AYUDA

### Problemas de conexión
→ Revisa [`GUIA_MIGRACION_NEON.md`](GUIA_MIGRACION_NEON.md) sección "Problemas Comunes"

### Errores con schemas
→ Lee [`4_configurar_java_schemas.md`](neon_scripts/4_configurar_java_schemas.md)

### API Banner no funciona
→ Consulta [`5_configurar_api_banner.md`](neon_scripts/5_configurar_api_banner.md)

---

## ✅ CHECKLIST RÁPIDO

- [ ] Leí `INICIO_RAPIDO_NEON.md`
- [ ] Creé cuenta en Neon
- [ ] Ejecuté los 3 scripts SQL
- [ ] Actualicé `application-cloud.properties`
- [ ] Activé perfil cloud
- [ ] Backend inicia sin errores
- [ ] Puedo hacer login
- [ ] (Opcional) API Banner configurado

---

## 🎓 SIGUIENTE PASO

Una vez completada la migración:

**→ Iniciar desarrollo de HU-004: Generación de archivo para SIMON**

---

## 🌟 BENEFICIOS DE LA MIGRACIÓN

✅ Datos compartidos en tiempo real
✅ Sin problemas de sincronización
✅ No más "funciona en mi máquina"
✅ Base de datos profesional en la nube
✅ Sin costo para desarrollo
✅ Backups automáticos

---

**¿Listo para empezar?**

→ **[INICIO_RAPIDO_NEON.md](INICIO_RAPIDO_NEON.md)** ← Empieza aquí

¡Éxito! 🚀

