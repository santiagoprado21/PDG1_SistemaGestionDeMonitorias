package com.pdg.sigma.repository;

import com.pdg.sigma.domain.Program;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProgramRepository extends JpaRepository<Program,Long> {
    public Optional<Program> findByName(String name);
    public Optional<Program> findByNameIgnoreCaseAndSchool(String name, com.pdg.sigma.domain.School school);
    public List<Program> findBySchool(com.pdg.sigma.domain.School school);
}
