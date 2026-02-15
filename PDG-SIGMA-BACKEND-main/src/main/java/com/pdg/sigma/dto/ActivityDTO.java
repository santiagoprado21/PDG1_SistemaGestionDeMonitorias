package com.pdg.sigma.dto;

import java.util.Date;

import com.pdg.sigma.domain.Activity;
import com.pdg.sigma.domain.Monitor;
import com.pdg.sigma.domain.Monitoring;
import com.pdg.sigma.domain.Professor;
import com.pdg.sigma.domain.StateActivity;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Data
@NoArgsConstructor
@Setter
public class ActivityDTO {
    private Integer id;
    private String name;
    private Date creation;
    private Date finish;
    private String roleCreator;
    private String roleResponsable;
    private String category;
    private String description;
    private Monitoring monitoring;
    private Professor professor;
    private Monitor monitor;
    private StateActivity state;
    private String type;
    private String responsableName;
    private String creatorName;
    private String course;
    private String userId;
    private Date edited;
    private Date delivey;
    private String semester;

    private Integer progressPercentage;
    private String progressComment;
    private Date progressUpdatedAt;
    private String progressUpdatedBy;
    private String progressUpdatedByRole;
    private String progressUpdatedByName;
    private String progressEvidencePath;
    private String progressEvidenceName;
    
    // HU-017: Información de rúbrica
    private Long rubricId;
    private String rubricName;
    private Integer rubricTotalPoints;

    public ActivityDTO(String userId){
        this.userId = userId;
    }
    public ActivityDTO(Activity activity){
        this.id= activity.getId();
        this.name= activity.getName();
        this.creation = activity.getCreation();
        this.finish = activity.getFinish();
        this.roleCreator = activity.getRoleCreator();
        this.roleResponsable = activity.getRoleResponsable();
        this.category = activity.getCategory();
        this.description = activity.getDescription();
        this.monitoring = activity.getMonitoring();
        this.professor = activity.getProfessor();
        this.monitor = activity.getMonitor();
        this.state = activity.getState();
        this.edited = activity.getEdited();
        this.delivey = activity.getDelivey();
        this.semester = activity.getSemester();
        this.progressPercentage = activity.getProgressPercentage();
        this.progressComment = activity.getProgressComment();
        this.progressUpdatedAt = activity.getProgressUpdatedAt();
        this.progressUpdatedBy = activity.getProgressUpdatedBy();
        this.progressUpdatedByRole = activity.getProgressUpdatedByRole();
        this.progressUpdatedByName = activity.getProgressUpdatedByName();
        this.progressEvidencePath = activity.getProgressEvidencePath();
        this.progressEvidenceName = activity.getProgressEvidenceName();
        
        // HU-017: Incluir información de rúbrica
        if (activity.getRubric() != null) {
            this.rubricId = activity.getRubric().getId();
            this.rubricName = activity.getRubric().getName();
            this.rubricTotalPoints = activity.getRubric().getTotalPoints();
        }
    }
    public ActivityDTO(String name, Date creation, Date finish, String category, String description, String course, String creatorName, String responsableName, String state, String type){

    }
}