package com.pdg.sigma;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pdg.sigma.domain.Professor;
import com.pdg.sigma.domain.Rubric;
import com.pdg.sigma.domain.Rubric.RubricCriterion;
import com.pdg.sigma.dto.CreateRubricRequest;
import com.pdg.sigma.dto.RubricDTO;
import com.pdg.sigma.repository.ProfessorRepository;
import com.pdg.sigma.repository.RubricRepository;
import com.pdg.sigma.service.RubricServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Pruebas Unitarias para RubricService
 * HU-011: Gestión de rúbricas de evaluación
 */
class RubricServiceTest {

    @Mock
    private RubricRepository rubricRepository;

    @Mock
    private ProfessorRepository professorRepository;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private RubricServiceImpl rubricService;

    private Professor mockProfessor;
    private CreateRubricRequest mockRequest;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        mockProfessor = new Professor();
        mockProfessor.setId("1001");
        mockProfessor.setName("Dr. Test");

        // Crear request de rúbrica mock
        mockRequest = new CreateRubricRequest();
        mockRequest.setName("Evaluación de Tutoría");
        mockRequest.setDescription("Rúbrica para evaluar sesiones de tutoría");
        mockRequest.setProfessorId("1001");
        mockRequest.setTotalPoints(100);

        List<RubricCriterion> criteria = new ArrayList<>();
        
        RubricCriterion criterion1 = new RubricCriterion();
        criterion1.setCriterion("Puntualidad");
        criterion1.setDescription("Llega a tiempo");
        criterion1.setPoints(25);
        criteria.add(criterion1);

        RubricCriterion criterion2 = new RubricCriterion();
        criterion2.setCriterion("Dominio del tema");
        criterion2.setDescription("Conoce el contenido");
        criterion2.setPoints(75);
        criteria.add(criterion2);

