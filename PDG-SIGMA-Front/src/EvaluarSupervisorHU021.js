import React, { useEffect, useMemo, useState } from 'react';
import VerticalNavbar from './VerticalNavbar';
import LoadingSpinner from './LoadingSpinner';
import { PopUp } from './PopUp';
import { BACKEND_URL } from './config/ApiBackend';
import './EvaluarMonitoresHU015.css';
import './EvaluarSupervisorHU021.css';

const SCORE_OPTIONS = [1, 2, 3, 4, 5, 6, 7];

const PERFORMANCE_LABELS = {
  EXCELENTE: 'Excelente',
  DESTACADO: 'Destacado',
  ADECUADO: 'Adecuado',
  EN_RIESGO: 'Deficiente'
};

const PERFORMANCE_CLASSES = {
  EXCELENTE: 'badge-excelente',
  DESTACADO: 'badge-destacado',
  ADECUADO: 'badge-adecuado',
  EN_RIESGO: 'badge-riesgo'
};

const EVALUATION_QUESTIONS = [
  {
    field: 'guidanceClarity',
    text: 'El profesor proporciono instrucciones y objetivos claros para el desarrollo de mis actividades.'
  },
  {
    field: 'roleExpectations',
    text: 'Las expectativas del profesor sobre mi rol y responsabilidades estuvieron bien definidas desde el inicio.'
  },
  {
    field: 'availabilityDisposition',
    text: 'El profesor mostro una disposicion constante para atenderme cuando necesite resolver dudas o problemas.'
  },
  {
    field: 'supportTimeliness',
    text: 'El acompanamiento brindado por el profesor fue suficiente y oportuno durante todo el semestre.'
  },
  {
    field: 'feedbackConstructive',
    text: 'La retroalimentacion que recibi sobre mi trabajo fue constructiva y me ayudo a mejorar.'
  },
  {
    field: 'feedbackFairness',
    text: 'El profesor evaluo mi desempeno de manera justa y basada en los criterios acordados.'
  },
  {
    field: 'respectfulTreatment',
    text: 'El trato del profesor hacia mi fue siempre respetuoso, profesional y cordial.'
  },
  {
    field: 'trustEnvironment',
    text: 'El profesor fomento un ambiente de confianza que me permitio expresar mis ideas o dificultades.'
  }
];

