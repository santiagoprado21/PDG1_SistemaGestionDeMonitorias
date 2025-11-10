package com.pdg.sigma.service;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.pdg.sigma.dto.NotificationPreferenceDTO;
import com.pdg.sigma.notification.NotificationPreference;
import com.pdg.sigma.notification.NotificationType;
import com.pdg.sigma.repository.NotificationPreferenceRepository;

@Service
public class NotificationPreferenceServiceImpl implements NotificationPreferenceService {

    @Autowired
    private NotificationPreferenceRepository repository;

    @Override
    public NotificationPreferenceDTO getPreferences(String professorId) {
        Optional<NotificationPreference> pref = repository.findByProfessorId(professorId);
        return new NotificationPreferenceDTO(pref.orElseGet(() -> new NotificationPreference(professorId)));
    }

    @Override
    public NotificationPreferenceDTO updatePreferences(NotificationPreferenceDTO dto) {
        NotificationPreference entity = dto.toEntity();
        repository.save(entity);
        return new NotificationPreferenceDTO(entity);
    }

    public boolean isTypeEnabled(String professorId, NotificationType type) {
        NotificationPreference pref = repository.findByProfessorId(professorId)
                .orElseGet(() -> new NotificationPreference(professorId));
        return switch (type) {
            case PROGRESS_UPDATE -> pref.isEnableProgressUpdate();
            case COMPLETED -> pref.isEnableCompleted();
            case OVERDUE -> pref.isEnableOverdue();
            default -> true;
        };
    }
}
