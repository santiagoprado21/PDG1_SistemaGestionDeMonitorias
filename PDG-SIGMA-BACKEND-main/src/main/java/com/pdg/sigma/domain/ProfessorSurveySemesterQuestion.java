package com.pdg.sigma.domain;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@Entity
@Table(name = "professor_survey_semester_question", uniqueConstraints = {
    @UniqueConstraint(name = "uk_professor_semester_question", columnNames = {"semester_config_id", "question_id"})
})
public class ProfessorSurveySemesterQuestion implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "semester_config_id", nullable = false)
    private ProfessorSurveySemesterConfig semesterConfig;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id", nullable = false)
    private ProfessorSurveyQuestion question;

    @Column(name = "display_order", nullable = false)
    private int displayOrder;

    @Column(name = "active", nullable = false)
    private boolean active = true;
}

