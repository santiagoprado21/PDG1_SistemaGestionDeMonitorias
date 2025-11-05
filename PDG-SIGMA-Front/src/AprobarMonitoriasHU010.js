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
    const [modalAction, setModalAction] = useState(null); // 'approve' o 'reject'
    const [selectedMonitoria, setSelectedMonitoria] = useState(null);
    const [comentario, setComentario] = useState("");

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
            const response = await fetch(`${BACKEND_URL}/monitoring/pending-approval`, {
                method: 'GET',
                headers: {
                    'Content-Type': 'application/json',
                    'Authorization': localStorage.getItem('token')
                },
            });

            if (!response.ok) {
                throw new Error('Error al cargar monitorías pendientes');
            }

            const data = await response.json();
            console.log('Monitorías cargadas:', data);
            console.log('Primera monitoría:', data[0]);
            console.log('Campos disponibles:', data[0] ? Object.keys(data[0]) : 'Sin datos');
            setMonitorias(data || []);
        } catch (error) {
            console.error("Error loading monitorias:", error);
            setMessage("Error cargando monitorías: " + error.message);
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
        setShowModal(true);
    };

    const closeModal = () => {
        setShowModal(false);
        setModalAction(null);
        setSelectedMonitoria(null);
        setComentario("");
    };

    const handleSubmitDecision = async () => {
        if (!comentario.trim()) {
            setMessage("Por favor ingrese un comentario");
            setIsOpen(true);
            return;
        }

        setIsLoading(true);
        const endpoint = modalAction === 'approve' 
            ? `${BACKEND_URL}/monitoring/approve/${selectedMonitoria.id}`
            : `${BACKEND_URL}/monitoring/reject/${selectedMonitoria.id}`;

        try {
            const response = await fetch(endpoint, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                    'Authorization': localStorage.getItem('token')
                },
                body: JSON.stringify({
                    departmentHeadId: departmentHeadId,
                    comment: comentario
                })
            });

            if (!response.ok) {
                const errorText = await response.text();
                throw new Error(errorText);
            }

            const result = await response.json();
            setMessage(result.message || `Monitoría ${modalAction === 'approve' ? 'aprobada' : 'rechazada'} exitosamente`);
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
                    <div className="title-aprobar-hu010">Aprobar Monitorías HU-010</div>
                    <div className="subtitle-aprobar-hu010">
                        Gestión de monitorías pendientes de aprobación (Nuevo Flujo)
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
                                <h3>✓ No hay monitorías pendientes de aprobación</h3>
                                <p>Todas las monitorías del nuevo flujo han sido procesadas.</p>
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
                                                <th>Monitor Asignado</th>
                                                <th>Horas</th>
                                                <th>Período</th>
                                                <th>Justificación</th>
                                                <th>Acciones</th>
                                            </tr>
                                        </thead>
                                        <tbody>
                                            {currentRecords.map((monitoria) => {
                                                // Extraer datos con diferentes posibles estructuras
                                                const courseName = monitoria.courseName || monitoria.course?.name || 'N/A';
                                                const programName = monitoria.programName || monitoria.program?.name || 'N/A';
                                                const professorName = monitoria.professorName || monitoria.professor?.name || 'N/A';
                                                const monitorName = monitoria.assignedMonitorName || monitoria.monitor?.name || 'Sin asignar';
                                                const hours = monitoria.estimatedHours || monitoria.totalHours || 'N/A';
                                                const startDate = monitoria.startDate || monitoria.initialDate;
                                                const endDate = monitoria.endDate || monitoria.finalDate;
                                                
                                                return (
                                                <tr key={monitoria.id}>
                                                    <td>{monitoria.id}</td>
                                                    <td>{courseName}</td>
                                                    <td>{programName}</td>
                                                    <td>{professorName}</td>
                                                    <td>
                                                        <strong>{monitorName}</strong>
                                                    </td>
                                                    <td>{hours} hrs</td>
                                                    <td>
                                                        {startDate && endDate ? (
                                                            <>
                                                                {formatDate(startDate)} - {formatDate(endDate)}
                                                            </>
                                                        ) : 'N/A'}
                                                    </td>
                                                    <td>
                                                        <div className="justification-preview">
                                                            {monitoria.justification?.substring(0, 80)}
                                                            {monitoria.justification?.length > 80 && '...'}
                                                        </div>
                                                    </td>
                                                    <td>
                                                        <div className="action-buttons-hu010">
                                                            <button 
                                                                className="btn-approve-hu010"
                                                                onClick={() => openModal('approve', monitoria)}
                                                            >
                                                                ✓ Aprobar
                                                            </button>
                                                            <button 
                                                                className="btn-reject-hu010"
                                                                onClick={() => openModal('reject', monitoria)}
                                                            >
                                                                ✗ Rechazar
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
                                            ← Anterior
                                        </button>
                                        <span>Página {currentPage} de {totalPages}</span>
                                        <button 
                                            onClick={() => setCurrentPage(prev => Math.min(prev + 1, totalPages))}
                                            disabled={currentPage === totalPages}
                                        >
                                            Siguiente →
                                        </button>
                                    </div>
                                )}
                            </>
                        )}
                    </>
                )}
            </div>

            {/* Modal para aprobar/rechazar */}
            {showModal && selectedMonitoria && (
                <div className="modal-overlay-hu010" onClick={closeModal}>
                    <div className="modal-content-aprobar-hu010" onClick={(e) => e.stopPropagation()}>
                        <div className="modal-header-hu010">
                            <h2>
                                {modalAction === 'approve' ? '✓ Aprobar Monitoría' : '✗ Rechazar Monitoría'}
                            </h2>
                            <button className="modal-close-hu010" onClick={closeModal}>×</button>
                        </div>
                        <div className="modal-body-hu010">
                            <div className="monitoria-details-hu010">
                                <h3>Detalles de la Monitoría</h3>
                                <div className="detail-row">
                                    <span className="detail-label">Curso:</span>
                                    <span className="detail-value">
                                        {selectedMonitoria.courseName || selectedMonitoria.course?.name || 'N/A'}
                                    </span>
                                </div>
                                <div className="detail-row">
                                    <span className="detail-label">Profesor:</span>
                                    <span className="detail-value">
                                        {selectedMonitoria.professorName || selectedMonitoria.professor?.name || 'N/A'}
                                    </span>
                                </div>
                                <div className="detail-row">
                                    <span className="detail-label">Monitor Asignado:</span>
                                    <span className="detail-value">
                                        <strong>
                                            {selectedMonitoria.assignedMonitorName || selectedMonitoria.monitor?.name || 'Sin asignar'}
                                        </strong>
                                    </span>
                                </div>
                                <div className="detail-row">
                                    <span className="detail-label">Horas Estimadas:</span>
                                    <span className="detail-value">
                                        {selectedMonitoria.estimatedHours || selectedMonitoria.totalHours || 'N/A'} horas
                                    </span>
                                </div>
                                <div className="detail-row">
                                    <span className="detail-label">Período:</span>
                                    <span className="detail-value">
                                        {(selectedMonitoria.startDate || selectedMonitoria.initialDate) && 
                                         (selectedMonitoria.endDate || selectedMonitoria.finalDate)
                                            ? `${formatDate(selectedMonitoria.startDate || selectedMonitoria.initialDate)} - ${formatDate(selectedMonitoria.endDate || selectedMonitoria.finalDate)}`
                                            : 'N/A'}
                                    </span>
                                </div>
                            </div>
                            
                            <div className="justification-section-hu010">
                                <label><strong>Justificación del Profesor:</strong></label>
                                <div className="justification-full-hu010">
                                    {selectedMonitoria.justification || 'Sin justificación'}
                                </div>
                            </div>

                            <div className="form-group-hu010">
                                <label>Comentario*:</label>
                                <textarea 
                                    value={comentario}
                                    onChange={(e) => setComentario(e.target.value)}
                                    placeholder={modalAction === 'approve' 
                                        ? "Ingrese el motivo de aprobación o comentarios adicionales..."
                                        : "Ingrese el motivo del rechazo..."
                                    }
                                    rows="4"
                                    className="textarea-comentario-hu010"
                                />
                                <small style={{ color: comentario.length > 0 ? 'green' : 'red' }}>
                                    {comentario.length === 0 ? 'El comentario es obligatorio' : '✓ Comentario válido'}
                                </small>
                            </div>

                            {modalAction === 'reject' && (
                                <div className="warning-box-hu010">
                                    <strong>⚠️ Advertencia:</strong>
                                    <p>Al rechazar esta monitoría:</p>
                                    <ul>
                                        <li>La convocatoria será marcada como RECHAZADA</li>
                                        <li>El monitor asignado será notificado</li>
                                        <li>El profesor deberá crear una nueva convocatoria si lo desea</li>
                                    </ul>
                                </div>
                            )}
                        </div>
                        <div className="modal-footer-hu010">
                            <button className="btn-cancel-hu010" onClick={closeModal}>Cancelar</button>
                            <button 
                                className={modalAction === 'approve' ? 'btn-confirm-approve-hu010' : 'btn-confirm-reject-hu010'}
                                onClick={handleSubmitDecision}
                                disabled={!comentario.trim()}
                            >
                                {modalAction === 'approve' ? 'Confirmar Aprobación' : 'Confirmar Rechazo'}
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

