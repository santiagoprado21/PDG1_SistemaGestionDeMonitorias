package com.pdg.sigma.service;

import java.io.IOException;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.pdg.sigma.domain.Activity;
import com.pdg.sigma.domain.ActivityProgress;
import com.pdg.sigma.domain.Monitor;
import com.pdg.sigma.domain.Professor;
import com.pdg.sigma.domain.StateActivity;
import com.pdg.sigma.dto.ActivityProgressRequestDTO;
import com.pdg.sigma.repository.ActivityProgressRepository;
import com.pdg.sigma.repository.ActivityRepository;
import com.pdg.sigma.repository.MonitorRepository;
import com.pdg.sigma.repository.ProfessorRepository;
import com.pdg.sigma.repository.ProspectRepository;

@Service
public class ActivityProgressServiceImpl implements ActivityProgressService {

    @Autowired
    private ActivityRepository activityRepository;

    @Autowired
    private ActivityProgressRepository progressRepository;

    @Autowired
    private MonitorRepository monitorRepository;

    @Autowired
    private ProfessorRepository professorRepository;

    @Autowired
    private ProspectRepository prospectRepository;

    @Autowired
    private ActivityEvidenceStorageService evidenceStorageService;

    @Autowired
    private NotificationService notificationService;

    @Override
    @Transactional
    public ActivityProgress registerProgress(Integer activityId, ActivityProgressRequestDTO payload, MultipartFile evidence) throws Exception {
        Activity activity = activityRepository.findById(activityId)
                .orElseThrow(() -> new Exception("No se encontró la actividad"));

        validatePermission(activity, payload.getUserId(), payload.getUserRole());

        ActivityProgress progress = new ActivityProgress();
        progress.setActivity(activity);
        progress.setProgressPercentage(payload.getProgressPercentage());
        progress.setProgressComment(payload.getProgressComment());
        progress.setCreatedBy(payload.getUserId());
        progress.setCreatedByRole(payload.getUserRole());
        progress.setCreatedByName(resolveDisplayName(payload.getUserId(), payload.getUserRole()));

        if (evidence != null && !evidence.isEmpty()) {
            try {
                ActivityEvidenceStorageService.StoredEvidence stored = evidenceStorageService.store(activityId, evidence);
                if (stored != null) {
                    progress.setEvidencePath(stored.relativePath());
                    progress.setEvidenceName(stored.originalFilename());
                }
            } catch (IOException ioe) {
                throw new Exception("No fue posible almacenar la evidencia: " + ioe.getMessage(), ioe);
            }
        }

        ActivityProgress savedProgress = progressRepository.save(progress);

        updateActivitySummary(activity, savedProgress);

        if (savedProgress.getProgressPercentage() != null && savedProgress.getProgressPercentage() >= 100) {
            notificationService.notifyCompleted(activity);
        } else {
            notificationService.notifyProgressUpdate(activity);
        }

        return savedProgress;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ActivityProgress> findByActivity(Integer activityId) {
        return progressRepository.findByActivityIdOrderByCreatedAtDesc(activityId);
    }

    @Override
    @Transactional(readOnly = true)
    public ActivityProgress findById(Long progressId) throws Exception {
        return progressRepository.findById(progressId)
                .orElseThrow(() -> new Exception("No se encontró el registro de progreso solicitado"));
    }

    private void updateActivitySummary(Activity activity, ActivityProgress savedProgress) {
        activity.setProgressPercentage(savedProgress.getProgressPercentage());
        activity.setProgressComment(savedProgress.getProgressComment());
        activity.setProgressUpdatedAt(savedProgress.getCreatedAt());
        activity.setProgressUpdatedBy(savedProgress.getCreatedBy());
        activity.setProgressUpdatedByRole(savedProgress.getCreatedByRole());
        activity.setProgressUpdatedByName(savedProgress.getCreatedByName());
        activity.setProgressEvidencePath(savedProgress.getEvidencePath());
        activity.setProgressEvidenceName(savedProgress.getEvidenceName());
        activity.setEdited(new Date());

        if (savedProgress.getProgressPercentage() != null) {
            int percentage = savedProgress.getProgressPercentage();
            if (percentage > 0 && percentage < 100 && activity.getState() == StateActivity.PENDIENTE) {
                activity.setState(StateActivity.EN_PROGRESO);
            }
            if (percentage >= 100) {
                Date delivery = savedProgress.getCreatedAt();
                activity.setDelivey(delivery);
                if (activity.getFinish() != null) {
                    Calendar calendar = Calendar.getInstance();
                    calendar.setTime(activity.getFinish());
                    calendar.add(Calendar.DAY_OF_MONTH, 2);
                    Date extensionDelivery = calendar.getTime();

                    if (activity.getFinish().after(delivery) || activity.getFinish().equals(delivery)
                            || delivery.before(extensionDelivery) || delivery.equals(extensionDelivery)) {
                        activity.setState(StateActivity.COMPLETADO);
                    } else {
                        activity.setState(StateActivity.COMPLETADOT);
                    }
                } else {
                    activity.setState(StateActivity.COMPLETADO);
                }
            }
        }

        activityRepository.save(activity);
    }

    private void validatePermission(Activity activity, String userId, String role) throws Exception {
        if (role == null || role.isBlank()) {
            throw new Exception("Rol del usuario requerido");
        }
        String normalizedRole = role.trim().toLowerCase();
        if ("monitor".equals(normalizedRole)) {
            if (activity.getMonitor() == null) {
                throw new Exception("La actividad no tiene un monitor asignado");
            }
            if (!matchesMonitor(activity.getMonitor(), userId)) {
                throw new Exception("El monitor no está autorizado para actualizar esta actividad");
            }
        } else if ("professor".equals(normalizedRole)) {
            if (activity.getProfessor() == null || !activity.getProfessor().getId().equalsIgnoreCase(userId)) {
                throw new Exception("El profesor no está autorizado para actualizar esta actividad");
            }
        } else if ("jfedpto".equals(normalizedRole)) {
            // El jefe de departamento solo puede consultar, no registrar progreso
            throw new Exception("Los jefes de departamento no pueden registrar progreso");
        }
    }

    private boolean matchesMonitor(Monitor monitor, String userId) {
        if (monitor == null || userId == null) {
            return false;
        }
        return userId.equalsIgnoreCase(monitor.getIdMonitor()) || userId.equalsIgnoreCase(monitor.getCode());
    }

    private String resolveDisplayName(String userId, String role) {
        if (userId == null || role == null) {
            return userId;
        }
        String normalizedRole = role.trim().toLowerCase();
        if ("monitor".equals(normalizedRole)) {
            return monitorRepository.findByIdMonitor(userId)
                .or(() -> monitorRepository.findById(userId))
                .map(m -> (m.getName() + " " + m.getLastName()).trim())
                .orElseGet(() -> prospectRepository.findById(userId)
                    .map(p -> (p.getName() + " " + p.getLastName()).trim())
                    .orElse(userId));
        }
        if ("professor".equals(normalizedRole)) {
            return professorRepository.findById(userId)
                .map(Professor::getName)
                .orElse(userId);
        }
        if ("jfedpto".equals(normalizedRole)) {
            return prospectRepository.findById(userId)
                .map(p -> (p.getName() + " " + p.getLastName()).trim())
                .orElse(userId);
        }
        return prospectRepository.findById(userId)
            .map(p -> (p.getName() + " " + p.getLastName()).trim())
            .orElse(userId);
    }
}
