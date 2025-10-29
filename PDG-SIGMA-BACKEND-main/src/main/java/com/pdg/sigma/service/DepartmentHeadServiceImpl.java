package com.pdg.sigma.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import com.pdg.sigma.domain.*;
import com.pdg.sigma.dto.DepartmentHeadDTO;
import com.pdg.sigma.dto.PendingApplicationDTO;
import com.pdg.sigma.repository.CourseRepository;
import com.pdg.sigma.repository.ProfessorRepository;
import com.pdg.sigma.repository.HeadProgramRepository;
import com.pdg.sigma.repository.MonitoringRepository;
import com.pdg.sigma.repository.MonitoringMonitorRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.pdg.sigma.repository.DepartmentHeadRepository;
import com.pdg.sigma.repository.CourseProfessorRepository;

@Service
public class DepartmentHeadServiceImpl implements DepartmentHeadService {

    @Autowired
    private DepartmentHeadRepository departmentHeadRepository;

    @Autowired
    private HeadProgramRepository headProgramRepository;

    @Autowired
    private CourseProfessorRepository courseProfessorRepository;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private MonitoringRepository monitoringRepository;

    @Autowired
    private MonitoringMonitorRepository monitoringMonitorRepository;

    @Autowired
    private ProfessorRepository professorRepository;

    @Override
    public List<DepartmentHead> findAll() {
        return departmentHeadRepository.findAll();
    }

    @Override
    public Optional<DepartmentHead> findById(Integer id) {
        return departmentHeadRepository.findById(id.toString());
    }

    @Override
    public DepartmentHead save(DepartmentHead departmentHead) {
        return departmentHeadRepository.save(departmentHead);
    }

    @Override
    public void deleteById(Integer id) {
        departmentHeadRepository.deleteById(id.toString());
    }

    @Override
    public DepartmentHead update(DepartmentHead entity) throws Exception {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void delete(DepartmentHead entity) throws Exception {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void validate(DepartmentHead entity) throws Exception {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Long count() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public DepartmentHeadDTO getProfile(String id)throws Exception{
        Optional<DepartmentHead> departmentHead = departmentHeadRepository.findById(id);
        if(departmentHead.isPresent()){
            List<HeadProgram> list = headProgramRepository.findByDepartmentHeadId(id); //HeadProfessor is table between department head and program who it's from
            HeadProgram headProfessor= list.get(0);
            Program program = headProfessor.getProgram();
            School school = program.getSchool();

            return new DepartmentHeadDTO(school.getName(), program.getName(),"Jefe de Departamento", departmentHead.get().getName());
        }
        else
            throw new Exception("No existe jefe con este id");

    }
    
    @Override
    public List<Professor> getProfessorsByDepartmentHead(String departmentHeadId) {
        List<HeadProgram> headPrograms = headProgramRepository.findByDepartmentHeadId(departmentHeadId);

        // Profesores asociados a cursos dentro de los programas del jefe
        List<Professor> scopedProfessors = Collections.emptyList();
        if (!headPrograms.isEmpty()) {
            List<Long> programIds = headPrograms.stream()
                    .map(hp -> hp.getProgram().getId())
                    .collect(Collectors.toList());

            List<Course> courses = courseRepository.findByProgramIdIn(programIds);
            if (!courses.isEmpty()) {
                List<Long> courseIds = courses.stream().map(Course::getId).collect(Collectors.toList());
                scopedProfessors = courseProfessorRepository.findProfessorsByCourseIds(courseIds);
            }
        }

        // Unión con todos los profesores disponibles para asegurar que el jefe pueda seleccionar cualquiera (demo/semillas)
        List<Professor> all = professorRepository.findAll();
        // Evitar duplicados manteniendo el orden: primero los del ámbito, luego el resto
        List<Professor> result = new java.util.ArrayList<>();
        java.util.Set<String> seen = new java.util.HashSet<>();
        for (Professor p : scopedProfessors) {
            if (seen.add(p.getId())) result.add(p);
        }
        for (Professor p : all) {
            if (seen.add(p.getId())) result.add(p);
        }
        return result;
    }

    @Override
    public List<HeadProgram> getProgramsByDepartmentHead(String departmentHeadId) {
        return headProgramRepository.findByDepartmentHeadId(departmentHeadId);
    }

    @Override
    public List<PendingApplicationDTO> getPendingApplications(String departmentHeadId) throws Exception {
        // 1. Obtener los programas del jefe de departamento
        List<HeadProgram> headPrograms = headProgramRepository.findByDepartmentHeadId(departmentHeadId);
        if (headPrograms.isEmpty()) {
            throw new Exception("No se encontraron programas para este jefe de departamento");
        }

        // 2. Obtener todos los cursos de esos programas
        List<Long> programIds = headPrograms.stream()
                .map(hp -> hp.getProgram().getId())
                .collect(Collectors.toList());
        
        List<Course> courses = courseRepository.findByProgramIdIn(programIds);
        if (courses.isEmpty()) {
            return new ArrayList<>();
        }

        // 3. Obtener todas las monitorías de esos cursos
        List<PendingApplicationDTO> pendingApplications = new ArrayList<>();
        
        for (Course course : courses) {
            Optional<Monitoring> monitoringOpt = monitoringRepository.findByCourse(course);
            if (monitoringOpt.isPresent()) {
                Monitoring monitoring = monitoringOpt.get();
                
                // 4. Obtener todas las postulaciones de esta monitoría
                List<MonitoringMonitor> applications = monitoringMonitorRepository.findByMonitoring(monitoring);
                
                for (MonitoringMonitor mm : applications) {
                    // FILTRO: Solo incluir postulaciones que el profesor seleccionó
                    // y que aún pueden ser aprobadas/rechazadas por el jefe
                    String estado = mm.getEstadoSeleccion();
                    if (estado == null || estado.isEmpty() || "no seleccionado".equalsIgnoreCase(estado)) {
                        continue; // Skip postulaciones que el profesor no seleccionó
                    }
                    
                    Monitor monitor = mm.getMonitor();
                    
                    PendingApplicationDTO dto = new PendingApplicationDTO();
                    dto.setId(mm.getId());
                    dto.setMonitoringId(monitoring.getId());
                    dto.setCourseName(course.getName());
                    dto.setProfessorName(monitoring.getProfessor().getName());
                    dto.setMonitorName(monitor.getName() + " " + monitor.getLastName());
                    dto.setMonitorCode(monitor.getCode());
                    dto.setMonitorEmail(monitor.getEmail());
                    dto.setGradeAverage(monitor.getGradeAverage());
                    dto.setGradeCourse(monitor.getGradeCourse());
                    dto.setSemester(monitor.getSemester());
                    dto.setEstadoSeleccion(mm.getEstadoSeleccion());
                    dto.setComentarioDecision(mm.getComentarioDecision());
                    dto.setFechaDecision(mm.getFechaDecision());
                    dto.setDecididoPor(mm.getDecididoPor());
                    dto.setProgramName(course.getProgram().getName());
                    dto.setSchoolName(course.getProgram().getSchool().getName());
                    
                    pendingApplications.add(dto);
                }
            }
        }
        
        return pendingApplications;
    }
}
