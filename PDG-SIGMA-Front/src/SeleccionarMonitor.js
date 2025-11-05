import React, { useState, useEffect } from 'react';
import './Applicants.css';
import VerticalNavbar from './VerticalNavbar';
import { PopUp } from "./PopUp";
import { BACKEND_URL } from './config/ApiBackend';
import LoadingSpinner from './LoadingSpinner';
import { useParams, useNavigate } from 'react-router-dom';

function SeleccionarMonitor() {
    const { convocatoriaId } = useParams();
    const navigate = useNavigate();
    
    const [convocatoria, setConvocatoria] = useState(null);
    const [postulantes, setPostulantes] = useState([]);
    const [currentPage, setCurrentPage] = useState(1);
    const recordsPerPage = 6;

    const [isOpen, setIsOpen] = useState(false);
    const [message, setMessage] = useState("");
    const [isLoading, setIsLoading] = useState(false);

    // Modal de confirmación
    const [showModal, setShowModal] = useState(false);
    const [selectedApplication, setSelectedApplication] = useState(null);
    const [selectionNotes, setSelectionNotes] = useState("");

    const professorId = localStorage.getItem('userId');

    useEffect(() => {
        if (convocatoriaId) {
            loadConvocatoria();
            loadPostulantes();
        }
    }, [convocatoriaId]);

    const loadConvocatoria = async () => {
        try {
            const response = await fetch(`${BACKEND_URL}/monitoring-request/${convocatoriaId}`, {
                headers: {
                    'Authorization': localStorage.getItem('token')
                }
            });
            
            if (response.ok) {
                const data = await response.json();
                setConvocatoria(data);
            }
        } catch (error) {
            console.error('Error loading convocatoria:', error);
        }
    };

    const loadPostulantes = async () => {
        setIsLoading(true);
        try {
            const response = await fetch(`${BACKEND_URL}/monitor-application/request/${convocatoriaId}`, {
                headers: {
                    'Authorization': localStorage.getItem('token')
                }
            });
            
            if (response.ok) {
                const data = await response.json();
                setPostulantes(data || []);
            }
        } catch (error) {
            console.error('Error loading postulantes:', error);
            setMessage("Error al cargar postulantes");
            setIsOpen(true);
        } finally {
            setIsLoading(false);
        }
    };

    const openModal = (application) => {
        setSelectedApplication(application);
        setSelectionNotes("");
        setShowModal(true);
    };

    const closeModal = () => {
        setShowModal(false);
        setSelectedApplication(null);
        setSelectionNotes("");
    };

    const handleSeleccionarMonitor = async (e) => {
        e.preventDefault();

        if (!selectionNotes.trim()) {
            setMessage("Por favor agrega una nota sobre la selección");
            setIsOpen(true);
            return;
        }

        setIsLoading(true);

        const selectionData = {
            applicationId: selectedApplication.id,
            professorId: professorId,
            notes: selectionNotes
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
                setMessage(result.message || "¡Monitor seleccionado exitosamente! La monitoría ha sido enviada para aprobación.");
                setIsOpen(true);
                closeModal();
                
                // Redirigir después de un delay
                setTimeout(() => {
                    navigate('/crear-convocatoria');
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

    const getStatusBadge = (status) => {
        const statusMap = {
            'POSTULADO': { text: 'Postulado', color: '#2196F3' },
            'SELECCIONADO': { text: 'Seleccionado', color: '#4CAF50' },
            'NO_SELECCIONADO': { text: 'No Seleccionado', color: '#9E9E9E' }
        };
        const statusInfo = statusMap[status] || { text: status, color: '#757575' };
        return (
            <span style={{
                padding: '6px 12px',
                borderRadius: '12px',
                backgroundColor: statusInfo.color,
                color: 'white',
                fontSize: '13px',
                fontWeight: 'bold'
            }}>
                {statusInfo.text}
            </span>
        );
    };

    // Paginación
    const indexOfLastRecord = currentPage * recordsPerPage;
    const indexOfFirstRecord = indexOfLastRecord - recordsPerPage;
    const currentRecords = postulantes.slice(indexOfFirstRecord, indexOfLastRecord);
    const totalPages = Math.ceil(postulantes.length / recordsPerPage);

    const hasSelection = postulantes.some(p => p.status === 'SELECCIONADO' || p.status === 'NO_SELECCIONADO');

    return (
        <div className="applicants-container">
            <VerticalNavbar />
            
            {isLoading && <LoadingSpinner />}
            {isOpen && <PopUp message={message} onClose={() => setIsOpen(false)} />}

            <div className="title-container-applicants">
                <div className="title-applicants">Seleccionar Monitor</div>
                <div className="subtitle-create-monitoria">
                    {convocatoria ? `${convocatoria.courseName} - ${convocatoria.semester}` : 'Cargando...'}
                </div>
            </div>

            {/* Información de la convocatoria */}
            {convocatoria && (
                <div style={{ 
                    margin: '20px', 
                    padding: '20px', 
                    backgroundColor: '#f5f5f5', 
                    borderRadius: '8px' 
                }}>
                    <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(200px, 1fr))', gap: '15px' }}>
                        <div>
                            <strong>Curso:</strong> {convocatoria.courseName}
                        </div>
                        <div>
                            <strong>Horas:</strong> {convocatoria.requestedHours}
                        </div>
                        <div>
                            <strong>Postulantes:</strong> {postulantes.length}
                        </div>
                        <div>
                            <strong>Semestre:</strong> {convocatoria.semester}
                        </div>
                    </div>
                    {convocatoria.justification && (
                        <div style={{ marginTop: '15px' }}>
                            <strong>Justificación:</strong>
                            <p style={{ marginTop: '5px', color: '#666' }}>{convocatoria.justification}</p>
                        </div>
                    )}
                </div>
            )}

            {/* Lista de postulantes */}
            <div style={{ padding: '20px' }}>
                {postulantes.length === 0 ? (
                    <div style={{ 
                        textAlign: 'center', 
                        padding: '60px',
                        backgroundColor: '#f5f5f5',
                        borderRadius: '8px',
                        color: '#888'
                    }}>
                        <h3>No hay postulantes aún</h3>
                        <p>Todavía no se ha postulado ningún estudiante a esta convocatoria</p>
                        <button
                            onClick={() => navigate('/crear-convocatoria')}
                            style={{
                                marginTop: '20px',
                                padding: '12px 24px',
                                backgroundColor: '#2196F3',
                                color: 'white',
                                border: 'none',
                                borderRadius: '4px',
                                cursor: 'pointer'
                            }}
                        >
                            Volver a Mis Convocatorias
                        </button>
                    </div>
                ) : (
                    <>
                        {hasSelection && (
                            <div style={{
                                padding: '15px',
                                backgroundColor: '#E8F5E9',
                                borderRadius: '4px',
                                marginBottom: '20px',
                                color: '#2E7D32',
                                fontWeight: 'bold'
                            }}>
                                ✓ Ya has seleccionado un monitor. La monitoría ha sido enviada para aprobación del jefe de departamento.
                            </div>
                        )}

                        <div style={{ 
                            display: 'grid', 
                            gridTemplateColumns: 'repeat(auto-fill, minmax(400px, 1fr))',
                            gap: '20px'
                        }}>
                            {currentRecords.map((postulante) => (
                                <div key={postulante.id} style={{
                                    border: '1px solid #ddd',
                                    borderRadius: '8px',
                                    padding: '20px',
                                    backgroundColor: 'white',
                                    boxShadow: '0 2px 4px rgba(0,0,0,0.1)'
                                }}>
                                    <div style={{ 
                                        display: 'flex', 
                                        justifyContent: 'space-between', 
                                        alignItems: 'flex-start',
                                        marginBottom: '15px'
                                    }}>
                                        <div>
                                            <h3 style={{ margin: '0 0 5px 0', color: '#333' }}>
                                                {postulante.monitorName}
                                            </h3>
                                            <p style={{ margin: 0, color: '#666', fontSize: '14px' }}>
                                                Código: {postulante.monitorId}
                                            </p>
                                        </div>
                                        {getStatusBadge(postulante.status)}
                                    </div>

                                    <div style={{ marginTop: '15px' }}>
                                        <strong style={{ display: 'block', marginBottom: '8px' }}>
                                            Carta de Motivación:
                                        </strong>
                                        <div style={{
                                            padding: '12px',
                                            backgroundColor: '#f9f9f9',
                                            borderRadius: '4px',
                                            fontSize: '14px',
                                            lineHeight: '1.6',
                                            color: '#555',
                                            maxHeight: '150px',
                                            overflow: 'auto'
                                        }}>
                                            {postulante.motivationLetter}
                                        </div>
                                    </div>

                                    <div style={{ marginTop: '15px', fontSize: '12px', color: '#888' }}>
                                        Fecha de postulación: {new Date(postulante.applicationDate).toLocaleDateString()}
                                    </div>

                                    {postulante.status === 'POSTULADO' && !hasSelection && (
                                        <button
                                            onClick={() => openModal(postulante)}
                                            style={{
                                                width: '100%',
                                                marginTop: '15px',
                                                padding: '12px',
                                                backgroundColor: '#4CAF50',
                                                color: 'white',
                                                border: 'none',
                                                borderRadius: '4px',
                                                cursor: 'pointer',
                                                fontSize: '16px',
                                                fontWeight: 'bold'
                                            }}
                                            onMouseOver={(e) => e.target.style.backgroundColor = '#45a049'}
                                            onMouseOut={(e) => e.target.style.backgroundColor = '#4CAF50'}
                                        >
                                            Seleccionar como Monitor
                                        </button>
                                    )}

                                    {postulante.notes && (
                                        <div style={{ 
                                            marginTop: '15px',
                                            padding: '10px',
                                            backgroundColor: '#FFF9C4',
                                            borderRadius: '4px',
                                            fontSize: '13px'
                                        }}>
                                            <strong>Nota del profesor:</strong> {postulante.notes}
                                        </div>
                                    )}
                                </div>
                            ))}
                        </div>

                        {/* Paginación */}
                        {totalPages > 1 && (
                            <div style={{ marginTop: '30px', textAlign: 'center' }}>
                                <button 
                                    onClick={() => setCurrentPage(prev => Math.max(1, prev - 1))}
                                    disabled={currentPage === 1}
                                    style={{ 
                                        margin: '0 5px', 
                                        padding: '10px 20px', 
                                        cursor: currentPage === 1 ? 'not-allowed' : 'pointer',
                                        opacity: currentPage === 1 ? 0.5 : 1
                                    }}
                                >
                                    Anterior
                                </button>
                                <span style={{ margin: '0 15px', fontSize: '16px' }}>
                                    Página {currentPage} de {totalPages}
                                </span>
                                <button 
                                    onClick={() => setCurrentPage(prev => Math.min(totalPages, prev + 1))}
                                    disabled={currentPage === totalPages}
                                    style={{ 
                                        margin: '0 5px', 
                                        padding: '10px 20px', 
                                        cursor: currentPage === totalPages ? 'not-allowed' : 'pointer',
                                        opacity: currentPage === totalPages ? 0.5 : 1
                                    }}
                                >
                                    Siguiente
                                </button>
                            </div>
                        )}
                    </>
                )}
            </div>

            {/* Modal de confirmación */}
            {showModal && (
                <div style={{
                    position: 'fixed',
                    top: 0,
                    left: 0,
                    right: 0,
                    bottom: 0,
                    backgroundColor: 'rgba(0,0,0,0.5)',
                    display: 'flex',
                    justifyContent: 'center',
                    alignItems: 'center',
                    zIndex: 1000
                }}>
                    <div style={{
                        backgroundColor: 'white',
                        borderRadius: '8px',
                        padding: '30px',
                        maxWidth: '600px',
                        width: '90%'
                    }}>
                        <h2 style={{ marginTop: 0 }}>Confirmar Selección</h2>
                        <p style={{ color: '#666', marginTop: '10px' }}>
                            Vas a seleccionar a <strong>{selectedApplication?.monitorName}</strong> como monitor.
                            Esta acción creará automáticamente una monitoría que será enviada para aprobación.
                        </p>
                        
                        <form onSubmit={handleSeleccionarMonitor}>
                            <div style={{ marginTop: '20px' }}>
                                <label style={{ display: 'block', marginBottom: '10px', fontWeight: 'bold' }}>
                                    Nota sobre la selección *
                                </label>
                                <textarea
                                    value={selectionNotes}
                                    onChange={(e) => setSelectionNotes(e.target.value)}
                                    placeholder="Ejemplo: Seleccionado por su excelente promedio académico y experiencia previa en tutorías..."
                                    rows="4"
                                    required
                                    style={{
                                        width: '100%',
                                        padding: '12px',
                                        borderRadius: '4px',
                                        border: '1px solid #ccc',
                                        fontSize: '14px',
                                        fontFamily: 'inherit'
                                    }}
                                />
                            </div>

                            <div style={{ 
                                marginTop: '30px', 
                                display: 'flex', 
                                gap: '10px',
                                justifyContent: 'flex-end'
                            }}>
                                <button
                                    type="button"
                                    onClick={closeModal}
                                    style={{
                                        padding: '12px 24px',
                                        backgroundColor: '#9E9E9E',
                                        color: 'white',
                                        border: 'none',
                                        borderRadius: '4px',
                                        cursor: 'pointer',
                                        fontSize: '16px'
                                    }}
                                >
                                    Cancelar
                                </button>
                                <button
                                    type="submit"
                                    style={{
                                        padding: '12px 24px',
                                        backgroundColor: '#4CAF50',
                                        color: 'white',
                                        border: 'none',
                                        borderRadius: '4px',
                                        cursor: 'pointer',
                                        fontSize: '16px',
                                        fontWeight: 'bold'
                                    }}
                                >
                                    Confirmar Selección
                                </button>
                            </div>
                        </form>
                    </div>
                </div>
            )}
        </div>
    );
}

export default SeleccionarMonitor;

