package com.pdg.sigma.service;

import com.pdg.sigma.domain.School;
import com.pdg.sigma.dto.SchoolDTO;
import com.pdg.sigma.repository.SchoolRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SchoolServiceImplTest {

    @Mock
    private SchoolRepository schoolRepository;

    @InjectMocks
    private SchoolServiceImpl schoolService;

    @Test
    void findAll_returnsSchoolDTOList() {
        School school = new School();
        school.setId(1L);
        school.setName("Ingeniería");
        when(schoolRepository.findAll()).thenReturn(List.of(school));

        List<SchoolDTO> result = schoolService.findAll();

        assertEquals(1, result.size());
        assertEquals(1L, result.get(0).getId());
        assertEquals("Ingeniería", result.get(0).getName());
    }

    @Test
    void findAll_emptyList() {
        when(schoolRepository.findAll()).thenReturn(List.of());

        List<SchoolDTO> result = schoolService.findAll();

        assertTrue(result.isEmpty());
    }

    @Test
    void findById_returnsEmpty() {
        assertTrue(schoolService.findById(1L).isEmpty());
    }

    @Test
    void save_returnsNull() throws Exception {
        assertNull(schoolService.save(new SchoolDTO(1L, "Test")));
    }

    @Test
    void update_returnsNull() throws Exception {
        assertNull(schoolService.update(new SchoolDTO(1L, "Test")));
    }

    @Test
    void delete_doesNothing() throws Exception {
        schoolService.delete(new SchoolDTO(1L, "Test"));
    }

    @Test
    void deleteById_doesNothing() throws Exception {
        schoolService.deleteById(1L);
    }

    @Test
    void validate_doesNothing() throws Exception {
        schoolService.validate(new SchoolDTO(1L, "Test"));
    }

    @Test
    void count_returnsNull() {
        assertNull(schoolService.count());
    }
}
