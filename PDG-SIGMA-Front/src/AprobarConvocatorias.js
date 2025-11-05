import React, { useState, useEffect } from 'react';
import './ApproveApplications.css';
import VerticalNavbar from './VerticalNavbar';
import { PopUp } from "./PopUp";
import { BACKEND_URL } from './config/ApiBackend';
import LoadingSpinner from './LoadingSpinner';

function AprobarConvocatorias() {
    const [monitorias, setMonitorias] = useState([]);
    const [filteredMonitorias, setFilteredMonitorias] = useState([]);
    const [currentPage, setCurrentPage] = useState(1);
    const [filterProgram, setFilterProgram] = useState("Todos");
    const recordsPerPage = 5;

    const [isOpen, setIsOpen] = useState(false);
    const [message, setMessage] = useState("");
    const [isLoading, setIsLoading] = useState(false);

    // Modal de aprobación/rechazo
    const [showModal, setShowModal] = useState(false);
    const [modalAction, setModalAction] = useState(null); // 'approve' | 'reject'
    const [selectedMonitoria, setSelectedMonitoria] = useState(null);
    const [comment, setComment] = useState("");

    const departmentHeadId = localStorage.getItem('userId');

    useEffect(() => {
        loadPendingMonitorias();
    }, []);

    useEffect(() => {
        applyFilters();
    }, [monitorias, filterProgram]);

    const loadPendingMonitorias = async () => {
        setIsLoading(true);
        try {
            const response = await fetch(`${BACKEND_URL}/monitoring/pending-approval`, {
                headers: {
                    'Authorization': localStorage.getItem('token')
                }
            });
            
            if (response.ok) {
                const data = await response.json();
                setMonitorias(data || []);
            }
        } catch (error) {
            console.error('Error loading monitorias:', error);
            setMessage("Error al cargar monitorías pendientes");
            setIsOpen(true);
        } finally {
            setIsLoading(false);
        }
    };

    const applyFilters = () => {
        let filtered = monitorias;

        if (filterProgram !== "Todos") {
            filtered = filtered.filter(m => m.program?.name === filterProgram);
        }

        setFilteredMonitorias(filtered);
        setCurrentPage(1);
    };

    const openModal = (monitoria, action) => {
        setSelectedMonitoria(monitoria);
        setModalAction(action);
        setComment("");
        setShowModal(true);
    };

    const closeModal = () => {
        setShowModal(false);
        setSelectedMonitoria(null);
        setModalAction(null);
        setComment("");
    };

    const handleApproveOrReject = async (e) => {
        e.preventDefault();

        if (!comment.trim()) {
            setMessage("Por favor agrega un comentario");
            setIsOpen(true);
            return;
        }

        setIsLoading(true);

        const requestData = {
            departmentHeadId: departmentHeadId,
            comment: comment
        };

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
                body: JSON.stringify(requestData)
            });

            if (response.ok) {
                const actionText = modalAction === 'approve' ? 'aprobada' : 'rechazada';
                setMessage(`¡Monitoría ${actionText} exitosamente!`);
                setIsOpen(true);
                closeModal();
                loadPendingMonitorias();
            } else {
                const error = await response.text();
                setMessage(`Error: ${error}`);
                setIsOpen(true);
            }
        } catch (error) {
            setMessage("Error al procesar la monitoría: " + error.message);
            setIsOpen(true);
        } finally {
            setIsLoading(false);
        }
    };

    const getUniquePrograms = () => {
        const programs = [...new Set(monitorias.map(m => m.program?.name).filter(p => p))];
        return programs;
    };

    // Paginación
    const indexOfLastRecord = currentPage * recordsPerPage;
    const indexOfFirstRecord = indexOfLastRecord - recordsPerPage;
    const currentRecords = filteredMonitorias.slice(indexOfFirstRecord, indexOfLastRecord);
    const totalPages = Math.ceil(filteredMonitorias.length / recordsPerPage);

    return (
        <div className="approve-applications-container">
            <VerticalNavbar />
            
            {isLoading && <LoadingSpinner />}
            {isOpen && <PopUp message={message} onClose={() => setIsOpen(false)} />}

            <div className="title-container-approve">
                <div className="title-approve">Aprobar Monitorías (HU-010)</div>
                <div className="subtitle-create-monitoria">
                    Monitorías pendientes de aprobación del jefe de departamento
                </div>
            </div>

            {/* Filtros */}
            <div style={{ padding: '20px', display: 'flex', gap: '20px', alignItems: 'center' }}>
                <label>
                    <strong>Filtrar por Programa:</strong>
                    <select 
                        value={filterProgram}
                        onChange={(e) => setFilterProgram(e.target.value)}
                        style={{ marginLeft: '10px', padding: '8px', borderRadius: '4px' }}
                    >
                        <option value="Todos">Todos</option>
                        {getUniquePrograms().map(program => (
                            <option key={program} value={program}>{program}</option>
                        ))}
                    </select>
                </label>
                <span style={{ color: '#666' }}>
                    {filteredMonitorias.length} monitorías pendientes
                </span>
            </div>

            {/* Lista de monitorías */}
            <div style={{ padding: '20px' }}>
                {filteredMonitorias.length === 0 ? (
                    <div style={{ 
                        textAlign: 'center', 
                        padding: '60px',
                        backgroundColor: '#f5f5f5',
                        borderRadius: '8px',
                        color: '#888'
                    }}>
                        <h3>No hay monitorías pendientes</h3>
                        <p>Todas las monitorías han sido procesadas</p>
                    </div>
                ) : (
                    <>
                        {currentRecords.map((monitoria) => (
                            <div key={monitoria.id} style={{
                                border: '1px solid #ddd',
                                borderRadius: '8px',
                                padding: '25px',
                                marginBottom: '20px',
                                backgroundColor: 'white',
                                boxShadow: '0 2px 4px rgba(0,0,0,0.1)'
                            }}>
                                <div style={{ 
                                    display: 'grid',
                                    gridTemplateColumns: '2fr 1fr',
                                    gap: '20px'
                                }}>
                                    {/* Columna izquierda - Información principal */}
                                    <div>
                                        <h3 style={{ margin: '0 0 15px 0', color: '#333', fontSize: '20px' }}>
                                            {monitoria.course?.name || 'Sin curso'}
                                        </h3>
                                        
                                        <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '12px', marginBottom: '15px' }}>
                                            <div>
                                                <strong>Profesor:</strong> {monitoria.professor?.name || 'N/A'}
                                            </div>
                                            <div>
                                                <strong>Monitor Asignado:</strong> {monitoria.assignedMonitorName || 'N/A'}
                                            </div>
                                            <div>
                                                <strong>Programa:</strong> {monitoria.program?.name || 'N/A'}
                                            </div>
                                            <div>
                                                <strong>Facultad:</strong> {monitoria.school?.name || 'N/A'}
                                            </div>
                                            <div>
                                                <strong>Semestre:</strong> {monitoria.semester || 'N/A'}
                                            </div>
                                            <div>
                                                <strong>Horas Estimadas:</strong> {monitoria.estimatedHours || 'N/A'}
                                            </div>
                                            <div>
                                                <strong>Valor/Hora:</strong> ${monitoria.hourlyRate?.toLocaleString() || 'N/A'}
                                            </div>
                                            <div>
                                                <strong>Costo Total:</strong> ${((monitoria.estimatedHours || 0) * (monitoria.hourlyRate || 0)).toLocaleString()}
                                            </div>
                                        </div>

                                        {/* Justificación */}
                                        {monitoria.justification && (
                                            <div style={{ marginTop: '15px' }}>
                                                <strong style={{ display: 'block', marginBottom: '8px' }}>
                                                    Justificación:
                                                </strong>
                                                <div style={{
                                                    padding: '15px',
                                                    backgroundColor: '#f9f9f9',
                                                    borderRadius: '4px',
                                                    fontSize: '14px',
                                                    lineHeight: '1.6',
                                                    color: '#555',
                                                    border: '1px solid #e0e0e0'
                                                }}>
                                                    {monitoria.justification}
                                                </div>
                                            </div>
                                        )}

                                        {/* Fechas */}
                                        <div style={{ 
                                            marginTop: '15px',
                                            padding: '12px',
                                            backgroundColor: '#E3F2FD',
                                            borderRadius: '4px',
                                            fontSize: '13px'
                                        }}>
                                            <strong>Período:</strong> {' '}
                                            {monitoria.start ? new Date(monitoria.start).toLocaleDateString() : 'N/A'} 
                                            {' → '}
                                            {monitoria.finish ? new Date(monitoria.finish).toLocaleDateString() : 'N/A'}
                                        </div>
                                    </div>

                                    {/* Columna derecha - Acciones */}
                                    <div style={{ 
                                        display: 'flex',
                                        flexDirection: 'column',
                                        gap: '15px',
                                        justifyContent: 'center'
                                    }}>
                                        <button
                                            onClick={() => openModal(monitoria, 'approve')}
                                            style={{
                                                padding: '15px 20px',
                                                backgroundColor: '#4CAF50',
                                                color: 'white',
                                                border: 'none',
                                                borderRadius: '4px',
                                                cursor: 'pointer',
                                                fontSize: '16px',
                                                fontWeight: 'bold',
                                                display: 'flex',
                                                alignItems: 'center',
                                                justifyContent: 'center',
                                                gap: '8px'
                                            }}
                                            onMouseOver={(e) => e.target.style.backgroundColor = '#45a049'}
                                            onMouseOut={(e) => e.target.style.backgroundColor = '#4CAF50'}
                                        >
                                            ✓ Aprobar
                                        </button>

                                        <button
                                            onClick={() => openModal(monitoria, 'reject')}
                                            style={{
                                                padding: '15px 20px',
                                                backgroundColor: '#F44336',
                                                color: 'white',
                                                border: 'none',
                                                borderRadius: '4px',
                                                cursor: 'pointer',
                                                fontSize: '16px',
                                                fontWeight: 'bold',
                                                display: 'flex',
                                                alignItems: 'center',
                                                justifyContent: 'center',
                                                gap: '8px'
                                            }}
                                            onMouseOver={(e) => e.target.style.backgroundColor = '#d32f2f'}
                                            onMouseOut={(e) => e.target.style.backgroundColor = '#F44336'}
                                        >
                                            ✗ Rechazar
                                        </button>

                                        <div style={{
                                            marginTop: '10px',
                                            padding: '12px',
                                            backgroundColor: '#FFF9C4',
                                            borderRadius: '4px',
                                            fontSize: '12px',
                                            textAlign: 'center',
                                            border: '1px solid #FFF176'
                                        }}>
                                            <strong>Estado:</strong><br/>
                                            {monitoria.approvalStatus || 'PENDIENTE'}
                                        </div>
                                    </div>
                                </div>
                            </div>
                        ))}

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
                        <h2 style={{ marginTop: 0, color: modalAction === 'approve' ? '#4CAF50' : '#F44336' }}>
                            {modalAction === 'approve' ? '✓ Aprobar Monitoría' : '✗ Rechazar Monitoría'}
                        </h2>
                        <p style={{ color: '#666', marginTop: '10px' }}>
                            <strong>Curso:</strong> {selectedMonitoria?.course?.name}<br/>
                            <strong>Profesor:</strong> {selectedMonitoria?.professor?.name}<br/>
                            <strong>Monitor:</strong> {selectedMonitoria?.assignedMonitorName}
                        </p>
                        
                        <form onSubmit={handleApproveOrReject}>
                            <div style={{ marginTop: '20px' }}>
                                <label style={{ display: 'block', marginBottom: '10px', fontWeight: 'bold' }}>
                                    Comentario *
                                </label>
                                <textarea
                                    value={comment}
                                    onChange={(e) => setComment(e.target.value)}
                                    placeholder={
                                        modalAction === 'approve' 
                                        ? "Ejemplo: Aprobado. La justificación es válida y el monitor cumple con los requisitos..."
                                        : "Ejemplo: Rechazado. El presupuesto del programa ya está comprometido..."
                                    }
                                    rows="5"
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
                                        backgroundColor: modalAction === 'approve' ? '#4CAF50' : '#F44336',
                                        color: 'white',
                                        border: 'none',
                                        borderRadius: '4px',
                                        cursor: 'pointer',
                                        fontSize: '16px',
                                        fontWeight: 'bold'
                                    }}
                                >
                                    {modalAction === 'approve' ? 'Confirmar Aprobación' : 'Confirmar Rechazo'}
                                </button>
                            </div>
                        </form>
                    </div>
                </div>
            )}
        </div>
    );
}

export default AprobarConvocatorias;

