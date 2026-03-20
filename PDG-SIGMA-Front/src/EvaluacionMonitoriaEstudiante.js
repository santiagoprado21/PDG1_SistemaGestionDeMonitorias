import React, { useEffect, useMemo, useState } from 'react';
import VerticalNavbar from './VerticalNavbar';
import { PopUp } from './PopUp';
import './EvaluacionMonitoriaEstudiante.css';

const APPS_SCRIPT_URL = 'https://script.google.com/macros/s/AKfycbxZ6-xGZk9S0pQ-RjxTWShR362EGiI_l4TqeXGUt1F_ZjoPfgJe0vD9DGQCV69I9Rh_Bg/exec';
const DASHBOARD_URL = 'https://docs.google.com/spreadsheets/d/1xMZBNO-msHyZUHAy2GMBuSDgwAlgz_rX0EsFHzREwA4/edit?usp=sharing';

const SCORE_OPTIONS = [1, 2, 3, 4, 5, 6, 7];

const QUESTION_GROUPS = [
  {
    title: 'Efectividad del apoyo pedagogico',
    items: [
      { key: 'topicMastery', label: 'El monitor demostro dominio de los temas tratados.' },
      { key: 'explanationClarity', label: 'Las explicaciones del monitor fueron claras y utiles.' },
      { key: 'doubtResolution', label: 'El monitor resolvio mis dudas de manera efectiva.' }
    ]
  },
  {
    title: 'Disponibilidad y puntualidad',
    items: [
      { key: 'scheduleCompliance', label: 'El monitor cumplio con los horarios establecidos.' },
      { key: 'availability', label: 'Fue facil contactar al monitor y asistir a sus sesiones.' }
    ]
  },
  {
    title: 'Actitud y metodologia',
    items: [
      { key: 'respectfulAttitude', label: 'El monitor tuvo una actitud respetuosa y paciente.' },
      { key: 'learningResources', label: 'El monitor uso recursos o ejemplos utiles.' }
    ]
  },
  {
    title: 'Percepcion de valor',
    items: [
      { key: 'perceivedValue', label: 'El apoyo del monitor fue fundamental para mi desempeno.' },
      { key: 'recommendation', label: 'Recomendaria a este monitor para futuros semestres.' }
    ]
  }
];

const INITIAL_FORM = {
  topicMastery: 4,
  explanationClarity: 4,
  doubtResolution: 4,
  scheduleCompliance: 4,
  availability: 4,
  respectfulAttitude: 4,
  learningResources: 4,
  perceivedValue: 4,
  recommendation: 4,
  positiveFeedback: '',
  improvementFeedback: ''
};

