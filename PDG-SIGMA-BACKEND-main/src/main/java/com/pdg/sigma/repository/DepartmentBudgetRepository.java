package com.pdg.sigma.repository;

import com.pdg.sigma.domain.DepartmentBudget;
import com.pdg.sigma.domain.Program;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DepartmentBudgetRepository extends JpaRepository<DepartmentBudget, Long> {
    Optional<DepartmentBudget> findByProgramAndSemester(Program program, String semester);
}
