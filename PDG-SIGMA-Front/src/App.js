import './App.css';
import React from 'react';
import Login from './Login'; 
import Task from './Task'; 
import { BrowserRouter as Router, Route, Routes, useLocation, Navigate } from 'react-router-dom';
import CreateActivity from './CreateActivity';
import Profile from './Profile';
import Reports from './Reports';
import GenerateSimonFile from './GenerateSimonFile';
import { useEffect } from "react";
import NotificationSettings from './NotificationSettings';
import EvaluarMonitoresHU015 from './EvaluarMonitoresHU015';
import MisEvaluacionesHU015 from './MisEvaluacionesHU015';

// HU-010: Componentes para el flujo de convocatorias (nuevo flujo único)
import CreateConvocatoria from './CreateConvocatoria';
import VerConvocatorias from './VerConvocatorias';
import SeleccionarMonitor from './SeleccionarMonitor';
import AprobarMonitoriasHU010 from './AprobarMonitoriasHU010';

function App() {
  // Hook para obtener la ruta actual
  const location = useLocation();
  
  useEffect(() => {
      return () => {
          localStorage.clear(); // Se borra el localStorage al desmontar el frontend
      };
    }, []);
  return (
    <div className="App">
      <Routes>
        {/* Ruta predeterminada - Redirige a Login */}
        <Route path="/" element={<Navigate to="/Login" replace />} />

        {/* Ruta para Login */}
        <Route path="/Login" element={<Login />} />

        {/* Route for Task */}
        <Route path="/Task" element={<Task />} />

         {/* Route for Create Activity */}
         <Route path="/CreateActivity" element={<CreateActivity />} />

         {/* Route for Profile */}
         <Route path="/Profile" element={<Profile />} />

         {/* Route for Reports */}
         <Route path="/Reports" element={<Reports />} />

         {/* Route for Generate SIMON File */}
         <Route path="/GenerateSimonFile" element={<GenerateSimonFile />} />

         {/* HU-015: Evaluación de monitores */}
         <Route path="/evaluar-monitores" element={<EvaluarMonitoresHU015 />} />
         <Route path="/mis-evaluaciones" element={<MisEvaluacionesHU015 />} />

         {/* Notificaciones - Preferencias */}
         <Route path="/notification-settings" element={<NotificationSettings />} />

         {/* ========== HU-010: Rutas para Convocatorias de Monitoría ========== */}
         
         {/* Profesor: Crear Convocatoria */}
         <Route path="/crear-convocatoria" element={<CreateConvocatoria />} />
         
         {/* Estudiante: Ver y Postularse */}
         <Route path="/ver-convocatorias" element={<VerConvocatorias />} />
         
         {/* Profesor: Seleccionar Monitor de una Convocatoria */}
         <Route path="/seleccionar-monitor/:requestId" element={<SeleccionarMonitor />} />
         
         {/* Jefe de Departamento: Aprobar Monitorías HU-010 */}
         <Route path="/aprobar-monitorias-hu010" element={<AprobarMonitoriasHU010 />} />
      </Routes>
    </div>
  );
}

export default function WrappedApp() {
  return (
    <Router>
      <App />
    </Router>
  );
}


