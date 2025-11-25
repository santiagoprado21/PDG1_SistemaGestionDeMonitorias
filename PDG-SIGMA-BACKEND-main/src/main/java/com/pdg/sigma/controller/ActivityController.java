package com.pdg.sigma.controller;

import com.pdg.sigma.domain.Activity;
import com.pdg.sigma.domain.ActivityProgress;
import com.pdg.sigma.domain.Course;
import com.pdg.sigma.domain.HeadProgram;
import com.pdg.sigma.domain.Professor;
import com.pdg.sigma.dto.ActivityDTO;
import com.pdg.sigma.dto.ActivityProgressDTO;
import com.pdg.sigma.dto.ActivityProgressRequestDTO;
import com.pdg.sigma.dto.ActivityRequestDTO;
import com.pdg.sigma.dto.NewActivityRequestDTO;
import com.pdg.sigma.service.ActivityEvidenceStorageService;
import com.pdg.sigma.service.ActivityProgressService;
import com.pdg.sigma.service.ActivityService;
import com.pdg.sigma.service.DepartmentHeadServiceImpl;
import com.pdg.sigma.service.CourseServiceImpl;

import com.pdg.sigma.repository.CourseProfessorRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

//import com.pdg.sigma.dto.ActivityDTO;
//import com.pdg.sigma.service.ActivityServiceImpl;

@CrossOrigin(origins = {"http://localhost:3000", "https://pdg-sigma.vercel.app/"})
@RequestMapping("/activity")
@RestController
public class ActivityController {

    @Autowired
    private ActivityService activityService;

    @Autowired
    private CourseServiceImpl courseService;
    
    @Autowired
    private CourseProfessorRepository courseProfessorRepository;
    
    @Autowired
    private DepartmentHeadServiceImpl departmentHeadService;

    @Autowired
    private ActivityProgressService activityProgressService;

    @Autowired
    private ActivityEvidenceStorageService activityEvidenceStorageService;

    @RequestMapping(value = "/findAll/{userId}/{role}", method = RequestMethod.GET)
    public ResponseEntity<?> getActivitiesPerUser(@PathVariable String userId, @PathVariable String role) {
        try {
            List<ActivityDTO> activities = new ArrayList<>();

            if (role.equals("jfedpto")) {
                List<HeadProgram> headPrograms = departmentHeadService.getProgramsByDepartmentHead(userId);

                if (headPrograms.isEmpty()) {
                    return ResponseEntity.ok(Collections.emptyList());
                }

                List<Long> programIds = headPrograms.stream()
                        .map(headProgram -> headProgram.getProgram().getId())
                        .collect(Collectors.toList());

                List<Course> courses = courseService.findByProgramIds(programIds);

                if (courses.isEmpty()) {
                    return ResponseEntity.ok(Collections.emptyList());
                }

                List<Long> courseIds = courses.stream()
                        .map(Course::getId)
                        .collect(Collectors.toList());

                // profesores 
                List<Professor> professors = courseProfessorRepository.findProfessorsByCourseIds(courseIds);

                if (professors.isEmpty()) {
                    return ResponseEntity.ok(Collections.emptyList());
                }

                for (Professor professor : professors) {
                    List<ActivityDTO> professorActivities = activityService.findAll(professor.getId(), "professor");
                    // Filtrar actividades solo de los cursos del programa
                    List<ActivityDTO> filteredActivities = professorActivities.stream()
                    .filter(activity -> courseIds.contains(activity.getMonitoring().getCourse().getId()))
                    .collect(Collectors.toList());

                    activities.addAll(filteredActivities);
                }
            } else {
                activities = activityService.findAll(userId, role);
            }

            return ResponseEntity.status(200).body(activities);

        } catch (Exception e) {
            return ResponseEntity.status(404).body(e.getMessage());
        }
    }

