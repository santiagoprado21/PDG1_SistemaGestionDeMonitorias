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
    const [showConfirmComplete, setShowConfirmComplete] = useState(false);
    const [activityToComplete, setActivityToComplete] = useState(null);

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
     * Carga todas las actividades del monitor y las organiza por monitoría
     */
    const loadActivityPlans = async () => {
        setIsLoading(true);
        try {
            // Usar el mismo endpoint que Task.js (funciona correctamente)
            const response = await fetch(
                `${BACKEND_URL}/activity/findAll/${userId}/${role}`,
                {
                    method: 'GET',
                    headers: {
                        'Content-Type': 'application/json',
                        'Authorization': localStorage.getItem('token')
                    }
                }
            );

            if (!response.ok) {
                throw new Error('Error al cargar actividades');
            }

            const activities = await response.json();
            console.log('Actividades recibidas:', activities);

            // Organizar actividades por monitoría
            const plans = organizePlansByMonitoring(activities);
            console.log('Planes organizados:', plans);
            
            setActivityPlans(plans);
            setFilteredPlans(plans);
        } catch (error) {
            console.error('Error al cargar actividades:', error);
            setMessage('Error al cargar tus actividades: ' + error.message);
            setIsOpen(true);
            setActivityPlans([]);
            setFilteredPlans([]);
        } finally {
            setIsLoading(false);
        }
    };

    /**
     * Organiza las actividades en planes agrupados por monitoría
     */
    const organizePlansByMonitoring = (activities) => {
        if (!activities || activities.length === 0) {
            return [];
        }

        // Agrupar actividades por monitoring.id
        const monitoringMap = new Map();

        activities.forEach(activity => {
            const monitoring = activity.monitoring;
            if (!monitoring) return;

            const monitoringId = monitoring.id;

            if (!monitoringMap.has(monitoringId)) {
                // Crear nuevo plan para esta monitoría
                monitoringMap.set(monitoringId, {
                    monitoringId: monitoringId,
                    courseName: monitoring.course?.name || 'N/A',
                    programName: monitoring.program?.name || 'N/A',
                    professorName: monitoring.professor?.name || 'N/A',
                    monitorName: activity.monitor?.name + ' ' + (activity.monitor?.lastName || '') || 'N/A',
                    semester: monitoring.semester || activity.semester || 'N/A',
                    activities: []
                });
            }

            // Agregar actividad al plan
            const plan = monitoringMap.get(monitoringId);
            plan.activities.push({
                id: activity.id,
                name: activity.name,
                description: activity.description,
                category: activity.category,
                finish: activity.finish,
                state: activity.state,
                priority: activity.priority || 'MEDIA',
                startTime: activity.startTime,
                endTime: activity.endTime,
                durationHours: activity.durationHours,
                recurrence: activity.recurrence,
                rubricId: activity.rubricId,
                rubricName: activity.rubricName,
                rubricTotalPoints: activity.rubricTotalPoints,
                monitorId: activity.monitor?.code
            });
        });

        // Convertir Map a Array y calcular estadísticas
        const plans = Array.from(monitoringMap.values()).map(plan => {
            const totalActivities = plan.activities.length;
            const completedActivities = plan.activities.filter(a => 
                a.state === 'COMPLETADO' || a.state === 'COMPLETADOT'
            ).length;
            const pendingActivities = plan.activities.filter(a => 
                a.state === 'PENDIENTE'
            ).length;

            return {
                ...plan,
                totalActivities,
                completedActivities,
                pendingActivities
            };
        });

        return plans;
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
     * Abre el modal de confirmación para completar actividad
     */
    const requestCompleteActivity = (activity) => {
        setActivityToComplete(activity);
        setShowConfirmComplete(true);
    };

    /**
     * Marca una actividad como completada
     */
    const confirmCompleteActivity = async () => {
        if (!activityToComplete) return;

        setShowConfirmComplete(false);
        
        try {
            const activityId = activityToComplete.id;
            const deadlineDate = activityToComplete.finish;

            // Determinar si se completó tarde
            const now = new Date();
            const deadline = new Date(deadlineDate);
            const isLate = now > deadline;

            // Actualizar en el backend
            const response = await fetch(`${BACKEND_URL}/activity/updateState`, {
                method: 'PUT',
                headers: {
                    'Content-Type': 'application/json',
                    'Authorization': localStorage.getItem('token')
                },
                body: JSON.stringify(activityId),
            });

            if (!response.ok) {
                throw new Error('Error al actualizar el estado');
            }

            // Actualizar en el frontend
            const updatedPlans = activityPlans.map(plan => ({
                ...plan,
                activities: plan.activities.map(activity => {
                    if (activity.id === activityId) {
                        return {
                            ...activity,
                            state: isLate ? 'COMPLETADOT' : 'COMPLETADO',
                            delivey: now.toISOString()
                        };
                    }
                    return activity;
                })
            }));

            // Recalcular estadísticas
            const recalculatedPlans = updatedPlans.map(plan => {
                const completedActivities = plan.activities.filter(a => 
                    a.state === 'COMPLETADO' || a.state === 'COMPLETADOT'
                ).length;
                const pendingActivities = plan.activities.filter(a => 
                    a.state === 'PENDIENTE'
                ).length;

                return {
                    ...plan,
                    completedActivities,
                    pendingActivities
                };
            });

            setActivityPlans(recalculatedPlans);
            applyFilters();

            setMessage(isLate ? 
                '✅ Actividad marcada como completada (tardía)' : 
                '✅ Actividad marcada como completada'
            );
            setIsOpen(true);
            setShowActivityDetail(false);
            setActivityToComplete(null);

            // Recargar para asegurar sincronización
            setTimeout(() => loadActivityPlans(), 1000);

        } catch (error) {
            console.error('Error al marcar actividad:', error);
            setMessage('❌ Error al marcar la actividad como completada');
            setIsOpen(true);
            setActivityToComplete(null);
        }
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
                    <p><strong>Total de horas:</strong> {(plan.totalHours || 0).toFixed(1)} h</p>
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
                                    <div className="activity-actions">
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
                                        {activity.state === 'PENDIENTE' && (
                                            <button 
                                                className="complete-activity-btn"
                                                onClick={(e) => {
                                                    e.stopPropagation();
                                                    requestCompleteActivity(activity);
                                                }}
                                                title="Marcar como completada"
                                            >
                                                ✓ Completar
                                            </button>
                                        )}
                                    </div>
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

                                {selectedActivity.state === 'PENDIENTE' && (
                                    <div className="detail-section">
                                        <h3>Marcar como Completada</h3>
                                        <p>Al marcar esta actividad como completada, se registrará la fecha y hora actual.</p>
                                        {getDaysRemaining(selectedActivity.finish) < 0 && (
                                            <p className="warning-text">⚠️ Esta actividad está vencida. Se marcará como completada tardíamente.</p>
                                        )}
                                        <button 
                                            className="complete-btn-modal"
                                            onClick={() => requestCompleteActivity(selectedActivity)}
                                        >
                                            ✓ Marcar como Completada
                                        </button>
                                    </div>
                                )}
                            </div>
                        </div>
                    </div>
                )}
            </div>

            {isOpen && <PopUp message={message} onClose={() => setIsOpen(false)} />}

            {/* Modal de confirmación para completar actividad */}
            {showConfirmComplete && activityToComplete && (
                <div className="modal-overlay" onClick={() => setShowConfirmComplete(false)}>
                    <div className="confirm-modal" onClick={(e) => e.stopPropagation()}>
                        <div className="confirm-modal-header">
                            <h2>✓ Confirmar Completitud</h2>
                            <button 
                                className="close-btn"
                                onClick={() => setShowConfirmComplete(false)}
                            >
                                ✕
                            </button>
                        </div>
                        
                        <div className="confirm-modal-body">
                            <div className="activity-info-confirm">
                                <h3>{activityToComplete.name}</h3>
                                <p className="activity-course-confirm">
                                    📚 {activityToComplete.monitoring?.courseName || 'N/A'}
                                </p>
                            </div>

                            <div className="confirm-message">
                                <p>¿Estás seguro de marcar esta actividad como completada?</p>
                                <p className="confirm-detail">Se registrará la fecha y hora actual como fecha de entrega.</p>
                            </div>

                            {getDaysRemaining(activityToComplete.finish) < 0 && (
                                <div className="warning-box">
                                    <span className="warning-icon">⚠️</span>
                                    <div className="warning-content">
                                        <strong>Actividad vencida</strong>
                                        <p>Esta actividad se marcará como <strong>completada tardíamente</strong>.</p>
                                    </div>
                                </div>
                            )}

                            {getDaysRemaining(activityToComplete.finish) >= 0 && (
                                <div className="success-box">
                                    <span className="success-icon">✓</span>
                                    <div className="success-content">
                                        <strong>¡Bien hecho!</strong>
                                        <p>Estás completando esta actividad a tiempo.</p>
                                    </div>
                                </div>
                            )}
                        </div>

                        <div className="confirm-modal-footer">
                            <button 
                                className="btn-cancel"
                                onClick={() => setShowConfirmComplete(false)}
                            >
                                Cancelar
                            </button>
                            <button 
                                className="btn-confirm"
                                onClick={confirmCompleteActivity}
                            >
                                ✓ Marcar como Completada
                            </button>
                        </div>
                    </div>
                </div>
            )}
        </div>
    );
}

export default VistaMonitorActividades;

