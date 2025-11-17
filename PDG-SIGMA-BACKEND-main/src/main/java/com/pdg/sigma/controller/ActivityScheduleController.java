package com.pdg.sigma.controller;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.pdg.sigma.dto.ActivityPlanDTO;
import com.pdg.sigma.dto.ActivityScheduleDTO;
import com.pdg.sigma.dto.ScheduleConflictDTO;
import com.pdg.sigma.service.ActivityScheduleService;

import jakarta.validation.Valid;

/**
 * Controller para gestión de horarios y planes de actividades
 * HU-011: Creación de plan de actividades para monitores
 */
@RestController
@RequestMapping("/api/activity-schedule")
public class ActivityScheduleController {

    @Autowired
    private ActivityScheduleService activityScheduleService;

    /**
     * POST /api/activity-schedule/create
     * Crea o actualiza una actividad con horarios (con validación de conflictos)
     */
    @PostMapping("/create")
    public ResponseEntity<?> createActivityWithSchedule(@Valid @RequestBody ActivityScheduleDTO dto) {
        try {
            ActivityScheduleDTO activity = activityScheduleService.saveActivityWithSchedule(dto);
            return ResponseEntity.status(HttpStatus.CREATED).body(activity);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Error: " + e.getMessage());
        }
    }

    /**
     * POST /api/activity-schedule/validate-conflicts
     * Valida si hay conflictos de horarios para una actividad (sin guardarla)
     */
    @PostMapping("/validate-conflicts")
    public ResponseEntity<?> validateScheduleConflicts(@Valid @RequestBody ActivityScheduleDTO dto) {
        try {
            List<ScheduleConflictDTO> conflicts = activityScheduleService.validateScheduleConflicts(dto);
            return ResponseEntity.ok(conflicts);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Error: " + e.getMessage());
        }
    }

    /**
     * GET /api/activity-schedule/plan/{monitoringId}
     * Obtiene el plan completo de actividades para una monitoría
     */
    @GetMapping("/plan/{monitoringId}")
    public ResponseEntity<?> getActivityPlan(@PathVariable Integer monitoringId) {
        try {
            ActivityPlanDTO plan = activityScheduleService.getActivityPlan(monitoringId);
            return ResponseEntity.ok(plan);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Error: " + e.getMessage());
        }
    }

    /**
     * GET /api/activity-schedule/monitor/{monitorId}?startDate=...&endDate=...
     * Obtiene el cronograma de un monitor en un rango de fechas
     */
    @GetMapping("/monitor/{monitorId}")
    public ResponseEntity<?> getMonitorSchedule(
            @PathVariable String monitorId,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") Date startDate,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") Date endDate) {
        try {
            List<ActivityScheduleDTO> schedule = activityScheduleService.getMonitorSchedule(monitorId, startDate, endDate);
            return ResponseEntity.ok(schedule);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error: " + e.getMessage());
        }
    }

    /**
     * GET /api/activity-schedule/professor/{professorId}?startDate=...&endDate=...
     * Obtiene el cronograma de un profesor en un rango de fechas
     */
    @GetMapping("/professor/{professorId}")
    public ResponseEntity<?> getProfessorSchedule(
            @PathVariable String professorId,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") Date startDate,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") Date endDate) {
        try {
            List<ActivityScheduleDTO> schedule = activityScheduleService.getProfessorSchedule(professorId, startDate, endDate);
            return ResponseEntity.ok(schedule);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error: " + e.getMessage());
        }
    }
}