    @PostMapping(value = "/create")
    public ResponseEntity<?> createActivity(@RequestBody NewActivityRequestDTO requestDTO) {
        try {
            ActivityDTO activity = activityService.save(requestDTO);
            return ResponseEntity.status(HttpStatus.CREATED).body(activity);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Error: " + e.getMessage());
        }
    }


    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteActivity(@PathVariable Integer id) {
        try {
            activityService.deleteById(id);
            return ResponseEntity.ok("Actividad eliminada");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }


    @PutMapping("/update")
    public ResponseEntity<?> updateActivity(@RequestBody ActivityRequestDTO updatedActivity) {
        try {
            ActivityDTO activity = activityService.update(updatedActivity);
            return ResponseEntity.ok(activity);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    @GetMapping(value = "/{id}")
    public ResponseEntity<?> getActivityById(@PathVariable Integer id) {
        Optional<Activity> activity = activityService.findById(id);

        return activity.isPresent() 
            ? ResponseEntity.ok(new ActivityDTO(activity.get())) 
            : ResponseEntity.status(HttpStatus.NOT_FOUND).body("Activity not found");
    }


    @RequestMapping(value = "/getA", method = RequestMethod.GET)
    public ResponseEntity<?> getAllActivities() {
        try {
            List<ActivityDTO> dtos = activityService.findAll()
                .stream()
                .map(ActivityDTO::new)
                .collect(Collectors.toList());

            if (!dtos.isEmpty()) {
                return ResponseEntity.ok(dtos);
            }
            return ResponseEntity.status(400).body("No hay actividades en la lista");
        } catch (Exception e) {
            return ResponseEntity.status(500).body(e.getMessage());
        }
    }


    @RequestMapping(value= "/updateState", method = RequestMethod.PUT)
    public ResponseEntity<?> setActivityState(@RequestBody String id){
        try{
            activityService.updateState(id);
            return ResponseEntity.status(200).body("Estado cambiado");

        }catch (Exception e){
            return ResponseEntity.status(500).body(e.getMessage());
        }

    }

    @PostMapping(value = "/{activityId}/progress", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> registerActivityProgress(
            @PathVariable Integer activityId,
            @RequestPart("payload") ActivityProgressRequestDTO payload,
            @RequestPart(value = "file", required = false) MultipartFile file) {
        try {
            ActivityProgress progress = activityProgressService.registerProgress(activityId, payload, file);
            ActivityProgressDTO dto = mapProgress(progress);
            return ResponseEntity.status(HttpStatus.CREATED).body(dto);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    @GetMapping(value = "/{activityId}/progress")
    public ResponseEntity<?> getActivityProgress(@PathVariable Integer activityId) {
        List<ActivityProgress> progressEntries = activityProgressService.findByActivity(activityId);
        List<ActivityProgressDTO> dtos = progressEntries.stream()
                .map(this::mapProgress)
                .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    @GetMapping(value = "/progress/{progressId}/evidence")
    public ResponseEntity<?> downloadEvidence(@PathVariable Long progressId) {
        try {
            ActivityProgress progress = activityProgressService.findById(progressId);
            if (progress.getEvidencePath() == null || progress.getEvidencePath().isBlank()) {
                return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
            }

            Resource resource = activityEvidenceStorageService.loadAsResource(progress.getEvidencePath());
            String contentDisposition = "attachment; filename=\"" +
                    URLEncoder.encode(progress.getEvidenceName(), StandardCharsets.UTF_8) + "\"";

            String contentType = null;
            try {
                contentType = Files.probeContentType(resource.getFile().toPath());
            } catch (IOException ignored) {
                // Usar el default cuando no se pueda determinar el tipo de contenido
            }

            MediaType mediaType = contentType != null ? MediaType.parseMediaType(contentType) : MediaType.APPLICATION_OCTET_STREAM;

            return ResponseEntity.ok()
                    .contentType(mediaType)
                    .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition)
                    .body(resource);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }

    private ActivityProgressDTO mapProgress(ActivityProgress progress) {
        ActivityProgressDTO dto = new ActivityProgressDTO(progress);
        if (progress.getEvidencePath() != null && !progress.getEvidencePath().isBlank()) {
            dto.setEvidenceUrl(buildEvidenceUrl(progress.getId()));
        }
        return dto;
    }

    private String buildEvidenceUrl(Long progressId) {
        return ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/activity/progress/")
                .path(progressId.toString())
                .path("/evidence")
                .toUriString();
    }


}
