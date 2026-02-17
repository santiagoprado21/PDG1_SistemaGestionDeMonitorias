package com.pdg.sigma;

import com.pdg.sigma.domain.*;
import com.pdg.sigma.dto.ActivityProgressRequestDTO;
import com.pdg.sigma.repository.ActivityProgressRepository;
import com.pdg.sigma.repository.ActivityRepository;
import com.pdg.sigma.repository.MonitorRepository;
import com.pdg.sigma.repository.ProfessorRepository;
import com.pdg.sigma.repository.ProspectRepository;
import com.pdg.sigma.service.ActivityEvidenceStorageService;
import com.pdg.sigma.service.ActivityEvidenceStorageService.StoredEvidence;
import com.pdg.sigma.service.ActivityProgressServiceImpl;
import com.pdg.sigma.service.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Paths;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Calendar;
import java.util.Date;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ActivityProgressServiceImplTest {

    @Mock
    private ActivityRepository activityRepository;

    @Mock
    private ActivityProgressRepository progressRepository;

    @Mock
    private MonitorRepository monitorRepository;

    @Mock
    private ProfessorRepository professorRepository;

    @Mock
    private ProspectRepository prospectRepository;

    @Mock
    private ActivityEvidenceStorageService evidenceStorageService;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private ActivityProgressServiceImpl service;

    private Activity activity;
    private Monitor monitor;
    private Professor professor;

    @BeforeEach
    void setUp() {
        monitor = buildMonitor();
        professor = buildProfessor();
        activity = buildActivity(monitor, professor, StateActivity.PENDIENTE, futureDate(5));
    }

    @Test
    void registerProgress_asignedMonitorWithEvidence_updatesActivityAndNotifiesProgressUpdate() throws Exception {
        when(activityRepository.findById(eq(99))).thenReturn(Optional.of(activity));
        when(progressRepository.save(any(ActivityProgress.class))).thenAnswer(invocation -> {
            ActivityProgress progress = invocation.getArgument(0);
            progress.setId(1L);
            progress.setCreatedAt(Date.from(Instant.now()));
            return progress;
        });

        ActivityProgressRequestDTO payload = buildPayload(monitor.getIdMonitor(), "MONITOR", 60, "Avance parcial");

        when(monitorRepository.findByIdMonitor(monitor.getIdMonitor())).thenReturn(Optional.of(monitor));

        MultipartFile evidence = mock(MultipartFile.class);
        when(evidence.isEmpty()).thenReturn(false);
        when(evidence.getOriginalFilename()).thenReturn("avance.pdf");
        when(evidenceStorageService.store(eq(99), eq(evidence))).thenReturn(
                new StoredEvidence("activity-99/avance.pdf", "avance.pdf", Paths.get("avance.pdf"))
        );

        ActivityProgress result = service.registerProgress(99, payload, evidence);

        assertThat(result.getProgressPercentage()).isEqualTo(60);
        assertThat(result.getEvidencePath()).isEqualTo("activity-99/avance.pdf");
        assertThat(activity.getProgressPercentage()).isEqualTo(60);
        assertThat(activity.getProgressComment()).isEqualTo("Avance parcial");
        assertThat(activity.getProgressUpdatedBy()).isEqualTo(monitor.getIdMonitor());
        assertThat(activity.getState()).isEqualTo(StateActivity.EN_PROGRESO);

        verify(evidenceStorageService).store(eq(99), eq(evidence));
        verify(activityRepository).save(activity);
        verify(notificationService).notifyProgressUpdate(activity);
        verify(notificationService, never()).notifyCompleted(any());
    }

    @Test
    void registerProgress_monitorCompletesBeforeDeadline_setsCompletedStateAndNotifiesCompleted() throws Exception {
        when(activityRepository.findById(eq(99))).thenReturn(Optional.of(activity));
        when(progressRepository.save(any(ActivityProgress.class))).thenAnswer(invocation -> {
            ActivityProgress progress = invocation.getArgument(0);
            progress.setId(1L);
            progress.setCreatedAt(Date.from(Instant.now()));
            return progress;
        });

        ActivityProgressRequestDTO payload = buildPayload(monitor.getIdMonitor(), "monitor", 100, "Finalizado a tiempo");

        when(monitorRepository.findByIdMonitor(monitor.getIdMonitor())).thenReturn(Optional.of(monitor));

        ActivityProgress result = service.registerProgress(99, payload, null);

        assertThat(result.getProgressPercentage()).isEqualTo(100);
        assertThat(activity.getState()).isEqualTo(StateActivity.COMPLETADO);
        assertThat(activity.getDelivey()).isNotNull();
        verify(notificationService).notifyCompleted(activity);
        verify(notificationService, never()).notifyProgressUpdate(any());
        verify(evidenceStorageService, never()).store(any(), any());
    }

    @Test
    void registerProgress_monitorCompletesAfterDeadline_setsCompletedTState() throws Exception {
        Activity lateActivity = buildActivity(monitor, professor, StateActivity.PENDIENTE, pastDate(7));
        when(activityRepository.findById(eq(199))).thenReturn(Optional.of(lateActivity));
        when(progressRepository.save(any(ActivityProgress.class))).thenAnswer(invocation -> {
            ActivityProgress progress = invocation.getArgument(0);
            progress.setId(2L);
            Date createdAt = Date.from(Instant.now());
            progress.setCreatedAt(createdAt);
            return progress;
        });

        ActivityProgressRequestDTO payload = buildPayload(monitor.getIdMonitor(), "MONITOR", 100, "Entregado tarde");
        when(monitorRepository.findByIdMonitor(monitor.getIdMonitor())).thenReturn(Optional.of(monitor));

        ActivityProgress result = service.registerProgress(199, payload, null);

        assertThat(result.getProgressPercentage()).isEqualTo(100);
        assertThat(lateActivity.getState()).isEqualTo(StateActivity.COMPLETADOT);
        verify(notificationService).notifyCompleted(lateActivity);
    }

    @Test
    void registerProgress_whenMonitorNotAssigned_throwsException() {
        when(activityRepository.findById(eq(99))).thenReturn(Optional.of(activity));

        ActivityProgressRequestDTO payload = buildPayload("UNAUTHORIZED", "monitor", 50, "Avance no válido");

        assertThatThrownBy(() -> service.registerProgress(99, payload, null))
                .isInstanceOf(Exception.class)
                .hasMessageContaining("no está autorizado");

        verify(progressRepository, never()).save(any());
        verify(notificationService, never()).notifyProgressUpdate(any());
        verify(notificationService, never()).notifyCompleted(any());
    }

    @Test
    void registerProgress_professorCanUpdateProgress() throws Exception {
        when(activityRepository.findById(eq(99))).thenReturn(Optional.of(activity));
        when(progressRepository.save(any(ActivityProgress.class))).thenAnswer(invocation -> {
            ActivityProgress progress = invocation.getArgument(0);
            progress.setId(3L);
            progress.setCreatedAt(Date.from(Instant.now()));
            return progress;
        });

        ActivityProgressRequestDTO payload = buildPayload(professor.getId(), "professor", 40, "Seguimiento profesor");
        when(professorRepository.findById(professor.getId())).thenReturn(Optional.of(professor));

        ActivityProgress result = service.registerProgress(99, payload, null);

        assertThat(result.getProgressPercentage()).isEqualTo(40);
        assertThat(activity.getProgressUpdatedByRole()).isEqualTo("professor");
        assertThat(activity.getProgressUpdatedBy()).isEqualTo(professor.getId());
        verify(notificationService).notifyProgressUpdate(activity);
    }

    private ActivityProgressRequestDTO buildPayload(String userId, String role, int progress, String comment) {
        ActivityProgressRequestDTO dto = new ActivityProgressRequestDTO();
        dto.setUserId(userId);
        dto.setUserRole(role);
        dto.setProgressPercentage(progress);
        dto.setProgressComment(comment);
        return dto;
    }

    private Activity buildActivity(Monitor monitor, Professor professor, StateActivity state, Date finishDate) {
        Activity activity = new Activity();
        activity.setId(99);
        activity.setName("Tutoría");
        activity.setCreation(Date.from(Instant.now().minus(10, ChronoUnit.DAYS)));
        activity.setFinish(finishDate);
        activity.setRoleCreator("P");
        activity.setRoleResponsable("M");
        activity.setCategory("Tutoría");
        activity.setDescription("Ayuda a estudiantes");
        activity.setMonitoring(buildMonitoring(professor));
        activity.setProfessor(professor);
        activity.setMonitor(monitor);
        activity.setState(state);
        activity.setSemester("2025-1");
        return activity;
    }

    private Monitoring buildMonitoring(Professor professor) {
        Monitoring monitoring = new Monitoring();
        monitoring.setId(1L);
        monitoring.setProfessor(professor);
        monitoring.setSemester("2025-1");
        monitoring.setStart(Date.from(Instant.now().minus(30, ChronoUnit.DAYS)));
        monitoring.setFinish(Date.from(Instant.now().plus(30, ChronoUnit.DAYS)));

        Program program = new Program();
        program.setId(1L);
        program.setName("Ingeniería");
        program.setSchool(buildSchool());
        monitoring.setProgram(program);

        Course course = new Course();
        course.setId(1L);
        course.setName("Programación");
        course.setProgram(program);
        monitoring.setCourse(course);

        monitoring.setSchool(program.getSchool());
        return monitoring;
    }

    private School buildSchool() {
        School school = new School();
        school.setId(1L);
        school.setName("Ingenierías");
        return school;
    }

    private Monitor buildMonitor() {
        Monitor monitor = new Monitor();
        monitor.setCode("MON-001");
        monitor.setName("Ana");
        monitor.setLastName("Díaz");
        monitor.setSemester(5);
        monitor.setGradeAverage(4.2);
        monitor.setGradeCourse(4.5);
        monitor.setEmail("ana.diaz@uni.edu");
        monitor.setIdMonitor("ID-MON-001");
        return monitor;
    }

    private Professor buildProfessor() {
        Professor prof = new Professor();
        prof.setId("PROF-001");
        prof.setName("Dr. Test");
        prof.setPassword("secret");
        return prof;
    }

    private Date futureDate(int daysAhead) {
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DAY_OF_MONTH, daysAhead);
        return calendar.getTime();
    }

    private Date pastDate(int daysAgo) {
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DAY_OF_MONTH, -daysAgo);
        return calendar.getTime();
    }
}
