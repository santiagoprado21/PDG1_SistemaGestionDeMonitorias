import React, { useEffect, useMemo, useState } from 'react';
import VerticalNavbar from './VerticalNavbar';
import LoadingSpinner from './LoadingSpinner';
import { PopUp } from './PopUp';
import { BACKEND_URL } from './config/ApiBackend';
import './EvaluarMonitoresHU015.css';
import './EvaluarSupervisorHU021.css';

const SCORE_OPTIONS = [1, 2, 3, 4, 5, 6, 7];
const PERIOD_REGEX = /^\d{4}-[12]$/;

const normalizeValidPeriod = (value) => {
  const normalized = (value || '').trim();
  return PERIOD_REGEX.test(normalized) ? normalized : '';
};

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

const normalizeCourseLabel = (value) => (value || '')
  .toLowerCase()
  .replace(/[·:\-–—|]/g, ' ')
  .replace(/\s+/g, ' ')
  .trim();

const isDuplicateCourseLabel = (monitoringName, courseName, semester) => {
  if (!monitoringName) {
    return false;
  }
  const courseLine = [courseName, semester].filter(Boolean).join(' ');
  if (!courseLine) {
    return false;
  }
  return normalizeCourseLabel(monitoringName) === normalizeCourseLabel(courseLine);
};

const buildDefaultScores = (questions) => {
  const defaults = {};
  questions.forEach((question) => {
    defaults[question.id] = 4;
  });
  return defaults;
};

