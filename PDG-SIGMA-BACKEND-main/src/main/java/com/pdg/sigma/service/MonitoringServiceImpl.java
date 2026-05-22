package com.pdg.sigma.service;

import com.pdg.sigma.domain.*;
import com.pdg.sigma.dto.*;
import com.pdg.sigma.repository.*;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.text.Normalizer;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Month;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.*;
import java.util.function.Function;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

@Service
public class MonitoringServiceImpl implements MonitoringService{

    @Autowired
    private MonitoringRepository monitoringRepository;

    @Autowired
    private SchoolRepository schoolRepository;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private ProgramRepository programRepository;

    @Autowired
    private ProfessorRepository professorRepository;

    @Autowired
    private MonitoringMonitorRepository monitoringMonitorRepository;

    @Autowired
    private MonitorRepository monitorRepository;

    @Autowired
    private HeadProgramRepository headProgramRepository;

    @Autowired
    private ActivityRepository activityRepository;

    @Autowired
    private DepartmentHeadRepository departmentHeadRepository;
    @Autowired
    private CourseProfessorRepository courseProfessorRepository;

    @Autowired
    private AttendanceRepository attendanceRepository;

    @Autowired
    private DepartmentBudgetRepository departmentBudgetRepository;

    @Autowired
    private StudentCourseRepository studentCourseRepository;

    @Autowired
    private MonitoringRequestRepository monitoringRequestRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    /**
     * Método auxiliar para verificar si una monitoría ya tiene un monitor aprobado
     * @param monitoring La monitoría a verificar
     * @return true si tiene un monitor aprobado, false en caso contrario
     */
    private boolean hasApprovedMonitor(Monitoring monitoring) {
        List<MonitoringMonitor> applications = monitoringMonitorRepository.findByMonitoring(monitoring);
        return applications.stream()
                .anyMatch(mm -> "aprobado".equalsIgnoreCase(mm.getEstadoSeleccion()));
    }

    private void syncMonitoringSequenceIfNeeded() {
        try {
            String sequence = jdbcTemplate.queryForObject(
                    "select pg_get_serial_sequence('sigma.monitoring','id')",
                    String.class
            );
            if (sequence == null || sequence.isBlank()) {
                return;
            }
            jdbcTemplate.queryForObject(
                    "select setval(?::regclass, coalesce((select max(id) from sigma.monitoring), 0))",
                    Long.class,
                    sequence
            );
        } catch (Exception ex) {
            System.out.println("No fue posible sincronizar la secuencia de monitoring: " + ex.getMessage());
        }
    }

