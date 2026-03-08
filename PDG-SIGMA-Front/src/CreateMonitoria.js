import React, { useState, useEffect } from 'react';
import './CreateMonitoria.css'; 
// import './Task.css';
import { Link } from 'react-router-dom';
import VerticalNavbar from './VerticalNavbar';
import {PopUp, PopUpUpdateBudget} from "./PopUp";
import { useNavigate } from "react-router-dom";
import { useMemo } from 'react';
import { BACKEND_URL, getApiUrl } from './config/ApiBackend';

function CreateMonitoria() {

    const [column, setColumn] = useState([]);
    const [records, setRecords] = useState([]);
    const [currentPage, setCurrentPage] = useState(1);
    const recordsPerPage = 5;
    const navigate = useNavigate();

    const [monitories, setMonitories] = useState([]); // State for Monitories list
    const [faculties, setFaculties] = useState([]); // State for Faculty options
    const [programs, setPrograms] = useState([]); // State for Program options
    const [subject, setSubject] = useState([]); // State for Subject options
    const [selectedFaculty, setSelectedFaculty] = useState(""); // Selected Faculty
    const [selectedProgram, setSelectedProgram] = useState(""); // Selected Program
    const [selectedSubject, setSelectedSubject] = useState(""); // Selected Subject
    const [selectedSemester, setSelectedSemester] = useState(""); // Selected Semester
    const [selectedStartDate, setSelectedStartDate] = useState(""); // Selected StartDate
    const [selectedFinishDate, setSelectedFinishDate] = useState(""); // Selected FinishDate
    const [requestedHours, setRequestedHours] = useState(""); // Horas solicitadas
    const [hourlyRate, setHourlyRate] = useState("15000"); // Valor por hora
    const [requiredAverageGrade, setRequiredAverageGrade] = useState("4.0"); // Promedio requerido
    const [requiredCourseGrade, setRequiredCourseGrade] = useState("4.0"); // Nota curso requerida
    const [isOpen, setIsOpen] = useState(false)
    const [message, setMessage] = useState("")
    const [change, setChange] = useState(false)
    const [nextAction, setNextAction] = useState(null); // 'upload' | null
    const [file, setFile] = useState(null); //File
    const [role, setRole] = useState(localStorage.getItem('role') || '');
    const [professors, setProfessors] = useState([]);
    const [selectedProfessorId, setSelectedProfessorId] = useState("");
    const [showBudgetPopup, setShowBudgetPopup] = useState(false);
    const [budgetRecord, setBudgetRecord] = useState(null);
    const [budgetInfo, setBudgetInfo] = useState({ remainingHours: 0 });


    // Fetch Faculty options
    useEffect(() => {
        const idProfessor = localStorage.getItem('userId');
        console.log(idProfessor);
        fetch(`${BACKEND_URL}/school/getSchools`)
            .then(res => {
                if (!res.ok) {
                    throw new Error(`HTTP error! Status: ${res.status}`);
                }
                return res.json();
            })
            .then(data => {
                if (data) {
                    setFaculties(data);
                } else {
                    console.error("Faculty data format is incorrect.");
                }
            })
            .catch(error => console.error('Error fetching faculty data:', error));

            // Cargar monitorías existentes: si es jefe, esperar selección de profesor; si es profesor, usar su propio id
            const targetId = role === 'jfedpto' ? selectedProfessorId : idProfessor;
            if (targetId || role === 'jfedpto') {
                // Refresca la tabla; si no hay profesor seleccionado (jefe), dejar lista vacía
                refreshRecords();
            }

            // Si es jefe, cargar lista de profesores asociados
            if (role === 'jfedpto') {
                fetch(`${BACKEND_URL}/department-head/${idProfessor}/professors`, {
                    headers: {
                        'Authorization': localStorage.getItem('token')
                    }
                })
                .then(res => res.ok ? res.json() : [])
                .then(data => setProfessors(Array.isArray(data) ? data : []))
                .catch(err => {
                    console.error('Error fetching professors:', err);
                    setProfessors([]);
                });
            }
    }, [role, selectedProfessorId]);

    useEffect(() => {
        const refresh = async () => {
            await refreshRecords();
        };
        refresh();
    }, [change]);

    const refreshRecords = async () => {
        try {
            const idLogged = localStorage.getItem('userId');
            const targetId = role === 'jfedpto' ? selectedProfessorId : idLogged;
            if (!targetId) { setRecords([]); return; }
            const res = await fetch(`${BACKEND_URL}/monitoring/getAllByProfessor/${targetId}`);
            if (!res.ok) return setRecords([]);
            const data = await res.json();
            setRecords(Array.isArray(data) ? data : []);
        } catch (e) { console.error('Error refreshing records:', e); }
    };

    const openBudgetPopup = async (record) => {
        try {
            // Obtener presupuesto restante del programa/semestre
            const res = await fetch(`${BACKEND_URL}/budget/${encodeURIComponent(record.program.name)}/${encodeURIComponent(record.semester)}`);
            let remaining = 0;
            if (res.ok) {
                const data = await res.json();
                remaining = Number(data.remainingHours || 0);
            } else {
                // Si no hay presupuesto configurado, usar un valor alto para no bloquear el flujo visual
                remaining = 999999;
            }
            setBudgetInfo({ remainingHours: remaining });
            setBudgetRecord(record);
            setShowBudgetPopup(true);
        } catch (e) {
            console.error('Error fetching budget:', e);
            // Fallback generoso si hay fallo de red
            setBudgetInfo({ remainingHours: 999999 });
            setBudgetRecord(record);
            setShowBudgetPopup(true);
        }
    };

    const submitBudgetUpdate = async (hours, rate) => {
        if (!budgetRecord) return;
        const current = Number(budgetRecord.estimatedHours || 0);
        const available = Number(budgetInfo.remainingHours || 0) + current;
        if (hours > available) {
            setMessage(`No se pueden asignar más horas de las disponibles. Disponibles: ${available}`);
            setIsOpen(true);
            return;
        }
        try {
            const res = await fetch(`${BACKEND_URL}/monitoring/updateBudget/${budgetRecord.id}`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                    'Authorization': localStorage.getItem('token')
                },
                body: JSON.stringify({ estimatedHours: hours, hourlyRate: rate })
            });
            const text = await res.text();
            if (!res.ok) {
                setMessage(text || 'No fue posible actualizar el presupuesto');
            } else {
                setMessage(text || 'Presupuesto actualizado');
                await refreshRecords();
            }
            setIsOpen(true);
        } catch (e) {
            console.error('Error updating budget:', e);
            setMessage('Error en el servidor al actualizar presupuesto');
            setIsOpen(true);
        } finally {
            setShowBudgetPopup(false);
            setBudgetRecord(null);
        }
    };


    // Fetch Program options
    useEffect(() => {
        const school ={
            name:selectedFaculty
        }

        fetch(`${BACKEND_URL}/program/getProgramsSchool`,{
            method: 'POST',
          headers: {
            'Content-Type': 'application/json'
          },
          body: JSON.stringify(school),
        })
            .then(res => {
                if (!res.ok) {
                    throw new Error(`HTTP error! Status: ${res.status}`);
                }
                return res.json();
            })
            .then(data => {
                if (data) {
                    setPrograms(data);
                    setSubject([])
                } else {
                    setPrograms([])
                    console.error("Program data format is incorrect.");
                }
            })
            .catch(error => console.error('Error fetching program data:', error));
    }, [selectedFaculty]);

    // Fetch Subject options
    useEffect(() => {
        const prog = {
            name:selectedProgram
        }
        fetch(`${BACKEND_URL}/course/getCoursesProgram`,{
            method: 'POST',
            headers: {
              'Content-Type': 'application/json'
            },
            body: JSON.stringify(prog),
        })
            .then(res => {
                if (!res.ok) {
                    throw new Error(`HTTP error! Status: ${res.status}`);
                }
                return res.json();
            })
            .then(data => {
                if (data) {
                    setSubject(data);
                } else {
                    setSubject([])
                    console.error("Program data format is incorrect.");
                }
            })
            .catch(error => console.error('Error fetching program data:', error));
    }, [selectedProgram]);

    // Handle change for Faculty dropdown
    const handleFacultyChange = (event) => {
        setSelectedFaculty(event.target.value);
    };

    // Handle change for Program dropdown
    const handleProgramChange = (event) => {
        setSelectedProgram(event.target.value);
    };

    // Handle change for Subject dropdown
    const handleSubjectChange = (event) => {
        setSelectedSubject(event.target.value);
    };

    // Handle change for Semester dropdown
    const handleSemesterChange = (event) => {
        setSelectedSemester(event.target.value);
    };

    // Handle change for Start Date Calendar
    const handleStartDateChange = (event) => {
        setSelectedStartDate(event.target.value);
    };

    // Handle change for Finish Date Calendar
    const handleFinishDateChange = (event) => {
        setSelectedFinishDate(event.target.value);
    };


    const handleUpload = () => {
        console.log("handleUpload llamado");
        console.log("Role:", role);
        console.log("selectedProfessorId:", selectedProfessorId);
        
        // Validación para jefe de departamento
        if (role === 'jfedpto' && !selectedProfessorId) {
            setMessage("Por favor selecciona un profesor responsable antes de cargar el archivo CSV.");
            setIsOpen(true);
            return;
        }

        // Abrir selector de archivos directamente
        console.log("Creando input file...");
        const input = document.createElement("input");
        input.type = "file";
        input.accept = ".xlsx, .xls, .csv";

        input.onchange = async (event) => {
            console.log("Archivo seleccionado:", event.target.files[0]);
            const file = event.target.files[0];
            
            if (!file) {
                setMessage("No se seleccionó ningún archivo.");
                setIsOpen(true);
                return;
            }

            const formData = new FormData();
            formData.append("file", file);

            let idProfessor = localStorage.getItem('userId');
            if (role === 'jfedpto') {
                idProfessor = selectedProfessorId;
            }

            console.log("Enviando archivo al backend para profesor:", idProfessor);

            try {
                const response = await fetch(`${BACKEND_URL}/monitoring/createAll/${idProfessor}`, {
                    method: "POST",
                    headers: {
                        'Authorization': localStorage.getItem('token')
                    },
                    body: formData,
                });

                const message = await response.text();
                console.log("Respuesta del servidor:", message);
                setMessage(message);
                setIsOpen(true);
                
                // Refrescar tabla tras cargue exitoso
                if (response.ok) {
                    await refreshRecords();
                }

            } catch (error) {
                console.error("Error:", error);
                setMessage("Error al conectar con el servidor");
                setIsOpen(true);
            }
        };

        console.log("Haciendo click en input...");
        input.click();
        console.log("Click ejecutado");
    };



    const handleDelete = async (id) => {
        try {
            const responseDelete = await fetch(`${BACKEND_URL}/monitoring/deleteMonitoring/${id}`, {
                method: "DELETE",
                headers: {
                    "Content-Type": "application/json",
                    'Authorization': localStorage.getItem('token')
                }
            });
            const answer = await responseDelete.json();
            if (answer) {
                setMessage("Se ha eliminado la monitoría");
                await refreshRecords();
            } else {
                setMessage("La monitoria no pudo ser eliminada debido a que está asociada a monitores o postulantes a monitoría. Asegúrate de revisar el proceso de postulación");
            }
            setIsOpen(true);
        } catch (error) {
            console.error("Error deleting data:", error);
            setMessage("Error en el servidor: No ha sido posible eliminar la monitoría");
            setIsOpen(true);
        }
    };

    const handleClose = () => {
        setIsOpen(false);
        setChange(!change);
    };

    const handleCreate = async () => {
        const data = {
            programName: selectedProgram,
            courseName: selectedSubject,
            schoolName: selectedFaculty,
            start: selectedStartDate,
            finish: selectedFinishDate,
            professorId: role === 'jfedpto' ? selectedProfessorId : localStorage.getItem('userId'),
            semester: selectedSemester,
            estimatedHours: parseInt(requestedHours) || 0,
            hourlyRate: parseFloat(hourlyRate) || 15000,
            requiredAverageGrade: parseFloat(requiredAverageGrade) || 4.0,
            requiredCourseGrade: parseFloat(requiredCourseGrade) || 4.0,
        };

        // Validaciones mínimas
        if (!data.schoolName || !data.programName || !data.courseName || !data.semester || !data.start || !data.finish) {
            setMessage("Por favor completa todos los campos requeridos antes de confirmar.");
            setIsOpen(true);
            return;
        }
        if (!requestedHours || parseInt(requestedHours) <= 0) {
            setMessage("Debes ingresar las horas solicitadas (mayor a 0).");
            setIsOpen(true);
            return;
        }
        if (role === 'jfedpto' && !selectedProfessorId) {
            setMessage("Selecciona un profesor responsable antes de confirmar.");
            setIsOpen(true);
            return;
        }

        try {
            const response = await fetch(`${BACKEND_URL}/monitoring/create`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                    'Authorization': localStorage.getItem('token')
                },
                body: JSON.stringify(data),
            });
            const text = await response.text();
            setMessage(text);
            setIsOpen(true);
            
            // Limpiar formulario después de crear exitosamente
            if (response.ok) {
                setSelectedFaculty("");
                setSelectedProgram("");
                setSelectedSubject("");
                setSelectedSemester("");
                setSelectedStartDate("");
                setSelectedFinishDate("");
                setRequestedHours("");
                setHourlyRate("15000");
                setRequiredAverageGrade("4.0");
                setRequiredCourseGrade("4.0");
                setPrograms([]);
                setSubject([]);
            }
            
            await refreshRecords();
        } catch (error) {
            console.error('Error creating monitoring:', error);
            setMessage('Error al crear la monitoría.');
            setIsOpen(true);
        }
    };

    const indexOfLastRecord = currentPage * recordsPerPage;
    const indexOfFirstRecord = indexOfLastRecord - recordsPerPage;
    const currentRecords = records.slice(indexOfFirstRecord, indexOfLastRecord);

    const totalPages = Math.ceil(records.length / recordsPerPage);

    const nextPage = () => {
        if (currentPage < totalPages) {
            setCurrentPage(currentPage + 1);
        }
    };

    const prevPage = () => {
        if (currentPage > 1) {
            setCurrentPage(currentPage - 1);
        }
    };

    const processedRecords = useMemo(() => {
        return currentRecords.map(record => {
            const startDateR = record.start.split('T')[0];
            const endDateR = record.finish.split('T')[0];
            return {
                ...record,
                startFormatted: startDateR,
                endFormatted: endDateR,
                estimatedHours: record.estimatedHours || 0,
                hourlyRate: record.hourlyRate || 0,
                totalCost: (Number(record.estimatedHours || 0) * Number(record.hourlyRate || 0))
            };
        });
    }, [currentRecords]);

    const formatCurrency = (value) => {
        const n = Number(value);
        if (isNaN(n)) return 0;
        try {
            return new Intl.NumberFormat('es-CO', { style: 'currency', currency: 'COP', maximumFractionDigits: 0 }).format(n);
        } catch (e) {
            return n;
        }
    };

    const checkDates = (startPostulation, endPostulation) => {
        
        const startDateR = startPostulation.split('T')[0];
        const endDateR = endPostulation.split('T')[0];
        
        return startDateR, endDateR
    };

    return (
        <div className="create-monitoria-container">
            <VerticalNavbar />
            
            {isOpen && <PopUp message={message} onClose={() => handleClose()} />}

            <div className="title-container-create-monitoria">
                <div className="title-create-monitoria">Crear/Cargar Monitorías</div>
                <div className="subtitle-create-monitoria">Carga individual o masiva con CSV/Excel</div>
            </div>

            <div className="create-monitoria-content">
                {/* Botón de carga CSV */}
                <div style={{ display: 'flex', justifyContent: 'flex-end', marginBottom: '20px' }}>
                    <button className="btn-upload-csv" onClick={handleUpload}>
                        📂 Cargar Datos (CSV/Excel)
                    </button>
                </div>

                {/* Formulario de creación */}
                <div className="form-section">
                    <h3>Nueva Monitoría</h3>
                    <form onSubmit={(e) => { e.preventDefault(); handleCreate(); }}>
                        {/* Selección de profesor visible solo para Jefe de Departamento */}
                        {role === 'jfedpto' && (
                            <div className="form-row">
                                <div className="form-group full-width">
                                    <label>Profesor responsable *</label>
                                    <select 
                                        value={selectedProfessorId}
                                        onChange={(e) => setSelectedProfessorId(e.target.value)}
                                        required
                                    >
                                        <option value="">Seleccione un profesor</option>
                                        {professors.map(p => (
                                            <option key={p.id} value={p.id}>{p.name} ({p.id})</option>
                                        ))}
                                    </select>
                                </div>
                            </div>
                        )}

                        <div className="form-row">
                            <div className="form-group">
                                <label>Facultad *</label>
                                <select 
                                    value={selectedFaculty} 
                                    onChange={handleFacultyChange}
                                    required
                                >
                                    <option value="">Seleccione una facultad</option>
                                    {faculties.map(faculty => (
                                        <option key={faculty.name} value={faculty.name}>
                                            {faculty.name}
                                        </option>
                                    ))}
                                </select>
                            </div>

                            <div className="form-group">
                                <label>Programa *</label>
                                <select 
                                    value={selectedProgram} 
                                    onChange={handleProgramChange}
                                    required
                                    disabled={!selectedFaculty}
                                >
                                    <option value="">Seleccione un programa</option>
                                    {programs.map(program => (
                                        <option key={program.name} value={program.name}>
                                            {program.name}
                                        </option>
                                    ))}
                                </select>
                            </div>

                            <div className="form-group">
                                <label>Curso *</label>
                                <select 
                                    value={selectedSubject} 
                                    onChange={handleSubjectChange}
                                    required
                                    disabled={!selectedProgram}
                                >
                                    <option value="">Seleccione un curso</option>
                                    {subject.map(subj => (
                                        <option key={subj.name} value={subj.name}>
                                            {subj.name}
                                        </option>
                                    ))}
                                </select>
                            </div>
                        </div>

                        <div className="form-row">
                            <div className="form-group">
                                <label>Semestre *</label>
                                <input 
                                    type="text" 
                                    value={selectedSemester}
                                    onChange={handleSemesterChange}
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
                                    placeholder="Ej: 80"
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
                                    placeholder="Ej: 15000"
                                    required
                                />
                            </div>
                        </div>

                        <div className="form-row">
                            <div className="form-group">
                                <label>Fecha Inicio *</label>
                                <input 
                                    type="date" 
                                    value={selectedStartDate}
                                    onChange={handleStartDateChange}
                                    required
                                />
                            </div>

                            <div className="form-group">
                                <label>Fecha Fin *</label>
                                <input 
                                    type="date" 
                                    value={selectedFinishDate}
                                    onChange={handleFinishDateChange}
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
                                    placeholder="Ej: 4.0"
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
                                    placeholder="Ej: 4.0"
                                    required
                                />
                            </div>
                        </div>

                        <button type="submit" className="btn-create-monitoria">
                            Crear Monitoría
                        </button>
                    </form>
                </div>

                {/* Tabla de monitorías */}
                <div className="table-section" style={{ marginTop: '40px' }}>
                    <h3>Mis Monitorías ({records.length})</h3>
                    
                    {currentRecords.length === 0 ? (
                        <p style={{ textAlign: 'center', color: '#88898c', padding: '20px' }}>
                            No tienes monitorías creadas aún
                        </p>
                    ) : (
                        <>
                            <table className="monitorias-table">
                                <thead>
                                    <tr>
                                        <th>Facultad</th>
                                        <th>Programa</th>
                                        <th>Curso</th>
                                        <th>Semestre</th>
                                        <th>Fecha Inicio</th>
                                        <th>Fecha Fin</th>
                                        <th>Horas</th>
                                        <th>Valor Hora</th>
                                        <th>Costo Total</th>
                                        <th>Acciones</th>
                                        {role === 'jfedpto' && <th>Presupuesto</th>}
                                    </tr>
                                </thead>
                                <tbody>
                                    {processedRecords.map((record, i) => (
                                        <tr key={i}>
                                            <td>{record.school.name}</td>
                                            <td>{record.program.name}</td>
                                            <td>{record.course.name}</td>
                                            <td>{record.semester}</td>
                                            <td>{record.startFormatted}</td>
                                            <td>{record.endFormatted}</td>
                                            <td>{record.estimatedHours}h</td>
                                            <td>{formatCurrency(record.hourlyRate)}</td>
                                            <td>{formatCurrency(record.totalCost)}</td>
                                            <td>
                                                <button 
                                                    onClick={() => handleDelete(record.id)}
                                                    style={{
                                                        padding: '6px 12px',
                                                        backgroundColor: '#e9683b',
                                                        color: 'white',
                                                        border: 'none',
                                                        borderRadius: '4px',
                                                        cursor: 'pointer'
                                                    }}
                                                >
                                                    Eliminar
                                                </button>
                                            </td>
                                            {role === 'jfedpto' && (
                                                <td>
                                                    <button 
                                                        onClick={() => openBudgetPopup(record)}
                                                        style={{
                                                            padding: '6px 12px',
                                                            backgroundColor: '#5454e9',
                                                            color: 'white',
                                                            border: 'none',
                                                            borderRadius: '4px',
                                                            cursor: 'pointer'
                                                        }}
                                                    >
                                                        Editar
                                                    </button>
                                                </td>
                                            )}
                                        </tr>
                                    ))}
                                </tbody>
                            </table>

                            {/* Paginación */}
                            {totalPages > 1 && (
                                <div className="pagination" style={{ marginTop: '20px', textAlign: 'center' }}>
                                    <button 
                                        onClick={prevPage}
                                        disabled={currentPage === 1}
                                        style={{ margin: '0 5px', padding: '8px 12px', cursor: 'pointer' }}
                                    >
                                        Anterior
                                    </button>
                                    <span style={{ margin: '0 15px' }}>
                                        Página {currentPage} de {totalPages}
                                    </span>
                                    <button 
                                        onClick={nextPage}
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

            <PopUpUpdateBudget
                show={showBudgetPopup}
                onClose={() => { setShowBudgetPopup(false); setBudgetRecord(null); }}
                onSubmit={submitBudgetUpdate}
                initialHours={budgetRecord ? (budgetRecord.estimatedHours || 0) : 0}
                initialRate={budgetRecord ? (budgetRecord.hourlyRate || 0) : 0}
                remainingHours={budgetInfo.remainingHours}
                currentHours={budgetRecord ? (budgetRecord.estimatedHours || 0) : 0}
            />

            </div>
        </div>
    );
}

export default CreateMonitoria;

