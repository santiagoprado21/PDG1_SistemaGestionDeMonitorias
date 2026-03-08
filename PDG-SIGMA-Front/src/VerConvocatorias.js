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

    // Modal de postulación
    const [showModal, setShowModal] = useState(false);
    const [selectedConvocatoria, setSelectedConvocatoria] = useState(null);
    const [motivationLetter, setMotivationLetter] = useState("");

    const monitorId = localStorage.getItem('userId');
    const userRole = localStorage.getItem('role');

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
                headers: {
                    'Authorization': localStorage.getItem('token')
                }
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
                headers: {
                    'Authorization': localStorage.getItem('token')
                }
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

    const hasApplied = (convocatoriaId) => {
        return myApplications.some(app => app.monitoringRequestId === convocatoriaId);
    };

    const openModal = (convocatoria) => {
        // Validar que solo estudiantes y monitores puedan postularse
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
            monitorId: monitorId,
            motivationLetter: motivationLetter
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
                setMessage("¡Has enviado tu postulación correctamente a la monitoría!\n\nEl profesor revisará tu carta de motivación y decidirá quién será el monitor seleccionado.\n\nTe notificaremos el resultado de tu postulación.");
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

    const getUniquePrograms = () => {
        const programs = [...new Set(convocatorias.map(c => c.programName))];
        return programs.filter(p => p);
    };

    // Paginación
    const indexOfLastRecord = currentPage * recordsPerPage;
    const indexOfFirstRecord = indexOfLastRecord - recordsPerPage;
    const currentRecords = filteredConvocatorias.slice(indexOfFirstRecord, indexOfLastRecord);
    const totalPages = Math.ceil(filteredConvocatorias.length / recordsPerPage);

    return (
        <div className="apply-monitor-container">
            <VerticalNavbar />
            
            {isLoading && <LoadingSpinner />}
            <PopUp show={isOpen} onClose={() => setIsOpen(false)}>
                <div style={{ 
                    textAlign: 'center', 
                    padding: '20px',
                    whiteSpace: 'pre-line',
                    fontSize: '16px',
                    lineHeight: '1.6'
                }}>
                    {message}
                </div>
            </PopUp>

            <div className="title-container-apply-monitor">
                <div className="title-apply-monitor">Convocatorias de Monitoría Abiertas</div>
                <div className="subtitle-create-monitoria">Postúlate a las convocatorias disponibles</div>
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
                <span style={{ color: '#88898c' }}>
                    Mostrando {filteredConvocatorias.length} convocatorias
                </span>
            </div>

            {/* Grid de convocatorias */}
            <div style={{ padding: '20px' }}>
                {currentRecords.length === 0 ? (
                    <div style={{ 
                        textAlign: 'center', 
                        padding: '60px',
                        backgroundColor: '#ffffff',
                        borderRadius: '8px',
                        color: '#88898c'
                    }}>
                        <h3>No hay convocatorias disponibles</h3>
                        <p>En este momento no hay convocatorias abiertas para postularse</p>
                    </div>
                ) : (
                    <div style={{ 
                        display: 'grid', 
                        gridTemplateColumns: 'repeat(auto-fill, minmax(350px, 1fr))',
                        gap: '20px'
                    }}>
                        {currentRecords.map((conv) => {
                            const applied = hasApplied(conv.id);
                            return (
                                <div key={conv.id} style={{
                                    border: '1px solid #cecfd4',
                                    borderRadius: '8px',
                                    padding: '20px',
                                    backgroundColor: 'white',
                                    boxShadow: '0 2px 4px rgba(0,0,0,0.1)',
                                    transition: 'transform 0.2s',
                                    ':hover': { transform: 'translateY(-2px)' }
                                }}>
                                    <div style={{ marginBottom: '15px' }}>
                                        <h3 style={{ margin: '0 0 10px 0', color: '#000000' }}>
                                            {conv.courseName}
                                        </h3>
                                        <div style={{ fontSize: '14px', color: '#88898c' }}>
                                            <p style={{ margin: '5px 0' }}>
                                                <strong>Profesor:</strong> {conv.professorName}
                                            </p>
                                            <p style={{ margin: '5px 0' }}>
                                                <strong>Programa:</strong> {conv.programName}
                                            </p>
                                            <p style={{ margin: '5px 0' }}>
                                                <strong>Semestre:</strong> {conv.semester}
                                            </p>
                                            <p style={{ margin: '5px 0' }}>
                                                <strong>Horas:</strong> {conv.requestedHours} horas
                                            </p>
                                            <p style={{ margin: '5px 0' }}>
                                                <strong>Postulantes:</strong> {conv.applicationCount || 0}
                                            </p>
                                        </div>
                                    </div>

                                    <div style={{ 
                                        borderTop: '1px solid #cecfd4', 
                                        paddingTop: '15px',
                                        marginTop: '15px'
                                    }}>
                                        {applied ? (
                                            <div style={{
                                                padding: '12px',
                                                backgroundColor: '#4cb979',
                                                color: '#ffffff',
                                                borderRadius: '4px',
                                                textAlign: 'center',
                                                fontWeight: 'bold'
                                            }}>
                                                Ya te postulaste
                                            </div>
                                        ) : (
                                            <button
                                                onClick={() => openModal(conv)}
                                                style={{
                                                    width: '100%',
                                                    padding: '12px',
                                                    backgroundColor: '#5454e9',
                                                    color: 'white',
                                                    border: 'none',
                                                    borderRadius: '4px',
                                                    cursor: 'pointer',
                                                    fontSize: '16px',
                                                    fontWeight: 'bold'
                                                }}
                                                onMouseOver={(e) => e.target.style.backgroundColor = '#5454e9'}
                                                onMouseOut={(e) => e.target.style.backgroundColor = '#5454e9'}
                                            >
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
            </div>

            {/* Modal de postulación */}
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
                        width: '90%',
                        maxHeight: '80vh',
                        overflow: 'auto'
                    }}>
                        <h2 style={{ marginTop: 0 }}>Postularse a Monitoría</h2>
                        <h3 style={{ color: '#88898c', marginTop: '10px' }}>
                            {selectedConvocatoria?.courseName}
                        </h3>
                        
                        <form onSubmit={handlePostularse}>
                            <div style={{ marginTop: '20px' }}>
                                <label style={{ display: 'block', marginBottom: '10px', fontWeight: 'bold' }}>
                                    Carta de Motivación * (mínimo 50 caracteres)
                                </label>
                                <textarea
                                    value={motivationLetter}
                                    onChange={(e) => setMotivationLetter(e.target.value)}
                                    placeholder="Explica por qué quieres ser monitor de este curso, tu experiencia y habilidades relevantes..."
                                    rows="8"
                                    required
                                    style={{
                                        width: '100%',
                                        padding: '12px',
                                        borderRadius: '4px',
                                        border: '1px solid #cecfd4',
                                        fontSize: '14px',
                                        fontFamily: 'inherit'
                                    }}
                                />
                                <small style={{ 
                                    color: motivationLetter.length < 50 ? 'red' : 'green',
                                    display: 'block',
                                    marginTop: '5px'
                                }}>
                                    {motivationLetter.length} / 50 caracteres mínimos
                                </small>
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
                                        backgroundColor: '#88898c',
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
                                    disabled={motivationLetter.length < 50}
                                    style={{
                                        padding: '12px 24px',
                                        backgroundColor: motivationLetter.length < 50 ? '#cecfd4' : '#4cb979',
                                        color: 'white',
                                        border: 'none',
                                        borderRadius: '4px',
                                        cursor: motivationLetter.length < 50 ? 'not-allowed' : 'pointer',
                                        fontSize: '16px',
                                        fontWeight: 'bold'
                                    }}
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


