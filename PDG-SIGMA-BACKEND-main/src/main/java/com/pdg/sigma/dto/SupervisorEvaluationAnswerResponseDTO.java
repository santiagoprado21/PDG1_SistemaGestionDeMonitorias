package com.pdg.sigma.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class SupervisorEvaluationAnswerResponseDTO {
    private Long questionId;
    private String questionKey;
    private String statement;
    private String category;
    private Integer displayOrder;
    private Integer score;
}
