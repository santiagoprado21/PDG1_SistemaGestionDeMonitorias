import './PlanActividades.css';
import React, { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import VerticalNavbar from './VerticalNavbar';
import { PopUp } from './PopUp';
import { BACKEND_URL } from './config/ApiBackend';

/**
 * HU-011: Creación de plan de actividades para monitores (Como profesor)
 * Componente para gestionar el plan completo de actividades de una monitoría
 */
function PlanActividades() {
    const { monitoringId } = useParams();
    const navigate = useNavigate();
    const role = localStorage.getItem('role');
    const user = localStorage.getItem('userId');

    // Estado del plan de actividades
    const [activityPlan, setActivityPlan] = useState(null);
    const [activities, setActivities] = useState([]);
    const [rubrics, setRubrics] = useState([]);
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
        rubricId: null
    });

    // Estado de conflictos
    const [conflicts, setConflicts] = useState([]);

    // PopUp
    const [isOpen, setIsOpen] = useState(false);
    const [message, setMessage] = useState('');

    const categorias = [
        'Asistencia a clases',
        'Tutoría',
        'Calificación de trabajos/talleres',
        'Preparación de material didáctico',
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
        if (monitoringId) {
            loadActivityPlan();
            loadRubrics();
        }
    }, [monitoringId]);

    const loadActivityPlan = async () => {
        setIsLoading(true);
        try {
            const response = await fetch(`${BACKEND_URL}/api/activity-schedule/plan/${monitoringId}`, {
                headers: {
                    'Authorization': localStorage.getItem('token')
                }
            });
            if (!response.ok) throw new Error('Error al cargar el plan de actividades');
            
            const data = await response.json();
            console.log('Plan de actividades cargado:', data);
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
                rubricId: activity.rubricId || null
            });
        } else {
            // Nueva actividad
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
                rubricId: null
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
        if (!formData.startTime || !formData.endTime || !formData.finish) {
            return; // No validar si faltan campos
        }

        const activityDTO = {
            id: editingActivity?.id,
            name: formData.name,
            finish: formData.finish,
            startTime: formData.startTime,
            endTime: formData.endTime,
            monitoringId: parseInt(monitoringId),
            monitorId: activityPlan?.monitorName ? activityPlan.monitorName.split(' ')[0] : null,
            state: 'Pendiente',
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
    }, [formData.startTime, formData.endTime, formData.finish]);

    const handleSubmit = async (e) => {
        e.preventDefault();

        if (conflicts.length > 0) {
            setMessage('No se puede guardar: hay conflictos de horarios pendientes');
            setIsOpen(true);
            return;
        }

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
            monitoringId: parseInt(monitoringId),
            professorId: user,
            monitorId: activityPlan?.monitorName ? activityPlan.monitorName.split(' ')[0] : null,
            state: 'Pendiente',
            roleCreator: 'profesor',
            roleResponsable: 'monitor',
            semester: activityPlan?.semester || '2025-1',
            creation: new Date().toISOString()
        };

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
            loadActivityPlan(); // Recargar el plan
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
                <h1>📋 Plan de Actividades</h1>

                {activityPlan && (
                    <div className="plan-header">
                        <div className="plan-info">
                            <h2>{activityPlan.courseName} - {activityPlan.programName}</h2>
                            <p><strong>Profesor:</strong> {activityPlan.professorName}</p>
                            <p><strong>Monitor:</strong> {activityPlan.monitorName}</p>
                            <p><strong>Semestre:</strong> {activityPlan.semester}</p>
                        </div>
                        <div className="plan-stats">
                            <div className="stat-card">
                                <span className="stat-value">{activityPlan.totalActivities}</span>
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
                    <button className="btn-primary" onClick={() => handleOpenModal()}>
                        ➕ Crear Nueva Actividad
                    </button>
                    <button className="btn-secondary" onClick={() => navigate('/crear-convocatoria')}>
                        ⬅️ Volver
                    </button>
                </div>

                <div className="activities-list">
                    {activities.length === 0 ? (
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
                                        <td>{activity.rubricName || '-'}</td>
                                        <td>
                                            <span className={`state-badge state-${activity.state?.toLowerCase()}`}>
                                                {activity.state}
                                            </span>
                                        </td>
                                        <td>
                                            <button 
                                                className="btn-edit"
                                                onClick={() => handleOpenModal(activity)}
                                                title="Editar">
                                                ✏️
                                            </button>
                                            <button 
                                                className="btn-delete"
                                                onClick={() => handleDeleteActivity(activity.id)}
                                                title="Eliminar">
                                                🗑️
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
                                <button className="btn-close" onClick={handleCloseModal}>×</button>
                            </div>

                            {conflicts.length > 0 && (
                                <div className="conflicts-alert">
                                    <strong>⚠️ Conflictos de horarios detectados:</strong>
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

                <PopUp isOpen={isOpen} onClose={() => setIsOpen(false)} message={message} />
            </div>
        </div>
    );
}

export default PlanActividades;

