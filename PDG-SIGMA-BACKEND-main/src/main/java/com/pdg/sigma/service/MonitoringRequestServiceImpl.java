package com.pdg.sigma.service;

import com.pdg.sigma.domain.*;
import com.pdg.sigma.dto.MonitoringRequestDTO;
import com.pdg.sigma.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Implementación del servicio para gestionar MonitoringRequest (Convocatorias)
 * HU-010: Crear postulación de monitorias por parte de los profesores
 */
@Service
@Transactional
public class MonitoringRequestServiceImpl implements MonitoringRequestService {

    @Autowired
    private MonitoringRequestRepository monitoringRequestRepository;
    
    @Autowired
    private ProfessorRepository professorRepository;
    
    @Autowired
    private CourseRepository courseRepository;
    
    @Autowired
    private SchoolRepository schoolRepository;
    
    @Autowired
    private ProgramRepository programRepository;
    
    @Autowired
    private MonitorApplicationRepository monitorApplicationRepository;
    
    @Autowired
    private CourseProfessorRepository courseProfessorRepository;
    
    @Autowired
    private DepartmentBudgetRepository departmentBudgetRepository;
    
    @Autowired
    private HeadProgramRepository headProgramRepository;
    
    @Autowired
    private DepartmentHeadRepository departmentHeadRepository;

    // ==================== MÉTODOS PRINCIPALES ====================

    @Override
    public MonitoringRequest createConvocatoria(MonitoringRequestDTO dto) throws Exception {
        System.out.println("=== CREANDO CONVOCATORIA DE MONITORÍA ===");
        
        // 1. Validar y obtener entidades
        Professor professor = professorRepository.findById(dto.getProfessorId())
                .orElseThrow(() -> new Exception("Profesor no encontrado con ID: " + dto.getProfessorId()));
        
        Course course = courseRepository.findById(dto.getCourseId())
                .orElseThrow(() -> new Exception("Curso no encontrado con ID: " + dto.getCourseId()));
        
        School school = schoolRepository.findById(Long.valueOf(dto.getSchoolId()))
                .orElseThrow(() -> new Exception("Facultad no encontrada con ID: " + dto.getSchoolId()));
        
        Program program = programRepository.findById(dto.getProgramId())
                .orElseThrow(() -> new Exception("Programa no encontrado con ID: " + dto.getProgramId()));
        
        // 2. Validar que el profesor tenga permiso para este curso
        if (!validateProfessorPermission(dto.getProfessorId(), dto.getCourseId())) {
            throw new Exception("El profesor no tiene permiso para crear convocatoria en este curso");
        }
        
        // 3. Validar que no exista otra convocatoria activa para el mismo curso y semestre
        Optional<MonitoringRequest> existing = monitoringRequestRepository
                .findByProfessorAndCourseAndSemester(professor, course, dto.getSemester());
        
        if (existing.isPresent()) {
            MonitoringRequest existingRequest = existing.get();
            // Solo rechazar si está en estado activo (no rechazada o cancelada)
            if (existingRequest.getStatus() == RequestStatus.CONVOCATORIA_ABIERTA ||
                existingRequest.getStatus() == RequestStatus.MONITOR_SELECCIONADO ||
                existingRequest.getStatus() == RequestStatus.PENDIENTE_APROBACION) {
                throw new Exception("Ya existe una convocatoria activa para este curso en el semestre " + dto.getSemester());
            }
        }
        
        // 4. Validar presupuesto disponible
        if (!validateBudgetAvailability(dto.getProgramId(), dto.getSemester(), dto.getRequestedHours())) {
            throw new Exception("No hay presupuesto disponible para las horas solicitadas");
        }
        
        // 5. Validar datos de entrada
        if (dto.getRequestedHours() == null || dto.getRequestedHours() <= 0) {
            throw new Exception("Las horas solicitadas deben ser mayores a 0");
        }
        
        if (dto.getJustification() == null || dto.getJustification().trim().isEmpty()) {
            throw new Exception("La justificación es obligatoria");
        }
        
        if (dto.getStartDate() == null || dto.getFinishDate() == null) {
            throw new Exception("Las fechas de inicio y fin son obligatorias");
        }
        
        if (dto.getStartDate().after(dto.getFinishDate())) {
            throw new Exception("La fecha de inicio debe ser anterior a la fecha de fin");
        }
        
        // 6. Crear la convocatoria
        MonitoringRequest request = new MonitoringRequest(
                professor, course, school, program,
                dto.getRequestedHours(),
                dto.getJustification(),
                dto.getSemester(),
                dto.getStartDate(),
                dto.getFinishDate(),
                dto.getRequiredAverageGrade() != null ? dto.getRequiredAverageGrade() : 4.0,
                dto.getRequiredCourseGrade() != null ? dto.getRequiredCourseGrade() : 4.0,
                dto.getHourlyRate()
        );
        
        MonitoringRequest saved = monitoringRequestRepository.save(request);
        
        System.out.println("Convocatoria creada con ID: " + saved.getId());
        System.out.println("Estado: " + saved.getStatus());
        System.out.println("=========================================");
        
        return saved;
    }

