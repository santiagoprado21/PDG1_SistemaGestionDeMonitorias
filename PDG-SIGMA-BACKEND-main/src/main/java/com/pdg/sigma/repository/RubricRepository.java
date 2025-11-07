package com.pdg.sigma.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.pdg.sigma.domain.Rubric;

/**
 * Repository para Rubric
 * HU-011: Creación de plan de actividades para monitores
 */
@Repository
public interface RubricRepository extends JpaRepository<Rubric, Long> {

    /**
     * Busca todas las rúbricas creadas por un profesor específico
     */
    @Query("SELECT r FROM Rubric r WHERE r.createdBy.id = :professorId ORDER BY r.createdAt DESC")
    List<Rubric> findByCreatedByProfessorId(@Param("professorId") String professorId);

    /**
     * Busca rúbricas por nombre (búsqueda parcial, case-insensitive)
     */
    @Query("SELECT r FROM Rubric r WHERE LOWER(r.name) LIKE LOWER(CONCAT('%', :name, '%')) ORDER BY r.name")
    List<Rubric> findByNameContainingIgnoreCase(@Param("name") String name);

    /**
     * Verifica si existe una rúbrica con un nombre específico para un profesor
     */
    @Query("SELECT COUNT(r) > 0 FROM Rubric r WHERE r.name = :name AND r.createdBy.id = :professorId")
    boolean existsByNameAndProfessorId(@Param("name") String name, @Param("professorId") String professorId);

    /**
     * Obtiene las rúbricas más recientes (últimas 10)
     */
    @Query("SELECT r FROM Rubric r ORDER BY r.createdAt DESC LIMIT 10")
    List<Rubric> findRecentRubrics();
}

