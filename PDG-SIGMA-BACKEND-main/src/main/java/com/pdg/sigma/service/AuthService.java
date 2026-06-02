package com.pdg.sigma.service;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

import com.pdg.sigma.domain.*;
import com.pdg.sigma.repository.*;
import com.pdg.sigma.util.JwtService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import com.pdg.sigma.dto.AuthDTO;

import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

@Service
public class AuthService {

    @Autowired
    private ProspectRepository prospectRepository;

    @Autowired
    private ProfessorRepository professorRepository;

    @Autowired
    private MonitorRepository monitorRepository;

    @Autowired
    private DepartmentHeadRepository departmentHeadRepository;

    @Autowired
    private MonitoringMonitorRepository monitoringMonitorRepository;

    @Autowired
    private JwtService jwtService;

    private final WebClient webClient;

    @Value("${sigma.banner-api.base-url:http://localhost:5435}")
    private String bannerApiBaseUrl;

    public AuthService(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.build();
    }

    public AuthDTO loginUser(AuthDTO auth) throws Exception{
        Optional<Prospect> student = prospectRepository.findById(auth.getUserId());
        Optional<Professor> professor = professorRepository.findById(auth.getUserId());
        Optional<Monitor> monitor = monitorRepository.findByIdMonitor(auth.getUserId());
        Optional<DepartmentHead> departmentHead = departmentHeadRepository.findById(auth.getUserId());
        String response = authAPI(auth.getUserId(), auth.getPassword());

        if(!response.equalsIgnoreCase("false all") && !response.equalsIgnoreCase("false")){
            if(response.equalsIgnoreCase("student")){
                if(monitor.isPresent()){
                    List<MonitoringMonitor> list = monitoringMonitorRepository.findByMonitor(monitor.get());
                    if(!list.isEmpty()){
                        boolean selected = false;
                        for(MonitoringMonitor monitoringMonitor : list){
                            // HU-017: Considerar tanto "seleccionado" como "aprobado" para asignar rol monitor
                            if(monitoringMonitor.getEstadoSeleccion().equalsIgnoreCase("seleccionado") ||
                               monitoringMonitor.getEstadoSeleccion().equalsIgnoreCase("aprobado")){
                                selected = true;
                            }
                        }
                        if(selected){
                            String token = jwtService.generateToken(auth.getUserId(),"monitor");
                            return new AuthDTO("monitor", token, 1);
                        }
                    }
                }
            }
            if(response.equalsIgnoreCase("professor")){
                if(professor.isPresent()){
                    String token = jwtService.generateToken(auth.getUserId(),response);
                    return new AuthDTO(response, token,1);
                }
                else
                    throw new Exception("Este profesor no tiene materias asignadas dentro del sistema");
            }
            String token = jwtService.generateToken(auth.getUserId(),response);
            return new AuthDTO(response, token,1);
        }
        else
            throw new Exception("No hay un usuario con este id o contraseña");
    }

   /* public String authAPI(String id, String password) throws Exception {
        AuthDTO authDTO = new AuthDTO(id, password);
        String respuesta = getAuthData(authDTO).block();
        return respuesta;
    }*/

    public String authAPI(String id, String password) throws Exception{
        AuthDTO authDTO = new AuthDTO(id, password);
        // Reintenta hasta 4 veces con 5s de espera si el API-Banner responde 502
        // (ocurre cuando Render despierta el servicio desde reposo en plan gratuito)
        String respuesta = webClient.post()
            .uri(bannerApiBaseUrl + "/api/auth/login")
            .bodyValue(authDTO)
            .retrieve()
            .onStatus(
                status -> status == HttpStatus.BAD_GATEWAY || status == HttpStatus.SERVICE_UNAVAILABLE,
                clientResponse -> Mono.error(new WebClientResponseException(
                    clientResponse.statusCode().value(),
                    "API-Banner iniciando, reintentando...", null, null, null))
            )
            .bodyToMono(String.class)
            .retryWhen(Retry.fixedDelay(4, Duration.ofSeconds(5))
                .filter(e -> e instanceof WebClientResponseException wcre
                    && (wcre.getStatusCode() == HttpStatus.BAD_GATEWAY
                        || wcre.getStatusCode() == HttpStatus.SERVICE_UNAVAILABLE)))
            .block(Duration.ofSeconds(60));

        return respuesta;
    }

    
}