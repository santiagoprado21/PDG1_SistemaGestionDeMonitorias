import React, { useEffect, useState } from 'react';
import VerticalNavbar from './VerticalNavbar';
import LoadingSpinner from './LoadingSpinner';
import { PopUp } from './PopUp';
import { BACKEND_URL } from './config/ApiBackend';
import './MisEvaluaciones.css';

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

function MisEvaluacionesHU015() {
  const role = localStorage.getItem('role') || 'monitor';
  const userIdentifier = localStorage.getItem('userId');
  const isProfessor = role === 'professor';
  const token = localStorage.getItem('token');

  const [evaluations, setEvaluations] = useState([]);
  const [loading, setLoading] = useState(true);
  const [acknowledging, setAcknowledging] = useState(false);

  const [isOpen, setIsOpen] = useState(false);
  const [message, setMessage] = useState('');

  const showMessage = (text) => {
    setMessage(text);
    setIsOpen(true);
  };

  const closePopup = () => setIsOpen(false);

  const fetchEvaluations = async () => {
    if (!userIdentifier) {
      showMessage(
        isProfessor
          ? 'No se pudo identificar al profesor autenticado.'
          : 'No se pudo identificar al monitor autenticado.'
      );
      setLoading(false);
      return;
    }
    setLoading(true);
    try {
      const endpoint = isProfessor
        ? `${BACKEND_URL}/monitor-evaluations/professor/${userIdentifier}/assignments`
        : `${BACKEND_URL}/monitor-evaluations/monitor/${userIdentifier}`;

      const response = await fetch(endpoint, {
        method: 'GET',
        headers: {
          'Content-Type': 'application/json',
          Authorization: token
        }
      });

      if (!response.ok) {
        const body = await response.json().catch(() => ({}));
        throw new Error(body.error || 'No fue posible obtener tus evaluaciones.');
      }

      const data = await response.json();
      if (!Array.isArray(data)) {
        setEvaluations([]);
        return;
      }

      if (isProfessor) {
        const professorHistory = data
          .filter((item) => item.evaluated)
          .map((item) => ({
            evaluationId: item.evaluationId || `${item.monitoringId}-${item.monitorCode}`,
            monitoringName: item.monitoringName,
            courseName: item.courseName,
            semester: item.semester,
            professorId: item.professorId || userIdentifier,
            totalScore: Number(item.totalScore || 0),
            performanceLevel: item.performanceLevel,
            taskCompliance: item.taskCompliance,
            timelyCommunication: item.timelyCommunication,
            planFulfillment: item.planFulfillment,
            attitude: item.attitude,
            comments: item.comments,
            penaltyFlag: item.penaltyFlag ?? Number(item.totalScore || 0) < 3,
            monitorFullName: item.monitorFullName,
            monitorCode: item.monitorCode,
            visibleToMonitor: item.visibleToMonitor
          }));

        setEvaluations(professorHistory);
        return;
      }

      setEvaluations(data);
    } catch (error) {
      console.error(
        isProfessor
          ? 'Error cargando historial de evaluaciones del profesor:'
          : 'Error cargando evaluaciones del monitor:',
        error
      );
      showMessage(error.message);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchEvaluations();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const acknowledgeEvaluation = async (evaluationId) => {
    if (isProfessor) {
      return;
    }
    setAcknowledging(true);
    try {
      const response = await fetch(`${BACKEND_URL}/monitor-evaluations/${evaluationId}/acknowledge`, {
        method: 'PATCH',
        headers: {
          'Content-Type': 'application/json',
          Authorization: token
        },
        body: JSON.stringify({ monitorIdentifier: userIdentifier })
      });

      const body = await response.json().catch(() => ({}));
      if (!response.ok) {
        throw new Error(body.error || 'No se pudo registrar la lectura de la evaluación.');
      }

      showMessage('¡Gracias! Registramos que revisaste la evaluación.');
      fetchEvaluations();
    } catch (error) {
      console.error('Error al reconocer evaluación:', error);
      showMessage(error.message);
    } finally {
      setAcknowledging(false);
    }
  };

  const renderEvaluationCard = (evaluation) => {
    const badgeClass = PERFORMANCE_CLASSES[evaluation.performanceLevel] || 'badge-adecuado';
    const acknowledged = evaluation.acknowledgedByMonitor;

    return (
      <article className="evaluation-card" key={evaluation.evaluationId}>
        <header className="evaluation-card__header">
          <div>
            <h3>{evaluation.monitoringName || 'Monitoría sin nombre'}</h3>
            <p>{evaluation.courseName || 'Curso no asignado'} · {evaluation.semester || 'Periodo sin registrar'}</p>
            {isProfessor ? (
              <span className="evaluation-card__meta">
                Monitor evaluado: {evaluation.monitorFullName || 'Sin nombre'} {evaluation.monitorCode ? `(${evaluation.monitorCode})` : ''}
              </span>
            ) : (
              <span className="evaluation-card__meta">Profesor responsable: {evaluation.professorId || 'No disponible'}</span>
            )}
          </div>
          <div className={`impact-badge ${badgeClass}`}>
            <span>{PERFORMANCE_LABELS[evaluation.performanceLevel] || 'Sin definir'}</span>
            <strong>{evaluation.totalScore?.toFixed(2)}</strong>
          </div>
        </header>

        <section className="evaluation-card__scores">
          <div>
            <span className="label">Cumplimiento de tareas</span>
            <strong>{evaluation.taskCompliance}</strong>
          </div>
          <div>
            <span className="label">Comunicación oportuna</span>
            <strong>{evaluation.timelyCommunication}</strong>
          </div>
          <div>
            <span className="label">Cumplimiento del plan</span>
            <strong>{evaluation.planFulfillment}</strong>
          </div>
          <div>
            <span className="label">Actitud y servicio</span>
            <strong>{evaluation.attitude}</strong>
          </div>
        </section>

        <section className="evaluation-card__comments">
          <h4>{isProfessor ? 'Comentarios registrados' : 'Comentarios del profesor'}</h4>
          <p>{evaluation.comments || 'Sin comentarios adicionales.'}</p>
        </section>

        <footer className="evaluation-card__footer">
          {isProfessor ? (
            <div className="ack-row">
              <span className={`badge ${evaluation.visibleToMonitor ? 'badge-ack' : 'badge-pending'}`}>
                {evaluation.visibleToMonitor ? 'Visible para monitor' : 'Visible solo para profesor'}
              </span>
            </div>
          ) : (
            <>
              <div className="ack-row">
                <span className={`badge ${acknowledged ? 'badge-ack' : 'badge-pending'}`}>
                  {acknowledged ? 'Retroalimentación revisada' : 'Pendiente por revisar'}
                </span>
                {!acknowledged && (
                  <button
                    type="button"
                    className="ack-button"
                    onClick={() => acknowledgeEvaluation(evaluation.evaluationId)}
                    disabled={acknowledging}
                  >
                    {acknowledging ? 'Registrando…' : 'Marcar como revisada'}
                  </button>
                )}
              </div>
              {evaluation.penaltyFlag && (
                <span className="penalty-flag">⚠ Esta evaluación reporta un puntaje bajo. Te recomendamos conversar con tu profesor.</span>
              )}
            </>
          )}
        </footer>
      </article>
    );
  };

  return (
    <div className="mis-evaluaciones-layout">
      <VerticalNavbar />
      <PopUp show={isOpen} onClose={closePopup}>
        {message}
      </PopUp>
      <div className="mis-evaluaciones-content">
        <header className="mis-evaluaciones-header prof-page-header">
          <h2 className="prof-page-title">{isProfessor ? 'Historial de evaluaciones registradas' : 'Mis evaluaciones de desempeño'}</h2>
          <p className="prof-page-subtitle">
            {isProfessor
              ? 'Consulta las evaluaciones que has registrado a tus monitores. Esta vista mantiene el mismo diseño para facilitar la consulta histórica.'
              : 'Consulta la retroalimentación que han registrado tus profesores. Estas evaluaciones refuerzan la transparencia y te ayudarán a seguir creciendo.'}
          </p>
        </header>

        {loading ? (
          <div className="loading-wrapper">
            <LoadingSpinner message="Cargando tus evaluaciones" />
          </div>
        ) : evaluations.length === 0 ? (
          <div className="empty-state">
            <h3>No se encontraron evaluaciones</h3>
            <p>
              {isProfessor
                ? 'Cuando registres evaluaciones de monitores, aparecerán en este historial.'
                : 'Cuando un profesor registre tu desempeño, podrás consultarlo aquí.'}
            </p>
          </div>
        ) : (
          <div className="evaluations-grid">
            {evaluations.map(renderEvaluationCard)}
          </div>
        )}
      </div>
    </div>
  );
}

export default MisEvaluacionesHU015;
