package com.pdg.sigma;

import com.pdg.sigma.controller.AttendanceController;
import com.pdg.sigma.domain.Activity;
import com.pdg.sigma.domain.Attendance;
import com.pdg.sigma.domain.Student;
import com.pdg.sigma.service.AttendanceServiceImpl;
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
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = AttendanceController.class,
        excludeAutoConfiguration = {
                SecurityAutoConfiguration.class,
                SecurityFilterAutoConfiguration.class,
                UserDetailsServiceAutoConfiguration.class,
                OAuth2ResourceServerAutoConfiguration.class
        })
@AutoConfigureMockMvc(addFilters = false)
class AttendanceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AttendanceServiceImpl attendanceService;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Test
    void getAllAttendances_returnsList() throws Exception {
        Attendance a = new Attendance();
        a.setId(1);
        when(attendanceService.findAll()).thenReturn(List.of(a));

        mockMvc.perform(get("/attendance/getA"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1));
    }

    @Test
    void getAttendancesByActivity_returnsList() throws Exception {
        Attendance a = new Attendance();
        a.setId(1);
        when(attendanceService.findByActivity(1)).thenReturn(List.of(a));

        mockMvc.perform(get("/attendance/activity/{activityId}", 1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1));
    }

    @Test
    void getAttendancesByActivity_null_returnsEmptyList() throws Exception {
        when(attendanceService.findByActivity(1)).thenReturn(null);

        mockMvc.perform(get("/attendance/activity/{activityId}", 1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void createAttendance_returnsCreated() throws Exception {
        Attendance a = new Attendance();
        a.setId(1);
        Activity act = new Activity();
        act.setId(1);
        Student s = new Student();
        s.setCode("S001");
        a.setActivity(act);
        a.setStudent(s);

        when(attendanceService.save(any(Attendance.class))).thenReturn(a);

        mockMvc.perform(post("/attendance/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"activity\":{\"id\":1},\"student\":{\"code\":\"S001\"}}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void createAttendance_noActivity_returns400() throws Exception {
        mockMvc.perform(post("/attendance/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"student\":{\"code\":\"S001\"}}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("La actividad no puede ser nula."));
    }

    @Test
    void createAttendance_noStudent_returns400() throws Exception {
        mockMvc.perform(post("/attendance/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"activity\":{\"id\":1}}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("El estudiante no puede ser nulo."));
    }

    @Test
    void createAttendance_dataIntegrityViolation_returns409() throws Exception {
        when(attendanceService.save(any(Attendance.class)))
                .thenThrow(new org.springframework.dao.DataIntegrityViolationException("duplicado"));

        mockMvc.perform(post("/attendance/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"activity\":{\"id\":1},\"student\":{\"code\":\"S001\"}}"))
                .andExpect(status().isConflict())
                .andExpect(content().string("Error: La asistencia ya existe."));
    }

    @Test
    void checkAttendanceExists_found_returnsTrue() throws Exception {
        when(attendanceService.findByActivityAndStudent(1, "S001"))
                .thenReturn(Optional.of(new Attendance()));

        mockMvc.perform(get("/attendance/check/{activityId}/{studentId}", 1, "S001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(true));
    }

    @Test
    void checkAttendanceExists_notFound_returnsFalse() throws Exception {
        when(attendanceService.findByActivityAndStudent(1, "S001"))
                .thenReturn(Optional.empty());

        mockMvc.perform(get("/attendance/check/{activityId}/{studentId}", 1, "S001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(false));
    }

    @Test
    void deleteAttendance_found_returns204() throws Exception {
        Attendance a = new Attendance();
        a.setId(1);
        when(attendanceService.findByActivityAndStudent(1, "S001"))
                .thenReturn(Optional.of(a));
        doNothing().when(attendanceService).delete(a);

        mockMvc.perform(delete("/attendance/delete/{activityId}/{studentId}", 1, "S001"))
                .andExpect(status().isNoContent());
    }

    @Test
    void deleteAttendance_notFound_returns404() throws Exception {
        when(attendanceService.findByActivityAndStudent(1, "S001"))
                .thenReturn(Optional.empty());

        mockMvc.perform(delete("/attendance/delete/{activityId}/{studentId}", 1, "S001"))
                .andExpect(status().isNotFound());
    }
}
