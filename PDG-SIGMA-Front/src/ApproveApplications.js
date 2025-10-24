import './ApproveApplications.css';
import React, { useState, useEffect } from 'react';
import VerticalNavbar from './VerticalNavbar';
import { PopUp } from "./PopUp";
import { BACKEND_URL } from './config/ApiBackend';
import LoadingSpinner from './LoadingSpinner';

function ApproveApplications() {
    const [applications, setApplications] = useState([]);
    const [filteredApplications, setFilteredApplications] = useState([]);
    const [currentPage, setCurrentPage] = useState(1);
    const [filterStatus, setFilterStatus] = useState("Todos");
    const [filterCourse, setFilterCourse] = useState("Todos");
    const recordsPerPage = 8;

    const [isOpen, setIsOpen] = useState(false);
    const [message, setMessage] = useState("");
    const [isLoading, setIsLoading] = useState(false);

    const [showModal, setShowModal] = useState(false);
    const [modalAction, setModalAction] = useState(null); // 'approve' o 'reject'
    const [selectedApplication, setSelectedApplication] = useState(null);
    const [comentario, setComentario] = useState("");

    const departmentHeadId = localStorage.getItem('userId');

    useEffect(() => {
        loadApplications();
    }, []);

    useEffect(() => {
        applyFilters();
    }, [applications, filterStatus, filterCourse]);

    const loadApplications = async () => {
        setIsLoading(true);
        try {
            const response = await fetch(`${BACKEND_URL}/department-head/${departmentHeadId}/pending-applications`, {
                method: 'GET',
                headers: {
                    'Content-Type': 'application/json',
                    'Authorization': localStorage.getItem('token')
                },
            });

            if (!response.ok) {
                throw new Error('Error al cargar postulaciones');
            }

            const data = await response.json();
            setApplications(data);
            setIsLoading(false);
        } catch (error) {
            console.error("Error loading applications:", error);
            setMessage("Error cargando postulaciones: " + error.message);
            setIsOpen(true);
            setIsLoading(false);
        }
    };

    const applyFilters = () => {
        let filtered = [...applications];

        // Filtro por estado
        if (filterStatus !== "Todos") {
            filtered = filtered.filter(app => {
                if (filterStatus === "Pendiente") {
                    // "Pendiente" = postulaciones que el profesor seleccionó pero el jefe aún no ha procesado
                    return app.estadoSeleccion === "seleccionado";
                } else if (filterStatus === "Aprobado") {
                    return app.estadoSeleccion === "aprobado";
                } else if (filterStatus === "Rechazado") {
                    return app.estadoSeleccion === "rechazado";
                }
                return true;
            });
        }

        // Filtro por curso
        if (filterCourse !== "Todos") {
            filtered = filtered.filter(app => app.courseName === filterCourse);
        }

        setFilteredApplications(filtered);
        setCurrentPage(1);
    };

    const uniqueCourses = ["Todos", ...new Set(applications.map(app => app.courseName))];

    const openModal = (action, application) => {
        setModalAction(action);
        setSelectedApplication(application);
        setComentario("");
        setShowModal(true);
    };

    const closeModal = () => {
        setShowModal(false);
        setModalAction(null);
        setSelectedApplication(null);
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
            ? `${BACKEND_URL}/department-head/approve`
            : `${BACKEND_URL}/department-head/reject`;

        try {
            const response = await fetch(endpoint, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                    'Authorization': localStorage.getItem('token')
                },
                body: JSON.stringify({
                    monitoringId: selectedApplication.monitoringId,
                    monitorCode: selectedApplication.monitorCode,
                    comentario: comentario,
                    departmentHeadId: departmentHeadId
                })
            });

            if (!response.ok) {
                const errorText = await response.text();
                throw new Error(errorText);
            }

            setMessage(`Postulación ${modalAction === 'approve' ? 'aprobada' : 'rechazada'} exitosamente`);
            setIsOpen(true);
            closeModal();
            loadApplications(); // Recargar datos
            setIsLoading(false);
        } catch (error) {
            console.error("Error al procesar decisión:", error);
            setMessage("Error: " + error.message);
            setIsOpen(true);
            setIsLoading(false);
        }
    };

    const handleClose = () => {
        setIsOpen(false);
    };

    // Paginación
    const indexOfLastRecord = currentPage * recordsPerPage;
    const indexOfFirstRecord = indexOfLastRecord - recordsPerPage;
    const currentRecords = filteredApplications.slice(indexOfFirstRecord, indexOfLastRecord);
    const totalPages = Math.ceil(filteredApplications.length / recordsPerPage);

    const getStatusBadge = (estado) => {
        if (estado === "aprobado") {
            return <span className="badge badge-approved">✓ Aprobado</span>;
        } else if (estado === "rechazado") {
            return <span className="badge badge-rejected">✗ Rechazado</span>;
        } else {
            return <span className="badge badge-pending">⏳ Pendiente</span>;
        }
    };

    const formatDate = (dateString) => {
        if (!dateString) return "N/A";
        const date = new Date(dateString);
        return date.toLocaleDateString('es-ES') + ' ' + date.toLocaleTimeString('es-ES');
    };

    return (
        <div className="approve-applications-container">
            <VerticalNavbar />
            
            <div className="main-content-approve">
                <div className="title-container-approve">
                    <div className="title-approve">Aprobación de Postulaciones</div>
                    <div className="subtitle-approve">Gestión de monitores por Jefe de Departamento</div>
                </div>

                {/* Filtros */}
                <div className="filters-section-approve">
                    <div className="filter-group">
                        <label>Estado:</label>
                        <select value={filterStatus} onChange={(e) => setFilterStatus(e.target.value)}>
                            <option>Todos</option>
                            <option>Pendiente</option>
                            <option>Aprobado</option>
                            <option>Rechazado</option>
                        </select>
                    </div>
                    <div className="filter-group">
                        <label>Curso:</label>
                        <select value={filterCourse} onChange={(e) => setFilterCourse(e.target.value)}>
                            {uniqueCourses.map((course, idx) => (
                                <option key={idx}>{course}</option>
                            ))}
                        </select>
                    </div>
                    <div className="filter-stats">
                        <span>Total: {filteredApplications.length} postulaciones</span>
                    </div>
                </div>

                {/* Tabla de postulaciones */}
                {isLoading ? (
                    <LoadingSpinner />
                ) : (
                    <>
                        <div className="table-container-approve">
                            <table className="applications-table">
                                <thead>
                                    <tr>
                                        <th>Curso</th>
                                        <th>Programa</th>
                                        <th>Profesor</th>
                                        <th>Monitor</th>
                                        <th>Código</th>
                                        <th>Promedio</th>
                                        <th>Semestre</th>
                                        <th>Estado</th>
                                        <th>Acciones</th>
                                    </tr>
                                </thead>
                                <tbody>
                                    {currentRecords.length === 0 ? (
                                        <tr>
                                            <td colSpan="9" className="no-data">No hay postulaciones para mostrar</td>
                                        </tr>
                                    ) : (
                                        currentRecords.map((app) => (
                                            <tr key={app.id} className={app.estadoSeleccion === 'aprobado' ? 'row-approved' : app.estadoSeleccion === 'rechazado' ? 'row-rejected' : ''}>
                                                <td>{app.courseName}</td>
                                                <td>{app.programName}</td>
                                                <td>{app.professorName}</td>
                                                <td>{app.monitorName}</td>
                                                <td>{app.monitorCode}</td>
                                                <td>{app.gradeAverage?.toFixed(2)}</td>
                                                <td>{app.semester}°</td>
                                                <td>{getStatusBadge(app.estadoSeleccion)}</td>
                                                <td>
                                                    {app.estadoSeleccion === 'aprobado' || app.estadoSeleccion === 'rechazado' ? (
                                                        <div className="decision-info">
                                                            <small>{formatDate(app.fechaDecision)}</small>
                                                            {app.comentarioDecision && (
                                                                <div className="comment-tooltip">
                                                                    <span className="comment-icon">💬</span>
                                                                    <span className="tooltip-text">{app.comentarioDecision}</span>
                                                                </div>
                                                            )}
                                                        </div>
                                                    ) : (
                                                        <div className="action-buttons">
                                                            <button 
                                                                className="btn-approve"
                                                                onClick={() => openModal('approve', app)}
                                                            >
                                                                ✓ Aprobar
                                                            </button>
                                                            <button 
                                                                className="btn-reject"
                                                                onClick={() => openModal('reject', app)}
                                                            >
                                                                ✗ Rechazar
                                                            </button>
                                                        </div>
                                                    )}
                                                </td>
                                            </tr>
                                        ))
                                    )}
                                </tbody>
                            </table>
                        </div>

                        {/* Paginación */}
                        {totalPages > 1 && (
                            <div className="pagination-approve">
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
            </div>

            {/* Modal para aprobar/rechazar */}
            {showModal && (
                <div className="modal-overlay" onClick={closeModal}>
                    <div className="modal-content-approve" onClick={(e) => e.stopPropagation()}>
                        <div className="modal-header">
                            <h2>{modalAction === 'approve' ? '✓ Aprobar Postulación' : '✗ Rechazar Postulación'}</h2>
                            <button className="modal-close" onClick={closeModal}>×</button>
                        </div>
                        <div className="modal-body">
                            <div className="application-details">
                                <p><strong>Monitor:</strong> {selectedApplication?.monitorName}</p>
                                <p><strong>Curso:</strong> {selectedApplication?.courseName}</p>
                                <p><strong>Promedio:</strong> {selectedApplication?.gradeAverage?.toFixed(2)}</p>
                            </div>
                            <div className="form-group">
                                <label>Comentario*:</label>
                                <textarea 
                                    value={comentario}
                                    onChange={(e) => setComentario(e.target.value)}
                                    placeholder="Ingrese el motivo de su decisión..."
                                    rows="4"
                                    className="textarea-comentario"
                                />
                            </div>
                        </div>
                        <div className="modal-footer">
                            <button className="btn-cancel" onClick={closeModal}>Cancelar</button>
                            <button 
                                className={modalAction === 'approve' ? 'btn-confirm-approve' : 'btn-confirm-reject'}
                                onClick={handleSubmitDecision}
                            >
                                {modalAction === 'approve' ? 'Confirmar Aprobación' : 'Confirmar Rechazo'}
                            </button>
                        </div>
                    </div>
                </div>
            )}

            {isOpen && <PopUp isOpen={isOpen} onClose={handleClose} message={message} />}
        </div>
    );
}

export default ApproveApplications;

