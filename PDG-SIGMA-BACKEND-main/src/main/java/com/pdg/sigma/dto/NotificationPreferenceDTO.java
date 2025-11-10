package com.pdg.sigma.dto;

import com.pdg.sigma.notification.NotificationPreference;

public class NotificationPreferenceDTO {
    private String professorId;
    private boolean enableProgressUpdate;
    private boolean enableCompleted;
    private boolean enableOverdue;
    private boolean enableSound;

    public NotificationPreferenceDTO() {}

    public NotificationPreferenceDTO(NotificationPreference pref) {
        this.professorId = pref.getProfessorId();
        this.enableProgressUpdate = pref.isEnableProgressUpdate();
        this.enableCompleted = pref.isEnableCompleted();
        this.enableOverdue = pref.isEnableOverdue();
        this.enableSound = pref.isEnableSound();
    }

    public NotificationPreference toEntity() {
        NotificationPreference p = new NotificationPreference(this.professorId);
        p.setEnableProgressUpdate(this.enableProgressUpdate);
        p.setEnableCompleted(this.enableCompleted);
        p.setEnableOverdue(this.enableOverdue);
        p.setEnableSound(this.enableSound);
        return p;
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
