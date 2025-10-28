package com.pdg.sigma.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "simon_file_generation")
public class SimonFileGeneration {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "generated_at", nullable = false)
    private LocalDateTime generatedAt;

    @Column(name = "generated_by", nullable = false, length = 255)
    private String generatedBy;

    @Column(name = "total_monitorings", nullable = false)
    private Integer totalMonitorings;

    @Column(name = "file_name", nullable = false, length = 500)
    private String fileName;

    @Column(name = "semester", length = 10)
    private String semester;

    public SimonFileGeneration(LocalDateTime generatedAt, String generatedBy, Integer totalMonitorings, String fileName, String semester) {
        this.generatedAt = generatedAt;
        this.generatedBy = generatedBy;
        this.totalMonitorings = totalMonitorings;
        this.fileName = fileName;
        this.semester = semester;
    }
}

