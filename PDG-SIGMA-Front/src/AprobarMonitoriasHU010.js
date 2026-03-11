import React, { useState, useEffect } from 'react';
import './AprobarMonitoriasHU010.css';
import VerticalNavbar from './VerticalNavbar';
import { PopUp } from "./PopUp";
import { BACKEND_URL } from './config/ApiBackend';
import LoadingSpinner from './LoadingSpinner';

function AprobarMonitoriasHU010() {
    const [monitorias, setMonitorias] = useState([]);
    const [filteredMonitorias, setFilteredMonitorias] = useState([]);
    const [currentPage, setCurrentPage] = useState(1);
    const [filterCourse, setFilterCourse] = useState("Todos");
    const recordsPerPage = 8;

    const [isOpen, setIsOpen] = useState(false);
    const [message, setMessage] = useState("");
    const [isLoading, setIsLoading] = useState(false);

    // Modal
    const [showModal, setShowModal] = useState(false);
    const [modalAction, setModalAction] = useState(null); // 'approve', 'reject' o 'modify'
    const [selectedMonitoria, setSelectedMonitoria] = useState(null);
    const [comentario, setComentario] = useState("");
    
    // Estado para modificación de convocatoria
    const [modifiedData, setModifiedData] = useState({
        requestedHours: '',
        justification: '',
        startDate: '',
        finishDate: '',
        requiredAverageGrade: '',
        requiredCourseGrade: '',
        hourlyRate: ''
    });

    const departmentHeadId = localStorage.getItem('userId');

    useEffect(() => {
        loadMonitorias();
    }, []);

    useEffect(() => {
        applyFilters();
    }, [monitorias, filterCourse]);

    const loadMonitorias = async () => {
        setIsLoading(true);
        try {
            // NUEVO FLUJO: Cargar convocatorias pendientes de aprobación INICIAL del jefe
            const response = await fetch(`${BACKEND_URL}/monitoring-request/pending-head-approval/${departmentHeadId}`, {
                method: 'GET',
                headers: {
                    'Content-Type': 'application/json',
                    'Authorization': localStorage.getItem('token')
                },
            });

            if (!response.ok) {
                throw new Error('Error al cargar convocatorias pendientes');
            }

            const data = await response.json();
            console.log('Convocatorias pendientes de aprobación cargadas:', data);
            console.log('Primera convocatoria:', data[0]);
            console.log('Campos disponibles:', data[0] ? Object.keys(data[0]) : 'Sin datos');
            setMonitorias(data || []);
        } catch (error) {
            console.error("Error loading convocatorias:", error);
            setMessage("Error cargando convocatorias: " + error.message);
            setIsOpen(true);
        } finally {
            setIsLoading(false);
        }
    };

    const applyFilters = () => {
        let filtered = [...monitorias];

        if (filterCourse !== "Todos") {
            filtered = filtered.filter(m => {
                const courseName = m.courseName || m.course?.name;
                return courseName === filterCourse;
            });
        }

        setFilteredMonitorias(filtered);
        setCurrentPage(1);
    };

    const uniqueCourses = ["Todos", ...new Set(monitorias.map(m => m.courseName || m.course?.name).filter(Boolean))];

    const openModal = (action, monitoria) => {
        setModalAction(action);
        setSelectedMonitoria(monitoria);
        setComentario("");
        
        // Si es modificar, pre-llenar con datos actuales
        if (action === 'modify') {
            setModifiedData({
                requestedHours: monitoria.requestedHours || '',
                justification: monitoria.justification || '',
                startDate: monitoria.startDate ? new Date(monitoria.startDate).toISOString().split('T')[0] : '',
                finishDate: monitoria.finishDate ? new Date(monitoria.finishDate).toISOString().split('T')[0] : '',
                requiredAverageGrade: monitoria.requiredAverageGrade || '',
                requiredCourseGrade: monitoria.requiredCourseGrade || '',
                hourlyRate: monitoria.hourlyRate || ''
            });
        }
        
        setShowModal(true);
    };

    const closeModal = () => {
        setShowModal(false);
        setModalAction(null);
        setSelectedMonitoria(null);
        setComentario("");
        setModifiedData({
            requestedHours: '',
            justification: '',
            startDate: '',
            finishDate: '',
            requiredAverageGrade: '',
            requiredCourseGrade: '',
            hourlyRate: ''
        });
    };

    const handleSubmitDecision = async () => {
        if (!comentario.trim()) {
            setMessage("Por favor ingrese un comentario");
            setIsOpen(true);
            return;
        }

        setIsLoading(true);
        
        let endpoint = '';
        let method = 'POST';
        let body = {};

        if (modalAction === 'approve') {
            // Aprobar sin modificar
            endpoint = `${BACKEND_URL}/monitoring-request/${selectedMonitoria.id}/approve-by-head`;
            body = {
                departmentHeadId: departmentHeadId,
                comment: comentario
            };
        } else if (modalAction === 'reject') {
            // Rechazar
            endpoint = `${BACKEND_URL}/monitoring-request/${selectedMonitoria.id}/reject-by-head`;
            body = {
                departmentHeadId: departmentHeadId,
                comment: comentario
            };
        } else if (modalAction === 'modify') {
            // Modificar y aprobar
            endpoint = `${BACKEND_URL}/monitoring-request/${selectedMonitoria.id}/modify-by-head`;
            method = 'PUT';
            body = {
                departmentHeadId: departmentHeadId,
                comment: comentario,
                requestedHours: parseInt(modifiedData.requestedHours),
                justification: modifiedData.justification,
                startDate: modifiedData.startDate,
                finishDate: modifiedData.finishDate,
                requiredAverageGrade: parseFloat(modifiedData.requiredAverageGrade),
                requiredCourseGrade: parseFloat(modifiedData.requiredCourseGrade),
                hourlyRate: parseFloat(modifiedData.hourlyRate)
            };
        }

        try {
            const response = await fetch(endpoint, {
                method: method,
                headers: {
                    'Content-Type': 'application/json',
                    'Authorization': localStorage.getItem('token')
                },
                body: JSON.stringify(body)
            });

            if (!response.ok) {
                const errorText = await response.text();
                throw new Error(errorText);
            }

            const result = await response.json();
            setMessage(result.message || `Convocatoria procesada exitosamente`);
            setIsOpen(true);
            closeModal();
            loadMonitorias(); // Recargar datos
        } catch (error) {
            console.error("Error al procesar decisión:", error);
            setMessage("Error: " + error.message);
            setIsOpen(true);
        } finally {
            setIsLoading(false);
        }
    };

    const handleClose = () => {
        setIsOpen(false);
    };

    // Paginación
    const indexOfLastRecord = currentPage * recordsPerPage;
    const indexOfFirstRecord = indexOfLastRecord - recordsPerPage;
    const currentRecords = filteredMonitorias.slice(indexOfFirstRecord, indexOfLastRecord);
    const totalPages = Math.ceil(filteredMonitorias.length / recordsPerPage);

    const formatDate = (dateString) => {
        if (!dateString) return "N/A";
        const date = new Date(dateString);
        return date.toLocaleDateString('es-ES');
    };

    return (
        <div className="aprobar-monitorias-hu010-container">
            <VerticalNavbar />
            
            <div className="main-content-aprobar-hu010">
                <div className="title-container-aprobar-hu010">
                    <div className="title-aprobar-hu010">Gestión de Convocatorias</div>
                    <div className="subtitle-aprobar-hu010">
                        Revisar, modificar y aprobar las convocatorias creadas por profesores
                    </div>
                </div>

                {/* Filtros */}
                <div className="filters-section-aprobar-hu010">
                    <div className="filter-group">
                        <label>Curso:</label>
                        <select value={filterCourse} onChange={(e) => setFilterCourse(e.target.value)}>
                            {uniqueCourses.map((course, idx) => (
                                <option key={idx}>{course}</option>
                            ))}
                        </select>
                    </div>
                    <div className="filter-stats">
                        <span>Total pendientes: {filteredMonitorias.length}</span>
                    </div>
                </div>

                {/* Tabla de monitorías */}
                {isLoading ? (
                    <LoadingSpinner />
                ) : (
                    <>
                        {currentRecords.length === 0 ? (
                            <div className="no-data-hu010">
                                <h3>No hay convocatorias pendientes de aprobación</h3>
                                <p>Todas las convocatorias creadas por profesores han sido procesadas.</p>
                            </div>
                        ) : (
                            <>
                                <div className="table-container-aprobar-hu010">
                                    <table className="monitorias-table-hu010">
                                        <thead>
                                            <tr>
                                                <th>ID</th>
                                                <th>Curso</th>
                                                <th>Programa</th>
                                                <th>Profesor</th>
                                                <th>Horas Solicitadas</th>
                                                <th>Período</th>
                                                <th>Justificación</th>
                                                <th>Acciones</th>
                                            </tr>
                                        </thead>
                                        <tbody>
                                            {currentRecords.map((monitoria) => {
                                                // Extraer datos
                                                const courseName = monitoria.courseName || 'N/A';
                                                const programName = monitoria.programName || 'N/A';
                                                const professorName = monitoria.professorName || 'N/A';
                                                const hours = monitoria.requestedHours || 'N/A';
                                                const startDate = monitoria.startDate;
                                                const endDate = monitoria.finishDate;
                                                
                                                return (
                                                <tr key={monitoria.id}>
                                                    <td>{monitoria.id}</td>
                                                    <td>{courseName}</td>
                                                    <td>{programName}</td>
                                                    <td>{professorName}</td>
                                                    <td><strong>{hours} hrs</strong></td>
                                                    <td>
                                                        {startDate && endDate ? (
                                                            <>
                                                                {formatDate(startDate)} - {formatDate(endDate)}
                                                            </>
                                                        ) : 'N/A'}
                                                    </td>
                                                    <td>
                                                        <div className="justification-preview">
                                                            {monitoria.justification?.substring(0, 60)}
                                                            {monitoria.justification?.length > 60 && '...'}
                                                        </div>
                                                    </td>
                                                    <td>
                                                        <div className="action-buttons-hu010">
                                                            <button 
                                                                className="btn-approve-hu010"
                                                                onClick={() => openModal('approve', monitoria)}
                                                                title="Aprobar sin cambios"
                                                            >
                                                                Aprobar
                                                            </button>
                                                            <button 
                                                                className="btn-modify-hu010"
                                                                onClick={() => openModal('modify', monitoria)}
                                                                title="Modificar y aprobar"
                                                            >
                                                                Modificar
                                                            </button>
                                                            <button 
                                                                className="btn-reject-hu010"
                                                                onClick={() => openModal('reject', monitoria)}
                                                                title="Rechazar convocatoria"
                                                            >
                                                                Rechazar
                                                            </button>
                                                        </div>
                                                    </td>
                                                </tr>
                                                );
                                            })}
                                        </tbody>
                                    </table>
                                </div>

                                {/* Paginación */}
                                {totalPages > 1 && (
                                    <div className="pagination-aprobar-hu010">
                                        <button 
                                            onClick={() => setCurrentPage(prev => Math.max(prev - 1, 1))}
                                            disabled={currentPage === 1}
                                        >
                                             Anterior
                                        </button>
                                        <span>Página {currentPage} de {totalPages}</span>
                                        <button 
                                            onClick={() => setCurrentPage(prev => Math.min(prev + 1, totalPages))}
                                            disabled={currentPage === totalPages}
                                        >
                                            Siguiente 
                                        </button>
                                    </div>
                                )}
                            </>
                        )}
                    </>
                )}
            </div>

            {/* Modal para aprobar/rechazar/modificar */}
            {showModal && selectedMonitoria && (
                <div className="modal-overlay-hu010" onClick={closeModal}>
                    <div className="modal-content-aprobar-hu010" onClick={(e) => e.stopPropagation()}>
                        <div className="modal-header-hu010">
                            <h2>
                                {modalAction === 'approve' && 'Aprobar Convocatoria'}
                                {modalAction === 'reject' && 'Rechazar Convocatoria'}
                                {modalAction === 'modify' && 'Modificar y Aprobar Convocatoria'}
                            </h2>
                            <button className="modal-close-hu010" onClick={closeModal}>×</button>
                        </div>
                        <div className="modal-body-hu010">
                            <div className="monitoria-details-hu010">
                                <h3>Detalles de la Convocatoria</h3>
                                <div className="detail-row">
                                    <span className="detail-label">Curso:</span>
                                    <span className="detail-value">
                                        {selectedMonitoria.courseName || 'N/A'}
                                    </span>
                                </div>
                                <div className="detail-row">
                                    <span className="detail-label">Profesor:</span>
                                    <span className="detail-value">
                                        {selectedMonitoria.professorName || 'N/A'}
                                    </span>
                                </div>
                                <div className="detail-row">
                                    <span className="detail-label">Programa:</span>
                                    <span className="detail-value">
                                        {selectedMonitoria.programName || 'N/A'}
                                    </span>
                                </div>
                                
                                {/* Campos editables o estáticos según la acción */}
                                {modalAction === 'modify' ? (
                                    <>
                                        <div className="detail-row">
                                            <span className="detail-label">Horas Solicitadas:*</span>
                                            <input 
                                                type="number" 
                                                className="input-modify-hu010"
                                                value={modifiedData.requestedHours}
                                                onChange={(e) => setModifiedData({...modifiedData, requestedHours: e.target.value})}
                                                min="1"
                                            />
                                        </div>
                                        <div className="detail-row">
                                            <span className="detail-label">Fecha Inicio:*</span>
                                            <input 
                                                type="date" 
                                                className="input-modify-hu010"
                                                value={modifiedData.startDate}
                                                onChange={(e) => setModifiedData({...modifiedData, startDate: e.target.value})}
                                            />
                                        </div>
                                        <div className="detail-row">
                                            <span className="detail-label">Fecha Fin:*</span>
                                            <input 
                                                type="date" 
                                                className="input-modify-hu010"
                                                value={modifiedData.finishDate}
                                                onChange={(e) => setModifiedData({...modifiedData, finishDate: e.target.value})}
                                            />
                                        </div>
                                        <div className="detail-row">
                                            <span className="detail-label">Promedio Mínimo:</span>
                                            <input 
                                                type="number" 
                                                className="input-modify-hu010"
                                                value={modifiedData.requiredAverageGrade}
                                                onChange={(e) => setModifiedData({...modifiedData, requiredAverageGrade: e.target.value})}
                                                step="0.1"
                                                min="0"
                                                max="5"
                                            />
                                        </div>
                                        <div className="detail-row">
                                            <span className="detail-label">Nota Curso Mínima:</span>
                                            <input 
                                                type="number" 
                                                className="input-modify-hu010"
                                                value={modifiedData.requiredCourseGrade}
                                                onChange={(e) => setModifiedData({...modifiedData, requiredCourseGrade: e.target.value})}
                                                step="0.1"
                                                min="0"
                                                max="5"
                                            />
                                        </div>
                                        <div className="detail-row">
                                            <span className="detail-label">Tarifa por Hora:</span>
                                            <input 
                                                type="number" 
                                                className="input-modify-hu010"
                                                value={modifiedData.hourlyRate}
                                                onChange={(e) => setModifiedData({...modifiedData, hourlyRate: e.target.value})}
                                                step="1000"
                                                min="0"
                                            />
                                        </div>
                                    </>
                                ) : (
                                    <>
                                        <div className="detail-row">
                                            <span className="detail-label">Horas Solicitadas:</span>
                                            <span className="detail-value">
                                                <strong>{selectedMonitoria.requestedHours || 'N/A'} horas</strong>
                                            </span>
                                        </div>
                                        <div className="detail-row">
                                            <span className="detail-label">Período:</span>
                                            <span className="detail-value">
                                                {selectedMonitoria.startDate && selectedMonitoria.finishDate
                                                    ? `${formatDate(selectedMonitoria.startDate)} - ${formatDate(selectedMonitoria.finishDate)}`
                                                    : 'N/A'}
                                            </span>
                                        </div>
                                    </>
                                )}
                            </div>
                            
                            <div className="justification-section-hu010">
                                <label><strong>Justificación del Profesor:</strong></label>
                                {modalAction === 'modify' ? (
                                    <textarea 
                                        className="textarea-modify-hu010"
                                        value={modifiedData.justification}
                                        onChange={(e) => setModifiedData({...modifiedData, justification: e.target.value})}
                                        rows="4"
                                        placeholder="Modificar justificación..."
                                    />
                                ) : (
                                    <div className="justification-full-hu010">
                                        {selectedMonitoria.justification || 'Sin justificación'}
                                    </div>
                                )}
                            </div>

                            <div className="form-group-hu010">
                                <label>Comentario*:</label>
                                <textarea 
                                    value={comentario}
                                    onChange={(e) => setComentario(e.target.value)}
                                    placeholder={
                                        modalAction === 'approve' 
                                            ? "Ingrese el motivo de aprobación o comentarios adicionales..."
                                            : modalAction === 'reject'
                                            ? "Ingrese el motivo del rechazo..."
                                            : "Ingrese comentario explicando las modificaciones realizadas..."
                                    }
                                    rows="4"
                                    className="textarea-comentario-hu010"
                                />
                                <small style={{ color: comentario.length > 0 ? 'green' : 'red' }}>
                                    {comentario.length === 0 ? 'El comentario es obligatorio' : 'Comentario válido'}
                                </small>
                            </div>

                            {modalAction === 'reject' && (
                                <div className="warning-box-hu010">
                                    <strong>Advertencia:</strong>
                                    <p>Al rechazar esta convocatoria:</p>
                                    <ul>
                                        <li>La convocatoria será marcada como RECHAZADA</li>
                                        <li>El profesor será notificado</li>
                                        <li>No se permitirán postulaciones</li>
                                        <li>El profesor deberá crear una nueva convocatoria si lo desea</li>
                                    </ul>
                                </div>
                            )}
                            
                            {modalAction === 'approve' && (
                                <div className="info-box-hu010">
                                    <strong>Información:</strong>
                                    <p>Al aprobar esta convocatoria:</p>
                                    <ul>
                                        <li>La convocatoria estará ABIERTA para postulaciones</li>
                                        <li>Los estudiantes podrán ver y postularse</li>
                                        <li>El profesor podrá seleccionar un monitor de los postulantes</li>
                                    </ul>
                                </div>
                            )}
                            
                            {modalAction === 'modify' && (
                                <div className="info-box-hu010">
                                    <strong>Modificación:</strong>
                                    <p>Al modificar y aprobar esta convocatoria:</p>
                                    <ul>
                                        <li>Se aplicarán los cambios realizados</li>
                                        <li>La convocatoria estará ABIERTA para postulaciones</li>
                                        <li>El profesor verá las modificaciones que realizó</li>
                                    </ul>
                                </div>
                            )}
                        </div>
                        <div className="modal-footer-hu010">
                            <button className="btn-cancel-hu010" onClick={closeModal}>Cancelar</button>
                            <button 
                                className={
                                    modalAction === 'approve' ? 'btn-confirm-approve-hu010' : 
                                    modalAction === 'reject' ? 'btn-confirm-reject-hu010' :
                                    'btn-confirm-modify-hu010'
                                }
                                onClick={handleSubmitDecision}
                                disabled={!comentario.trim()}
                            >
                                {modalAction === 'approve' && 'Confirmar Aprobación'}
                                {modalAction === 'reject' && 'Confirmar Rechazo'}
                                {modalAction === 'modify' && 'Modificar y Aprobar'}
                            </button>
                        </div>
                    </div>
                </div>
            )}

            <PopUp show={isOpen} onClose={handleClose}>
                <div style={{whiteSpace: 'pre-wrap'}}>{message}</div>
            </PopUp>
        </div>
    );
}

export default AprobarMonitoriasHU010;


