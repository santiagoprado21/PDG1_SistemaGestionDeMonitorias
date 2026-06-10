package com.pdg.sigma;

import com.pdg.sigma.domain.Program;
import com.pdg.sigma.domain.School;
import com.pdg.sigma.dto.ProgramDTO;
import com.pdg.sigma.repository.ProgramRepository;
import com.pdg.sigma.service.ProgramServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ProgramServiceImplTest {

    @Mock
    private ProgramRepository programRepository;

    @InjectMocks
    private ProgramServiceImpl programService;

    private Program mockProgram;
    private School mockSchool;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        mockSchool = new School();
        mockSchool.setId(1L);
        mockSchool.setName("Facultad de Ingeniería");

        mockProgram = new Program();
        mockProgram.setId(1L);
        mockProgram.setName("Ingeniería de Sistemas");
        mockProgram.setSchool(mockSchool);
    }

    @Test
    @DisplayName("Debe listar todos los programas como DTO")
    void testFindAll() {
        when(programRepository.findAll()).thenReturn(List.of(mockProgram));

        List<ProgramDTO> result = programService.findAll();

        assertEquals(1, result.size());
        assertEquals("Ingeniería de Sistemas", result.get(0).getName());
    }

    @Test
    @DisplayName("Debe buscar programas por escuela")
    void testFindBySchoolName() {
        when(programRepository.findAll()).thenReturn(List.of(mockProgram));

        ProgramDTO filter = new ProgramDTO(null, null, null);
        School school = new School();
        school.setId(1L);
        filter.setSchool(school);

        List<ProgramDTO> result = programService.findBySchoolName(filter);

        assertEquals(1, result.size());
        assertEquals("Ingeniería de Sistemas", result.get(0).getName());
    }

    @Test
    @DisplayName("Debe retornar vacío si no hay programas para la escuela")
    void testFindBySchoolNameNoMatch() {
        when(programRepository.findAll()).thenReturn(List.of(mockProgram));

        ProgramDTO filter = new ProgramDTO(null, null, null);
        School school = new School();
        school.setId(999L);
        filter.setSchool(school);

        List<ProgramDTO> result = programService.findBySchoolName(filter);

        assertTrue(result.isEmpty());
    }
}
