package com.pdg.sigma.notification;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "notification_preferences")
public class NotificationPreference {

    @Id
    @Column(name = "professor_id", length = 64)
    private String professorId;

    @Column(name = "enable_progress_update")
    private boolean enableProgressUpdate = true;

    @Column(name = "enable_completed")
    private boolean enableCompleted = true;

    @Column(name = "enable_overdue")
    private boolean enableOverdue = true;

    @Column(name = "enable_sound")
    private boolean enableSound = true;

    public NotificationPreference() {}

    public NotificationPreference(String professorId) {
        this.professorId = professorId;
    }

    public String getProfessorId() { return professorId; }
    public void setProfessorId(String professorId) { this.professorId = professorId; }
    public boolean isEnableProgressUpdate() { return enableProgressUpdate; }
    public void setEnableProgressUpdate(boolean enableProgressUpdate) { this.enableProgressUpdate = enableProgressUpdate; }
    public boolean isEnableCompleted() { return enableCompleted; }
    public void setEnableCompleted(boolean enableCompleted) { this.enableCompleted = enableCompleted; }
    public boolean isEnableOverdue() { return enableOverdue; }
    public void setEnableOverdue(boolean enableOverdue) { this.enableOverdue = enableOverdue; }
    public boolean isEnableSound() { return enableSound; }
    public void setEnableSound(boolean enableSound) { this.enableSound = enableSound; }
}
