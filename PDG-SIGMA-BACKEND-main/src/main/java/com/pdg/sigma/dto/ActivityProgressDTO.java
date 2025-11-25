package com.pdg.sigma.dto;

import java.util.Date;

import com.pdg.sigma.domain.ActivityProgress;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class ActivityProgressDTO {

    private Long id;
    private Integer progressPercentage;
    private String progressComment;
    private String evidenceName;
    private String evidenceUrl;
    private Date createdAt;
    private String createdBy;
    private String createdByRole;
    private String createdByName;

    public ActivityProgressDTO(ActivityProgress progress) {
        this.id = progress.getId();
        this.progressPercentage = progress.getProgressPercentage();
        this.progressComment = progress.getProgressComment();
        this.evidenceName = progress.getEvidenceName();
        this.createdAt = progress.getCreatedAt();
        this.createdBy = progress.getCreatedBy();
        this.createdByRole = progress.getCreatedByRole();
        this.createdByName = progress.getCreatedByName();
    }
}
