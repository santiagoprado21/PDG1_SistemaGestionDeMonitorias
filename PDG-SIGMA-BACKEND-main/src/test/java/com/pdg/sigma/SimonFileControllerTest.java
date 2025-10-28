package com.pdg.sigma;

import com.pdg.sigma.controller.SimonFileController;
import com.pdg.sigma.domain.SimonFileGeneration;
import com.pdg.sigma.dto.SimonMonitoringDTO;
import com.pdg.sigma.service.SimonFileService;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Pruebas de integración para SimonFileController
 * HU-004: Generación de archivo SIMON
 * 
 * Casos de prueba:
 * 1. Generación exitosa del archivo Excel (GET /simon/generate)
 * 2. Error 204 cuando no hay monitorías aprobadas
 * 3. Error 500 en caso de IOException
 * 4. Preview de datos exitoso (GET /simon/preview)
 * 5. Preview sin monitorías disponibles
 * 6. Obtención del historial completo (GET /simon/history)
 * 7. Obtención del historial por semestre (GET /simon/history/{semester})
 * 8. Verificación de headers HTTP para descarga del archivo
 * 9. Validación de parámetros opcionales (generatedBy, semester)
 * 10. Manejo de errores en endpoints de historial
 */
@WebMvcTest(SimonFileController.class)
@ComponentScan(basePackages = "com.pdg.sigma.util")
class SimonFileControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private SimonFileService simonFileService;

    private SimonMonitoringDTO mockDTO;
    private Workbook mockWorkbook;

    @BeforeEach
    void setUp() {
        // Crear DTO de prueba
        mockDTO = new SimonMonitoringDTO();
        mockDTO.setEstudianteTipo("PRE");
        mockDTO.setCenco("CA021202");
        mockDTO.setNumeroMonitoria("");
        mockDTO.setNombre("Juan");
        mockDTO.setApellido("Pérez");
        mockDTO.setCedula("1234567890");
        mockDTO.setCodigoEstudiante("2220001");
        mockDTO.setEmail("juan.perez@universidad.edu.co");
        mockDTO.setCelular("");
        mockDTO.setCodigoCurso("TIC - 09704");
        mockDTO.setNombreCurso("Programación Orientada a Objetos");
        mockDTO.setDescripcionMonitoria("NRC-1");
        mockDTO.setFechaInicio("01/08/2024");
        mockDTO.setFechaFin("01/11/2024");
        mockDTO.setTotalHoras(30);
        mockDTO.setTotalSemanas(13);
        mockDTO.setProfesorSolicita("Dr. Carlos Rodríguez");

        // Crear workbook de prueba
        mockWorkbook = new XSSFWorkbook();
        mockWorkbook.createSheet("Monitorías SIMON");
    }

    /**
     * Caso 1: Generación exitosa del archivo Excel
     * Debe retornar 200 OK con el archivo Excel descargable
     */
    @Test
    @WithMockUser(roles = "JEFE")
    void generateSimonFile_WithApprovedMonitorings_ShouldReturnExcelFile() throws Exception {
        // Arrange
        when(simonFileService.generateSimonFile(anyString(), anyString()))
            .thenReturn(mockWorkbook);

        // Act & Assert
        mockMvc.perform(get("/simon/generate")
                .param("generatedBy", "coordinador")
                .param("semester", "2024-2"))
                .andExpect(status().isOk())
                .andExpect(header().exists("Content-Disposition"))
                .andExpect(header().string("Content-Disposition", containsString("attachment")))
                .andExpect(header().string("Content-Disposition", containsString("SIMON_2024-2_")))
                .andExpect(header().string("Content-Disposition", containsString(".xlsx")))
                .andExpect(header().string("Content-Type", 
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .andExpect(content().contentType(
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));

        verify(simonFileService).generateSimonFile("coordinador", "2024-2");
    }

    /**
     * Caso 2: Generación con parámetros por defecto
     * Cuando no se envían parámetros, debe usar valores por defecto
     */
    @Test
    @WithMockUser(roles = "JEFE")
    void generateSimonFile_WithDefaultParameters_ShouldUseDefaults() throws Exception {
        // Arrange
        when(simonFileService.generateSimonFile("coordinador", "2024-2"))
            .thenReturn(mockWorkbook);

        // Act & Assert
        mockMvc.perform(get("/simon/generate"))
                .andExpect(status().isOk())
                .andExpect(header().exists("Content-Disposition"));

        verify(simonFileService).generateSimonFile("coordinador", "2024-2");
    }

    /**
     * Caso 3: Error cuando no hay monitorías aprobadas
     * Debe retornar 204 NO CONTENT
     */
    @Test
    @WithMockUser(roles = "JEFE")
    void generateSimonFile_WithNoMonitorings_ShouldReturn204() throws Exception {
        // Arrange
        when(simonFileService.generateSimonFile(anyString(), anyString()))
            .thenThrow(new IllegalStateException("No hay monitorías aprobadas para generar el archivo"));

        // Act & Assert
        mockMvc.perform(get("/simon/generate")
                .param("generatedBy", "coordinador")
                .param("semester", "2024-2"))
                .andExpect(status().isNoContent());

        verify(simonFileService).generateSimonFile("coordinador", "2024-2");
    }

    /**
     * Caso 4: Error interno al generar el archivo
     * Debe retornar 500 INTERNAL SERVER ERROR
     */
    @Test
    @WithMockUser(roles = "JEFE")
    void generateSimonFile_WithIOException_ShouldReturn500() throws Exception {
        // Arrange
        when(simonFileService.generateSimonFile(anyString(), anyString()))
            .thenThrow(new IOException("Error al escribir el archivo"));

        // Act & Assert
        mockMvc.perform(get("/simon/generate")
                .param("generatedBy", "coordinador")
                .param("semester", "2024-2"))
                .andExpect(status().isInternalServerError());

        verify(simonFileService).generateSimonFile("coordinador", "2024-2");
    }

    /**
     * Caso 5: Preview exitoso con monitorías disponibles
     * Debe retornar 200 OK con la lista de monitorías y metadata
     */
    @Test
    @WithMockUser(roles = "JEFE")
    void previewSimonData_WithMonitorings_ShouldReturnData() throws Exception {
        // Arrange
        List<SimonMonitoringDTO> mockMonitorings = Arrays.asList(mockDTO);
        when(simonFileService.getApprovedMonitoringsForSimon())
            .thenReturn(mockMonitorings);

        // Act & Assert
        mockMvc.perform(get("/simon/preview")
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.totalMonitorings", is(1)))
                .andExpect(jsonPath("$.canGenerate", is(true)))
                .andExpect(jsonPath("$.monitorings", hasSize(1)))
                .andExpect(jsonPath("$.monitorings[0].nombre", is("Juan")))
                .andExpect(jsonPath("$.monitorings[0].apellido", is("Pérez")))
                .andExpect(jsonPath("$.monitorings[0].codigoEstudiante", is("2220001")))
                .andExpect(jsonPath("$.monitorings[0].nombreCurso", is("Programación Orientada a Objetos")))
                .andExpect(jsonPath("$.monitorings[0].totalHoras", is(30)))
                .andExpect(jsonPath("$.monitorings[0].totalSemanas", is(13)));

        verify(simonFileService).getApprovedMonitoringsForSimon();
    }

    /**
     * Caso 6: Preview sin monitorías disponibles
     * Debe retornar canGenerate = false
     */
    @Test
    @WithMockUser(roles = "JEFE")
    void previewSimonData_WithNoMonitorings_ShouldIndicateCannotGenerate() throws Exception {
        // Arrange
        when(simonFileService.getApprovedMonitoringsForSimon())
            .thenReturn(Collections.emptyList());

        // Act & Assert
        mockMvc.perform(get("/simon/preview")
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.totalMonitorings", is(0)))
                .andExpect(jsonPath("$.canGenerate", is(false)))
                .andExpect(jsonPath("$.monitorings", hasSize(0)));

        verify(simonFileService).getApprovedMonitoringsForSimon();
    }

    /**
     * Caso 7: Preview con múltiples monitorías
     */
    @Test
    @WithMockUser(roles = "JEFE")
    void previewSimonData_WithMultipleMonitorings_ShouldReturnAll() throws Exception {
        // Arrange
        SimonMonitoringDTO dto2 = new SimonMonitoringDTO();
        dto2.setNombre("María");
        dto2.setApellido("González");
        dto2.setCodigoEstudiante("2220002");
        dto2.setNombreCurso("Estructuras de Datos");
        dto2.setTotalHoras(30);
        dto2.setTotalSemanas(14);

        List<SimonMonitoringDTO> mockMonitorings = Arrays.asList(mockDTO, dto2);
        when(simonFileService.getApprovedMonitoringsForSimon())
            .thenReturn(mockMonitorings);

        // Act & Assert
        mockMvc.perform(get("/simon/preview")
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalMonitorings", is(2)))
                .andExpect(jsonPath("$.canGenerate", is(true)))
                .andExpect(jsonPath("$.monitorings", hasSize(2)))
                .andExpect(jsonPath("$.monitorings[1].nombre", is("María")))
                .andExpect(jsonPath("$.monitorings[1].codigoEstudiante", is("2220002")));
    }

    /**
     * Caso 8: Error en preview
     * Debe retornar 500 con información del error
     */
    @Test
    @WithMockUser(roles = "JEFE")
    void previewSimonData_WithException_ShouldReturn500() throws Exception {
        // Arrange
        when(simonFileService.getApprovedMonitoringsForSimon())
            .thenThrow(new RuntimeException("Error de base de datos"));

        // Act & Assert
        mockMvc.perform(get("/simon/preview")
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error", is("Error de base de datos")))
                .andExpect(jsonPath("$.canGenerate", is(false)));
    }

    /**
     * Caso 9: Obtención del historial completo
     * Debe retornar 200 OK con la lista de generaciones
     */
    @Test
    @WithMockUser(roles = "JEFE")
    void getGenerationHistory_ShouldReturnAllGenerations() throws Exception {
        // Arrange
        List<SimonFileGeneration> mockHistory = Arrays.asList(
            new SimonFileGeneration(1L, LocalDateTime.now(), "coordinador", 5, 
                "SIMON_2024-2_20241101_120000.xlsx", "2024-2"),
            new SimonFileGeneration(2L, LocalDateTime.now().minusDays(1), "jefe", 3, 
                "SIMON_2024-1_20240601_100000.xlsx", "2024-1")
        );
        
        when(simonFileService.getGenerationHistory())
            .thenReturn(mockHistory);

        // Act & Assert
        mockMvc.perform(get("/simon/history")
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].id", is(1)))
                .andExpect(jsonPath("$[0].generatedBy", is("coordinador")))
                .andExpect(jsonPath("$[0].totalMonitorings", is(5)))
                .andExpect(jsonPath("$[0].semester", is("2024-2")))
                .andExpect(jsonPath("$[1].totalMonitorings", is(3)));

        verify(simonFileService).getGenerationHistory();
    }

    /**
     * Caso 10: Historial vacío
     * Debe retornar 200 OK con lista vacía
     */
    @Test
    @WithMockUser(roles = "JEFE")
    void getGenerationHistory_WithNoHistory_ShouldReturnEmptyList() throws Exception {
        // Arrange
        when(simonFileService.getGenerationHistory())
            .thenReturn(Collections.emptyList());

        // Act & Assert
        mockMvc.perform(get("/simon/history")
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(0)));

        verify(simonFileService).getGenerationHistory();
    }

    /**
     * Caso 11: Error al obtener historial
     * Debe retornar 500 INTERNAL SERVER ERROR
     */
    @Test
    @WithMockUser(roles = "JEFE")
    void getGenerationHistory_WithException_ShouldReturn500() throws Exception {
        // Arrange
        when(simonFileService.getGenerationHistory())
            .thenThrow(new RuntimeException("Error de base de datos"));

        // Act & Assert
        mockMvc.perform(get("/simon/history")
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isInternalServerError());

        verify(simonFileService).getGenerationHistory();
    }

    /**
     * Caso 12: Obtención del historial por semestre
     * Debe retornar solo las generaciones del semestre especificado
     */
    @Test
    @WithMockUser(roles = "JEFE")
    void getGenerationHistoryBySemester_ShouldReturnFilteredGenerations() throws Exception {
        // Arrange
        String semester = "2024-2";
        List<SimonFileGeneration> mockHistory = Arrays.asList(
            new SimonFileGeneration(1L, LocalDateTime.now(), "coordinador", 5, 
                "SIMON_2024-2_20241101_120000.xlsx", semester),
            new SimonFileGeneration(3L, LocalDateTime.now().minusDays(5), "coordinador", 4, 
                "SIMON_2024-2_20241015_140000.xlsx", semester)
        );
        
        when(simonFileService.getGenerationHistoryBySemester(semester))
            .thenReturn(mockHistory);

        // Act & Assert
        mockMvc.perform(get("/simon/history/{semester}", semester)
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].semester", is(semester)))
                .andExpect(jsonPath("$[1].semester", is(semester)));

        verify(simonFileService).getGenerationHistoryBySemester(semester);
    }

    /**
     * Caso 13: Historial por semestre sin resultados
     */
    @Test
    @WithMockUser(roles = "JEFE")
    void getGenerationHistoryBySemester_WithNoResults_ShouldReturnEmptyList() throws Exception {
        // Arrange
        String semester = "2023-1";
        when(simonFileService.getGenerationHistoryBySemester(semester))
            .thenReturn(Collections.emptyList());

        // Act & Assert
        mockMvc.perform(get("/simon/history/{semester}", semester)
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(0)));

        verify(simonFileService).getGenerationHistoryBySemester(semester);
    }

    /**
     * Caso 14: Error al obtener historial por semestre
     */
    @Test
    @WithMockUser(roles = "JEFE")
    void getGenerationHistoryBySemester_WithException_ShouldReturn500() throws Exception {
        // Arrange
        String semester = "2024-2";
        when(simonFileService.getGenerationHistoryBySemester(semester))
            .thenThrow(new RuntimeException("Error de base de datos"));

        // Act & Assert
        mockMvc.perform(get("/simon/history/{semester}", semester)
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isInternalServerError());

        verify(simonFileService).getGenerationHistoryBySemester(semester);
    }

    /**
     * Caso 15: Verificación de nombre de archivo generado
     * El nombre debe incluir semestre y timestamp
     */
    @Test
    @WithMockUser(roles = "JEFE")
    void generateSimonFile_ShouldIncludeSemesterAndTimestampInFilename() throws Exception {
        // Arrange
        String semester = "2025-1";
        when(simonFileService.generateSimonFile(anyString(), eq(semester)))
            .thenReturn(mockWorkbook);

        // Act & Assert
        mockMvc.perform(get("/simon/generate")
                .param("semester", semester))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", 
                    matchesRegex(".*SIMON_2025-1_\\d{8}_\\d{6}\\.xlsx.*")));
    }

    /**
     * Caso 16: Verificación de Content-Length en respuesta
     */
    @Test
    @WithMockUser(roles = "JEFE")
    void generateSimonFile_ShouldIncludeContentLength() throws Exception {
        // Arrange
        when(simonFileService.generateSimonFile(anyString(), anyString()))
            .thenReturn(mockWorkbook);

        // Act & Assert
        mockMvc.perform(get("/simon/generate"))
                .andExpect(status().isOk())
                .andExpect(header().exists("Content-Length"));
    }

    /**
     * Caso 17: Generación con parámetros personalizados
     */
    @Test
    @WithMockUser(roles = "JEFE")
    void generateSimonFile_WithCustomParameters_ShouldUseProvidedValues() throws Exception {
        // Arrange
        String customUser = "jefe_departamento";
        String customSemester = "2025-1";
        
        when(simonFileService.generateSimonFile(customUser, customSemester))
            .thenReturn(mockWorkbook);

        // Act & Assert
        mockMvc.perform(get("/simon/generate")
                .param("generatedBy", customUser)
                .param("semester", customSemester))
                .andExpect(status().isOk());

        verify(simonFileService).generateSimonFile(customUser, customSemester);
    }
}

