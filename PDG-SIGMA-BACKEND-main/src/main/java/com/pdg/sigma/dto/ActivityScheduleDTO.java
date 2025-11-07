package com.pdg.sigma.dto;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.Date;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO extendido para Activity con campos de horarios (HU-011)
 * Incluye todos los campos de ActivityRequestDTO + los nuevos campos
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ActivityScheduleDTO {

    private Integer id;

    private Date creation;

    @NotNull
    @Size(max = 100)
    private String name;

    @NotNull
    private Date finish;

    @NotNull
    private String roleCreator;

    @NotNull
    private String roleResponsable;

    private String category;

    @NotNull
    @Size(max = 255)
    private String description;

    @NotNull
    private Integer monitoringId;

    private String professorId;
    private String monitorId;

    @NotNull
    private String state;

    private String semester;

    private Date delivey;

    // ============================================================================
    // Campos HU-011: Horarios, duración y rúbricas
    // ============================================================================

    private LocalTime startTime;
    private LocalTime endTime;
    private BigDecimal durationHours;
    private String recurrence; // 'NONE', 'DAILY', 'WEEKLY'
    private String priority; // 'ALTA', 'MEDIA', 'BAJA'
    private Long rubricId;

    // Información adicional de la rúbrica (solo para lectura)
    private String rubricName;
    private Integer rubricTotalPoints;
}

