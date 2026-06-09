package com.pdg.sigma;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pdg.sigma.controller.ActivityController;
import com.pdg.sigma.domain.*;
import com.pdg.sigma.dto.ActivityDTO;
import com.pdg.sigma.dto.ActivityProgressDTO;
import com.pdg.sigma.dto.ActivityProgressRequestDTO;
import com.pdg.sigma.dto.NewActivityRequestDTO;
import com.pdg.sigma.dto.ActivityRequestDTO;
import com.pdg.sigma.service.*;
import com.pdg.sigma.repository.CourseProfessorRepository;
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
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = ActivityController.class,
        excludeAutoConfiguration = {
                SecurityAutoConfiguration.class,
                SecurityFilterAutoConfiguration.class,
                UserDetailsServiceAutoConfiguration.class,
                OAuth2ResourceServerAutoConfiguration.class
        })
@AutoConfigureMockMvc(addFilters = false)
class ActivityControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ActivityService activityService;

    @MockBean
    private CourseServiceImpl courseService;

    @MockBean
    private CourseProfessorRepository courseProfessorRepository;

    @MockBean
    private DepartmentHeadServiceImpl departmentHeadService;

    @MockBean
    private ActivityProgressService activityProgressService;

    @MockBean
    private ActivityEvidenceStorageService activityEvidenceStorageService;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Test
    void getActivitiesPerUser_professor_returnsOk() throws Exception {
        ActivityDTO dto = new ActivityDTO();
        when(activityService.findAll("P001", "professor")).thenReturn(List.of(dto));

        mockMvc.perform(get("/activity/findAll/{userId}/{role}", "P001", "professor"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0]").exists());
    }

    @Test
    void getActivitiesPerUser_jfedpto_withPrograms_returnsOk() throws Exception {
        HeadProgram hp = new HeadProgram();
        Program p = new Program();
        p.setId(1L);
        hp.setProgram(p);

        Course c = new Course();
        c.setId(1L);

        Professor prof = new Professor();
        prof.setId("P001");

        ActivityDTO dto = new ActivityDTO();
        Monitoring m = new Monitoring();
        Course mc = new Course();
        mc.setId(1L);
        m.setCourse(mc);
        dto.setMonitoring(m);

        when(departmentHeadService.getProgramsByDepartmentHead("H001")).thenReturn(List.of(hp));
        when(courseService.findByProgramIds(List.of(1L))).thenReturn(List.of(c));
        when(courseProfessorRepository.findProfessorsByCourseIds(List.of(1L))).thenReturn(List.of(prof));
        when(activityService.findAll("P001", "professor")).thenReturn(List.of(dto));

        mockMvc.perform(get("/activity/findAll/{userId}/{role}", "H001", "jfedpto"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0]").exists());
    }

    @Test
    void getActivitiesPerUser_jfedpto_noPrograms_returnsEmpty() throws Exception {
        when(departmentHeadService.getProgramsByDepartmentHead("H001")).thenReturn(List.of());

        mockMvc.perform(get("/activity/findAll/{userId}/{role}", "H001", "jfedpto"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void createActivity_returnsCreated() throws Exception {
        ActivityDTO dto = new ActivityDTO();
        when(activityService.save(any(NewActivityRequestDTO.class))).thenReturn(dto);

        mockMvc.perform(post("/activity/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Activity 1\"}"))
                .andExpect(status().isCreated());
    }

    @Test
    void createActivity_throws_returnsBadRequest() throws Exception {
        when(activityService.save(any(NewActivityRequestDTO.class))).thenThrow(new RuntimeException("Error"));

        mockMvc.perform(post("/activity/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Activity 1\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void deleteActivity_returnsOk() throws Exception {
        mockMvc.perform(delete("/activity/{id}", 1))
                .andExpect(status().isOk())
                .andExpect(content().string("Actividad eliminada"));
    }

    @Test
    void deleteActivity_throws_returnsNotFound() throws Exception {
        doThrow(new RuntimeException("Not found")).when(activityService).deleteById(99);

        mockMvc.perform(delete("/activity/{id}", 99))
                .andExpect(status().isNotFound());
    }

    @Test
    void updateActivity_returnsOk() throws Exception {
        when(activityService.update(any(ActivityRequestDTO.class))).thenReturn(new ActivityDTO());

        mockMvc.perform(put("/activity/update")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"id\":1}"))
                .andExpect(status().isOk());
    }

    @Test
    void updateActivity_throws_returnsBadRequest() throws Exception {
        when(activityService.update(any(ActivityRequestDTO.class))).thenThrow(new RuntimeException("Error"));

        mockMvc.perform(put("/activity/update")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"id\":1}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getActivityById_found_returnsOk() throws Exception {
        Activity activity = new Activity();
        activity.setId(1);
        when(activityService.findById(1)).thenReturn(Optional.of(activity));

        mockMvc.perform(get("/activity/{id}", 1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void getActivityById_notFound_returns404() throws Exception {
        when(activityService.findById(99)).thenReturn(Optional.empty());

        mockMvc.perform(get("/activity/{id}", 99))
                .andExpect(status().isNotFound())
                .andExpect(content().string("Activity not found"));
    }

    @Test
    void getAllActivities_returnsOk() throws Exception {
        Activity activity = new Activity();
        activity.setId(1);
        when(activityService.findAll()).thenReturn(List.of(activity));

        mockMvc.perform(get("/activity/getA"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1));
    }

    @Test
    void getAllActivities_empty_returns400() throws Exception {
        when(activityService.findAll()).thenReturn(List.of());

        mockMvc.perform(get("/activity/getA"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("No hay actividades en la lista"));
    }

    @Test
    void getAllActivities_throws_returns500() throws Exception {
        when(activityService.findAll()).thenThrow(new RuntimeException("Error"));

        mockMvc.perform(get("/activity/getA"))
                .andExpect(status().isInternalServerError());
    }

    @Test
    void setActivityState_returnsOk() throws Exception {
        mockMvc.perform(put("/activity/updateState")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("\"1\""))
                .andExpect(status().isOk())
                .andExpect(content().string("Estado cambiado"));
    }

    @Test
    void setActivityState_throws_returns500() throws Exception {
        doThrow(new RuntimeException("Error")).when(activityService).updateState(anyString());

        mockMvc.perform(put("/activity/updateState")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("\"1\""))
                .andExpect(status().isInternalServerError());
    }

    @Test
    void registerActivityProgress_returnsCreated() throws Exception {
        ActivityProgress progress = new ActivityProgress();
        progress.setId(1L);

        MockMultipartFile payloadPart = new MockMultipartFile(
                "payload",
                "payload",
                MediaType.APPLICATION_JSON_VALUE,
                objectMapper.writeValueAsBytes(new ActivityProgressRequestDTO())
        );

        when(activityProgressService.registerProgress(any(), any(), any())).thenReturn(progress);
        when(activityProgressService.findById(1L)).thenReturn(progress);

        mockMvc.perform(multipart("/activity/{activityId}/progress", 1)
                        .file(payloadPart)
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isCreated());
    }

    @Test
    void registerActivityProgress_throws_returnsBadRequest() throws Exception {
        MockMultipartFile payloadPart = new MockMultipartFile(
                "payload",
                "payload",
                MediaType.APPLICATION_JSON_VALUE,
                "{}".getBytes()
        );

        when(activityProgressService.registerProgress(any(), any(), any()))
                .thenThrow(new RuntimeException("Error"));

        mockMvc.perform(multipart("/activity/{activityId}/progress", 1)
                        .file(payloadPart)
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getActivityProgress_returnsOk() throws Exception {
        ActivityProgress progress = new ActivityProgress();
        progress.setId(1L);
        when(activityProgressService.findByActivity(1)).thenReturn(List.of(progress));

        mockMvc.perform(get("/activity/{activityId}/progress", 1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1));
    }

    @Test
    void downloadEvidence_noPath_returns204() throws Exception {
        ActivityProgress progress = new ActivityProgress();
        progress.setId(1L);
        progress.setEvidencePath(null);
        when(activityProgressService.findById(1L)).thenReturn(progress);

        mockMvc.perform(get("/activity/progress/{progressId}/evidence", 1L))
                .andExpect(status().isNoContent());
    }

    @Test
    void downloadEvidence_withPath_returnsOk() throws Exception {
        ActivityProgress progress = new ActivityProgress();
        progress.setId(1L);
        progress.setEvidencePath("/path/file.pdf");
        progress.setEvidenceName("file.pdf");
        when(activityProgressService.findById(1L)).thenReturn(progress);
        when(activityEvidenceStorageService.loadAsResource("/path/file.pdf"))
                .thenReturn(new ByteArrayResource("data".getBytes()));

        mockMvc.perform(get("/activity/progress/{progressId}/evidence", 1L))
                .andExpect(status().isOk());
    }

    @Test
    void downloadEvidence_throws_returnsNotFound() throws Exception {
        when(activityProgressService.findById(99L)).thenThrow(new RuntimeException("Not found"));

        mockMvc.perform(get("/activity/progress/{progressId}/evidence", 99L))
                .andExpect(status().isNotFound());
    }
}