    @Override
    public List<MonitoringRequest> findOpenConvocatorias() {
        return monitoringRequestRepository.findAllOpenConvocatorias();
    }

    @Override
    public List<MonitoringRequest> findOpenConvocatoriasByProgram(Integer programId) {
        return monitoringRequestRepository.findOpenConvocatoriasByProgram(programId);
    }

    @Override
    public List<MonitoringRequest> findByProfessor(String professorId) {
        Optional<Professor> professor = professorRepository.findById(professorId);
        if (professor.isEmpty()) {
            return List.of();
        }
        return monitoringRequestRepository.findByProfessor(professor.get());
    }

    @Override
    public List<MonitoringRequest> findPendingApprovalForDepartmentHead(String departmentHeadId) throws Exception {
        // Buscar el jefe de departamento
        Optional<DepartmentHead> departmentHead = departmentHeadRepository.findById(departmentHeadId);
        if (departmentHead.isEmpty()) {
            throw new Exception("Jefe de departamento no encontrado");
        }
        
        // Buscar los programas asociados al jefe
        List<HeadProgram> headPrograms = headProgramRepository.findByDepartmentHeadId(departmentHeadId);
        if (headPrograms.isEmpty()) {
            return List.of();
        }
        
        // Obtener convocatorias pendientes de todos los programas del jefe
        return headPrograms.stream()
                .flatMap(hp -> monitoringRequestRepository
                        .findPendingApprovalByProgram(hp.getProgram().getId().intValue())
                        .stream())
                .distinct()
                .collect(java.util.stream.Collectors.toList());
    }

    @Override
    public void cancelConvocatoria(Long requestId, String professorId) throws Exception {
        MonitoringRequest request = monitoringRequestRepository.findById(requestId)
                .orElseThrow(() -> new Exception("Convocatoria no encontrada"));
        
        // Validar que el profesor sea el dueño de la convocatoria
        if (!request.getProfessor().getId().equals(professorId)) {
            throw new Exception("Solo el profesor que creó la convocatoria puede cancelarla");
        }
        
        // Solo se puede cancelar si está abierta
        if (request.getStatus() != RequestStatus.CONVOCATORIA_ABIERTA) {
            throw new Exception("Solo se pueden cancelar convocatorias en estado ABIERTA");
        }
        
        // Cambiar estado a CANCELADA
        request.setStatus(RequestStatus.CANCELADA);
        request.setUpdatedAt(LocalDateTime.now());
        
        monitoringRequestRepository.save(request);
        
        System.out.println("Convocatoria " + requestId + " cancelada por profesor " + professorId);
    }

    @Override
    public void markMonitorSelected(Long requestId) throws Exception {
        MonitoringRequest request = monitoringRequestRepository.findById(requestId)
                .orElseThrow(() -> new Exception("Convocatoria no encontrada"));
        
        request.setStatus(RequestStatus.MONITOR_SELECCIONADO);
        request.setUpdatedAt(LocalDateTime.now());
        monitoringRequestRepository.save(request);
    }

    @Override
    public void markPendingApproval(Long requestId) throws Exception {
        MonitoringRequest request = monitoringRequestRepository.findById(requestId)
                .orElseThrow(() -> new Exception("Convocatoria no encontrada"));
        
        request.setStatus(RequestStatus.PENDIENTE_APROBACION);
        request.setUpdatedAt(LocalDateTime.now());
        monitoringRequestRepository.save(request);
    }

    @Override
    public void markApproved(Long requestId) throws Exception {
        MonitoringRequest request = monitoringRequestRepository.findById(requestId)
                .orElseThrow(() -> new Exception("Convocatoria no encontrada"));
        
        request.setStatus(RequestStatus.APROBADA);
        request.setUpdatedAt(LocalDateTime.now());
        monitoringRequestRepository.save(request);
    }

    @Override
    public void markRejected(Long requestId) throws Exception {
        MonitoringRequest request = monitoringRequestRepository.findById(requestId)
                .orElseThrow(() -> new Exception("Convocatoria no encontrada"));
        
        request.setStatus(RequestStatus.RECHAZADA);
        request.setUpdatedAt(LocalDateTime.now());
        monitoringRequestRepository.save(request);
    }

