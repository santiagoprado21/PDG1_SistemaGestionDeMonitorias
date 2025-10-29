package com.pdg.sigma.service;

import com.pdg.sigma.domain.MonitoringMonitor;
import com.pdg.sigma.dto.ApproveApplicationRequest;
import com.pdg.sigma.dto.MonitorDTO;

import java.util.List;

public interface MonitoringMonitorService extends GenericService<MonitoringMonitor, Long>{
	List<MonitorDTO> getMonitorsByMonitoringId(Long monitoringId);
	void deleteRelation(Long idMonitoring, String monitorCode) throws Exception;
	void updateApplicantSelectionStatus(Long monitoringId, String monitorCode, String newStatus);
	void approveApplication(ApproveApplicationRequest request) throws Exception;
	void rejectApplication(ApproveApplicationRequest request) throws Exception;
}
