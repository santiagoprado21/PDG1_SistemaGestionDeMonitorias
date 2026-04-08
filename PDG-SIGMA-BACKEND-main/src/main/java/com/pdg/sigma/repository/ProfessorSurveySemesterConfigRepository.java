package com.pdg.sigma.repository;

import com.pdg.sigma.domain.ProfessorSurveySemesterConfig;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProfessorSurveySemesterConfigRepository extends JpaRepository<ProfessorSurveySemesterConfig, Long> {
    Optional<ProfessorSurveySemesterConfig> findBySemester(String semester);
    Optional<ProfessorSurveySemesterConfig> findFirstByActiveTrueOrderByUpdatedAtDesc();
    List<ProfessorSurveySemesterConfig> findAllByOrderByUpdatedAtDesc();
    List<ProfessorSurveySemesterConfig> findAllByTemplateId(Long templateId);
}