    @Override
    public boolean validateProfessorPermission(String professorId, Long courseId) throws Exception {
        // Verificar que el profesor esté asignado al curso
        Optional<Professor> professor = professorRepository.findById(professorId);
        if (professor.isEmpty()) {
            return false;
        }
        
        List<CourseProfessor> assignments = courseProfessorRepository.findByProfessor(professor.get());
        
        return assignments.stream()
                .anyMatch(cp -> cp.getCourse().getId().equals(courseId));
    }

    @Override
    public boolean validateBudgetAvailability(Long programId, String semester, Integer requestedHours) throws Exception {
        // Buscar el presupuesto del programa para el semestre
        Optional<Program> program = programRepository.findById(programId);
        if (program.isEmpty()) {
            return false;
        }
        
        Optional<DepartmentBudget> budgetOpt = departmentBudgetRepository
                .findByProgramAndSemester(program.get(), semester);
        
        if (budgetOpt.isEmpty()) {
            // Si no hay presupuesto configurado, permitir (para no bloquear el flujo)
            System.out.println("ADVERTENCIA: No hay presupuesto configurado para programa " + programId + " semestre " + semester);
            return true;
        }
        
        DepartmentBudget budget = budgetOpt.get();
        int totalBudget = budget.getTotalHours();
        
        // Calcular horas ya usadas en convocatorias aprobadas o pendientes
        List<MonitoringRequest> existingRequests = monitoringRequestRepository
                .findByProgramAndSemester(program.get(), semester);
        
        int usedHours = existingRequests.stream()
                .filter(r -> r.getStatus() == RequestStatus.PENDIENTE_APROBACION ||
                            r.getStatus() == RequestStatus.APROBADA)
                .mapToInt(r -> r.getRequestedHours() != null ? r.getRequestedHours() : 0)
                .sum();
        
        int availableHours = totalBudget - usedHours;
        
        System.out.println("Presupuesto - Total: " + totalBudget + "h, Usado: " + usedHours + "h, Disponible: " + availableHours + "h, Solicitado: " + requestedHours + "h");
        
        return requestedHours <= availableHours;
    }

    @Override
    public Integer getApplicationCount(Long requestId) {
        Optional<MonitoringRequest> request = monitoringRequestRepository.findById(requestId);
        if (request.isEmpty()) {
            return 0;
        }
        return request.get().getApplicationCount();
    }

    // ==================== MÉTODOS DE GenericService ====================

    @Override
    public List<MonitoringRequest> findAll() {
        return monitoringRequestRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<MonitoringRequest> findById(Long id) {
        Optional<MonitoringRequest> requestOpt = monitoringRequestRepository.findById(id);
        // Forzar carga de relaciones lazy dentro de la transacción
        requestOpt.ifPresent(request -> {
            if (request.getProfessor() != null) {
                request.getProfessor().getName();
            }
            if (request.getCourse() != null) {
                request.getCourse().getName();
            }
            if (request.getSchool() != null) {
                request.getSchool().getName();
            }
            if (request.getProgram() != null) {
                request.getProgram().getName();
            }
        });
        return requestOpt;
    }

    @Override
    public MonitoringRequest save(MonitoringRequest entity) throws Exception {
        return monitoringRequestRepository.save(entity);
    }

    @Override
    public MonitoringRequest update(MonitoringRequest entity) throws Exception {
        if (entity.getId() == null || !monitoringRequestRepository.existsById(entity.getId())) {
            throw new Exception("No se puede actualizar una convocatoria que no existe");
        }
        entity.setUpdatedAt(LocalDateTime.now());
        return monitoringRequestRepository.save(entity);
    }

    @Override
    public void delete(MonitoringRequest entity) throws Exception {
        // Solo se puede eliminar si no tiene postulaciones o si está cancelada
        Long count = monitorApplicationRepository.countByMonitoringRequest(entity);
        if (count > 0 && entity.getStatus() != RequestStatus.CANCELADA) {
            throw new Exception("No se puede eliminar una convocatoria con postulaciones");
        }
        monitoringRequestRepository.delete(entity);
    }

    @Override
    public void deleteById(Long id) throws Exception {
        Optional<MonitoringRequest> request = monitoringRequestRepository.findById(id);
        if (request.isEmpty()) {
            throw new Exception("Convocatoria no encontrada");
        }
        delete(request.get());
    }

    @Override
    public void validate(MonitoringRequest entity) throws Exception {
        if (entity.getProfessor() == null) {
            throw new Exception("El profesor es obligatorio");
        }
        if (entity.getCourse() == null) {
            throw new Exception("El curso es obligatorio");
        }
        if (entity.getRequestedHours() == null || entity.getRequestedHours() <= 0) {
            throw new Exception("Las horas solicitadas deben ser mayores a 0");
        }
        if (entity.getJustification() == null || entity.getJustification().trim().isEmpty()) {
            throw new Exception("La justificación es obligatoria");
        }
    }

    @Override
    public Long count() {
        return monitoringRequestRepository.count();
    }
}

