package com.pdg.sigma.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.pdg.sigma.domain.CourseProfessor;
import com.pdg.sigma.domain.Professor;

@Repository
public interface CourseProfessorRepository extends JpaRepository<CourseProfessor, Integer> {

    @Query("SELECT cp FROM CourseProfessor cp WHERE cp.professor.id = :professorId")
    List<CourseProfessor> findByProfessor(@Param("professorId") String professorId);

    List<CourseProfessor> findByCourseId(Long courseId);

    @Query("SELECT cp.professor FROM CourseProfessor cp WHERE cp.course.id IN :courseId")
    List<Professor> findProfessorsByCourseId(@Param("courseId") List<Long> courseId);
}