function EvaluacionMonitoriaEstudiante() {
  const role = localStorage.getItem('role');
  const [monitoringId, setMonitoringId] = useState('');
  const [monitorCode, setMonitorCode] = useState('');
  const [monitorName, setMonitorName] = useState('');
  const [lockIdentifiers, setLockIdentifiers] = useState(false);
  const [formValues, setFormValues] = useState(INITIAL_FORM);
  const [saving, setSaving] = useState(false);

  const [isOpen, setIsOpen] = useState(false);
  const [message, setMessage] = useState('');

  useEffect(() => {
    const params = new URLSearchParams(window.location.search);
    const monitoringParam = params.get('monitoringId');
    const monitorCodeParam = params.get('monitorCode');
    const monitorNameParam = params.get('monitorName');

    if (monitoringParam) setMonitoringId(monitoringParam);
    if (monitorCodeParam) setMonitorCode(monitorCodeParam);
    if (monitorNameParam) setMonitorName(monitorNameParam);
    if (monitoringParam || monitorCodeParam || monitorNameParam) {
      setLockIdentifiers(true);
    }
  }, []);

  const showMessage = (text) => {
    setMessage(text);
    setIsOpen(true);
  };

  const closePopup = () => {
    setIsOpen(false);
  };

  const handleScoreChange = (field, value) => {
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
      handleScoreChange(field, SCORE_OPTIONS[nextIndex]);
    }
    if (event.key === 'ArrowLeft' || event.key === 'ArrowDown') {
      event.preventDefault();
      const prevIndex = Math.max(0, currentIndex - 1);
      handleScoreChange(field, SCORE_OPTIONS[prevIndex]);
    }
    if (event.key === 'Home') {
      event.preventDefault();
      handleScoreChange(field, SCORE_OPTIONS[0]);
    }
    if (event.key === 'End') {
      event.preventDefault();
      handleScoreChange(field, SCORE_OPTIONS[SCORE_OPTIONS.length - 1]);
    }
  };

  const handleCommentChange = (field, value) => {
    setFormValues((prev) => ({
      ...prev,
      [field]: value
    }));
  };

  const averageScore = useMemo(() => {
    const values = QUESTION_GROUPS.flatMap((group) => group.items.map((item) => formValues[item.key]));
    if (values.length === 0) return '0.00';
    const total = values.reduce((sum, value) => sum + (Number(value) || 0), 0);
    return (total / values.length).toFixed(2);
  }, [formValues]);

  const validateForm = () => {
    if (!monitoringId.trim()) {
      return 'Debes ingresar el codigo de la monitoria.';
    }
    if (!monitorCode.trim()) {
      return 'Debes ingresar el codigo del monitor.';
    }
    for (const group of QUESTION_GROUPS) {
      for (const item of group.items) {
        const value = formValues[item.key];
        if (value === null || value === undefined) {
          return `Debes calificar: ${item.label}`;
        }
        if (Number(value) < 1 || Number(value) > 7) {
          return `La calificacion para "${item.label}" debe estar entre 1 y 7.`;
        }
      }
    }
    return null;
  };

  const resetForm = () => {
    setFormValues(INITIAL_FORM);
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

    const payload = {
      monitoringId: monitoringId.trim(),
      monitorCode: monitorCode.trim(),
      monitorName: monitorName.trim(),
      topicMastery: formValues.topicMastery,
      explanationClarity: formValues.explanationClarity,
      doubtResolution: formValues.doubtResolution,
      scheduleCompliance: formValues.scheduleCompliance,
      availability: formValues.availability,
      respectfulAttitude: formValues.respectfulAttitude,
      learningResources: formValues.learningResources,
      perceivedValue: formValues.perceivedValue,
      recommendation: formValues.recommendation,
      positiveFeedback: formValues.positiveFeedback,
      improvementFeedback: formValues.improvementFeedback,
      averageScore,
      submittedAt: new Date().toISOString()
    };

    setSaving(true);
    try {
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
    if (monitoringId.trim()) params.set('monitoringId', monitoringId.trim());
    if (monitorCode.trim()) params.set('monitorCode', monitorCode.trim());
    if (monitorName.trim()) params.set('monitorName', monitorName.trim());
    const query = params.toString();
    return query ? `${baseUrl}?${query}` : baseUrl;
  }, [monitoringId, monitorCode, monitorName]);

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

              {QUESTION_GROUPS.map((group) => (
                <div key={group.title} className="monitoria-section">
                  <h3>{group.title}</h3>
                  {group.items.map((item) => (
                    <div key={item.key} className="monitoria-question">
                      <label>{item.label}</label>
                      <div className="monitoria-scale" role="radiogroup" aria-label={item.label}>
                        {SCORE_OPTIONS.map((value) => (
                          <button
                            key={value}
                            type="button"
                            className={`monitoria-scale-chip ${Number(formValues[item.key]) === value ? 'is-selected' : ''}`}
                            onClick={() => handleScoreChange(item.key, value)}
                            onKeyDown={(event) => handleScoreKeyDown(event, item.key, formValues[item.key])}
                            disabled={saving}
                            role="radio"
                            tabIndex={Number(formValues[item.key]) === value ? 0 : -1}
                            aria-checked={Number(formValues[item.key]) === value}
                            aria-pressed={Number(formValues[item.key]) === value}
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
                    value={formValues.positiveFeedback}
                    onChange={(event) => handleCommentChange('positiveFeedback', event.target.value)}
                    placeholder="Escribe tu comentario..."
                    disabled={saving}
                  />
                </label>
                <label className="monitoria-text-label">
                  Que aspectos consideras que el monitor deberia mejorar?
                  <textarea
                    value={formValues.improvementFeedback}
                    onChange={(event) => handleCommentChange('improvementFeedback', event.target.value)}
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
