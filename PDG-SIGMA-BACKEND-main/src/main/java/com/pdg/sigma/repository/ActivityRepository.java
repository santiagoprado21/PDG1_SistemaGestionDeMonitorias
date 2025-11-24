package com.pdg.sigma.repository;

import com.pdg.sigma.domain.Activity;
import com.pdg.sigma.domain.Monitor;
import com.pdg.sigma.domain.Monitoring;
import com.pdg.sigma.domain.Professor;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalTime;
import java.util.Date;
import java.util.List;

@Repository
public interface ActivityRepository extends JpaRepository<Activity, Integer> {
    public List<Activity> findByMonitor(Monitor monitor);
    public List<Activity> findByMonitorAndRoleCreator(Monitor monitor, String role);
    public List<Activity> findByMonitorAndRoleResponsable(Monitor monitor, String role);

    public List<Activity> findByProfessorAndRoleCreator(Professor monitor, String role);
    public List<Activity> findByProfessorAndRoleResponsable(Professor monitor, String role);

    List<Activity> findByMonitoring(Monitoring monitoring);

    // ============================================================================
    // HU-011: Queries para horarios y plan de actividades
    // ============================================================================

    /**
     * Busca actividades de un monitor que tengan horarios definidos
     */
    @Query("SELECT a FROM Activity a WHERE a.monitor = :monitor AND a.startTime IS NOT NULL ORDER BY a.finish, a.startTime")
    List<Activity> findActivitiesWithScheduleByMonitor(@Param("monitor") Monitor monitor);

    /**
     * Busca actividades de un profesor que tengan horarios definidos
     */
    @Query("SELECT a FROM Activity a WHERE a.professor = :professor AND a.startTime IS NOT NULL ORDER BY a.finish, a.startTime")
    List<Activity> findActivitiesWithScheduleByProfessor(@Param("professor") Professor professor);

    /**
     * Busca actividades con conflictos de horario para un monitor en una fecha específica
     * Retorna actividades que se solapan con el rango de tiempo dado
     */
    @Query("SELECT a FROM Activity a WHERE a.monitor = :monitor " +
           "AND a.finish = :activityDate " +
           "AND a.startTime IS NOT NULL AND a.endTime IS NOT NULL " +
           "AND (:activityId IS NULL OR a.id != :activityId) " +
           "AND ((a.startTime < :endTime AND a.endTime > :startTime))")
    List<Activity> findScheduleConflicts(
        @Param("monitor") Monitor monitor, 
        @Param("activityDate") Date activityDate,
        @Param("startTime") LocalTime startTime,
        @Param("endTime") LocalTime endTime,
        @Param("activityId") Integer activityId
    );

    /**
     * Busca actividades por rango de fechas para un monitor (para vista de calendario)
     */
    @Query("SELECT a FROM Activity a WHERE a.monitor = :monitor " +
           "AND a.finish BETWEEN :startDate AND :endDate " +
           "ORDER BY a.finish, a.startTime")
    List<Activity> findByMonitorAndDateRange(
        @Param("monitor") Monitor monitor,
        @Param("startDate") Date startDate,
        @Param("endDate") Date endDate
    );

    /**
     * Busca actividades por rango de fechas para un profesor (para vista de calendario)
     */
    @Query("SELECT a FROM Activity a WHERE a.professor = :professor " +
           "AND a.finish BETWEEN :startDate AND :endDate " +
           "ORDER BY a.finish, a.startTime")
    List<Activity> findByProfessorAndDateRange(
        @Param("professor") Professor professor,
        @Param("startDate") Date startDate,
        @Param("endDate") Date endDate
    );

    /**
     * Busca actividades por monitoría (para plan de actividades de una monitoría)
     */
    @Query("SELECT a FROM Activity a WHERE a.monitoring = :monitoring ORDER BY a.finish, a.startTime")
    List<Activity> findByMonitoringOrderedBySchedule(@Param("monitoring") Monitoring monitoring);

    /**
     * Busca actividades con rúbrica asignada
     */
    @Query("SELECT a FROM Activity a WHERE a.rubric IS NOT NULL AND a.monitoring = :monitoring")
    List<Activity> findActivitiesWithRubricByMonitoring(@Param("monitoring") Monitoring monitoring);

}
