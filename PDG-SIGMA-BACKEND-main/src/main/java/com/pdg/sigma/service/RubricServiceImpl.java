package com.pdg.sigma.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pdg.sigma.domain.Professor;
import com.pdg.sigma.domain.Rubric;
import com.pdg.sigma.dto.CreateRubricRequest;
import com.pdg.sigma.dto.RubricDTO;
import com.pdg.sigma.repository.ProfessorRepository;
import com.pdg.sigma.repository.RubricRepository;

/**
 * Implementación del servicio para Rubric
 * HU-011: Creación de plan de actividades para monitores
 */
@Service
public class RubricServiceImpl implements RubricService {

    @Autowired
    private RubricRepository rubricRepository;

    @Autowired
    private ProfessorRepository professorRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Override
    @Transactional
    public RubricDTO createRubric(CreateRubricRequest request) throws Exception {
        // Validar que el profesor existe
        Professor professor = professorRepository.findById(request.getProfessorId())
                .orElseThrow(() -> new Exception("Profesor no encontrado"));

        // Validar que no exista una rúbrica con el mismo nombre para este profesor
        if (rubricRepository.existsByNameAndProfessorId(request.getName(), request.getProfessorId())) {
            throw new Exception("Ya existe una rúbrica con este nombre");
        }

        // Validar que la suma de puntos de los criterios sea igual al total
        int sumPoints = request.getCriteria().stream()
                .mapToInt(c -> c.getPoints())
                .sum();
        
        if (sumPoints != request.getTotalPoints()) {
            throw new Exception("La suma de puntos de los criterios (" + sumPoints + ") no coincide con el total (" + request.getTotalPoints() + ")");
        }

        // Convertir criterios a JSON
        String criteriaJson = objectMapper.writeValueAsString(request.getCriteria());

        // Crear la rúbrica
        Rubric rubric = new Rubric(
            request.getName(),
            request.getDescription(),
            request.getTotalPoints(),
            criteriaJson,
            professor
        );

        rubric = rubricRepository.save(rubric);

        return toDTO(rubric);
    }

    @Override
    @Transactional
    public RubricDTO updateRubric(Long id, CreateRubricRequest request) throws Exception {
        // Buscar la rúbrica existente
        Rubric rubric = rubricRepository.findById(id)
                .orElseThrow(() -> new Exception("Rúbrica no encontrada"));

        // Validar que el profesor existe
        Professor professor = professorRepository.findById(request.getProfessorId())
                .orElseThrow(() -> new Exception("Profesor no encontrado"));

        // Validar que la suma de puntos de los criterios sea igual al total
        int sumPoints = request.getCriteria().stream()
                .mapToInt(c -> c.getPoints())
                .sum();
        
        if (sumPoints != request.getTotalPoints()) {
            throw new Exception("La suma de puntos de los criterios (" + sumPoints + ") no coincide con el total (" + request.getTotalPoints() + ")");
        }

        // Convertir criterios a JSON
        String criteriaJson = objectMapper.writeValueAsString(request.getCriteria());

        // Actualizar campos
        rubric.setName(request.getName());
        rubric.setDescription(request.getDescription());
        rubric.setTotalPoints(request.getTotalPoints());
        rubric.setCriteria(criteriaJson);
        rubric.setCreatedBy(professor);

        rubric = rubricRepository.save(rubric);

        return toDTO(rubric);
    }

    @Override
    public RubricDTO getRubricById(Long id) throws Exception {
        Rubric rubric = rubricRepository.findById(id)
                .orElseThrow(() -> new Exception("Rúbrica no encontrada"));
        return toDTO(rubric);
    }

    @Override
    public List<RubricDTO> getRubricsByProfessor(String professorId) throws Exception {
        List<Rubric> rubrics = rubricRepository.findByCreatedByProfessorId(professorId);
        return rubrics.stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<RubricDTO> getAllRubrics() throws Exception {
        List<Rubric> rubrics = rubricRepository.findAll();
        return rubrics.stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<RubricDTO> searchRubricsByName(String name) throws Exception {
        List<Rubric> rubrics = rubricRepository.findByNameContainingIgnoreCase(name);
        return rubrics.stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void deleteRubric(Long id) throws Exception {
        if (!rubricRepository.existsById(id)) {
            throw new Exception("Rúbrica no encontrada");
        }
        // Nota: Si hay actividades asociadas, la FK está configurada como SET NULL
        rubricRepository.deleteById(id);
    }

    @Override
    public boolean existsByNameAndProfessor(String name, String professorId) {
        return rubricRepository.existsByNameAndProfessorId(name, professorId);
    }

    @Override
    public List<RubricDTO> getRecentRubrics() throws Exception {
        List<Rubric> rubrics = rubricRepository.findRecentRubrics();
        return rubrics.stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    @Override
    public RubricDTO toDTO(Rubric rubric) {
        RubricDTO dto = new RubricDTO();
        dto.setId(rubric.getId());
        dto.setName(rubric.getName());
        dto.setDescription(rubric.getDescription());
        dto.setTotalPoints(rubric.getTotalPoints());
        dto.setCriteria(rubric.getCriteriaList());
        dto.setCreatedBy(rubric.getCreatedBy().getId());
        dto.setCreatedByName(rubric.getCreatedBy().getName());
        dto.setCreatedAt(rubric.getCreatedAt());
        dto.setUpdatedAt(rubric.getUpdatedAt());
        return dto;
    }
}

