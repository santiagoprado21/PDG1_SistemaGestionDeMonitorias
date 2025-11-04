# HU-010: Scripts de Migración

## Descripción

Scripts SQL para implementar el nuevo flujo de HU-010 que separa el proceso de postulación del de monitoría.

## Nuevo Flujo

```
1. PROFESOR → Crea convocatoria (MonitoringRequest)
                ↓
2. ESTUDIANTES → Se postulan (MonitorApplication)
                ↓
3. PROFESOR → Selecciona 1 monitor
                ↓
4. SISTEMA → Crea Monitoring con monitor asignado
                ↓
5. JEFE DEPTO → Aprueba/Rechaza paquete completo
```

## Archivos

### `6_hu010_create_new_tables.sql`
**Script principal de migración**

Crea:
- Tabla `monitoring_request` (convocatorias)
- Tabla `monitor_application` (postulaciones)
- Nuevas columnas en `monitoring` para integración

**Ejecutar:**
```bash
psql -h ep-wispy-firefly-aeueocfk-pooler.c-2.us-east-2.aws.neon.tech \
     -U neondb_owner \
     -d neondb \
     -f 6_hu010_create_new_tables.sql
```

O desde SQL:
```sql
\i 6_hu010_create_new_tables.sql
```

### `6_hu010_rollback.sql`
**Script de rollback (usar con precaución)**

Elimina todas las tablas y columnas creadas por la migración.

**Solo usar si necesitas revertir los cambios:**
```sql
\i 6_hu010_rollback.sql
```

### `7_hu010_test_data.sql`
**Datos de prueba**

Inserta datos de ejemplo para testear el nuevo flujo.

⚠️ **IMPORTANTE:** Antes de ejecutar:
1. Abre el archivo
2. Reemplaza los IDs de ejemplo con IDs reales de tu base de datos:
   - `professor_id`
   - `course_id`
   - `school_id`
   - `program_id`
   - `monitor_id` (códigos de estudiantes)

**Ejecutar:**
```sql
\i 7_hu010_test_data.sql
```

## Orden de Ejecución

1. **Migración inicial:**
   ```sql
   \i 6_hu010_create_new_tables.sql
   ```

2. **Datos de prueba (opcional):**
   ```sql
   \i 7_hu010_test_data.sql
   ```

## Estructura de Tablas

### monitoring_request
Convocatorias creadas por profesores.

| Campo | Tipo | Descripción |
|-------|------|-------------|
| id | BIGSERIAL | ID autoincremental |
| professor_id | VARCHAR(20) | Profesor que crea la convocatoria |
| course_id | BIGINT | Curso para la monitoría |
| requested_hours | INTEGER | Horas solicitadas |
| justification | TEXT | Justificación (HU-010) |
| status | VARCHAR(30) | Estado de la convocatoria |
| created_at | TIMESTAMP | Fecha de creación |

**Estados:**
- `CONVOCATORIA_ABIERTA` → Recién creada, esperando postulantes
- `MONITOR_SELECCIONADO` → Profesor eligió un estudiante
- `PENDIENTE_APROBACION` → Enviada al jefe con monitor
- `APROBADA` → Jefe aprobó
- `RECHAZADA` → Jefe rechazó
- `CANCELADA` → Profesor canceló

### monitor_application
Postulaciones de estudiantes.

| Campo | Tipo | Descripción |
|-------|------|-------------|
| id | BIGSERIAL | ID autoincremental |
| monitoring_request_id | BIGINT | Convocatoria a la que se postula |
| monitor_id | VARCHAR(20) | Estudiante que se postula |
| status | VARCHAR(20) | Estado de la postulación |
| motivation_letter | TEXT | Carta de motivación |
| application_date | TIMESTAMP | Fecha de postulación |

**Estados:**
- `POSTULADO` → Se postuló
- `SELECCIONADO` → Profesor lo eligió
- `NO_SELECCIONADO` → No fue elegido

### monitoring (modificaciones)
Nuevas columnas agregadas:

| Campo | Tipo | Descripción |
|-------|------|-------------|
| monitoring_request_id | BIGINT | Convocatoria que originó esta monitoría |
| assigned_monitor_id | VARCHAR(20) | Monitor asignado desde creación |
| approval_status | VARCHAR(30) | Estado de aprobación |
| justification | TEXT | Justificación (de la convocatoria) |
| approved_by | VARCHAR(20) | Jefe que aprobó/rechazó |
| approval_comment | TEXT | Comentario de aprobación |
| approval_date | TIMESTAMP | Fecha de aprobación |

