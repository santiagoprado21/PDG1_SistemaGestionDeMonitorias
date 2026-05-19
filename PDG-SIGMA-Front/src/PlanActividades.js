import './PlanActividades.css';
import React, { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import VerticalNavbar from './VerticalNavbar';
import { PopUp } from './PopUp';
import { BACKEND_URL } from './config/ApiBackend';
import { AlertTriangle, Plus, BarChart3, ArrowLeft, Pencil, Trash2 } from 'lucide-react';

/**
 * HU-011: Creación de plan de actividades para monitores (Como profesor)
 * Componente para gestionar el plan completo de actividades de una monitoría
 */
function PlanActividades() {
    const iconProps = {
        size: 16,
        strokeWidth: 2,
        strokeLinecap: 'butt',
        strokeLinejoin: 'miter'
    };

    const { monitoringId: urlMonitoringId } = useParams();
    const navigate = useNavigate();
    const role = localStorage.getItem('role');
    const user = localStorage.getItem('userId');

    // Estado del plan de actividades
    const [monitorings, setMonitorings] = useState([]);
    const [selectedMonitoringId, setSelectedMonitoringId] = useState(urlMonitoringId || '');
    const [activityPlan, setActivityPlan] = useState(null);
    const [activities, setActivities] = useState([]);
    const [rubrics, setRubrics] = useState([]);
    const [isLoadingMonitorings, setIsLoadingMonitorings] = useState(true);
    const [isLoading, setIsLoading] = useState(false);

    // Estado del modal de creación/edición
    const [showModal, setShowModal] = useState(false);
    const [editingActivity, setEditingActivity] = useState(null);

    // Campos del formulario
    const [formData, setFormData] = useState({
        name: '',
        description: '',
        category: '',
        finish: '',
        startTime: '',
        endTime: '',
        durationHours: '',
        priority: 'MEDIA',
        recurrence: 'NONE',
        rubricId: null,
        monitoringId: ''
    });

    // Estado de conflictos
    const [conflicts, setConflicts] = useState([]);

    // PopUp
    const [isOpen, setIsOpen] = useState(false);
    const [message, setMessage] = useState('');

    const categorias = [
        'Asistencia a clases',
        'Tutoría',
        'Calificación de trabajos',
        'Preparación de material',
        'Revisión de exámenes',
        'Apoyo en laboratorio',
        'Investigación',
        'Otra'
    ];

    const priorities = [
        { value: 'ALTA', label: 'Alta' },
        { value: 'MEDIA', label: 'Media' },
        { value: 'BAJA', label: 'Baja' }
    ];

    const recurrences = [
        { value: 'NONE', label: 'No recurrente' },
        { value: 'WEEKLY', label: 'Semanal' }
    ];

    useEffect(() => {
        loadMonitorings();
        loadRubrics();
    }, []);

    useEffect(() => {
        if (selectedMonitoringId) {
            loadActivityPlan();
        }
    }, [selectedMonitoringId]);

    const loadMonitorings = async () => {
        setIsLoadingMonitorings(true);
        try {
            const response = await fetch(`${BACKEND_URL}/monitoring/getAllByProfessor/${user}`, {
                headers: {
                    'Authorization': localStorage.getItem('token')
                }
            });
            if (!response.ok) throw new Error('Error al cargar monitorías');
            
            const data = await response.json();
            console.log('Monitorías recibidas del backend:', data);
            
            // Procesar monitorías para extraer información del monitor
            // Soporta AMBOS flujos: antiguo (monitoringMonitors) y nuevo HU-010 (assignedMonitor)
            const processedMonitorings = (data || []).map(m => {
                let monitor = null;
                let monitorCode = null;
                
                // NUEVO FLUJO (HU-010): Monitor asignado directamente
                if (m.assignedMonitor) {
                    monitor = `${m.assignedMonitor.name || ''} ${m.assignedMonitor.lastName || ''}`.trim();
                    monitorCode = m.assignedMonitor.code || m.assignedMonitor.idMonitor;
                    console.log(`Monitoría ${m.id} - Nuevo flujo: ${monitor}`);
                }
                // FLUJO ANTIGUO: Buscar en relación monitoringMonitors
                else if (m.monitoringMonitors && m.monitoringMonitors.length > 0) {
                    const selectedMonitor = m.monitoringMonitors.find(
                        mm => mm.estadoSeleccion === 'seleccionado' || mm.estadoSeleccion === 'aprobado'
                    );
                    if (selectedMonitor) {
                        monitor = `${selectedMonitor.monitor?.name || ''} ${selectedMonitor.monitor?.lastName || ''}`.trim();
                        monitorCode = selectedMonitor.monitor?.code || selectedMonitor.monitor?.idMonitor;
                        console.log(`Monitoría ${m.id} - Flujo antiguo: ${monitor}`);
                    }
                }
                
                return {
                    id: m.id,
                    courseName: m.course?.name || 'Sin nombre',
                    programName: m.program?.name || '',
                    semester: m.semester,
                    monitor: monitor,
                    monitorCode: monitorCode,
                    approvalStatus: m.approvalStatus || null,
                    isNewFlow: m.assignedMonitor !== null
                };
            });
            
            // Filtrar solo las que tienen monitor asignado
            const monitoringsWithMonitor = processedMonitorings.filter(m => m.monitor);
            console.log('Monitorías con monitor:', monitoringsWithMonitor);
            console.log('Total nuevo flujo:', monitoringsWithMonitor.filter(m => m.isNewFlow).length);
            console.log('Total flujo antiguo:', monitoringsWithMonitor.filter(m => !m.isNewFlow).length);
            
            setMonitorings(monitoringsWithMonitor);
            
            // Si viene de URL, usar ese ID, sino usar el primero disponible
            if (urlMonitoringId) {
                setSelectedMonitoringId(urlMonitoringId);
            } else if (monitoringsWithMonitor.length > 0) {
                setSelectedMonitoringId(monitoringsWithMonitor[0].id.toString());
            }
        } catch (error) {
            console.error('Error al cargar monitorías:', error);
            setMessage('Error al cargar monitorías: ' + error.message);
            setIsOpen(true);
        } finally {
            setIsLoadingMonitorings(false);
        }
    };

    const loadActivityPlan = async () => {
        if (!selectedMonitoringId) {
            console.log('No hay monitoringId seleccionado, no se puede cargar el plan');
            return;
        }
        
        console.log('Cargando plan para monitoringId:', selectedMonitoringId);
        setIsLoading(true);
        try {
            const response = await fetch(`${BACKEND_URL}/api/activity-schedule/plan/${selectedMonitoringId}`, {
                headers: {
                    'Authorization': localStorage.getItem('token')
                }
            });
            if (!response.ok) throw new Error('Error al cargar el plan de actividades');
            
            const data = await response.json();
            console.log('Plan de actividades cargado:', data);
            console.log('Número de actividades:', data.activities?.length || 0);
            setActivityPlan(data);
            setActivities(data.activities || []);
        } catch (error) {
            console.error('Error:', error);
            setMessage('Error al cargar el plan de actividades: ' + error.message);
            setIsOpen(true);
        } finally {
            setIsLoading(false);
        }
    };

    const loadRubrics = async () => {
        try {
            const response = await fetch(`${BACKEND_URL}/api/rubric/professor/${user}`, {
                headers: {
                    'Authorization': localStorage.getItem('token')
                }
            });
            if (!response.ok) throw new Error('Error al cargar rúbricas');
            
            const data = await response.json();
            setRubrics(data || []);
        } catch (error) {
            console.error('Error al cargar rúbricas:', error);
        }
    };

    const handleOpenModal = (activity = null) => {
        if (activity) {
            // Editar actividad existente
            setEditingActivity(activity);
            setFormData({
                name: activity.name || '',
                description: activity.description || '',
                category: activity.category || '',
                finish: activity.finish ? activity.finish.split('T')[0] : '',
                startTime: activity.startTime || '',
                endTime: activity.endTime || '',
                durationHours: activity.durationHours || '',
                priority: activity.priority || 'MEDIA',
                recurrence: activity.recurrence || 'NONE',
                rubricId: activity.rubricId || null,
                monitoringId: activity.monitoringId || selectedMonitoringId,
                state: activity.state || 'PENDIENTE',
                progressPercentage: activity.progressPercentage ?? 0
            });
        } else {
            // Nueva actividad - preseleccionar la monitoría actual
            setEditingActivity(null);
            setFormData({
                name: '',
                description: '',
                category: '',
                finish: '',
                startTime: '',
                endTime: '',
                durationHours: '',
                priority: 'MEDIA',
                recurrence: 'NONE',
                rubricId: null,
                monitoringId: selectedMonitoringId,
                state: 'PENDIENTE'
            });
        }
        setConflicts([]);
        setShowModal(true);
    };

    const handleCloseModal = () => {
        setShowModal(false);
        setEditingActivity(null);
        setConflicts([]);
    };

    const handleInputChange = (e) => {
        const { name, value } = e.target;
        setFormData(prev => ({
            ...prev,
            [name]: value
        }));

        // Calcular duración automáticamente si se cambian las horas
        if (name === 'startTime' || name === 'endTime') {
            const start = name === 'startTime' ? value : formData.startTime;
            const end = name === 'endTime' ? value : formData.endTime;
            if (start && end) {
                const duration = calculateDuration(start, end);
                setFormData(prev => ({
                    ...prev,
                    durationHours: duration
                }));
            }
        }
    };

    const calculateDuration = (startTime, endTime) => {
        if (!startTime || !endTime) return '';
        const [startHour, startMin] = startTime.split(':').map(Number);
        const [endHour, endMin] = endTime.split(':').map(Number);
        const startMinutes = startHour * 60 + startMin;
        const endMinutes = endHour * 60 + endMin;
        const diffMinutes = endMinutes - startMinutes;
        return (diffMinutes / 60).toFixed(2);
    };

    const validateConflicts = async () => {
        if (!formData.startTime || !formData.endTime || !formData.finish || !formData.monitoringId) {
            return; // No validar si faltan campos
        }

        const selectedMonitoring = monitorings.find(m => m.id.toString() === formData.monitoringId.toString());
        
        const activityDTO = {
            id: editingActivity?.id,
            name: formData.name,
            finish: formData.finish,
            startTime: formData.startTime,
            endTime: formData.endTime,
            monitoringId: parseInt(formData.monitoringId),
            monitorId: selectedMonitoring?.monitorCode || null,
            state: 'PENDIENTE',
            roleCreator: 'profesor',
            roleResponsable: 'monitor',
            description: formData.description || 'Sin descripción'
        };

        try {
            const response = await fetch(`${BACKEND_URL}/api/activity-schedule/validate-conflicts`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                    'Authorization': localStorage.getItem('token')
                },
                body: JSON.stringify(activityDTO)
            });

            if (!response.ok) throw new Error('Error al validar conflictos');

            const data = await response.json();
            setConflicts(data || []);
        } catch (error) {
            console.error('Error al validar conflictos:', error);
        }
    };

    useEffect(() => {
        validateConflicts();
    }, [formData.startTime, formData.endTime, formData.finish, formData.monitoringId]);

    const handleSubmit = async (e) => {
        e.preventDefault();

        if (conflicts.length > 0) {
            setMessage('No se puede guardar: hay conflictos de horarios pendientes');
            setIsOpen(true);
            return;
        }

        if (!formData.monitoringId) {
            setMessage('Debe seleccionar una monitoría');
            setIsOpen(true);
            return;
        }

        // HU-01: Validar consistencia estado-progreso en el frontend
        const progress = formData.progressPercentage ?? 0;
        if ((formData.state === 'COMPLETADO' || formData.state === 'COMPLETADOT') && progress < 100) {
            setMessage(`El estado "Completado" solo se puede asignar cuando el progreso es 100%. Progreso actual: ${progress}%.`);
            setIsOpen(true);
            return;
        }

        const selectedMonitoring = monitorings.find(m => m.id.toString() === formData.monitoringId.toString());
        
        console.log('Monitoría seleccionada:', selectedMonitoring);

        const activityDTO = {
            id: editingActivity?.id,
            name: formData.name,
            description: formData.description,
            category: formData.category,
            finish: formData.finish,
            startTime: formData.startTime || null,
            endTime: formData.endTime || null,
            durationHours: formData.durationHours ? parseFloat(formData.durationHours) : null,
            priority: formData.priority,
            recurrence: formData.recurrence,
            rubricId: formData.rubricId ? parseInt(formData.rubricId) : null,
            monitoringId: parseInt(formData.monitoringId),
            professorId: user,
            monitorId: selectedMonitoring?.monitorCode || null,
            state: formData.state || 'PENDIENTE',
            roleCreator: 'P',
            roleResponsable: 'M',
            semester: selectedMonitoring?.semester || '2025-1',
            creation: new Date().toISOString()
        };
        
        console.log('ActivityDTO a enviar:', activityDTO);

        try {
            const response = await fetch(`${BACKEND_URL}/api/activity-schedule/create`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                    'Authorization': localStorage.getItem('token')
                },
                body: JSON.stringify(activityDTO)
            });

            if (!response.ok) {
                const errorText = await response.text();
                throw new Error(errorText);
            }

            const data = await response.json();
            console.log('Actividad guardada:', data);

            setMessage(editingActivity ? 'Actividad actualizada exitosamente' : 'Actividad creada exitosamente');
            setIsOpen(true);
            handleCloseModal();
            
            // Recargar el plan - siempre recargar para ver los cambios
            console.log('Recargando plan de actividades...');
            await loadActivityPlan();
        } catch (error) {
            console.error('Error:', error);
            setMessage('Error al guardar la actividad: ' + error.message);
            setIsOpen(true);
        }
    };

    const handleDeleteActivity = async (activityId) => {
        if (!window.confirm('¿Está seguro de eliminar esta actividad?')) {
            return;
        }

        try {
            const response = await fetch(`${BACKEND_URL}/activity/${activityId}`, {
                method: 'DELETE',
                headers: {
                    'Authorization': localStorage.getItem('token')
                }
            });

            if (!response.ok) throw new Error('Error al eliminar la actividad');

            setMessage('Actividad eliminada exitosamente');
            setIsOpen(true);
            loadActivityPlan();
        } catch (error) {
            console.error('Error:', error);
            setMessage('Error al eliminar la actividad: ' + error.message);
            setIsOpen(true);
        }
    };

    if (isLoading) {
        return (
            <div className="monitoring-container">
                <VerticalNavbar />
                <div className="main-content">
                    <div className="loading">Cargando plan de actividades...</div>
                </div>
            </div>
        );
    }

    return (
        <div className="monitoring-container">
            <VerticalNavbar />
            <div className="main-content plan-actividades-content">
                <div className="title-container-plan-actividades prof-page-header">
                    <h1 className="prof-page-title">Plan de Actividades</h1>
                    <p className="subtitle-plan-actividades prof-page-subtitle">Gestiona y da seguimiento al plan de actividades de tus monitorías</p>
                </div>

                {/* Selector de Monitoría */}
                <div className="monitoring-selector">
                    <label htmlFor="monitoring-select">Seleccionar Monitoría:</label>
                    <select 
                        id="monitoring-select"
                        value={selectedMonitoringId}
                        onChange={(e) => setSelectedMonitoringId(e.target.value)}
                        disabled={isLoadingMonitorings || monitorings.length === 0}
                    >
                        {isLoadingMonitorings ? (
                            <option value="">Cargando monitorías...</option>
                        ) : monitorings.length === 0 ? (
                            <option value="">No hay monitorías con monitor asignado</option>
                        ) : (
                            monitorings.map(monitoring => (
                                <option key={monitoring.id} value={monitoring.id}>
                                    {monitoring.courseName} - {monitoring.semester} ({monitoring.monitor})
                                </option>
                            ))
                        )}
                    </select>
                </div>

                {/* Mensaje informativo cuando no hay monitorías */}
                {!isLoadingMonitorings && monitorings.length === 0 && (
                    <div className="warning-banner">
                        <span className="warning-icon"><AlertTriangle {...iconProps} /></span>
                        <div className="warning-text">
                            <strong>No tienes monitorías con monitor asignado</strong>
                            <p>Para crear un plan de actividades necesitas:</p>
                            <ol>
                                <li>Crear una convocatoria de monitoría desde "Crear Convocatoria"</li>
                                <li>Esperar a que estudiantes se postulen</li>
                                <li>Seleccionar un monitor para la convocatoria</li>
                            </ol>
                            <p>O si eres jefe de departamento, puedes crear monitorías directamente desde "Crear Monitorías CSV"</p>
                        </div>
                    </div>
                )}

                {activityPlan && (
                    <div className="plan-header">
                        <div className="plan-info">
                            <h2>{activityPlan.courseName} - {activityPlan.programName}</h2>
                            <p><strong>Profesor:</strong> {activityPlan.professorName}</p>
                            <p><strong>Monitor:</strong> {activityPlan.monitorName}</p>
                            <p><strong>Periodo:</strong> {activityPlan.semester}</p>
                        </div>
                        <div className="plan-stats">
                            <div className="stat-card">
                                <span className="stat-value">{activityPlan.totalActivities ?? activities.length ?? 0}</span>
                                <span className="stat-label">Total</span>
                            </div>
                            <div className="stat-card completed">
                                <span className="stat-value">{activityPlan.completedActivities}</span>
                                <span className="stat-label">Completadas</span>
                            </div>
                            <div className="stat-card pending">
                                <span className="stat-value">{activityPlan.pendingActivities}</span>
                                <span className="stat-label">Pendientes</span>
                            </div>
                            <div className="stat-card hours">
                                <span className="stat-value">{activityPlan.totalHours?.toFixed(1) || 0}</span>
                                <span className="stat-label">Horas</span>
                            </div>
                        </div>
                    </div>
                )}

                <div className="plan-actions">
                    <button 
                        className="btn-primary plan-add-activity-btn" 
                        onClick={() => handleOpenModal()}
                        disabled={!selectedMonitoringId || monitorings.length === 0}
                    >
                        <Plus {...iconProps} style={{ marginRight: '6px', verticalAlign: 'text-bottom' }} />Agregar Actividad
                    </button>
                    <button 
                        className="btn-secondary" 
                        onClick={() => navigate('/gestion-rubricas')}
                    >
                        <BarChart3 {...iconProps} style={{ marginRight: '6px', verticalAlign: 'text-bottom' }} />Gestionar Rúbricas
                    </button>
                    <button className="btn-secondary" onClick={() => navigate(-1)}>
                        <ArrowLeft {...iconProps} style={{ marginRight: '6px', verticalAlign: 'text-bottom' }} />Volver
                    </button>
                </div>

                <div className="activities-list">
                    {isLoadingMonitorings ? (
                        <div className="no-activities">
                            <p>Cargando monitorías y plan de actividades...</p>
                        </div>
                    ) : !selectedMonitoringId ? (
                        <div className="no-activities">
                            <p>Selecciona una monitoría para ver su plan de actividades.</p>
                        </div>
                    ) : activities.length === 0 ? (
                        <div className="no-activities">
                            <p>No hay actividades en el plan.</p>
                            <p>Haz clic en "Crear Nueva Actividad" para comenzar.</p>
                        </div>
                    ) : (
                        <table className="activities-table">
                            <thead>
                                <tr>
                                    <th>Nombre</th>
                                    <th>Categoría</th>
                                    <th>Fecha</th>
                                    <th>Horario</th>
                                    <th>Duración</th>
                                    <th>Prioridad</th>
                                    <th>Rúbrica</th>
                                    <th>Estado</th>
                                    <th>Acciones</th>
                                </tr>
                            </thead>
                            <tbody>
                                {activities.map(activity => (
                                    <tr key={activity.id}>
                                        <td>{activity.name}</td>
                                        <td>{activity.category}</td>
                                        <td>{activity.finish ? new Date(activity.finish).toLocaleDateString() : 'N/A'}</td>
                                        <td>
                                            {activity.startTime && activity.endTime ? 
                                                `${activity.startTime} - ${activity.endTime}` : 
                                                'Sin horario'}
                                        </td>
                                        <td>{activity.durationHours ? `${activity.durationHours}h` : '-'}</td>
                                        <td>
                                            <span className={`priority-badge priority-${activity.priority?.toLowerCase()}`}>
                                                {activity.priority || 'MEDIA'}
                                            </span>
                                        </td>
                                        <td className="rubric-cell">
                                            <span className="rubric-text">{activity.rubricName?.trim() || 'Sin rúbrica'}</span>
                                        </td>
                                        <td>
                                            <span className={`state-badge state-${activity.state?.toLowerCase()}`}>
                                                {activity.state === 'PENDIENTE' ? 'Pendiente' : 
                                                 activity.state === 'COMPLETADO' ? 'Completado' :
                                                 activity.state === 'COMPLETADOT' ? 'Completado (Tardío)' :
                                                 activity.state}
                                            </span>
                                        </td>
                                        <td>
                                            <button 
                                                className="activity-action-btn btn-edit"
                                                onClick={() => handleOpenModal(activity)}
                                                title="Editar actividad">
                                                <Pencil {...iconProps} size={18} />
                                            </button>
                                            <button 
                                                className="activity-action-btn btn-delete"
                                                onClick={() => handleDeleteActivity(activity.id)}
                                                title="Eliminar actividad">
                                                <Trash2 {...iconProps} size={18} />
                                            </button>
                                        </td>
                                    </tr>
                                ))}
                            </tbody>
                        </table>
                    )}
                </div>

                {/* Modal de creación/edición */}
                {showModal && (
                    <div className="modal-overlay" onClick={handleCloseModal}>
                        <div className="modal-content" onClick={(e) => e.stopPropagation()}>
                            <div className="modal-header">
                                <h2>{editingActivity ? 'Editar Actividad' : 'Nueva Actividad'}</h2>
                                <button className="btn-close btn-secondary" onClick={handleCloseModal}>×</button>
                            </div>

                            {conflicts.length > 0 && (
                                <div className="conflicts-alert">
                                    <strong><AlertTriangle {...iconProps} size={14} style={{ marginRight: '6px', verticalAlign: 'text-bottom' }} />Conflictos de horarios detectados:</strong>
                                    <ul>
                                        {conflicts.map((conflict, idx) => (
                                            <li key={idx}>
                                                {conflict.activityName} ({conflict.startTime} - {conflict.endTime})
                                            </li>
                                        ))}
                                    </ul>
                                </div>
                            )}

                            <form onSubmit={handleSubmit}>
                                <div className="form-group">
                                    <label>Asignar a Monitoría *</label>
                                    <select
                                        name="monitoringId"
                                        value={formData.monitoringId}
                                        onChange={handleInputChange}
                                        required>
                                        <option value="">Seleccione una monitoría...</option>
                                        {monitorings.map(monitoring => (
                                            <option key={monitoring.id} value={monitoring.id}>
                                                {monitoring.courseName} - {monitoring.semester} ({monitoring.monitor})
                                            </option>
                                        ))}
                                    </select>
                                </div>

                                <div className="form-row">
                                    <div className="form-group">
                                        <label>Nombre *</label>
                                        <input
                                            type="text"
                                            name="name"
                                            value={formData.name}
                                            onChange={handleInputChange}
                                            required
                                        />
                                    </div>
                                    <div className="form-group">
                                        <label>Categoría *</label>
                                        <select
                                            name="category"
                                            value={formData.category}
                                            onChange={handleInputChange}
                                            required>
                                            <option value="">Seleccione...</option>
                                            {categorias.map(cat => (
                                                <option key={cat} value={cat}>{cat}</option>
                                            ))}
                                        </select>
                                    </div>
                                </div>

                                <div className="form-group">
                                    <label>Descripción *</label>
                                    <textarea
                                        name="description"
                                        value={formData.description}
                                        onChange={handleInputChange}
                                        rows="3"
                                        required
                                    />
                                </div>

                                {/* Estado (solo visible al editar) */}
                                {editingActivity && (
                                    <div className="form-group">
                                        <label>Progreso actual</label>
                                        <div style={{
                                            display: 'flex',
                                            alignItems: 'center',
                                            gap: '10px',
                                            marginBottom: '4px'
                                        }}>
                                            <div style={{
                                                flex: 1,
                                                height: '12px',
                                                background: '#e0e0e0',
                                                borderRadius: '6px',
                                                overflow: 'hidden'
                                            }}>
                                                <div style={{
                                                    width: `${formData.progressPercentage ?? 0}%`,
                                                    height: '100%',
                                                    background: (formData.progressPercentage ?? 0) >= 100 ? '#4cb979' : '#5454e9',
                                                    borderRadius: '6px',
                                                    transition: 'width 0.3s'
                                                }} />
                                            </div>
                                            <span style={{ fontWeight: '700', minWidth: '40px', color: (formData.progressPercentage ?? 0) >= 100 ? '#4cb979' : '#5454e9' }}>
                                                {formData.progressPercentage ?? 0}%
                                            </span>
                                        </div>

                                        <label style={{ marginTop: '10px' }}>Estado</label>
                                        <select
                                            name="state"
                                            value={formData.state}
                                            onChange={handleInputChange}
                                            style={{
                                                fontWeight: 'bold',
                                                padding: '10px',
                                                fontSize: '15px',
                                                borderRadius: '4px',
                                                border: '2px solid #cecfd4',
                                                color: formData.state === 'COMPLETADO' || formData.state === 'COMPLETADOT' ? '#4cb979' : '#e4eb60'
                                            }}
                                        >
                                            <option value="PENDIENTE">PENDIENTE</option>
                                            <option
                                                value="COMPLETADO"
                                                disabled={(formData.progressPercentage ?? 0) < 100}
                                            >
                                                COMPLETADO{(formData.progressPercentage ?? 0) < 100 ? ` (requiere 100% de progreso)` : ''}
                                            </option>
                                            <option
                                                value="COMPLETADOT"
                                                disabled={(formData.progressPercentage ?? 0) < 100}
                                            >
                                                COMPLETADO TARDE{(formData.progressPercentage ?? 0) < 100 ? ` (requiere 100% de progreso)` : ''}
                                            </option>
                                        </select>
                                        <small style={{ display: 'block', marginTop: '8px', color: '#88898c', fontSize: '13px' }}>
                                            {formData.state === 'PENDIENTE' && 'La actividad está pendiente de completar'}
                                            {formData.state === 'COMPLETADO' && 'La actividad fue completada a tiempo'}
                                            {formData.state === 'COMPLETADOT' && 'La actividad fue completada con retraso'}
                                            {(formData.progressPercentage ?? 0) < 100 && formData.state === 'PENDIENTE' &&
                                                <span style={{ display: 'block', color: '#e9683b' }}>
                                                    El estado Completado solo está disponible cuando el progreso sea 100%
                                                </span>
                                            }
                                        </small>
                                    </div>
                                )}

                                <div className="form-row">
                                    <div className="form-group">
                                        <label>Fecha *</label>
                                        <input
                                            type="date"
                                            name="finish"
                                            value={formData.finish}
                                            onChange={handleInputChange}
                                            required
                                        />
                                    </div>
                                    <div className="form-group">
                                        <label>Hora Inicio</label>
                                        <input
                                            type="time"
                                            name="startTime"
                                            value={formData.startTime}
                                            onChange={handleInputChange}
                                        />
                                    </div>
                                    <div className="form-group">
                                        <label>Hora Fin</label>
                                        <input
                                            type="time"
                                            name="endTime"
                                            value={formData.endTime}
                                            onChange={handleInputChange}
                                        />
                                    </div>
                                    <div className="form-group">
                                        <label>Duración (horas)</label>
                                        <input
                                            type="number"
                                            name="durationHours"
                                            value={formData.durationHours}
                                            onChange={handleInputChange}
                                            step="0.5"
                                            min="0"
                                            readOnly
                                        />
                                    </div>
                                </div>

                                <div className="form-row">
                                    <div className="form-group">
                                        <label>Prioridad</label>
                                        <select
                                            name="priority"
                                            value={formData.priority}
                                            onChange={handleInputChange}>
                                            {priorities.map(p => (
                                                <option key={p.value} value={p.value}>{p.label}</option>
                                            ))}
                                        </select>
                                    </div>
                                    <div className="form-group">
                                        <label>Recurrencia</label>
                                        <select
                                            name="recurrence"
                                            value={formData.recurrence}
                                            onChange={handleInputChange}>
                                            {recurrences.map(r => (
                                                <option key={r.value} value={r.value}>{r.label}</option>
                                            ))}
                                        </select>
                                    </div>
                                    <div className="form-group">
                                        <label>Rúbrica</label>
                                        <select
                                            name="rubricId"
                                            value={formData.rubricId || ''}
                                            onChange={handleInputChange}>
                                            <option value="">Sin rúbrica</option>
                                            {rubrics.map(rubric => (
                                                <option key={rubric.id} value={rubric.id}>
                                                    {rubric.name} ({rubric.totalPoints} pts)
                                                </option>
                                            ))}
                                        </select>
                                    </div>
                                </div>

                                <div className="modal-actions">
                                    <button type="button" className="btn-secondary" onClick={handleCloseModal}>
                                        Cancelar
                                    </button>
                                    <button type="submit" className="btn-primary" disabled={conflicts.length > 0}>
                                        {editingActivity ? 'Actualizar' : 'Crear'}
                                    </button>
                                </div>
                            </form>
                        </div>
                    </div>
                )}

                <PopUp show={isOpen} onClose={() => setIsOpen(false)}>
                    {message}
                </PopUp>
            </div>
        </div>
    );
}

export default PlanActividades;

