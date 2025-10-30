package com.pdg.sigma.repository;

import com.pdg.sigma.domain.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repositorio para MonitoringRequest (Convocatorias de Monitoría)
 */
@Repository
public interface MonitoringRequestRepository extends JpaRepository<MonitoringRequest, Long> {
    
    /**
     * Busca todas las convocatorias de un profesor
     */
    List<MonitoringRequest> findByProfessor(Professor professor);
    
    /**
     * Busca convocatorias por ID de profesor
     */
    List<MonitoringRequest> findByProfessorId(String professorId);
    
    /**
     * Busca todas las convocatorias de una facultad
     */
    List<MonitoringRequest> findBySchool(School school);
    
    /**
     * Busca todas las convocatorias de un programa
     */
    List<MonitoringRequest> findByProgram(Program program);
    
    /**
     * Busca todas las convocatorias de un curso
     */
    List<MonitoringRequest> findByCourse(Course course);
    
    /**
     * Busca convocatorias por estado
     */
    List<MonitoringRequest> findByStatus(RequestStatus status);
    
    /**
     * Busca convocatorias por profesor y estado
     */
    List<MonitoringRequest> findByProfessorAndStatus(Professor professor, RequestStatus status);
    
    /**
     * Busca convocatorias por profesor y semestre
     */
    List<MonitoringRequest> findByProfessorAndSemester(Professor professor, String semester);
    
    /**
     * Busca convocatorias por programa y semestre
     */
    List<MonitoringRequest> findByProgramAndSemester(Program program, String semester);
    
    /**
     * Busca si existe una convocatoria para el mismo profesor, curso y semestre
     */
    Optional<MonitoringRequest> findByProfessorAndCourseAndSemester(Professor professor, Course course, String semester);
    
    /**
     * Busca convocatorias abiertas (estado CONVOCATORIA_ABIERTA)
     */
    @Query("SELECT mr FROM MonitoringRequest mr WHERE mr.status = 'CONVOCATORIA_ABIERTA'")
    List<MonitoringRequest> findAllOpenConvocatorias();
    
    /**
     * Busca convocatorias abiertas por programa (para que los estudiantes vean)
     */
    @Query("SELECT mr FROM MonitoringRequest mr WHERE mr.status = 'CONVOCATORIA_ABIERTA' AND mr.program.id = :programId")
    List<MonitoringRequest> findOpenConvocatoriasByProgram(@Param("programId") Integer programId);
    
    /**
     * Busca convocatorias pendientes de aprobación del jefe de departamento
     * (estado PENDIENTE_APROBACION)
     */
    @Query("SELECT mr FROM MonitoringRequest mr WHERE mr.status = 'PENDIENTE_APROBACION'")
    List<MonitoringRequest> findAllPendingApproval();
    
    /**
     * Busca convocatorias pendientes de aprobación por programa
     * (Para que el jefe de departamento vea solo las de su departamento)
     */
    @Query("SELECT mr FROM MonitoringRequest mr WHERE mr.status = 'PENDIENTE_APROBACION' AND mr.program.id = :programId")
    List<MonitoringRequest> findPendingApprovalByProgram(@Param("programId") Integer programId);
    
    /**
     * Cuenta convocatorias por profesor y semestre
     */
    Long countByProfessorAndSemester(Professor professor, String semester);
    
    /**
     * Cuenta convocatorias abiertas por programa y semestre
     */
    @Query("SELECT COUNT(mr) FROM MonitoringRequest mr WHERE mr.program.id = :programId " +
           "AND mr.semester = :semester AND mr.status = 'CONVOCATORIA_ABIERTA'")
    Long countOpenConvocatoriasByProgramAndSemester(@Param("programId") Integer programId, 
                                                     @Param("semester") String semester);
}

