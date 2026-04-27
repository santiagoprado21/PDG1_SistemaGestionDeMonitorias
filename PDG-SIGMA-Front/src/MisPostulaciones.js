import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import VerticalNavbar from './VerticalNavbar';
import { PopUp } from './PopUp';
import { BACKEND_URL } from './config/ApiBackend';
import LoadingSpinner from './LoadingSpinner';
import './MisPostulaciones.css';

/**
 * HU2-273: Vista del Monitor - Mis Postulaciones
 * Permite al monitor ver a qué convocatorias se ha postulado,
 * el estado actual de cada una y el resultado de las pasadas.
 */
function MisPostulaciones() {
    const navigate = useNavigate();
    const role = localStorage.getItem('role');
    const monitorId = localStorage.getItem('userId');

    const [postulaciones, setPostulaciones] = useState([]);
    const [filteredPostulaciones, setFilteredPostulaciones] = useState([]);
    const [filterEstado, setFilterEstado] = useState('todas');
    const [isLoading, setIsLoading] = useState(false);
    const [isOpen, setIsOpen] = useState(false);
    const [message, setMessage] = useState('');
    const [selectedPostulacion, setSelectedPostulacion] = useState(null);
    const [showModal, setShowModal] = useState(false);

    useEffect(() => {
        if (role !== 'monitor') {
            navigate('/');
            return;
        }
        loadPostulaciones();
    }, [role, navigate]);

    useEffect(() => {
        applyFilters();
    }, [postulaciones, filterEstado]);

    const loadPostulaciones = async () => {
        setIsLoading(true);
        try {
            const response = await fetch(`${BACKEND_URL}/monitor-application/monitor/${monitorId}`, {
                method: 'GET',
                headers: {
                    'Content-Type': 'application/json',
                    'Authorization': localStorage.getItem('token')
                }
            });

            if (!response.ok) {
                throw new Error('Error al cargar tus postulaciones');
            }

            const data = await response.json();
            setPostulaciones(data || []);
        } catch (error) {
            setMessage('Error al cargar tus postulaciones: ' + error.message);
            setIsOpen(true);
            setPostulaciones([]);
        } finally {
            setIsLoading(false);
        }
    };

    const applyFilters = () => {
        if (filterEstado === 'todas') {
            setFilteredPostulaciones(postulaciones);
            return;
        }
        const filtered = postulaciones.filter(p => p.status === filterEstado);
        setFilteredPostulaciones(filtered);
    };

    const getAppStatusLabel = (status) => {
        switch (status) {
            case 'POSTULADO': return 'En revisión';
            case 'SELECCIONADO': return '¡Seleccionado!';
            case 'NO_SELECCIONADO': return 'No seleccionado';
            default: return status || 'Desconocido';
        }
    };

    const getAppStatusClass = (status) => {
        switch (status) {
            case 'POSTULADO': return 'badge-postulado';
            case 'SELECCIONADO': return 'badge-seleccionado';
            case 'NO_SELECCIONADO': return 'badge-no-seleccionado';
            default: return '';
        }
    };

    const getRequestStatusLabel = (status) => {
        switch (status) {
            case 'PENDIENTE_APROBACION_JEFE': return 'Pendiente aprobación';
            case 'CONVOCATORIA_ABIERTA': return 'Convocatoria abierta';
            case 'MONITOR_SELECCIONADO': return 'Monitor seleccionado';
            case 'PENDIENTE_APROBACION': return 'Pendiente aprobación final';
            case 'APROBADA': return 'Cerrada';
            case 'RECHAZADA': return 'Rechazada';
            case 'CANCELADA': return 'Cancelada';
            default: return status || 'Desconocido';
        }
    };

    const getRequestStatusClass = (status) => {
        switch (status) {
            case 'PENDIENTE_APROBACION_JEFE':
            case 'PENDIENTE_APROBACION': return 'req-pendiente';
            case 'CONVOCATORIA_ABIERTA': return 'req-abierta';
            case 'MONITOR_SELECCIONADO': return 'req-seleccionado';
            case 'APROBADA': return 'req-aprobada';
            case 'RECHAZADA': return 'req-rechazada';
            case 'CANCELADA': return 'req-cancelada';
            default: return '';
        }
    };

    const formatDate = (dateStr) => {
        if (!dateStr) return 'N/A';
        return new Date(dateStr).toLocaleDateString('es-ES', {
            year: 'numeric',
            month: 'long',
            day: 'numeric'
        });
    };

    const openDetalle = (postulacion) => {
        setSelectedPostulacion(postulacion);
        setShowModal(true);
    };

    const closeModal = () => {
        setShowModal(false);
        setSelectedPostulacion(null);
    };

    const totalPostulaciones = postulaciones.length;
    const enRevision = postulaciones.filter(p => p.status === 'POSTULADO').length;
    const seleccionadas = postulaciones.filter(p => p.status === 'SELECCIONADO').length;
    const noSeleccionadas = postulaciones.filter(p => p.status === 'NO_SELECCIONADO').length;

    return (
        <div className="mis-postulaciones-container">
            <VerticalNavbar />

            {isLoading && <LoadingSpinner />}

            <PopUp show={isOpen} onClose={() => setIsOpen(false)}>
                <div style={{ textAlign: 'center', padding: '20px', fontSize: '16px' }}>
                    {message}
                </div>
            </PopUp>

            <div className="mis-postulaciones-content">
                <div className="mis-post-header app-page-header">
                    <h1 className="app-page-title">Mis Postulaciones</h1>
                    <p className="mis-post-subtitle app-page-subtitle">
                        Consulta el estado de las convocatorias en las que has participado
                    </p>
                </div>

                {/* Resumen estadístico */}
                {totalPostulaciones > 0 && (
                    <div className="mis-post-stats">
                        <div className="stat-card">
                            <span className="stat-number">{totalPostulaciones}</span>
                            <span className="stat-label">Total</span>
                        </div>
                        <div className="stat-card stat-revision">
                            <span className="stat-number">{enRevision}</span>
                            <span className="stat-label">En revisión</span>
                        </div>
                        <div className="stat-card stat-seleccionado">
                            <span className="stat-number">{seleccionadas}</span>
                            <span className="stat-label">Seleccionado</span>
                        </div>
                        <div className="stat-card stat-no-seleccionado">
                            <span className="stat-number">{noSeleccionadas}</span>
                            <span className="stat-label">No seleccionado</span>
                        </div>
                    </div>
                )}

                {/* Filtros por estado */}
                <div className="mis-post-filters">
                    {[
                        { value: 'todas', label: 'Todas' },
                        { value: 'POSTULADO', label: 'En revisión' },
                        { value: 'SELECCIONADO', label: 'Seleccionado' },
                        { value: 'NO_SELECCIONADO', label: 'No seleccionado' }
                    ].map(({ value, label }) => (
                        <button
                            key={value}
                            className={`filter-tab ${filterEstado === value ? 'filter-tab-active' : ''}`}
                            onClick={() => setFilterEstado(value)}
                        >
                            {label}
                            {value !== 'todas' && (
                                <span className="filter-count">
                                    {value === 'POSTULADO' ? enRevision
                                        : value === 'SELECCIONADO' ? seleccionadas
                                        : noSeleccionadas}
                                </span>
                            )}
                        </button>
                    ))}
                </div>

                {/* Lista de postulaciones */}
                {!isLoading && (
                    filteredPostulaciones.length === 0 ? (
                        <div className="mis-post-empty">
                            <p>No tienes postulaciones{filterEstado !== 'todas' ? ' en este estado' : ''}</p>
                            {filterEstado === 'todas' && (
                                <p className="empty-hint">
                                    Visita <strong>Convocatorias Abiertas</strong> para postularte a una monitoría
                                </p>
                            )}
                        </div>
                    ) : (
                        <div className="mis-post-grid">
                            {filteredPostulaciones.map((postulacion) => (
                                <div
                                    key={postulacion.id}
                                    className={`postulacion-card ${{
                                        'SELECCIONADO':    'card-seleccionado',
                                        'NO_SELECCIONADO': 'card-no-seleccionado',
                                        'POSTULADO':       'card-revision'
                                    }[postulacion.status] || ''}`}
                                    onClick={() => openDetalle(postulacion)}
                                >
                                    <div className="card-header-row">
                                        <h3 className="card-course">{postulacion.courseName || 'N/A'}</h3>
                                        <span className={`app-status-badge ${getAppStatusClass(postulacion.status)}`}>
                                            {getAppStatusLabel(postulacion.status)}
                                        </span>
                                    </div>

                                    <div className="card-body">
                                        <p>
                                            <span className="card-label">Profesor:</span>
                                            {postulacion.professorName || 'N/A'}
                                        </p>
                                        <p>
                                            <span className="card-label">Horas solicitadas:</span>
                                            {postulacion.requestedHours} h
                                        </p>
                                        <p>
                                            <span className="card-label">Fecha de postulación:</span>
                                            {formatDate(postulacion.applicationDate)}
                                        </p>
                                    </div>

                                    <div className="card-footer-row">
                                        <span className="card-label">Estado convocatoria:</span>
                                        <span className={`req-status-badge ${getRequestStatusClass(postulacion.requestStatus)}`}>
                                            {getRequestStatusLabel(postulacion.requestStatus)}
                                        </span>
                                    </div>

                                    <div className="card-action-hint">Ver detalle </div>
                                </div>
                            ))}
                        </div>
                    )
                )}
            </div>

            {/* Modal de detalle */}
            {showModal && selectedPostulacion && (
                <div className="modal-overlay" onClick={closeModal}>
                    <div className="modal-content" onClick={(e) => e.stopPropagation()}>
                        <div className="modal-header">
                            <h2>{selectedPostulacion.courseName}</h2>
                            <button className="close-btn" onClick={closeModal}>x</button>
                        </div>

                        <div className="modal-body">
                            <section className="detail-section">
                                <h3>Información de la convocatoria</h3>
                                <p><strong>Profesor:</strong> {selectedPostulacion.professorName || 'N/A'}</p>
                                <p><strong>Horas solicitadas:</strong> {selectedPostulacion.requestedHours} h</p>
                                <p>
                                    <strong>Estado de la convocatoria:</strong>{' '}
                                    <span className={`req-status-badge ${getRequestStatusClass(selectedPostulacion.requestStatus)}`}>
                                        {getRequestStatusLabel(selectedPostulacion.requestStatus)}
                                    </span>
                                </p>
                            </section>

                            <section className="detail-section">
                                <h3>Tu postulación</h3>
                                <p><strong>Fecha de postulación:</strong> {formatDate(selectedPostulacion.applicationDate)}</p>
                                <p><strong>Última actualización:</strong> {formatDate(selectedPostulacion.updatedAt)}</p>
                                <p>
                                    <strong>Resultado:</strong>{' '}
                                    <span className={`app-status-badge ${getAppStatusClass(selectedPostulacion.status)}`}>
                                        {getAppStatusLabel(selectedPostulacion.status)}
                                    </span>
                                </p>
                                {selectedPostulacion.notes && (
                                    <p><strong>Comentarios:</strong> {selectedPostulacion.notes}</p>
                                )}
                            </section>

                            {selectedPostulacion.motivationLetter && (
                                <section className="detail-section">
                                    <h3>Tu carta de motivación</h3>
                                    <p className="motivation-text">{selectedPostulacion.motivationLetter}</p>
                                </section>
                            )}

                            {selectedPostulacion.status === 'SELECCIONADO' && (
                                <div className="selected-banner">
                                    ¡Felicitaciones! Fuiste seleccionado como monitor para esta monitoría.
                                </div>
                            )}
                        </div>

                        <div className="modal-footer">
                            <button className="btn-cerrar" onClick={closeModal}>Cerrar</button>
                        </div>
                    </div>
                </div>
            )}
        </div>
    );
}

export default MisPostulaciones;

