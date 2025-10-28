package com.pdg.sigma;

import com.pdg.sigma.domain.*;
import com.pdg.sigma.dto.SimonMonitoringDTO;
import com.pdg.sigma.repository.MonitoringMonitorRepository;
import com.pdg.sigma.repository.SimonFileGenerationRepository;
import com.pdg.sigma.service.SimonFileServiceImpl;
import org.apache.poi.ss.usermodel.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Pruebas unitarias para SimonFileServiceImpl
 * HU-004: Generación de archivo SIMON
 * 
 * Casos de prueba:
 * 1. Generación exitosa del archivo con monitorías aprobadas
 * 2. Error cuando no hay monitorías aprobadas
 * 3. Verificación de estructura del Excel (headers y datos)
 * 4. Registro de la generación en la base de datos
 * 5. Obtención del historial de generaciones
 * 6. Filtrado de historial por semestre
 * 7. Mapeo correcto de datos a DTO
 * 8. Cálculo correcto de semanas entre fechas
 */
@ExtendWith(MockitoExtension.class)
class SimonFileServiceImplTest {

    @Mock
    private MonitoringMonitorRepository monitoringMonitorRepository;

    @Mock
    private SimonFileGenerationRepository simonFileGenerationRepository;

    @InjectMocks
    private SimonFileServiceImpl simonFileService;

    private List<MonitoringMonitor> mockApprovedMonitorings;
    private Monitor mockMonitor;
    private Monitoring mockMonitoring;
    private Course mockCourse;
    private Professor mockProfessor;

    @BeforeEach
    void setUp() throws Exception {
        // Crear datos de prueba
        mockMonitor = new Monitor();
        mockMonitor.setIdMonitor("1234567890");
        mockMonitor.setCode("2220001");
        mockMonitor.setName("Juan");
        mockMonitor.setLastName("Pérez");
        mockMonitor.setEmail("juan.perez@universidad.edu.co");

        mockCourse = new Course();
        mockCourse.setId(9704L);
        mockCourse.setName("Programación Orientada a Objetos");

        mockProfessor = new Professor();
        mockProfessor.setId("1001");
        mockProfessor.setName("Dr. Carlos Rodríguez");

        mockMonitoring = new Monitoring();
        mockMonitoring.setId(1L);
        mockMonitoring.setCourse(mockCourse);
        mockMonitoring.setProfessor(mockProfessor);
        
        // Fechas de ejemplo: 14 semanas de monitoría
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        mockMonitoring.setStart(sdf.parse("2024-08-01"));
        mockMonitoring.setFinish(sdf.parse("2024-11-01"));

        MonitoringMonitor mockMM = new MonitoringMonitor();
        mockMM.setId(1L);
        mockMM.setMonitor(mockMonitor);
        mockMM.setMonitoring(mockMonitoring);
        mockMM.setEstadoSeleccion("aprobado");

        mockApprovedMonitorings = Arrays.asList(mockMM);
    }

