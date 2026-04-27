import './GestionRubricas.css';
import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import VerticalNavbar from './VerticalNavbar';
import { PopUp } from './PopUp';
import { BACKEND_URL } from './config/ApiBackend';
import { Info, Plus, ClipboardList, Pencil, Trash2 } from 'lucide-react';

/**
 * HU-011: Componente para gestionar rúbricas de evaluación
 * El profesor puede crear, editar y eliminar rúbricas para asociar a actividades
 */
function GestionRubricas() {
    const iconProps = {
        size: 16,
        strokeWidth: 2,
        strokeLinecap: 'butt',
        strokeLinejoin: 'miter'
    };

    const navigate = useNavigate();
    const user = localStorage.getItem('userId');
    
    const [rubrics, setRubrics] = useState([]);
    const [isLoading, setIsLoading] = useState(false);
    const [showModal, setShowModal] = useState(false);
    const [editingRubric, setEditingRubric] = useState(null);
    
    // Formulario de rúbrica
    const [formData, setFormData] = useState({
        name: '',
        description: '',
        criteria: []
    });
    
    // PopUp
    const [isOpen, setIsOpen] = useState(false);
    const [message, setMessage] = useState('');

    useEffect(() => {
        loadRubrics();
    }, []);

    const loadRubrics = async () => {
        setIsLoading(true);
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
            console.error('Error:', error);
            setMessage('Error al cargar rúbricas: ' + error.message);
            setIsOpen(true);
        } finally {
            setIsLoading(false);
        }
    };

    const handleOpenModal = (rubric = null) => {
        if (rubric) {
            // Editar rúbrica existente
            setEditingRubric(rubric);
            
            // Mapear 'criterion' del backend a 'name' en el frontend
            const mappedCriteria = (rubric.criteria || []).map(c => ({
                name: c.criterion || c.name || '',  // Soportar ambos formatos
                description: c.description || '',
                points: c.points || 0
            }));
            
            setFormData({
                name: rubric.name || '',
                description: rubric.description || '',
                criteria: mappedCriteria.length > 0 ? mappedCriteria : [{ name: '', description: '', points: 0 }]
            });
        } else {
            // Nueva rúbrica
            setEditingRubric(null);
            setFormData({
                name: '',
                description: '',
                criteria: [{ name: '', description: '', points: 0 }]
            });
        }
        setShowModal(true);
    };

    const handleCloseModal = () => {
        setShowModal(false);
        setEditingRubric(null);
    };

    const handleInputChange = (e) => {
        const { name, value } = e.target;
        setFormData(prev => ({
            ...prev,
            [name]: value
        }));
    };

    const handleCriteriaChange = (index, field, value) => {
        console.log(`Cambiando criterio ${index}, campo ${field}, valor:`, value);
        const newCriteria = [...formData.criteria];
        newCriteria[index][field] = field === 'points' ? parseFloat(value) || 0 : value;
        console.log('Criterios actualizados:', newCriteria);
        setFormData(prev => ({
            ...prev,
            criteria: newCriteria
        }));
    };

    const handleAddCriteria = () => {
        setFormData(prev => ({
            ...prev,
            criteria: [...prev.criteria, { name: '', description: '', points: 0 }]
        }));
    };

    const handleRemoveCriteria = (index) => {
        if (formData.criteria.length <= 1) {
            setMessage('Debe haber al menos un criterio en la rúbrica');
            setIsOpen(true);
            return;
        }
        const newCriteria = formData.criteria.filter((_, i) => i !== index);
        setFormData(prev => ({
            ...prev,
            criteria: newCriteria
        }));
    };

    const calculateTotalPoints = () => {
        return formData.criteria.reduce((sum, c) => sum + (c.points || 0), 0);
    };

    const handleSubmit = async (e) => {
        e.preventDefault();

        // Validaciones
        if (formData.criteria.length === 0) {
            setMessage('Debe agregar al menos un criterio');
            setIsOpen(true);
            return;
        }

        const emptyFields = formData.criteria.some(c => !c.name || !c.description || c.points <= 0);
        if (emptyFields) {
            setMessage('Todos los criterios deben tener nombre, descripción y puntos mayores a 0');
            setIsOpen(true);
            return;
        }

        const totalPoints = calculateTotalPoints();
        
        console.log('FormData antes de mapear:', formData);
        
        // Mapear 'name' del frontend a 'criterion' para el backend
        const mappedCriteria = formData.criteria.map(c => ({
            criterion: c.name,  // El backend espera 'criterion'
            description: c.description,
            points: c.points
        }));
        
        console.log('Criterios mapeados para enviar al backend:', mappedCriteria);
        
        const rubricDTO = {
            id: editingRubric?.id,
            name: formData.name,
            description: formData.description,
            professorId: user,
            totalPoints: totalPoints,
            criteria: mappedCriteria
        };
        
        console.log('RubricDTO a enviar:', rubricDTO);

        try {
            const url = editingRubric 
                ? `${BACKEND_URL}/api/rubric/update/${editingRubric.id}`
                : `${BACKEND_URL}/api/rubric/create`;
            
            const method = editingRubric ? 'PUT' : 'POST';

            const response = await fetch(url, {
                method: method,
                headers: {
                    'Content-Type': 'application/json',
                    'Authorization': localStorage.getItem('token')
                },
                body: JSON.stringify(rubricDTO)
            });

            if (!response.ok) {
                const errorText = await response.text();
                throw new Error(errorText);
            }

            setMessage(editingRubric ? 'Rúbrica actualizada exitosamente' : 'Rúbrica creada exitosamente');
            setIsOpen(true);
            handleCloseModal();
            loadRubrics();
        } catch (error) {
            console.error('Error:', error);
            setMessage('Error al guardar la rúbrica: ' + error.message);
            setIsOpen(true);
        }
    };

    const handleDeleteRubric = async (rubricId) => {
        if (!window.confirm('¿Está seguro de eliminar esta rúbrica? Esta acción no se puede deshacer.')) {
            return;
        }

        try {
            const response = await fetch(`${BACKEND_URL}/api/rubric/delete/${rubricId}`, {
                method: 'DELETE',
                headers: {
                    'Authorization': localStorage.getItem('token')
                }
            });

            if (!response.ok) {
                const errorText = await response.text();
                throw new Error(errorText);
            }

            setMessage('Rúbrica eliminada exitosamente');
            setIsOpen(true);
            loadRubrics();
        } catch (error) {
            console.error('Error:', error);
            setMessage('Error al eliminar la rúbrica: ' + error.message);
            setIsOpen(true);
        }
    };

    if (isLoading) {
        return (
            <div className="monitoring-container gestion-rubricas-page">
                <VerticalNavbar />
                <div className="main-content">
                    <div className="loading">Cargando rúbricas...</div>
                </div>
            </div>
        );
    }

    return (
        <div className="monitoring-container gestion-rubricas-page">
            <VerticalNavbar />
            <div className="main-content gestion-rubricas-content">
                <div className="title-container-gestion-rubricas prof-page-header">
                    <h1 className="prof-page-title">Gestión de Rúbricas</h1>
                    <p className="subtitle prof-page-subtitle">Crea y gestiona rúbricas para evaluar las actividades de tus monitores</p>
                </div>
                <div className="info-banner">
                    <span className="info-icon"><Info {...iconProps} /></span>
                    <div className="info-text">
                        <strong>Las rúbricas son recursos reutilizables</strong> que puedes asociar a cualquier actividad de cualquiera de tus monitorías. 
                        Una misma rúbrica puede usarse en múltiples actividades.
                    </div>
                </div>

                <div className="rubricas-actions">
                    <button className="btn-primary" onClick={() => handleOpenModal()}>
                        <Plus {...iconProps} style={{ marginRight: '6px', verticalAlign: 'text-bottom' }} />Crear Nueva Rúbrica
                    </button>
                    <button className="btn-secondary" onClick={() => navigate('/plan-actividades')}>
                        <ClipboardList {...iconProps} style={{ marginRight: '6px', verticalAlign: 'text-bottom' }} />Ir a Plan de Actividades
                    </button>
                </div>

                <div className="rubricas-grid">
                    {rubrics.length === 0 ? (
                        <div className="no-rubricas">
                            <p>No hay rúbricas creadas.</p>
                            <p>Haz clic en "Crear Nueva Rúbrica" para comenzar.</p>
                        </div>
                    ) : (
                        rubrics.map(rubric => (
                            <div key={rubric.id} className="rubrica-card">
                                <div className="rubrica-header">
                                    <h3>{rubric.name}</h3>
                                    <div className="rubrica-score">
                                        <span className="score-value">{rubric.totalPoints}</span>
                                        <span className="score-label">pts</span>
                                    </div>
                                </div>
                                <p className="rubrica-description">{rubric.description}</p>
                                
                                <div className="rubrica-criteria">
                                    <h4>Criterios ({rubric.criteria?.length || 0})</h4>
                                    {rubric.criteria && rubric.criteria.length > 0 ? (
                                        <ul>
                                            {rubric.criteria.map((criterion, idx) => (
                                                <li key={idx}>
                                                    <div className="criterion-info">
                                                        <strong>{criterion.criterion || criterion.name || 'Sin nombre'}</strong>
                                                        <span className="criterion-points">{criterion.points} pts</span>
                                                    </div>
                                                    <p>{criterion.description}</p>
                                                </li>
                                            ))}
                                        </ul>
                                    ) : (
                                        <p className="no-criteria">Sin criterios definidos</p>
                                    )}
                                </div>

                                <div className="rubrica-actions">
                                    <button 
                                        className="btn-edit btn-secondary"
                                        onClick={() => handleOpenModal(rubric)}
                                        title="Editar rúbrica">
                                        <Pencil {...iconProps} size={14} style={{ marginRight: '6px', verticalAlign: 'text-bottom' }} />Editar
                                    </button>
                                    <button 
                                        className="btn-delete btn-danger"
                                        onClick={() => handleDeleteRubric(rubric.id)}
                                        title="Eliminar rúbrica">
                                        <Trash2 {...iconProps} size={14} style={{ marginRight: '6px', verticalAlign: 'text-bottom' }} />Eliminar
                                    </button>
                                </div>
                            </div>
                        ))
                    )}
                </div>

                {/* Modal de creación/edición */}
                {showModal && (
                    <div className="modal-overlay" onClick={handleCloseModal}>
                        <div className="modal-content large" onClick={(e) => e.stopPropagation()}>
                            <div className="modal-header">
                                <h2>{editingRubric ? 'Editar Rúbrica' : 'Nueva Rúbrica'}</h2>
                                <button className="btn-close btn-secondary" onClick={handleCloseModal}>×</button>
                            </div>

                            <form onSubmit={handleSubmit}>
                                <div className="form-section">
                                    <h3>Información General</h3>
                                    <div className="form-group">
                                        <label>Nombre de la Rúbrica *</label>
                                        <input
                                            type="text"
                                            name="name"
                                            value={formData.name}
                                            onChange={handleInputChange}
                                            placeholder="Ej: Evaluación de Tutoría"
                                            required
                                        />
                                    </div>
                                    <div className="form-group">
                                        <label>Descripción *</label>
                                        <textarea
                                            name="description"
                                            value={formData.description}
                                            onChange={handleInputChange}
                                            rows="2"
                                            placeholder="Describe el propósito de esta rúbrica"
                                            required
                                        />
                                    </div>
                                </div>

                                <div className="form-section">
                                    <div className="criteria-header">
                                        <h3>Criterios de Evaluación</h3>
                                        <div className="total-points">
                                            <span>Total: </span>
                                            <strong>{calculateTotalPoints()}</strong>
                                            <span> puntos</span>
                                        </div>
                                    </div>

                                    {formData.criteria.map((criterion, index) => (
                                        <div key={index} className="criterion-item">
                                            <div className="criterion-number">Criterio {index + 1}</div>
                                            <div className="criterion-fields">
                                                <div className="form-group">
                                                    <label>Nombre del Criterio *</label>
                                                    <input
                                                        type="text"
                                                        value={criterion.name}
                                                        onChange={(e) => handleCriteriaChange(index, 'name', e.target.value)}
                                                        placeholder="Ej: Puntualidad"
                                                        required
                                                    />
                                                </div>
                                                <div className="form-group">
                                                    <label>Descripción *</label>
                                                    <textarea
                                                        value={criterion.description}
                                                        onChange={(e) => handleCriteriaChange(index, 'description', e.target.value)}
                                                        rows="2"
                                                        placeholder="¿Qué se evalúa en este criterio?"
                                                        required
                                                    />
                                                </div>
                                                <div className="form-group points-group">
                                                    <label>Puntos *</label>
                                                    <input
                                                        type="number"
                                                        value={criterion.points}
                                                        onChange={(e) => handleCriteriaChange(index, 'points', e.target.value)}
                                                        min="0"
                                                        step="0.5"
                                                        required
                                                    />
                                                </div>
                                                <button
                                                    type="button"
                                                    className="btn-remove-criterion"
                                                    onClick={() => handleRemoveCriteria(index)}
                                                    title="Eliminar criterio">
                                                    <Trash2 {...iconProps} size={14} />
                                                </button>
                                            </div>
                                        </div>
                                    ))}

                                    <button
                                        type="button"
                                        className="btn-add-criterion"
                                        onClick={handleAddCriteria}>
                                        <Plus {...iconProps} size={14} style={{ marginRight: '6px', verticalAlign: 'text-bottom' }} />Agregar Criterio
                                    </button>
                                </div>

                                <div className="modal-actions">
                                    <button type="button" className="btn-secondary" onClick={handleCloseModal}>
                                        Cancelar
                                    </button>
                                    <button type="submit" className="btn-primary">
                                        {editingRubric ? 'Actualizar Rúbrica' : 'Crear Rúbrica'}
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

export default GestionRubricas;

