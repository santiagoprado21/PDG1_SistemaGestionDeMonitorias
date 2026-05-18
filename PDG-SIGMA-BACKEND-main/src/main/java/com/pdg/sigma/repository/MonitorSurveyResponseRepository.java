package com.pdg.sigma.repository;

import com.pdg.sigma.domain.MonitorSurveyResponse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface MonitorSurveyResponseRepository extends JpaRepository<MonitorSurveyResponse, Long> {

    @Query("select r from MonitorSurveyResponse r " +
	    "where (:semester is null or r.semester = :semester) " +
	    "and (:monitorCode is null or r.monitorCode = :monitorCode) " +
	    "and (:monitoringId is null or r.monitoringId = :monitoringId) " +
	    "order by r.createdAt desc")
    List<MonitorSurveyResponse> findByFilters(@Param("semester") String semester,
					      @Param("monitorCode") String monitorCode,
					      @Param("monitoringId") String monitoringId);
}
