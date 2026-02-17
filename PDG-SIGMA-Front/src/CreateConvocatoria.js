import React, { useState, useEffect } from 'react';
import './CreateConvocatoria.css'; 
import VerticalNavbar from './VerticalNavbar';
import { PopUp } from "./PopUp";
import { BACKEND_URL } from './config/ApiBackend';
import LoadingSpinner from './LoadingSpinner';
import { useNavigate } from 'react-router-dom';

function CreateConvocatoria() {
    const navigate = useNavigate();
    
    const [faculties, setFaculties] = useState([]);
    const [programs, setPrograms] = useState([]);
    const [courses, setCourses] = useState([]);
    const [myConvocatorias, setMyConvocatorias] = useState([]);
    
    const [selectedFaculty, setSelectedFaculty] = useState("");
    const [selectedProgram, setSelectedProgram] = useState("");
    const [selectedCourse, setSelectedCourse] = useState("");
    const [semester, setSemester] = useState("");
    const [startDate, setStartDate] = useState("");
    const [finishDate, setFinishDate] = useState("");
    const [requestedHours, setRequestedHours] = useState("");
    const [justification, setJustification] = useState("");
    const [requiredAverageGrade, setRequiredAverageGrade] = useState("4.0");
    const [requiredCourseGrade, setRequiredCourseGrade] = useState("4.0");
    const [hourlyRate, setHourlyRate] = useState("15000");
    
    const [isOpen, setIsOpen] = useState(false);
    const [message, setMessage] = useState("");
    const [isLoading, setIsLoading] = useState(false);
    const [currentPage, setCurrentPage] = useState(1);
    const recordsPerPage = 5;

    const professorId = localStorage.getItem('userId');

    useEffect(() => {
        loadFaculties();
        loadMyConvocatorias();
    }, []);

    useEffect(() => {
        if (selectedFaculty) {
            loadPrograms(selectedFaculty);
        }
    }, [selectedFaculty]);

    useEffect(() => {
        if (selectedProgram) {
            loadCourses(selectedProgram);
        }
    }, [selectedProgram]);

    const loadFaculties = async () => {
        try {
            const response = await fetch(`${BACKEND_URL}/school/getSchools`);
            const data = await response.json();
            console.log('Faculties loaded:', data);
            setFaculties(data || []);
        } catch (error) {
            console.error('Error loading faculties:', error);
        }
    };

    const loadPrograms = async (facultyId) => {
        try {
            console.log('Loading programs for faculty:', facultyId);
            const response = await fetch(`${BACKEND_URL}/program/getProgramsSchool`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify({
                    school: {
                        id: parseInt(facultyId)
                    }
                })
            });
            console.log('Programs response status:', response.status);
            if (!response.ok) {
                console.error('Error response:', response.status);
                setPrograms([]);
                return;
            }
            const data = await response.json();
            console.log('Programs loaded:', data);
            setPrograms(Array.isArray(data) ? data : []);
        } catch (error) {
            console.error('Error loading programs:', error);
            setPrograms([]);
        }
    };

    const loadCourses = async (programId) => {
        try {
            const response = await fetch(`${BACKEND_URL}/course/program/${programId}`);
            if (!response.ok) {
                console.error('Error response:', response.status);
                setCourses([]);
                return;
            }
            const data = await response.json();
            setCourses(Array.isArray(data) ? data : []);
        } catch (error) {
            console.error('Error loading courses:', error);
            setCourses([]);
        }
    };

    const loadMyConvocatorias = async () => {
        try {
            const response = await fetch(`${BACKEND_URL}/monitoring-request/professor/${professorId}`, {
                headers: {
                    'Authorization': localStorage.getItem('token')
                }
            });
            if (response.ok) {
                const data = await response.json();
                setMyConvocatorias(data || []);
            }
        } catch (error) {
            console.error('Error loading convocatorias:', error);
        }
    };

    const handleCreateConvocatoria = async (e) => {
        e.preventDefault();
        
        if (!selectedCourse || !semester || !startDate || !finishDate || !requestedHours || !justification) {
            setMessage("Por favor completa todos los campos obligatorios");
            setIsOpen(true);
            return;
        }

        if (justification.length < 50) {
            setMessage("La justificación debe tener al menos 50 caracteres");
            setIsOpen(true);
            return;
        }

        setIsLoading(true);

        const convocatoriaData = {
            professorId: professorId,
            courseId: parseInt(selectedCourse),
            schoolId: parseInt(selectedFaculty),
            programId: parseInt(selectedProgram),
            requestedHours: parseInt(requestedHours),
            justification: justification,
            semester: semester,
            startDate: startDate,
            finishDate: finishDate,
            requiredAverageGrade: parseFloat(requiredAverageGrade),
            requiredCourseGrade: parseFloat(requiredCourseGrade),
            hourlyRate: parseFloat(hourlyRate)
        };

        try {
            const response = await fetch(`${BACKEND_URL}/monitoring-request/create`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                    'Authorization': localStorage.getItem('token')
                },
                body: JSON.stringify(convocatoriaData)
            });

            if (response.ok) {
                setMessage("✅ ¡Convocatoria creada exitosamente!\n\n📋 Tu convocatoria está ahora PENDIENTE DE APROBACIÓN por el Jefe de Departamento.\n\n⏳ Una vez aprobada, los estudiantes podrán postularse.");
                setIsOpen(true);
                resetForm();
                loadMyConvocatorias();
            } else {
                const error = await response.text();
                setMessage(`Error: ${error}`);
                setIsOpen(true);
            }
        } catch (error) {
            setMessage("Error al crear la convocatoria: " + error.message);
            setIsOpen(true);
        } finally {
            setIsLoading(false);
        }
    };

    const resetForm = () => {
        setSelectedFaculty("");
        setSelectedProgram("");
        setSelectedCourse("");
        setSemester("");
        setStartDate("");
        setFinishDate("");
        setRequestedHours("");
        setJustification("");
        setRequiredAverageGrade("4.0");
        setRequiredCourseGrade("4.0");
        setHourlyRate("15000");
        setPrograms([]);
        setCourses([]);
    };

    const getStatusBadge = (status) => {
        const statusMap = {
            'PENDIENTE_APROBACION_JEFE': { text: 'Pendiente Aprobacion', color: '#FF9800' },
            'CONVOCATORIA_ABIERTA': { text: 'Abierta', color: '#4CAF50' },
            'MONITOR_SELECCIONADO': { text: 'Monitor Seleccionado', color: '#2196F3' },
            'APROBADA': { text: 'Cerrada', color: '#9E9E9E' },
            'RECHAZADA': { text: 'Rechazada', color: '#F44336' },
            'CANCELADA': { text: 'Cancelada', color: '#FF9800' }
        };
        const statusInfo = statusMap[status] || { 
            text: status ? status.replace(/_/g, ' ') : 'Sin estado', 
            color: '#757575' 
        };
        return (
            <span style={{
                padding: '4px 12px',
                borderRadius: '12px',
                backgroundColor: statusInfo.color,
                color: 'white',
                fontSize: '12px',
                fontWeight: 'bold'
            }}>
                {statusInfo.text}
            </span>
        );
    };

    // Paginación
    const indexOfLastRecord = currentPage * recordsPerPage;
    const indexOfFirstRecord = indexOfLastRecord - recordsPerPage;
    const currentRecords = myConvocatorias.slice(indexOfFirstRecord, indexOfLastRecord);
    const totalPages = Math.ceil(myConvocatorias.length / recordsPerPage);

    return (
        <div className="create-monitoria-container">
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

            <div className="title-container-create-monitoria">
                <div className="title-create-monitoria">Crear Convocatoria de Monitoría</div>
                <div className="subtitle-create-monitoria">Crea una convocatoria para tu curso y recibe postulaciones de estudiantes</div>
            </div>

            <div className="create-monitoria-content">
                {/* Formulario de creación */}
                <div className="form-section">
                    <h3>Nueva Convocatoria</h3>
                    <form onSubmit={handleCreateConvocatoria}>
                        <div className="form-row">
                            <div className="form-group">
                                <label>Facultad *</label>
                                <select 
                                    value={selectedFaculty} 
                                    onChange={(e) => setSelectedFaculty(e.target.value)}
                                    required
                                >
                                    <option key="empty-faculty" value="">Seleccione una facultad</option>
                                    {faculties.map(f => (
                                        <option key={`faculty-${f.id}`} value={f.id}>{f.name}</option>
                                    ))}
                                </select>
                            </div>

                            <div className="form-group">
                                <label>Programa *</label>
                                <select 
                                    value={selectedProgram} 
                                    onChange={(e) => setSelectedProgram(e.target.value)}
                                    required
                                    disabled={!selectedFaculty}
                                >
                                    <option key="empty-program" value="">Seleccione un programa</option>
                                    {programs.map(p => (
                                        <option key={`program-${p.id}`} value={p.id}>{p.name}</option>
                                    ))}
                                </select>
                            </div>

                            <div className="form-group">
                                <label>Curso *</label>
                                <select 
                                    value={selectedCourse} 
                                    onChange={(e) => setSelectedCourse(e.target.value)}
                                    required
                                    disabled={!selectedProgram}
                                >
                                    <option key="empty-course" value="">Seleccione un curso</option>
                                    {courses.map(c => (
                                        <option key={`course-${c.id}`} value={c.id}>{c.name}</option>
                                    ))}
                                </select>
                            </div>
                        </div>

                        <div className="form-row">
                            <div className="form-group">
                                <label>Semestre *</label>
                                <input 
                                    type="text" 
                                    value={semester}
                                    onChange={(e) => setSemester(e.target.value)}
                                    placeholder="ej: 2025-1"
                                    required
                                />
                            </div>

                            <div className="form-group">
                                <label>Horas Solicitadas *</label>
                                <input 
                                    type="number" 
                                    value={requestedHours}
                                    onChange={(e) => setRequestedHours(e.target.value)}
                                    min="1"
                                    max="200"
                                    required
                                />
                            </div>

                            <div className="form-group">
                                <label>Valor por Hora *</label>
                                <input 
                                    type="number" 
                                    value={hourlyRate}
                                    onChange={(e) => setHourlyRate(e.target.value)}
                                    min="1000"
                                    step="1000"
                                    required
                                />
                            </div>
                        </div>

                        <div className="form-row">
                            <div className="form-group">
                                <label>Fecha Inicio *</label>
                                <input 
                                    type="date" 
                                    value={startDate}
                                    onChange={(e) => setStartDate(e.target.value)}
                                    required
                                />
                            </div>

                            <div className="form-group">
                                <label>Fecha Fin *</label>
                                <input 
                                    type="date" 
                                    value={finishDate}
                                    onChange={(e) => setFinishDate(e.target.value)}
                                    required
                                />
                            </div>
                        </div>

                        <div className="form-row">
                            <div className="form-group">
                                <label>Promedio Requerido *</label>
                                <input 
                                    type="number" 
                                    value={requiredAverageGrade}
                                    onChange={(e) => setRequiredAverageGrade(e.target.value)}
                                    min="0"
                                    max="5"
                                    step="0.1"
                                    required
                                />
                            </div>

                            <div className="form-group">
                                <label>Nota Curso Requerida *</label>
                                <input 
                                    type="number" 
                                    value={requiredCourseGrade}
                                    onChange={(e) => setRequiredCourseGrade(e.target.value)}
                                    min="0"
                                    max="5"
                                    step="0.1"
                                    required
                                />
                            </div>
                        </div>

                        <div className="form-group full-width">
                            <label>Justificación * (mínimo 50 caracteres)</label>
                            <textarea 
                                value={justification}
                                onChange={(e) => setJustification(e.target.value)}
                                placeholder="Explica por qué se requiere un monitor para este curso..."
                                rows="5"
                                required
                                style={{ width: '100%', padding: '10px', borderRadius: '4px', border: '1px solid #ccc' }}
                            />
                            <small style={{ color: justification.length < 50 ? 'red' : 'green' }}>
                                {justification.length} / 50 caracteres mínimos
                            </small>
                        </div>

                        <button type="submit" className="btn-create-monitoria" disabled={isLoading}>
                            {isLoading ? 'Creando...' : 'Crear Convocatoria'}
                        </button>
                    </form>
                </div>

                {/* Tabla de mis convocatorias */}
                <div className="table-section" style={{ marginTop: '40px' }}>
                    <h3>Mis Convocatorias ({myConvocatorias.length})</h3>
                    
                    {currentRecords.length === 0 ? (
                        <p style={{ textAlign: 'center', color: '#888', padding: '20px' }}>
                            No tienes convocatorias creadas aún
                        </p>
                    ) : (
                        <>
                            <table className="monitorias-table">
                                <thead>
                                    <tr>
                                        <th>ID</th>
                                        <th>Curso</th>
                                        <th>Semestre</th>
                                        <th>Horas</th>
                                        <th>Estado</th>
                                        <th>Fecha Creación</th>
                                        <th>Acciones</th>
                                    </tr>
                                </thead>
                                <tbody>
                                    {currentRecords.map((conv) => (
                                        <tr key={conv.id}>
                                            <td>{conv.id}</td>
                                            <td>{conv.courseName}</td>
                                            <td>{conv.semester}</td>
                                            <td>{conv.requestedHours}h</td>
                                            <td>{getStatusBadge(conv.status)}</td>
                                            <td>{new Date(conv.createdAt).toLocaleDateString()}</td>
                                            <td>
                                                {conv.status === 'CONVOCATORIA_ABIERTA' && (
                                                    <button 
                                                        onClick={() => navigate(`/seleccionar-monitor/${conv.id}`)}
                                                        style={{
                                                            padding: '6px 12px',
                                                            backgroundColor: '#2196F3',
                                                            color: 'white',
                                                            border: 'none',
                                                            borderRadius: '4px',
                                                            cursor: 'pointer',
                                                            marginRight: '8px'
                                                        }}
                                                    >
                                                        Ver Postulantes
                                                    </button>
                                                )}
                                            </td>
                                        </tr>
                                    ))}
                                </tbody>
                            </table>

                            {/* Paginación */}
                            {totalPages > 1 && (
                                <div className="pagination" style={{ marginTop: '20px', textAlign: 'center' }}>
                                    <button 
                                        onClick={() => setCurrentPage(prev => Math.max(1, prev - 1))}
                                        disabled={currentPage === 1}
                                        style={{ margin: '0 5px', padding: '8px 12px', cursor: 'pointer' }}
                                    >
                                        Anterior
                                    </button>
                                    <span style={{ margin: '0 15px' }}>
                                        Página {currentPage} de {totalPages}
                                    </span>
                                    <button 
                                        onClick={() => setCurrentPage(prev => Math.min(totalPages, prev + 1))}
                                        disabled={currentPage === totalPages}
                                        style={{ margin: '0 5px', padding: '8px 12px', cursor: 'pointer' }}
                                    >
                                        Siguiente
                                    </button>
                                </div>
                            )}
                        </>
                    )}
                </div>
            </div>
        </div>
    );
}

export default CreateConvocatoria;

