import React, { useState, useEffect } from 'react';
import './CerrarMonitorias.css';
import VerticalNavbar from './VerticalNavbar';
import { BACKEND_URL } from './config/ApiBackend';
import { PopUp } from './PopUp';

function CerrarMonitorias() {
    const userId = localStorage.getItem('userId');
    const [monitorings, setMonitorings] = useState([]);
    const [closedMonitorings, setClosedMonitorings] = useState([]);
    const [selectedMonitorings, setSelectedMonitorings] = useState([]);
    const [isLoading, setIsLoading] = useState(false);
    const [activeTab, setActiveTab] = useState('pendientes'); // 'pendientes' o 'cerradas'
    const [semester, setSemester] = useState('2026-1');
    
    // Modal de cierre
    const [showCloseModal, setShowCloseModal] = useState(false);
    const [closureComment, setClosureComment] = useState('');
    const [autoCalculate, setAutoCalculate] = useState(true);
    
    // Popup
    const [showPopup, setShowPopup] = useState(false);
    const [popupMessage, setPopupMessage] = useState('');
    
    // Reporte
    const [showReportModal, setShowReportModal] = useState(false);
    const [selectedReport, setSelectedReport] = useState(null);

    useEffect(() => {
        loadMonitorings();
    }, [semester, activeTab]);

    const loadMonitorings = async () => {
        setIsLoading(true);
        try {
            const endpoint = activeTab === 'pendientes' 
                ? `${BACKEND_URL}/monitoring-closure/ready-for-closure?semester=${semester}`
                : `${BACKEND_URL}/monitoring-closure/closed?semester=${semester}`;
            
            const response = await fetch(endpoint, {
                headers: {
                    'Authorization': localStorage.getItem('token')
                }
            });

            if (!response.ok) throw new Error('Error al cargar monitorías');
            
            const data = await response.json();
            
            if (activeTab === 'pendientes') {
                setMonitorings(data);
            } else {
                setClosedMonitorings(data);
            }
        } catch (error) {
            console.error('Error:', error);
            setPopupMessage('Error al cargar las monitorías: ' + error.message);
            setShowPopup(true);
        } finally {
            setIsLoading(false);
        }
    };

    const handleSelectMonitoring = (monitoringId) => {
        setSelectedMonitorings(prev => {
            if (prev.includes(monitoringId)) {
                return prev.filter(id => id !== monitoringId);
            } else {
                return [...prev, monitoringId];
            }
        });
    };

    const handleSelectAll = () => {
        if (selectedMonitorings.length === monitorings.length) {
            setSelectedMonitorings([]);
        } else {
            setSelectedMonitorings(monitorings.map(m => m.id));
        }
    };

    const handleOpenCloseModal = () => {
        if (selectedMonitorings.length === 0) {
            setPopupMessage('Debe seleccionar al menos una monitoría para cerrar');
            setShowPopup(true);
            return;
        }
        setShowCloseModal(true);
    };

    const handleCloseMonitorings = async () => {
        if (!closureComment.trim()) {
            setPopupMessage('Debe ingresar un comentario de cierre');
            setShowPopup(true);
            return;
        }

        setIsLoading(true);
        try {
            const closureRequest = {
                comment: closureComment,
                autoCalculate: autoCalculate
            };

            if (selectedMonitorings.length === 1) {
                // Cerrar individual
                const response = await fetch(`${BACKEND_URL}/monitoring-closure/${selectedMonitorings[0]}/close?directorId=${userId}`, {
                    method: 'POST',
                    headers: {
                        'Content-Type': 'application/json',
                        'Authorization': localStorage.getItem('token')
                    },
                    body: JSON.stringify(closureRequest)
                });

                if (!response.ok) {
                    const error = await response.text();
                    throw new Error(error);
                }
            } else {
                // Cerrar en lote
                const response = await fetch(`${BACKEND_URL}/monitoring-closure/close-batch`, {
                    method: 'POST',
                    headers: {
                        'Content-Type': 'application/json',
                        'Authorization': localStorage.getItem('token')
                    },
                    body: JSON.stringify({
                        monitoringIds: selectedMonitorings,
                        directorId: userId,
                        closureData: closureRequest
                    })
                });

                if (!response.ok) {
                    const error = await response.text();
                    throw new Error(error);
                }
            }

            setPopupMessage(`✅ Se ${selectedMonitorings.length === 1 ? 'cerró' : 'cerraron'} exitosamente ${selectedMonitorings.length} monitoría${selectedMonitorings.length > 1 ? 's' : ''}`);
            setShowPopup(true);
            setShowCloseModal(false);
            setClosureComment('');
            setSelectedMonitorings([]);
            loadMonitorings();
        } catch (error) {
            console.error('Error:', error);
            setPopupMessage('Error al cerrar monitorías: ' + error.message);
            setShowPopup(true);
        } finally {
            setIsLoading(false);
        }
    };

    const handleViewReport = async (monitoringId) => {
        setIsLoading(true);
        try {
            const response = await fetch(`${BACKEND_URL}/monitoring-closure/${monitoringId}/report`, {
                headers: {
                    'Authorization': localStorage.getItem('token')
                }
            });

            if (!response.ok) throw new Error('Error al cargar el reporte');
            
            const report = await response.json();
            setSelectedReport(report);
            setShowReportModal(true);
        } catch (error) {
            console.error('Error:', error);
            setPopupMessage('Error al cargar el reporte: ' + error.message);
            setShowPopup(true);
        } finally {
            setIsLoading(false);
        }
    };

    const getComplianceColor = (percentage) => {
        if (percentage >= 90) return '#4cb979'; // Verde
        if (percentage >= 70) return '#e4eb60'; // Naranja
        return '#e9683b'; // Rojo
    };

    return (
        <div className="cerrar-monitorias-container">
            <VerticalNavbar />
            <div className="main-content">
                <h1>🔒 Cierre de Monitorías</h1>
                <p className="subtitle">Cierre de monitorías al final del semestre</p>

                {/* Selector de semestre */}
                <div className="filters-section">
                    <div className="filter-group">
                        <label>Semestre:</label>
                        <select value={semester} onChange={(e) => setSemester(e.target.value)}>
                            <option value="2026-1">2026-1</option>
                            <option value="2025-2">2025-2</option>
                            <option value="2025-1">2025-1</option>
                        </select>
                    </div>
                </div>

                {/* Tabs */}
                <div className="tabs-container">
                    <button 
                        className={`tab ${activeTab === 'pendientes' ? 'active' : ''}`}
                        onClick={() => setActiveTab('pendientes')}
                    >
                        📋 Listas para Cerrar ({monitorings.length})
                    </button>
                    <button 
                        className={`tab ${activeTab === 'cerradas' ? 'active' : ''}`}
                        onClick={() => setActiveTab('cerradas')}
                    >
                        🔒 Monitorías Cerradas ({closedMonitorings.length})
                    </button>
                </div>

                {isLoading ? (
                    <div className="loading">Cargando...</div>
                ) : (
                    <>
                        {activeTab === 'pendientes' ? (
                            <>
                                {/* Acciones de cierre */}
                                {monitorings.length > 0 && (
                                    <div className="actions-bar">
                                        <label className="checkbox-container">
                                            <input 
                                                type="checkbox" 
                                                checked={selectedMonitorings.length === monitorings.length && monitorings.length > 0}
                                                onChange={handleSelectAll}
                                            />
                                            <span>Seleccionar todas</span>
                                        </label>
                                        <button 
                                            className="btn-primary"
                                            onClick={handleOpenCloseModal}
                                            disabled={selectedMonitorings.length === 0}
                                        >
                                            🔒 Cerrar Seleccionadas ({selectedMonitorings.length})
                                        </button>
                                    </div>
                                )}

                                {/* Lista de monitorías listas para cerrar */}
                                {monitorings.length === 0 ? (
                                    <div className="empty-state">
                                        <p>📭 No hay monitorías listas para cerrar en este semestre</p>
                                        <small>Solo se pueden cerrar monitorías en estado "APROBADA"</small>
                                    </div>
                                ) : (
                                    <div className="monitorings-grid">
                                        {monitorings.map(monitoring => (
                                            <div key={monitoring.id} className="monitoring-card">
                                                <div className="card-header">
                                                    <input 
                                                        type="checkbox"
                                                        checked={selectedMonitorings.includes(monitoring.id)}
                                                        onChange={() => handleSelectMonitoring(monitoring.id)}
                                                    />
                                                    <h3>{monitoring.course?.name || 'Sin curso'}</h3>
                                                </div>
                                                <div className="card-body">
                                                    <p><strong>Programa:</strong> {monitoring.program?.name || 'N/A'}</p>
                                                    <p><strong>Profesor:</strong> {monitoring.professor?.name || 'N/A'}</p>
                                                    <p><strong>Monitor:</strong> {monitoring.assignedMonitor?.name || 'Sin asignar'}</p>
                                                    <p><strong>Horas contratadas:</strong> {monitoring.estimatedHours || 0}h</p>
                                                    <p><strong>Semestre:</strong> {monitoring.semester}</p>
                                                </div>
                                            </div>
                                        ))}
                                    </div>
                                )}
                            </>
                        ) : (
                            <>
                                {/* Lista de monitorías cerradas */}
                                {closedMonitorings.length === 0 ? (
                                    <div className="empty-state">
                                        <p>📭 No hay monitorías cerradas en este semestre</p>
                                    </div>
                                ) : (
                                    <div className="monitorings-grid">
                                        {closedMonitorings.map(monitoring => (
                                            <div key={monitoring.id} className="monitoring-card closed">
                                                <div className="card-header">
                                                    <h3>{monitoring.course?.name || 'Sin curso'}</h3>
                                                    <span className="badge-closed">🔒 Cerrada</span>
                                                </div>
                                                <div className="card-body">
                                                    <p><strong>Programa:</strong> {monitoring.program?.name || 'N/A'}</p>
                                                    <p><strong>Profesor:</strong> {monitoring.professor?.name || 'N/A'}</p>
                                                    <p><strong>Monitor:</strong> {monitoring.assignedMonitor?.name || 'Sin asignar'}</p>
                                                    <p><strong>Cerrada el:</strong> {new Date(monitoring.closureDate).toLocaleDateString('es-ES')}</p>
                                                    {monitoring.compliancePercentage !== null && (
                                                        <p>
                                                            <strong>Cumplimiento:</strong> 
                                                            <span style={{ 
                                                                color: getComplianceColor(monitoring.compliancePercentage),
                                                                fontWeight: 'bold',
                                                                marginLeft: '5px'
                                                            }}>
                                                                {monitoring.compliancePercentage}%
                                                            </span>
                                                        </p>
                                                    )}
                                                </div>
                                                <div className="card-footer">
                                                    <button 
                                                        className="btn-secondary"
                                                        onClick={() => handleViewReport(monitoring.id)}
                                                    >
                                                        📊 Ver Reporte
                                                    </button>
                                                </div>
                                            </div>
                                        ))}
                                    </div>
                                )}
                            </>
                        )}
                    </>
                )}

                {/* Modal de cierre */}
                {showCloseModal && (
                    <div className="modal-overlay" onClick={() => setShowCloseModal(false)}>
                        <div className="modal-content" onClick={(e) => e.stopPropagation()}>
                            <div className="modal-header">
                                <h2>🔒 Cerrar Monitorías</h2>
                                <button className="btn-close" onClick={() => setShowCloseModal(false)}>×</button>
                            </div>
                            <div className="modal-body">
                                <p>Va a cerrar <strong>{selectedMonitorings.length}</strong> monitoría{selectedMonitorings.length > 1 ? 's' : ''}.</p>
                                
                                <div className="form-group">
                                    <label>Comentario de Cierre *</label>
                                    <textarea
                                        value={closureComment}
                                        onChange={(e) => setClosureComment(e.target.value)}
                                        rows="4"
                                        placeholder="Ingrese un comentario sobre el cierre de la monitoría..."
                                        required
                                    />
                                </div>

                                <div className="form-group">
                                    <label className="checkbox-container">
                                        <input 
                                            type="checkbox"
                                            checked={autoCalculate}
                                            onChange={(e) => setAutoCalculate(e.target.checked)}
                                        />
                                        <span>Calcular automáticamente las métricas de cumplimiento</span>
                                    </label>
                                    <small className="help-text">
                                        {autoCalculate 
                                            ? '✅ El sistema calculará automáticamente las actividades completadas y horas trabajadas'
                                            : '⚠️ Deberá ingresar manualmente las métricas de cumplimiento'}
                                    </small>
                                </div>
                            </div>
                            <div className="modal-footer">
                                <button className="btn-secondary" onClick={() => setShowCloseModal(false)}>
                                    Cancelar
                                </button>
                                <button className="btn-primary" onClick={handleCloseMonitorings}>
                                    🔒 Cerrar Monitorías
                                </button>
                            </div>
                        </div>
                    </div>
                )}

                {/* Modal de reporte */}
                {showReportModal && selectedReport && (
                    <div className="modal-overlay" onClick={() => setShowReportModal(false)}>
                        <div className="modal-content modal-large" onClick={(e) => e.stopPropagation()}>
                            <div className="modal-header">
                                <h2>📊 Reporte de Cumplimiento</h2>
                                <button className="btn-close" onClick={() => setShowReportModal(false)}>×</button>
                            </div>
                            <div className="modal-body report-body">
                                <div className="report-section">
                                    <h3>Información General</h3>
                                    <p><strong>Curso:</strong> {selectedReport.courseName}</p>
                                    <p><strong>Programa:</strong> {selectedReport.programName}</p>
                                    <p><strong>Profesor:</strong> {selectedReport.professorName}</p>
                                    <p><strong>Monitor:</strong> {selectedReport.monitorName}</p>
                                    <p><strong>Semestre:</strong> {selectedReport.semester}</p>
                                </div>

                                <div className="report-section">
                                    <h3>Fechas</h3>
                                    <p><strong>Inicio:</strong> {selectedReport.startDate ? new Date(selectedReport.startDate).toLocaleDateString('es-ES') : 'N/A'}</p>
                                    <p><strong>Fin:</strong> {selectedReport.finishDate ? new Date(selectedReport.finishDate).toLocaleDateString('es-ES') : 'N/A'}</p>
                                    <p><strong>Fecha de cierre:</strong> {selectedReport.closureDate ? new Date(selectedReport.closureDate).toLocaleDateString('es-ES') : 'N/A'}</p>
                                </div>

                                <div className="report-section">
                                    <h3>Métricas de Cumplimiento</h3>
                                    <div className="metrics-grid">
                                        <div className="metric-card">
                                            <div className="metric-value" style={{ color: getComplianceColor(selectedReport.compliancePercentage) }}>
                                                {selectedReport.compliancePercentage}%
                                            </div>
                                            <div className="metric-label">Cumplimiento General</div>
                                        </div>
                                        <div className="metric-card">
                                            <div className="metric-value">{selectedReport.completedActivities} / {selectedReport.totalActivities}</div>
                                            <div className="metric-label">Actividades</div>
                                        </div>
                                        <div className="metric-card">
                                            <div className="metric-value">{selectedReport.actualHours}h / {selectedReport.estimatedHours}h</div>
                                            <div className="metric-label">Horas</div>
                                        </div>
                                    </div>
                                </div>

                                {selectedReport.totalBudgetUsed && (
                                    <div className="report-section">
                                        <h3>Presupuesto</h3>
                                        <p><strong>Monto total usado:</strong> ${selectedReport.totalBudgetUsed.toLocaleString()}</p>
                                        <p><strong>Tarifa horaria:</strong> ${selectedReport.hourlyRate?.toLocaleString() || 'N/A'}</p>
                                    </div>
                                )}

                                <div className="report-section">
                                    <h3>Auditoría</h3>
                                    <p><strong>Cerrada por:</strong> {selectedReport.closedBy}</p>
                                    <p><strong>Comentario:</strong> {selectedReport.closureComment}</p>
                                </div>
                            </div>
                            <div className="modal-footer">
                                <button className="btn-secondary" onClick={() => setShowReportModal(false)}>
                                    Cerrar
                                </button>
                            </div>
                        </div>
                    </div>
                )}

                {/* Popup */}
                <PopUp show={showPopup} onClose={() => setShowPopup(false)}>
                    <div style={{ textAlign: 'center', padding: '20px', whiteSpace: 'pre-line' }}>
                        {popupMessage}
                    </div>
                </PopUp>
            </div>
        </div>
    );
}

export default CerrarMonitorias;