        mockRequest.setCriteria(criteria);
    }

    @Test
    @DisplayName("Debe crear rúbrica correctamente")
    void testCreateRubric() throws Exception {
        // Given
        String mockJsonCriteria = "[{\"criterion\":\"Puntualidad\",\"points\":25,\"description\":\"Llega a tiempo\"},{\"criterion\":\"Dominio del tema\",\"points\":75,\"description\":\"Conoce el contenido\"}]";
        
        when(professorRepository.findById("1001")).thenReturn(Optional.of(mockProfessor));
        when(objectMapper.writeValueAsString(any())).thenReturn(mockJsonCriteria);
        when(rubricRepository.save(any(Rubric.class))).thenAnswer(i -> {
            Rubric rubric = i.getArgument(0);
            rubric.setId(1L);
            return rubric;
        });

        // When
        RubricDTO result = rubricService.createRubric(mockRequest);

        // Then
        assertNotNull(result);
        assertEquals("Evaluación de Tutoría", result.getName());
        assertEquals(100, result.getTotalPoints());
        assertEquals(2, result.getCriteria().size());
        verify(rubricRepository, times(1)).save(any(Rubric.class));
    }

    @Test
    @DisplayName("Debe actualizar rúbrica existente")
    void testUpdateRubric() throws Exception {
        // Given
        String mockJsonCriteria = "[{\"criterion\":\"Puntualidad\",\"points\":25,\"description\":\"Llega a tiempo\"},{\"criterion\":\"Dominio del tema\",\"points\":75,\"description\":\"Conoce el contenido\"}]";
        
        Rubric existingRubric = new Rubric();
        existingRubric.setId(1L);
        existingRubric.setName("Rúbrica Original");
        existingRubric.setCreatedBy(mockProfessor);

        mockRequest.setName("Rúbrica Actualizada");

        when(rubricRepository.findById(1L)).thenReturn(Optional.of(existingRubric));
        when(professorRepository.findById("1001")).thenReturn(Optional.of(mockProfessor));
        when(objectMapper.writeValueAsString(any())).thenReturn(mockJsonCriteria);
        when(rubricRepository.save(any(Rubric.class))).thenAnswer(i -> i.getArguments()[0]);

        // When
        RubricDTO result = rubricService.updateRubric(1L, mockRequest);

        // Then
        assertNotNull(result);
        assertEquals("Rúbrica Actualizada", result.getName());
        verify(rubricRepository, times(1)).save(any(Rubric.class));
    }

    @Test
    @DisplayName("Debe obtener rúbricas por profesor")
    void testGetRubricsByProfessor() throws Exception {
        // Given
        Rubric rubric1 = new Rubric();
        rubric1.setId(1L);
        rubric1.setName("Rúbrica 1");
        rubric1.setCreatedBy(mockProfessor);

        Rubric rubric2 = new Rubric();
        rubric2.setId(2L);
        rubric2.setName("Rúbrica 2");
        rubric2.setCreatedBy(mockProfessor);

        List<Rubric> rubrics = List.of(rubric1, rubric2);

        when(rubricRepository.findByCreatedByProfessorId("1001")).thenReturn(rubrics);

        // When
        List<RubricDTO> result = rubricService.getRubricsByProfessor("1001");

        // Then
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("Rúbrica 1", result.get(0).getName());
        assertEquals("Rúbrica 2", result.get(1).getName());
    }

    @Test
    @DisplayName("Debe eliminar rúbrica")
    void testDeleteRubric() throws Exception {
        // Given
        when(rubricRepository.existsById(1L)).thenReturn(true);
        doNothing().when(rubricRepository).deleteById(1L);

        // When
        rubricService.deleteRubric(1L);

        // Then
        verify(rubricRepository, times(1)).existsById(1L);
        verify(rubricRepository, times(1)).deleteById(1L);
    }

    @Test
    @DisplayName("Debe fallar al eliminar rúbrica inexistente")
    void testDeleteNonExistentRubric() {
        // Given
        when(rubricRepository.existsById(999L)).thenReturn(false);

        // When & Then
        Exception exception = assertThrows(Exception.class, () -> {
            rubricService.deleteRubric(999L);
        });

        assertTrue(exception.getMessage().contains("no encontrada"));
        verify(rubricRepository, never()).deleteById(any());
    }

    @Test
    @DisplayName("Debe validar puntos totales correctos")
    void testValidateTotalPoints() throws Exception {
        // Given
        String mockJsonCriteria = "[{\"criterion\":\"Puntualidad\",\"points\":25,\"description\":\"Llega a tiempo\"},{\"criterion\":\"Dominio del tema\",\"points\":75,\"description\":\"Conoce el contenido\"}]";
        
        mockRequest.setTotalPoints(100);
        // Los criterios suman 100 (25 + 75)

        when(professorRepository.findById("1001")).thenReturn(Optional.of(mockProfessor));
        when(objectMapper.writeValueAsString(any())).thenReturn(mockJsonCriteria);
        when(rubricRepository.save(any(Rubric.class))).thenAnswer(i -> {
            Rubric rubric = i.getArgument(0);
            rubric.setId(1L);
            return rubric;
        });

        // When
        RubricDTO result = rubricService.createRubric(mockRequest);

        // Then
        assertNotNull(result);
        assertEquals(100, result.getTotalPoints());
        
        // Verificar que la suma de criterios coincide
        int sum = result.getCriteria().stream()
                .mapToInt(RubricCriterion::getPoints)
                .sum();
        assertEquals(100, sum);
    }

    @Test
    @DisplayName("Debe buscar rúbrica por ID")
    void testGetRubricById() throws Exception {
        // Given
        Rubric rubric = new Rubric();
        rubric.setId(1L);
        rubric.setName("Test Rubric");
        rubric.setDescription("Description");
        rubric.setTotalPoints(100);
        rubric.setCreatedBy(mockProfessor);

        when(rubricRepository.findById(1L)).thenReturn(Optional.of(rubric));

        // When
        RubricDTO result = rubricService.getRubricById(1L);

        // Then
        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("Test Rubric", result.getName());
    }

    @Test
    @DisplayName("Debe fallar al buscar rúbrica inexistente")
    void testGetNonExistentRubric() {
        // Given
        when(rubricRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        Exception exception = assertThrows(Exception.class, () -> {
            rubricService.getRubricById(999L);
        });

        assertTrue(exception.getMessage().contains("no encontrada"));
    }

    @Test
    @DisplayName("Debe verificar existencia de rúbrica por nombre y profesor")
    void testExistsByNameAndProfessor() {
        // Given
        when(rubricRepository.existsByNameAndProfessorId("Test Rubric", "1001"))
                .thenReturn(true);

        // When
        boolean result = rubricService.existsByNameAndProfessor("Test Rubric", "1001");

        // Then
        assertTrue(result);
        verify(rubricRepository, times(1))
                .existsByNameAndProfessorId("Test Rubric", "1001");
    }
}

