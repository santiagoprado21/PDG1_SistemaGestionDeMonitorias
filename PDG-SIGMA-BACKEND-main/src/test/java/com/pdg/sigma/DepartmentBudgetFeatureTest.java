package com.pdg.sigma;

import com.pdg.sigma.controller.DepartmentBudgetController;
import com.pdg.sigma.domain.*;
import com.pdg.sigma.repository.*;
import com.pdg.sigma.service.MonitoringServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.ResponseEntity;

import java.util.Map;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class DepartmentBudgetFeatureTest {

    // Mock mail sender to satisfy EmailSenderService dependency in test context
    @MockBean
    private JavaMailSender javaMailSender;

    @Autowired private SchoolRepository schoolRepository;
    @Autowired private ProgramRepository programRepository;
    @Autowired private CourseRepository courseRepository;
    @Autowired private ProfessorRepository professorRepository;
    @Autowired private MonitoringRepository monitoringRepository;
    @Autowired private DepartmentBudgetRepository departmentBudgetRepository;

    @Autowired private MonitoringServiceImpl monitoringService;
    @Autowired private DepartmentBudgetController budgetController;

    private School school;
    private Program program;
    private Course course1;
    private Course course2;
    private Professor professor;

    private final String semester = "2025-2";

    @BeforeEach
    void setup() {
        monitoringRepository.deleteAll();
        courseRepository.deleteAll();
        programRepository.deleteAll();
        schoolRepository.deleteAll();
        professorRepository.deleteAll();
        departmentBudgetRepository.deleteAll();

        school = new School();
        school.setName("Facultad de Ingeniería");
        school = schoolRepository.save(school);

        program = new Program();
        program.setName("Ingeniería de Sistemas");
        program.setSchool(school);
        program = programRepository.save(program);

        course1 = new Course();
        course1.setName("Estructuras de Datos");
        course1.setProgram(program);
        course1 = courseRepository.save(course1);

        course2 = new Course();
        course2.setName("Bases de Datos");
        course2.setProgram(program);
        course2 = courseRepository.save(course2);

        professor = new Professor();
        professor.setId("P-1");
        professor.setName("Prof. Budget");
        professor.setPassword("pwd");
        professorRepository.save(professor);
    }

    private Monitoring newMonitoring(Course course) {
        Monitoring m = new Monitoring();
        m.setSchool(school);
        m.setProgram(program);
        m.setCourse(course);
        m.setProfessor(professor);
        m.setSemester(semester);
        // fechas requeridas por la entidad (start/finish no nulos)
        Date now = new Date();
        m.setStart(now);
        m.setFinish(new Date(now.getTime() + 7L * 24 * 60 * 60 * 1000)); // +7 días
        // valores por defecto para notas
        m.setAverageGrade(0.0);
        m.setCourseGrade(0.0);
        return monitoringRepository.save(m);
    }

    @Test
    void can_define_hours_and_rate_and_cost_is_derived() throws Exception {
        // Presupuesto amplio para no bloquear
        departmentBudgetRepository.save(new DepartmentBudget(program, semester, 100));
        Monitoring m = newMonitoring(course1);

        Monitoring updated = monitoringService.updateMonitoringBudget(m.getId(), 12, 15000.0);
        assertEquals(12, updated.getEstimatedHours());
        assertEquals(15000.0, updated.getHourlyRate());

        // Costo presupuestal se deriva de horas x valor hora (no se persiste explícitamente)
        double cost = updated.getEstimatedHours() * updated.getHourlyRate();
        assertEquals(180000.0, cost, 0.001);
    }

    @Test
    void rejects_assigning_hours_over_remaining_budget() throws Exception {
        // total 10 horas para el semestre
        departmentBudgetRepository.save(new DepartmentBudget(program, semester, 10));

        // Una monitoría ya usa 8 horas
        Monitoring m1 = newMonitoring(course1);
        monitoringService.updateMonitoringBudget(m1.getId(), 8, 10000.0);

        // Otra intenta usar 5 -> excede (8 + 5 > 10)
        Monitoring m2 = newMonitoring(course2);
        Exception ex = assertThrows(Exception.class, () ->
                monitoringService.updateMonitoringBudget(m2.getId(), 5, 12000.0)
        );
        assertTrue(ex.getMessage().contains("No se pueden asignar más horas"));
        assertTrue(ex.getMessage().contains("Disponibles: 2"));
    }

    @Test
    void remaining_budget_updates_in_real_time() throws Exception {
        departmentBudgetRepository.save(new DepartmentBudget(program, semester, 20));

        Monitoring m1 = newMonitoring(course1);
        monitoringService.updateMonitoringBudget(m1.getId(), 6, 9000.0);

        // Consulta de presupuesto
        ResponseEntity<?> resp1 = budgetController.getBudget(program.getName(), semester);
        assertEquals(200, resp1.getStatusCode().value());
        @SuppressWarnings("unchecked")
        Map<String, Object> payload1 = (Map<String, Object>) resp1.getBody();
        assertNotNull(payload1);
        assertEquals(20, payload1.get("totalHours"));
        assertEquals(6, payload1.get("usedHours"));
        assertEquals(14, payload1.get("remainingHours"));

        // Aumenta horas y el restante baja inmediatamente
        monitoringService.updateMonitoringBudget(m1.getId(), 10, 9000.0);
        ResponseEntity<?> resp2 = budgetController.getBudget(program.getName(), semester);
        @SuppressWarnings("unchecked")
        Map<String, Object> payload2 = (Map<String, Object>) resp2.getBody();
        assertNotNull(payload2);
        assertEquals(10, payload2.get("usedHours"));
        assertEquals(10, payload2.get("remainingHours"));
    }

    @Test
    void rejects_negative_values_for_hours_and_rate() throws Exception {
        departmentBudgetRepository.save(new DepartmentBudget(program, semester, 50));
        Monitoring m = newMonitoring(course1);

        Exception ex1 = assertThrows(Exception.class, () ->
                monitoringService.updateMonitoringBudget(m.getId(), -1, 10000.0)
        );
        assertTrue(ex1.getMessage().toLowerCase().contains("horas"));

        Exception ex2 = assertThrows(Exception.class, () ->
                monitoringService.updateMonitoringBudget(m.getId(), 5, -100.0)
        );
        assertTrue(ex2.getMessage().toLowerCase().contains("hora"));
    }

    @Test
    void boundary_equal_to_remaining_is_allowed() throws Exception {
        departmentBudgetRepository.save(new DepartmentBudget(program, semester, 10));
        Monitoring m1 = newMonitoring(course1);
        monitoringService.updateMonitoringBudget(m1.getId(), 8, 10000.0);

        // Quedan 2 horas, exactamente 2 debe permitir
        Monitoring m2 = newMonitoring(course2);
        Monitoring updated = monitoringService.updateMonitoringBudget(m2.getId(), 2, 12000.0);
        assertEquals(2, updated.getEstimatedHours());

        // Verifica used/remaining
        ResponseEntity<?> resp = budgetController.getBudget(program.getName(), semester);
        @SuppressWarnings("unchecked")
        Map<String, Object> payload = (Map<String, Object>) resp.getBody();
        assertEquals(10, payload.get("usedHours"));
        assertEquals(0, payload.get("remainingHours"));
    }

    @Test
    void update_only_rate_or_only_hours_preserves_the_other_field() throws Exception {
        departmentBudgetRepository.save(new DepartmentBudget(program, semester, 30));
        Monitoring m = newMonitoring(course1);

        // Inicial: set horas y tarifa
        Monitoring first = monitoringService.updateMonitoringBudget(m.getId(), 10, 8000.0);
        assertEquals(10, first.getEstimatedHours());
        assertEquals(8000.0, first.getHourlyRate());

        // Solo cambia tarifa; horas deben mantenerse
        Monitoring second = monitoringService.updateMonitoringBudget(m.getId(), null, 9000.0);
        assertEquals(10, second.getEstimatedHours());
        assertEquals(9000.0, second.getHourlyRate());

        // Solo cambia horas; tarifa debe mantenerse
        Monitoring third = monitoringService.updateMonitoringBudget(m.getId(), 6, null);
        assertEquals(6, third.getEstimatedHours());
        assertEquals(9000.0, third.getHourlyRate());
    }

    @Test
    void decreasing_hours_frees_budget_and_is_reflected() throws Exception {
        departmentBudgetRepository.save(new DepartmentBudget(program, semester, 12));
        Monitoring m = newMonitoring(course1);
        monitoringService.updateMonitoringBudget(m.getId(), 10, 7000.0);

        ResponseEntity<?> resp1 = budgetController.getBudget(program.getName(), semester);
        @SuppressWarnings("unchecked")
        Map<String, Object> payload1 = (Map<String, Object>) resp1.getBody();
        assertEquals(2, payload1.get("remainingHours"));

        // Disminuye horas a 5
        monitoringService.updateMonitoringBudget(m.getId(), 5, null);
        ResponseEntity<?> resp2 = budgetController.getBudget(program.getName(), semester);
        @SuppressWarnings("unchecked")
        Map<String, Object> payload2 = (Map<String, Object>) resp2.getBody();
        assertEquals(7, payload2.get("remainingHours"));
    }

    @Test
    void budget_controller_returns_meaningful_errors() {
        // 404 si no hay presupuesto configurado
        ResponseEntity<?> notFound = budgetController.getBudget(program.getName(), semester);
        assertEquals(404, notFound.getStatusCode().value());

        // 400 si el programa no existe
        ResponseEntity<?> bad = budgetController.getBudget("Programa Inexistente", semester);
        assertEquals(400, bad.getStatusCode().value());
    }
}
