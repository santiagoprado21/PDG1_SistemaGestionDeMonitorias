import React, { useEffect, useState } from 'react';
import VerticalNavbar from './VerticalNavbar';
import './CustomComponents.css';

const defaultTypes = {
  PROGRESS_UPDATE: true,
  COMPLETED: true,
  OVERDUE: true,
  DUE_SOON: true // usado para alertas locales de proximidad
};

const loadPrefs = () => {
  try {
    const sound = JSON.parse(localStorage.getItem('sigmaNotif.sound') ?? 'true');
    const types = JSON.parse(localStorage.getItem('sigmaNotif.types') ?? JSON.stringify(defaultTypes));
    return { sound, types: { ...defaultTypes, ...types } };
  } catch {
    return { sound: true, types: { ...defaultTypes } };
  }
};

const savePrefs = (prefs) => {
  localStorage.setItem('sigmaNotif.sound', JSON.stringify(!!prefs.sound));
  localStorage.setItem('sigmaNotif.types', JSON.stringify(prefs.types));
};

export default function NotificationSettings() {
  const [sound, setSound] = useState(true);
  const [types, setTypes] = useState(defaultTypes);
  const [saved, setSaved] = useState(false);

  useEffect(() => {
    const p = loadPrefs();
    setSound(p.sound);
    setTypes(p.types);
  }, []);

  const toggleType = (key) => {
    setTypes(prev => ({ ...prev, [key]: !prev[key] }));
  };

  const onSubmit = (e) => {
    e.preventDefault();
    savePrefs({ sound, types });
    setSaved(true);
    setTimeout(() => setSaved(false), 1500);
  };

  return (
    <div className="task-container">
      <VerticalNavbar />
      <div className="content">
        <div className="header">
          <div className="title-container" id="title-container">
            <div className="title" id="title">Preferencias de Notificaciones</div>
          </div>
        </div>

        <form onSubmit={onSubmit} style={{ maxWidth: 560, margin: '16px auto', background: '#ffffff', padding: 16, borderRadius: 12, boxShadow: '0 8px 22px -8px rgba(0,0,0,0.2)' }}>
          <div style={{ marginBottom: 14 }}>
            <label style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
              <input type="checkbox" checked={sound} onChange={() => setSound(!sound)} />
              Activar sonido en nuevas notificaciones
            </label>
          </div>

          <div style={{ fontWeight: 600, margin: '18px 0 10px 0' }}>Tipos a mostrar</div>
          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 10 }}>
            <label style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
              <input type="checkbox" checked={!!types.PROGRESS_UPDATE} onChange={() => toggleType('PROGRESS_UPDATE')} />
              Progreso (actualizaciones)
            </label>
            <label style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
              <input type="checkbox" checked={!!types.COMPLETED} onChange={() => toggleType('COMPLETED')} />
              Completadas
            </label>
            <label style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
              <input type="checkbox" checked={!!types.OVERDUE} onChange={() => toggleType('OVERDUE')} />
              Atrasos
            </label>
            <label style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
              <input type="checkbox" checked={!!types.DUE_SOON} onChange={() => toggleType('DUE_SOON')} />
              Próximas a vencer (local)
            </label>
          </div>

          <div style={{ display: 'flex', justifyContent: 'flex-end', marginTop: 18 }}>
            <button type="submit" className="save-button-act">Guardar</button>
          </div>

          {saved && <div style={{ marginTop: 10, color: 'green' }}>Preferencias guardadas</div>}
        </form>
      </div>
    </div>
  );
}
