import React, { useEffect, useMemo, useState } from 'react';
import VerticalNavbar from './VerticalNavbar';
import LoadingSpinner from './LoadingSpinner';
import { PopUp } from './PopUp';
import { BACKEND_URL } from './config/ApiBackend';
import './EvaluarMonitoresHU015.css';

const SCORE_OPTIONS = [1, 2, 3, 4, 5];
const PERFORMANCE_LABELS = {
  EXCELENTE: 'Excelente',
  DESTACADO: 'Destacado',
  ADECUADO: 'Adecuado',
  EN_RIESGO: 'En riesgo'
};

const PERFORMANCE_CLASSES = {
  EXCELENTE: 'badge-excelente',
  DESTACADO: 'badge-destacado',
  ADECUADO: 'badge-adecuado',
  EN_RIESGO: 'badge-riesgo'
};

function EvaluarMonitoresHU015() {
  const professorId = localStorage.getItem('userId');
  const token = localStorage.getItem('token');

  const [assignments, setAssignments] = useState([]);
  const [loadingAssignments, setLoadingAssignments] = useState(true);
  const [searchTerm, setSearchTerm] = useState('');
  const [selectedAssignment, setSelectedAssignment] = useState(null);
  const [visibleToMonitor, setVisibleToMonitor] = useState(true);
  const [saving, setSaving] = useState(false);

  const [formValues, setFormValues] = useState({
    taskCompliance: 3,
    timelyCommunication: 3,
    planFulfillment: 3,
    attitude: 3,
    comments: ''
  });

  const [isOpen, setIsOpen] = useState(false);
  const [message, setMessage] = useState('');

  const showMessage = (text) => {
    setMessage(text);
    setIsOpen(true);
  };

  const closePopup = () => {
    setIsOpen(false);
  };

  const fetchAssignments = async (term = '') => {
    if (!professorId) {
      showMessage('No se pudo identificar al profesor autenticado.');
      return;
    }
    setLoadingAssignments(true);
    try {
      const url = new URL(`${BACKEND_URL}/monitor-evaluations/professor/${professorId}/assignments`);
      if (term && term.trim()) {
        url.searchParams.append('search', term.trim());
      }

      const response = await fetch(url.toString(), {
        method: 'GET',
        headers: {
          'Content-Type': 'application/json',
          Authorization: token
        }
      });

      if (!response.ok) {
        const errorBody = await response.json().catch(() => ({}));
        throw new Error(errorBody.error || 'No fue posible obtener las monitorías pendientes de evaluación.');
      }

      const data = await response.json();
      setAssignments(Array.isArray(data) ? data : []);
    } catch (error) {
      console.error('Error obteniendo monitorías a evaluar:', error);
      showMessage(error.message);
    } finally {
      setLoadingAssignments(false);
    }
  };

  useEffect(() => {
    fetchAssignments();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  useEffect(() => {
    const timeout = setTimeout(() => {
      fetchAssignments(searchTerm);
    }, 350);

    return () => clearTimeout(timeout);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [searchTerm]);

  const pendingAssignments = useMemo(
    () => assignments.filter((item) => !item.evaluated),
    [assignments]
  );

  const completedAssignments = useMemo(
    () => assignments.filter((item) => item.evaluated),
    [assignments]
  );

  const handleSelectAssignment = (assignment) => {
    setSelectedAssignment(assignment);
    setFormValues({
      taskCompliance: assignment.taskCompliance ?? 3,
      timelyCommunication: assignment.timelyCommunication ?? 3,
      planFulfillment: assignment.planFulfillment ?? 3,
      attitude: assignment.attitude ?? 3,
      comments: assignment.comments ?? ''
    });
    setVisibleToMonitor(assignment.visibleToMonitor ?? true);
  };

  const updateScore = (field, value) => {
    setFormValues((prev) => ({
      ...prev,
      [field]: Number(value)
    }));
  };

  const handleScoreKeyDown = (event, field, currentValue) => {
    const currentIndex = SCORE_OPTIONS.indexOf(Number(currentValue));
    if (event.key === 'ArrowRight' || event.key === 'ArrowUp') {
      event.preventDefault();
      const nextIndex = Math.min(SCORE_OPTIONS.length - 1, currentIndex + 1);
      updateScore(field, SCORE_OPTIONS[nextIndex]);
    }
    if (event.key === 'ArrowLeft' || event.key === 'ArrowDown') {
      event.preventDefault();
      const prevIndex = Math.max(0, currentIndex - 1);
      updateScore(field, SCORE_OPTIONS[prevIndex]);
    }
    if (event.key === 'Home') {
      event.preventDefault();
      updateScore(field, SCORE_OPTIONS[0]);
    }
    if (event.key === 'End') {
      event.preventDefault();
      updateScore(field, SCORE_OPTIONS[SCORE_OPTIONS.length - 1]);
    }
  };

  const formattedAverage = useMemo(() => {
    const { taskCompliance, timelyCommunication, planFulfillment, attitude } = formValues;
    const average = (taskCompliance + timelyCommunication + planFulfillment + attitude) / 4;
    return average.toFixed(2);
  }, [formValues]);

  const performanceLevel = useMemo(() => {
    const total = parseFloat(formattedAverage);
    if (total >= 4.5) return 'EXCELENTE';
    if (total >= 3.5) return 'DESTACADO';
    if (total >= 3.0) return 'ADECUADO';
    return 'EN_RIESGO';
  }, [formattedAverage]);

  const penaltyFlag = useMemo(() => parseFloat(formattedAverage) < 3, [formattedAverage]);

  const resetForm = () => {
    setSelectedAssignment(null);
    setFormValues({
      taskCompliance: 3,
      timelyCommunication: 3,
      planFulfillment: 3,
      attitude: 3,
      comments: ''
    });
    setVisibleToMonitor(true);
  };

  const handleSubmit = async (event) => {
    event.preventDefault();
    if (!selectedAssignment) {
      showMessage('Selecciona una monitoría para evaluar.');
      return;
    }
    const payload = {
      professorId,
      monitoringId: selectedAssignment.monitoringId,
      monitorCode: selectedAssignment.monitorCode,
      taskCompliance: formValues.taskCompliance,
      timelyCommunication: formValues.timelyCommunication,
      planFulfillment: formValues.planFulfillment,
      attitude: formValues.attitude,
      comments: formValues.comments,
      visibleToMonitor
    };

    const method = selectedAssignment.evaluationId ? 'PUT' : 'POST';
    const url = selectedAssignment.evaluationId
      ? `${BACKEND_URL}/monitor-evaluations/${selectedAssignment.evaluationId}`
      : `${BACKEND_URL}/monitor-evaluations`;

    setSaving(true);
    try {
      const response = await fetch(url, {
        method,
        headers: {
          'Content-Type': 'application/json',
          Authorization: token
        },
        body: JSON.stringify(payload)
      });

      const body = await response.json().catch(() => ({}));
      if (!response.ok) {
        throw new Error(body.error || 'No se pudo guardar la evaluación.');
      }

      showMessage('La evaluación se guardó correctamente.');
      resetForm();
      fetchAssignments(searchTerm);
    } catch (error) {
      console.error('Error guardando la evaluación:', error);
      showMessage(error.message);
    } finally {
      setSaving(false);
    }
  };

  const renderAssignmentItem = (assignment) => {
    const isSelected = selectedAssignment && selectedAssignment.monitoringId === assignment.monitoringId && selectedAssignment.monitorCode === assignment.monitorCode;
    const badgeClass = PERFORMANCE_CLASSES[assignment.performanceLevel] || 'badge-adecuado';
    return (
      <button
        key={`${assignment.monitoringId}-${assignment.monitorCode}`}
        className={`assignment-card ${isSelected ? 'selected' : ''}`}
        onClick={() => handleSelectAssignment(assignment)}
      >
        <div className="assignment-header">
          <h4>{assignment.monitorFullName || 'Monitor sin nombre'}</h4>
          <span className={`badge ${badgeClass}`}>
            {assignment.evaluated ? PERFORMANCE_LABELS[assignment.performanceLevel] || 'Evaluado' : 'Pendiente'}
          </span>
        </div>
        <p className="assignment-subtitle">{assignment.monitoringName || 'Monitoría sin nombre'}</p>
        <p className="assignment-meta">{assignment.courseName || 'Curso no asignado'} · {assignment.semester || 'Semestre sin registrar'}</p>
        {assignment.evaluated && (
          <div className="assignment-score">
            <strong>{assignment.totalScore.toFixed(2)}</strong>
            <span>Puntaje final</span>
          </div>
        )}
      </button>
    );
  };

  return (
    <div className="evaluar-monitores-layout">
      <VerticalNavbar />
      <PopUp show={isOpen} onClose={closePopup}>
        {message}
      </PopUp>
      <div className="evaluar-monitores-content">
        <section className="assignments-panel">
          <header className="panel-header">
            <h2>Evaluaciones de monitores</h2>
            <p className="panel-description">
              Selecciona una monitoría para valorar el desempeño del monitor asignado. Puedes buscar por nombre del estudiante, curso o nombre de la monitoría.
            </p>
            <input
              type="search"
              placeholder="Buscar por estudiante, curso o monitoría"
              value={searchTerm}
              onChange={(event) => setSearchTerm(event.target.value)}
              className="search-input"
            />
          </header>

          {loadingAssignments ? (
            <div className="loading-wrapper">
              <LoadingSpinner message="Cargando monitorías" />
            </div>
          ) : (
            <>
              <div className="assignments-group">
                <h3>Pendientes de evaluación</h3>
                {pendingAssignments.length === 0 ? (
                  <p className="empty-state">No hay monitorías pendientes de evaluación.</p>
                ) : (
                  <div className="cards-grid">
                    {pendingAssignments.map(renderAssignmentItem)}
                  </div>
                )}
              </div>

              <div className="assignments-group">
                <h3>Evaluaciones registradas</h3>
                {completedAssignments.length === 0 ? (
                  <p className="empty-state">Aún no has registrado evaluaciones.</p>
                ) : (
                  <div className="cards-grid">
                    {completedAssignments.map(renderAssignmentItem)}
                  </div>
                )}
              </div>
            </>
          )}
        </section>

        <section className="form-panel evaluacion-container">
          {!selectedAssignment ? (
            <div className="placeholder-panel">
              <h3>Selecciona una monitoría</h3>
              <p>Usa la lista a la izquierda para escoger la monitoría y el monitor que deseas evaluar.</p>
            </div>
          ) : (
            <form className="evaluation-form" onSubmit={handleSubmit}>
              <header className="form-header">
                <div>
                  <h3>{selectedAssignment.monitorFullName}</h3>
                  <p>{selectedAssignment.monitoringName}</p>
                  <span className="form-meta">{selectedAssignment.courseName} · {selectedAssignment.semester}</span>
                </div>
                <div className={`impact-badge ${PERFORMANCE_CLASSES[performanceLevel]}`}>
                  <span>{PERFORMANCE_LABELS[performanceLevel]}</span>
                  <strong>{formattedAverage}</strong>
                </div>
              </header>

              <div className="scores-grid">
                <label>
                  Cumplimiento de tareas
                  <div className="score-scale" role="radiogroup" aria-label="Cumplimiento de tareas">
                    {SCORE_OPTIONS.map((option) => (
                      <button
                        key={`task-${option}`}
                        type="button"
                        className={`score-chip ${formValues.taskCompliance === option ? 'is-selected' : ''}`}
                        onClick={() => updateScore('taskCompliance', option)}
                        onKeyDown={(event) => handleScoreKeyDown(event, 'taskCompliance', formValues.taskCompliance)}
                        disabled={saving}
                        role="radio"
                        tabIndex={formValues.taskCompliance === option ? 0 : -1}
                        aria-checked={formValues.taskCompliance === option}
                        aria-pressed={formValues.taskCompliance === option}
                      >
                        {option}
                      </button>
                    ))}
                  </div>
                  <div className="scale-hint" aria-hidden="true">
                    <span>1 = Bajo</span>
                    <span>5 = Alto</span>
                  </div>
                </label>

                <label>
                  Comunicación oportuna
                  <div className="score-scale" role="radiogroup" aria-label="Comunicación oportuna">
                    {SCORE_OPTIONS.map((option) => (
                      <button
                        key={`comm-${option}`}
                        type="button"
                        className={`score-chip ${formValues.timelyCommunication === option ? 'is-selected' : ''}`}
                        onClick={() => updateScore('timelyCommunication', option)}
                        onKeyDown={(event) => handleScoreKeyDown(event, 'timelyCommunication', formValues.timelyCommunication)}
                        disabled={saving}
                        role="radio"
                        tabIndex={formValues.timelyCommunication === option ? 0 : -1}
                        aria-checked={formValues.timelyCommunication === option}
                        aria-pressed={formValues.timelyCommunication === option}
                      >
                        {option}
                      </button>
                    ))}
                  </div>
                  <div className="scale-hint" aria-hidden="true">
                    <span>1 = Bajo</span>
                    <span>5 = Alto</span>
                  </div>
                </label>

                <label>
                  Cumplimiento del plan de trabajo
                  <div className="score-scale" role="radiogroup" aria-label="Cumplimiento del plan de trabajo">
                    {SCORE_OPTIONS.map((option) => (
                      <button
                        key={`plan-${option}`}
                        type="button"
                        className={`score-chip ${formValues.planFulfillment === option ? 'is-selected' : ''}`}
                        onClick={() => updateScore('planFulfillment', option)}
                        onKeyDown={(event) => handleScoreKeyDown(event, 'planFulfillment', formValues.planFulfillment)}
                        disabled={saving}
                        role="radio"
                        tabIndex={formValues.planFulfillment === option ? 0 : -1}
                        aria-checked={formValues.planFulfillment === option}
                        aria-pressed={formValues.planFulfillment === option}
                      >
                        {option}
                      </button>
                    ))}
                  </div>
                  <div className="scale-hint" aria-hidden="true">
                    <span>1 = Bajo</span>
                    <span>5 = Alto</span>
                  </div>
                </label>

                <label>
                  Actitud y servicio
                  <div className="score-scale" role="radiogroup" aria-label="Actitud y servicio">
                    {SCORE_OPTIONS.map((option) => (
                      <button
                        key={`att-${option}`}
                        type="button"
                        className={`score-chip ${formValues.attitude === option ? 'is-selected' : ''}`}
                        onClick={() => updateScore('attitude', option)}
                        onKeyDown={(event) => handleScoreKeyDown(event, 'attitude', formValues.attitude)}
                        disabled={saving}
                        role="radio"
                        tabIndex={formValues.attitude === option ? 0 : -1}
                        aria-checked={formValues.attitude === option}
                        aria-pressed={formValues.attitude === option}
                      >
                        {option}
                      </button>
                    ))}
                  </div>
                  <div className="scale-hint" aria-hidden="true">
                    <span>1 = Bajo</span>
                    <span>5 = Alto</span>
                  </div>
                </label>
              </div>

              <label className="comments-field">
                Comentarios cualitativos
                <textarea
                  value={formValues.comments}
                  onChange={(event) => setFormValues((prev) => ({ ...prev, comments: event.target.value }))}
                  placeholder="Comparte retroalimentación específica que apoye el crecimiento del monitor."
                />
              </label>

              <div className="visibility-row">
                <label className="toggle">
                  <input
                    type="checkbox"
                    checked={visibleToMonitor}
                    onChange={(event) => setVisibleToMonitor(event.target.checked)}
                  />
                  Mostrar esta evaluación al monitor 
                </label>
                {penaltyFlag && (
                  <span className="penalty-alert">⚠ Puntaje por debajo de 3.0. Recomendado revisar posibles penalizaciones.</span>
                )}
              </div>

              <button type="submit" className="submit-button" disabled={saving}>
                {saving ? 'Guardando…' : selectedAssignment.evaluated ? 'Actualizar evaluación' : 'Guardar evaluación'}
              </button>
            </form>
          )}
        </section>
      </div>
    </div>
  );
}

export default EvaluarMonitoresHU015;
