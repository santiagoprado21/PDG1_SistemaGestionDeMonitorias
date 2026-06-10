package com.pdg.sigma;

import com.pdg.sigma.controller.CourseController;
import com.pdg.sigma.domain.Course;
import com.pdg.sigma.dto.CourseDTO;
import com.pdg.sigma.service.CourseServiceImpl;
import com.pdg.sigma.util.JwtAuthenticationFilter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.oauth2.resource.servlet.OAuth2ResourceServerAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = CourseController.class,
        excludeAutoConfiguration = {
                SecurityAutoConfiguration.class,
                SecurityFilterAutoConfiguration.class,
                UserDetailsServiceAutoConfiguration.class,
                OAuth2ResourceServerAutoConfiguration.class
        })
@AutoConfigureMockMvc(addFilters = false)
class CourseControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CourseServiceImpl courseService;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Test
    void getCoursesPerProgram_returnsOk() throws Exception {
        CourseDTO dto = new CourseDTO("Test");
        dto.setId(1L);
        when(courseService.findByProgram(any(CourseDTO.class))).thenReturn(List.of(dto));

        mockMvc.perform(post("/course/getCoursesProgram")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"id\":1}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1));
    }

    @Test
    void getCoursesByProgram_found_returnsOk() throws Exception {
        Course c = new Course();
        c.setId(1L);
        when(courseService.findByProgramId(1L)).thenReturn(List.of(c));

        mockMvc.perform(get("/course/program/{programId}", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1));
    }

    @Test
    void getCoursesByProgram_empty_returns404() throws Exception {
        when(courseService.findByProgramId(1L)).thenReturn(List.of());

        mockMvc.perform(get("/course/program/{programId}", 1L))
                .andExpect(status().isNotFound());
    }

    @Test
    void getAll_returnsOk() throws Exception {
        CourseDTO dto = new CourseDTO("Test");
        dto.setId(1L);
        when(courseService.findAll()).thenReturn(List.of(dto));

        mockMvc.perform(get("/course/getA"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1));
    }

    @Test
    void getCoursesByProfessor_returnsOk() throws Exception {
        Course c = new Course();
        c.setId(1L);
        when(courseService.getCoursesByProfessorId("P001")).thenReturn(List.of(c));

        mockMvc.perform(get("/course/getCoursesByProfessor/{professorId}", "P001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1));
    }
}
