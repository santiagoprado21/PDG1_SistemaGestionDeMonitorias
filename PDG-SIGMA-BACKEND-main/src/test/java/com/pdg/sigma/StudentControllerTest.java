package com.pdg.sigma;

import com.pdg.sigma.controller.StudentController;
import com.pdg.sigma.domain.Student;
import com.pdg.sigma.domain.StudentCourse;
import com.pdg.sigma.service.StudentServiceImpl;
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
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = StudentController.class,
        excludeAutoConfiguration = {
                SecurityAutoConfiguration.class,
                SecurityFilterAutoConfiguration.class,
                UserDetailsServiceAutoConfiguration.class,
                OAuth2ResourceServerAutoConfiguration.class
        })
@AutoConfigureMockMvc(addFilters = false)
class StudentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private StudentServiceImpl studentService;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Test
    void getAllStudents_returnsList() throws Exception {
        Student s = new Student();
        s.setCode("S001");
        when(studentService.findAll()).thenReturn(List.of(s));

        mockMvc.perform(get("/student/getA"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].code").value("S001"));
    }

    @Test
    void getStudentByCode_found_returnsOk() throws Exception {
        Student s = new Student();
        s.setCode("S001");
        when(studentService.findById("S001")).thenReturn(Optional.of(s));

        mockMvc.perform(get("/student/{code}", "S001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("S001"));
    }

    @Test
    void getStudentByCode_notFound_returns404() throws Exception {
        when(studentService.findById("S001")).thenReturn(Optional.empty());

        mockMvc.perform(get("/student/{code}", "S001"))
                .andExpect(status().isNotFound());
    }

    @Test
    void getStudentsByCourse_returnsList() throws Exception {
        StudentCourse sc = new StudentCourse();
        mockMvc.perform(get("/student/course/{courseId}", 1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }
}
