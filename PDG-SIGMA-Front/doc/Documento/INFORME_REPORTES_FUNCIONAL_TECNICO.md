# Informe funcional y tecnico - Modulo de Reportes

## 1. Objetivo
Describir el funcionamiento funcional y tecnico de cada reporte visible en la vista `Reportes` de la aplicacion SIGMA.

## 2. Alcance
Componente principal documentado:
- `PDG-SIGMA-Front/src/Reports.js`

## 3. Filtros globales
Filtros disponibles en la cabecera de la vista:
- `Semestre*`
- `Programa*`
- `Curso*`
- `Profesor` (cuando el rol no es `professor`)

Los filtros afectan de forma transversal las visualizaciones, tablas y archivos exportados.

## 4. Reportes y tablas

### 4.1 Rendimiento de monitores
**Proposito funcional**
- Mostrar el estado de actividades por monitor.
- Estados mostrados: `Completado`, `Pendiente`, `Tarde`.

**Visualizacion**
- Grafico de barras apiladas por monitor.
- Tabla de monitores con numeracion.
- Selector de orden `A-Z` y `Z-A`.
- Paginacion para grandes volumenes.

**Interpretacion**
- Mayor altura total de barra = mayor numero total de actividades.
- Segmentos por color = distribucion de estados por monitor.

**Exportacion**
- `Rendimiento_Monitores.csv`
- `Rendimiento_Monitores.pdf`

---

### 4.2 Comparativo por semestre
**Proposito funcional**
- Comparar desempeno entre dos semestres consecutivos disponibles.

**Visualizacion**
- Grafico de barras comparativas por indicador.
- Indicadores: `Completadas`, `Pendientes`, `Tardias`.

**Logica tecnica**
- Se calcula sobre el dataset base de monitores filtrado.
- Requiere al menos dos semestres con datos para generar comparacion.

**Interpretacion**
- Permite identificar mejoras o retrocesos por estado de actividad entre periodos.

**Exportacion**
- `Comparativo_Semestres.csv`
- `Comparativo_Semestres.pdf`

---

### 4.3 Resumen de tareas de monitores
**Proposito funcional**
- Mostrar porcentajes globales de avance de monitores.

**Visualizacion**
- Tarjetas de porcentaje:
- `Completadas`
- `Completadas tardias`
- `Pendientes`

**Logica tecnica**
- Se agregan conteos por estado en el conjunto filtrado.
- Se calcula porcentaje sobre el total de actividades.

**Interpretacion**
- Ayuda a medir la salud operativa global del trabajo de monitoria.

**Exportacion**
- `Resumen_Tareas_Monitor.csv`
- `Resumen_Tareas_Monitor.pdf`

---

### 4.4 Uso de categorias por curso
**Proposito funcional**
- Mostrar distribucion de categorias de actividades para el curso filtrado.

**Visualizacion**
- Grafico de pastel (top 5 categorias por cantidad).

**Interpretacion**
- Permite ver en que categorias se concentra la actividad academica.

**Exportacion**
- `Categorias_Por_Curso.csv`
- `Categorias_Por_Curso.pdf`

---

### 4.5 Rendimiento de profesores
**Proposito funcional**
- Visualizar distribucion de estados de actividades por profesor y curso.

**Visualizacion**
- Grafico de barras apiladas por profesor.
- Etiquetas normalizadas para lectura consistente.


**Interpretacion**
- Facilita comparacion de carga/respuesta de actividades entre profesores.

**Exportacion**
- `Rendimiento_Profesores.csv`
- `Rendimiento_Profesores.pdf`

---

### 4.6 Resumen de tareas de profesores
**Proposito funcional**
- Consolidar porcentajes globales de estado de tareas de profesores.

**Visualizacion**
- Tarjetas de porcentaje:
- `Completadas`
- `Completadas tardias`
- `Pendientes`

**Interpretacion**
- Proporciona una lectura ejecutiva del avance docente en actividades.

**Exportacion**
- `Resumen_Tareas_Profesor.csv`
- `Resumen_Tareas_Profesor.pdf`

## 5. Ayudas contextuales en la vista
Cada bloque de reporte incluye icono de ayuda con tooltip contextual.

Contenido de ayuda por bloque:
- descripcion corta del objetivo del reporte,
- guia de lectura del grafico o tabla,
- recomendaciones de uso de filtros.

## 6. Flujo general del modulo
1. Se leen `userId` y `role` desde sesion.
2. Se consulta dataset principal de reportes.
3. Se derivan listas y agregaciones por filtro.
4. Se renderizan graficos, tablas y tarjetas.
5. Se habilita exportacion de cada bloque con filtros aplicados.

## 7. Criterios funcionales de validacion
1. Los filtros cambian coherentemente todos los reportes visibles.
2. Las tablas y graficos muestran datos consistentes entre si.
3. El orden de monitores responde al selector A-Z/Z-A.
4. Las exportaciones reflejan el mismo estado filtrado de pantalla.
5. Las ayudas contextuales describen correctamente cada reporte.
