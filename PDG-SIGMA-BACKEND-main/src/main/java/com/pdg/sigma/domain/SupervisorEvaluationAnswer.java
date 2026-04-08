package com.pdg.sigma.domain;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@Entity
@Table(name = "supervisor_evaluation_answer", uniqueConstraints = {
        @UniqueConstraint(name = "uk_supervisor_eval_answer", columnNames = {"evaluation_id", "question_id"})
})
public class SupervisorEvaluationAnswer implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "evaluation_id", nullable = false)
    private SupervisorEvaluation evaluation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id", nullable = false)
    private ProfessorSurveyQuestion question;

    @Column(name = "display_order", nullable = false)
    private int displayOrder;

    @Column(name = "score", nullable = false)
    private int score;
}
