package com.pdg.sigma.service;

import java.util.List;

import com.pdg.sigma.domain.Rubric;
import com.pdg.sigma.dto.CreateRubricRequest;
import com.pdg.sigma.dto.RubricDTO;

/**
 * Service interface para Rubric
 * HU-011: Creación de plan de actividades para monitores
 */
public interface RubricService {

    /**
     * Crea una nueva rúbrica
     */
    RubricDTO createRubric(CreateRubricRequest request) throws Exception;

    /**
     * Actualiza una rúbrica existente
     */
    RubricDTO updateRubric(Long id, CreateRubricRequest request) throws Exception;

    /**
     * Obtiene una rúbrica por ID
     */
    RubricDTO getRubricById(Long id) throws Exception;

    /**
     * Obtiene todas las rúbricas de un profesor
     */
    List<RubricDTO> getRubricsByProfessor(String professorId) throws Exception;

    /**
     * Obtiene todas las rúbricas
     */
    List<RubricDTO> getAllRubrics() throws Exception;

    /**
     * Busca rúbricas por nombre (parcial)
     */
    List<RubricDTO> searchRubricsByName(String name) throws Exception;

    /**
     * Elimina una rúbrica
     */
    void deleteRubric(Long id) throws Exception;

    /**
     * Verifica si existe una rúbrica con un nombre para un profesor
     */
    boolean existsByNameAndProfessor(String name, String professorId);

    /**
     * Convierte Rubric entity a RubricDTO
     */
    RubricDTO toDTO(Rubric rubric);

    /**
     * Obtiene las rúbricas más recientes
     */
    List<RubricDTO> getRecentRubrics() throws Exception;
}

