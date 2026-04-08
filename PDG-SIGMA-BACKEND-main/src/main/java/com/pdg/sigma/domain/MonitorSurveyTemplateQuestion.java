package com.pdg.sigma.domain;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@Entity
@Table(name = "monitor_survey_template_question", uniqueConstraints = {
        @UniqueConstraint(name = "uk_template_question", columnNames = {"template_id", "question_id"})
})
public class MonitorSurveyTemplateQuestion implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "template_id", nullable = false)
    private MonitorSurveyTemplate template;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id", nullable = false)
    private MonitorSurveyQuestion question;

    @Column(name = "display_order", nullable = false)
    private int displayOrder;
}
