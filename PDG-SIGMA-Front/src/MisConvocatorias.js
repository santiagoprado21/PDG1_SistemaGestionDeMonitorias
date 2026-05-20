import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import './CreateConvocatoria.css';
import VerticalNavbar from './VerticalNavbar';
import { PopUp } from "./PopUp";
import { BACKEND_URL } from './config/ApiBackend';
import LoadingSpinner from './LoadingSpinner';
import { Clock3, Megaphone, UserRound, X, ClipboardList, Search, Users, ChevronLeft, ChevronRight } from 'lucide-react';

function MisConvocatorias() {
    const iconProps = {
        size: 14,
        strokeWidth: 2,
        strokeLinecap: 'butt',
        strokeLinejoin: 'miter'
    };

    // Estados activos que se muestran por defecto en la vista principal
    const ACTIVE_STATUSES = ['PENDIENTE_APROBACION_JEFE', 'CONVOCATORIA_ABIERTA', 'MONITOR_SELECCIONADO'];

    const navigate = useNavigate();
    const [myConvocatorias, setMyConvocatorias] = useState([]);
    const [filteredConvocatorias, setFilteredConvocatorias] = useState([]);
    const [currentPage, setCurrentPage] = useState(1);
    const [recordsPerPage, setRecordsPerPage] = useState(8);
    
    // Filtros — por defecto muestra solo convocatorias activas
    const [filterStatus, setFilterStatus] = useState("ACTIVAS");
    const [filterCourse, setFilterCourse] = useState("Todos");
    const [filterSemester, setFilterSemester] = useState("Todos");
    
    const [isOpen, setIsOpen] = useState(false);
    const [message, setMessage] = useState("");
    const [isLoading, setIsLoading] = useState(false);

    const professorId = localStorage.getItem('userId');

    useEffect(() => {
        loadMyConvocatorias();
    }, []);
    
    useEffect(() => {
        applyFilters();
    }, [myConvocatorias, filterStatus, filterCourse, filterSemester]);

    const loadMyConvocatorias = async () => {
        setIsLoading(true);
        try {
            const response = await fetch(`${BACKEND_URL}/monitoring-request/professor/${professorId}`, {
                method: 'GET',
                headers: {
                    'Content-Type': 'application/json',
                    'Authorization': localStorage.getItem('token')
                }
            });

            if (!response.ok) {
                throw new Error('Error al cargar convocatorias');
            }

            const data = await response.json();
            console.log('Convocatorias del profesor:', data);
            setMyConvocatorias(data || []);
        } catch (error) {
            console.error("Error loading convocatorias:", error);
            setMessage("Error cargando convocatorias: " + error.message);
            setIsOpen(true);
        } finally {
            setIsLoading(false);
        }
    };

    const applyFilters = () => {
        let filtered = [...myConvocatorias];

        // Filtro por estado — "ACTIVAS" agrupa los estados relevantes para el profesor
        if (filterStatus === "ACTIVAS") {
            filtered = filtered.filter(conv => ACTIVE_STATUSES.includes(conv.status));
        } else if (filterStatus !== "Todos") {
            filtered = filtered.filter(conv => conv.status === filterStatus);
        }

        // Filtro por curso
        if (filterCourse !== "Todos") {
            filtered = filtered.filter(conv => conv.courseName === filterCourse);
        }

        // Filtro por semestre
        if (filterSemester !== "Todos") {
            filtered = filtered.filter(conv => conv.semester === filterSemester);
        }

        setFilteredConvocatorias(filtered);
        setCurrentPage(1);
    };

    const handleClose = () => {
        setIsOpen(false);
    };

    // Opciones únicas para filtros
    const uniqueCourses = ["Todos", ...new Set(myConvocatorias.map(c => c.courseName).filter(Boolean))];
    const uniqueStatuses = ["ACTIVAS", "Todos", ...new Set(myConvocatorias.map(c => c.status).filter(Boolean))];
    const uniqueSemesters = ["Todos", ...new Set(myConvocatorias.map(c => c.semester).filter(Boolean)).values()].sort().reverse();

    // Conteo de activas para informar al usuario
    const activeCount = myConvocatorias.filter(c => ACTIVE_STATUSES.includes(c.status)).length;

    // Paginación
    const indexOfLastRecord = currentPage * recordsPerPage;
    const indexOfFirstRecord = indexOfLastRecord - recordsPerPage;
    const currentRecords = filteredConvocatorias.slice(indexOfFirstRecord, indexOfLastRecord);
    const totalPages = Math.ceil(filteredConvocatorias.length / recordsPerPage);

    const getStatusBadge = (status) => {
        const statusConfig = {
            'PENDIENTE_APROBACION_JEFE': { label: 'Pendiente Aprobacion', color: '#e4eb60', icon: <Clock3 {...iconProps} /> },
            'CONVOCATORIA_ABIERTA': { label: 'Abierta', color: '#4cb979', icon: <Megaphone {...iconProps} /> },
            'MONITOR_SELECCIONADO': { label: 'Monitor Seleccionado', color: '#5454e9', icon: <UserRound {...iconProps} /> },
            'APROBADA': { label: 'Cerrada', color: '#88898c' },
            'RECHAZADA': { label: 'Rechazada', color: '#e9683b', icon: <X {...iconProps} /> },
            'CANCELADA': { label: 'Cancelada', color: '#e4eb60', icon: <Clock3 {...iconProps} /> }
        };

        const config = statusConfig[status] || { 
            label: status ? status.replace(/_/g, ' ') : 'Sin estado', 
            color: '#e4eb60', 
            icon: null 
        };

        return (
            <span style={{
                padding: '4px 12px',
                backgroundColor: config.color,
                color: 'white',
                borderRadius: '0',
                fontSize: '0.85rem',
                fontWeight: '600',
                display: 'inline-flex',
                alignItems: 'center',
                gap: '6px'
            }}>
                {config.icon}
                {config.label}
            </span>
        );
    };

    const getStatusLabel = (status) => {
        const labels = {
            'PENDIENTE_APROBACION_JEFE': 'Pendiente Aprobacion',
            'CONVOCATORIA_ABIERTA': 'Abierta',
            'MONITOR_SELECCIONADO': 'Monitor Seleccionado',
            'APROBADA': 'Completada',
            'RECHAZADA': 'Rechazada',
            'CANCELADA': 'Cancelada'
        };
        return labels[status] || status.replace(/_/g, ' ');
    };

    return (
        <div className="create-monitoria-container">
            <VerticalNavbar />
            
            <div className="create-monitoria-main">
                <div className="title-container-create-monitoria prof-page-header">
                    <div className="title-content-create-monitoria">
                        <div className="title-create-monitoria prof-page-title">Mis Convocatorias</div>
                        <div className="subtitle-create-monitoria prof-page-subtitle">
                            Gestiona y da seguimiento a todas tus convocatorias de monitoría
                        </div>
                    </div>
                </div>

                <div className="create-monitoria-content">
                    {/* Sección de filtros */}
                    <div className="filters-section-aprobar-hu010" style={{ marginBottom: '20px', flexWrap: 'wrap', gap: '12px' }}>
                        <div className="filter-group">
                            <label>Estado:</label>
                            <select 
                                value={filterStatus} 
                                onChange={(e) => { setFilterStatus(e.target.value); }}
                                style={{
                                    padding: '8px 12px',
                                    border: '2px solid #cecfd4',
                                    borderRadius: '0',
                                    fontSize: '1rem',
                                    cursor: 'pointer',
                                    minWidth: '180px'
                                }}
                            >
                                {uniqueStatuses.map((status, idx) => (
                                    <option key={idx} value={status}>
                                        {status === "Todos" ? "Todas" : status === "ACTIVAS" ? `Activas (${activeCount})` : getStatusLabel(status)}
                                    </option>
                                ))}
                            </select>
                        </div>
                        <div className="filter-group">
                            <label>Semestre:</label>
                            <select 
                                value={filterSemester} 
                                onChange={(e) => setFilterSemester(e.target.value)}
                                style={{
                                    padding: '8px 12px',
                                    border: '2px solid #cecfd4',
                                    borderRadius: '0',
                                    fontSize: '1rem',
                                    cursor: 'pointer',
                                    minWidth: '140px'
                                }}
                            >
                                {uniqueSemesters.map((sem, idx) => (
                                    <option key={idx} value={sem}>{sem}</option>
                                ))}
                            </select>
                        </div>
                        <div className="filter-group">
                            <label>Curso:</label>
                            <select 
                                value={filterCourse} 
                                onChange={(e) => setFilterCourse(e.target.value)}
                                style={{
                                    padding: '8px 12px',
                                    border: '2px solid #cecfd4',
                                    borderRadius: '0',
                                    fontSize: '1rem',
                                    cursor: 'pointer',
                                    minWidth: '200px'
                                }}
                            >
                                {uniqueCourses.map((course, idx) => (
                                    <option key={idx}>{course}</option>
                                ))}
                            </select>
                        </div>
                        <div className="filter-group">
                            <label>Por página:</label>
                            <select
                                value={recordsPerPage}
                                onChange={(e) => { setRecordsPerPage(Number(e.target.value)); setCurrentPage(1); }}
                                style={{
                                    padding: '8px 12px',
                                    border: '2px solid #cecfd4',
                                    borderRadius: '0',
                                    fontSize: '1rem',
                                    cursor: 'pointer',
                                    minWidth: '80px'
                                }}
                            >
                                {[5, 8, 10, 20].map(n => <option key={n} value={n}>{n}</option>)}
                            </select>
                        </div>
                        <div className="filter-stats">
                            <span>Mostrando: <strong>{filteredConvocatorias.length}</strong> de {myConvocatorias.length}</span>
                        </div>
                    </div>

                    {/* Tabla de mis convocatorias */}
                    <div className="table-section">
                        <h3>Mis Convocatorias ({filteredConvocatorias.length})</h3>
                        
                        {isLoading ? (
                            <LoadingSpinner />
                        ) : currentRecords.length === 0 ? (
                            <div style={{ 
                                textAlign: 'center', 
                                padding: '40px', 
                                background: 'white',
                                borderRadius: '0',
                                boxShadow: '0 2px 8px rgba(0,0,0,0.1)'
                            }}>
                                <p style={{ color: '#88898c', fontSize: '1.1rem' }}>
                                    {myConvocatorias.length === 0 
                                        ? (<><ClipboardList {...iconProps} style={{ marginRight: '6px', verticalAlign: 'text-bottom' }} />No tienes convocatorias creadas aun</>) 
                                        : (<><Search {...iconProps} style={{ marginRight: '6px', verticalAlign: 'text-bottom' }} />No se encontraron convocatorias con los filtros aplicados</>)}
                                </p>
                            </div>
                        ) : (
                            <>
                                <table className="monitorias-table" style={{ width: '100%', borderCollapse: 'collapse' }}>
                                    <thead>
                                        <tr>
                                            <th style={{ width: '5%' }}>ID</th>
                                            <th style={{ width: '20%' }}>Curso</th>
                                            <th style={{ width: '12%' }}>Periodo</th>
                                            <th style={{ width: '8%' }}>Horas</th>
                                            <th style={{ width: '15%' }}>Estado</th>
                                            <th style={{ width: '10%' }}>Postulantes</th>
                                            <th style={{ width: '12%' }}>Creada</th>
                                            <th style={{ width: '18%' }}>Acciones</th>
                                        </tr>
                                    </thead>
                                    <tbody>
                                        {currentRecords.map((conv) => (
                                            <tr key={conv.id} style={{ borderBottom: '1px solid #cecfd4' }}>
                                                <td style={{ padding: '12px 8px', textAlign: 'center' }}>
                                                    <strong>#{conv.id}</strong>
                                                </td>
                                                <td style={{ padding: '12px 8px' }}>
                                                    <div style={{ fontWeight: '600', color: '#000000' }}>
                                                        {conv.courseName}
                                                    </div>
                                                    <div style={{ fontSize: '0.85rem', color: '#88898c' }}>
                                                        {conv.programName}
                                                    </div>
                                                </td>
                                                <td style={{ padding: '12px 8px', textAlign: 'center' }}>
                                                    {conv.semester}
                                                </td>
                                                <td style={{ padding: '12px 8px', textAlign: 'center' }}>
                                                    <strong style={{ color: '#5454e9' }}>{conv.requestedHours}h</strong>
                                                </td>
                                                <td style={{ padding: '12px 8px', textAlign: 'center' }}>
                                                    {getStatusBadge(conv.status)}
                                                </td>
                                                <td style={{ padding: '12px 8px', textAlign: 'center' }}>
                                                    {conv.status === 'CONVOCATORIA_ABIERTA' || conv.status === 'MONITOR_SELECCIONADO' || conv.status === 'APROBADA' ? (
                                                        <span style={{
                                                            padding: '6px 12px',
                                                            background: conv.applicationCount > 0 ? '#cecfd4' : '#ffffff',
                                                            borderRadius: '0',
                                                            fontWeight: '700',
                                                            fontSize: '1rem',
                                                            color: conv.applicationCount > 0 ? '#5454e9' : '#88898c'
                                                        }}>
                                                            {conv.applicationCount || 0}
                                                        </span>
                                                    ) : (
                                                        <span style={{ color: '#88898c' }}>-</span>
                                                    )}
                                                </td>
                                                <td style={{ padding: '12px 8px', textAlign: 'center', fontSize: '0.9rem' }}>
                                                    {new Date(conv.createdAt).toLocaleDateString('es-ES')}
                                                </td>
                                                <td style={{ padding: '12px 8px', textAlign: 'center' }}>
                                                    {conv.status === 'CONVOCATORIA_ABIERTA' && (
                                                        <button 
                                                            onClick={() => navigate(`/seleccionar-monitor/${conv.id}`)}
                                                            style={{
                                                                padding: '8px 16px',
                                                                backgroundColor: '#5454e9',
                                                                color: 'white',
                                                                border: 'none',
                                                                borderRadius: '0',
                                                                cursor: 'pointer',
                                                                fontWeight: '600',
                                                                fontSize: '0.9rem',
                                                                transition: 'all 0.3s'
                                                            }}
                                                            onMouseOver={(e) => e.target.style.backgroundColor = '#5454e9'}
                                                            onMouseOut={(e) => e.target.style.backgroundColor = '#5454e9'}
                                                        >
                                                            <Users {...iconProps} style={{ marginRight: '6px', verticalAlign: 'text-bottom' }} />Ver Postulantes
                                                        </button>
                                                    )}
                                                    {conv.status === 'PENDIENTE_APROBACION_JEFE' && (
                                                        <span style={{ color: '#e4eb60', fontSize: '0.85rem', display: 'inline-flex', alignItems: 'center', gap: '6px' }}>
                                                            <Clock3 {...iconProps} />Esperando jefe
                                                        </span>
                                                    )}
                                                    {conv.status === 'APROBADA' && (
                                                        <span style={{ 
                                                            color: '#88898c', 
                                                            fontSize: '0.85rem', 
                                                            fontStyle: 'italic',
                                                            display: 'flex',
                                                            alignItems: 'center',
                                                            justifyContent: 'center',
                                                            gap: '5px'
                                                        }}>
                                                            Cerrada
                                                        </span>
                                                    )}
                                                    {conv.status === 'RECHAZADA' && (
                                                        <span style={{ color: '#e9683b', fontSize: '0.85rem', display: 'inline-flex', alignItems: 'center', gap: '6px' }}>
                                                            <X {...iconProps} />Rechazada
                                                        </span>
                                                    )}
                                                    {conv.status === 'MONITOR_SELECCIONADO' && (
                                                        <span style={{ color: '#5454e9', fontSize: '0.85rem', display: 'inline-flex', alignItems: 'center', gap: '6px' }}>
                                                            <UserRound {...iconProps} />Monitor elegido
                                                        </span>
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
                                            style={{ 
                                                margin: '0 5px', 
                                                padding: '8px 16px',
                                                backgroundColor: currentPage === 1 ? '#cecfd4' : '#5454e9',
                                                color: 'white',
                                                border: 'none',
                                                borderRadius: '0',
                                                cursor: currentPage === 1 ? 'not-allowed' : 'pointer',
                                                fontWeight: '600'
                                            }}
                                        >
                                            <ChevronLeft {...iconProps} style={{ marginRight: '4px', verticalAlign: 'text-bottom' }} />Anterior
                                        </button>
                                        <span style={{ margin: '0 15px', fontWeight: '600', color: '#000000' }}>
                                            Página {currentPage} de {totalPages}
                                        </span>
                                        <button 
                                            onClick={() => setCurrentPage(prev => Math.min(totalPages, prev + 1))}
                                            disabled={currentPage === totalPages}
                                            style={{ 
                                                margin: '0 5px', 
                                                padding: '8px 16px',
                                                backgroundColor: currentPage === totalPages ? '#cecfd4' : '#5454e9',
                                                color: 'white',
                                                border: 'none',
                                                borderRadius: '0',
                                                cursor: currentPage === totalPages ? 'not-allowed' : 'pointer',
                                                fontWeight: '600'
                                            }}
                                        >
                                            Siguiente<ChevronRight {...iconProps} style={{ marginLeft: '4px', verticalAlign: 'text-bottom' }} />
                                        </button>
                                    </div>
                                )}
                            </>
                        )}
                    </div>
                </div>
            </div>

            <PopUp show={isOpen} onClose={handleClose}>
                <div style={{whiteSpace: 'pre-wrap'}}>{message}</div>
            </PopUp>
        </div>
    );
}

export default MisConvocatorias;
