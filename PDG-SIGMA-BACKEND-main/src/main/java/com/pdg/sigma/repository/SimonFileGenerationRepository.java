package com.pdg.sigma.repository;

import com.pdg.sigma.domain.SimonFileGeneration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SimonFileGenerationRepository extends JpaRepository<SimonFileGeneration, Long> {
    
    // Obtener historial ordenado por fecha descendente
    List<SimonFileGeneration> findAllByOrderByGeneratedAtDesc();
    
    // Obtener por semestre
    List<SimonFileGeneration> findBySemesterOrderByGeneratedAtDesc(String semester);
}

