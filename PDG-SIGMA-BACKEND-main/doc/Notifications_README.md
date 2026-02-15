# HU-014: Notificaciones de progreso de monitores

Este módulo agrega notificaciones para profesores cuando:
- Monitores actualizan actividades (progreso)
- Actividades se marcan como completadas
- Actividades quedan atrasadas (job diario configurable)

Se añadieron preferencias por profesor y stream SSE opcional.

## Endpoints
Base path: `/notifications`

| Método | Path | Descripción |
|--------|------|-------------|
| GET | `/unread/{professorId}` | Lista de no leídas |
| GET | `/count/{professorId}` | Conteo de no leídas |
| PUT | `/{id}/read` | Marcar 1 como leída |
| PUT | `/read-all/{professorId}` | Marcar todas como leídas |
| GET | `/prefs/{professorId}` | Obtener preferencias |
| PUT | `/prefs/{professorId}` | Actualizar preferencias |
| GET | `/stream/{professorId}` | SSE tiempo real (si habilitado) |

Requiere autenticación (JWT) como el resto del backend.

## Configuración
En `application.properties`:
```
sigma.notifications.enabled=true
sigma.notifications.overdue-enabled=true
sigma.notifications.overdue-cron=0 0 8 * * *
sigma.notifications.realtime-enabled=true
```

## Esquema de base de datos
Si no se usa auto-DDL de Hibernate, cree las tablas:
```sql
CREATE TABLE IF NOT EXISTS notifications (
  id BIGSERIAL PRIMARY KEY,
  professor_id varchar(255) NOT NULL,
  type varchar(30) NOT NULL,
  message text NOT NULL,
  activity_id bigint,
  created_at timestamp NOT NULL,
  read_flag boolean NOT NULL DEFAULT FALSE
);
CREATE INDEX IF NOT EXISTS idx_notifications_professor_read ON notifications(professor_id, read_flag);

CREATE TABLE IF NOT EXISTS notification_preferences (
  professor_id varchar(255) PRIMARY KEY,
  enable_progress_update boolean NOT NULL DEFAULT TRUE,
  enable_completed boolean NOT NULL DEFAULT TRUE,
  enable_overdue boolean NOT NULL DEFAULT TRUE,
  enable_sound boolean NOT NULL DEFAULT TRUE
);
```

## SSE
`/notifications/stream/{professorId}` emite eventos con payload `NotificationDTO`. Usa `Sinks.Many` en memoria (no apto para clustering). Considere WebSocket/Redis para producción.

## Frontend
`NotificationIcon.js` consume las APIs y aplica preferencias locales (sincronizables con backend). Incluye beep opcional y deep link a actividades.
