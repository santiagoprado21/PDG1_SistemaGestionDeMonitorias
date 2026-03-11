import React, { useState, useEffect } from 'react';
import './VerConvocatorias.css';
import VerticalNavbar from './VerticalNavbar';
import { PopUp } from "./PopUp";
import { BACKEND_URL } from './config/ApiBackend';
import LoadingSpinner from './LoadingSpinner';

function VerConvocatorias() {
    const [convocatorias, setConvocatorias] = useState([]);
    const [filteredConvocatorias, setFilteredConvocatorias] = useState([]);
    const [myApplications, setMyApplications] = useState([]);
    const [currentPage, setCurrentPage] = useState(1);
    const [filterProgram, setFilterProgram] = useState("Todos");
    const recordsPerPage = 6;

    const [isOpen, setIsOpen] = useState(false);
    const [message, setMessage] = useState("");
    const [isLoading, setIsLoading] = useState(false);

    const [showModal, setShowModal] = useState(false);
    const [selectedConvocatoria, setSelectedConvocatoria] = useState(null);
    const [motivationLetter, setMotivationLetter] = useState("");

    const monitorId = localStorage.getItem('userId');
    const userRole  = localStorage.getItem('role');

    useEffect(() => {
        loadConvocatorias();
        loadMyApplications();
    }, []);

    useEffect(() => {
        applyFilters();
    }, [convocatorias, filterProgram]);

    const loadConvocatorias = async () => {
        setIsLoading(true);
        try {
            const response = await fetch(`${BACKEND_URL}/monitoring-request/open`, {
                headers: { 'Authorization': localStorage.getItem('token') }
            });
            if (response.ok) {
                const data = await response.json();
                setConvocatorias(data || []);
            }
        } catch (error) {
            console.error('Error loading convocatorias:', error);
            setMessage("Error al cargar convocatorias");
            setIsOpen(true);
        } finally {
            setIsLoading(false);
        }
    };

    const loadMyApplications = async () => {
        try {
            const response = await fetch(`${BACKEND_URL}/monitor-application/monitor/${monitorId}`, {
                headers: { 'Authorization': localStorage.getItem('token') }
            });
            if (response.ok) {
                const data = await response.json();
                setMyApplications(data || []);
            }
        } catch (error) {
            console.error('Error loading applications:', error);
        }
    };

    const applyFilters = () => {
        let filtered = convocatorias;
        if (filterProgram !== "Todos") {
            filtered = filtered.filter(c => c.programName === filterProgram);
        }
        setFilteredConvocatorias(filtered);
        setCurrentPage(1);
    };

    const hasApplied = (convocatoriaId) =>
        myApplications.some(app => app.monitoringRequestId === convocatoriaId);

    const openModal = (convocatoria) => {
        if (userRole !== 'student' && userRole !== 'monitor') {
            setMessage("Solo los estudiantes pueden postularse a las convocatorias.");
            setIsOpen(true);
            return;
        }
        setSelectedConvocatoria(convocatoria);
        setMotivationLetter("");
        setShowModal(true);
    };

    const closeModal = () => {
        setShowModal(false);
        setSelectedConvocatoria(null);
        setMotivationLetter("");
    };

    const handlePostularse = async (e) => {
        e.preventDefault();
        if (motivationLetter.length < 10) {
            setMessage("La carta de motivación debe tener al menos 50 caracteres");
            setIsOpen(true);
            return;
        }
        setIsLoading(true);
        const applicationData = {
            monitoringRequestId: selectedConvocatoria.id,
            monitorId,
            motivationLetter
        };
        try {
            const response = await fetch(`${BACKEND_URL}/monitor-application/apply`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                    'Authorization': localStorage.getItem('token')
                },
                body: JSON.stringify(applicationData)
            });
            if (response.ok) {
                setMessage("✅ ¡Has enviado tu postulación correctamente a la monitoría!\n\n📋 El profesor revisará tu carta de motivación y decidirá quién será el monitor seleccionado.\n\n⏳ Te notificaremos el resultado de tu postulación.");
                setIsOpen(true);
                closeModal();
                loadMyApplications();
            } else {
                const error = await response.text();
                setMessage(`Error: ${error}`);
                setIsOpen(true);
            }
        } catch (error) {
            setMessage("Error al enviar postulación: " + error.message);
            setIsOpen(true);
        } finally {
            setIsLoading(false);
        }
    };

    const getUniquePrograms = () =>
        [...new Set(convocatorias.map(c => c.programName))].filter(Boolean);

    const indexOfLastRecord  = currentPage * recordsPerPage;
    const indexOfFirstRecord = indexOfLastRecord - recordsPerPage;
    const currentRecords     = filteredConvocatorias.slice(indexOfFirstRecord, indexOfLastRecord);
    const totalPages         = Math.ceil(filteredConvocatorias.length / recordsPerPage);

    return (
        <div className="apply-monitor-container">
            <VerticalNavbar />

            {isLoading && <LoadingSpinner />}
            <PopUp show={isOpen} onClose={() => setIsOpen(false)}>
                <div style={{ textAlign: 'center', padding: '20px', whiteSpace: 'pre-line', fontSize: '16px', lineHeight: '1.6' }}>
                    {message}
                </div>
            </PopUp>

            <div className="ver-convocatorias-content">

                {/* Header */}
                <div className="ver-conv-header">
                    <h1>Convocatorias de Monitoría Abiertas</h1>
                    <p className="ver-conv-subtitle">Postúlate a las convocatorias disponibles</p>
                </div>

                {/* Filtros */}
                <div className="ver-conv-filters">
                    <label>
                        Filtrar por Programa:
                        <select value={filterProgram} onChange={(e) => setFilterProgram(e.target.value)}>
                            <option value="Todos">Todos</option>
                            {getUniquePrograms().map(program => (
                                <option key={program} value={program}>{program}</option>
                            ))}
                        </select>
                    </label>
                    <span className="ver-conv-count">
                        {filteredConvocatorias.length} convocatoria{filteredConvocatorias.length !== 1 ? 's' : ''}
                    </span>
                </div>

                {/* Grid */}
                {currentRecords.length === 0 ? (
                    <div className="empty-state">
                        <h3>No hay convocatorias disponibles</h3>
                        <p>En este momento no hay convocatorias abiertas para postularse</p>
                    </div>
                ) : (
                    <div className="convocatorias-grid">
                        {currentRecords.map((conv) => {
                            const applied = hasApplied(conv.id);
                            return (
                                <div key={conv.id} className={`convocatoria-card ${applied ? 'card-applied' : ''}`}>
                                    <h3>{conv.courseName}</h3>

                                    <div className="convocatoria-info">
                                        <p><strong>Profesor:</strong> {conv.professorName}</p>
                                        <p><strong>Programa:</strong> {conv.programName}</p>
                                        <p><strong>Facultad:</strong> {conv.schoolName}</p>

                                        <div className="conv-chips">
                                            <span className="conv-chip chip-semestre">📅 {conv.semester}</span>
                                            <span className="conv-chip chip-horas">⏱ {conv.requestedHours} horas</span>
                                            <span className="conv-chip chip-postulantes">👥 {conv.applicationCount || 0} postulante{conv.applicationCount !== 1 ? 's' : ''}</span>
                                        </div>
                                    </div>

                                    <div className="convocatoria-actions">
                                        {applied ? (
                                            <div className="already-applied">✓ Ya te postulaste</div>
                                        ) : (
                                            <button className="btn-postularse" onClick={() => openModal(conv)}>
                                                Postularse
                                            </button>
                                        )}
                                    </div>
                                </div>
                            );
                        })}
                    </div>
                )}

                {/* Paginación */}
                {totalPages > 1 && (
                    <div className="pagination">
                        <button
                            onClick={() => setCurrentPage(prev => Math.max(1, prev - 1))}
                            disabled={currentPage === 1}
                        >
                            ← Anterior
                        </button>
                        <span>Página {currentPage} de {totalPages}</span>
                        <button
                            onClick={() => setCurrentPage(prev => Math.min(totalPages, prev + 1))}
                            disabled={currentPage === totalPages}
                        >
                            Siguiente →
                        </button>
                    </div>
                )}
            </div>

            {/* Modal de postulación */}
            {showModal && (
                <div className="modal-overlay" onClick={closeModal}>
                    <div className="modal-content" onClick={(e) => e.stopPropagation()}>
                        <h2>Postularse a Monitoría</h2>
                        <h3>{selectedConvocatoria?.courseName}</h3>

                        <form onSubmit={handlePostularse}>
                            <label>Carta de Motivación * (mínimo 50 caracteres)</label>
                            <textarea
                                value={motivationLetter}
                                onChange={(e) => setMotivationLetter(e.target.value)}
                                placeholder="Explica por qué quieres ser monitor de este curso, tu experiencia y habilidades relevantes..."
                                rows="8"
                                required
                            />
                            <small className={`char-counter ${motivationLetter.length >= 50 ? 'valid' : 'invalid'}`}>
                                {motivationLetter.length} / 50 caracteres mínimos
                            </small>

                            <div className="modal-actions">
                                <button type="button" className="btn-modal btn-cancel" onClick={closeModal}>
                                    Cancelar
                                </button>
                                <button
                                    type="submit"
                                    className="btn-modal btn-submit"
                                    disabled={motivationLetter.length < 50}
                                >
                                    Enviar Postulación
                                </button>
                            </div>
                        </form>
                    </div>
                </div>
            )}
        </div>
    );
}

export default VerConvocatorias;
