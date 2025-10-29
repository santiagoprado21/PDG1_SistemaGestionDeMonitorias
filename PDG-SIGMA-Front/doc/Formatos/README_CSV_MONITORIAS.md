# Formato CSV para carga de monitorías (con presupuesto)

Este es el formato oficial aceptado por el sistema para la carga masiva de monitorías desde CSV/XLSX.

## Cabeceras obligatorias (en este orden)

1. FACULTAD
2. PROGRAMA
3. CURSO
4. FECHA INICIO (dd-mm-aaaa)
5. FECHA FINALIZACION (dd-mm-aaaa)
6. PERIODO (aaaa-1 | aaaa-2)
7. PROMEDIO ACUMULADO
8. PROMEDIO MATERIA
9. HORAS ESTIMADAS
10. VALOR HORA

> Nota: Las cabeceras deben ser exactamente como arriba (se ignoran mayúsculas/minúsculas), sin espacios extra al final. No deje campos obligatorios vacíos.

## Ejemplo (CSV)

```
FACULTAD,PROGRAMA,CURSO,FECHA INICIO,FECHA FINALIZACION,PERIODO,PROMEDIO ACUMULADO,PROMEDIO MATERIA,HORAS ESTIMADAS,VALOR HORA
Facultad de Ingeniería,Ingeniería de Sistemas,Programación I,28-10-2025,30-11-2025,2025-2,4.5,4.6,80,15000
```

## Reglas de validación clave

- Fechas en CSV: `dd-mm-aaaa` (ej. `28-10-2025`).
- Semestre: `aaaa-1` o `aaaa-2`, y debe corresponder al semestre de la fecha de inicio.
- Valores numéricos: usar punto decimal (ej. `15000.5`), no coma.
- El sistema calcula y muestra automáticamente:
  - Horas cargadas (HORAS ESTIMADAS)
  - Valor por hora (VALOR HORA)
  - Costo total = horas × valor hora
- Si no hay presupuesto configurado para ese programa/semestre, la carga y edición no se bloquean. Al configurar presupuesto, se empezará a validar que no se excedan las horas disponibles.

## Dónde cargar

En el frontend: vista "Crear/Cargar monitorías" → botón "Cargar datos".

Tras una carga exitosa:
- Se refresca la tabla y verás columnas de Horas, Valor hora y Costo.
- Cada fila permite editar presupuesto manualmente con el botón "Editar presupuesto".

## Archivos de ejemplo

En esta carpeta encontrarás archivos como:
- `monitorias_barberi_ingenieria.csv`
- `monitorias_ciencias_humanas.csv`
- `monitorias_ciencias_salud.csv`
- `monitorias_educacion.csv`
- `monitorias_innovacion.csv`
- `monitorias_negocios_economia.csv`

Todos ya incluyen las columnas `HORAS ESTIMADAS` y `VALOR HORA` con datos de muestra.
