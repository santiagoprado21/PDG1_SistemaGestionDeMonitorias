package com.pdg.sigma.repository;

import com.pdg.sigma.domain.ProfessorSurveySemesterQuestion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProfessorSurveySemesterQuestionRepository extends JpaRepository<ProfessorSurveySemesterQuestion, Long> {
    List<ProfessorSurveySemesterQuestion> findBySemesterConfigIdOrderByDisplayOrderAsc(Long semesterConfigId);
    List<ProfessorSurveySemesterQuestion> findBySemesterConfigIdAndActiveTrueOrderByDisplayOrderAsc(Long semesterConfigId);
    void deleteBySemesterConfigId(Long semesterConfigId);
}

