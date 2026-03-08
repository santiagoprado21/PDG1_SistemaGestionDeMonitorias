import React, { useState, useEffect } from 'react';
import './SeleccionarMonitor.css';
import VerticalNavbar from './VerticalNavbar';
import { PopUp } from "./PopUp";
import { BACKEND_URL } from './config/ApiBackend';
import LoadingSpinner from './LoadingSpinner';
import { useParams, useNavigate } from 'react-router-dom';

function SeleccionarMonitor() {
    const { requestId } = useParams();
    const navigate = useNavigate();
    
    const [convocatoria, setConvocatoria] = useState(null);
    const [postulantes, setPostulantes] = useState([]);
    const [selectedApplicationId, setSelectedApplicationId] = useState(null);
    const [currentPage, setCurrentPage] = useState(1);
    const recordsPerPage = 8;

    const [isOpen, setIsOpen] = useState(false);
    const [message, setMessage] = useState("");
    const [isLoading, setIsLoading] = useState(false);

    // Modal de confirmación
    const [showModal, setShowModal] = useState(false);
    const [selectedPostulante, setSelectedPostulante] = useState(null);
    const [notes, setNotes] = useState("");

    const professorId = localStorage.getItem('userId');

    useEffect(() => {
        if (requestId) {
            console.log('SeleccionarMonitor - requestId:', requestId);
            loadConvocatoriaDetails();
            loadPostulantes();
        }
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [requestId]);

    const loadConvocatoriaDetails = async () => {
        try {
            console.log('Cargando convocatoria con ID:', requestId);
            const response = await fetch(`${BACKEND_URL}/monitoring-request/${requestId}`, {
                headers: {
                    'Authorization': localStorage.getItem('token')
                }
            });
            
            if (response.ok) {
                const data = await response.json();
                console.log('Convocatoria cargada:', data);
                setConvocatoria(data);
            } else {
                console.error('Error al cargar convocatoria:', response.status);
                setMessage(`Error al cargar convocatoria: ${response.status}`);
                setIsOpen(true);
            }
        } catch (error) {
            console.error('Error loading convocatoria:', error);
            setMessage('Error al cargar convocatoria: ' + error.message);
            setIsOpen(true);
        }
    };

    const loadPostulantes = async () => {
        setIsLoading(true);
        try {
            console.log('Cargando postulantes para convocatoria:', requestId);
            const response = await fetch(`${BACKEND_URL}/monitor-application/request/${requestId}`, {
                headers: {
                    'Authorization': localStorage.getItem('token')
                }
            });
            
            if (response.ok) {
                const data = await response.json();
                console.log('Postulantes cargados:', data);
                // Ordenar por fecha de postulación
                const sorted = data.sort((a, b) => 
                    new Date(b.applicationDate) - new Date(a.applicationDate)
                );
                setPostulantes(sorted);
            } else {
                console.error('Error al cargar postulantes:', response.status);
                setMessage(`Error al cargar postulantes: ${response.status}`);
                setIsOpen(true);
            }
        } catch (error) {
            console.error('Error loading postulantes:', error);
            setMessage("Error al cargar postulantes: " + error.message);
            setIsOpen(true);
        } finally {
            setIsLoading(false);
        }
    };

    const openModal = (postulante) => {
        setSelectedPostulante(postulante);
        setNotes("");
        setShowModal(true);
    };

    const closeModal = () => {
        setShowModal(false);
        setSelectedPostulante(null);
        setNotes("");
    };

    const handleSelectMonitor = async () => {
        if (!selectedPostulante) return;

        setIsLoading(true);

        const selectionData = {
            applicationId: selectedPostulante.id,
            professorId: professorId,
            notes: notes || "Monitor seleccionado"
        };

        try {
            const response = await fetch(`${BACKEND_URL}/monitor-application/select`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                    'Authorization': localStorage.getItem('token')
                },
                body: JSON.stringify(selectionData)
            });

            if (response.ok) {
                const result = await response.json();
                setMessage(result.message || "Monitor seleccionado exitosamente. La monitoría ha sido enviada para aprobación del jefe de departamento.");
                setIsOpen(true);
                closeModal();
                
                // Redirigir después de 2 segundos
                setTimeout(() => {
                    navigate('/mis-convocatorias');
                }, 2000);
            } else {
                const error = await response.text();
                setMessage(`Error: ${error}`);
                setIsOpen(true);
            }
        } catch (error) {
            setMessage("Error al seleccionar monitor: " + error.message);
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
    const currentRecords = postulantes.slice(indexOfFirstRecord, indexOfLastRecord);
    const totalPages = Math.ceil(postulantes.length / recordsPerPage);

    const formatDate = (dateString) => {
        if (!dateString) return "N/A";
        const date = new Date(dateString);
        return date.toLocaleDateString('es-ES') + ' ' + date.toLocaleTimeString('es-ES', { 
            hour: '2-digit', 
            minute: '2-digit' 
        });
    };

    const getStatusBadge = (status) => {
        const statusMap = {
            'POSTULADO': { text: 'Postulado', color: '#5454e9' },
            'SELECCIONADO': { text: '✓ Seleccionado', color: '#4cb979' },
            'NO_SELECCIONADO': { text: 'No Seleccionado', color: '#88898c' }
        };
        const statusInfo = statusMap[status] || { text: status, color: '#88898c' };
        return (
            <span style={{
                padding: '6px 12px',
                borderRadius: '12px',
                backgroundColor: statusInfo.color,
                color: 'white',
                fontSize: '13px',
                fontWeight: 'bold',
                display: 'inline-block'
            }}>
                {statusInfo.text}
            </span>
        );
    };

    return (
        <div className="seleccionar-monitor-container">
            <VerticalNavbar />
            
            {isLoading && <LoadingSpinner />}
            
            <div className="main-content-seleccionar">
                {/* Header */}
                <div className="header-seleccionar">
                    <button 
                        className="btn-back"
                        onClick={() => navigate('/mis-convocatorias')}
                    >
                        ← Volver a Mis Convocatorias
                    </button>
                    <div className="title-container-seleccionar">
                        <div className="title-seleccionar">Seleccionar Monitor</div>
                        {convocatoria && (
                            <div className="subtitle-seleccionar">
                                {convocatoria.courseName} - {convocatoria.semester}
                            </div>
                        )}
                    </div>
                </div>

                {/* Info de la convocatoria */}
                {convocatoria && (
                    <div className="convocatoria-info">
                        <div className="info-row">
                            <span className="info-label">Horas solicitadas:</span>
                            <span className="info-value">{convocatoria.requestedHours} horas</span>
                        </div>
                        <div className="info-row">
                            <span className="info-label">Promedio requerido:</span>
                            <span className="info-value">{convocatoria.requiredAverageGrade}</span>
                        </div>
                        <div className="info-row">
                            <span className="info-label">Nota curso requerida:</span>
                            <span className="info-value">{convocatoria.requiredCourseGrade}</span>
                        </div>
                        <div className="info-row">
                            <span className="info-label">Total postulantes:</span>
                            <span className="info-value">{postulantes.length}</span>
                        </div>
                    </div>
                )}

                {/* Lista de postulantes */}
                <div className="postulantes-section">
                    <h3>Postulantes ({postulantes.length})</h3>
                    
                    {postulantes.length === 0 ? (
                        <div className="no-postulantes">
                            <p>No hay postulantes para esta convocatoria aún.</p>
                            <p>Espera a que los estudiantes se postulen.</p>
                        </div>
                    ) : (
                        <>
                            <div className="table-container-seleccionar">
                                <table className="postulantes-table">
                                    <thead>
                                        <tr>
                                            <th>Monitor</th>
                                            <th>Código</th>
                                            <th>Estado</th>
                                            <th>Fecha Postulación</th>
                                            <th>Carta de Motivación</th>
                                            <th>Acciones</th>
                                        </tr>
                                    </thead>
                                    <tbody>
                                        {currentRecords.map((postulante) => (
                                            <tr 
                                                key={postulante.id}
                                                className={postulante.status === 'SELECCIONADO' ? 'row-selected' : ''}
                                            >
                                                <td>{postulante.monitorName}</td>
                                                <td>{postulante.monitorId}</td>
                                                <td>{getStatusBadge(postulante.status)}</td>
                                                <td>{formatDate(postulante.applicationDate)}</td>
                                                <td>
                                                    <div className="motivation-preview">
                                                        {postulante.motivationLetter?.substring(0, 100)}
                                                        {postulante.motivationLetter?.length > 100 && '...'}
                                                    </div>
                                                </td>
                                                <td>
                                                    {postulante.status === 'POSTULADO' ? (
                                                        <button 
                                                            className="btn-select-monitor"
                                                            onClick={() => openModal(postulante)}
                                                        >
                                                            Seleccionar
                                                        </button>
                                                    ) : postulante.status === 'SELECCIONADO' ? (
                                                        <span style={{ color: '#4cb979', fontWeight: 'bold' }}>
                                                            Ya seleccionado
                                                        </span>
                                                    ) : (
                                                        <span style={{ color: '#88898c' }}>
                                                            No seleccionado
                                                        </span>
                                                    )}
                                                </td>
                                            </tr>
                                        ))}
                                    </tbody>
                                </table>
                            </div>

                            {/* Paginación */}
                            {totalPages > 1 && (
                                <div className="pagination-seleccionar">
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
            </div>

            {/* Modal de confirmación */}
            {showModal && selectedPostulante && (
                <div className="modal-overlay" onClick={closeModal}>
                    <div className="modal-content-seleccionar" onClick={(e) => e.stopPropagation()}>
                        <div className="modal-header">
                            <h2>Confirmar Selección de Monitor</h2>
                            <button className="modal-close" onClick={closeModal}>×</button>
                        </div>
                        <div className="modal-body">
                            <div className="postulante-details">
                                <p><strong>Monitor:</strong> {selectedPostulante.monitorName}</p>
                                <p><strong>Código:</strong> {selectedPostulante.monitorId}</p>
                                <p><strong>Fecha de postulación:</strong> {formatDate(selectedPostulante.applicationDate)}</p>
                            </div>
                            
                            <div className="motivation-section">
                                <label><strong>Carta de Motivación:</strong></label>
                                <div className="motivation-full">
                                    {selectedPostulante.motivationLetter}
                                </div>
                            </div>

                            <div className="form-group">
                                <label>Notas adicionales (opcional):</label>
                                <textarea 
                                    value={notes}
                                    onChange={(e) => setNotes(e.target.value)}
                                    placeholder="Razones para la selección, expectativas, comentarios..."
                                    rows="3"
                                    className="textarea-notes"
                                />
                            </div>

                            <div className="warning-box">
                                <strong>⚠️ Importante:</strong>
                                <p>Al seleccionar este monitor:</p>
                                <ul>
                                    <li>Los demás postulantes serán marcados como "No Seleccionados"</li>
                                    <li>Se creará automáticamente la monitoría</li>
                                    <li>La monitoría será enviada al Jefe de Departamento para su aprobación final</li>
                                </ul>
                            </div>
                        </div>
                        <div className="modal-footer">
                            <button className="btn-cancel" onClick={closeModal}>Cancelar</button>
                            <button 
                                className="btn-confirm-select"
                                onClick={handleSelectMonitor}
                            >
                                Confirmar Selección
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

export default SeleccionarMonitor;

