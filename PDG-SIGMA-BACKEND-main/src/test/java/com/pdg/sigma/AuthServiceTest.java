package com.pdg.sigma;

import com.pdg.sigma.domain.*;
import com.pdg.sigma.dto.AuthDTO;
import com.pdg.sigma.repository.*;
import com.pdg.sigma.service.AuthService;
import com.pdg.sigma.util.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AuthServiceTest {

    private static final String TEST_PASSWORD = "test-pwd-placeholder";

    @Mock
    private ProspectRepository prospectRepository;

    @Mock
    private ProfessorRepository professorRepository;

    @Mock
    private MonitorRepository monitorRepository;

    @Mock
    private DepartmentHeadRepository departmentHeadRepository;

    @Mock
    private MonitoringMonitorRepository monitoringMonitorRepository;

    @Mock
    private JwtService jwtService;

    @Mock
    private WebClient.Builder webClientBuilder;

    @Mock
    private WebClient webClient;

    private AuthService authService;
    private AuthService authServiceStudent;
    private AuthService authServiceProfessor;
    private AuthService authServiceHead;

    private AuthDTO authDTO;
    private Monitor mockMonitor;
    private Professor mockProfessor;
    private Prospect mockProspect;

    @BeforeEach
    void setUp() throws Exception {
        MockitoAnnotations.openMocks(this);

        when(webClientBuilder.build()).thenReturn(webClient);

        authDTO = new AuthDTO();
        authDTO.setUserId("U001");
        authDTO.setPassword(TEST_PASSWORD);

        mockMonitor = new Monitor();
        mockMonitor.setCode("M001");
        mockMonitor.setIdMonitor("U001");

        mockProfessor = new Professor();
        mockProfessor.setId("U001");

        mockProspect = new Prospect();
        mockProspect.setId("U001");
        mockProspect.setCode("M001");

        authService = createAuthService("false all");
        authServiceStudent = createAuthService("student");
        authServiceProfessor = createAuthService("professor");
        authServiceHead = createAuthService("departmentHead");
    }

    private AuthService createAuthService(String authResponse) {
        AuthService service = new AuthService(webClientBuilder) {
            @Override
            public String authAPI(String id, String password) throws Exception {
                return authResponse;
            }
        };
        ReflectionTestUtils.setField(service, "prospectRepository", prospectRepository);
        ReflectionTestUtils.setField(service, "professorRepository", professorRepository);
        ReflectionTestUtils.setField(service, "monitorRepository", monitorRepository);
        ReflectionTestUtils.setField(service, "departmentHeadRepository", departmentHeadRepository);
        ReflectionTestUtils.setField(service, "monitoringMonitorRepository", monitoringMonitorRepository);
        ReflectionTestUtils.setField(service, "jwtService", jwtService);
        return service;
    }

    @Test
    @DisplayName("Debe fallar si authAPI retorna false all")
    void testLoginAuthFails() {
        when(prospectRepository.findById("U001")).thenReturn(Optional.empty());
        when(professorRepository.findById("U001")).thenReturn(Optional.empty());
        when(monitorRepository.findByIdMonitor("U001")).thenReturn(Optional.empty());
        when(departmentHeadRepository.findById("U001")).thenReturn(Optional.empty());

        Exception exception = assertThrows(Exception.class, () -> {
            authService.loginUser(authDTO);
        });

        assertTrue(exception.getMessage().contains("No hay un usuario"));
    }

    @Test
    @DisplayName("Debe autenticar como monitor aprobado")
    void testLoginAsMonitorApproved() throws Exception {
        when(prospectRepository.findById("U001")).thenReturn(Optional.of(mockProspect));
        when(professorRepository.findById("U001")).thenReturn(Optional.empty());
        when(monitorRepository.findByIdMonitor("U001")).thenReturn(Optional.of(mockMonitor));
        when(departmentHeadRepository.findById("U001")).thenReturn(Optional.empty());

        MonitoringMonitor mm = new MonitoringMonitor();
        mm.setEstadoSeleccion("aprobado");
        when(monitoringMonitorRepository.findByMonitor(mockMonitor)).thenReturn(List.of(mm));
        when(jwtService.generateToken("U001", "monitor")).thenReturn("token-monitor");

        AuthDTO result = authServiceStudent.loginUser(authDTO);

        assertEquals("monitor", result.getRole());
    }

    @Test
    @DisplayName("Debe autenticar como monitor seleccionado")
    void testLoginAsMonitorSelected() throws Exception {
        when(prospectRepository.findById("U001")).thenReturn(Optional.of(mockProspect));
        when(professorRepository.findById("U001")).thenReturn(Optional.empty());
        when(monitorRepository.findByIdMonitor("U001")).thenReturn(Optional.of(mockMonitor));
        when(departmentHeadRepository.findById("U001")).thenReturn(Optional.empty());

        MonitoringMonitor mm = new MonitoringMonitor();
        mm.setEstadoSeleccion("seleccionado");
        when(monitoringMonitorRepository.findByMonitor(mockMonitor)).thenReturn(List.of(mm));
        when(jwtService.generateToken("U001", "monitor")).thenReturn("token-monitor");

        AuthDTO result = authServiceStudent.loginUser(authDTO);

        assertEquals("monitor", result.getRole());
    }

    @Test
    @DisplayName("Debe autenticar como estudiante sin rol monitor")
    void testLoginAsStudent() throws Exception {
        when(prospectRepository.findById("U001")).thenReturn(Optional.of(mockProspect));
        when(professorRepository.findById("U001")).thenReturn(Optional.empty());
        when(monitorRepository.findByIdMonitor("U001")).thenReturn(Optional.empty());
        when(departmentHeadRepository.findById("U001")).thenReturn(Optional.empty());
        when(jwtService.generateToken("U001", "student")).thenReturn("token-student");

        AuthDTO result = authServiceStudent.loginUser(authDTO);

        assertEquals("student", result.getRole());
    }

    @Test
    @DisplayName("Debe autenticar como profesor")
    void testLoginAsProfessor() throws Exception {
        when(prospectRepository.findById("U001")).thenReturn(Optional.empty());
        when(professorRepository.findById("U001")).thenReturn(Optional.of(mockProfessor));
        when(monitorRepository.findByIdMonitor("U001")).thenReturn(Optional.empty());
        when(departmentHeadRepository.findById("U001")).thenReturn(Optional.empty());
        when(jwtService.generateToken("U001", "professor")).thenReturn("token-professor");

        AuthDTO result = authServiceProfessor.loginUser(authDTO);

        assertEquals("professor", result.getRole());
    }

    @Test
    @DisplayName("Debe fallar si profesor no está en sistema")
    void testLoginProfessorNotInSystem() {
        when(prospectRepository.findById("U001")).thenReturn(Optional.empty());
        when(professorRepository.findById("U001")).thenReturn(Optional.empty());
        when(monitorRepository.findByIdMonitor("U001")).thenReturn(Optional.empty());
        when(departmentHeadRepository.findById("U001")).thenReturn(Optional.empty());

        Exception exception = assertThrows(Exception.class, () -> {
            authServiceProfessor.loginUser(authDTO);
        });

        assertTrue(exception.getMessage().contains("no tiene materias"));
    }

    @Test
    @DisplayName("Debe autenticar como department head")
    void testLoginAsDepartmentHead() throws Exception {
        when(prospectRepository.findById("U001")).thenReturn(Optional.empty());
        when(professorRepository.findById("U001")).thenReturn(Optional.empty());
        when(monitorRepository.findByIdMonitor("U001")).thenReturn(Optional.empty());
        when(departmentHeadRepository.findById("U001")).thenReturn(Optional.empty());
        when(jwtService.generateToken("U001", "departmentHead")).thenReturn("token-head");

        AuthDTO result = authServiceHead.loginUser(authDTO);

        assertEquals("departmentHead", result.getRole());
    }
}