## Consultas Útiles

### Ver convocatorias abiertas
```sql
SELECT 
    mr.id,
    c.name as curso,
    p.name as profesor,
    mr.requested_hours,
    mr.semester,
    COUNT(ma.id) as num_postulantes
FROM monitoring_request mr
LEFT JOIN course c ON c.id = mr.course_id
LEFT JOIN professor p ON p.id = mr.professor_id
LEFT JOIN monitor_application ma ON ma.monitoring_request_id = mr.id
WHERE mr.status = 'CONVOCATORIA_ABIERTA'
GROUP BY mr.id, c.name, p.name, mr.requested_hours, mr.semester;
```

### Ver postulantes de una convocatoria
```sql
SELECT 
    ma.id,
    m.name || ' ' || m.last_name as estudiante,
    m.code,
    ma.status,
    LEFT(ma.motivation_letter, 100) as motivacion
FROM monitor_application ma
JOIN monitor m ON m.code = ma.monitor_id
WHERE ma.monitoring_request_id = 1  -- ID de la convocatoria
ORDER BY ma.application_date;
```

### Ver monitorías pendientes de aprobación
```sql
SELECT 
    m.id,
    c.name as curso,
    p.name as profesor,
    mon.name || ' ' || mon.last_name as monitor_asignado,
    m.approval_status,
    LEFT(m.justification, 100) as justificacion
FROM monitoring m
JOIN course c ON c.id = m.course_id
JOIN professor p ON p.id = m.professor_id
LEFT JOIN monitor mon ON mon.code = m.assigned_monitor_id
WHERE m.approval_status = 'PENDIENTE_APROBACION';
```

## Verificación Post-Migración

Después de ejecutar el script de migración, verifica:

1. **Tablas creadas:**
   ```sql
   SELECT table_name 
   FROM information_schema.tables 
   WHERE table_schema = 'sigma' 
     AND table_name IN ('monitoring_request', 'monitor_application');
   ```

2. **Columnas agregadas a monitoring:**
   ```sql
   SELECT column_name, data_type 
   FROM information_schema.columns 
   WHERE table_schema = 'sigma' 
     AND table_name = 'monitoring'
     AND column_name IN ('monitoring_request_id', 'assigned_monitor_id', 'approval_status');
   ```

3. **Índices creados:**
   ```sql
   SELECT indexname 
   FROM pg_indexes 
   WHERE schemaname = 'sigma' 
     AND tablename IN ('monitoring_request', 'monitor_application');
   ```

## Integración con Backend

El backend (Spring Boot) ya está configurado para:
- Detectar automáticamente las nuevas tablas
- Crear las entidades JPA correspondientes
- Usar los servicios implementados:
  - `MonitoringRequestService`
  - `MonitorApplicationService`
  - `MonitoringService` (extendido)

**Endpoints REST disponibles:**
- `POST /monitoring-request/create` - Crear convocatoria
- `GET /monitoring-request/open` - Listar abiertas
- `POST /monitor-application/apply` - Postularse
- `POST /monitor-application/select` - Seleccionar monitor
- `POST /monitoring/approve/{id}` - Aprobar monitoría
- `POST /monitoring/reject/{id}` - Rechazar monitoría

Ver `MonitoringRequestController` y `MonitorApplicationController` para la lista completa.

## Compatibilidad con Flujo Antiguo

Las monitorías existentes (creadas antes de HU-010) **NO se verán afectadas**:
- `monitoring_request_id` será `NULL`
- `approval_status` será `NULL`
- Funcionarán con el flujo anterior

Solo las monitorías creadas a través del nuevo flujo tendrán estos campos poblados.

## Soporte

Si encuentras problemas con la migración:
1. Revisa los logs del script SQL
2. Verifica que todos los IDs de ejemplo fueron reemplazados
3. Ejecuta las consultas de verificación
4. Si es necesario, usa el script de rollback y vuelve a intentar

## Changelog

- **v1.0** (2025-11-04): Migración inicial para HU-010
  - Creación de tablas `monitoring_request` y `monitor_application`
  - Extensión de tabla `monitoring`
  - Scripts de rollback y datos de prueba

