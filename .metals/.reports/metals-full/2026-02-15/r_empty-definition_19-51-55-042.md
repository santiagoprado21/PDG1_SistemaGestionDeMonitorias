error id: file:///C:/Users/cardo/Desktop/Semestre%2010/Proyecto%20de%20grado%201/PDG1_SistemaGestionDeMonitorias/PDG-SIGMA-BACKEND-main/src/main/java/com/pdg/sigma/service/SupervisorEvaluationServiceImpl.java:com/pdg/sigma/domain/SupervisorEvaluation#
file:///C:/Users/cardo/Desktop/Semestre%2010/Proyecto%20de%20grado%201/PDG1_SistemaGestionDeMonitorias/PDG-SIGMA-BACKEND-main/src/main/java/com/pdg/sigma/service/SupervisorEvaluationServiceImpl.java
empty definition using pc, found symbol in pc: com/pdg/sigma/domain/SupervisorEvaluation#
empty definition using semanticdb
empty definition using fallback
non-local guesses:

offset: 229
uri: file:///C:/Users/cardo/Desktop/Semestre%2010/Proyecto%20de%20grado%201/PDG1_SistemaGestionDeMonitorias/PDG-SIGMA-BACKEND-main/src/main/java/com/pdg/sigma/service/SupervisorEvaluationServiceImpl.java
text:
```scala
package com.pdg.sigma.service;

import com.pdg.sigma.domain.Monitor;
import com.pdg.sigma.domain.Monitoring;
import com.pdg.sigma.domain.MonitoringMonitor;
import com.pdg.sigma.domain.Professor;
import com.pdg.sigma.domain.@@SupervisorEvaluation;
import com.pdg.sigma.dto.SupervisorEvaluationRequest;
import com.pdg.sigma.dto.SupervisorEvaluationResponse;
import com.pdg.sigma.dto.SupervisorEvaluationStatusDTO;
import com.pdg.sigma.repository.MonitorRepository;
import com.pdg.sigma.repository.MonitoringMonitorRepository;
import com.pdg.sigma.repository.MonitoringRepository;
import com.pdg.sigma.repository.ProfessorRepository;
import com.pdg.sigma.repository.SupervisorEvaluationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class SupervisorEvaluationServiceImpl implements SupervisorEvaluationService {

    private static final int MIN_SCORE = 1;
    private static final int MAX_SCORE = 5;

    @Autowired
    private SupervisorEvaluationRepository supervisorEvaluationRepository;

    @Autowired
    private MonitoringRepository monitoringRepository;

    @Autowired
    private MonitoringMonitorRepository monitoringMonitorRepository;

    @Autowired
    private MonitorRepository monitorRepository;

    @Autowired
    private ProfessorRepository professorRepository;

    @Override
    @Transactional
    public SupervisorEvaluationResponse createEvaluation(String monitorIdentifier, SupervisorEvaluationRequest request) throws Exception {
        String resolvedMonitorId = resolveMonitorIdentifier(monitorIdentifier, request);
        validateRequest(resolvedMonitorId, request);

        Monitor monitor = resolveMonitor(resolvedMonitorId);
        Monitoring monitoring = monitoringRepository.findById(request.getMonitoringId())
                .orElseThrow(() -> new Exception("Monitoría no encontrada"));

        ensureMonitorAssignment(monitoring, monitor);

        supervisorEvaluationRepository.findByMonitoringIdAndMonitorCode(monitoring.getId(), monitor.getCode())
                .ifPresent(existing -> {
                    throw new IllegalStateException("Ya existe una evaluación registrada para esta monitoría");
                });

        Professor professor = Optional.ofNullable(monitoring.getProfessor())
                .orElseThrow(() -> new Exception("Profesor supervisor no encontrado"));

        MonitoringMonitor monitoringMonitor = monitoringMonitorRepository
                .findByMonitoringIdAndMonitorCode(monitoring.getId(), monitor.getCode())
                .orElse(null);

        SupervisorEvaluation evaluation = new SupervisorEvaluation();
        evaluation.setMonitoring(monitoring);
        evaluation.setMonitor(monitor);
        evaluation.setProfessor(professor);
        evaluation.setMonitoringMonitor(monitoringMonitor);
        evaluation.setSemester(monitoring.getSemester());
        evaluation.setSubmittedBy(resolveSubmittedBy(monitor));
        evaluation.applyScores(
                request.getGuidanceQuality(),
                request.getAvailabilitySupport(),
                request.getFeedbackQuality(),
                request.getRespectSupport(),
                request.getComments()
        );

        SupervisorEvaluation saved = supervisorEvaluationRepository.save(evaluation);
        return toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SupervisorEvaluationStatusDTO> getAssignmentsForMonitor(String monitorIdentifier) throws Exception {
        Monitor monitor = resolveMonitor(monitorIdentifier);

        Map<Long, SupervisorEvaluation> evaluationMap = supervisorEvaluationRepository
                .findByMonitorCodeOrderByCreatedAtDesc(monitor.getCode())
                .stream()
                .filter(evaluation -> evaluation.getMonitoring() != null && evaluation.getMonitoring().getId() != null)
                .collect(Collectors.toMap(
                        evaluation -> evaluation.getMonitoring().getId(),
                        evaluation -> evaluation,
                        (first, second) -> first
                ));

        Set<Long> processedMonitoringIds = new HashSet<>();
        List<SupervisorEvaluationStatusDTO> assignments = new ArrayList<>();

        List<MonitoringMonitor> relations = monitoringMonitorRepository.findByMonitor(monitor);
        if (relations != null) {
            for (MonitoringMonitor relation : relations) {
                if (relation == null || relation.getMonitoring() == null) {
                    continue;
                }
                if (!isEligibleStatus(relation.getEstadoSeleccion())) {
                    continue;
                }
                Monitoring monitoring = relation.getMonitoring();
                if (monitoring.getId() == null || !processedMonitoringIds.add(monitoring.getId())) {
                    continue;
                }
                SupervisorEvaluation evaluation = evaluationMap.get(monitoring.getId());
                assignments.add(buildStatusDto(monitoring, relation, evaluation));
            }
        }

        List<Monitoring> directMonitorings = monitoringRepository.findByAssignedMonitor(monitor);
        if (directMonitorings != null) {
            for (Monitoring monitoring : directMonitorings) {
                if (monitoring == null || monitoring.getId() == null || !processedMonitoringIds.add(monitoring.getId())) {
                    continue;
                }
                SupervisorEvaluation evaluation = evaluationMap.get(monitoring.getId());
                assignments.add(buildStatusDto(monitoring, null, evaluation));
            }
        }

        assignments.sort(Comparator
                .comparing(SupervisorEvaluationStatusDTO::isEvaluated)
                .thenComparing(SupervisorEvaluationStatusDTO::getMonitoringName, Comparator.nullsLast(String::compareToIgnoreCase))
        );

        return assignments;
    }

    @Override
    @Transactional(readOnly = true)
    public List<SupervisorEvaluationResponse> getEvaluationsForCoordinator() throws Exception {
        return supervisorEvaluationRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<SupervisorEvaluationResponse> getEvaluationsByProfessor(String professorId) throws Exception {
        if (professorId == null || professorId.trim().isEmpty()) {
            throw new Exception("Debe indicar el profesor supervisor");
        }
        if (!professorRepository.existsById(professorId)) {
            throw new Exception("Profesor supervisor no encontrado");
        }
        return supervisorEvaluationRepository.findByProfessorIdOrderByCreatedAtDesc(professorId)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<SupervisorEvaluationResponse> getEvaluationsByMonitor(String monitorIdentifier) throws Exception {
        Monitor monitor = resolveMonitor(monitorIdentifier);
        return supervisorEvaluationRepository.findByMonitorCodeOrderByCreatedAtDesc(monitor.getCode())
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<SupervisorEvaluationResponse> getEvaluation(Long evaluationId) {
        return supervisorEvaluationRepository.findById(evaluationId)
                .map(this::toResponse);
    }

    private void validateRequest(String monitorIdentifier, SupervisorEvaluationRequest request) throws Exception {
        if (monitorIdentifier == null || monitorIdentifier.trim().isEmpty()) {
            throw new Exception("Debe indicar el monitor que envía la evaluación");
        }
        if (request.getMonitoringId() == null) {
            throw new Exception("Debe especificar la monitoría a evaluar");
        }
        validateScores(request);
    }

    private void validateScores(SupervisorEvaluationRequest request) throws Exception {
        ensureScoreInRange("Claridad y orientación", request.getGuidanceQuality());
        ensureScoreInRange("Disponibilidad y acompañamiento", request.getAvailabilitySupport());
        ensureScoreInRange("Calidad de la retroalimentación", request.getFeedbackQuality());
        ensureScoreInRange("Respeto y apoyo", request.getRespectSupport());
    }

    private void ensureScoreInRange(String label, Integer value) throws Exception {
        if (value == null) {
            throw new Exception("Debe proporcionar una calificación para " + label);
        }
        if (value < MIN_SCORE || value > MAX_SCORE) {
            throw new Exception(label + " debe estar entre " + MIN_SCORE + " y " + MAX_SCORE);
        }
    }

    private String resolveMonitorIdentifier(String explicit, SupervisorEvaluationRequest request) {
        if (explicit != null && !explicit.trim().isEmpty()) {
            return explicit.trim();
        }
        if (request.getMonitorIdentifier() != null && !request.getMonitorIdentifier().trim().isEmpty()) {
            return request.getMonitorIdentifier().trim();
        }
        return null;
    }

    private Monitor resolveMonitor(String monitorIdentifier) throws Exception {
        if (monitorIdentifier == null || monitorIdentifier.trim().isEmpty()) {
            throw new Exception("Debe indicar el monitor");
        }
        String trimmed = monitorIdentifier.trim();

        Optional<Monitor> monitor = monitorRepository.findById(trimmed);
        if (monitor.isPresent()) {
            return monitor.get();
        }
        monitor = monitorRepository.findByCode(trimmed);
        if (monitor.isPresent()) {
            return monitor.get();
        }
        monitor = monitorRepository.findByIdMonitor(trimmed);
        if (monitor.isPresent()) {
            return monitor.get();
        }
        throw new Exception("Monitor no encontrado");
    }

    private void ensureMonitorAssignment(Monitoring monitoring, Monitor monitor) throws Exception {
        if (monitoring == null || monitor == null) {
            throw new Exception("Monitoría o monitor inválido");
        }
        Monitor assigned = monitoring.getAssignedMonitor();
        if (assigned != null && (assigned.getCode().equalsIgnoreCase(monitor.getCode())
 (assigned.getIdMonitor() != null && assigned.getIdMonitor().equalsIgnoreCase(monitor.getIdMonitor())))) {
            return;
        }

        Optional<MonitoringMonitor> relation = monitoringMonitorRepository
                .findByMonitoringIdAndMonitorCode(monitoring.getId(), monitor.getCode());
        if (relation.isPresent() && isEligibleStatus(relation.get().getEstadoSeleccion())) {
            return;
        }

        throw new Exception("El monitor no está asignado a la monitoría indicada");
    }

    private boolean isEligibleStatus(String status) {
        if (status == null) {
            return false;
        }
        String normalized = status.trim().toLowerCase(Locale.ROOT);
        return "seleccionado".equals(normalized)
 "aprobado".equals(normalized)
 "asignado".equals(normalized);
    }

    private SupervisorEvaluationStatusDTO buildStatusDto(Monitoring monitoring, MonitoringMonitor relation, SupervisorEvaluation evaluation) {
        SupervisorEvaluationStatusDTO dto = new SupervisorEvaluationStatusDTO();
        dto.setMonitoringId(monitoring.getId());
        dto.setMonitoringMonitorId(relation != null ? relation.getId() : null);
        dto.setEvaluationId(evaluation != null ? evaluation.getId() : null);

        String courseName = monitoring.getCourse() != null ? monitoring.getCourse().getName() : null;
        String programName = monitoring.getProgram() != null ? monitoring.getProgram().getName() : null;
        String semester = monitoring.getSemester();
        String monitoringName = buildMonitoringName(courseName, semester);

        dto.setMonitoringName(monitoringName);
        dto.setCourseName(courseName);
        dto.setProgramName(programName);
        dto.setSemester(semester);

        Professor professor = monitoring.getProfessor();
        dto.setProfessorId(professor != null ? professor.getId() : null);
        dto.setProfessorName(professor != null ? professor.getName() : null);

        if (evaluation != null) {
            dto.setEvaluated(true);
            dto.setStatus("ENVIADA");
            dto.setSubmittedAt(evaluation.getCreatedAt());
        } else {
            dto.setEvaluated(false);
            dto.setStatus("PENDIENTE");
        }
        return dto;
    }

    private SupervisorEvaluationResponse toResponse(SupervisorEvaluation evaluation) {
        SupervisorEvaluationResponse response = new SupervisorEvaluationResponse();
        response.setEvaluationId(evaluation.getId());
        response.setMonitoringId(evaluation.getMonitoring() != null ? evaluation.getMonitoring().getId() : null);
        response.setMonitoringMonitorId(evaluation.getMonitoringMonitor() != null ? evaluation.getMonitoringMonitor().getId() : null);

        Monitoring monitoring = evaluation.getMonitoring();
        if (monitoring != null) {
            String courseName = monitoring.getCourse() != null ? monitoring.getCourse().getName() : null;
            String programName = monitoring.getProgram() != null ? monitoring.getProgram().getName() : null;
            response.setMonitoringName(buildMonitoringName(courseName, monitoring.getSemester()));
            response.setCourseName(courseName);
            response.setProgramName(programName);
            response.setSemester(monitoring.getSemester());
            response.setProfessorId(monitoring.getProfessor() != null ? monitoring.getProfessor().getId() : null);
            response.setProfessorName(monitoring.getProfessor() != null ? monitoring.getProfessor().getName() : null);
        }

        Monitor monitor = evaluation.getMonitor();
        if (monitor != null) {
            response.setMonitorCode(monitor.getCode());
            response.setMonitorIdentifier(monitor.getIdMonitor());
            response.setMonitorFullName(buildMonitorFullName(monitor));
            response.setMonitorEmail(monitor.getEmail());
        }

        response.setGuidanceQuality(evaluation.getGuidanceQuality());
        response.setAvailabilitySupport(evaluation.getAvailabilitySupport());
        response.setFeedbackQuality(evaluation.getFeedbackQuality());
        response.setRespectSupport(evaluation.getRespectSupport());

        response.setTotalScore(evaluation.getTotalScore());
        response.setPerformanceLevel(evaluation.getPerformanceLevel());
        response.setComments(evaluation.getComments());
        response.setSubmittedBy(evaluation.getSubmittedBy());

        response.setCreatedAt(evaluation.getCreatedAt());
        response.setUpdatedAt(evaluation.getUpdatedAt());
        return response;
    }

    private String resolveSubmittedBy(Monitor monitor) {
        if (monitor == null) {
            return null;
        }
        String identifier = monitor.getIdMonitor();
        if (identifier != null && !identifier.isBlank()) {
            return identifier;
        }
        return monitor.getCode();
    }

    private String buildMonitoringName(String courseName, String semester) {
        if (courseName == null && semester == null) {
            return null;
        }
        if (courseName == null) {
            return semester;
        }
        if (semester == null || semester.isBlank()) {
            return courseName;
        }
        return courseName + " - " + semester;
    }

    private String buildMonitorFullName(Monitor monitor) {
        if (monitor == null) {
            return null;
        }
        String first = Optional.ofNullable(monitor.getName()).orElse("");
        String last = Optional.ofNullable(monitor.getLastName()).orElse("");
        return (first + " " + last).trim();
    }
}

```


#### Short summary: 

empty definition using pc, found symbol in pc: com/pdg/sigma/domain/SupervisorEvaluation#