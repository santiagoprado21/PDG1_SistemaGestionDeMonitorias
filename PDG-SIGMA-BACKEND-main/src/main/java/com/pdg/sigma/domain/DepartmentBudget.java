package com.pdg.sigma.domain;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Entity
@Table(name = "department_budget")
@NoArgsConstructor
public class DepartmentBudget {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "program_id", nullable = false)
    private Program program;

    @Column(name = "semester", nullable = false, length = 16)
    private String semester; // e.g., 2025-2

    @Column(name = "total_hours", nullable = false)
    private Integer totalHours; // Presupuesto disponible en horas para el semestre

    public DepartmentBudget(Program program, String semester, Integer totalHours) {
        this.program = program;
        this.semester = semester;
        this.totalHours = totalHours;
    }
}
