package com.pdg.sigma.repository;

import com.pdg.sigma.domain.SupervisorEvaluationAnswer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SupervisorEvaluationAnswerRepository extends JpaRepository<SupervisorEvaluationAnswer, Long> {
    boolean existsByQuestionIdAndEvaluationSemester(Long questionId, String semester);
    List<SupervisorEvaluationAnswer> findByEvaluationIdOrderByDisplayOrderAsc(Long evaluationId);
}