    @Override
    public List<Monitoring> findAll() {
        Date now = new Date();

        return monitoringRepository.findAll().stream()
                // FILTRO: Excluir monitorías que ya tienen un monitor aprobado
                .filter(monitoring -> !hasApprovedMonitor(monitoring))
                .sorted(Comparator.comparing(m -> {
                    Date start = m.getStart();
                    Date end = m.getFinish();

                    if ((start.before(now) || start.equals(now)) && (end.after(now) || end.equals(now))) {
                        return 0;
                    } else if (start.after(now) && end.after(now)) {
                        return 1;
                    } else {
                        return 2;
                    }
                }))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<Monitoring> findAllByProfessor(String id) {
        Date now = new Date();
        Optional<Professor> professor = professorRepository.findById(id);
        if (professor.isPresent()) {
            System.out.println("inside findAllByProfessor");
            return monitoringRepository.findByProfessor(professor.get()).stream()
                    .peek(m -> {
                        // Forzar carga de assignedMonitor para el nuevo flujo HU-010
                        if (m.getAssignedMonitor() != null) {
                            m.getAssignedMonitor().getName(); // trigger lazy load
                        }
                    })
                    .sorted(Comparator.comparing(m -> {
                        Date start = m.getStart();
                        Date end = m.getFinish();

                        if ((start.before(now) || start.equals(now)) && (end.after(now) || end.equals(now))) {
                            return 0;
                        } else if (start.after(now) && end.after(now)) {
                            return 1;
                        } else {
                            return 2;
                        }
                    }))
                    .collect(Collectors.toList());
        }
        return null;
    }

    @Override
    public Optional<Monitoring> findById(Long aLong) {
        return monitoringRepository.findById(aLong);
    }

    @Override
    public Monitoring save(Monitoring entity) throws Exception {
        return null;
    }

    @Override
    public Monitoring save(MonitoringDTO entity) throws Exception{
        if (entity == null) {
            throw new Exception("Datos de monitoria invalidos");
        }
        if (entity.getSchoolName() == null || entity.getSchoolName().trim().isEmpty()) {
            throw new Exception("No existe una facultad con este nombre");
        }
        if (entity.getProgramName() == null || entity.getProgramName().trim().isEmpty()) {
            throw new Exception("No existe un programa con este nombre");
        }
        if (entity.getCourseName() == null || entity.getCourseName().trim().isEmpty()) {
            throw new Exception("No existe un curso con este nombre");
        }
        if (entity.getProfessorId() == null || entity.getProfessorId().trim().isEmpty()) {
            throw new Exception("El profesor no está registrado");
        }
        if (entity.getSemester() == null || entity.getSemester().trim().isEmpty()) {
            throw new Exception("El periodo es obligatorio");
        }
        if (entity.getStart() == null || entity.getFinish() == null) {
            throw new Exception("Las fechas de inicio y fin son obligatorias");
        }

        School school = findSchoolByName(entity.getSchoolName());
        if (school == null) {
            throw new Exception("No existe una facultad con este nombre");
        }
        Program program = findProgramByName(entity.getProgramName(), school);
        if (program == null) {
            throw new Exception("No existe un programa con este nombre");
        }
        Course course = findCourseByName(entity.getCourseName(), program);
        if (course == null) {
            throw new Exception("No existe un curso con este nombre");
        }
        Professor professor = professorRepository.findById(entity.getProfessorId())
                .orElseThrow(() -> new Exception("El profesor no está registrado"));

        boolean exists = monitoringRepository
                .findByProfessorAndCourseAndSemester(professor, course, entity.getSemester())
                .isPresent();
        if (exists) {
            throw new Exception("Ya existe una monitoria para este curso en el periodo " + entity.getSemester());
        }

        syncMonitoringSequenceIfNeeded();

        Monitoring newMonitoring = new Monitoring(
                school,
                program,
                course,
                entity.getStart(),
                entity.getFinish(),
                4.5,
                4.5,
                entity.getSemester(),
                professor
        );
        newMonitoring.setEstimatedHours(entity.getEstimatedHours());
        newMonitoring.setHourlyRate(entity.getHourlyRate());
        monitoringRepository.save(newMonitoring);
        return newMonitoring;
    }
    public List<Monitoring> findBySchool(MonitoringDTO monitoringDTO){ //programName = nombre elemento a buscar, courseName = state o estado
        if(!monitoringDTO.getProgramName().isBlank()){
            School entity = schoolRepository.findByName(monitoringDTO.getProgramName()).get();
            System.out.println("Facultad: " + entity.getName());
            List<Monitoring> monitoring = monitoringRepository.findBySchool(entity);
            Date currentDate = new Date();
            if(monitoringDTO.getCourseName().equalsIgnoreCase("Activo") || monitoringDTO.getCourseName().isBlank()){
                List<Monitoring> temp = new ArrayList<>();
                for(Monitoring element: monitoring){
                    // Filtrar monitorías sin monitor aprobado Y activas
                    if(!hasApprovedMonitor(element) && (element.getStart().before(currentDate) ||element.getStart().equals(currentDate)) && (element.getFinish().after(currentDate) || element.getFinish().equals(currentDate))){
                        temp.add(element);
                    }
                }
                return temp;
            }
            else if(monitoringDTO.getCourseName().equalsIgnoreCase("Inactivo")){
                List<Monitoring> temp = new ArrayList<>();
                for(Monitoring element: monitoring){
                    // Filtrar monitorías sin monitor aprobado Y inactivas (futuras)
                    if(!hasApprovedMonitor(element) && element.getStart().after(currentDate) && element.getFinish().after(currentDate)){
                        temp.add(element);
                    }
                }
                return temp;
            }

            // Vencidas - mostrar todas (son históricas, no se puede postular)
            List<Monitoring> temp = new ArrayList<>();
            for(Monitoring element: monitoring){
                if(element.getStart().before(currentDate) && element.getFinish().before(currentDate)){
                    temp.add(element);
                }
            }
            return temp;

        }else{
            Date currentDate = new Date();
            
            // Para monitorías vencidas, obtener TODAS (sin filtro de monitor aprobado)
            // Para activas/inactivas, usar findAll() que filtra monitorías con monitor aprobado
            if(monitoringDTO.getCourseName().equalsIgnoreCase("Vencido")){
                List<Monitoring> allMonitoring = monitoringRepository.findAll();
                List<Monitoring> temp = new ArrayList<>();
                for(Monitoring element: allMonitoring){
                    if(element.getStart().before(currentDate) && element.getFinish().before(currentDate)){
                        temp.add(element);
                    }
                }
                return temp;
            }
            
            // Para activas e inactivas, usar findAll() filtrado
            List<Monitoring> monitoring = this.findAll();
            if(monitoringDTO.getCourseName().equalsIgnoreCase("Activo") || monitoringDTO.getCourseName().isBlank()){
                List<Monitoring> temp = new ArrayList<>();
                for(Monitoring element: monitoring){
                    if(element.getStart().before(currentDate) && element.getFinish().after(currentDate)){
                        temp.add(element);
                    }
                }
                return temp;
            }
            else if(monitoringDTO.getCourseName().equalsIgnoreCase("Inactivo")){
                List<Monitoring> temp = new ArrayList<>();
                for(Monitoring element: monitoring){
                    if(element.getStart().after(currentDate) && element.getFinish().after(currentDate)){
                        temp.add(element);
                    }
                }
                return temp;
            }

            // Default: retornar todas las activas
            return monitoring;
        }

    }

    public List<Monitoring> findByProgram(MonitoringDTO monitoringDTO) {//programName = nombre elemento a buscar, courseName = state o estado
        Program entity = programRepository.findByName(monitoringDTO.getProgramName()).get();
        List<Monitoring> monitoring = monitoringRepository.findByProgram(entity);

        Date currentDate = new Date();
        if(monitoringDTO.getCourseName().equalsIgnoreCase("Activo") || monitoringDTO.getCourseName().isBlank()){
            List<Monitoring> temp = new ArrayList<>();
            for(Monitoring element: monitoring){
                // Filtrar monitorías sin monitor aprobado Y activas
                if(!hasApprovedMonitor(element) && element.getStart().before(currentDate) && element.getFinish().after(currentDate)){
                    temp.add(element);
                }
            }
            return temp;
        }
        else if(monitoringDTO.getCourseName().equalsIgnoreCase("Inactivo")){
            List<Monitoring> temp = new ArrayList<>();
            for(Monitoring element: monitoring){
                // Filtrar monitorías sin monitor aprobado Y inactivas (futuras)
                if(!hasApprovedMonitor(element) && element.getStart().after(currentDate) && element.getFinish().after(currentDate)){
                    temp.add(element);
                }
            }
            return temp;
        }

        // Vencidas - mostrar todas (son históricas, no se puede postular)
        List<Monitoring> temp = new ArrayList<>();
        for(Monitoring element: monitoring){
            if(element.getStart().before(currentDate) && element.getFinish().before(currentDate)){
                temp.add(element);
            }
        }
        return temp;
    }

    public List<Monitoring> findByCourse(MonitoringDTO monitoringDTO) {//programName = nombre elemento a buscar, courseName = state o estado
        Course entity = courseRepository.findByName(monitoringDTO.getProgramName()).get();
        Optional<Monitoring> monitoring = monitoringRepository.findByCourse(entity);
        Date currentDate = new Date();
        if(monitoringDTO.getCourseName().equalsIgnoreCase("Activo") || monitoringDTO.getCourseName().isBlank()){
            List<Monitoring> temp = new ArrayList<>();
            for(Monitoring element: monitoring.stream().toList()){
                // Filtrar monitorías sin monitor aprobado Y activas
                if(!hasApprovedMonitor(element) && element.getStart().before(currentDate) && element.getFinish().after(currentDate)){
                    temp.add(element);
                }
            }
            return temp;
        }
        else if(monitoringDTO.getCourseName().equalsIgnoreCase("Inactivo")){
            List<Monitoring> temp = new ArrayList<>();
            for(Monitoring element: monitoring.stream().toList()){
                // Filtrar monitorías sin monitor aprobado Y inactivas (futuras)
                if(!hasApprovedMonitor(element) && element.getStart().after(currentDate) && element.getFinish().after(currentDate)){
                    temp.add(element);
                }
            }
            return temp;
        }

        // Vencidas - mostrar todas (son históricas, no se puede postular)
        List<Monitoring> temp = new ArrayList<>();
        for(Monitoring element: monitoring.stream().toList()){
            if(element.getStart().before(currentDate) && element.getFinish().before(currentDate)){
                temp.add(element);
            }
        }
        return temp;
    }
    @Override
    public Monitoring update(Monitoring entity) throws Exception {
        return null;
    }

    @Override
    public void delete(Monitoring entity) throws Exception {

    }

    @Override
    public void deleteById(Long aLong) throws Exception {

    }

    @Override
    public void validate(Monitoring entity) throws Exception {

    }

    @Override
    public Long count() {
        return null;
    }

    /**
     * Actualiza horas estimadas y/o valor hora de una monitoría respetando el presupuesto
     * del programa en el semestre. Si estimatedHours o hourlyRate son null, se preserva el valor actual.
     */
    @Override
    public Monitoring updateMonitoringBudget(Long monitoringId, Integer estimatedHours, Double hourlyRate) throws Exception {
        Monitoring monitoring = monitoringRepository.findById(monitoringId)
                .orElseThrow(() -> new Exception("Monitoría no encontrada"));

        // Validaciones de valores negativos
        if (estimatedHours != null && estimatedHours < 0) {
            throw new Exception("Las horas no pueden ser negativas");
        }
        if (hourlyRate != null && hourlyRate < 0) {
            throw new Exception("El valor por hora no puede ser negativo");
        }

        // Si se pretende actualizar horas, validar presupuesto disponible
        if (estimatedHours != null) {
            // Buscar presupuesto para el programa y semestre
            Program program = monitoring.getProgram();
            String semester = monitoring.getSemester();
            var budgetOpt = departmentBudgetRepository.findByProgramAndSemester(program, semester);

            if (budgetOpt.isPresent()) {
                int totalHours = budgetOpt.get().getTotalHours();

                // Horas usadas por otras monitorías del mismo programa/semestre (excluyendo la actual)
                int usedByOthers = monitoringRepository.findByProgram(program).stream()
                        .filter(m -> semester.equals(m.getSemester()))
                        .filter(m -> !Objects.equals(m.getId(), monitoring.getId()))
                        .map(m -> m.getEstimatedHours() == null ? 0 : m.getEstimatedHours())
                        .reduce(0, Integer::sum);

                int available = Math.max(0, totalHours - usedByOthers);
                if (estimatedHours > available) {
                    // El mensaje debe contener estas frases para las aserciones del test
                    throw new Exception("No se pueden asignar más horas de las disponibles. Disponibles: " + available);
                }
            }
            // Si no hay presupuesto configurado, permitir la actualización sin restricción para no bloquear el flujo.
        }

        // Aplicar cambios solicitados
        if (estimatedHours != null) monitoring.setEstimatedHours(estimatedHours);
        if (hourlyRate != null) monitoring.setHourlyRate(hourlyRate);

        return monitoringRepository.save(monitoring);
    }

    public List<MonitoringDTO> getByProfessor(String id) throws Exception{
        Optional<Professor> professor = professorRepository.findById(id);
        if(professor.isPresent()){
            List<Monitoring> monitorings = monitoringRepository.findByProfessor(professor.get());
            List<MonitoringDTO> monitoringDTOs = new ArrayList<>();

            if(!monitorings.isEmpty()){
                for(Monitoring monitoring:monitorings){
                    List<MonitoringMonitor> list = monitoringMonitorRepository.findByMonitoring(monitoring);
                    String name = "";

                    if(!list.isEmpty()){
                        // Filtrar solo monitores con estado "seleccionado" o "aprobado"
                        List<String> monitorNames = new ArrayList<>();
                        for(MonitoringMonitor monitoringMonitor:list){
                            if("seleccionado".equalsIgnoreCase(monitoringMonitor.getEstadoSeleccion()) || 
                               "aprobado".equalsIgnoreCase(monitoringMonitor.getEstadoSeleccion())){
                                String fullName = monitoringMonitor.getMonitor().getName() + " " + 
                                                 monitoringMonitor.getMonitor().getLastName();
                                // Evitar duplicados
                                if(!monitorNames.contains(fullName)){
                                    monitorNames.add(fullName);
                                }
                            }
                        }
                        
                        // Construir string con los nombres
                        if(!monitorNames.isEmpty()){
                            name = String.join(", ", monitorNames);
                        } else {
                            name = "N/A";
                        }
                        
                        monitoringDTOs.add(new MonitoringDTO(monitoring.getId(), monitoring.getCourse().getName(), monitoring.getStart(), monitoring.getFinish(), monitoring.getSemester(), name));
                    }
                    else{
                        monitoringDTOs.add(new MonitoringDTO(monitoring.getId(), monitoring.getCourse().getName(), monitoring.getStart(), monitoring.getFinish(), monitoring.getSemester(), "N/A"));
                    }
                }

                return monitoringDTOs;
            }
            else
                throw new Exception("No tiene monitorias creadas");
        }
        else
            throw new Exception("No existe un profesor con este ID");
    }

    public List<MonitoringDTO> getByMonitor(String id) throws Exception{
        Optional<Monitor> monitor = monitorRepository.findByIdMonitor(id);
        if(monitor.isPresent()){
            List<MonitoringMonitor> monitoringMonitors = monitoringMonitorRepository.findByMonitor(monitor.get());
            List<Monitoring> monitorings = new ArrayList<>();
            for(MonitoringMonitor monitoringMonitor:monitoringMonitors){
                monitorings.add(monitoringMonitor.getMonitoring());
            }
            List<MonitoringDTO> monitoringDTOs = new ArrayList<>();

            if(!monitorings.isEmpty()){
                for(Monitoring monitoring:monitorings){
                    monitoringDTOs.add(new MonitoringDTO(monitoring.getId(), monitoring.getCourse().getName(), monitoring.getStart(), monitoring.getFinish(), monitoring.getSemester(), monitoring.getProfessor().getName()));
                }
                System.out.println(monitoringDTOs.get(0).getMonitor());

                return monitoringDTOs;
            }
            else
                throw new Exception("No tiene monitorias creadas");
        }
        else
            throw new Exception("No existe un monitor con este ID");
    }

    public List<ReportDTO>getReportMonitors(String idProfessor, String role) throws Exception{
        if(role.equalsIgnoreCase("professor")){
            Optional<Professor> professor = professorRepository.findById(idProfessor);
            if(professor.isEmpty()){
                throw new Exception("No hay un profesor con este Id");
            }
            List<Monitoring> monitorings = monitoringRepository.findByProfessor(professor.get());

            if(monitorings.isEmpty()){
                return new ArrayList<>();
            }
            List<MonitoringMonitor> monitors = new ArrayList<>();
            for(Monitoring monitoring:monitorings){
                monitors.addAll(monitoringMonitorRepository.findByMonitoring(monitoring));
            }
            if(monitors.isEmpty()){
                return new ArrayList<>();
            }

            List<ReportDTO> reportDTOList = new ArrayList<>();
            for(MonitoringMonitor monitor:monitors){
                List<Activity> activities = filterAssigned(activityRepository.findByMonitorAndRoleResponsable(monitor.getMonitor(), "M"),
                        activityRepository.findByMonitorAndRoleCreator(monitor.getMonitor(), "M"));

                ReportDTO reportDTO = new ReportDTO(0,0,0);
                if(!activities.isEmpty()){
                    for(Activity activity:activities){
                        if(activity.getMonitoring().equals(monitor.getMonitoring())){
                            if(activity.getState().equals(StateActivity.PENDIENTE))
                                reportDTO.setPending(reportDTO.getPending()+1);

                            if(activity.getState().equals(StateActivity.COMPLETADO))
                                reportDTO.setCompleted(reportDTO.getCompleted()+1);

                            if(activity.getState().equals(StateActivity.COMPLETADOT))
                                reportDTO.setLate(reportDTO.getLate()+1);
                        }
                    }
                    reportDTO.setName(monitor.getMonitor().getName());
                    reportDTO.setCourse(monitor.getMonitoring().getCourse().getName());
                    reportDTO.setProfessor(professor.get().getName());
                    reportDTO.setSemester(monitor.getMonitoring().getSemester());
                    reportDTO.setProgram(monitor.getMonitoring().getCourse().getProgram().getName());
                    reportDTO.setIdProfessor(professor.get().getId());
                    String[] nameCourse = monitor.getMonitoring().getCourse().getName().split(" ");
                    if(nameCourse.length>2){
                        reportDTO.setNameAndCourse(monitor.getMonitor().getName()+" - "+nameCourse[0]+" "+nameCourse[1]+"...");
                    }else{
                        reportDTO.setNameAndCourse(monitor.getMonitor().getName()+" - "+monitor.getMonitoring().getCourse().getName());
                    }
                    reportDTOList.add(reportDTO);
                }
            }
            return reportDTOList;
        }
        Optional<DepartmentHead> hd = departmentHeadRepository.findById(idProfessor);
        if(hd.isEmpty()){
            throw new Exception("No existe un jefe con este Id");
        }
        List<HeadProgram> hp = headProgramRepository.findByDepartmentHeadId(hd.get().getId());

        if(hp.isEmpty()){
            throw new Exception("No existe un programa al que este asociado con este Id");
        }

        List<Course> courses = new ArrayList<>();
        for (HeadProgram headProgram : hp) {
            courses.addAll(courseRepository.findByProgram(headProgram.getProgram()));
        }
        if(courses.isEmpty()){
            throw new Exception("No existe un cursos con este programa");
        }

        List<Course> uniqueCourses = new ArrayList<>();
        Set<Long> seenCourseIds = new HashSet<>();
        for (Course course : courses) {
            if (course != null && course.getId() != null && seenCourseIds.add(course.getId())) {
                uniqueCourses.add(course);
                System.out.println(course.getId());
            }
        }
        List<ReportDTO> reportDTOList = new ArrayList<>();
        for(Course course:uniqueCourses){
            List<CourseProfessor> courseProfessors = courseProfessorRepository.findByCourseId(course.getId());
            if(courseProfessors.isEmpty()){
                continue;
            }
            System.out.println("Clases "+ courseProfessors.get(0).getCourse().getName());
            for(CourseProfessor courseProfessor:courseProfessors){
                Professor professor = courseProfessor.getProfessor();
                System.out.println(professor.getName());
                List<Monitoring> monitorings = monitoringRepository.findByProfessor(professor);

                if(monitorings.isEmpty()){
                    continue;
                }
                List<MonitoringMonitor> monitors = new ArrayList<>();
                for(Monitoring monitoring:monitorings){
                    if(monitoring.getCourse().equals(courseProfessor.getCourse())){
                        monitors.addAll(monitoringMonitorRepository.findByMonitoring(monitoring));
                    }
                }
                if( monitors.isEmpty()){
                    continue;
                }
                for(MonitoringMonitor monitoring:monitors){
                    System.out.println("Monitorias "+monitoring.getMonitoring().getCourse().getName());
                }


                for(MonitoringMonitor monitor:monitors){
                    List<Activity> activities = filterAssigned(activityRepository.findByMonitorAndRoleResponsable(monitor.getMonitor(), "M"),
                            activityRepository.findByMonitorAndRoleCreator(monitor.getMonitor(), "M"));

                    ReportDTO reportDTO = new ReportDTO(0,0,0);
                    if(!activities.isEmpty()){
                        for(Activity activity:activities){
                            if(activity.getMonitoring().equals(monitor.getMonitoring())){
                                if(activity.getState().equals(StateActivity.PENDIENTE))
                                    reportDTO.setPending(reportDTO.getPending()+1);

                                if(activity.getState().equals(StateActivity.COMPLETADO))
                                    reportDTO.setCompleted(reportDTO.getCompleted()+1);

                                if(activity.getState().equals(StateActivity.COMPLETADOT))
                                    reportDTO.setLate(reportDTO.getLate()+1);
                            }
                        }
                        reportDTO.setName(monitor.getMonitor().getName());
                        reportDTO.setCourse(monitor.getMonitoring().getCourse().getName());
                        reportDTO.setProfessor(professor.getName());
                        reportDTO.setIdProfessor(professor.getId());
                        reportDTO.setSemester(monitor.getMonitoring().getSemester());
                        reportDTO.setProgram(monitor.getMonitoring().getCourse().getProgram().getName());
                        String[] nameCourse = monitor.getMonitoring().getCourse().getName().split(" ");
                        if(nameCourse.length>2){
                            reportDTO.setNameAndCourse(monitor.getMonitor().getName()+" - "+nameCourse[0]+" "+nameCourse[1]+"...");
                        }else{
                            reportDTO.setNameAndCourse(monitor.getMonitor().getName()+" - "+monitor.getMonitoring().getCourse().getName());
                        }
                        reportDTOList.add(reportDTO);
                    }
                }

            }
        }
        if(!reportDTOList.isEmpty()){
            return reportDTOList;
        }else
            throw new Exception("No hay reportes por mostrar");
    }

    public List<ReportDTO> getProfessorReport(String idProfessor)throws Exception {
        Optional<Professor> professor = professorRepository.findById(idProfessor);
        if(professor.isPresent()) {
            List<Monitoring> monitorings = monitoringRepository.findByProfessor(professor.get());

            if (monitorings.isEmpty()) {
                throw new Exception("No hay monitorías creadas");
            }
            List<Activity> activitiesAssigned =filterAssigned(activityRepository.findByProfessorAndRoleResponsable(professor.get(), "P"),
                    activityRepository.findByProfessorAndRoleCreator(professor.get(), "P"));

            List<ReportDTO> reportProfessor = new ArrayList<>();
            for (Monitoring monitoring : monitorings) {
                ReportDTO reportDTO = new ReportDTO(0, 0, 0);
                for (Activity activity : activitiesAssigned) {
                    if (monitoring.equals(activity.getMonitoring())) {
                        switch (activity.getState()) {
                            case StateActivity.PENDIENTE:
                                reportDTO.setPending(reportDTO.getPending() + 1);
                                break;

                            case StateActivity.COMPLETADO:
                                reportDTO.setCompleted(reportDTO.getCompleted() + 1);
                                break;

                            case StateActivity.COMPLETADOT:
                                reportDTO.setLate(reportDTO.getLate() + 1);
                                break;

                            default:
                                throw new Exception("Estado incorrecto");
                        }

                    }
                }

                reportDTO.setName(professor.get().getName());
                reportDTO.setIdProfessor(professor.get().getId());
                reportDTO.setCourse(monitoring.getCourse().getName());
                reportDTO.setProgram(monitoring.getCourse().getProgram().getName());
                reportProfessor.add(reportDTO);
            }
            return reportProfessor;

        }
        else
            throw new Exception("No existe professor con este id");
    }

    public List<Activity> filterAssigned(List<Activity> assigned, List<Activity> creator){
        List<Activity> result = new ArrayList<>(assigned);
        result.removeIf(creator::contains);
        return result;
    }


    public String processListMonitor(MultipartFile file, String professorId) throws Exception {

        List<MonitoringDTO> toCreate = new ArrayList<>();
        List<String> createdCourses = new ArrayList<>();
        List<String> omittedCourses = new ArrayList<>();

        String filename = file.getOriginalFilename() == null ? "" : file.getOriginalFilename().toLowerCase();
        String contentType = file.getContentType() == null ? "" : file.getContentType();
        boolean isCsv = filename.endsWith(".csv") || contentType.contains("text/csv") || contentType.contains("application/vnd.ms-excel");

        List<List<String>> rows = new ArrayList<>();

        if (isCsv) {
            String text = new String(file.getBytes(), java.nio.charset.StandardCharsets.UTF_8);
            String[] lines = text.split("\r?\n");
            if (lines.length == 0) {
                throw new Exception("Incompatibilidad con alguno de los campos del archivo");
            }
            // header
            String headerLine = lines[0];
            String delimiter = ",";
            String[] header = headerLine.split(delimiter, -1);
            if (header.length < 2 && headerLine.contains(";")) {
                delimiter = ";";
                header = headerLine.split(delimiter, -1);
            }
            List<String> columnsName = new ArrayList<>();
            for (int i = 0; i < header.length; i++) {
                String value = header[i].trim();
                if (i == 0) {
                    value = value.replace("\uFEFF", "");
                }
                columnsName.add(value);
            }
            while (!columnsName.isEmpty() && columnsName.get(columnsName.size() - 1).trim().isEmpty()) {
                columnsName.remove(columnsName.size() - 1);
            }
            if (columnsName.size() < 10) {
                System.out.println("ERROR CSV: columnas insuficientes: " + columnsName.size());
                throw new Exception("Incompatibilidad con alguno de los campos del archivo (se requieren 10 columnas, se encontraron " + columnsName.size() + ")");
            }
            if (columnsName.size() > 10) {
                columnsName = new ArrayList<>(columnsName.subList(0, 10));
            }
            if (!checkColumns(columnsName)) {
                System.out.println("ERROR CSV: nombres de columnas incorrectos: " + columnsName);
                throw new Exception("Incompatibilidad con alguno de los campos del archivo (nombres de columnas incorrectos)");
            }
            for (int r = 1; r < lines.length; r++) {
                if (lines[r].trim().isEmpty()) continue;
                String[] parts = lines[r].split(delimiter, -1);
                if (parts.length < columnsName.size()) {
                    System.out.println("ERROR CSV fila " + r + ": pocas columnas: " + parts.length);
                    throw new Exception("Incompatibilidad con alguno de los campos del archivo (fila " + r + " tiene " + parts.length + " columnas)");
                }
                List<String> row = new ArrayList<>();
                for (int i = 0; i < columnsName.size(); i++) {
                    row.add(parts[i].trim());
                }
                rows.add(row);
            }
            // Build DTOs from rows
            for (int r = 0; r < rows.size(); r++) {
                List<String> row = rows.get(r);
                MonitoringDTO monitoring = new MonitoringDTO(0.0, 0.0);
                for (int i = 0; i < columnsName.size(); i++) {
                    String value = row.get(i);
                    if (value.equals("")) {
                        System.out.println("ERROR CSV fila " + (r+2) + " columna " + i + " (" + columnsName.get(i) + "): valor vacio");
                        throw new Exception("Incompatibilidad con alguno de los campos del archivo (fila " + (r+2) + ", columna '" + columnsName.get(i) + "' vacia)");
                    }
                    Object check = checkValue(i, value, monitoring, columnsName);
                    if (check == null) {
                        System.out.println("ERROR CSV fila " + (r+2) + " columna " + i + " (" + columnsName.get(i) + "): valor '" + value + "' no reconocido");
                        throw new Exception("Incompatibilidad con alguno de los campos del archivo (fila " + (r+2) + ", columna '" + columnsName.get(i) + "': '" + value + "' no valido)");
                    }
                    monitoring = (MonitoringDTO) check;
                }
                validateSemesterAndDates(monitoring);
                toCreate.add(monitoring);
            }
        } else {
            // Excel path
            try (InputStream is = file.getInputStream(); Workbook workbook = new XSSFWorkbook(is)) {
                Sheet sheet = workbook.getSheetAt(0);
                Iterator<Row> rowIterator = sheet.iterator();
                if (!rowIterator.hasNext()) {
                    throw new Exception("Incompatibilidad con alguno de los campos del archivo");
                }
                Row header = rowIterator.next();
                List<String> columnsName = new ArrayList<>();
                short lastCell = header.getLastCellNum();
                for (int i = 0; i < lastCell; i++) {
                    Cell cell = header.getCell(i, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
                    columnsName.add(getCellValue(cell));
                }
                while (!columnsName.isEmpty() && columnsName.get(columnsName.size() - 1).trim().isEmpty()) {
                    columnsName.remove(columnsName.size() - 1);
                }
                if (columnsName.size() < 10) {
                    throw new Exception("Incompatibilidad con alguno de los campos del archivo");
                }
                if (columnsName.size() > 10) {
                    columnsName = new ArrayList<>(columnsName.subList(0, 10));
                }
                if (!checkColumns(columnsName)) {
                    throw new Exception("Incompatibilidad con alguno de los campos del archivo");
                }
                while (rowIterator.hasNext()) {
                    Row row = rowIterator.next();
                    MonitoringDTO monitoring = new MonitoringDTO(0.0, 0.0);
                    for (int i = 0; i < columnsName.size(); i++) {
                        Cell cell = row.getCell(i, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
                        String value = getCellValue(cell);
                        if (value.equals("")) {
                            throw new Exception("Incompatibilidad con alguno de los campos del archivo");
                        }
                        Object check = checkValue(i, value, monitoring, columnsName);
                        if (check == null) {
                            throw new Exception("Incompatibilidad con alguno de los campos del archivo");
                        }
                        monitoring = (MonitoringDTO) check;
                    }
                    validateSemesterAndDates(monitoring);
                    toCreate.add(monitoring);
                }
            }
        }

        Professor professor = professorRepository.findById(professorId)
                .orElseThrow(() -> new Exception("Profesor no encontrado"));

        syncMonitoringSequenceIfNeeded();

        for (MonitoringDTO monitoring : toCreate) {
            // Duplicado para mismo profesor-curso-semestre
            boolean exists = monitoringRepository
                    .findByProfessorAndCourseAndSemester(professor, monitoring.getCourse(), monitoring.getSemester())
                    .isPresent();
            if (exists) {
                omittedCourses.add(monitoring.getCourse().getName() + " ya existía para el semestre " + monitoring.getSemester());
                continue;
            }

            monitoring.setProfessor(professor);
            monitoringRepository.save(new Monitoring(monitoring));
            createdCourses.add(monitoring.getCourse().getName());
        }

        StringBuilder sb = new StringBuilder();
        sb.append("Creadas (").append(createdCourses.size()).append(")");
        if (!createdCourses.isEmpty()) {
            sb.append(": ").append(String.join(", ", createdCourses));
        }
        if (!omittedCourses.isEmpty()) {
            sb.append(". Omitidas (").append(omittedCourses.size()).append("): ")
              .append(String.join(", ", omittedCourses));
        }
        return sb.toString();
    }

    //Method to check values header
    private boolean checkColumns(List<String> columns) {
        if (columns.size() < 10) {
            return false;
        }
        String c0 = normalizeHeaderName(columns.get(0));
        String c1 = normalizeHeaderName(columns.get(1));
        String c2 = normalizeHeaderName(columns.get(2));
        String c3 = normalizeHeaderName(columns.get(3));
        String c4 = normalizeHeaderName(columns.get(4));
        String c5 = normalizeHeaderName(columns.get(5));
        String c6 = normalizeHeaderName(columns.get(6));
        String c7 = normalizeHeaderName(columns.get(7));
        String c8 = normalizeHeaderName(columns.get(8));
        String c9 = normalizeHeaderName(columns.get(9));

        boolean ok = c0.equals("FACULTAD") &&
                c1.equals("PROGRAMA") &&
                c2.equals("CURSO") &&
                c3.equals("FECHA INICIO") &&
                c4.equals("FECHA FINALIZACION") &&
                c5.equals("PERIODO") &&
                c6.equals("PROMEDIO ACUMULADO") &&
                c7.equals("PROMEDIO MATERIA") &&
                c8.equals("HORAS ESTIMADAS") &&
                c9.equals("VALOR HORA");
        if (!ok) {
            System.out.println("DEBUG checkColumns: [" + c0 + "|" + c1 + "|" + c2 + "|" + c3 + "|" + c4 + "|" + c5 + "|" + c6 + "|" + c7 + "|" + c8 + "|" + c9 + "]");
        }
        return ok;
    }

    private String normalizeHeaderName(String value) {
        if (value == null) {
            return "";
        }
        String normalized = Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        normalized = normalized.replaceAll("\\s+", " ").trim().toUpperCase();
        return normalized;
    }

    //Method to check type of value
    private String getCellValue(Cell cell) {
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue().trim();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    SimpleDateFormat formatter = new SimpleDateFormat("dd-MM-yyyy");
                    return formatter.format(cell.getDateCellValue());
                }
                double numericValue = cell.getNumericCellValue();
                if (Math.floor(numericValue) == numericValue) {
                    return String.valueOf((long) numericValue);
                }
                return String.valueOf(numericValue);
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case FORMULA:
                return cell.getCellFormula();
            default:
                return "";
        }
    }

    private Date parseDateFlexible(String value) {
        String[] patterns = new String[] {"dd-MM-yyyy", "dd/MM/yyyy", "yyyy-MM-dd", "yyyy/MM/dd"};
        for (String pattern : patterns) {
            try {
                SimpleDateFormat formatter = new SimpleDateFormat(pattern);
                formatter.setLenient(false);
                return formatter.parse(value);
            } catch (ParseException ignored) {
            }
        }
        return null;
    }

    private String normalizeNumeric(String value) {
        String v = value.trim();
        if (v.contains(",") && v.contains(".")) {
            v = v.replace(".", "").replace(",", ".");
        } else if (v.contains(",")) {
            v = v.replace(",", ".");
        }
        return v;
    }

    private String semesterFromDate(Date date) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH) + 1;
        int semester = month <= 6 ? 1 : 2;
        return year + "-" + semester;
    }

    private String normalizeComparableName(String value) {
        if (value == null) {
            return "";
        }
        String normalized = Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        normalized = normalized.replaceAll("\\s+", " ").trim().toLowerCase();
        return normalized;
    }

    private <T> T findByNormalizedName(List<T> items, String name, Function<T, String> nameGetter) {
        String target = normalizeComparableName(name);
        for (T item : items) {
            String candidate = normalizeComparableName(nameGetter.apply(item));
            if (candidate.equals(target)) {
                return item;
            }
        }
        return null;
    }

    private School findSchoolByName(String name) {
        Optional<School> direct = schoolRepository.findByNameIgnoreCase(name);
        if (direct.isPresent()) {
            return direct.get();
        }
        School fallback = findByNormalizedName(schoolRepository.findAll(), name, School::getName);
        if (fallback == null) {
            System.out.println("DEBUG: Escuela no encontrada: '" + name + "'. Escuelas disponibles: " + schoolRepository.findAll().stream().map(School::getName).toList());
        }
        return fallback;
    }

    private Program findProgramByName(String name, School school) {
        Optional<Program> direct = programRepository.findByNameIgnoreCaseAndSchool(name, school);
        if (direct.isPresent()) {
            return direct.get();
        }
        Program fallback = findByNormalizedName(programRepository.findBySchool(school), name, Program::getName);
        if (fallback == null) {
            System.out.println("DEBUG: Programa no encontrado: '" + name + "' para escuela '" + school.getName() + "'. Programas disponibles: " + programRepository.findBySchool(school).stream().map(Program::getName).toList());
        }
        return fallback;
    }

    private Course findCourseByName(String name, Program program) {
        Optional<Course> direct = courseRepository.findByNameIgnoreCaseAndProgram(name, program);
        if (direct.isPresent()) {
            return direct.get();
        }
        Course fallback = findByNormalizedName(courseRepository.findByProgram(program), name, Course::getName);
        if (fallback == null) {
            System.out.println("DEBUG: Curso no encontrado: '" + name + "' para programa '" + program.getName() + "'. Cursos disponibles: " + courseRepository.findByProgram(program).stream().map(Course::getName).toList());
        }
        return fallback;
    }

    private String stripQuotes(String value) {
        if (value == null) {
            return "";
        }
        String trimmed = value.trim();
        if (trimmed.length() >= 2 && trimmed.startsWith("\"") && trimmed.endsWith("\"")) {
            return trimmed.substring(1, trimmed.length() - 1).trim();
        }
        return trimmed;
    }

    //Method to check value and return object of the value
    private Object checkValue(int column, String value, MonitoringDTO monitoring, List<String> header) throws ParseException {
        String regexSemester = "^\\d{4}-(1|2)$";
        String cleanedValue = stripQuotes(value);

        boolean valid=true;
            switch (column) {
                case 0:
                    School school = findSchoolByName(cleanedValue);
                    if(school != null){
                        monitoring.setSchool(school);
                        return monitoring;
                    }
                    return null;
                case 1:
                    if (monitoring.getSchool() == null) {
                        return null;
                    }
                    Program program = findProgramByName(cleanedValue, monitoring.getSchool());
                    if(program != null){
                        monitoring.setProgram(program);
                        return monitoring;
                    }
                    return null;
                case 2:
                    if (monitoring.getProgram() == null) {
                        return null;
                    }
                    Course course = findCourseByName(cleanedValue, monitoring.getProgram());
                    if(course != null){
                        monitoring.setCourse(course);
                        return monitoring;
                    }
                    return null;

                case 3:
                    Date parsedStart = parseDateFlexible(cleanedValue);
                    if (parsedStart != null) {
                        monitoring.setStart(parsedStart);
                        return monitoring;
                    }
                    return null;
                case 4:
                    Date parsedFinish = parseDateFlexible(cleanedValue);
                    if (parsedFinish != null) {
                        monitoring.setFinish(parsedFinish);
                        return monitoring;
                    }
                    return null;
                case 5:
                    String semesterValue = cleanedValue.trim().replace("/", "-");
                    if(semesterValue.matches(regexSemester)){
                        monitoring.setSemester(semesterValue);
                        return monitoring;
                    }
                    Date semesterDate = parseDateFlexible(cleanedValue);
                    if (semesterDate != null) {
                        monitoring.setSemester(semesterFromDate(semesterDate));
                        return monitoring;
                    }
                    return null;
                case 6:
                    monitoring.setAverageGrade(Double.parseDouble(normalizeNumeric(cleanedValue)));
                    return monitoring;

                case 7:
                    monitoring.setCourseGrade(Double.parseDouble(normalizeNumeric(cleanedValue)));
                    return monitoring;
                case 8:
                    monitoring.setEstimatedHours((int) Math.round(Double.parseDouble(normalizeNumeric(cleanedValue))));
                    return monitoring;
                case 9:
                    monitoring.setHourlyRate(Double.parseDouble(normalizeNumeric(cleanedValue)));
                    return monitoring;
                default:
                    System.out.println("Opción no disponible"); //Temporal mientras se lleva a producción
            }


        return valid;
    }

    private void validateSemesterAndDates(MonitoringDTO monitoring) throws Exception {
        if(!(monitoring.getStart().before(monitoring.getFinish()) || monitoring.getStart().equals(monitoring.getFinish())) &&
                !(monitoring.getStart().after(new Date()) || monitoring.getStart().equals(new Date()))){

            throw new Exception("Incompatibilidad con alguno de los campos del archivo");
        }
        String semesterDraft = monitoring.getSemester();

        Date currentDate = monitoring.getStart();
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(currentDate);

        int currentYear = calendar.get(Calendar.YEAR);
        int currentMonth = calendar.get(Calendar.MONTH) + 1; // 1..12

        String[] parts = semesterDraft.split("-");
        int givenYear = Integer.parseInt(parts[0]);
        int givenSemester = Integer.parseInt(parts[1]);

        if (givenYear != currentYear) {
            throw new Exception("Incompatibilidad con alguno de los campos del archivo (Debe ser el año actual)");
        }

        if (givenSemester == 1) {
            if(currentMonth > Calendar.JUNE + 1){
                throw new Exception("Incompatibilidad con alguno de los campos del archivo (Debe ser el semestre actual)");
            }
        } else if (givenSemester == 2) {
            if(currentMonth < Calendar.JULY + 1 ){
                throw new Exception("Incompatibilidad con alguno de los campos del archivo (Debe ser el semestre actual)");
            }
        }
    }

    public List<MonitoringDTO> getByHeadDepartment(String id) throws Exception {
        List<HeadProgram> list = headProgramRepository.findByDepartmentHeadId(id);
        List<MonitoringDTO> monitoringDTOS = new ArrayList<>();
        if(!list.isEmpty()){
            List<Course> courses = courseRepository.findByProgram(list.get(0).getProgram());
            List<Monitoring> monitorings = new ArrayList<>();
            for(Course course:courses){
                Optional<Monitoring> temporal = monitoringRepository.findByCourse(course);
                if(temporal.isPresent()){
                    monitorings.add(temporal.get());
                }
            }
            if(!monitorings.isEmpty()){
                for(Monitoring data: monitorings){
                    List<MonitoringMonitor> monitoringMonitor = monitoringMonitorRepository.findByMonitoring(data);
                    String monitor="";
                    if(!monitoringMonitor.isEmpty()) {
                        for (MonitoringMonitor value : monitoringMonitor) {
                            if (value.getEstadoSeleccion().equalsIgnoreCase("seleccionado")) {
                                monitor = monitor + value.getMonitor().getName() + " " + value.getMonitor().getLastName() + ", ";
                            } else {
                                monitor = "N/A";
                            }
                        }
                        monitor.replaceAll(", $", "");
                    }
                    else{
                        monitor = "N/A";
                    }
                    monitoringDTOS.add(new MonitoringDTO(data.getId(), data.getCourse().getName(), data.getSemester(), monitor, data.getProfessor().getName()));

                }

                return monitoringDTOS;
            }
            else
                throw new Exception("No hay monitorias creadas");
        }
        else
            throw new Exception("No existe jefe con este id");

    }

    public Map<String, Object> getCategoryReport(String professorId, Optional<Long> optionalMonitoringId) throws Exception {

        Optional<Professor> optionalProfessor = professorRepository.findById(professorId);
        if (optionalProfessor.isEmpty()) {
            throw new Exception("Profesor con ID " + professorId + " no encontrado.");
        }
        Professor professor = optionalProfessor.get();

        List<Monitoring> monitorings;
        if (optionalMonitoringId.isPresent()) {
            Long monitoringId = optionalMonitoringId.get();
            Optional<Monitoring> optionalMonitoring = monitoringRepository.findById(monitoringId);
            if (optionalMonitoring.isEmpty()) {
                throw new Exception("Monitoría con ID " + monitoringId + " no encontrada.");
            }
            Monitoring specificMonitoring = optionalMonitoring.get();
            if (!specificMonitoring.getProfessor().equals(professor)) {
                throw new Exception("La monitoría con ID " + monitoringId + " no pertenece al profesor con ID " + professorId);
            }
            monitorings = List.of(specificMonitoring);
        } else {
            monitorings = monitoringRepository.findByProfessor(professor);
        }

        Map<String, Object> finalReport = new LinkedHashMap<>();
        finalReport.put("detalle_por_curso", Collections.emptyList());
        finalReport.put("totales_por_categoria", Collections.emptyList());


        if (monitorings.isEmpty()) {
            System.out.println("No se encontraron monitorías para los criterios.");
            return finalReport;
        }

        List<Activity> relevantActivities = new ArrayList<>();
        for (Monitoring m : monitorings) {
            if (m.getCourse() != null && m.getCourse().getName() != null && !m.getCourse().getName().trim().isEmpty()) {
                relevantActivities.addAll(
                    activityRepository.findByMonitoring(m).stream()
                            .filter(act -> act.getCategory() != null && !act.getCategory().trim().isEmpty())
                            .collect(Collectors.toList())
                );
            }
        }

        if (relevantActivities.isEmpty()) {
            System.out.println("No se encontraron actividades con categoría y curso válido para las monitorías seleccionadas.");
            return finalReport; 
        }

        Map<String, Map<String, Long>> perCourseCategoryCounts = relevantActivities.stream()
                .collect(Collectors.groupingBy(
                    activity -> activity.getMonitoring().getCourse().getName(),
                    Collectors.groupingBy(
                        Activity::getCategory,
                        Collectors.counting()
                    )
                ));

        Map<String, Long> overallCategoryCounts = relevantActivities.stream()
                .collect(Collectors.groupingBy(
                    Activity::getCategory,
                    Collectors.counting()
                ));

        List<Map<String, Object>> courseDetailsList = new ArrayList<>();

        List<String> sortedCourseNames = perCourseCategoryCounts.keySet().stream().sorted().collect(Collectors.toList());

        for (String courseName : sortedCourseNames) {
            Map<String, Long> categoriesInCourse = perCourseCategoryCounts.get(courseName);
            List<Map<String, Object>> categoryListForCourse = new ArrayList<>();

            List<Map.Entry<String, Long>> sortedCategoriesInCourse = categoriesInCourse.entrySet().stream()
                    .sorted(Map.Entry.comparingByKey())
                    .collect(Collectors.toList());

            for (Map.Entry<String, Long> categoryEntry : sortedCategoriesInCourse) {
                Map<String, Object> categoryDetail = new HashMap<>();
                categoryDetail.put("categoria", categoryEntry.getKey());
                categoryDetail.put("cantidad", categoryEntry.getValue());
                categoryListForCourse.add(categoryDetail);
                // totalForCourse += categoryEntry.getValue();
            }

            Map<String, Object> courseDetailMap = new LinkedHashMap<>();
            courseDetailMap.put("curso", courseName);
            courseDetailMap.put("categorias", categoryListForCourse);
            // courseDetailMap.put("total_curso", totalForCourse);
            courseDetailsList.add(courseDetailMap);
        }
        
        List<Map<String, Object>> overallCategoryTotalsList = new ArrayList<>();
        // long grandTotalActivities = 0L;
         List<Map.Entry<String, Long>> sortedOverallCategories = overallCategoryCounts.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .collect(Collectors.toList());

        for (Map.Entry<String, Long> overallEntry : sortedOverallCategories) {
            Map<String, Object> totalCategoryDetail = new HashMap<>();
            totalCategoryDetail.put("categoria", overallEntry.getKey());
            totalCategoryDetail.put("cantidad_total", overallEntry.getValue());
            overallCategoryTotalsList.add(totalCategoryDetail);
            // grandTotalActivities += overallEntry.getValue();
        }

        finalReport.put("detalle_por_curso", courseDetailsList);
        finalReport.put("totales_por_categoria", overallCategoryTotalsList);
        // finalReport.put("total_actividades_general", grandTotalActivities);

        System.out.println("Reporte complejo de categorías generado (sin totales): " + finalReport);
        return finalReport;
    }

    public List<Map<String, Object>> getMonthlyAttendanceReport(String professorId, Optional<Long> optionalMonitoringId) throws Exception {
        Optional<Professor> optionalProfessor = professorRepository.findById(professorId);
        if (optionalProfessor.isEmpty()) {
            throw new Exception("Profesor con ID " + professorId + " no encontrado.");
        }
        Professor professor = optionalProfessor.get();

        List<Monitoring> monitorings;
        if (optionalMonitoringId.isPresent()) {
            Long monitoringId = optionalMonitoringId.get();
            Optional<Monitoring> optionalMonitoring = monitoringRepository.findById(monitoringId);
            if (optionalMonitoring.isEmpty()) {
                throw new Exception("Monitoría con ID " + monitoringId + " no encontrada.");
            }
            Monitoring specificMonitoring = optionalMonitoring.get();
            if (!specificMonitoring.getProfessor().equals(professor)) {
                throw new Exception("La monitoría con ID " + monitoringId + " no pertenece al profesor con ID " + professorId);
            }
            monitorings = List.of(specificMonitoring);
        } else {
            monitorings = monitoringRepository.findByProfessor(professor);
        }

        if (monitorings.isEmpty()) return Collections.emptyList();

        List<Activity> relevantActivities = new ArrayList<>();
        for (Monitoring m : monitorings) {
            if (m.getCourse() != null) {
                relevantActivities.addAll(activityRepository.findByMonitoring(m));
            }
        }

        if (relevantActivities.isEmpty()) return Collections.emptyList();

        List<Attendance> attendances = attendanceRepository.findByActivityIn(relevantActivities);

        if (attendances.isEmpty()) return Collections.emptyList();

        // Agrupar por mes y curso
        Map<YearMonth, Map<String, Map<String, Object>>> groupedData = new HashMap<>();

        for (Attendance attendance : attendances) {
            Activity activity = attendance.getActivity();
            if (activity == null || activity.getDelivey() == null) continue;

            Monitoring monitoring = activity.getMonitoring();
            if (monitoring == null || monitoring.getCourse() == null || monitoring.getCourse().getName() == null) continue;

            String courseName = monitoring.getCourse().getName().trim();
            if (courseName.isEmpty()) continue;

            YearMonth yearMonth = YearMonth.from(activity.getDelivey().toInstant().atZone(ZoneId.systemDefault()).toLocalDate());

            groupedData.putIfAbsent(yearMonth, new HashMap<>());
            Map<String, Map<String, Object>> courseMap = groupedData.get(yearMonth);

            courseMap.putIfAbsent(courseName, new HashMap<>());
            Map<String, Object> courseData = courseMap.get(courseName);

            // Inicializar lista de estudiantes y contador si no existen
            courseData.putIfAbsent("cantidad", 0L);
            courseData.putIfAbsent("estudiantes", new HashSet<String>());

            // Incrementar contador
            Long currentCount = (Long) courseData.get("cantidad");
            courseData.put("cantidad", currentCount + 1);

            // Agregar estudiante
            Set<String> studentNames = (Set<String>) courseData.get("estudiantes");
            studentNames.add(attendance.getStudent().getName());
        }

        // Ordenar y transformar a lista estructurada
        List<Map<String, Object>> reportList = new ArrayList<>();
        Locale spanishLocale = Locale.forLanguageTag("es-ES");

        groupedData.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> {
                    YearMonth yearMonth = entry.getKey();
                    Map<String, Map<String, Object>> courseData = entry.getValue();

                    String monthName = Month.of(yearMonth.getMonthValue()).getDisplayName(TextStyle.FULL, spanishLocale);
                    monthName = monthName.substring(0, 1).toUpperCase() + monthName.substring(1);
                    String semester = (yearMonth.getMonthValue() <= 6) ? yearMonth.getYear() + "-1" : yearMonth.getYear() + "-2";

                    Map<String, Object> reportEntry = new LinkedHashMap<>();
                    reportEntry.put("mes", monthName);
                    reportEntry.put("semestre", semester);

                    List<Map<String, Object>> courseDetailsList = new ArrayList<>();
                    long totalForMonth = 0;

                    for (Map.Entry<String, Map<String, Object>> courseEntry : courseData.entrySet()) {
                        String courseName = courseEntry.getKey();
                        Map<String, Object> data = courseEntry.getValue();

                        Long cantidad = (Long) data.get("cantidad");
                        Set<String> estudiantes = (Set<String>) data.get("estudiantes");

                        Map<String, Object> courseDetail = new LinkedHashMap<>();
                        courseDetail.put("curso", courseName);
                        courseDetail.put("cantidad", cantidad);
                        courseDetail.put("estudiantes", estudiantes); // <-- Agregamos nombres

                        courseDetailsList.add(courseDetail);
                        totalForMonth += cantidad;
                    }

                    reportEntry.put("asistencia_por_curso", courseDetailsList);
                    reportEntry.put("total_mes", totalForMonth);
                    reportList.add(reportEntry);
                });

        return reportList;
    }


    public boolean deleteMonitoring(long l) {
        Optional<Monitoring> monitoring = monitoringRepository.findById(l);
        if (!monitoring.isPresent()) {
            return false;
        }
        if(!monitoringMonitorRepository.findByMonitoring(monitoring.get()).isEmpty()) {
            return false;
        }
        monitoringRepository.delete(monitoring.get());
        return true;
    }

    private Map<String, Object> buildReportMapFromActivities(List<Activity> relevantActivities, String contextDescription) {
        Map<String, Object> finalReport = new LinkedHashMap<>();
        finalReport.put("detalle_por_curso", Collections.emptyList());
        finalReport.put("totales_por_categoria", Collections.emptyList());

        if (relevantActivities.isEmpty()) {
            System.out.println("No se encontraron actividades con categoría y curso válido para " + contextDescription);
            return finalReport;
        }

        Map<String, Map<String, Long>> perCourseCategoryCounts = relevantActivities.stream()
            .filter(activity -> activity.getMonitoring() != null &&
                            activity.getMonitoring().getCourse() != null &&
                            activity.getMonitoring().getCourse().getName() != null &&
                            !activity.getMonitoring().getCourse().getName().trim().isEmpty() &&
                            activity.getCategory() != null &&
                            !activity.getCategory().trim().isEmpty())
            .collect(Collectors.groupingBy(
                activity -> {
                    // String professorName = activity.getMonitoring().getProfessor() != null ?
                    //                        activity.getMonitoring().getProfessor().getName() : "N/A";
                    // return professorName + " - " + activity.getMonitoring().getCourse().getName();
                    return activity.getMonitoring().getCourse().getName();
                },
                Collectors.groupingBy(
                    Activity::getCategory,
                    Collectors.counting()
                )
            ));

        Map<String, Long> overallCategoryCounts = relevantActivities.stream()
            .filter(activity -> activity.getCategory() != null && !activity.getCategory().trim().isEmpty())
            .collect(Collectors.groupingBy(
                Activity::getCategory,
                Collectors.counting()
            ));

        if (perCourseCategoryCounts.isEmpty() && overallCategoryCounts.isEmpty()) {
            System.out.println("Aunque había actividades, ninguna tenía la información completa para agrupar en " + contextDescription);
            return finalReport;
        }


        List<Map<String, Object>> courseDetailsList = new ArrayList<>();
        List<String> sortedCourseNames = perCourseCategoryCounts.keySet().stream().sorted().collect(Collectors.toList());

        for (String courseName : sortedCourseNames) {
            Map<String, Long> categoriesInCourse = perCourseCategoryCounts.get(courseName);
            List<Map<String, Object>> categoryListForCourse = new ArrayList<>();

            List<Map.Entry<String, Long>> sortedCategoriesInCourse = categoriesInCourse.entrySet().stream()
                    .sorted(Map.Entry.comparingByKey())
                    .collect(Collectors.toList());

            for (Map.Entry<String, Long> categoryEntry : sortedCategoriesInCourse) {
                Map<String, Object> categoryDetail = new HashMap<>();
                categoryDetail.put("categoria", categoryEntry.getKey());
                categoryDetail.put("cantidad", categoryEntry.getValue());
                categoryListForCourse.add(categoryDetail);
            }

            Map<String, Object> courseDetailMap = new LinkedHashMap<>();
            courseDetailMap.put("curso", courseName);
            courseDetailMap.put("categorias", categoryListForCourse);
            courseDetailsList.add(courseDetailMap);
        }
        
        List<Map<String, Object>> overallCategoryTotalsList = new ArrayList<>();
        List<Map.Entry<String, Long>> sortedOverallCategories = overallCategoryCounts.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .collect(Collectors.toList());

        for (Map.Entry<String, Long> overallEntry : sortedOverallCategories) {
            Map<String, Object> totalCategoryDetail = new HashMap<>();
            totalCategoryDetail.put("categoria", overallEntry.getKey());
            totalCategoryDetail.put("cantidad_total", overallEntry.getValue());
            overallCategoryTotalsList.add(totalCategoryDetail);
        }

        finalReport.put("detalle_por_curso", courseDetailsList);
        finalReport.put("totales_por_categoria", overallCategoryTotalsList);

        System.out.println("Reporte de categorías generado para " + contextDescription + ": " + finalReport);
        return finalReport;
    }


    public Map<String, Object> getDepartmentCategoryReport(String departmentHeadId, Optional<Long> optionalMonitoringId) throws Exception {
        Optional<DepartmentHead> optionalDepartmentHead = departmentHeadRepository.findById(departmentHeadId);
        if (optionalDepartmentHead.isEmpty()) {
            throw new Exception("Jefe de departamento con ID " + departmentHeadId + " no encontrado.");
        }
        DepartmentHead departmentHead = optionalDepartmentHead.get();
        String reportContext = "jefe de departamento: " + departmentHeadId;

        List<HeadProgram> headPrograms = headProgramRepository.findByDepartmentHeadId(departmentHead.getId());
        if (headPrograms.isEmpty()) {
            System.out.println("No se encontraron programas asociados al jefe de departamento: " + departmentHeadId);
            Map<String, Object> emptyReport = new LinkedHashMap<>();
            emptyReport.put("detalle_por_curso", Collections.emptyList());
            emptyReport.put("totales_por_categoria", Collections.emptyList());
            return emptyReport;
        }

        Set<Professor> uniqueProfessorsInDepartment = new HashSet<>();
        for (HeadProgram hp : headPrograms) {
            Program program = hp.getProgram();
            if (program != null) {
                List<Course> coursesInProgram = courseRepository.findByProgram(program);
                for (Course course : coursesInProgram) {
                    List<CourseProfessor> courseProfessors = courseProfessorRepository.findByCourseId(course.getId());
                    for (CourseProfessor cp : courseProfessors) {
                        if (cp.getProfessor() != null) {
                            uniqueProfessorsInDepartment.add(cp.getProfessor());
                        }
                    }
                }
            }
        }

        if (uniqueProfessorsInDepartment.isEmpty()) {
            System.out.println("No se encontraron profesores en los programas del jefe de departamento: " + departmentHeadId);
            Map<String, Object> emptyReport = new LinkedHashMap<>();
            emptyReport.put("detalle_por_curso", Collections.emptyList());
            emptyReport.put("totales_por_categoria", Collections.emptyList());
            return emptyReport;
        }

        List<Monitoring> monitoringsToProcess = new ArrayList<>();
        if (optionalMonitoringId.isPresent()) {
            Long specificMonId = optionalMonitoringId.get();
            reportContext += ", monitoría específica: " + specificMonId;
            Optional<Monitoring> monOpt = monitoringRepository.findById(specificMonId);
            if (monOpt.isPresent()) {
                Monitoring specificMonitoring = monOpt.get();
                if (specificMonitoring.getProfessor() != null && uniqueProfessorsInDepartment.contains(specificMonitoring.getProfessor())) {
                    monitoringsToProcess.add(specificMonitoring);
                } else {
                    System.out.println("La monitoría específica " + specificMonId + " no pertenece a ningún profesor de este departamento.");
                }
            } else {
                System.out.println("Monitoría específica " + specificMonId + " no encontrada.");
            }
        } else {
            for (Professor prof : uniqueProfessorsInDepartment) {
                monitoringsToProcess.addAll(monitoringRepository.findByProfessor(prof));
            }
        }
        
        if (monitoringsToProcess.isEmpty()) {
            System.out.println("No se encontraron monitorías para procesar para " + reportContext);
            Map<String, Object> emptyReport = new LinkedHashMap<>();
            emptyReport.put("detalle_por_curso", Collections.emptyList());
            emptyReport.put("totales_por_categoria", Collections.emptyList());
            return emptyReport;
        }

        List<Activity> relevantActivities = new ArrayList<>();
        for (Monitoring m : monitoringsToProcess) {
            relevantActivities.addAll(activityRepository.findByMonitoring(m));
        }

        return buildReportMapFromActivities(relevantActivities, reportContext);
    }

    public List<Map<String, Object>> getDepartmentMonthlyAttendanceReport(String departmentHeadId, Optional<Long> optionalMonitoringId) throws Exception {
        Optional<DepartmentHead> optionalDepartmentHead = departmentHeadRepository.findById(departmentHeadId);
        if (optionalDepartmentHead.isEmpty()) {
            throw new Exception("Jefe de departamento con ID " + departmentHeadId + " no encontrado.");
        }
        DepartmentHead departmentHead = optionalDepartmentHead.get();

        // 1. Encontrar todos los HeadProgram asociados al DepartmentHead
        List<HeadProgram> headPrograms = headProgramRepository.findByDepartmentHeadId(departmentHead.getId());
        if (headPrograms.isEmpty()) {
            System.out.println("No se encontraron programas asociados al jefe de departamento: " + departmentHeadId + " para el reporte de asistencia.");
            return Collections.emptyList();
        }

        // 2. Recolectar todos los profesores únicos de esos programas
        Set<Professor> uniqueProfessorsInDepartment = new HashSet<>();
        for (HeadProgram hp : headPrograms) {
            Program program = hp.getProgram();
            if (program != null) {
                List<Course> coursesInProgram = courseRepository.findByProgram(program);
                for (Course course : coursesInProgram) {
                    List<CourseProfessor> courseProfessors = courseProfessorRepository.findByCourseId(course.getId());
                    for (CourseProfessor cp : courseProfessors) {
                        if (cp.getProfessor() != null) {
                            uniqueProfessorsInDepartment.add(cp.getProfessor());
                        }
                    }
                }
            }
        }

        if (uniqueProfessorsInDepartment.isEmpty()) {
            System.out.println("No se encontraron profesores en los programas del jefe de departamento: " + departmentHeadId + " para el reporte de asistencia.");
            return Collections.emptyList();
        }

        List<Monitoring> monitoringsToProcess = new ArrayList<>();
        if (optionalMonitoringId.isPresent()) {
            Long specificMonId = optionalMonitoringId.get();
            Optional<Monitoring> monOpt = monitoringRepository.findById(specificMonId);
            if (monOpt.isPresent()) {
                Monitoring specificMonitoring = monOpt.get();
                if (specificMonitoring.getProfessor() != null && uniqueProfessorsInDepartment.contains(specificMonitoring.getProfessor())) {
                    monitoringsToProcess.add(specificMonitoring);
                } else {
                    System.out.println("La monitoría específica " + specificMonId + " no pertenece a ningún profesor de este departamento para el reporte de asistencia.");
                }
            } else {
                System.out.println("Monitoría específica " + specificMonId + " no encontrada para el reporte de asistencia.");
            }
        } else {
            for (Professor prof : uniqueProfessorsInDepartment) {
                monitoringsToProcess.addAll(monitoringRepository.findByProfessor(prof));
            }
        }
        
        if (monitoringsToProcess.isEmpty()) {
            System.out.println("No se encontraron monitorías para procesar para el jefe de departamento: " + departmentHeadId + " para el reporte de asistencia.");
            return Collections.emptyList();
        }

        return buildAttendanceReportFromMonitorings(monitoringsToProcess, "jefe de departamento " + departmentHeadId);
    }

    private List<Map<String, Object>> buildAttendanceReportFromMonitorings(List<Monitoring> monitorings, String contextDescription) {
        if (monitorings.isEmpty()) {
            System.out.println("BuildAttendanceReport: No hay monitorías para procesar para " + contextDescription);
            return Collections.emptyList();
        }

        List<Activity> relevantActivities = new ArrayList<>();
        for (Monitoring m : monitorings) {
            if (m.getCourse() != null) {
                relevantActivities.addAll(activityRepository.findByMonitoring(m));
            }
        }

        if (relevantActivities.isEmpty()) {
            System.out.println("BuildAttendanceReport: No se encontraron actividades relevantes para " + contextDescription);
            return Collections.emptyList();
        }

        List<Attendance> attendances = attendanceRepository.findByActivityIn(relevantActivities);

        if (attendances.isEmpty()) {
            System.out.println("BuildAttendanceReport: No se encontraron registros de asistencia para " + contextDescription);
            // return List.of(Map.of("message", "No se encontraron datos de asistencia para los criterios seleccionados.", "data", Collections.emptyList()));
            return Collections.emptyList();
        }

        // Agrupar por mes y curso
        Map<YearMonth, Map<String, Map<String, Object>>> groupedData = new HashMap<>();

        for (Attendance attendance : attendances) {
            Activity activity = attendance.getActivity();
            if (activity == null || activity.getDelivey() == null) continue;

            Monitoring monitoring = activity.getMonitoring();
            if (monitoring == null || monitoring.getCourse() == null || monitoring.getCourse().getName() == null) continue;

            String courseName = monitoring.getCourse().getName().trim();
            if (courseName.isEmpty()) continue;

            // String professorName = monitoring.getProfessor() != null ? monitoring.getProfessor().getName() : "N/A";
            // String uniqueCourseKey = professorName + " - " + courseName; 

            YearMonth yearMonth = YearMonth.from(activity.getDelivey().toInstant().atZone(ZoneId.systemDefault()).toLocalDate());

            groupedData.putIfAbsent(yearMonth, new HashMap<>());
            Map<String, Map<String, Object>> courseMap = groupedData.get(yearMonth);

            courseMap.putIfAbsent(courseName, new HashMap<>()); 
            Map<String, Object> courseData = courseMap.get(courseName);

            courseData.putIfAbsent("cantidad", 0L);
            courseData.putIfAbsent("estudiantes", new HashSet<String>());

            Long currentCount = (Long) courseData.get("cantidad");
            courseData.put("cantidad", currentCount + 1);

            Set<String> studentNames = (Set<String>) courseData.get("estudiantes");
            if(attendance.getStudent() != null && attendance.getStudent().getName() != null) { 
                studentNames.add(attendance.getStudent().getName());
            }
        }

        if (groupedData.isEmpty()) { 
            System.out.println("BuildAttendanceReport: No hay datos agrupados para " + contextDescription);
            return Collections.emptyList();
        }

        List<Map<String, Object>> reportList = new ArrayList<>();
        Locale spanishLocale = Locale.forLanguageTag("es-ES");

        groupedData.entrySet().stream()
                .sorted(Map.Entry.comparingByKey()) // Order YearMonth
                .forEach(entry -> {
                    YearMonth yearMonth = entry.getKey();
                    Map<String, Map<String, Object>> coursesInMonthData = entry.getValue();

                    String monthName = Month.of(yearMonth.getMonthValue()).getDisplayName(TextStyle.FULL, spanishLocale);
                    monthName = monthName.substring(0, 1).toUpperCase() + monthName.substring(1);
                    String semester = (yearMonth.getMonthValue() <= 6) ? yearMonth.getYear() + "-1" : yearMonth.getYear() + "-2";

                    Map<String, Object> reportEntry = new LinkedHashMap<>();
                    reportEntry.put("mes", monthName);
                    reportEntry.put("semestre", semester);

                    List<Map<String, Object>> courseDetailsList = new ArrayList<>();
                    final AtomicLong currentMonthTotal = new AtomicLong(0);
                    
                    coursesInMonthData.entrySet().stream()
                        .sorted(Map.Entry.comparingByKey())
                        .forEach(courseEntry -> {
                            String courseKey = courseEntry.getKey();
                            Map<String, Object> dataForCourse = courseEntry.getValue();

                            Long cantidad = (Long) dataForCourse.get("cantidad");
                            Set<String> estudiantes = (Set<String>) dataForCourse.get("estudiantes");

                            Map<String, Object> courseDetail = new LinkedHashMap<>();
                            courseDetail.put("curso", courseKey);
                            courseDetail.put("cantidad", cantidad);
                            courseDetail.put("estudiantes", new ArrayList<>(estudiantes)); 

                            courseDetailsList.add(courseDetail);
                            if (cantidad != null) {
                                currentMonthTotal.addAndGet(cantidad);
                            }
                        });

                    reportEntry.put("asistencia_por_curso", courseDetailsList);
                    reportEntry.put("total_mes", currentMonthTotal);
                    reportList.add(reportEntry);
                });
        System.out.println("Reporte de asistencia generado para " + contextDescription + ". Elementos: " + reportList.size());
        return reportList;
    }

    public List<Monitoring> findMonitoringsByProfessorWithAssignedMonitors(String professorId) {
        // List<Monitoring> professorMonitorings = monitoringRepository.findByProfessorId(professorId);
        List<Monitoring> professorMonitorings = this.findAllByProfessor(professorId); 

        if (professorMonitorings.isEmpty()) {
            return List.of(); // lista vacía
        }

        // List<Integer> monitoringIds = professorMonitorings.stream().map(Monitoring::getId).collect(Collectors.toList());
        // if (monitoringIds.isEmpty()) return List.of();
        // List<Monitoring> filteredMonitorings = monitoringRepository.findMonitoringsByIdsWithAtLeastOneMonitor(monitoringIds);
        // return filteredMonitorings;

        return monitoringRepository.findMonitoringsByProfessorAndHavingSelectedMonitors(professorId);
    }

    public List<Monitoring> findMonitoringsByAssignedMonitor(String monitorId) {
        System.out.println("MonitoringServiceImpl.findMonitoringsByAssignedMonitor (con filtro de estado) para el monitor: " + monitorId);
        return monitoringRepository.findMonitoringsDirectlyAssignedToMonitorWithStatusSelected(monitorId);
    }

    // ==================== NUEVOS MÉTODOS PARA HU-010 ====================

    @Override
    public void approveMonitoring(Long monitoringId, String departmentHeadId, String comment) throws Exception {
        System.out.println("=== APROBANDO MONITORÍA (HU-010) ===");
        
        Monitoring monitoring = monitoringRepository.findById(monitoringId)
                .orElseThrow(() -> new Exception("Monitoría no encontrada"));
        
        // Validar que es del nuevo flujo (tiene MonitoringRequest asociada)
        if (!monitoring.isFromNewFlow()) {
            throw new Exception("Esta monitoría no pertenece al nuevo flujo de aprobación");
        }
        
        // Validar que está pendiente de aprobación
        if (!monitoring.requiresApproval()) {
            throw new Exception("Esta monitoría no está pendiente de aprobación. Estado: " + monitoring.getApprovalStatus());
        }
        
        // Aprobar la monitoría
        monitoring.approve(departmentHeadId, comment);
        monitoringRepository.save(monitoring);
        
        // Actualizar el estado de la MonitoringRequest asociada
        if (monitoring.getOriginatingRequest() != null) {
            MonitoringRequest request = monitoring.getOriginatingRequest();
            request.setStatus(RequestStatus.APROBADA);
            request.setUpdatedAt(LocalDateTime.now());
            monitoringRequestRepository.save(request);
        }
        
        System.out.println("Monitoría " + monitoringId + " aprobada por " + departmentHeadId);
        System.out.println("=====================================");
    }

    @Override
    public void rejectMonitoring(Long monitoringId, String departmentHeadId, String comment) throws Exception {
        System.out.println("=== RECHAZANDO MONITORÍA (HU-010) ===");
        
        Monitoring monitoring = monitoringRepository.findById(monitoringId)
                .orElseThrow(() -> new Exception("Monitoría no encontrada"));
        
        // Validar que es del nuevo flujo
        if (!monitoring.isFromNewFlow()) {
            throw new Exception("Esta monitoría no pertenece al nuevo flujo de aprobación");
        }
        
        // Validar que está pendiente de aprobación
        if (!monitoring.requiresApproval()) {
            throw new Exception("Esta monitoría no está pendiente de aprobación. Estado: " + monitoring.getApprovalStatus());
        }
        
        // Rechazar la monitoría
        monitoring.reject(departmentHeadId, comment);
        monitoringRepository.save(monitoring);
        
        // Actualizar el estado de la MonitoringRequest asociada
        if (monitoring.getOriginatingRequest() != null) {
            MonitoringRequest request = monitoring.getOriginatingRequest();
            request.setStatus(RequestStatus.RECHAZADA);
            request.setUpdatedAt(LocalDateTime.now());
            monitoringRequestRepository.save(request);
        }
        
        System.out.println("Monitoría " + monitoringId + " rechazada por " + departmentHeadId);
        System.out.println("Motivo: " + comment);
        System.out.println("======================================");
    }

    @Override
    public List<Monitoring> findPendingApproval() {
        return monitoringRepository.findAll().stream()
                .filter(Monitoring::requiresApproval)
                .collect(Collectors.toList());
    }
}