    /**
     * Caso 1: Generación exitosa del archivo con monitorías aprobadas
     * Verifica que el archivo se genera correctamente y se registra en la BD
     */
    @Test
    void generateSimonFile_WithApprovedMonitorings_ShouldGenerateFileSuccessfully() throws IOException {
        // Arrange
        String generatedBy = "coordinador";
        String semester = "2024-2";
        
        when(monitoringMonitorRepository.findByEstadoSeleccion("aprobado"))
            .thenReturn(mockApprovedMonitorings);
        when(simonFileGenerationRepository.save(any(SimonFileGeneration.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        Workbook workbook = simonFileService.generateSimonFile(generatedBy, semester);

        // Assert
        assertNotNull(workbook, "El workbook no debe ser nulo");
        
        Sheet sheet = workbook.getSheetAt(0);
        assertNotNull(sheet, "La hoja no debe ser nula");
        assertEquals("Monitorías SIMON", sheet.getSheetName());
        
        // Verificar que se guardó el registro
        ArgumentCaptor<SimonFileGeneration> captor = ArgumentCaptor.forClass(SimonFileGeneration.class);
        verify(simonFileGenerationRepository).save(captor.capture());
        
        SimonFileGeneration savedGeneration = captor.getValue();
        assertEquals(generatedBy, savedGeneration.getGeneratedBy());
        assertEquals(semester, savedGeneration.getSemester());
        assertEquals(1, savedGeneration.getTotalMonitorings());
        assertTrue(savedGeneration.getFileName().contains("SIMON_2024-2_"));
        
        workbook.close();
    }

    /**
     * Caso 2: Error cuando no hay monitorías aprobadas
     * Debe lanzar IllegalStateException
     */
    @Test
    void generateSimonFile_WithNoApprovedMonitorings_ShouldThrowException() {
        // Arrange
        String generatedBy = "coordinador";
        String semester = "2024-2";
        
        when(monitoringMonitorRepository.findByEstadoSeleccion("aprobado"))
            .thenReturn(Collections.emptyList());

        // Act & Assert
        IllegalStateException exception = assertThrows(
            IllegalStateException.class,
            () -> simonFileService.generateSimonFile(generatedBy, semester)
        );

        assertEquals("No hay monitorías aprobadas para generar el archivo", exception.getMessage());
        
        // Verificar que NO se intentó guardar ningún registro
        verify(simonFileGenerationRepository, never()).save(any());
    }

    /**
     * Caso 3: Verificación de estructura del Excel
     * Comprueba que los headers y datos estén correctamente formateados
     */
    @Test
    void generateSimonFile_ShouldHaveCorrectExcelStructure() throws IOException {
        // Arrange
        when(monitoringMonitorRepository.findByEstadoSeleccion("aprobado"))
            .thenReturn(mockApprovedMonitorings);
        when(simonFileGenerationRepository.save(any(SimonFileGeneration.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        Workbook workbook = simonFileService.generateSimonFile("coordinador", "2024-2");
        Sheet sheet = workbook.getSheetAt(0);

        // Assert - Verificar headers
        Row headerRow = sheet.getRow(0);
        assertNotNull(headerRow);
        
        String[] expectedHeaders = {
            "Estudiante de PRE/POS",
            "CENCO",
            "No.Monitoria",
            "NOMBRE",
            "APELLIDO",
            "CÉDULA",
            "CÓDIGO DE ESTUDIANTE",
            "EMAIL",
            "NÚMERO DE CELULAR\nDAVIPLATA",
            "Código curso",
            "CURSO O PROYECTO",
            "DESCRIPCIÓN MONITORÍA",
            "FECHA\nINICIO",
            "FECHA\nFIN",
            "TOTAL\nHORAS",
            "Total Semanas",
            "Profesor que solicita la monitoria"
        };

        for (int i = 0; i < expectedHeaders.length; i++) {
            Cell cell = headerRow.getCell(i);
            assertNotNull(cell, "Header en columna " + i + " no debe ser nulo");
            assertEquals(expectedHeaders[i], cell.getStringCellValue());
        }

        // Verificar datos de la primera fila
        Row dataRow = sheet.getRow(1);
        assertNotNull(dataRow);
        
        assertEquals("PRE", dataRow.getCell(0).getStringCellValue());
        assertEquals("CA021202", dataRow.getCell(1).getStringCellValue());
        assertEquals("Juan", dataRow.getCell(3).getStringCellValue());
        assertEquals("Pérez", dataRow.getCell(4).getStringCellValue());
        assertEquals("1234567890", dataRow.getCell(5).getStringCellValue());
        assertEquals("2220001", dataRow.getCell(6).getStringCellValue());
        assertEquals("juan.perez@universidad.edu.co", dataRow.getCell(7).getStringCellValue());
        assertEquals("TIC - 9704", dataRow.getCell(9).getStringCellValue());
        assertEquals("Programación Orientada a Objetos", dataRow.getCell(10).getStringCellValue());
        
        workbook.close();
    }

    /**
     * Caso 4: Obtención de monitorías aprobadas en formato DTO
     * Verifica que los datos se mapeen correctamente al DTO
     */
    @Test
    void getApprovedMonitoringsForSimon_ShouldReturnCorrectDTOs() {
        // Arrange
        when(monitoringMonitorRepository.findByEstadoSeleccion("aprobado"))
            .thenReturn(mockApprovedMonitorings);

        // Act
        List<SimonMonitoringDTO> result = simonFileService.getApprovedMonitoringsForSimon();

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        
        SimonMonitoringDTO dto = result.get(0);
        assertEquals("PRE", dto.getEstudianteTipo());
        assertEquals("CA021202", dto.getCenco());
        assertEquals("Juan", dto.getNombre());
        assertEquals("Pérez", dto.getApellido());
        assertEquals("1234567890", dto.getCedula());
        assertEquals("2220001", dto.getCodigoEstudiante());
        assertEquals("juan.perez@universidad.edu.co", dto.getEmail());
        assertEquals("TIC - 9704", dto.getCodigoCurso());
        assertEquals("Programación Orientada a Objetos", dto.getNombreCurso());
        assertEquals("NRC-1", dto.getDescripcionMonitoria());
        assertEquals("01/08/2024", dto.getFechaInicio());
        assertEquals("01/11/2024", dto.getFechaFin());
        assertEquals(30, dto.getTotalHoras());
        assertEquals(13, dto.getTotalSemanas()); // ~13 semanas entre agosto y noviembre
        assertEquals("Dr. Carlos Rodríguez", dto.getProfesorSolicita());
    }

    /**
     * Caso 5: Múltiples monitorías aprobadas
     * Verifica que se procesen correctamente múltiples registros
     */
    @Test
    void getApprovedMonitoringsForSimon_WithMultipleMonitorings_ShouldReturnAllDTOs() throws Exception {
        // Arrange - Crear segundo monitor y monitoría
        Monitor monitor2 = new Monitor();
        monitor2.setIdMonitor("0987654321");
        monitor2.setCode("2220002");
        monitor2.setName("María");
        monitor2.setLastName("González");
        monitor2.setEmail("maria.gonzalez@universidad.edu.co");

        Course course2 = new Course();
        course2.setId(9705L);
        course2.setName("Estructuras de Datos");

        Monitoring monitoring2 = new Monitoring();
        monitoring2.setId(2L);
        monitoring2.setCourse(course2);
        monitoring2.setProfessor(mockProfessor);
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        monitoring2.setStart(sdf.parse("2024-08-15"));
        monitoring2.setFinish(sdf.parse("2024-11-15"));

        MonitoringMonitor mm2 = new MonitoringMonitor();
        mm2.setId(2L);
        mm2.setMonitor(monitor2);
        mm2.setMonitoring(monitoring2);
        mm2.setEstadoSeleccion("aprobado");

        List<MonitoringMonitor> multipleMonitorings = Arrays.asList(
            mockApprovedMonitorings.get(0), 
            mm2
        );

        when(monitoringMonitorRepository.findByEstadoSeleccion("aprobado"))
            .thenReturn(multipleMonitorings);

        // Act
        List<SimonMonitoringDTO> result = simonFileService.getApprovedMonitoringsForSimon();

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        
        // Verificar primer monitor
        SimonMonitoringDTO dto1 = result.get(0);
        assertEquals("Juan", dto1.getNombre());
        assertEquals("2220001", dto1.getCodigoEstudiante());
        
        // Verificar segundo monitor
        SimonMonitoringDTO dto2 = result.get(1);
        assertEquals("María", dto2.getNombre());
        assertEquals("2220002", dto2.getCodigoEstudiante());
        assertEquals("Estructuras de Datos", dto2.getNombreCurso());
    }

    /**
     * Caso 6: Obtención del historial de generaciones
     */
    @Test
    void getGenerationHistory_ShouldReturnAllGenerations() {
        // Arrange
        SimonFileGeneration gen1 = new SimonFileGeneration(LocalDateTime.now(), "coordinador", 5, "SIMON_2024-2_20241101_120000.xlsx", "2024-2");
        gen1.setId(1L);
        SimonFileGeneration gen2 = new SimonFileGeneration(LocalDateTime.now(), "jefe", 3, "SIMON_2024-1_20240601_100000.xlsx", "2024-1");
        gen2.setId(2L);
        List<SimonFileGeneration> mockHistory = Arrays.asList(gen1, gen2);
        
        when(simonFileGenerationRepository.findAllByOrderByGeneratedAtDesc())
            .thenReturn(mockHistory);

        // Act
        List<SimonFileGeneration> result = simonFileService.getGenerationHistory();

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals(5, result.get(0).getTotalMonitorings());
        assertEquals("coordinador", result.get(0).getGeneratedBy());
        
        verify(simonFileGenerationRepository).findAllByOrderByGeneratedAtDesc();
    }

    /**
     * Caso 7: Filtrado de historial por semestre
     */
    @Test
    void getGenerationHistoryBySemester_ShouldReturnFilteredGenerations() {
        // Arrange
        String semester = "2024-2";
        SimonFileGeneration gen1 = new SimonFileGeneration(LocalDateTime.now(), "coordinador", 5, "SIMON_2024-2_20241101_120000.xlsx", semester);
        gen1.setId(1L);
        SimonFileGeneration gen2 = new SimonFileGeneration(LocalDateTime.now(), "coordinador", 4, "SIMON_2024-2_20241015_140000.xlsx", semester);
        gen2.setId(2L);
        List<SimonFileGeneration> mockHistory = Arrays.asList(gen1, gen2);
        
        when(simonFileGenerationRepository.findBySemesterOrderByGeneratedAtDesc(semester))
            .thenReturn(mockHistory);

        // Act
        List<SimonFileGeneration> result = simonFileService.getGenerationHistoryBySemester(semester);

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        assertTrue(result.stream().allMatch(g -> semester.equals(g.getSemester())));
        
        verify(simonFileGenerationRepository).findBySemesterOrderByGeneratedAtDesc(semester);
    }

    /**
     * Caso 8: Verificación de formato de fechas
     */
    @Test
    void getApprovedMonitoringsForSimon_ShouldFormatDatesCorrectly() throws Exception {
        // Arrange
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        mockMonitoring.setStart(sdf.parse("2024-03-15"));
        mockMonitoring.setFinish(sdf.parse("2024-06-15"));
        
        when(monitoringMonitorRepository.findByEstadoSeleccion("aprobado"))
            .thenReturn(mockApprovedMonitorings);

        // Act
        List<SimonMonitoringDTO> result = simonFileService.getApprovedMonitoringsForSimon();

        // Assert
        SimonMonitoringDTO dto = result.get(0);
        assertEquals("15/03/2024", dto.getFechaInicio());
        assertEquals("15/06/2024", dto.getFechaFin());
    }

    /**
     * Caso 9: Generación con diferentes usuarios y semestres
     */
    @Test
    void generateSimonFile_WithDifferentParameters_ShouldUseCorrectValues() throws IOException {
        // Arrange
        String generatedBy = "jefe_departamento";
        String semester = "2025-1";
        
        when(monitoringMonitorRepository.findByEstadoSeleccion("aprobado"))
            .thenReturn(mockApprovedMonitorings);
        when(simonFileGenerationRepository.save(any(SimonFileGeneration.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        Workbook workbook = simonFileService.generateSimonFile(generatedBy, semester);

        // Assert
        ArgumentCaptor<SimonFileGeneration> captor = ArgumentCaptor.forClass(SimonFileGeneration.class);
        verify(simonFileGenerationRepository).save(captor.capture());
        
        SimonFileGeneration savedGeneration = captor.getValue();
        assertEquals(generatedBy, savedGeneration.getGeneratedBy());
        assertEquals(semester, savedGeneration.getSemester());
        assertTrue(savedGeneration.getFileName().contains("SIMON_2025-1_"));
        
        workbook.close();
    }

    /**
     * Caso 10: Verificación de que el archivo contiene el número correcto de filas
     */
    @Test
    void generateSimonFile_ShouldHaveCorrectNumberOfRows() throws Exception {
        // Arrange - Crear 3 monitorías aprobadas
        List<MonitoringMonitor> threeMonitorings = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            Monitor m = new Monitor();
            m.setIdMonitor("123456789" + i);
            m.setCode("222000" + i);
            m.setName("Monitor" + i);
            m.setLastName("Apellido" + i);
            m.setEmail("monitor" + i + "@test.com");

            MonitoringMonitor mm = new MonitoringMonitor();
            mm.setId((long) i);
            mm.setMonitor(m);
            mm.setMonitoring(mockMonitoring);
            mm.setEstadoSeleccion("aprobado");
            
            threeMonitorings.add(mm);
        }

        when(monitoringMonitorRepository.findByEstadoSeleccion("aprobado"))
            .thenReturn(threeMonitorings);
        when(simonFileGenerationRepository.save(any(SimonFileGeneration.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        Workbook workbook = simonFileService.generateSimonFile("coordinador", "2024-2");
        Sheet sheet = workbook.getSheetAt(0);

        // Assert
        // 1 fila de headers + 3 filas de datos = 4 filas totales
        assertEquals(4, sheet.getPhysicalNumberOfRows());
        
        // Verificar que el registro guardado tenga el conteo correcto
        ArgumentCaptor<SimonFileGeneration> captor = ArgumentCaptor.forClass(SimonFileGeneration.class);
        verify(simonFileGenerationRepository).save(captor.capture());
        assertEquals(3, captor.getValue().getTotalMonitorings());
        
        workbook.close();
    }
}

