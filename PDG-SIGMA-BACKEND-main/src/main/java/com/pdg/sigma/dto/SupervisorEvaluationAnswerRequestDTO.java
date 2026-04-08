package com.pdg.sigma.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class SupervisorEvaluationAnswerRequestDTO {
    private Long questionId;
    private Integer score;
}
