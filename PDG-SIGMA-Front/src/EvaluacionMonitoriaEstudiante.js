import React, { useEffect, useMemo, useState } from 'react';
import { Link2, Copy, Check, ShieldCheck, Share2 } from 'lucide-react';
import VerticalNavbar from './VerticalNavbar';
import { PopUp } from './PopUp';
import { BACKEND_URL } from './config/ApiBackend';
import { generateAcademicPeriodOptions, getCurrentAcademicPeriod, isSelectableAcademicPeriod } from './globalFix';
import './EvaluacionMonitoriaEstudiante.css';

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
    statement: 'Recomendaria a este monitor para futuros periodos.',
    category: 'Percepcion de Valor',
    displayOrder: 9
  }
];

const buildInitialScores = (questions) => {
  const scores = {};
  questions.forEach((question) => {
    scores[String(question.id)] = 4;
  });
  return scores;
};

function EvaluacionMonitoriaEstudiante() {
  const role = localStorage.getItem('role');
  const token = localStorage.getItem('token');
  const academicPeriodOptions = useMemo(() => generateAcademicPeriodOptions(), []);
  const currentAcademicPeriod = useMemo(() => getCurrentAcademicPeriod(), []);
  const [monitoringId, setMonitoringId] = useState('');
  const [monitorCode, setMonitorCode] = useState('');
  const [monitorName, setMonitorName] = useState('');
  const [semester, setSemester] = useState(currentAcademicPeriod);
  const [hasExplicitSemester, setHasExplicitSemester] = useState(false);
  const [lockIdentifiers, setLockIdentifiers] = useState(false);
  const [surveyQuestions, setSurveyQuestions] = useState(FALLBACK_QUESTIONS);
  const [questionScores, setQuestionScores] = useState(buildInitialScores(FALLBACK_QUESTIONS));
  const [positiveFeedback, setPositiveFeedback] = useState('');
  const [improvementFeedback, setImprovementFeedback] = useState('');
  const [saving, setSaving] = useState(false);
  const [usingFallbackQuestions, setUsingFallbackQuestions] = useState(false);
  const [surveyLoadMessage, setSurveyLoadMessage] = useState('');

  const [isOpen, setIsOpen] = useState(false);
  const [message, setMessage] = useState('');
  const [linkCopied, setLinkCopied] = useState(false);
  const [reportFilters, setReportFilters] = useState({
    semester: currentAcademicPeriod,
    monitorCode: '',
    monitoringId: ''
  });
  const [reportData, setReportData] = useState(null);
  const [reportLoading, setReportLoading] = useState(false);
  const [reportError, setReportError] = useState('');
  const [exportingReport, setExportingReport] = useState(false);

  useEffect(() => {
    const params = new URLSearchParams(window.location.search);
    const monitoringParam = params.get('monitoringId');
    const monitorCodeParam = params.get('monitorCode');
    const monitorNameParam = params.get('monitorName');
    const semesterParam = params.get('semester');

    if (monitoringParam) setMonitoringId(monitoringParam);
    if (monitorCodeParam) {
      setMonitorCode(monitorCodeParam);
    } else if (role === 'monitor') {
      setMonitorCode(localStorage.getItem('userId') || '');
    }
    if (monitorNameParam) setMonitorName(monitorNameParam);
    if (semesterParam && isSelectableAcademicPeriod(semesterParam)) {
      setSemester(semesterParam);
      setHasExplicitSemester(true);
    }
    if (monitoringParam || monitorCodeParam || monitorNameParam) {
      setLockIdentifiers(true);
    }
  }, [role]);

  useEffect(() => {
    const loadSurveyQuestions = async () => {
      try {
        const response = await fetch(`${BACKEND_URL}/monitor-survey/public/current-config`, {
          cache: 'no-store'
        });
        const body = await response.json().catch(() => ({}));
        if (!response.ok) {
          throw new Error(body.error || 'No se pudo cargar la encuesta activa');
        }

        const activeQuestions = Array.isArray(body.questions) ? body.questions : [];
        if (body.semester && isSelectableAcademicPeriod(body.semester)) {
          setSemester(body.semester);
        }

        if (activeQuestions.length === 0) {
          setSurveyQuestions(FALLBACK_QUESTIONS);
          setQuestionScores(buildInitialScores(FALLBACK_QUESTIONS));
          setUsingFallbackQuestions(true);
          setSurveyLoadMessage('No se encontró una configuración activa; se usa la plantilla base.');
          return;
        }

        const sortedQuestions = activeQuestions
          .slice()
          .sort((a, b) => (a.displayOrder || 0) - (b.displayOrder || 0));
        setSurveyQuestions(sortedQuestions);
        setQuestionScores(buildInitialScores(sortedQuestions));
        setUsingFallbackQuestions(false);
        setSurveyLoadMessage('');
      } catch (error) {
        setSurveyQuestions(FALLBACK_QUESTIONS);
        setQuestionScores(buildInitialScores(FALLBACK_QUESTIONS));
        setUsingFallbackQuestions(true);
        setSurveyLoadMessage(`No se pudo cargar la configuración activa (${error.message || 'sin detalle'}); se usa la plantilla base.`);
      }
    };

    loadSurveyQuestions();
  }, [semester, hasExplicitSemester]);

  const showMessage = (text) => {
    setMessage(text);
    setIsOpen(true);
  };

  const closePopup = () => {
    setIsOpen(false);
  };

  const authHeaders = token ? { Authorization: token } : {};

  const handleSemesterChange = (event) => {
    setSemester(event.target.value);
    setHasExplicitSemester(true);
  };

  const handleReportFilterChange = (field) => (event) => {
    setReportFilters((prev) => ({ ...prev, [field]: event.target.value }));
  };

  const buildReportQuery = () => {
    const params = new URLSearchParams();
    if (reportFilters.semester) params.set('semester', reportFilters.semester);
    if (reportFilters.monitorCode.trim()) params.set('monitorCode', reportFilters.monitorCode.trim());
    if (reportFilters.monitoringId.trim()) params.set('monitoringId', reportFilters.monitoringId.trim());
    return params.toString();
  };

  const fetchReport = async () => {
    if (!token) {
      setReportError('Debes iniciar sesión para consultar los resultados.');
      return;
    }

    setReportLoading(true);
    setReportError('');
    try {
      const query = buildReportQuery();
      const response = await fetch(
        `${BACKEND_URL}/monitor-survey/admin/report${query ? `?${query}` : ''}`,
        { headers: authHeaders }
      );
      const body = await response.json().catch(() => ({}));
      if (!response.ok) {
        throw new Error(body.error || 'No se pudo cargar el reporte');
      }
      setReportData(body);
    } catch (error) {
      setReportError(error.message || 'No se pudo cargar el reporte');
    } finally {
      setReportLoading(false);
    }
  };

  const handleExportReport = async () => {
    if (!token) {
      showMessage('Debes iniciar sesión para exportar el reporte.');
      return;
    }

    setExportingReport(true);
    try {
      const query = buildReportQuery();
      const response = await fetch(
        `${BACKEND_URL}/monitor-survey/admin/report/csv${query ? `?${query}` : ''}`,
        { headers: authHeaders }
      );
      if (!response.ok) {
        const body = await response.json().catch(() => ({}));
        throw new Error(body.error || 'No se pudo exportar el reporte');
      }
      const blob = await response.blob();
      const url = window.URL.createObjectURL(blob);
      const link = document.createElement('a');
      const suffix = reportFilters.semester ? reportFilters.semester : 'todos';
      link.href = url;
      link.download = `resultados_monitorias_${suffix}.csv`;
      document.body.appendChild(link);
      link.click();
      link.remove();
      window.URL.revokeObjectURL(url);
    } catch (error) {
      showMessage(error.message || 'No se pudo exportar el reporte');
    } finally {
      setExportingReport(false);
    }
  };

  useEffect(() => {
    if (role === 'jfedpto') {
      fetchReport();
    }
  }, [role]);

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
      return 'Debes indicar el periodo de la evaluación.';
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

    setSaving(true);
    try {
      // Persistencia en backend para habilitar reglas de edición del banco por periodo.
      await fetch(`${BACKEND_URL}/monitor-survey/public/responses`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json'
        },
        body: JSON.stringify({
          semester: semester.trim(),
          monitoringId: monitoringId.trim(),
          monitorCode: monitorCode.trim(),
          monitorName: monitorName.trim(),
          answers: answers.map((item) => ({ questionId: item.questionId, score: item.score })),
          positiveFeedback,
          improvementFeedback,
          averageScore: Number(averageScore)
        })
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
    if (hasExplicitSemester && semester.trim()) params.set('semester', semester.trim());
    if (monitoringId.trim()) params.set('monitoringId', monitoringId.trim());
    if (monitorCode.trim()) params.set('monitorCode', monitorCode.trim());
    if (monitorName.trim()) params.set('monitorName', monitorName.trim());
    const query = params.toString();
    return query ? `${baseUrl}?${query}` : baseUrl;
  }, [semester, hasExplicitSemester, monitoringId, monitorCode, monitorName]);

  const showForm = role === 'student' || !role;

  const handleCopyLink = async () => {
    try {
      await navigator.clipboard.writeText(shareLink);
      setLinkCopied(true);
      setTimeout(() => setLinkCopied(false), 2500);
    } catch (error) {
      showMessage('No se pudo copiar el enlace. Copialo manualmente.');
    }
  };

  const formatReportDate = (value) => {
    if (!value) return '—';
    const date = new Date(value);
    if (Number.isNaN(date.getTime())) return value;
    return date.toLocaleString('es-CO');
  };

  const reportQuestionStats = reportData?.questionStats || [];
  const reportResponses = reportData?.responses || [];

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
                  <p className="monitoria-instructions">{surveyLoadMessage || 'No se encontró una configuración activa; se usa la plantilla base.'}</p>
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
                  Periodo
                  <select
                    value={semester}
                    onChange={handleSemesterChange}
                    required
                    disabled={lockIdentifiers && hasExplicitSemester}
                  >
                    {academicPeriodOptions.map((period) => (
                      <option key={period} value={period}>{period}</option>
                    ))}
                  </select>
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
          <section className="monitor-section">
            <header className="monitor-page-header app-page-header">
              <div className="monitor-page-header-left">
                <div>
                  <h2 className="app-page-title">Evaluacion de mi Monitoria</h2>
                  <p className="app-page-subtitle">Genera y comparte el enlace de evaluacion con tus estudiantes</p>
                </div>
              </div>
              <div className="monitor-id-badge">
                <span className="monitor-id-label">Monitor</span>
                <span className="monitor-id-value">{localStorage.getItem('userId') || '—'}</span>
              </div>
            </header>

            <div className="monitor-content-grid">
              <div className="monitor-card monitor-share-card">
                <div className="monitor-card-title-row">
                  <div className="monitor-card-icon-wrap">
                    <Share2 size={20} />
                  </div>
                  <div>
                    <h3>Generar enlace de evaluacion</h3>
                    <p>Completa los datos para personalizar el enlace</p>
                  </div>
                </div>

                <div className="monitor-fields-grid">
                  <label className="monitor-field">
                    <span>Codigo de la monitoria <span className="monitor-req">*</span></span>
                    <input
                      type="text"
                      value={monitoringId}
                      onChange={(event) => setMonitoringId(event.target.value)}
                      placeholder="Ej: 1024"
                    />
                  </label>
                  <label className="monitor-field">
                    <span>Codigo del monitor</span>
                    <input
                      type="text"
                      value={monitorCode}
                      onChange={(event) => setMonitorCode(event.target.value)}
                      placeholder="Ej: 2220001"
                    />
                  </label>
                  <label className="monitor-field monitor-field--full">
                    <span>Tu nombre (opcional)</span>
                    <input
                      type="text"
                      value={monitorName}
                      onChange={(event) => setMonitorName(event.target.value)}
                      placeholder="Nombre completo"
                    />
                  </label>
                </div>

                <div className="monitor-divider" />

                <div className="monitor-link-group">
                  <p className="monitor-link-label">
                    <Link2 size={14} />
                    Enlace generado para compartir
                  </p>
                  <div className="monitor-link-row">
                    <div className="monitor-link-display">
                      <span className="monitor-link-text">{shareLink}</span>
                    </div>
                    <button
                      type="button"
                      className={`monitor-copy-btn ${linkCopied ? 'monitor-copy-btn--copied' : ''}`}
                      onClick={handleCopyLink}
                    >
                      {linkCopied ? (
                        <><Check size={15} /> Copiado</>
                      ) : (
                        <><Copy size={15} /> Copiar</>
                      )}
                    </button>
                  </div>
                  <p className="monitor-link-hint">
                    El enlace pre-completa tus datos para que los estudiantes solo califiquen.
                  </p>
                </div>
              </div>

              <div className="monitor-right-col">
                <div className="monitor-card monitor-steps-card">
                  <h3 className="monitor-steps-title">Como funciona</h3>
                  <ol className="monitor-steps-list">
                    <li>
                      <span className="monitor-step-num">1</span>
                      <span>Ingresa el codigo de tu monitoria activa.</span>
                    </li>
                    <li>
                      <span className="monitor-step-num">2</span>
                      <span>Copia el enlace generado y envialo a tus estudiantes.</span>
                    </li>
                    <li>
                      <span className="monitor-step-num">3</span>
                      <span>Los estudiantes completan la encuesta de forma anonima.</span>
                    </li>
                    <li>
                      <span className="monitor-step-num">4</span>
                      <span>Los resultados son revisados por el jefe de departamento.</span>
                    </li>
                  </ol>
                </div>

                <div className="monitor-card monitor-privacy-card">
                  <div className="monitor-privacy-icon">
                    <ShieldCheck size={22} />
                  </div>
                  <div className="monitor-privacy-body">
                    <h4>Respuestas anonimas</h4>
                    <p>
                      Los estudiantes evaluan de forma completamente anonima.
                      No podras identificar quien envio cada respuesta.
                    </p>
                  </div>
                </div>
              </div>
            </div>
          </section>
        )}

        {role === 'jfedpto' && (
          <section className="monitoria-dashboard-panel">
            <header className="monitoria-dashboard-header app-page-header">
              <h2 className="app-page-title">Resultados de encuestas</h2>
              <p className="app-page-subtitle">Explora el consolidado interno y exporta los datos cuando lo necesites.</p>
            </header>

            <div className="monitoria-dashboard-grid">
              <div className="monitoria-report-panel">
                <h3>Filtros y resumen</h3>
                <p className="monitoria-report-hint">
                  Ajusta los filtros para revisar periodos, monitorias o monitores específicos.
                </p>
                <div className="monitoria-report-filters">
                  <label>
                    Periodo
                    <select
                      value={reportFilters.semester}
                      onChange={handleReportFilterChange('semester')}
                    >
                      <option value="">Todos</option>
                      {academicPeriodOptions.map((period) => (
                        <option key={period} value={period}>{period}</option>
                      ))}
                    </select>
                  </label>
                  <label>
                    Codigo de monitoria
                    <input
                      type="text"
                      value={reportFilters.monitoringId}
                      onChange={handleReportFilterChange('monitoringId')}
                      placeholder="Ej: 1024"
                    />
                  </label>
                  <label>
                    Codigo del monitor
                    <input
                      type="text"
                      value={reportFilters.monitorCode}
                      onChange={handleReportFilterChange('monitorCode')}
                      placeholder="Ej: M-045"
                    />
                  </label>
                </div>
                <div className="monitoria-report-actions">
                  <button
                    type="button"
                    className="monitoria-primary"
                    onClick={fetchReport}
                    disabled={reportLoading}
                  >
                    {reportLoading ? 'Actualizando...' : 'Actualizar'}
                  </button>
                  <button
                    type="button"
                    className="monitoria-secondary"
                    onClick={handleExportReport}
                    disabled={exportingReport || reportLoading}
                  >
                    {exportingReport ? 'Exportando...' : 'Exportar CSV'}
                  </button>
                </div>
                {reportError && (
                  <p className="monitoria-report-error">{reportError}</p>
                )}
                <div className="monitoria-report-summary">
                  <div className="monitoria-summary-card">
                    <span>Respuestas</span>
                    <strong>{reportData?.totalResponses ?? 0}</strong>
                  </div>
                  <div className="monitoria-summary-card">
                    <span>Promedio general</span>
                    <strong>{(reportData?.averageScore ?? 0).toFixed(2)}</strong>
                  </div>
                  <div className="monitoria-summary-card">
                    <span>Respuestas a preguntas</span>
                    <strong>{reportData?.totalAnswers ?? 0}</strong>
                  </div>
                </div>
              </div>

              <div className="monitoria-dashboard-card">
                <div className="monitoria-report-section">
                  <div className="monitoria-report-section-header">
                    <h3>Resultados por pregunta</h3>
                    <p>Promedios y dispersion por cada enunciado activo.</p>
                  </div>
                  {reportLoading ? (
                    <div className="monitoria-placeholder">
                      <p>Cargando resultados...</p>
                    </div>
                  ) : reportQuestionStats.length === 0 ? (
                    <div className="monitoria-placeholder">
                      <p>No hay respuestas para los filtros seleccionados.</p>
                    </div>
                  ) : (
                    <div className="monitoria-report-table-wrap">
                      <table className="monitoria-report-table">
                        <thead>
                          <tr>
                            <th>Pregunta</th>
                            <th>Categoria</th>
                            <th>Promedio</th>
                            <th>Respuestas</th>
                            <th>Min</th>
                            <th>Max</th>
                          </tr>
                        </thead>
                        <tbody>
                          {reportQuestionStats.map((item) => (
                            <tr key={item.questionId}>
                              <td>{item.statement}</td>
                              <td>{item.category}</td>
                              <td>{Number(item.averageScore || 0).toFixed(2)}</td>
                              <td>{item.responsesCount}</td>
                              <td>{item.minScore}</td>
                              <td>{item.maxScore}</td>
                            </tr>
                          ))}
                        </tbody>
                      </table>
                    </div>
                  )}
                </div>

                <div className="monitoria-report-section">
                  <div className="monitoria-report-section-header">
                    <h3>Respuestas registradas</h3>
                    <p>Lista base para seguimiento y auditoria.</p>
                  </div>
                  {reportLoading ? (
                    <div className="monitoria-placeholder">
                      <p>Cargando respuestas...</p>
                    </div>
                  ) : reportResponses.length === 0 ? (
                    <div className="monitoria-placeholder">
                      <p>Sin respuestas disponibles.</p>
                    </div>
                  ) : (
                    <div className="monitoria-report-table-wrap">
                      <table className="monitoria-report-table">
                        <thead>
                          <tr>
                            <th>Fecha</th>
                            <th>Periodo</th>
                            <th>Monitoria</th>
                            <th>Monitor</th>
                            <th>Promedio</th>
                          </tr>
                        </thead>
                        <tbody>
                          {reportResponses.map((item) => (
                            <tr key={item.responseId}>
                              <td>{formatReportDate(item.createdAt)}</td>
                              <td>{item.semester}</td>
                              <td>{item.monitoringId || '—'}</td>
                              <td>{item.monitorName || item.monitorCode || '—'}</td>
                              <td>{Number(item.averageScore || 0).toFixed(2)}</td>
                            </tr>
                          ))}
                        </tbody>
                      </table>
                    </div>
                  )}
                </div>
              </div>
            </div>
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
