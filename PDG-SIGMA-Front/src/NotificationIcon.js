import React, { useState, useEffect, useRef } from "react";
import { Bell } from "./CustomComponents";
import { BACKEND_URL } from './config/ApiBackend';
import { useNavigate } from 'react-router-dom';

const NotificationIcon = () => {
  const [showNotifications, setShowNotifications] = useState(false);
  const [alerts, setAlerts] = useState([]);
  const [showSettings, setShowSettings] = useState(false);
  const [sound, setSound] = useState(true);
  const [types, setTypes] = useState({ PROGRESS_UPDATE: true, COMPLETED: true, OVERDUE: true, DUE_SOON: true, CHAT: true });
  const [loadingPrefs, setLoadingPrefs] = useState(false);
  const [sseEnabled, setSseEnabled] = useState(true); // flag local para habilitar SSE
  const eventSourceRef = useRef(null);
  const [saved, setSaved] = useState(false);
  const [chatAlerts, setChatAlerts] = useState([]);
  const previousChatUnreadRef = useRef(0);
  const navigate = useNavigate();

  const getChatSeenStorageKey = () => {
    const user = localStorage.getItem('userId') || '';
    const role = localStorage.getItem('role') || '';
    return `sigmaChatLastSeen:${role}:${user}`;
  };

  const loadChatSeenMap = () => {
    try {
      const raw = localStorage.getItem(getChatSeenStorageKey());
      return raw ? JSON.parse(raw) : {};
    } catch {
      return {};
    }
  };

  const saveChatSeenMap = (map) => {
    localStorage.setItem(getChatSeenStorageKey(), JSON.stringify(map));
  };

  const fetchChatAlerts = async () => {
    if (types.CHAT === false) {
      setChatAlerts([]);
      previousChatUnreadRef.current = 0;
      return;
    }

    const userId = localStorage.getItem("userId");
    const role = (localStorage.getItem("role") || '').toLowerCase();
    const token = localStorage.getItem('token');
    if (!userId || !(role === 'professor' || role === 'monitor')) {
      setChatAlerts([]);
      previousChatUnreadRef.current = 0;
      return;
    }

    try {
      const headers = {
        'Content-Type': 'application/json',
        Authorization: token
      };

      const convResp = await fetch(`${BACKEND_URL}/chat/conversations/${userId}/${role}`, {
        method: 'GET',
        headers
      });
      if (!convResp.ok) {
        setChatAlerts([]);
        return;
      }

      const conversations = await convResp.json();
      const safeConversations = Array.isArray(conversations) ? conversations : [];
      const seenMap = loadChatSeenMap();
      const nextAlerts = [];

      for (const conv of safeConversations) {
        if (!conv?.id) continue;
        const msgResp = await fetch(`${BACKEND_URL}/chat/messages/${conv.id}`, {
          method: 'GET',
          headers
        });
        if (!msgResp.ok) continue;

        const messages = await msgResp.json();
        const safeMessages = Array.isArray(messages) ? messages : [];
        const lastSeen = seenMap[conv.id] || '';

        const unreadMessages = safeMessages.filter(m =>
          m &&
          m.senderId !== userId &&
          !!m.createdAt &&
          (!lastSeen || new Date(m.createdAt).getTime() > new Date(lastSeen).getTime())
        );

        if (unreadMessages.length > 0) {
          const lastUnread = unreadMessages[unreadMessages.length - 1];
          nextAlerts.push({
            id: `chat-${conv.id}`,
            type: 'CHAT',
            message: `Nuevo mensaje en chat con ${conv.title?.replace(/^(Monitor: |Profesor: )/, '') || 'usuario'}`,
            date: new Date(lastUnread.createdAt).toLocaleDateString(),
            conversationId: conv.id,
            unreadCount: unreadMessages.length
          });
        }
      }

      const newUnread = nextAlerts.reduce((acc, item) => acc + (item.unreadCount || 0), 0);
      const soundPref = JSON.parse(localStorage.getItem('sigmaNotif.sound') || 'true');
      if (soundPref && previousChatUnreadRef.current > 0 && newUnread > previousChatUnreadRef.current) {
        try { new Audio('https://actions.google.com/sounds/v1/alarms/beep_short.ogg').play(); } catch(_){ }
      }
      previousChatUnreadRef.current = newUnread;
      setChatAlerts(nextAlerts);
    } catch (e) {
      console.warn('Error al consultar alertas de chat', e);
      setChatAlerts([]);
    }
  };

  const loadPrefsLocal = () => {
    try {
      const s = JSON.parse(localStorage.getItem('sigmaNotif.sound') ?? 'true');
      const t = JSON.parse(localStorage.getItem('sigmaNotif.types') ?? '{}');
      setSound(!!s);
      setTypes(prev => ({ ...prev, ...t }));
    } catch {/* noop */}
  };

  const loadPrefsBackend = async () => {
    const user = localStorage.getItem("userId");
    const role = localStorage.getItem("role");
    if (!user || role !== 'professor') { loadPrefsLocal(); return; }
    setLoadingPrefs(true);
    try {
      const resp = await fetch(`${BACKEND_URL}/notifications/prefs/${user}`, {
        headers: { 'Authorization': localStorage.getItem('token') }
      });
      if (resp.ok) {
        const data = await resp.json();
        setSound(!!data.enableSound);
        setTypes(prev => ({ ...prev, PROGRESS_UPDATE: data.enableProgressUpdate, COMPLETED: data.enableCompleted, OVERDUE: data.enableOverdue, DUE_SOON: prev.DUE_SOON }));
        // Sincronizar a localStorage como cache
        localStorage.setItem('sigmaNotif.sound', JSON.stringify(!!data.enableSound));
        localStorage.setItem('sigmaNotif.types', JSON.stringify({ PROGRESS_UPDATE: data.enableProgressUpdate, COMPLETED: data.enableCompleted, OVERDUE: data.enableOverdue, DUE_SOON: types.DUE_SOON }));
      } else {
        loadPrefsLocal();
      }
    } catch (e) {
      console.warn('Fallo carga prefs backend, uso cache local', e);
      loadPrefsLocal();
    } finally {
      setLoadingPrefs(false);
    }
  };

  const savePrefs = async () => {
    localStorage.setItem('sigmaNotif.sound', JSON.stringify(!!sound));
    localStorage.setItem('sigmaNotif.types', JSON.stringify(types));
    const user = localStorage.getItem('userId');
    const role = localStorage.getItem('role');
    if (user && role === 'professor') {
      try {
        await fetch(`${BACKEND_URL}/notifications/prefs/${user}`, {
          method: 'PUT',
          headers: { 'Content-Type':'application/json', 'Authorization': localStorage.getItem('token') },
          body: JSON.stringify({
            enableProgressUpdate: !!types.PROGRESS_UPDATE,
            enableCompleted: !!types.COMPLETED,
            enableOverdue: !!types.OVERDUE,
            enableSound: !!sound
          })
        });
      } catch(e){ console.warn('No se pudo guardar preferencias backend', e); }
    }
    setSaved(true);
    setTimeout(() => setSaved(false), 1200);
  };

  useEffect(() => {
    const fetchActivities = async () => {
      const user = localStorage.getItem("userId");
      const role = localStorage.getItem("role");

      if (!user || !role) {
        console.error("Usuario o rol no encontrado en localStorage");
        return;
      }

      try {
        const prefTypes = JSON.parse(localStorage.getItem('sigmaNotif.types') || '{}');
        const soundPref = JSON.parse(localStorage.getItem('sigmaNotif.sound') || 'true');

        if (role === 'professor') {
          // Usar backend de notificaciones para profesores
          const response = await fetch(`${BACKEND_URL}/notifications/unread/${user}`,{
            method: 'GET',
            headers: { 'Content-Type': 'application/json' ,
                'Authorization':localStorage.getItem('token')
            },
          });
          if (!response.ok) throw new Error("Error al obtener notificaciones");
          const notifications = await response.json();
          let mapped = notifications.map(n => ({ id: n.id, type: n.type, message: n.message, date: new Date(n.createdAt).toLocaleDateString(), activityId: n.activityId }));
          mapped = mapped.filter(n => prefTypes[n.type] !== false);
          if (soundPref && alerts.length && mapped.length > alerts.length) {
            try { new Audio('https://actions.google.com/sounds/v1/alarms/beep_short.ogg').play(); } catch(_){}
          }
          setAlerts(mapped);
        } else {
          // Fallback: lógica local para monitores y otros roles
          const response = await fetch(`${BACKEND_URL}/activity/findAll/${user}/${role}`,{
            method: 'GET',
            headers: { 'Content-Type': 'application/json' ,
                'Authorization':localStorage.getItem('token')
            },
          });
          if (!response.ok) throw new Error("Error al obtener actividades");
          const activities = await response.json();
          const today = new Date();
          const daysThreshold = 1; // 1 día para la fecha límite
          const upcomingAlerts = activities
            .filter(activity => activity.finish && activity.state === "PENDIENTE")
            .map(activity => {
              const finishDate = new Date(activity.finish);
              const diffInDays = Math.ceil((finishDate - today) / (1000 * 60 * 60 * 24));
              return diffInDays > 0 && diffInDays <= daysThreshold
                ? { id: activity.id, type: 'DUE_SOON', message: `La actividad "${activity.name}" vence en ${diffInDays} día`, date: finishDate.toLocaleDateString(), activityId: activity.id }
                : null;
            })
            .filter(Boolean);
          const filtered = upcomingAlerts.filter(n => prefTypes[n.type] !== false);
          if (soundPref && alerts.length && filtered.length > alerts.length) {
            try { new Audio('https://actions.google.com/sounds/v1/alarms/beep_short.ogg').play(); } catch(_){}
          }
          setAlerts(filtered);
        }
      } catch (error) {
        console.error("Error al obtener actividades:", error);
      }
    };

    fetchActivities();
    fetchChatAlerts();
    // Activar polling solo si NO hay SSE activo (o no aplica para el rol)
    let interval;
    const roleNow = localStorage.getItem('role');
    const sseActive = !!eventSourceRef.current;
    if (!(roleNow === 'professor' && sseActive)) {
      interval = setInterval(fetchActivities, 60000); // fallback polling
    }
    const chatInterval = setInterval(fetchChatAlerts, 10000);

    return () => {
      if (interval) clearInterval(interval);
      clearInterval(chatInterval);
    };
  }, [types, sseEnabled]);

  // SSE subscription for professor real-time
  useEffect(() => {
    const user = localStorage.getItem('userId');
    const role = localStorage.getItem('role');
    if (!sseEnabled || role !== 'professor' || !user) return;
    if (eventSourceRef.current) return; // evitar doble conexión
    try {
      const es = new EventSource(`${BACKEND_URL}/notifications/stream/${user}`);
      eventSourceRef.current = es;
      es.onmessage = (e) => {
        try {
          const data = JSON.parse(e.data);
          if (!types[data.type]) return; // filtrar tipo
          setAlerts(prev => {
            // evitar duplicados si ya está
            if (prev.find(p => p.id === data.id)) return prev;
            const mapped = { id: data.id, type: data.type, message: data.message, date: new Date(data.createdAt).toLocaleDateString(), activityId: data.activityId };
            // sonido
            const soundPref = JSON.parse(localStorage.getItem('sigmaNotif.sound') || 'true');
            if (soundPref && prev.length) {
              try { new Audio('https://actions.google.com/sounds/v1/alarms/beep_short.ogg').play(); } catch(_){ }
            }
            return [mapped, ...prev];
          });
        } catch { /* noop */ }
      };
      es.onerror = () => {
        console.warn('SSE error, desactivando SSE y usando polling');
        setSseEnabled(false);
        es.close();
        eventSourceRef.current = null;
      };
    } catch (e) {
      console.warn('No se pudo iniciar SSE', e);
      setSseEnabled(false);
    }
    return () => { if (eventSourceRef.current) { eventSourceRef.current.close(); eventSourceRef.current = null; } };
  }, [sseEnabled, types]);

  // Cargar preferencias backend al abrir ajustes

  const displayedAlerts = [...chatAlerts, ...alerts];

  return (
    <div className="notification-container">
      <div className="bell-wrapper" title="Notificaciones" onClick={() => setShowNotifications(!showNotifications)}>
        <Bell size={30} className="bell-icon" />
        {displayedAlerts.length > 0 && <span className="notification-badge">{displayedAlerts.length}</span>}
      </div>

      {showNotifications && (
        <div className="custom-card">
          <div style={{ display:'flex', alignItems:'center', justifyContent:'space-between' }}>
            <h3 className="card-title" style={{ margin:0 }}>{showSettings ? 'Preferencias' : 'Notificaciones'}</h3>
            <button
              style={{ background:'transparent', border:'none', color:'#5454e9', cursor:'pointer', fontWeight:600 }}
              onClick={() => { const ns = !showSettings; setShowSettings(ns); if(ns) loadPrefsBackend(); }}
            >{showSettings ? '← Atrás' : '⚙ Preferencias'}</button>
          </div>

          {!showSettings && (
            <>
              {displayedAlerts.length > 0 ? (
                <ul className="notification-list">
                  {/* Acciones generales */}
                  <div style={{ display:'flex', justifyContent:'flex-end', padding:'0 6px 8px 6px' }}>
                    {localStorage.getItem('role') === 'professor' && (
                      <button
                        className="save-button-act"
                        style={{ padding:'4px 8px' }}
                        onClick={async (e) => {
                          e.preventDefault();
                          try {
                            const user = localStorage.getItem('userId');
                            await fetch(`${BACKEND_URL}/notifications/read-all/${user}`, {
                              method: 'PUT',
                              headers: { 'Content-Type': 'application/json', 'Authorization': localStorage.getItem('token') }
                            });
                            setAlerts([]);
                          } catch(err) { console.warn('No se pudo marcar todas como leídas', err); }
                        }}
                      >Marcar todas como leídas</button>
                    )}
                  </div>
                  {displayedAlerts.map(alert => (
                    <li key={alert.id} className="notification-item">
                      <div className="notification-box" onClick={async () => {
                          try {
                            // Marcar como leída si viene del backend
                            const role = localStorage.getItem('role');
                            if (role === 'professor' && alert.type !== 'CHAT') {
                              await fetch(`${BACKEND_URL}/notifications/${alert.id}/read`, {
                                method: 'PUT',
                                headers: { 'Content-Type': 'application/json' , 'Authorization':localStorage.getItem('token') }
                              });
                            }
                          } catch (e) { /* noop */ }
                          if (alert.type === 'CHAT' && alert.conversationId) {
                            const seenMap = loadChatSeenMap();
                            seenMap[alert.conversationId] = new Date().toISOString();
                            saveChatSeenMap(seenMap);
                            setChatAlerts(prev => prev.filter(a => a.id !== alert.id));
                            localStorage.setItem('focusChatConversationId', alert.conversationId);
                            navigate('/chat');
                            setShowNotifications(false);
                            return;
                          }
                          if (alert.activityId) {
                            localStorage.setItem('focusActivityId', String(alert.activityId));
                          }
                          navigate('/Task');
                          setShowNotifications(false);
                        }}>
                        <p>{alert.message}</p>
                        <span className="notification-date">{alert.date}</span>
                      </div>
                    </li>
                  ))}
                </ul>
              ) : (
                <p>No hay notificaciones pendientes</p>
              )}
            </>
          )}

          {showSettings && (
            <form onSubmit={(e) => { e.preventDefault(); savePrefs(); }} style={{ marginTop: 10 }}>
              {loadingPrefs && <div style={{ marginBottom:8, fontSize:12 }}>Cargando preferencias...</div>}
              <div style={{ marginBottom: 10 }}>
                <label style={{ display:'flex', gap:8, alignItems:'center' }}>
                  <input type="checkbox" checked={sound} onChange={() => setSound(!sound)} />
                  Activar sonido en nuevas notificaciones
                </label>
              </div>
              <div style={{ fontWeight:600, margin:'12px 0 8px 0' }}>Tipos a mostrar</div>
              <div style={{ display:'grid', gridTemplateColumns:'1fr 1fr', gap:8 }}>
                <label style={{ display:'flex', gap:8, alignItems:'center' }}>
                  <input type="checkbox" checked={!!types.PROGRESS_UPDATE} onChange={() => setTypes(prev => ({ ...prev, PROGRESS_UPDATE: !prev.PROGRESS_UPDATE }))} />
                  Progreso
                </label>
                <label style={{ display:'flex', gap:8, alignItems:'center' }}>
                  <input type="checkbox" checked={!!types.COMPLETED} onChange={() => setTypes(prev => ({ ...prev, COMPLETED: !prev.COMPLETED }))} />
                  Completadas
                </label>
                <label style={{ display:'flex', gap:8, alignItems:'center' }}>
                  <input type="checkbox" checked={!!types.OVERDUE} onChange={() => setTypes(prev => ({ ...prev, OVERDUE: !prev.OVERDUE }))} />
                  Atrasos
                </label>
                <label style={{ display:'flex', gap:8, alignItems:'center' }}>
                  <input type="checkbox" checked={!!types.DUE_SOON} onChange={() => setTypes(prev => ({ ...prev, DUE_SOON: !prev.DUE_SOON }))} />
                  Próximas a vencer
                </label>
                <label style={{ display:'flex', gap:8, alignItems:'center' }}>
                  <input type="checkbox" checked={!!types.CHAT} onChange={() => setTypes(prev => ({ ...prev, CHAT: !prev.CHAT }))} />
                  Chat
                </label>
              </div>
              <div style={{ display:'flex', justifyContent:'flex-end', marginTop: 12 }}>
                <button type="submit" className="save-button-act">Guardar</button>
              </div>
              {saved && <div style={{ marginTop:8, color:'green' }}>Preferencias guardadas</div>}
              {!saved && !loadingPrefs && <div style={{ marginTop:8, fontSize:11, color:'#88898c' }}>Se guardan también en el backend.</div>}
            </form>
          )}
        </div>
      )}
    </div>
  );
};

export default NotificationIcon;
