import './VistaMonitorActividades.css';
import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import VerticalNavbar from './VerticalNavbar';
import { PopUp } from './PopUp';
import { BACKEND_URL } from './config/ApiBackend';

/**
 * HU-017: Vista del Monitor - Plan de Actividades
 * Componente para que el monitor vea sus actividades asignadas
 */
function VistaMonitorActividades() {
    const navigate = useNavigate();
    const role = localStorage.getItem('role');
    const userId = localStorage.getItem('userId');

    // Estado
    const [activityPlans, setActivityPlans] = useState([]);
    const [filteredPlans, setFilteredPlans] = useState([]);
    const [isLoading, setIsLoading] = useState(false);
    
    // Filtros
    const [searchTerm, setSearchTerm] = useState('');
    const [filterCourse, setFilterCourse] = useState('');
    const [filterProfessor, setFilterProfessor] = useState('');
    const [filterStatus, setFilterStatus] = useState('todas'); // todas, pendientes, completadas
    
    // Vista
    const [viewMode, setViewMode] = useState('cards'); // cards, calendar, list
    const [selectedMonitoring, setSelectedMonitoring] = useState(null);
    const [showActivityDetail, setShowActivityDetail] = useState(false);
    const [selectedActivity, setSelectedActivity] = useState(null);

    // PopUp
    const [isOpen, setIsOpen] = useState(false);
    const [message, setMessage] = useState('');

    useEffect(() => {
        if (role !== 'monitor') {
            navigate('/');
            return;
        }
        loadActivityPlans();
    }, [role, userId, navigate]);

    useEffect(() => {
        applyFilters();
    }, [searchTerm, filterCourse, filterProfessor, filterStatus, activityPlans]);

    /**
     * Carga todos los planes de actividades del monitor
     */
    const loadActivityPlans = async () => {
        setIsLoading(true);
        try {
            const response = await fetch(
                `${BACKEND_URL}/activity-schedule/monitor/${userId}/all-plans`,
                {
                    headers: {
                        'Authorization': localStorage.getItem('token')
                    }
                }
            );

            if (!response.ok) {
                throw new Error('Error al cargar planes de actividades');
            }

            const data = await response.json();
            console.log('Planes de actividades recibidos:', data);
            setActivityPlans(data || []);
            setFilteredPlans(data || []);
        } catch (error) {
            console.error('Error al cargar planes:', error);
            setMessage('Error al cargar tus actividades: ' + error.message);
            setIsOpen(true);
            setActivityPlans([]);
            setFilteredPlans([]);
        } finally {
            setIsLoading(false);
        }
    };

    /**
     * Aplica filtros de búsqueda
     */
    const applyFilters = () => {
        let filtered = [...activityPlans];

        // Filtro por término de búsqueda (curso, profesor, nombre de monitoria)
        if (searchTerm) {
            const term = searchTerm.toLowerCase();
            filtered = filtered.filter(plan => 
                plan.courseName?.toLowerCase().includes(term) ||
                plan.professorName?.toLowerCase().includes(term) ||
                plan.programName?.toLowerCase().includes(term)
            );
        }

        // Filtro por curso específico
        if (filterCourse) {
            filtered = filtered.filter(plan => 
                plan.courseName === filterCourse
            );
        }

        // Filtro por profesor específico
        if (filterProfessor) {
            filtered = filtered.filter(plan => 
                plan.professorName === filterProfessor
            );
        }

        // Filtro por estado
        if (filterStatus !== 'todas') {
            filtered = filtered.map(plan => ({
                ...plan,
                activities: plan.activities?.filter(activity => {
                    if (filterStatus === 'pendientes') {
                        return activity.state === 'PENDIENTE';
                    } else if (filterStatus === 'completadas') {
                        return activity.state === 'COMPLETADO' || activity.state === 'COMPLETADOT';
                    }
                    return true;
                })
            })).filter(plan => plan.activities && plan.activities.length > 0);
        }

        setFilteredPlans(filtered);
    };

    /**
     * Descarga la rúbrica asociada a una actividad
     */
    const downloadRubric = async (rubricId, activityName) => {
        if (!rubricId) {
            setMessage('Esta actividad no tiene rúbrica asociada');
            setIsOpen(true);
            return;
        }

        try {
            const response = await fetch(
                `${BACKEND_URL}/rubric/${rubricId}`,
                {
                    headers: {
                        'Authorization': localStorage.getItem('token')
                    }
                }
            );

            if (!response.ok) {
                throw new Error('Error al obtener la rúbrica');
            }

            const rubricData = await response.json();
            
            // Generar contenido para descarga
            const rubricContent = generateRubricContent(rubricData, activityName);
            
            // Crear blob y descargar
            const blob = new Blob([rubricContent], { type: 'text/plain;charset=utf-8' });
            const url = window.URL.createObjectURL(blob);
            const link = document.createElement('a');
            link.href = url;
            link.download = `Rubrica_${activityName.replace(/\s/g, '_')}.txt`;
            document.body.appendChild(link);
            link.click();
            document.body.removeChild(link);
            window.URL.revokeObjectURL(url);

            setMessage('Rúbrica descargada exitosamente');
            setIsOpen(true);
        } catch (error) {
            console.error('Error al descargar rúbrica:', error);
            setMessage('Error al descargar la rúbrica: ' + error.message);
            setIsOpen(true);
        }
    };

    /**
     * Genera el contenido de la rúbrica para descarga
     */
    const generateRubricContent = (rubric, activityName) => {
        let content = `RÚBRICA DE EVALUACIÓN\n`;
        content += `Actividad: ${activityName}\n`;
        content += `=`.repeat(60) + `\n\n`;
        content += `Nombre: ${rubric.name}\n`;
        content += `Descripción: ${rubric.description || 'N/A'}\n`;
        content += `Puntaje Total: ${rubric.totalPoints} puntos\n`;
        content += `Creado: ${new Date(rubric.createdAt).toLocaleDateString('es-ES')}\n\n`;
        content += `CRITERIOS DE EVALUACIÓN:\n`;
        content += `=`.repeat(60) + `\n\n`;

        if (rubric.criteria && Array.isArray(rubric.criteria)) {
            rubric.criteria.forEach((criterion, index) => {
                content += `${index + 1}. ${criterion.criterion || criterion.name}\n`;
                content += `   Puntaje: ${criterion.points} puntos\n`;
                content += `   Descripción: ${criterion.description || 'N/A'}\n\n`;
            });
        }

        content += `\n` + `=`.repeat(60) + `\n`;
        content += `Descargado: ${new Date().toLocaleString('es-ES')}\n`;

        return content;
    };

    /**
     * Muestra el detalle de una actividad
     */
    const showActivityDetails = (activity, plan) => {
        setSelectedActivity({ ...activity, monitoring: plan });
        setShowActivityDetail(true);
    };

    /**
     * Obtiene el color según el estado de la actividad
     */
    const getStatusColor = (state) => {
        switch (state) {
            case 'PENDIENTE':
                return 'status-pending';
            case 'COMPLETADO':
            case 'COMPLETADOT':
                return 'status-completed';
            default:
                return '';
        }
    };

    /**
     * Obtiene el nombre amigable del estado
     */
    const getStatusLabel = (state) => {
        switch (state) {
            case 'PENDIENTE':
                return 'Pendiente';
            case 'COMPLETADO':
                return 'Completado';
            case 'COMPLETADOT':
                return 'Completado Tardío';
            default:
                return state;
        }
    };

    /**
     * Obtiene el color según la prioridad
     */
    const getPriorityColor = (priority) => {
        switch (priority) {
            case 'ALTA':
                return 'priority-high';
            case 'MEDIA':
                return 'priority-medium';
            case 'BAJA':
                return 'priority-low';
            default:
                return '';
        }
    };

    /**
     * Formatea una fecha
     */
    const formatDate = (dateStr) => {
        if (!dateStr) return 'N/A';
        const date = new Date(dateStr);
        return date.toLocaleDateString('es-ES', {
            year: 'numeric',
            month: 'long',
            day: 'numeric'
        });
    };

    /**
     * Calcula días restantes para una fecha límite
     */
    const getDaysRemaining = (finishDate) => {
        if (!finishDate) return null;
        const today = new Date();
        const finish = new Date(finishDate);
        const diffTime = finish - today;
        const diffDays = Math.ceil(diffTime / (1000 * 60 * 60 * 24));
        return diffDays;
    };

    /**
     * Obtiene listas únicas para filtros
     */
    const getUniqueCourses = () => {
        return [...new Set(activityPlans.map(plan => plan.courseName))].filter(Boolean);
    };

    const getUniqueProfessors = () => {
        return [...new Set(activityPlans.map(plan => plan.professorName))].filter(Boolean);
    };

    /**
     * Renderiza las actividades en vista de tarjetas
     */
    const renderCardView = () => {
        if (filteredPlans.length === 0) {
            return (
                <div className="empty-state">
                    <p>📭 No tienes actividades asignadas</p>
                    <p className="empty-subtitle">Espera a que tu profesor asigne actividades</p>
                </div>
            );
        }

        return filteredPlans.map((plan, index) => (
            <div key={index} className="monitoring-card">
                <div className="monitoring-header">
                    <h3>{plan.courseName}</h3>
                    <span className="semester-badge">{plan.semester}</span>
                </div>
                <div className="monitoring-info">
                    <p><strong>Programa:</strong> {plan.programName}</p>
                    <p><strong>Profesor:</strong> {plan.professorName}</p>
                    <p><strong>Total de horas:</strong> {plan.totalHours || 0} h</p>
                </div>
                <div className="activities-summary">
                    <span className="badge badge-total">Total: {plan.totalActivities || 0}</span>
                    <span className="badge badge-pending">Pendientes: {plan.pendingActivities || 0}</span>
                    <span className="badge badge-completed">Completadas: {plan.completedActivities || 0}</span>
                </div>

                <div className="activities-list">
                    {plan.activities && plan.activities.length > 0 ? (
                        plan.activities.map((activity) => {
                            const daysRemaining = getDaysRemaining(activity.finish);
                            const isUrgent = daysRemaining !== null && daysRemaining <= 3 && activity.state === 'PENDIENTE';

                            return (
                                <div 
                                    key={activity.id} 
                                    className={`activity-item ${isUrgent ? 'urgent' : ''}`}
                                    onClick={() => showActivityDetails(activity, plan)}
                                >
                                    <div className="activity-main">
                                        <div className="activity-title-row">
                                            <h4>{activity.name}</h4>
                                            <span className={`activity-status ${getStatusColor(activity.state)}`}>
                                                {getStatusLabel(activity.state)}
                                            </span>
                                        </div>
                                        <p className="activity-description">{activity.description}</p>
                                        <div className="activity-meta">
                                            <span>📅 {formatDate(activity.finish)}</span>
                                            {activity.category && <span>📂 {activity.category}</span>}
                                            {activity.durationHours && <span>⏱️ {activity.durationHours} h</span>}
                                            {activity.priority && (
                                                <span className={`priority-badge ${getPriorityColor(activity.priority)}`}>
                                                    {activity.priority}
                                                </span>
                                            )}
                                        </div>
                                        {isUrgent && (
                                            <div className="urgent-banner">
                                                ⚠️ ¡URGENTE! Quedan {daysRemaining} día{daysRemaining !== 1 ? 's' : ''}
                                            </div>
                                        )}
                                    </div>
                                    {activity.rubricId && (
                                        <button 
                                            className="download-rubric-btn"
                                            onClick={(e) => {
                                                e.stopPropagation();
                                                downloadRubric(activity.rubricId, activity.name);
                                            }}
                                            title="Descargar rúbrica"
                                        >
                                            📥 Rúbrica
                                        </button>
                                    )}
                                </div>
                            );
                        })
                    ) : (
                        <p className="no-activities">No hay actividades en esta monitoría</p>
                    )}
                </div>
            </div>
        ));
    };

    return (
        <div className="vista-monitor-container">
            <VerticalNavbar />
            <div className="vista-monitor-content">
                <div className="page-header">
                    <h1>📋 Mis Actividades</h1>
                    <p className="subtitle">Revisa y gestiona las actividades asignadas por tus profesores</p>
                </div>

                {/* Barra de búsqueda y filtros */}
                <div className="filters-section">
                    <div className="search-bar">
                        <input
                            type="text"
                            placeholder="🔍 Buscar por curso, profesor o programa..."
                            value={searchTerm}
                            onChange={(e) => setSearchTerm(e.target.value)}
                            className="search-input"
                        />
                    </div>

                    <div className="filters-row">
                        <select
                            value={filterCourse}
                            onChange={(e) => setFilterCourse(e.target.value)}
                            className="filter-select"
                        >
                            <option value="">Todos los cursos</option>
                            {getUniqueCourses().map((course, idx) => (
                                <option key={idx} value={course}>{course}</option>
                            ))}
                        </select>

                        <select
                            value={filterProfessor}
                            onChange={(e) => setFilterProfessor(e.target.value)}
                            className="filter-select"
                        >
                            <option value="">Todos los profesores</option>
                            {getUniqueProfessors().map((prof, idx) => (
                                <option key={idx} value={prof}>{prof}</option>
                            ))}
                        </select>

                        <select
                            value={filterStatus}
                            onChange={(e) => setFilterStatus(e.target.value)}
                            className="filter-select"
                        >
                            <option value="todas">Todas las actividades</option>
                            <option value="pendientes">Solo pendientes</option>
                            <option value="completadas">Solo completadas</option>
                        </select>

                        <button 
                            onClick={() => {
                                setSearchTerm('');
                                setFilterCourse('');
                                setFilterProfessor('');
                                setFilterStatus('todas');
                            }}
                            className="clear-filters-btn"
                        >
                            🔄 Limpiar filtros
                        </button>
                    </div>
                </div>

                {/* Estadísticas generales */}
                {activityPlans.length > 0 && (
                    <div className="stats-section">
                        <div className="stat-card">
                            <h3>{activityPlans.length}</h3>
                            <p>Monitorías</p>
                        </div>
                        <div className="stat-card">
                            <h3>{activityPlans.reduce((sum, p) => sum + (p.totalActivities || 0), 0)}</h3>
                            <p>Total Actividades</p>
                        </div>
                        <div className="stat-card pending">
                            <h3>{activityPlans.reduce((sum, p) => sum + (p.pendingActivities || 0), 0)}</h3>
                            <p>Pendientes</p>
                        </div>
                        <div className="stat-card completed">
                            <h3>{activityPlans.reduce((sum, p) => sum + (p.completedActivities || 0), 0)}</h3>
                            <p>Completadas</p>
                        </div>
                        <div className="stat-card hours">
                            <h3>{activityPlans.reduce((sum, p) => sum + (p.totalHours || 0), 0).toFixed(1)} h</h3>
                            <p>Total Horas</p>
                        </div>
                    </div>
                )}

                {/* Contenido principal */}
                {isLoading ? (
                    <div className="loading-state">
                        <div className="spinner"></div>
                        <p>Cargando tus actividades...</p>
                    </div>
                ) : (
                    <div className="activities-container">
                        {renderCardView()}
                    </div>
                )}

                {/* Modal de detalle de actividad */}
                {showActivityDetail && selectedActivity && (
                    <div className="modal-overlay" onClick={() => setShowActivityDetail(false)}>
                        <div className="modal-content" onClick={(e) => e.stopPropagation()}>
                            <div className="modal-header">
                                <h2>{selectedActivity.name}</h2>
                                <button 
                                    className="close-btn"
                                    onClick={() => setShowActivityDetail(false)}
                                >
                                    ✕
                                </button>
                            </div>
                            <div className="modal-body">
                                <div className="detail-section">
                                    <h3>Información General</h3>
                                    <p><strong>Curso:</strong> {selectedActivity.monitoring?.courseName}</p>
                                    <p><strong>Profesor:</strong> {selectedActivity.monitoring?.professorName}</p>
                                    <p><strong>Categoría:</strong> {selectedActivity.category || 'N/A'}</p>
                                    <p><strong>Estado:</strong> <span className={getStatusColor(selectedActivity.state)}>
                                        {getStatusLabel(selectedActivity.state)}
                                    </span></p>
                                </div>

                                <div className="detail-section">
                                    <h3>Descripción</h3>
                                    <p>{selectedActivity.description}</p>
                                </div>

                                <div className="detail-section">
                                    <h3>Fechas y Horarios</h3>
                                    <p><strong>Fecha límite:</strong> {formatDate(selectedActivity.finish)}</p>
                                    {selectedActivity.startTime && (
                                        <p><strong>Hora inicio:</strong> {selectedActivity.startTime}</p>
                                    )}
                                    {selectedActivity.endTime && (
                                        <p><strong>Hora fin:</strong> {selectedActivity.endTime}</p>
                                    )}
                                    {selectedActivity.durationHours && (
                                        <p><strong>Duración:</strong> {selectedActivity.durationHours} horas</p>
                                    )}
                                    {selectedActivity.recurrence && selectedActivity.recurrence !== 'NONE' && (
                                        <p><strong>Recurrencia:</strong> {selectedActivity.recurrence}</p>
                                    )}
                                    {selectedActivity.priority && (
                                        <p><strong>Prioridad:</strong> <span className={getPriorityColor(selectedActivity.priority)}>
                                            {selectedActivity.priority}
                                        </span></p>
                                    )}
                                </div>

                                {selectedActivity.rubricId && (
                                    <div className="detail-section">
                                        <h3>Rúbrica de Evaluación</h3>
                                        <p><strong>Nombre:</strong> {selectedActivity.rubricName || 'N/A'}</p>
                                        <p><strong>Puntaje total:</strong> {selectedActivity.rubricTotalPoints || 0} puntos</p>
                                        <button 
                                            className="download-btn-modal"
                                            onClick={() => downloadRubric(selectedActivity.rubricId, selectedActivity.name)}
                                        >
                                            📥 Descargar Rúbrica
                                        </button>
                                    </div>
                                )}
                            </div>
                        </div>
                    </div>
                )}
            </div>

            {isOpen && <PopUp message={message} onClose={() => setIsOpen(false)} />}
        </div>
    );
}

export default VistaMonitorActividades;

