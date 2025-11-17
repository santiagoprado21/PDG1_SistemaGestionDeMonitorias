package com.pdg.sigma.service;

import com.pdg.sigma.dto.NotificationPreferenceDTO;

public interface NotificationPreferenceService {
    NotificationPreferenceDTO getPreferences(String professorId);
    NotificationPreferenceDTO updatePreferences(NotificationPreferenceDTO dto);
}