function EvaluarSupervisorHU021() {
  const monitorIdentifier = localStorage.getItem('userId');
  const token = localStorage.getItem('token');

  const [assignments, setAssignments] = useState([]);
  const [loadingAssignments, setLoadingAssignments] = useState(true);
  const [searchTerm, setSearchTerm] = useState('');
  const [selectedAssignment, setSelectedAssignment] = useState(null);
  const [surveyQuestions, setSurveyQuestions] = useState([]);
  const [loadingSurveyQuestions, setLoadingSurveyQuestions] = useState(false);
  const [questionScores, setQuestionScores] = useState({});
  const [strengthsComments, setStrengthsComments] = useState('');
  const [improvementComments, setImprovementComments] = useState('');
  const [saving, setSaving] = useState(false);
  const [saveFeedback, setSaveFeedback] = useState('');

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

  const fetchSurveyQuestions = async (semester) => {
    const validSemester = normalizeValidPeriod(semester);
    const semesterQuery = validSemester ? `?semester=${encodeURIComponent(validSemester)}` : '';
    setLoadingSurveyQuestions(true);
    try {
      const response = await fetch(`${BACKEND_URL}/professor-survey/current-config${semesterQuery}`, {
        method: 'GET',
        headers: {
          'Content-Type': 'application/json',
          Authorization: token
        }
      });

      const body = await response.json().catch(() => ({}));
      if (!response.ok) {
        throw new Error(body.error || 'No fue posible cargar las preguntas de evaluación.');
      }

      const questions = Array.isArray(body.questions)
        ? body.questions.slice().sort((a, b) => (a.displayOrder || 0) - (b.displayOrder || 0))
        : [];

      setSurveyQuestions(questions);
      setQuestionScores(buildDefaultScores(questions));

      if (questions.length === 0) {
        showMessage('No hay preguntas activas configuradas para este periodo.');
      }
    } catch (error) {
      setSurveyQuestions([]);
      setQuestionScores({});
      console.error('Error cargando preguntas HU027:', error);
      showMessage(error.message);
    } finally {
      setLoadingSurveyQuestions(false);
    }
  };

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
    setStrengthsComments('');
    setImprovementComments('');
    fetchSurveyQuestions(assignment?.semester);
  };

  const updateScore = (questionId, value) => {
    setQuestionScores((prev) => ({
      ...prev,
      [questionId]: Number(value)
    }));
  };

  const formattedAverage = useMemo(() => {
    if (surveyQuestions.length === 0) {
      return '0.00';
    }

    const total = surveyQuestions.reduce((sum, question) => {
      const score = Number(questionScores[question.id] || 4);
      return sum + score;
    }, 0);

    const average = total / surveyQuestions.length;
    return average.toFixed(2);
  }, [surveyQuestions, questionScores]);

  const performanceLevel = useMemo(() => {
    const total = parseFloat(formattedAverage);
    if (total >= 6.0) return 'EXCELENTE';
    if (total >= 5.0) return 'DESTACADO';
    if (total >= 4.0) return 'ADECUADO';
    return 'EN_RIESGO';
  }, [formattedAverage]);

  const selectedAssignmentPeriod = normalizeValidPeriod(selectedAssignment?.semester);
  const selectedAssignmentPeriodLabel = selectedAssignmentPeriod || 'Periodo sin registrar';

  const handleSubmit = async (event) => {
    event.preventDefault();
    setSaveFeedback('');
    if (!selectedAssignment) {
      showMessage('Selecciona una monitoría para evaluar al profesor.');
      return;
    }
    if (selectedAssignment.evaluated) {
      showMessage('Ya enviaste una evaluación para esta monitoría.');
      return;
    }

    if (surveyQuestions.length === 0) {
      showMessage('No hay preguntas activas para evaluar este periodo.');
      return;
    }

    const answers = surveyQuestions.map((question) => ({
      questionId: question.id,
      score: Number(questionScores[question.id] || 4)
    }));

    const payload = {
      monitorIdentifier,
      monitoringId: selectedAssignment.monitoringId,
      strengthsComments,
      improvementComments,
      answers
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
    const period = normalizeValidPeriod(assignment.semester);
    const hasDuplicateCourse = isDuplicateCourseLabel(assignment.monitoringName, assignment.courseName, period);

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
        {!hasDuplicateCourse && (
          <p className="assignment-subtitle">{assignment.monitoringName || 'Monitoría sin nombre'}</p>
        )}
        <p className="assignment-meta">{assignment.courseName || 'Curso no asignado'} · {period || 'Periodo sin registrar'}</p>
        {assignment.evaluated && assignment.submittedAt && (
          <div className="assignment-score">
            <strong>Enviada</strong>
            <span>{new Date(assignment.submittedAt).toLocaleDateString()}</span>
          </div>
        )}
      </button>
    );
  };

  const renderScoreQuestion = (question) => {
    const selectedValue = Number(questionScores[question.id] || 4);

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
      updateScore(question.id, SCORE_OPTIONS[nextIndex]);
    };

    const selectedIndex = Math.max(0, SCORE_OPTIONS.indexOf(selectedValue));
    const labelId = `question-${question.id}-label`;

    return (
      <div className="score-question" key={question.id}>
        <span className="question-text" id={labelId}>
          {question.statement}
          <span className="required-asterisk">*</span>
        </span>

        <div className="score-options" role="radiogroup" aria-labelledby={labelId}>
          {SCORE_OPTIONS.map((option, index) => (
            <button
              type="button"
              key={`${question.id}-${option}`}
              className={`score-pill ${selectedValue === option ? 'active' : ''}`}
              onClick={() => updateScore(question.id, option)}
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
            <h2>Evaluación de tu profesor</h2>
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
              <p>Elige la monitoría para evaluar la experiencia con tu profesor.</p>
            </div>
          ) : (
            <form className="evaluation-form" onSubmit={handleSubmit}>
              <header className="form-header">
                <div>
                  <h3>{selectedAssignment.professorName || 'Profesor'}</h3>
                  {selectedAssignment.monitoringName && !isDuplicateCourseLabel(selectedAssignment.monitoringName, selectedAssignment.courseName, selectedAssignmentPeriod) && (
                    <p>{selectedAssignment.monitoringName}</p>
                  )}
                  <span className="form-meta">{selectedAssignment.courseName} · {selectedAssignmentPeriodLabel}</span>
                </div>
                <div className={`impact-badge ${PERFORMANCE_CLASSES[performanceLevel]}`}>
                  <span>{PERFORMANCE_LABELS[performanceLevel]}</span>
                  <strong>{formattedAverage}</strong>
                </div>
              </header>

              {loadingSurveyQuestions ? (
                <div className="loading-wrapper">
                  <LoadingSpinner message="Cargando preguntas activas" />
                </div>
              ) : surveyQuestions.length === 0 ? (
                <p className="empty-state">No hay preguntas configuradas para este periodo.</p>
              ) : (
                <div className="scores-grid scores-grid--single">
                  {surveyQuestions.map(renderScoreQuestion)}
                </div>
              )}

              <label className="comments-field">
                Que aspectos destacaria de la supervision del profesor? (Opcional)
                <textarea
                  value={strengthsComments}
                  onChange={(event) => setStrengthsComments(event.target.value)}
                  placeholder="Ej: claridad en los objetivos, seguimiento oportuno, apoyo constante."
                  disabled={selectedAssignment.evaluated}
                />
              </label>

              <label className="comments-field">
                Que sugerencias le darias al profesor para mejorar la experiencia de futuros monitores? (Opcional)
                <textarea
                  value={improvementComments}
                  onChange={(event) => setImprovementComments(event.target.value)}
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

              <button
                type="submit"
                className="submit-button"
                disabled={saving || selectedAssignment.evaluated || loadingSurveyQuestions || surveyQuestions.length === 0}
              >
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

