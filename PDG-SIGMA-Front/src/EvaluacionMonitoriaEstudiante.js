import React, { useEffect, useMemo, useState } from 'react';
import VerticalNavbar from './VerticalNavbar';
import { PopUp } from './PopUp';
import { BACKEND_URL } from './config/ApiBackend';
import './EvaluacionMonitoriaEstudiante.css';

const APPS_SCRIPT_URL = 'https://script.google.com/macros/s/AKfycbxZ6-xGZk9S0pQ-RjxTWShR362EGiI_l4TqeXGUt1F_ZjoPfgJe0vD9DGQCV69I9Rh_Bg/exec';
const DASHBOARD_URL = 'https://docs.google.com/spreadsheets/d/1xMZBNO-msHyZUHAy2GMBuSDgwAlgz_rX0EsFHzREwA4/edit?usp=sharing';

const SCORE_OPTIONS = [1, 2, 3, 4, 5, 6, 7];

const FALLBACK_QUESTIONS = [
  {
    id: 1,
    questionKey: 'topic_mastery',
    statement: 'El monitor demostro dominio de los temas tratados.',
    category: 'Apoyo Pedagogico',
    displayOrder: 1
  },
  {
    id: 2,
    questionKey: 'explanation_clarity',
    statement: 'Las explicaciones del monitor fueron claras y utiles.',
    category: 'Apoyo Pedagogico',
    displayOrder: 2
  },
  {
    id: 3,
    questionKey: 'doubt_resolution',
    statement: 'El monitor resolvio mis dudas de manera efectiva.',
    category: 'Apoyo Pedagogico',
    displayOrder: 3
  },
  {
    id: 4,
    questionKey: 'schedule_compliance',
    statement: 'El monitor cumplio con los horarios establecidos.',
    category: 'Disponibilidad y Puntualidad',
    displayOrder: 4
  },
  {
    id: 5,
    questionKey: 'availability',
    statement: 'Fue facil contactar al monitor y asistir a sus sesiones.',
    category: 'Disponibilidad y Puntualidad',
    displayOrder: 5
  },
  {
    id: 6,
    questionKey: 'respectful_attitude',
    statement: 'El monitor tuvo una actitud respetuosa y paciente.',
    category: 'Actitud y Metodologia',
    displayOrder: 6
  },
  {
    id: 7,
    questionKey: 'learning_resources',
    statement: 'El monitor uso recursos o ejemplos utiles.',
    category: 'Actitud y Metodologia',
    displayOrder: 7
  },
  {
    id: 8,
    questionKey: 'perceived_value',
    statement: 'El apoyo del monitor fue fundamental para mi desempeno.',
    category: 'Percepcion de Valor',
    displayOrder: 8
  },
  {
    id: 9,
    questionKey: 'recommendation',
    statement: 'Recomendaria a este monitor para futuros semestres.',
    category: 'Percepcion de Valor',
    displayOrder: 9
  }
];

const LEGACY_APPS_SCRIPT_MAP = {
  topic_mastery: 'topicMastery',
  explanation_clarity: 'explanationClarity',
  doubt_resolution: 'doubtResolution',
  schedule_compliance: 'scheduleCompliance',
  availability: 'availability',
  respectful_attitude: 'respectfulAttitude',
  learning_resources: 'learningResources',
  perceived_value: 'perceivedValue',
  recommendation: 'recommendation'
};

const buildInitialScores = (questions) => {
  const scores = {};
  questions.forEach((question) => {
    scores[String(question.id)] = 4;
  });
  return scores;
};

const getDefaultSemester = () => {
  const now = new Date();
  const month = now.getMonth() + 1;
  const period = month <= 6 ? 1 : 2;
  return `${now.getFullYear()}-${period}`;
};

