package com.pdg.sigma.repository;

import com.pdg.sigma.domain.MonitorSurveyResponseAnswer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface MonitorSurveyResponseAnswerRepository extends JpaRepository<MonitorSurveyResponseAnswer, Long> {
    boolean existsByQuestionIdAndResponseSemester(Long questionId, String semester);

    @Query("select a from MonitorSurveyResponseAnswer a " +
            "join fetch a.question q " +
            "join fetch a.response r " +
            "where r.id in :responseIds")
    List<MonitorSurveyResponseAnswer> findByResponseIdInWithDetails(@Param("responseIds") List<Long> responseIds);
}