function EvaluarSupervisorHU021() {
  const monitorIdentifier = localStorage.getItem('userId');
  const token = localStorage.getItem('token');

  const [assignments, setAssignments] = useState([]);
  const [loadingAssignments, setLoadingAssignments] = useState(true);
  const [searchTerm, setSearchTerm] = useState('');
  const [selectedAssignment, setSelectedAssignment] = useState(null);
  const [saving, setSaving] = useState(false);
  const [saveFeedback, setSaveFeedback] = useState('');

  const [formValues, setFormValues] = useState({
    guidanceClarity: 4,
    roleExpectations: 4,
    availabilityDisposition: 4,
    supportTimeliness: 4,
    feedbackConstructive: 4,
    feedbackFairness: 4,
    respectfulTreatment: 4,
    trustEnvironment: 4,
    strengthsComments: '',
    improvementComments: ''
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

  const fetchAssignments = async () => {
    if (!monitorIdentifier) {
      showMessage('No se pudo identificar al monitor autenticado.');
      return;
    }
    setLoadingAssignments(true);
    try {
      const response = await fetch(`${BACKEND_URL}/supervisor-evaluations/monitor/${monitorIdentifier}/assignments`, {
        method: 'GET',
        headers: {
          'Content-Type': 'application/json',
          Authorization: token
        }
      });

      if (!response.ok) {
        const errorBody = await response.json().catch(() => ({}));
        throw new Error(errorBody.error || 'No fue posible obtener las monitorías asignadas.');
      }

      const data = await response.json();
      setAssignments(Array.isArray(data) ? data : []);
    } catch (error) {
      console.error('Error cargando asignaciones:', error);
      showMessage(error.message);
    } finally {
      setLoadingAssignments(false);
    }
  };

  useEffect(() => {
    fetchAssignments();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const filteredAssignments = useMemo(() => {
    if (!searchTerm.trim()) {
      return assignments;
    }
    const term = searchTerm.trim().toLowerCase();
    return assignments.filter((assignment) => {
      return (
        assignment.monitoringName?.toLowerCase().includes(term) ||
        assignment.courseName?.toLowerCase().includes(term) ||
        assignment.programName?.toLowerCase().includes(term) ||
        assignment.professorName?.toLowerCase().includes(term) ||
        assignment.professorId?.toLowerCase().includes(term)
      );
    });
  }, [assignments, searchTerm]);

  const pendingAssignments = useMemo(
    () => filteredAssignments.filter((item) => !item.evaluated),
    [filteredAssignments]
  );

  const completedAssignments = useMemo(
    () => filteredAssignments.filter((item) => item.evaluated),
    [filteredAssignments]
  );

  const handleSelectAssignment = (assignment) => {
    setSelectedAssignment(assignment);
    setSaveFeedback('');
    setFormValues({
      guidanceClarity: 4,
      roleExpectations: 4,
      availabilityDisposition: 4,
      supportTimeliness: 4,
      feedbackConstructive: 4,
      feedbackFairness: 4,
      respectfulTreatment: 4,
      trustEnvironment: 4,
      strengthsComments: '',
      improvementComments: ''
    });
  };

  const updateScore = (field, value) => {
    setFormValues((prev) => ({
      ...prev,
      [field]: Number(value)
    }));
  };

  const formattedAverage = useMemo(() => {
    const {
      guidanceClarity,
      roleExpectations,
      availabilityDisposition,
      supportTimeliness,
      feedbackConstructive,
      feedbackFairness,
      respectfulTreatment,
      trustEnvironment
    } = formValues;
    const average = (
      guidanceClarity + roleExpectations + availabilityDisposition + supportTimeliness +
      feedbackConstructive + feedbackFairness + respectfulTreatment + trustEnvironment
    ) / 8;
    return average.toFixed(2);
  }, [formValues]);

  const performanceLevel = useMemo(() => {
    const total = parseFloat(formattedAverage);
    if (total >= 6.0) return 'EXCELENTE';
    if (total >= 5.0) return 'DESTACADO';
    if (total >= 4.0) return 'ADECUADO';
    return 'EN_RIESGO';
  }, [formattedAverage]);

  const resetForm = () => {
    setSelectedAssignment(null);
    setSaveFeedback('');
    setFormValues({
      guidanceClarity: 4,
      roleExpectations: 4,
      availabilityDisposition: 4,
      supportTimeliness: 4,
      feedbackConstructive: 4,
      feedbackFairness: 4,
      respectfulTreatment: 4,
      trustEnvironment: 4,
      strengthsComments: '',
      improvementComments: ''
    });
  };

  const handleSubmit = async (event) => {
    event.preventDefault();
    setSaveFeedback('');
    if (!selectedAssignment) {
      showMessage('Selecciona una monitoría para evaluar al supervisor.');
      return;
    }
    if (selectedAssignment.evaluated) {
      showMessage('Ya enviaste una evaluación para esta monitoría.');
      return;
    }

    const payload = {
      monitorIdentifier,
      monitoringId: selectedAssignment.monitoringId,
      guidanceClarity: formValues.guidanceClarity,
      roleExpectations: formValues.roleExpectations,
      availabilityDisposition: formValues.availabilityDisposition,
      supportTimeliness: formValues.supportTimeliness,
      feedbackConstructive: formValues.feedbackConstructive,
      feedbackFairness: formValues.feedbackFairness,
      respectfulTreatment: formValues.respectfulTreatment,
      trustEnvironment: formValues.trustEnvironment,
      strengthsComments: formValues.strengthsComments,
      improvementComments: formValues.improvementComments
    };

    setSaving(true);
    try {
      const response = await fetch(`${BACKEND_URL}/supervisor-evaluations`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          Authorization: token
        },
        body: JSON.stringify(payload)
      });

      const body = await response.json().catch(() => ({}));
      if (!response.ok) {
        throw new Error(body.error || 'No se pudo enviar la evaluación.');
      }

      showMessage('¡Gracias! Tu evaluación fue enviada correctamente.');
      setSaveFeedback('Evaluación guardada correctamente.');
      setSelectedAssignment((prev) => (prev ? {
        ...prev,
        evaluated: true,
        status: 'Enviada',
        submittedAt: new Date().toISOString()
      } : prev));
      fetchAssignments();
    } catch (error) {
      console.error('Error guardando evaluación:', error);
      showMessage(error.message);
    } finally {
      setSaving(false);
    }
  };

  const renderAssignmentItem = (assignment) => {
    const isSelected = selectedAssignment && selectedAssignment.monitoringId === assignment.monitoringId;
    return (
      <button
        key={`${assignment.monitoringId}`}
        className={`assignment-card ${isSelected ? 'selected' : ''}`}
        onClick={() => handleSelectAssignment(assignment)}
      >
        <div className="assignment-header">
          <h4>{assignment.professorName || 'Profesor sin nombre'}</h4>
          <span className={`badge ${assignment.evaluated ? 'badge-excelente' : 'badge-adecuado'}`}>
            {assignment.status || (assignment.evaluated ? 'Enviada' : 'Pendiente')}
          </span>
        </div>
        <p className="assignment-subtitle">{assignment.monitoringName || 'Monitoría sin nombre'}</p>
        <p className="assignment-meta">{assignment.courseName || 'Curso no asignado'} · {assignment.semester || 'Semestre sin registrar'}</p>
        {assignment.evaluated && assignment.submittedAt && (
          <div className="assignment-score">
            <strong>Enviada</strong>
            <span>{new Date(assignment.submittedAt).toLocaleDateString()}</span>
          </div>
        )}
      </button>
    );
  };

  const renderScoreQuestion = ({ field, text }) => {
    const selectedValue = formValues[field];

    const handleScoreKeyDown = (event, currentIndex) => {
      if (selectedAssignment.evaluated) {
        return;
      }

      let nextIndex = currentIndex;

      if (event.key === 'ArrowRight' || event.key === 'ArrowDown') {
        nextIndex = (currentIndex + 1) % SCORE_OPTIONS.length;
      } else if (event.key === 'ArrowLeft' || event.key === 'ArrowUp') {
        nextIndex = (currentIndex - 1 + SCORE_OPTIONS.length) % SCORE_OPTIONS.length;
      } else if (event.key === 'Home') {
        nextIndex = 0;
      } else if (event.key === 'End') {
        nextIndex = SCORE_OPTIONS.length - 1;
      } else {
        return;
      }

      event.preventDefault();
      updateScore(field, SCORE_OPTIONS[nextIndex]);
    };

    const selectedIndex = Math.max(0, SCORE_OPTIONS.indexOf(selectedValue));
    const labelId = `${field}-label`;

    return (
      <div className="score-question" key={field}>
        <span className="question-text" id={labelId}>
          {text}
          <span className="required-asterisk">*</span>
        </span>

        <div className="score-options" role="radiogroup" aria-labelledby={labelId}>
          {SCORE_OPTIONS.map((option, index) => (
            <button
              type="button"
              key={`${field}-${option}`}
              className={`score-pill ${selectedValue === option ? 'active' : ''}`}
              onClick={() => updateScore(field, option)}
              onKeyDown={(event) => handleScoreKeyDown(event, index)}
              disabled={selectedAssignment.evaluated}
              role="radio"
              aria-checked={selectedValue === option}
              tabIndex={index === selectedIndex ? 0 : -1}
            >
              {option}
            </button>
          ))}
        </div>

        <div className="score-legend">
          <span>1 = Bajo</span>
          <span>7 = Alto</span>
        </div>
      </div>
    );
  };

  return (
    <div className="evaluar-monitores-layout evaluar-supervisor-hu021">
      <VerticalNavbar />
      <PopUp show={isOpen} onClose={closePopup}>
        {message}
      </PopUp>
      <div className="evaluar-monitores-content">
        <section className="assignments-panel">
          <header className="panel-header">
            <h2>Evaluación de tu profesor supervisor</h2>
            <p className="panel-description">
              Califica la supervisión recibida en tus monitorías. La escala es de 1 a 7, donde 1 es "Totalmente en desacuerdo" y 7 "Totalmente de acuerdo". Tu evaluación es confidencial y será revisada por coordinación.
            </p>
            <input
              type="search"
              placeholder="Buscar por profesor, curso o monitoría"
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
                <h3>Pendientes</h3>
                {pendingAssignments.length === 0 ? (
                  <p className="empty-state">No hay evaluaciones pendientes.</p>
                ) : (
                  <div className="cards-grid">
                    {pendingAssignments.map(renderAssignmentItem)}
                  </div>
                )}
              </div>

              <div className="assignments-group">
                <h3>Enviadas</h3>
                {completedAssignments.length === 0 ? (
                  <p className="empty-state">Aún no has enviado evaluaciones.</p>
                ) : (
                  <div className="cards-grid">
                    {completedAssignments.map(renderAssignmentItem)}
                  </div>
                )}
              </div>
            </>
          )}
        </section>

        <section className="form-panel">
          {!selectedAssignment ? (
            <div className="placeholder-panel">
              <h3>Selecciona una monitoría</h3>
              <p>Elige la monitoría para evaluar la experiencia con tu profesor supervisor.</p>
            </div>
          ) : (
            <form className="evaluation-form" onSubmit={handleSubmit}>
              <header className="form-header">
                <div>
                  <h3>{selectedAssignment.professorName || 'Profesor supervisor'}</h3>
                  <p>{selectedAssignment.monitoringName}</p>
                  <span className="form-meta">{selectedAssignment.courseName} · {selectedAssignment.semester}</span>
                </div>
                <div className={`impact-badge ${PERFORMANCE_CLASSES[performanceLevel]}`}>
                  <span>{PERFORMANCE_LABELS[performanceLevel]}</span>
                  <strong>{formattedAverage}</strong>
                </div>
              </header>

              <div className="scores-grid scores-grid--single">
                {EVALUATION_QUESTIONS.map(renderScoreQuestion)}
              </div>

              <label className="comments-field">
                Que aspectos destacaria de la supervision del profesor? (Opcional)
                <textarea
                  value={formValues.strengthsComments}
                  onChange={(event) => setFormValues((prev) => ({ ...prev, strengthsComments: event.target.value }))}
                  placeholder="Ej: claridad en los objetivos, seguimiento oportuno, apoyo constante."
                  disabled={selectedAssignment.evaluated}
                />
              </label>

              <label className="comments-field">
                Que sugerencias le darias al profesor para mejorar la experiencia de futuros monitores? (Opcional)
                <textarea
                  value={formValues.improvementComments}
                  onChange={(event) => setFormValues((prev) => ({ ...prev, improvementComments: event.target.value }))}
                  placeholder="Ej: mas espacios de retroalimentacion o reuniones periodicas."
                  disabled={selectedAssignment.evaluated}
                />
              </label>

              {selectedAssignment.evaluated && (
                <div className="visibility-row">
                  <span className="penalty-alert">Ya enviaste esta evaluación. Si necesitas ajustes, contacta a coordinación.</span>
                </div>
              )}

              {saveFeedback && <p className="submit-feedback submit-feedback--success">{saveFeedback}</p>}

              <button type="submit" className="submit-button" disabled={saving || selectedAssignment.evaluated}>
                {saving ? 'Enviando…' : 'Enviar evaluación'}
              </button>
            </form>
          )}
        </section>
      </div>
    </div>
  );
}

export default EvaluarSupervisorHU021;

