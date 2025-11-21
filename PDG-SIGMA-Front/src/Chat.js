import React, { useMemo, useState, useRef } from 'react';
import VerticalNavbar from './VerticalNavbar';
import './Chat.css';

function Chat() {
  const role = localStorage.getItem('role');
  const userName = localStorage.getItem('userName') || 'Profesor/a';

  // Mock de conversaciones: actividades y monitores
  const conversations = useMemo(() => ([
    {
      id: 'conv-1',
      title: 'Algoritmos - Monitor: Ana López',
      subtitle: 'Act. 3: Taller de recursión',
      unread: 2,
      messages: [
        { id: 'm1', from: 'Ana López', at: '09:20', text: 'Profe, ¿revisó el material?' },
        { id: 'm2', from: userName, at: '09:45', text: 'Sí, está muy bien. Añade 2 ejemplos.' },
        { id: 'm3', from: 'Ana López', at: '10:05', text: 'Perfecto, los preparo para hoy.' },
      ],
    },
    {
      id: 'conv-2',
      title: 'Estructuras - Monitor: Carlos Díaz',
      subtitle: 'Act. 1: Laboratorio Pilas/Colas',
      unread: 0,
      messages: [
        { id: 'm1', from: userName, at: '08:10', text: 'Recuerda confirmar el salón del lab.' },
        { id: 'm2', from: 'Carlos Díaz', at: '08:15', text: 'Salón L-302 confirmado.' },
      ],
    },
    {
      id: 'conv-3',
      title: 'Bases de Datos - Monitor: María Pérez',
      subtitle: 'Act. 2: Normalización',
      unread: 5,
      messages: [
        { id: 'm1', from: 'María Pérez', at: '07:55', text: '¿Usamos el mismo dataset?' },
      ],
    },
  ]), [userName]);

  const [selectedId, setSelectedId] = useState(conversations[0]?.id);
  const selected = conversations.find(c => c.id === selectedId);

  const [draft, setDraft] = useState('');
  const [attachments, setAttachments] = useState([]); // [{file, preview}]
  const fileInputRef = useRef(null);

  const handleOpenFileDialog = () => {
    if (fileInputRef.current) fileInputRef.current.click();
  };

  const handleFilesSelected = (e) => {
    const files = Array.from(e.target.files || []);
    if (!files.length) return;
    const mapped = files.map(f => ({
      file: f,
      preview: f.type.startsWith('image/') ? URL.createObjectURL(f) : null
    }));
    setAttachments(prev => prev.concat(mapped));
    // reset input so selecting same file again works
    e.target.value = '';
  };

  const removeAttachment = (index) => {
    setAttachments(prev => prev.filter((_, i) => i !== index));
  };

  const handleSend = () => {
    if (!draft.trim() && attachments.length === 0) return;
    selected.messages.push({
      id: 'm' + (selected.messages.length + 1),
      from: userName,
      at: new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }),
      text: draft.trim(),
      attachments: attachments.map(a => ({
        name: a.file.name,
        type: a.file.type,
        size: a.file.size,
        preview: a.preview
      }))
    });
    // limpiar
    setDraft('');
    setAttachments([]);
  };

  return (
    <div className="page-layout">
      <div className="left-column">
        <VerticalNavbar />
      </div>

      <div className="right-column">
        <div className="chat-container">
          <div className="chat-sidebar">
            <div className="chat-sidebar-header">
              <h2>Chat</h2>
              <p className="muted">Comunicación directa con monitores</p>
            </div>
            <div className="chat-conversation-list">
              {conversations.map(conv => (
                <button
                  key={conv.id}
                  className={"chat-conversation-item" + (conv.id === selectedId ? ' selected' : '')}
                  onClick={() => setSelectedId(conv.id)}
                >
                  <div className="item-titles">
                    <div className="item-title">{conv.title}</div>
                    {!!conv.unread && <span className="badge">{conv.unread}</span>}
                  </div>
                  <div className="item-subtitle">{conv.subtitle}</div>
                </button>
              ))}
            </div>
          </div>

          <div className="chat-main">
            <div className="chat-header">
              <div className="chat-header-titles">
                <div className="chat-title">{selected?.title}</div>
                <div className="chat-subtitle">{selected?.subtitle}</div>
              </div>
            </div>

            <div className="chat-messages">
              {(selected?.messages || []).map(msg => (
                <div
                  key={msg.id}
                  className={"message-bubble " + (msg.from === userName ? 'own' : 'other')}
                >
                  <div className="message-meta">
                    <span className="author">{msg.from === userName ? 'Tú' : msg.from}</span>
                    <span className="time">{msg.at}</span>
                  </div>
                  {!!msg.text && <div className="message-text">{msg.text}</div>}
                  {msg.attachments && msg.attachments.length > 0 && (
                    <div className="message-attachments">
                      {msg.attachments.map((att, i) => (
                        <div key={i} className="attachment-item">
                          {att.preview && att.type.startsWith('image/') ? (
                            <img src={att.preview} alt={att.name} className="attachment-thumb" />
                          ) : (
                            <div className="attachment-file-icon" title={att.name}>
                              📎
                            </div>
                          )}
                          <div className="attachment-info" title={att.name}>
                            <span className="attachment-name">{att.name}</span>
                            <span className="attachment-size">{Math.round(att.size/1024)} KB</span>
                          </div>
                        </div>
                      ))}
                    </div>
                  )}
                </div>
              ))}
            </div>

            <div className="chat-input-area">
              <div className="input-left">
                <div className="attachment-bar" style={{display: attachments.length ? 'flex':'none'}}>
                  {attachments.map((a, i) => (
                    <div key={i} className="pending-attachment">
                      {a.preview && a.file.type.startsWith('image/') ? (
                        <img src={a.preview} alt={a.file.name} className="pending-thumb" />
                      ) : (
                        <div className="pending-icon" title={a.file.name}>📎</div>
                      )}
                      <span className="pending-name" title={a.file.name}>{a.file.name}</span>
                      <button type="button" className="remove-attachment-btn" onClick={() => removeAttachment(i)}>✕</button>
                    </div>
                  ))}
                </div>
                <textarea
                  className="chat-textarea"
                  rows={2}
                  placeholder="Escribe un mensaje relacionado con la actividad..."
                  value={draft}
                  onChange={e => setDraft(e.target.value)}
                />
                <div className="input-actions">
                  <button type="button" className="attach-btn" onClick={handleOpenFileDialog} title="Adjuntar archivos">📎</button>
                  <input
                    ref={fileInputRef}
                    type="file"
                    multiple
                    style={{display:'none'}}
                    onChange={handleFilesSelected}
                  />
                  <button className="chat-send-button" onClick={handleSend}>Enviar</button>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}

export default Chat;
