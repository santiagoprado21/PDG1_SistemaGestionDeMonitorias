package com.pdg.sigma.repository;

import com.pdg.sigma.domain.MonitorSurveySemesterQuestion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MonitorSurveySemesterQuestionRepository extends JpaRepository<MonitorSurveySemesterQuestion, Long> {
    List<MonitorSurveySemesterQuestion> findBySemesterConfigIdOrderByDisplayOrderAsc(Long semesterConfigId);
    List<MonitorSurveySemesterQuestion> findBySemesterConfigIdAndActiveTrueOrderByDisplayOrderAsc(Long semesterConfigId);
    void deleteBySemesterConfigId(Long semesterConfigId);
}
