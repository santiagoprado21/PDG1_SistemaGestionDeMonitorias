import React, { useEffect, useMemo, useRef, useState } from 'react';
import VerticalNavbar from './VerticalNavbar';
import './Chat.css';
import { BACKEND_URL } from './config/ApiBackend';
import { Paperclip, X } from 'lucide-react';
//.
function Chat() {
  const iconProps = {
    size: 16,
    strokeWidth: 2,
    strokeLinecap: 'butt',
    strokeLinejoin: 'miter'
  };

  const role = (localStorage.getItem('role') || '').toLowerCase();
  const userId = localStorage.getItem('userId') || '';
  const userName = localStorage.getItem('userName') || 'Usuario';
  const sidebarSubtitle = role === 'jfedpto'
    ? 'Comunicacion directa con profesores'
    : role === 'professor'
      ? 'Comunicacion directa con monitores y jefes de departamento'
      : 'Comunicacion directa con profesores';

  const getChatSeenStorageKey = () => `sigmaChatLastSeen:${role}:${userId}`;

  const markConversationAsSeen = (conversationId, currentMessages) => {
    if (!conversationId || !Array.isArray(currentMessages) || currentMessages.length === 0) return;
    const latest = currentMessages[currentMessages.length - 1];
    if (!latest?.createdAt) return;
    try {
      const key = getChatSeenStorageKey();
      const current = JSON.parse(localStorage.getItem(key) || '{}');
      current[conversationId] = latest.createdAt;
      localStorage.setItem(key, JSON.stringify(current));
    } catch {
      // noop
    }
  };

  const [conversations, setConversations] = useState([]);
  const [selectedId, setSelectedId] = useState('');
  const [messages, setMessages] = useState([]);
  const [loadingConversations, setLoadingConversations] = useState(false);
  const [loadingMessages, setLoadingMessages] = useState(false);
  const [error, setError] = useState('');

  const [draft, setDraft] = useState('');
  const [attachments, setAttachments] = useState([]); // [{file, preview}]
  const fileInputRef = useRef(null);
  const messagesRef = useRef(null);
  const shouldScrollToBottomRef = useRef(true);

  const selected = useMemo(
    () => conversations.find(c => c.id === selectedId),
    [conversations, selectedId]
  );

  const getAuthHeader = () => {
    const token = localStorage.getItem('token');
    if (!token) return {};
    return {
      Authorization: token.startsWith('Bearer ') ? token : `Bearer ${token}`
    };
  };

  const fetchConversations = async () => {
    if (!userId || !role) return;
    setLoadingConversations(true);
    setError('');
    try {
      const response = await fetch(`${BACKEND_URL}/chat/conversations/${userId}/${role}`, {
        method: 'GET',
        headers: {
          'Content-Type': 'application/json',
          ...getAuthHeader()
        }
      });
      if (!response.ok) {
        const text = await response.text();
        throw new Error(text || 'No fue posible cargar conversaciones');
      }
      const data = await response.json();
      setConversations(Array.isArray(data) ? data : []);

      const focusConversationId = localStorage.getItem('focusChatConversationId');
      if (focusConversationId && Array.isArray(data) && data.some(c => c.id === focusConversationId)) {
        setSelectedId(focusConversationId);
        localStorage.removeItem('focusChatConversationId');
        return;
      }

      if (Array.isArray(data) && data.length > 0) {
        setSelectedId(prev => {
          if (prev && data.some(c => c.id === prev)) return prev;
          return data[0].id;
        });
      } else {
        setSelectedId('');
        setMessages([]);
      }
    } catch (e) {
      setError(typeof e.message === 'string' ? e.message : 'Error al cargar conversaciones');
      setConversations([]);
      setSelectedId('');
      setMessages([]);
    } finally {
      setLoadingConversations(false);
    }
  };

  const fetchMessages = async (conversationId) => {
    if (!conversationId) return;
    setLoadingMessages(true);
    setError('');
    try {
      const response = await fetch(`${BACKEND_URL}/chat/messages/${conversationId}`, {
        method: 'GET',
        headers: {
          'Content-Type': 'application/json',
          ...getAuthHeader()
        }
      });
      if (!response.ok) {
        const text = await response.text();
        throw new Error(text || 'No fue posible cargar mensajes');
      }
      const data = await response.json();
      const safeMessages = Array.isArray(data) ? data : [];
      setMessages(safeMessages);
      markConversationAsSeen(conversationId, safeMessages);
    } catch (e) {
      setError(typeof e.message === 'string' ? e.message : 'Error al cargar mensajes');
      setMessages([]);
    } finally {
      setLoadingMessages(false);
    }
  };

  useEffect(() => {
    fetchConversations();
  }, [userId, role]);

  useEffect(() => {
    if (selectedId) {
      fetchMessages(selectedId);
    }
  }, [selectedId]);

  useEffect(() => {
    if (!selectedId) return;
    const interval = setInterval(() => {
      fetchMessages(selectedId);
      fetchConversations();
    }, 10000);
    return () => clearInterval(interval);
  }, [selectedId]);

  useEffect(() => {
    const container = messagesRef.current;
    if (!container) return;

    if (shouldScrollToBottomRef.current) {
      container.scrollTop = container.scrollHeight;
    }
  }, [messages]);

  const handleMessagesScroll = () => {
    const container = messagesRef.current;
    if (!container) return;
    const distanceToBottom = container.scrollHeight - container.scrollTop - container.clientHeight;
    shouldScrollToBottomRef.current = distanceToBottom < 60;
  };

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
    setAttachments(prev => {
      const item = prev[index];
      if (item?.preview) URL.revokeObjectURL(item.preview);
      return prev.filter((_, i) => i !== index);
    });
  };

  const formatTime = (dateValue) => {
    if (!dateValue) return '';
    return new Date(dateValue).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
  };

  const resolveAttachmentUrl = (att) => {
    if (!att?.url) return '';
    if (att.url.startsWith('http://') || att.url.startsWith('https://')) return att.url;
    return `${BACKEND_URL}${att.url}`;
  };

  const handleSend = async () => {
    if (!draft.trim() && attachments.length === 0) return;
    if (!selected?.id || !selected?.otherUserId || !userId || !role) return;

    const payload = {
      conversationId: selected.id,
      senderId: userId,
      senderRole: role,
      receiverId: selected.otherUserId,
      message: draft.trim()
    };

    try {
      setError('');
      let response;

      if (attachments.length > 0) {
        const formData = new FormData();
        formData.append('payload', new Blob([JSON.stringify(payload)], { type: 'application/json' }));
        attachments.forEach(a => formData.append('files', a.file));

        response = await fetch(`${BACKEND_URL}/chat/messages/with-attachments`, {
          method: 'POST',
          headers: {
            ...getAuthHeader()
          },
          body: formData
        });
      } else {
        response = await fetch(`${BACKEND_URL}/chat/messages`, {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json',
            ...getAuthHeader()
          },
          body: JSON.stringify(payload)
        });
      }

      if (!response.ok) {
        const text = await response.text();
        throw new Error(text || 'No fue posible enviar el mensaje');
      }

      attachments.forEach(a => {
        if (a.preview) URL.revokeObjectURL(a.preview);
      });
      setDraft('');
      setAttachments([]);
      await fetchMessages(selected.id);
      await fetchConversations();
    } catch (e) {
      setError(typeof e.message === 'string' ? e.message : 'Error al enviar mensaje');
    }
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
              <p className="muted">{sidebarSubtitle}</p>
            </div>
            <div className="chat-conversation-list">
              {loadingConversations && <p className="muted" style={{ padding: '0 16px' }}>Cargando conversaciones...</p>}
              {!loadingConversations && conversations.length === 0 && <p className="muted" style={{ padding: '0 16px' }}>No tienes chats disponibles.</p>}
              {conversations.map(conv => (
                <button
                  key={conv.id}
                  className={"chat-conversation-item" + (conv.id === selectedId ? ' selected' : '')}
                  onClick={() => setSelectedId(conv.id)}
                >
                  <div className="item-titles">
                    <div className="item-title">{conv.title}</div>
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

            <div className="chat-messages" ref={messagesRef} onScroll={handleMessagesScroll}>
              {!!error && <p className="muted">{error}</p>}
              {loadingMessages && <p className="muted">Cargando mensajes...</p>}
              {!loadingMessages && messages.length === 0 && <p className="muted">Aún no hay mensajes en esta conversación.</p>}
              {messages.map(msg => (
                <div
                  key={msg.id}
                  className={"message-bubble " + (msg.senderId === userId ? 'own' : 'other')}
                >
                  <div className="message-meta">
                    <span className="author">{msg.senderId === userId ? 'Tu' : selected?.title?.replace(/^(Monitor: |Profesor: |Jefe de Departamento: )/, '') || userName}</span>
                    <span className="time">{formatTime(msg.createdAt)}</span>
                  </div>
                  {!!msg.message && <div className="message-text">{msg.message}</div>}
                  {msg.attachments && msg.attachments.length > 0 && (
                    <div className="message-attachments">
                      {msg.attachments.map((att, i) => (
                        <div key={i} className="attachment-item">
                          {att.contentType && att.contentType.startsWith('image/') ? (
                            <img src={resolveAttachmentUrl(att)} alt={att.name} className="attachment-thumb" />
                          ) : (
                            <div className="attachment-file-icon" title={att.name}>
                              <Paperclip {...iconProps} size={14} />
                            </div>
                          )}
                          <div className="attachment-info" title={att.name}>
                            <a className="attachment-name" href={resolveAttachmentUrl(att)} target="_blank" rel="noreferrer">{att.name}</a>
                            <span className="attachment-size">{Math.max(1, Math.round((att.size || 0)/1024))} KB</span>
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
                        <div className="pending-icon" title={a.file.name}><Paperclip {...iconProps} size={14} /></div>
                      )}
                      <span className="pending-name" title={a.file.name}>{a.file.name}</span>
                      <button type="button" className="remove-attachment-btn" onClick={() => removeAttachment(i)}><X {...iconProps} size={14} /></button>
                    </div>
                  ))}
                </div>
                <textarea
                  className="chat-textarea"
                  rows={2}
                  placeholder="Escribe un mensaje relacionado con la actividad..."
                  value={draft}
                  onChange={e => setDraft(e.target.value)}
                  disabled={!selectedId}
                />
                <div className="input-actions">
                  <button type="button" className="attach-btn" onClick={handleOpenFileDialog} title="Adjuntar archivos" disabled={!selectedId}><Paperclip {...iconProps} size={14} /></button>
                  <input
                    ref={fileInputRef}
                    type="file"
                    multiple
                    style={{display:'none'}}
                    onChange={handleFilesSelected}
                  />
                  <button className="chat-send-button" onClick={handleSend} disabled={!selectedId}>Enviar</button>
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
