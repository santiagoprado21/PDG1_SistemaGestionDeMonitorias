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
  const [creatingQuestion, setCreatingQuestion] = useState(false);
  const [creatingTemplate, setCreatingTemplate] = useState(false);
  const [savingTemplateEdit, setSavingTemplateEdit] = useState(false);
  const [deletingTemplateId, setDeletingTemplateId] = useState(null);

  const [questions, setQuestions] = useState([]);
  const [selectedQuestionIds, setSelectedQuestionIds] = useState([]);
  const [templates, setTemplates] = useState([]);
  const [editingTemplateId, setEditingTemplateId] = useState(null);
  const [expandedTemplateIds, setExpandedTemplateIds] = useState([]);

  const [newQuestion, setNewQuestion] = useState({ statement: '', category: '' });
  const [editingQuestionId, setEditingQuestionId] = useState(null);
  const [editingQuestion, setEditingQuestion] = useState({ statement: '', category: '' });

  const [templateForm, setTemplateForm] = useState({ name: '', description: '', createdForSemester: '' });

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

      const effectiveSemester = (semesterParam || configBody.semester || '').trim();
      if (effectiveSemester) {
        setTemplateForm((prev) =>
          prev.createdForSemester && prev.createdForSemester.trim() !== ''
            ? prev
            : { ...prev, createdForSemester: effectiveSemester }
        );
      }

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

  const getTemplateQuestionIds = (template) => {
    return (template.questions || [])
      .slice()
      .sort((a, b) => (a.displayOrder || 0) - (b.displayOrder || 0))
      .map((question) => question.id)
      .filter(Boolean);
  };

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

  const createTemplate = async (event) => {
    event.preventDefault();
    if (!templateForm.name.trim()) {
      openMessage('Debes ingresar el nombre de la plantilla.');
      return;
    }
    if (!templateForm.createdForSemester.trim()) {
      openMessage('Debes indicar el periodo de creación de la plantilla.');
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
          createdForSemester: templateForm.createdForSemester,
          questionIds: selectedQuestionIds
        })
      });
      const body = await res.json().catch(() => ({}));
      if (!res.ok) throw new Error(body.error || 'No se pudo crear la plantilla');

      setTemplateForm({ name: '', description: '', createdForSemester: semester || '' });
      openMessage('Plantilla guardada correctamente.');
      await loadAll(semester);
    } catch (error) {
      openMessage(error.message || 'No se pudo crear la plantilla');
    } finally {
      setCreatingTemplate(false);
    }
  };

  const startEditingTemplate = (template) => {
    const templateQuestionIds = getTemplateQuestionIds(template);
    setEditingTemplateId(template.id);
    setTemplateForm({
      name: template.name || '',
      description: template.description || '',
      createdForSemester: template.createdForSemester || semester || ''
    });
    setSelectedQuestionIds(templateQuestionIds);
  };

  const cancelEditingTemplate = () => {
    setEditingTemplateId(null);
    setTemplateForm({ name: '', description: '', createdForSemester: semester || '' });
  };

  const saveTemplateEdit = async () => {
    if (!editingTemplateId) return;

    if (!templateForm.name.trim()) {
      openMessage('Debes ingresar el nombre de la plantilla.');
      return;
    }
    if (!templateForm.createdForSemester.trim()) {
      openMessage('Debes indicar el periodo de creación de la plantilla.');
      return;
    }
    if (selectedQuestionIds.length === 0) {
      openMessage('Selecciona preguntas antes de actualizar la plantilla.');
      return;
    }

    setSavingTemplateEdit(true);
    try {
      const res = await fetch(`${BACKEND_URL}/monitor-survey/admin/templates/${editingTemplateId}`, {
        method: 'PUT',
        headers: authHeaders,
        body: JSON.stringify({
          name: templateForm.name,
          description: templateForm.description,
          createdForSemester: templateForm.createdForSemester,
          questionIds: selectedQuestionIds
        })
      });
      const body = await res.json().catch(() => ({}));
      if (!res.ok) throw new Error(body.error || 'No se pudo actualizar la plantilla');

      openMessage('Plantilla actualizada correctamente.');
      cancelEditingTemplate();
      await loadAll(semester);
    } catch (error) {
      openMessage(error.message || 'No se pudo actualizar la plantilla');
    } finally {
      setSavingTemplateEdit(false);
    }
  };

  const deleteTemplate = async (templateId) => {
    const confirmed = window.confirm('¿Seguro que deseas eliminar esta plantilla?');
    if (!confirmed) return;

    setDeletingTemplateId(templateId);
    try {
      const res = await fetch(`${BACKEND_URL}/monitor-survey/admin/templates/${templateId}`, {
        method: 'DELETE',
        headers: authHeaders
      });
      const body = await res.json().catch(() => ({}));
      if (!res.ok) throw new Error(body.error || 'No se pudo eliminar la plantilla');

      if (editingTemplateId === templateId) {
        cancelEditingTemplate();
      }

      openMessage('Plantilla eliminada correctamente.');
      await loadAll(semester);
    } catch (error) {
      openMessage(error.message || 'No se pudo eliminar la plantilla');
    } finally {
      setDeletingTemplateId(null);
    }
  };

  const toggleTemplateQuestions = (templateId) => {
    setExpandedTemplateIds((prev) =>
      prev.includes(templateId) ? prev.filter((id) => id !== templateId) : [...prev, templateId]
    );
  };

  const applyTemplate = async (templateId) => {
    const periodToApply = (semester || '').trim();
    if (!periodToApply) {
      openMessage('Debes definir el periodo antes de aplicar una plantilla.');
      return;
    }

    try {
      const res = await fetch(`${BACKEND_URL}/monitor-survey/admin/apply-template`, {
        method: 'POST',
        headers: authHeaders,
        body: JSON.stringify({ templateId, semester: periodToApply })
      });
      const body = await res.json().catch(() => ({}));
      if (!res.ok) throw new Error(body.error || 'No se pudo aplicar la plantilla');

      setSemester(periodToApply);
      openMessage(`Plantilla aplicada al periodo ${periodToApply}.`);
      await loadAll(periodToApply);
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
          <p>Selecciona el periodo de trabajo, administra el banco de preguntas y gestiona plantillas.</p>
          <div className="hu026-semester-row">
            <label htmlFor="semesterInput">Periodo activo</label>
            <input
              id="semesterInput"
              type="text"
              value={semester}
              onChange={(event) => setSemester(event.target.value)}
              placeholder="Ej: 2026-1"
            />
            <button type="button" onClick={() => loadAll(semester)}>
              Cargar periodo
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
              <p className="hu026-card-help">Crea, edita y selecciona preguntas para la configuración actual.</p>
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
                              {isSelected ? 'Quitar de configuración' : 'Agregar a configuración'}
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
              <h3>{editingTemplateId ? 'Editar plantilla' : 'Crear plantilla'}</h3>
              <p className="hu026-card-help">Usa las preguntas seleccionadas del banco para guardar una plantilla con nombre y periodo.</p>

              <form onSubmit={editingTemplateId ? (event) => { event.preventDefault(); saveTemplateEdit(); } : createTemplate} className="hu026-template-form">
                <input
                  value={templateForm.name}
                  onChange={(event) => setTemplateForm((prev) => ({ ...prev, name: event.target.value }))}
                  placeholder="Nombre de la plantilla"
                />
                <input
                  value={templateForm.createdForSemester}
                  onChange={(event) => setTemplateForm((prev) => ({ ...prev, createdForSemester: event.target.value }))}
                  placeholder="Periodo de creación (Ej: 2026-1)"
                />
                <textarea
                  value={templateForm.description}
                  onChange={(event) => setTemplateForm((prev) => ({ ...prev, description: event.target.value }))}
                  placeholder="Descripción (opcional)"
                />

                <div className="hu026-selected-summary">
                  <strong>{selectedQuestionIds.length}</strong> preguntas seleccionadas
                </div>

                {selectedQuestions.length === 0 ? (
                  <p className="empty">No hay preguntas seleccionadas para la plantilla.</p>
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

                <div className="actions">
                  {editingTemplateId ? (
                    <>
                      <button type="submit" disabled={savingTemplateEdit}>
                        {savingTemplateEdit ? 'Actualizando...' : 'Actualizar plantilla'}
                      </button>
                      <button type="button" onClick={cancelEditingTemplate}>Cancelar edición</button>
                    </>
                  ) : (
                    <button type="submit" disabled={creatingTemplate}>
                      {creatingTemplate ? 'Guardando plantilla...' : 'Guardar plantilla'}
                    </button>
                  )}
                </div>
              </form>
            </section>

            <section className="hu026-card">
              <h3>Plantillas creadas</h3>
              <p className="hu026-card-help">Visualiza preguntas, edita la plantilla, aplícala al periodo o elimínala.</p>

              <div className="hu026-templates-list">
                {templates.length === 0 ? (
                  <p className="empty">No hay plantillas guardadas.</p>
                ) : (
                  templates.map((template) => {
                    const isExpanded = expandedTemplateIds.includes(template.id);
                    const questionItems = (template.questions || [])
                      .slice()
                      .sort((a, b) => (a.displayOrder || 0) - (b.displayOrder || 0));

                    return (
                      <article key={template.id}>
                        <div className="hu026-template-main">
                          <strong>{template.name}</strong>
                          <p>{template.description || 'Sin descripción'}</p>
                          <small>
                            Periodo de creación: {template.createdForSemester || 'No definido'} | {template.questions?.length || 0} preguntas
                          </small>
                        </div>

                        <div className="actions hu026-template-actions">
                          <button type="button" onClick={() => toggleTemplateQuestions(template.id)}>
                            {isExpanded ? 'Ocultar preguntas' : 'Ver preguntas'}
                          </button>
                          <button type="button" onClick={() => startEditingTemplate(template)}>
                            Editar
                          </button>
                          <button type="button" onClick={() => applyTemplate(template.id)}>
                            Aplicar al periodo
                          </button>
                          <button
                            type="button"
                            className="danger"
                            onClick={() => deleteTemplate(template.id)}
                            disabled={deletingTemplateId === template.id}
                          >
                            {deletingTemplateId === template.id ? 'Eliminando...' : 'Eliminar'}
                          </button>
                        </div>

                        {isExpanded && (
                          <ol className="hu026-template-questions">
                            {questionItems.map((question) => (
                              <li key={`${template.id}-${question.id}`}>
                                <strong>{question.category}</strong>
                                <p>{question.statement}</p>
                              </li>
                            ))}
                          </ol>
                        )}
                      </article>
                    );
                  })
                )}
              </div>
            </section>
          </div>
        )}
      </div>
    </div>
  );
}

export default GestionEncuestaMonitoresHU026;
