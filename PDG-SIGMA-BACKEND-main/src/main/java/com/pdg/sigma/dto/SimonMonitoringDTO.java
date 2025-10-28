package com.pdg.sigma.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SimonMonitoringDTO {

    // 1. Estudiante de PRE/POS
    private String estudianteTipo; // "PRE" o "POS"

    // 2. CENCO
    private String cenco; // Ej: "CA021202"

    // 3. No.Monitoria
    private String numeroMonitoria; // Puede ser vacío

    // 4. NOMBRE
    private String nombre;

    // 5. APELLIDO
    private String apellido;

    // 6. CÉDULA
    private String cedula;

    // 7. CÓDIGO DE ESTUDIANTE
    private String codigoEstudiante;

    // 8. EMAIL
    private String email;

    // 9. NÚMERO DE CELULAR DAVIPLATA
    private String celular;

    // 10. Código curso
    private String codigoCurso; // Ej: "TIC - 09704"

    // 11. CURSO O PROYECTO
    private String nombreCurso;

    // 12. DESCRIPCIÓN MONITORÍA
    private String descripcionMonitoria; // Ej: "NRC-10740"

    // 13. FECHA INICIO
    private String fechaInicio; // Formato: "dd/MM/yyyy"

    // 14. FECHA FIN
    private String fechaFin; // Formato: "dd/MM/yyyy"

    // 15. TOTAL HORAS
    private Integer totalHoras;

    // 16. Total Semanas
    private Integer totalSemanas;

    // 17. Profesor que solicita la monitoria
    private String profesorSolicita;
}

