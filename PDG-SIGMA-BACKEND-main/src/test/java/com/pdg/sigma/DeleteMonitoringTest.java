package com.pdg.sigma;

import com.pdg.sigma.domain.Monitoring;
import com.pdg.sigma.domain.MonitoringApprovalStatus;
import com.pdg.sigma.repository.MonitoringMonitorRepository;
import com.pdg.sigma.repository.MonitoringRepository;
import com.pdg.sigma.service.MonitoringServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;

/**
 * HU-498: Pruebas del soft-delete de monitorías.
 * El método deleteMonitoring ya NO borra el registro de la BD;
 * cambia el approvalStatus a ANULADA.
 */
@ExtendWith(MockitoExtension.class)
class DeleteMonitoringTest {

    @Mock
    private MonitoringRepository monitoringRepository;

    @Mock
    private MonitoringMonitorRepository monitoringMonitorRepository;

    @InjectMocks
    private MonitoringServiceImpl monitoringService;

    @Test
    void deleteMonitoring_shouldReturnFalse_whenMonitoringNotFound() {
        long id = 1L;
        Mockito.when(monitoringRepository.findById(id)).thenReturn(Optional.empty());

        boolean result = monitoringService.deleteMonitoring(id);

        assertFalse(result);
        Mockito.verify(monitoringRepository, never()).delete(any());
        Mockito.verify(monitoringRepository, never()).save(any());
    }

    @Test
    void deleteMonitoring_shouldReturnFalse_whenAlreadyAnulada() {
        long id = 2L;
        Monitoring monitoring = new Monitoring();
        monitoring.setApprovalStatus(MonitoringApprovalStatus.ANULADA);
        Mockito.when(monitoringRepository.findById(id)).thenReturn(Optional.of(monitoring));

        boolean result = monitoringService.deleteMonitoring(id);

        assertFalse(result);
        Mockito.verify(monitoringRepository, never()).delete(any());
        Mockito.verify(monitoringRepository, never()).save(any());
    }

    @Test
    void deleteMonitoring_shouldSetStatusAnulada_andNotDeleteFromDB() {
        long id = 3L;
        Monitoring monitoring = new Monitoring();
        monitoring.setApprovalStatus(MonitoringApprovalStatus.APROBADA);
        Mockito.when(monitoringRepository.findById(id)).thenReturn(Optional.of(monitoring));

        boolean result = monitoringService.deleteMonitoring(id);

        assertTrue(result);
        assertEquals(MonitoringApprovalStatus.ANULADA, monitoring.getApprovalStatus());
        Mockito.verify(monitoringRepository).save(monitoring);
        Mockito.verify(monitoringRepository, never()).delete(any());
    }

    @Test
    void deleteMonitoring_shouldAnnulPendingMonitoring() {
        long id = 4L;
        Monitoring monitoring = new Monitoring();
        monitoring.setApprovalStatus(MonitoringApprovalStatus.PENDIENTE_APROBACION);
        Mockito.when(monitoringRepository.findById(id)).thenReturn(Optional.of(monitoring));

        boolean result = monitoringService.deleteMonitoring(id);

        assertTrue(result);
        assertEquals(MonitoringApprovalStatus.ANULADA, monitoring.getApprovalStatus());
        Mockito.verify(monitoringRepository).save(monitoring);
        Mockito.verify(monitoringRepository, never()).delete(any());
    }
}