function EvaluacionMonitoriaEstudiante() {
  const role = localStorage.getItem('role');
  const [monitoringId, setMonitoringId] = useState('');
  const [monitorCode, setMonitorCode] = useState('');
  const [monitorName, setMonitorName] = useState('');
  const [semester, setSemester] = useState(getDefaultSemester());
  const [lockIdentifiers, setLockIdentifiers] = useState(false);
  const [surveyQuestions, setSurveyQuestions] = useState(FALLBACK_QUESTIONS);
  const [questionScores, setQuestionScores] = useState(buildInitialScores(FALLBACK_QUESTIONS));
  const [positiveFeedback, setPositiveFeedback] = useState('');
  const [improvementFeedback, setImprovementFeedback] = useState('');
  const [saving, setSaving] = useState(false);
  const [usingFallbackQuestions, setUsingFallbackQuestions] = useState(false);

  const [isOpen, setIsOpen] = useState(false);
  const [message, setMessage] = useState('');

  useEffect(() => {
    const params = new URLSearchParams(window.location.search);
    const monitoringParam = params.get('monitoringId');
    const monitorCodeParam = params.get('monitorCode');
    const monitorNameParam = params.get('monitorName');
    const semesterParam = params.get('semester');

    if (monitoringParam) setMonitoringId(monitoringParam);
    if (monitorCodeParam) setMonitorCode(monitorCodeParam);
    if (monitorNameParam) setMonitorName(monitorNameParam);
    if (semesterParam) setSemester(semesterParam);
    if (monitoringParam || monitorCodeParam || monitorNameParam) {
      setLockIdentifiers(true);
    }
  }, []);

  useEffect(() => {
    const loadSurveyQuestions = async () => {
      try {
        const query = semester ? `?semester=${encodeURIComponent(semester)}` : '';
        const response = await fetch(`${BACKEND_URL}/monitor-survey/public/questions${query}`);
        const body = await response.json().catch(() => []);
        if (!response.ok) {
          throw new Error(body.error || 'No se pudo cargar la encuesta activa');
        }

        if (!Array.isArray(body) || body.length === 0) {
          setSurveyQuestions(FALLBACK_QUESTIONS);
          setQuestionScores(buildInitialScores(FALLBACK_QUESTIONS));
          setUsingFallbackQuestions(true);
          return;
        }

        const sortedQuestions = body
          .slice()
          .sort((a, b) => (a.displayOrder || 0) - (b.displayOrder || 0));
        setSurveyQuestions(sortedQuestions);
        setQuestionScores(buildInitialScores(sortedQuestions));
        setUsingFallbackQuestions(false);
      } catch (error) {
        setSurveyQuestions(FALLBACK_QUESTIONS);
        setQuestionScores(buildInitialScores(FALLBACK_QUESTIONS));
        setUsingFallbackQuestions(true);
      }
    };

    loadSurveyQuestions();
  }, [semester]);

  const showMessage = (text) => {
    setMessage(text);
    setIsOpen(true);
  };

  const closePopup = () => {
    setIsOpen(false);
  };

  const handleScoreChange = (questionId, value) => {
    setQuestionScores((prev) => ({
      ...prev,
      [String(questionId)]: Number(value)
    }));
  };

  const handleScoreKeyDown = (event, questionId, currentValue) => {
    const currentIndex = SCORE_OPTIONS.indexOf(Number(currentValue));
    if (event.key === 'ArrowRight' || event.key === 'ArrowUp') {
      event.preventDefault();
      const nextIndex = Math.min(SCORE_OPTIONS.length - 1, currentIndex + 1);
      handleScoreChange(questionId, SCORE_OPTIONS[nextIndex]);
    }
    if (event.key === 'ArrowLeft' || event.key === 'ArrowDown') {
      event.preventDefault();
      const prevIndex = Math.max(0, currentIndex - 1);
      handleScoreChange(questionId, SCORE_OPTIONS[prevIndex]);
    }
    if (event.key === 'Home') {
      event.preventDefault();
      handleScoreChange(questionId, SCORE_OPTIONS[0]);
    }
    if (event.key === 'End') {
      event.preventDefault();
      handleScoreChange(questionId, SCORE_OPTIONS[SCORE_OPTIONS.length - 1]);
    }
  };

  const groupedQuestions = useMemo(() => {
    return surveyQuestions.reduce((acc, question) => {
      const category = question.category || 'General';
      if (!acc[category]) {
        acc[category] = [];
      }
      acc[category].push(question);
      return acc;
    }, {});
  }, [surveyQuestions]);

  const averageScore = useMemo(() => {
    const values = surveyQuestions.map((question) => Number(questionScores[String(question.id)]) || 0);
    if (values.length === 0) return '0.00';
    const total = values.reduce((sum, value) => sum + (Number(value) || 0), 0);
    return (total / values.length).toFixed(2);
  }, [surveyQuestions, questionScores]);

  const validateForm = () => {
    if (!semester.trim()) {
      return 'Debes indicar el semestre de la evaluación.';
    }
    if (!monitoringId.trim()) {
      return 'Debes ingresar el codigo de la monitoria.';
    }
    if (!monitorCode.trim()) {
      return 'Debes ingresar el codigo del monitor.';
    }
    for (const question of surveyQuestions) {
      const score = Number(questionScores[String(question.id)]);
      if (!score) {
        return `Debes calificar: ${question.statement}`;
      }
      if (score < 1 || score > 7) {
        return `La calificacion para "${question.statement}" debe estar entre 1 y 7.`;
      }
    }
    return null;
  };

  const resetForm = () => {
    setQuestionScores(buildInitialScores(surveyQuestions));
    setPositiveFeedback('');
    setImprovementFeedback('');
  };

  const handleSubmit = async (event) => {
    event.preventDefault();

    if (APPS_SCRIPT_URL === 'REPLACE_WITH_APPS_SCRIPT_URL') {
      showMessage('Falta configurar el URL del Apps Script.');
      return;
    }

    const validationError = validateForm();
    if (validationError) {
      showMessage(validationError);
      return;
    }

    const answers = surveyQuestions.map((question) => ({
      questionId: question.id,
      questionKey: question.questionKey,
      statement: question.statement,
      category: question.category,
      score: Number(questionScores[String(question.id)])
    }));

    const legacyPayload = {};
    answers.forEach((answer) => {
      const legacyKey = LEGACY_APPS_SCRIPT_MAP[answer.questionKey];
      if (legacyKey) {
        legacyPayload[legacyKey] = answer.score;
      }
    });

    const payload = {
      semester: semester.trim(),
      monitoringId: monitoringId.trim(),
      monitorCode: monitorCode.trim(),
      monitorName: monitorName.trim(),
      answers,
      positiveFeedback,
      improvementFeedback,
      averageScore,
      submittedAt: new Date().toISOString(),
      ...legacyPayload
    };

    setSaving(true);
    try {
      // Persistencia en backend para habilitar reglas de edición del banco por semestre.
      await fetch(`${BACKEND_URL}/monitor-survey/public/responses`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json'
        },
        body: JSON.stringify({
          semester: payload.semester,
          monitoringId: payload.monitoringId,
          monitorCode: payload.monitorCode,
          monitorName: payload.monitorName,
          answers: answers.map((item) => ({ questionId: item.questionId, score: item.score })),
          positiveFeedback: payload.positiveFeedback,
          improvementFeedback: payload.improvementFeedback,
          averageScore: Number(payload.averageScore)
        })
      });

      await fetch(APPS_SCRIPT_URL, {
        method: 'POST',
        mode: 'no-cors',
        headers: {
          'Content-Type': 'application/json'
        },
        body: JSON.stringify(payload)
      });

      showMessage('Gracias. Tu evaluacion fue registrada.');
      resetForm();
    } catch (error) {
      console.error('Error enviando evaluacion:', error);
      showMessage(error.message);
    } finally {
      setSaving(false);
    }
  };

  const shareLink = useMemo(() => {
    const baseUrl = `${window.location.origin}/evaluacion-monitoria`;
    const params = new URLSearchParams();
    if (semester.trim()) params.set('semester', semester.trim());
    if (monitoringId.trim()) params.set('monitoringId', monitoringId.trim());
    if (monitorCode.trim()) params.set('monitorCode', monitorCode.trim());
    if (monitorName.trim()) params.set('monitorName', monitorName.trim());
    const query = params.toString();
    return query ? `${baseUrl}?${query}` : baseUrl;
  }, [semester, monitoringId, monitorCode, monitorName]);

  const showForm = role === 'student' || !role;

  const handleCopyLink = async () => {
    try {
      await navigator.clipboard.writeText(shareLink);
      showMessage('Enlace copiado al portapapeles.');
    } catch (error) {
      showMessage('No se pudo copiar el enlace. Copialo manualmente.');
    }
  };

  return (
    <div className="monitoria-eval-layout">
      <VerticalNavbar />
      <PopUp show={isOpen} onClose={closePopup}>
        {message}
      </PopUp>
      <div className="monitoria-eval-content">
        {showForm && (
          <section className="monitoria-form-panel evaluacion-container">
            <header className="monitoria-header">
              <div>
                <h2>Encuesta de experiencia con monitores</h2>
                <p>Califica tu experiencia y comparte comentarios anonimos.</p>
                {usingFallbackQuestions && (
                  <p className="monitoria-instructions">No se encontro una configuracion activa, se usa la plantilla base.</p>
                )}
                <p className="monitoria-instructions">
                  Instrucciones: Califique de 1 a 7 las siguientes afirmaciones, donde 1 es "Totalmente en desacuerdo" y 7 es "Totalmente de acuerdo".
                </p>
              </div>
              <div className="monitoria-average">
                <span>Promedio</span>
                <strong>{averageScore}</strong>
              </div>
            </header>

            <form className="monitoria-form" onSubmit={handleSubmit}>
              <div className="monitoria-meta-grid">
                <label>
                  Semestre
                  <input
                    type="text"
                    value={semester}
                    onChange={(event) => setSemester(event.target.value)}
                    placeholder="Ej: 2026-1"
                    required
                    readOnly={lockIdentifiers}
                  />
                </label>
                <label>
                  Codigo de monitoria
                  <input
                    type="text"
                    value={monitoringId}
                    onChange={(event) => setMonitoringId(event.target.value)}
                    placeholder="Ej: 1024"
                    required
                    readOnly={lockIdentifiers}
                  />
                </label>
                <label>
                  Codigo del monitor
                  <input
                    type="text"
                    value={monitorCode}
                    onChange={(event) => setMonitorCode(event.target.value)}
                    placeholder="Ej: M-045"
                    required
                    readOnly={lockIdentifiers}
                  />
                </label>
                <label>
                  Nombre del monitor (opcional)
                  <input
                    type="text"
                    value={monitorName}
                    onChange={(event) => setMonitorName(event.target.value)}
                    placeholder="Nombre completo"
                    readOnly={lockIdentifiers}
                  />
                </label>
              </div>

              {Object.entries(groupedQuestions).map(([category, questions]) => (
                <div key={category} className="monitoria-section">
                  <h3>{category}</h3>
                  {questions.map((question) => (
                    <div key={question.id} className="monitoria-question">
                      <label>{question.statement}</label>
                      <div className="monitoria-scale" role="radiogroup" aria-label={question.statement}>
                        {SCORE_OPTIONS.map((value) => (
                          <button
                            key={value}
                            type="button"
                            className={`monitoria-scale-chip ${Number(questionScores[String(question.id)]) === value ? 'is-selected' : ''}`}
                            onClick={() => handleScoreChange(question.id, value)}
                            onKeyDown={(event) => handleScoreKeyDown(event, question.id, questionScores[String(question.id)])}
                            disabled={saving}
                            role="radio"
                            tabIndex={Number(questionScores[String(question.id)]) === value ? 0 : -1}
                            aria-checked={Number(questionScores[String(question.id)]) === value}
                          >
                            {value}
                          </button>
                        ))}
                      </div>
                      <div className="scale-hint" aria-hidden="true">
                        <span>1 = Bajo</span>
                        <span>7 = Alto</span>
                      </div>
                    </div>
                  ))}
                </div>
              ))}

              <div className="monitoria-section">
                <h3>Comentarios sobre la calidad de la monitoria</h3>
                <label className="monitoria-text-label">
                  Que fue lo que mas te gusto de las monitorias recibidas?
                  <textarea
                    value={positiveFeedback}
                    onChange={(event) => setPositiveFeedback(event.target.value)}
                    placeholder="Escribe tu comentario..."
                    disabled={saving}
                  />
                </label>
                <label className="monitoria-text-label">
                  Que aspectos consideras que el monitor deberia mejorar?
                  <textarea
                    value={improvementFeedback}
                    onChange={(event) => setImprovementFeedback(event.target.value)}
                    placeholder="Escribe tu comentario..."
                    disabled={saving}
                  />
                </label>
              </div>

              <div className="monitoria-actions">
                <button type="button" className="monitoria-secondary" onClick={resetForm} disabled={saving}>
                  Limpiar
                </button>
                <button type="submit" className="monitoria-primary" disabled={saving}>
                  {saving ? 'Enviando...' : 'Enviar evaluacion'}
                </button>
              </div>
            </form>
          </section>
        )}

        {role === 'monitor' && (
          <section className="monitoria-share-panel">
            <h2>Compartir encuesta</h2>
            <p>Completa los datos y genera un enlace para los estudiantes.</p>
            <div className="monitoria-meta-grid">
              <label>
                Semestre
                <input
                  type="text"
                  value={semester}
                  onChange={(event) => setSemester(event.target.value)}
                  placeholder="Ej: 2026-1"
                />
              </label>
              <label>
                Codigo de monitoria
                <input
                  type="text"
                  value={monitoringId}
                  onChange={(event) => setMonitoringId(event.target.value)}
                  placeholder="Ej: 1024"
                />
              </label>
              <label>
                Codigo del monitor
                <input
                  type="text"
                  value={monitorCode}
                  onChange={(event) => setMonitorCode(event.target.value)}
                  placeholder="Ej: M-045"
                />
              </label>
              <label>
                Nombre del monitor (opcional)
                <input
                  type="text"
                  value={monitorName}
                  onChange={(event) => setMonitorName(event.target.value)}
                  placeholder="Nombre completo"
                />
              </label>
            </div>
            <div className="monitoria-share-box">
              <input type="text" value={shareLink} readOnly />
              <button type="button" onClick={handleCopyLink}>
                Copiar enlace
              </button>
            </div>
            <p className="monitoria-share-note">
              Comparte este enlace por correo para que los estudiantes respondan la encuesta.
            </p>
          </section>
        )}

        {role === 'jfedpto' && (
          <section className="monitoria-dashboard-panel">
            <h2>Resultados de encuestas</h2>
            <p>Visualiza los resultados agregados de la monitoria.</p>
            {DASHBOARD_URL === 'REPLACE_WITH_DASHBOARD_URL' ? (
              <div className="monitoria-placeholder">
                <p>Configura el URL del dashboard o Google Sheet para mostrar los resultados.</p>
              </div>
            ) : (
              <iframe
                title="Resultados de monitoria"
                src={DASHBOARD_URL}
                className="monitoria-dashboard"
                loading="lazy"
              />
            )}
          </section>
        )}

        {(role !== 'monitor' && role !== 'student' && role !== 'professor' && role !== 'jfedpto') && (
          <section className="monitoria-loading">
            <p>Acceso anonimo: completa la encuesta y envia tu evaluacion.</p>
          </section>
        )}
      </div>
    </div>
  );
}

export default EvaluacionMonitoriaEstudiante;
