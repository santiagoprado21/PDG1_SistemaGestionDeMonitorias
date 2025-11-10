package com.pdg.sigma.notification;

import java.util.Date;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.pdg.sigma.domain.Activity;
import com.pdg.sigma.domain.StateActivity;
import com.pdg.sigma.repository.ActivityRepository;
import com.pdg.sigma.service.NotificationService;

@Component
public class NotificationScheduler {

    @Autowired
    private ActivityRepository activityRepository;

    @Autowired
    private NotificationService notificationService;

    @Value("${sigma.notifications.overdue-enabled:true}")
    private boolean overdueEnabled;

    // Ejecuta cada día a las 8:00 AM por defecto (configurable)
    @Scheduled(cron = "${sigma.notifications.overdue-cron:0 0 8 * * *}")
    public void checkOverdueActivities() {
        if (!overdueEnabled) return;
        try {
            Date today = new Date();
            List<Activity> all = activityRepository.findAll();
            for (Activity a : all) {
                if (a.getFinish() != null && a.getFinish().before(today) && a.getState() == StateActivity.PENDIENTE) {
                    notificationService.notifyOverdue(a);
                }
            }
        } catch (Exception ex) {
            // Evitar que falle el scheduler por errores transitorios
            System.err.println("NotificationScheduler error: " + ex.getMessage());
        }
    }
}
