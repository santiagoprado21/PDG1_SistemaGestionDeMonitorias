package com.pdg.sigma.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SimonMonitoringRowDTO {
    private String nombre;            // Nombres del estudiante
    private String apellido;          // Apellidos del estudiante
    private String codigoEstudiante;  // Código del estudiante
    private String email;             // Email del estudiante
    private String nombreCurso;       // Nombre del curso
    private String profesorSolicita;  // Profesor solicitante
    private String fechaInicio;       // dd/MM/yyyy
    private String fechaFin;          // dd/MM/yyyy
    private Integer totalHoras;       // Horas totales (estimadas o sumatoria asistencia)
    private String semestre;          // Semestre académico

    // Campos adicionales para completar 17 columnas de SIMON (dejar vacíos si no se usan)
    private String facultad;          // Facultad
    private String programa;          // Programa
    private String codigoCurso;       // Código del curso (si aplica)
    private String idProfesor;        // Identificador profesor
    private String codigoMonitor;     // Documento monitor (si difiere del código estudiante)
    private String semanas;           // Cantidad de semanas (si aplica)
    private String observacion;       // Observaciones
}
