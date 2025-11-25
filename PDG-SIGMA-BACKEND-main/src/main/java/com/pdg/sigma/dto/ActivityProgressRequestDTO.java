package com.pdg.sigma.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ActivityProgressRequestDTO {

    @NotNull
    @Min(0)
    @Max(100)
    private Integer progressPercentage;

    private String progressComment;

    @NotNull
    private String userId;

    @NotNull
    private String userRole;
}
