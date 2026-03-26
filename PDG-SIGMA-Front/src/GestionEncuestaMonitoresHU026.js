import React, { useEffect, useMemo, useState } from 'react';
import VerticalNavbar from './VerticalNavbar';
import LoadingSpinner from './LoadingSpinner';
import { PopUp } from './PopUp';
import { BACKEND_URL } from './config/ApiBackend';
import './GestionEncuestaMonitoresHU026.css';

function GestionEncuestaMonitoresHU026() {
  const token = localStorage.getItem('token');
  const [semester, setSemester] = useState('');
  const [loading, setLoading] = useState(true);
  const [savingConfig, setSavingConfig] = useState(false);
  const [creatingQuestion, setCreatingQuestion] = useState(false);
  const [creatingTemplate, setCreatingTemplate] = useState(false);

  const [questions, setQuestions] = useState([]);
  const [selectedQuestionIds, setSelectedQuestionIds] = useState([]);
  const [templates, setTemplates] = useState([]);

  const [newQuestion, setNewQuestion] = useState({ statement: '', category: '' });
  const [editingQuestionId, setEditingQuestionId] = useState(null);
  const [editingQuestion, setEditingQuestion] = useState({ statement: '', category: '' });

  const [templateForm, setTemplateForm] = useState({ name: '', description: '' });

  const [isOpen, setIsOpen] = useState(false);
  const [message, setMessage] = useState('');

  const openMessage = (text) => {
    setMessage(text);
    setIsOpen(true);
  };

  const closePopup = () => setIsOpen(false);

  const authHeaders = {
    'Content-Type': 'application/json',
    Authorization: token
  };

  const loadAll = async (semesterParam) => {
    const query = semesterParam ? `?semester=${encodeURIComponent(semesterParam)}` : '';
    setLoading(true);
    try {
      const [bankRes, configRes, templatesRes] = await Promise.all([
        fetch(`${BACKEND_URL}/monitor-survey/admin/questions${query}`, { headers: authHeaders }),
        fetch(`${BACKEND_URL}/monitor-survey/admin/current-config${query}`, { headers: authHeaders }),
        fetch(`${BACKEND_URL}/monitor-survey/admin/templates`, { headers: authHeaders })
      ]);

      const [bankBody, configBody, templatesBody] = await Promise.all([
        bankRes.json().catch(() => []),
        configRes.json().catch(() => ({})),
        templatesRes.json().catch(() => [])
      ]);

      if (!bankRes.ok) throw new Error(bankBody.error || 'No se pudo cargar el banco de preguntas');
      if (!configRes.ok) throw new Error(configBody.error || 'No se pudo cargar la configuración actual');
      if (!templatesRes.ok) throw new Error(templatesBody.error || 'No se pudieron cargar las plantillas');

      const bank = Array.isArray(bankBody) ? bankBody : [];
      const selected = Array.isArray(configBody.questions)
        ? configBody.questions
            .slice()
            .sort((a, b) => (a.displayOrder || 0) - (b.displayOrder || 0))
            .map((item) => item.id)
        : [];

      setQuestions(bank);
      setSelectedQuestionIds(selected);
      setTemplates(Array.isArray(templatesBody) ? templatesBody : []);
      if (configBody.semester && !semesterParam) {
        setSemester(configBody.semester);
      }
    } catch (error) {
      openMessage(error.message || 'Error cargando la información');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadAll();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const selectedQuestions = useMemo(() => {
    const map = new Map(questions.map((q) => [q.id, q]));
    return selectedQuestionIds.map((id, index) => ({ ...map.get(id), displayOrder: index + 1 })).filter(Boolean);
  }, [questions, selectedQuestionIds]);

  const toggleQuestionSelection = (questionId) => {
    setSelectedQuestionIds((prev) => {
      if (prev.includes(questionId)) {
        return prev.filter((id) => id !== questionId);
      }
      return [...prev, questionId];
    });
  };

  const moveSelectedQuestion = (questionId, direction) => {
    setSelectedQuestionIds((prev) => {
      const idx = prev.indexOf(questionId);
      if (idx === -1) return prev;
      const nextIdx = direction === 'up' ? idx - 1 : idx + 1;
      if (nextIdx < 0 || nextIdx >= prev.length) return prev;
      const next = [...prev];
      [next[idx], next[nextIdx]] = [next[nextIdx], next[idx]];
      return next;
    });
  };

  const handleCreateQuestion = async (event) => {
    event.preventDefault();
    if (!newQuestion.statement.trim() || !newQuestion.category.trim()) {
      openMessage('Debes ingresar el texto y la categoría de la pregunta.');
      return;
    }

    setCreatingQuestion(true);
    try {
      const res = await fetch(`${BACKEND_URL}/monitor-survey/admin/questions`, {
        method: 'POST',
        headers: authHeaders,
        body: JSON.stringify(newQuestion)
      });
      const body = await res.json().catch(() => ({}));
      if (!res.ok) throw new Error(body.error || 'No se pudo crear la pregunta');

      setNewQuestion({ statement: '', category: '' });
      openMessage('Pregunta creada correctamente.');
      await loadAll(semester);
    } catch (error) {
      openMessage(error.message || 'No se pudo crear la pregunta');
    } finally {
      setCreatingQuestion(false);
    }
  };

  const startEditing = (question) => {
    setEditingQuestionId(question.id);
    setEditingQuestion({ statement: question.statement || '', category: question.category || '' });
  };

  const cancelEditing = () => {
    setEditingQuestionId(null);
    setEditingQuestion({ statement: '', category: '' });
  };

  const saveEditing = async (questionId) => {
    if (!editingQuestion.statement.trim() || !editingQuestion.category.trim()) {
      openMessage('Debes completar texto y categoría.');
      return;
    }

    try {
      const semesterQuery = semester ? `?semester=${encodeURIComponent(semester)}` : '';
      const res = await fetch(`${BACKEND_URL}/monitor-survey/admin/questions/${questionId}${semesterQuery}`, {
        method: 'PUT',
        headers: authHeaders,
        body: JSON.stringify(editingQuestion)
      });
      const body = await res.json().catch(() => ({}));
      if (!res.ok) throw new Error(body.error || 'No se pudo editar la pregunta');

      openMessage('Pregunta actualizada.');
      cancelEditing();
      await loadAll(semester);
    } catch (error) {
      openMessage(error.message || 'No se pudo editar la pregunta');
    }
  };

  const toggleBankStatus = async (question) => {
    try {
      const res = await fetch(`${BACKEND_URL}/monitor-survey/admin/questions/${question.id}/status`, {
        method: 'PATCH',
        headers: authHeaders,
        body: JSON.stringify({ bankActive: !question.bankActive })
      });
      const body = await res.json().catch(() => ({}));
      if (!res.ok) throw new Error(body.error || 'No se pudo cambiar el estado de la pregunta');

      await loadAll(semester);
    } catch (error) {
      openMessage(error.message || 'No se pudo cambiar el estado');
    }
  };

  const saveCurrentConfig = async () => {
    if (!semester.trim()) {
      openMessage('Debes definir el semestre actual.');
      return;
    }
    if (selectedQuestionIds.length === 0) {
      openMessage('Selecciona al menos una pregunta para la encuesta activa.');
      return;
    }

    setSavingConfig(true);
    try {
      const res = await fetch(`${BACKEND_URL}/monitor-survey/admin/current-config`, {
        method: 'PUT',
        headers: authHeaders,
        body: JSON.stringify({ semester, questionIds: selectedQuestionIds })
      });
      const body = await res.json().catch(() => ({}));
      if (!res.ok) throw new Error(body.error || 'No se pudo guardar la configuración');

      openMessage('Configuración de encuesta guardada.');
      await loadAll(semester);
    } catch (error) {
      openMessage(error.message || 'No se pudo guardar la configuración');
    } finally {
      setSavingConfig(false);
    }
  };

  const createTemplate = async (event) => {
    event.preventDefault();
    if (!templateForm.name.trim()) {
      openMessage('Debes ingresar el nombre de la plantilla.');
      return;
    }
    if (selectedQuestionIds.length === 0) {
      openMessage('Selecciona preguntas antes de crear la plantilla.');
      return;
    }

    setCreatingTemplate(true);
    try {
      const res = await fetch(`${BACKEND_URL}/monitor-survey/admin/templates`, {
        method: 'POST',
        headers: authHeaders,
        body: JSON.stringify({
          name: templateForm.name,
          description: templateForm.description,
          questionIds: selectedQuestionIds
        })
      });
      const body = await res.json().catch(() => ({}));
      if (!res.ok) throw new Error(body.error || 'No se pudo crear la plantilla');

      setTemplateForm({ name: '', description: '' });
      openMessage('Plantilla guardada correctamente.');
      await loadAll(semester);
    } catch (error) {
      openMessage(error.message || 'No se pudo crear la plantilla');
    } finally {
      setCreatingTemplate(false);
    }
  };

  const applyTemplate = async (templateId) => {
    if (!semester.trim()) {
      openMessage('Debes definir el semestre antes de aplicar una plantilla.');
      return;
    }

    try {
      const res = await fetch(`${BACKEND_URL}/monitor-survey/admin/apply-template`, {
        method: 'POST',
        headers: authHeaders,
        body: JSON.stringify({ templateId, semester })
      });
      const body = await res.json().catch(() => ({}));
      if (!res.ok) throw new Error(body.error || 'No se pudo aplicar la plantilla');

      openMessage('Plantilla aplicada a la encuesta activa.');
      await loadAll(semester);
    } catch (error) {
      openMessage(error.message || 'No se pudo aplicar la plantilla');
    }
  };

  return (
    <div className="hu026-layout">
      <VerticalNavbar />
      <PopUp show={isOpen} onClose={closePopup}>{message}</PopUp>

      <div className="hu026-content">
        <header className="hu026-header">
          <h2>Gestión de encuesta de evaluación de monitores</h2>
          <p>Administra el banco de preguntas, define el formulario activo del semestre y guarda plantillas.</p>
          <div className="hu026-semester-row">
            <label htmlFor="semesterInput">Semestre activo</label>
            <input
              id="semesterInput"
              type="text"
              value={semester}
              onChange={(event) => setSemester(event.target.value)}
              placeholder="Ej: 2026-1"
            />
            <button type="button" onClick={() => loadAll(semester)}>
              Cargar semestre
            </button>
          </div>
        </header>

        {loading ? (
          <div className="hu026-loading">
            <LoadingSpinner message="Cargando configuración de encuesta" />
          </div>
        ) : (
          <div className="hu026-grid">
            <section className="hu026-card">
              <h3>Banco de preguntas</h3>
              <form className="hu026-new-question" onSubmit={handleCreateQuestion}>
                <textarea
                  value={newQuestion.statement}
                  onChange={(event) => setNewQuestion((prev) => ({ ...prev, statement: event.target.value }))}
                  placeholder="Texto de la afirmación"
                />
                <input
                  value={newQuestion.category}
                  onChange={(event) => setNewQuestion((prev) => ({ ...prev, category: event.target.value }))}
                  placeholder="Categoría"
                />
                <button type="submit" disabled={creatingQuestion}>
                  {creatingQuestion ? 'Creando...' : 'Agregar pregunta'}
                </button>
              </form>

              <div className="hu026-question-list">
                {questions.map((question) => {
                  const isSelected = selectedQuestionIds.includes(question.id);
                  const order = selectedQuestionIds.indexOf(question.id) + 1;
                  const isEditing = editingQuestionId === question.id;

                  return (
                    <article key={question.id} className={`hu026-question-item ${isSelected ? 'selected' : ''}`}>
                      <div className="hu026-question-meta">
                        <span className={`status ${question.bankActive ? 'active' : 'inactive'}`}>
                          {question.bankActive ? 'Activa' : 'Inactiva'}
                        </span>
                        <span className="category">{question.category}</span>
                        {isSelected && <span className="order">Orden {order}</span>}
                      </div>

                      {isEditing ? (
                        <div className="hu026-edit-block">
                          <textarea
                            value={editingQuestion.statement}
                            onChange={(event) => setEditingQuestion((prev) => ({ ...prev, statement: event.target.value }))}
                          />
                          <input
                            value={editingQuestion.category}
                            onChange={(event) => setEditingQuestion((prev) => ({ ...prev, category: event.target.value }))}
                          />
                          <div className="actions">
                            <button type="button" onClick={() => saveEditing(question.id)}>Guardar</button>
                            <button type="button" onClick={cancelEditing}>Cancelar</button>
                          </div>
                        </div>
                      ) : (
                        <>
                          <p>{question.statement}</p>
                          <div className="actions">
                            <button type="button" onClick={() => toggleQuestionSelection(question.id)} disabled={!question.bankActive && !isSelected}>
                              {isSelected ? 'Quitar de encuesta' : 'Agregar a encuesta'}
                            </button>
                            <button type="button" onClick={() => startEditing(question)}>Editar</button>
                            <button type="button" onClick={() => toggleBankStatus(question)}>
                              {question.bankActive ? 'Desactivar' : 'Activar'}
                            </button>
                          </div>
                        </>
                      )}
                    </article>
                  );
                })}
              </div>
            </section>

            <section className="hu026-card">
              <h3>Encuesta activa del semestre</h3>
              {selectedQuestions.length === 0 ? (
                <p className="empty">No hay preguntas seleccionadas.</p>
              ) : (
                <ol className="hu026-order-list">
                  {selectedQuestions.map((question) => (
                    <li key={question.id}>
                      <div>
                        <strong>{question.category}</strong>
                        <p>{question.statement}</p>
                      </div>
                      <div className="actions vertical">
                        <button type="button" onClick={() => moveSelectedQuestion(question.id, 'up')}>Subir</button>
                        <button type="button" onClick={() => moveSelectedQuestion(question.id, 'down')}>Bajar</button>
                      </div>
                    </li>
                  ))}
                </ol>
              )}

              <button type="button" className="save-config" onClick={saveCurrentConfig} disabled={savingConfig}>
                {savingConfig ? 'Guardando...' : 'Guardar encuesta activa'}
              </button>

              <div className="hu026-template-block">
                <h4>Plantillas</h4>
                <form onSubmit={createTemplate} className="hu026-template-form">
                  <input
                    value={templateForm.name}
                    onChange={(event) => setTemplateForm((prev) => ({ ...prev, name: event.target.value }))}
                    placeholder="Nombre de la plantilla"
                  />
                  <textarea
                    value={templateForm.description}
                    onChange={(event) => setTemplateForm((prev) => ({ ...prev, description: event.target.value }))}
                    placeholder="Descripción (opcional)"
                  />
                  <button type="submit" disabled={creatingTemplate}>
                    {creatingTemplate ? 'Guardando...' : 'Guardar plantilla desde selección actual'}
                  </button>
                </form>

                <div className="hu026-templates-list">
                  {templates.map((template) => (
                    <article key={template.id}>
                      <div>
                        <strong>{template.name}</strong>
                        <p>{template.description || 'Sin descripción'}</p>
                        <small>{template.questions?.length || 0} preguntas</small>
                      </div>
                      <button type="button" onClick={() => applyTemplate(template.id)}>
                        Aplicar al semestre
                      </button>
                    </article>
                  ))}
                </div>
              </div>
            </section>
          </div>
        )}
      </div>
    </div>
  );
}

export default GestionEncuestaMonitoresHU026;
