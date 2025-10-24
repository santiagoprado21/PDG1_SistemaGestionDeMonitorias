# Configuración de Schemas en Java

## Problema
En el proyecto original se usaban dos bases de datos separadas:
- `banner_db` (sistema externo)
- `sigma_db` (aplicación principal)

En Neon, usamos **schemas** en lugar de bases de datos separadas:
- `banner` (schema)
- `sigma` (schema)

## Solución

### 1. Schema por Defecto (application-cloud.properties)

Ya configuramos en `application-cloud.properties`:
```properties
spring.jpa.properties.hibernate.default_schema=sigma
```

Esto significa que todas las entidades sin `@Table(schema = "...")` buscarán las tablas en el schema `sigma`.

### 2. Configuración del API Banner

Si tu proyecto tiene un API Banner separado (`API-Banner-main`) que se conecta al schema `banner`, debes configurarlo así:

#### En API-Banner application.properties:
```properties
# Para conectarse al schema banner en Neon
spring.datasource.url=jdbc:postgresql://TU-HOST-NEON.neon.tech:5432/neondb?sslmode=require&currentSchema=banner
spring.datasource.username=TU-USUARIO-NEON
spring.datasource.password=TU-PASSWORD-NEON
spring.jpa.properties.hibernate.default_schema=banner
```

### 3. Verificación de Entidades

**IMPORTANTE**: Verifica que tus entidades NO tengan `schema` hardcodeado a menos que sea necesario.

#### ✅ Correcto (deja que use el default_schema):
```java
@Entity
@Table(name = "professor")
public class Professor {
    // ...
}
```

#### ⚠️ Solo si necesitas especificar un schema diferente:
```java
@Entity
@Table(name = "profesor", schema = "banner")  // Solo si esta entidad debe leer de banner
public class BannerProfessor {
    // ...
}
```

### 4. Consultas Nativas con Schemas

Si tienes consultas SQL nativas en tu código, debes especificar el schema:

#### ❌ Antes:
```java
@Query(value = "SELECT * FROM professor WHERE id = ?1", nativeQuery = true)
Professor findProfessorById(String id);
```

#### ✅ Ahora:
```java
@Query(value = "SELECT * FROM sigma.professor WHERE id = ?1", nativeQuery = true)
Professor findProfessorById(String id);
```

O si lees de banner:
```java
@Query(value = "SELECT * FROM banner.professor WHERE id = ?1", nativeQuery = true)
BannerProfessor findBannerProfessorById(String id);
```

### 5. Verificar Repositorios

Busca en tu código repositorios que tengan `@Query` con SQL nativo y asegúrate de agregar el schema:

```bash
# Buscar consultas nativas en tu proyecto
grep -r "@Query.*nativeQuery.*true" PDG-SIGMA-BACKEND-main/src
```

### 6. WebClient / RestTemplate a API Banner

Si tu backend de Sigma se comunica con el API Banner mediante HTTP, no necesitas cambios. El API Banner se encargará de conectarse a su schema correspondiente.

## Checklist de Verificación

- [ ] `application-cloud.properties` configurado con `default_schema=sigma`
- [ ] API Banner (si existe) configurado con `default_schema=banner`
- [ ] Entidades revisadas (sin schema hardcodeado innecesario)
- [ ] Consultas nativas actualizadas con schemas
- [ ] Repositorios con `@Query` nativo verificados
- [ ] Pruebas de conexión exitosas

## Solución de Problemas

### Error: "relation does not exist"
Significa que Hibernate busca la tabla en el schema incorrecto.

**Solución**: Verifica el `default_schema` en tu configuración o agrega explícitamente el schema en la entidad:
```java
@Table(name = "nombre_tabla", schema = "sigma")
```

### Error: "schema banner does not exist"
Significa que no ejecutaste los scripts SQL para crear los schemas.

**Solución**: Ejecuta en orden:
1. `1_create_schemas.sql`
2. `2_datos_banner.sql`
3. `3_datos_sigma.sql`

### Error de autenticación
Verifica que las credenciales de Neon estén correctas en `application-cloud.properties`.

## Notas Adicionales

- Neon te permite tener **múltiples schemas** en la misma base de datos
- Es más eficiente que tener múltiples bases de datos
- Los 3 miembros del equipo se conectan a la misma instancia de Neon
- Solo necesitas compartir las credenciales UNA VEZ

