import './App.css';
import React from 'react';
import Login from './Login'; 
import { BrowserRouter as Router, Route, Routes, useLocation, Navigate } from 'react-router-dom';
import CreateActivity from './CreateActivity';
import Profile from './Profile';
import Reports from './Reports';
import GenerateSimonFile from './GenerateSimonFile';
import NotificationSettings from './NotificationSettings';
import EvaluarMonitoresHU015 from './EvaluarMonitoresHU015';
import EvaluarSupervisorHU021 from './EvaluarSupervisorHU021';
import MisEvaluacionesHU015 from './MisEvaluacionesHU015';
import EvaluacionMonitoriaEstudiante from './EvaluacionMonitoriaEstudiante';
import GestionEncuestaMonitoresHU026 from './GestionEncuestaMonitoresHU026';
import GestionEncuestaProfesoresHU027 from './GestionEncuestaProfesoresHU027';
import Chat from './Chat';


// HU-010: Componentes para el flujo de convocatorias (nuevo flujo único)
import CreateConvocatoria from './CreateConvocatoria';
import MisConvocatorias from './MisConvocatorias';
import VerConvocatorias from './VerConvocatorias';
import SeleccionarMonitor from './SeleccionarMonitor';
import AprobarMonitoriasHU010 from './AprobarMonitoriasHU010';

// Componente para carga CSV de monitorías (flujo antiguo restaurado)
import CreateMonitoria from './CreateMonitoria';

// HU-011: Plan de Actividades
import PlanActividades from './PlanActividades';
import GestionRubricas from './GestionRubricas';

// HU-017: Vista Monitor - Plan de Actividades
import VistaMonitorActividades from './VistaMonitorActividades';

// HU-007: Cierre de Monitorías
import CerrarMonitorias from './CerrarMonitorias';

// HU2-273: Vista Monitor - Mis Postulaciones
import MisPostulaciones from './MisPostulaciones';

function App() {
  // Hook para obtener la ruta actual
  const location = useLocation();
  return (
    <div className="App">
      <Routes>
        {/* Ruta predeterminada - Redirige a Login */}
        <Route path="/" element={<Navigate to="/Login" replace />} />

        {/* Ruta para Login */}
        <Route path="/Login" element={<Login />} />

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
         {/* HU-021: Evaluación de profesor */}
         <Route path="/evaluar-supervisor" element={<EvaluarSupervisorHU021 />} />

         {/* HU-022: Evaluacion experiencia con monitores */}
         <Route path="/evaluacion-monitoria" element={<EvaluacionMonitoriaEstudiante />} />

         {/* HU-026: Gestion banco de preguntas de encuesta a estudiantes */}
         <Route path="/gestion-encuesta-monitores" element={<GestionEncuestaMonitoresHU026 />} />

         {/* HU-027: Gestion banco de preguntas de encuesta a profesores */}
         <Route path="/gestion-encuesta-profesores" element={<GestionEncuestaProfesoresHU027 />} />

         {/* Notificaciones - Preferencias */}
         <Route path="/notification-settings" element={<NotificationSettings />} />

         {/* Chat interno (UI) */}
         <Route path="/chat" element={<Chat />} />

         {/* ========== HU-010: Rutas para Convocatorias de Monitoría ========== */}
         
         {/* Profesor: Crear Convocatoria */}
         <Route path="/crear-convocatoria" element={<CreateConvocatoria />} />
         
         {/* Profesor: Ver Mis Convocatorias */}
         <Route path="/mis-convocatorias" element={<MisConvocatorias />} />
         
         {/* Estudiante: Ver y Postularse */}
         <Route path="/ver-convocatorias" element={<VerConvocatorias />} />
         
         {/* Profesor: Seleccionar Monitor de una Convocatoria */}
         <Route path="/seleccionar-monitor/:requestId" element={<SeleccionarMonitor />} />
         
         {/* Jefe de Departamento: Aprobar Convocatorias */}
         <Route path="/aprobar-monitorias-hu010" element={<AprobarMonitoriasHU010 />} />

         {/* ========== Carga CSV de Monitorías ========== */}
         {/* EXCEPCIÓN: Solo para Jefe de Departamento - Crear monitorías directamente sin convocatoria */}
         <Route path="/crear-monitoria" element={<CreateMonitoria />} />

         {/* ========== HU-011: Plan de Actividades ========== */}
         {/* Profesor: Gestionar plan de actividades - puede navegar con contexto o sin él */}
         <Route path="/plan-actividades/:monitoringId?" element={<PlanActividades />} />
         
         {/* Profesor: Gestionar rúbricas de evaluación */}
         <Route path="/gestion-rubricas" element={<GestionRubricas />} />

         {/* ========== HU-017: Vista Monitor - Plan de Actividades ========== */}
         {/* Monitor: Ver actividades asignadas por profesores */}
         <Route path="/mis-actividades" element={<VistaMonitorActividades />} />

         {/* ========== HU2-273: Mis Postulaciones ========== */}
         {/* Monitor: Ver el estado de sus postulaciones a convocatorias */}
         <Route path="/mis-postulaciones" element={<MisPostulaciones />} />

         {/* ========== HU-007: Cierre de Monitorías ========== */}
         {/* Jefe de Departamento: Cerrar monitorías al final del periodo */}
         <Route path="/cerrar-monitorias" element={<CerrarMonitorias />} />
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


