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


    const handleUpload = async () => {
                // 1) Mostrar anuncio previo y luego abrir el selector de archivo
                const infoMsg = (
                        <>
                                <p><strong>Si vas a cargar múltiples monitorias en "Cargar Datos", asegúrate de que el archivo cumpla con el formato:</strong></p>
                                <ul>
                                        <li>El archivo puede ser <strong>.xlsx</strong> o <strong>.csv</strong>.</li>
                                        <li>Cada fila representa una monitoría diferente.</li>
                                        <li>No dejes columnas vacías si son obligatorias.</li>
                                        <li>Fechas con formato <em>dd/mm/aaaa</em> (en CSV usar dd-mm-aaaa).</li>
                                </ul>
                                <p><strong>Campos obligatorios:</strong></p>
                                <ul>
                                        <li>FACULTAD, PROGRAMA, CURSO</li>
                                        <li>FECHA INICIO, FECHA FINALIZACION, PERIODO</li>
                                        <li>PROMEDIO ACUMULADO, PROMEDIO MATERIA, HORAS ESTIMADAS, VALOR HORA</li>
                                </ul>
                <p>
                    Puedes descargar un ejemplo del formato correcto haciendo clic aquí:&nbsp;
                    <a
                    href="https://github.com/danielaolartebo/PDG-SIGMA/tree/draft/doc/Formatos"
                    target="_blank"
                    rel="noopener noreferrer"
                    >
                    Ejemplo Formato
                    </a>
                </p>
                        </>
                );
                setMessage(infoMsg);
                setNextAction('upload');
                setIsOpen(true);
        };

        const openFilePicker = () => {
                const input = document.createElement("input");
                input.type = "file";
                input.accept = ".xlsx, .xls, .csv";

                input.onchange = async (event) => {
          const file = event.target.files[0];
    
          if (!file) {
            // alert("No se seleccionó ningún archivo.");
            setMessage("No se seleccionó ningún archivo.")
                        setIsOpen(true)
            
            return;
          }
    
          const formData = new FormData();
          formData.append("file", file);

                    let idProfessor = localStorage.getItem('userId')
                    if (role === 'jfedpto') {
                        if (!selectedProfessorId) {
                            setMessage("Selecciona un profesor antes de cargar el archivo");
                            setIsOpen(true);
                            return;
                        }
                        idProfessor = selectedProfessorId;
                    }
    
          try {
                        const response = await fetch(`${BACKEND_URL}/monitoring/createAll/${idProfessor}`, {
              method: "POST",
                headers: {
                    'Authorization':localStorage.getItem('token')
                },
              body: formData,
              
            });
            
            const message = await response.text();
            // alert(message);
            setMessage(message)
                        setIsOpen(true)
                        // refrescar tabla tras cargue
                        await refreshRecords();

          } catch (error) {
            console.error("Error:", error);
            // alert("Error al conectar con el servidor");
            setMessage("Error al conectar con el servidor")
                        setIsOpen(true)
            
          }
        };
    
        input.click(); // Simula el clic para abrir el explorador de archivos
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
        if (nextAction === 'upload') {
            setNextAction(null);
            setTimeout(() => openFilePicker(), 0);
        } else {
            setChange(!change);
        }
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
        };

        // Validaciones mínimas
        if (!data.schoolName || !data.programName || !data.courseName || !data.semester || !data.start || !data.finish) {
            setMessage("Por favor completa todos los campos requeridos antes de confirmar.");
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
        <div className="cm-task-container">
            {/* Load file button starts */}
            <button className="top-right-button" onClick={handleUpload}>Cargar datos</button>
            {/* Load file button ends */}

            {/* Ventana emergente */}
            <PopUp
                show={isOpen}
                onClose={() => handleClose()}
            >
                {message}
            </PopUp>

            <VerticalNavbar />
            <div className="cm-content">
                {/* Title begins */}
                <div className="cm-title-container">
                                        <h2 className="cm-title">Crear/Cargar monitorías</h2>
                </div>
                {/* Title ends */}

                <form className="cm-grid-container">

                                        {/* Selección de profesor visible solo para Jefe de Departamento */}
                                        {role === 'jfedpto' && (
                                            <>
                                                <label>Profesor responsable</label>
                                                <select className="cm-program"
                                                                value={selectedProfessorId}
                                                                onChange={(e)=>setSelectedProfessorId(e.target.value)}>
                                                    <option value=""> Seleccionar </option>
                                                    {professors.map(p => (
                                                        <option key={p.id} value={p.id}>{p.name} ({p.id})</option>
                                                    ))}
                                                </select>
                                            </>
                                        )}

                    {/* Facultad */}
                    <label>Nombre de facultad</label>
                    <select className="cm-faculty"
                            id="faculty-dropdown" 
                            value={selectedFaculty} 
                            onChange={handleFacultyChange}>
                        <option value=""> Seleccionar </option>
                        {faculties.map(faculty => (
                            <option key={faculty.name} value={faculty.name}>
                                {faculty.name}
                            </option>
                        ))}
                    </select>

                    {/* Programa */}
                    <label>Nombre de programa</label>
                    <select className="cm-program"
                            id="program-dropdown" 
                            value={selectedProgram} 
                            onChange={handleProgramChange}>
                        <option value=""> Seleccionar </option>
                        {programs.map(program => (
                            <option key={program.name} value={program.name}>
                                {program.name}
                            </option>
                        ))}
                    </select>

                    {/* Materia/Curso */}
                    <label>Curso académico</label>
                    <select className="cm-program"
                            id="subject-dropdown" 
                            value={selectedSubject} 
                            onChange={handleSubjectChange}>
                        <option value=""> Seleccionar </option>
                        {subject.map(subject => (
                            <option key={subject.name} value={subject.name}>
                                {subject.name}
                            </option>
                        ))}
                    </select>

                    {/* Periodo académico */}
                    <label>Periodo académico</label>
                    <select value={selectedSemester} onChange={handleSemesterChange}>
                    <option value=""> Seleccionar </option>
                        <option key='2024-1' value='2024-1'>
                            2024-1
                        </option>
                        <option key='2024-2' value='2024-2'>
                                2024-2
                        </option>
                    </select>


                    {/* Inicio de convocatoria en dos columnas */}
                    <label>Inicio de convocatoria</label>
                    <input type="date" className="cm-input-date" value={selectedStartDate} onChange={handleStartDateChange}/>

                    {/* Fin de convocatoria */}
                    <label>Fin de convocatoria</label>
                    <input type="date" className="cm-input-date" value={selectedFinishDate} onChange={handleFinishDateChange} />


                    {/* Requisitos */}
                    {/*
                    <label>Promedio acumulado:</label>
                    <input type="text" placeholder="4.5" className="cm-input-text-box"/>
                    <label>Promedio materia:</label>
                    <input type="text" placeholder="4.5" className="cm-input-text-box"/> */}

                    {/* Añadir nuevo requisito */}  
                   {/*  <label></label>
                    <Link to="#"></Link>

                    <label></label>
                    <Link to="#">¿Añadir nuevo requisito?</Link> */}
                    
                    {/* Botón de confirmación */}
                    <button 
                        type="button" 
                        className="cm-confirm-button" 
                        onClick={handleCreate}>
                        Confirmar
                    </button>
                </form>

            {/* Table starts */}

            <div className="main-container">
                <div className='table-main-container'>
                    <table className="table" id="table">
                        <thead>
                            <tr>
                                <th className="table-head"> Facultad </th>
                                <th className="table-head"> Programa </th>
                                <th className="table-head"> Materia/Curso </th>
                                <th className="table-head"> Periodo académico </th>
                                <th className="table-head"> Inicio de convocatoria</th>
                                <th className="table-head"> Fin de convocatoria</th>
                                <th className="table-head"> Horas </th>
                                <th className="table-head"> Valor hora </th>
                                <th className="table-head"> Costo </th>
                                <th className="table-head"> </th>
                                {role === 'jfedpto' && (<th className="table-head"> Presupuesto </th>)}
                            </tr>
                        </thead>
                        <tbody>
                                {processedRecords.map((record, i) => {
                                    
                                    return (
                                        <tr key={i}>
                                            <td className="table-data">{record.school.name}</td>
                                            <td className="table-data">{record.program.name}</td>
                                            <td className="table-data">{record.course.name}</td>
                                            <td className="table-data">{record.semester}</td>
                                            <td className="table-data">{record.startFormatted}</td>
                                            <td className="table-data">{record.endFormatted}</td>
                                            <td className="table-data">{record.estimatedHours}</td>
                                            <td className="table-data">{formatCurrency(record.hourlyRate)}</td>
                                            <td className="table-data">{formatCurrency(record.totalCost)}</td>
                                            <td className="table-data">
                                                <div className="requirement-container">
                                                    {/* <button className="edit-button">Editar</button> */}
                                                    <button className="cancel-button" onClick={() =>handleDelete(record.id)}>Eliminar</button>
                                                </div>
                                            </td>
                                            {role === 'jfedpto' && (
                                                <td className="table-data">
                                                    <button className="edit-button" onClick={() => openBudgetPopup(record)}>
                                                        Editar presupuesto
                                                    </button>
                                                </td>
                                            )}
                                        </tr>
                                    );
                                })}
                                
                    </tbody>
                    </table>
                </div>

            <div className="div-pagination">
                <div className="pagination-info">
                    Mostrando {indexOfFirstRecord + 1} - {Math.min(indexOfLastRecord, records.length)} de {records.length} resultados
                </div>

                <div className="main-pagination">
                    <div className="pagination">
                        <button onClick={prevPage} disabled={currentPage === 1}>Anterior</button>
                        {[...Array(totalPages)].map((_, index) => (
                            <button 
                                key={index} 
                                onClick={() => setCurrentPage(index + 1)}
                                className={currentPage === index + 1 ? 'active' : ''}
                            >
                                {index + 1}
                            </button>
                        ))}
                        <button onClick={nextPage} disabled={currentPage === totalPages}>Siguiente</button>
                    </div>
                </div>
            </div>
        </div>

        {/* Table ends */}

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