import React, { useState, useEffect } from "react";
import { NavLink } from "react-router-dom";
import logo from "../src/img/logo2.png";
import {PopUp} from "./PopUp";
import "./VerticalNavbar.css";
import { BACKEND_URL } from './config/ApiBackend';
import NotificationIcon from './NotificationIcon';
import {
  Megaphone,
  Star,
  MessageSquare,
  FileText,
  ClipboardList,
  ClipboardCheck,
  Plus,
  BarChart3,
  PieChart,
  UserCheck,
  FolderInput,
  CheckSquare,
  Lock,
  ChevronLeft,
  ChevronRight,
  LogOut
} from 'lucide-react';

function VerticalNavbar() {
  const navIconProps = {
    size: 18,
    strokeWidth: 2,
    strokeLinecap: 'butt',
    strokeLinejoin: 'miter',
    style: { marginRight: 8, verticalAlign: 'text-bottom' }
  };

  const [role, setRole] = useState("");
  const [user, setUser] = useState("");
  const [initials, setInitials] = useState("");
  const [showProfileOption, setShowProfileOption] = useState(true);
  const [isOpen, setIsOpen] = useState(false)
  const [message, setMessage] = useState("")
  const [change, setChange] = useState(false)
  const [isHoverExpanded, setIsHoverExpanded] = useState(false);
  const [isMobileView, setIsMobileView] = useState(() => window.innerWidth <= 768);
  const [isMobileOpen, setIsMobileOpen] = useState(false);

  const isExpanded = isMobileView ? isMobileOpen : isHoverExpanded;

  const handleClose = () =>{
      setIsOpen(!isOpen)
      setChange(!change)
  }

  useEffect(() => {
    let id = localStorage.getItem('userId')
    let roleS = localStorage.getItem('role')
    if (!id || !roleS) {
      setRole(roleS || "");
      return;
    }
    setRole(roleS);
    console.log(roleS);
    let nameToUse = "";
        if(roleS === 'professor'){
          console.log('Inside professor');
          fetch(`${BACKEND_URL}/professor/profile/${id}`,{
            method: 'GET',
            headers: { 'Content-Type': 'application/json' ,
                'Authorization': localStorage.getItem('token')
            },
            })
          .then(res => {
              if (!res.ok) {
                const responseData = res.json();
                throw new Error(`HTTP error! Status: ${responseData}`);
                  
              }
              return res.json();
          })
          .then(data => {
              if (data) {
                console.log('Inside professor2');
                getInitials(data.name);
                  setUser(data)
              } else {
                  console.error("No data.");
              }
          })
          .catch(error => console.error('Error fetching faculty data:', error));
        }
        else if(roleS === 'monitor'){
          fetch(`${BACKEND_URL}/monitor/profile/${id}`,{
            method: 'GET',
            headers: { 'Content-Type': 'application/json' ,
                Authorization:localStorage.getItem('token')
            },
            })
          .then(res => {
              if (!res.ok) {
                const responseData = res.json();
                console.log(responseData)
                throw new Error(`HTTP error! Status: ${responseData}`);
                  
              }
              return res.json();
          })
          .then(data => {
              if (data) {
                getInitials(data.name);
                setUser(data)
              } else {
                  console.error("No data.");
              }
          })
          .catch(error => console.error('Error fetching faculty data:', error));
        }
        else if(roleS === 'jfedpto'){
          fetch(`${BACKEND_URL}/department-head/profile/${id}`,{
            method: 'GET',
            headers: { 'Content-Type': 'application/json' ,
                'Authorization':localStorage.getItem('token')
            },
            })
          .then(res => {
              if (!res.ok) {
                const responseData = res.json();
                throw new Error(`HTTP error! Status: ${responseData}`);
                  
              }
              return res.json();
          })
          .then(data => {
              if (data) {
                getInitials(data.name);
                setUser(data)
              } else {
                  console.error("No data.");
              }
          })
          .catch(error => console.error('Error fetching faculty data:', error));
        }
  }, []);

  useEffect(() => {
    const handleResize = () => {
      const mobile = window.innerWidth <= 768;
      setIsMobileView(mobile);
      if (!mobile) {
        setIsMobileOpen(false);
      }
    };

    window.addEventListener('resize', handleResize);
    return () => window.removeEventListener('resize', handleResize);
  }, []);

  const handleSidebarMouseEnter = () => {
    if (!isMobileView) {
      setIsHoverExpanded(true);
    }
  };

  const handleSidebarMouseLeave = () => {
    if (!isMobileView) {
      setIsHoverExpanded(false);
    }
  };

  const handleToggleMobile = () => {
    setIsMobileOpen((prev) => !prev);
  };

  const handleNavItemClick = () => {
    if (isMobileView) {
      setIsMobileOpen(false);
    }
  };

  function getInitials(name) {
    const nameParts = name.trim().split(" ");
          
          if (nameParts.length > 2) {
            const firstInitial = nameParts[0]?.charAt(0)?.toUpperCase() || '';
            const secondInitial = nameParts[2]?.charAt(0)?.toUpperCase() || '';
            setInitials(firstInitial + secondInitial);
          } else {
            const firstInitial = nameParts[0]?.charAt(0)?.toUpperCase() || '';
            const secondInitial = nameParts[1]?.charAt(0)?.toUpperCase() || '';
            setInitials(firstInitial + secondInitial);
          }
  }

  const handleCloseLogout = () => {
    setMessage("Has cerrado sesión exitosamente.")
    setIsOpen(!isOpen)
    setChange(!change)
    localStorage.setItem("role", "");
    localStorage.setItem("userId", "");
    if (isMobileView) {
      setIsMobileOpen(false);
    }
  };

  return (
    <>
      {isMobileView && (
        <button
          type="button"
          className="mobile-sidebar-toggle"
          onClick={handleToggleMobile}
          aria-label={isMobileOpen ? 'Cerrar menu lateral' : 'Abrir menu lateral'}
        >
          {isMobileOpen ? <ChevronLeft size={20} /> : <ChevronRight size={20} />}
        </button>
      )}

      <div
        className={`vertical-navbar ${isExpanded ? 'expanded' : 'collapsed'} ${isMobileView ? (isMobileOpen ? 'mobile-open' : 'mobile-closed') : ''}`}
        onMouseEnter={handleSidebarMouseEnter}
        onMouseLeave={handleSidebarMouseLeave}
      >
      <PopUp
          show={isOpen}
          onClose={() => handleClose()}
      >
          {message}
      </PopUp>
      {/* Logo */}
      <div className="navbar-logo-container">
        <NavLink to="/Profile" onClick={handleNavItemClick}>
          <img src={logo} alt="Universidad Icesi" className="navbar-logo" />
        </NavLink>
      </div>

      {/* Menú */}
      <div className="menu-items">
        {/* Campana de notificaciones visible en todas las páginas */}
        <div className="menu-notifications">
          <NotificationIcon />
        </div>
        {/* Avatar del usuario */}
        {(role === "monitor" || role === "professor" || role === "jfedpto") && (
          <NavLink
            to="/Profile"
            className="user-avatar"
            title={user ? `${user.name}` : ""}
            onClick={handleNavItemClick}
          >
            {initials}
          </NavLink>
        )}
        
        {/* Ver Convocatorias (solo para estudiantes que pueden postularse) */}
        {(role === "student" || role === "monitor") && (
          <NavLink
            to="/ver-convocatorias"
            className={({ isActive }) => (isActive ? "active" : "")}
            onClick={handleNavItemClick}
          >
            <Megaphone {...navIconProps} />
            <span className="nav-label">Convocatorias Abiertas</span>
          </NavLink>
        )}

        {(role === "student" || role === "monitor") && (
          <NavLink
            to="/evaluacion-monitoria"
            className={({ isActive }) => (isActive ? "active" : "")}
            onClick={handleNavItemClick}
          >
            <Star {...navIconProps} />
            <span className="nav-label">Evaluacion de monitoria</span>
          </NavLink>
        )}

        {/* Acceso para Monitor, Profesor y Jefe de Departamento */}
        {(role === "monitor" || role === "professor" || role === "jfedpto") && (
          <>
            {(role === "monitor" || role === "professor" || role === "jfedpto") && (
              <NavLink
                to="/chat"
                className={({ isActive }) => (isActive ? "active" : "")}
                onClick={handleNavItemClick}
              >
                <MessageSquare {...navIconProps} />
                <span className="nav-label">Chat</span>
              </NavLink>
            )}

            {role === "monitor" && (
              <>
                <NavLink
                  to="/evaluar-supervisor"
                  className={({ isActive }) => (isActive ? "active" : "")}
                  onClick={handleNavItemClick}
                >
                  <ClipboardCheck {...navIconProps} />
                  <span className="nav-label">Evaluar supervisor</span>
                </NavLink>
                <NavLink
                  to="/mis-actividades"
                  className={({ isActive }) => (isActive ? "active" : "")}
                  onClick={handleNavItemClick}
                >
                  <ClipboardList {...navIconProps} />
                  <span className="nav-label">Mis Actividades</span>
                </NavLink>
                <NavLink
                  to="/mis-evaluaciones"
                  className={({ isActive }) => (isActive ? "active" : "")}
                  onClick={handleNavItemClick}
                >
                  <Star {...navIconProps} />
                  <span className="nav-label">Mis evaluaciones</span>
                </NavLink>
                <NavLink
                  to="/mis-postulaciones"
                  className={({ isActive }) => (isActive ? "active" : "")}
                  onClick={handleNavItemClick}
                >
                  <FileText {...navIconProps} />
                  <span className="nav-label">Mis Postulaciones</span>
                </NavLink>
              </>
            )}
            {/* Preferencias ahora desde la campanita; ruta se mantiene pero ocultamos el link */}
          </>
        )}

        {/* Acceso exclusivo para Profesores */}
        {role === "professor" && (
          <>
            {/* HU-010: Crear Convocatoria (FLUJO PRINCIPAL) */}
            <NavLink
              to="/crear-convocatoria"
              className={({ isActive }) => (isActive ? "active" : "")}
              onClick={handleNavItemClick}
            >
              <Plus {...navIconProps} />
              <span className="nav-label">Crear Convocatoria</span>
            </NavLink>
            
            <NavLink
              to="/evaluar-monitores"
              className={({ isActive }) => (isActive ? "active" : "")}
              onClick={handleNavItemClick}
            >
              <UserCheck {...navIconProps} />
              <span className="nav-label">Evaluar Monitores</span>
            </NavLink>

            {/* HU-011: Gestión de Rúbricas */}
            <NavLink
              to="/gestion-rubricas"
              className={({ isActive }) => (isActive ? "active" : "")}
              onClick={handleNavItemClick}
            >
              <BarChart3 {...navIconProps} />
              <span className="nav-label">Gestion de Rubricas</span>
            </NavLink>

            <NavLink
              to="/mis-convocatorias"
              className={({ isActive }) => (isActive ? "active" : "")}
              onClick={handleNavItemClick}
            >
              <ClipboardList {...navIconProps} />
              <span className="nav-label">Mis Convocatorias</span>
            </NavLink>

            <NavLink
              to="/plan-actividades"
              className={({ isActive }) => (isActive ? "active" : "")}
              onClick={handleNavItemClick}
            >
              <ClipboardCheck {...navIconProps} />
              <span className="nav-label">Plan de Actividades</span>
            </NavLink>

            <NavLink
              to="/mis-evaluaciones"
              className={({ isActive }) => (isActive ? "active" : "")}
              onClick={handleNavItemClick}
            >
              <Star {...navIconProps} />
              <span className="nav-label">Mis evaluaciones</span>
            </NavLink>

          </>
        )}

        {/* Acceso exclusivo para Jefe de Departamento */}
        {role === "jfedpto" && (
          <>
            {/* EXCEPCIÓN: Jefe puede crear monitorías directamente con CSV (sin convocatoria) */}
            <NavLink
              to="/aprobar-monitorias-hu010"
              className={({ isActive }) => (isActive ? "active" : "")}
              onClick={handleNavItemClick}
            >
              <CheckSquare {...navIconProps} />
              <span className="nav-label">Aprobar Convocatorias</span>
            </NavLink>

            <NavLink
              to="/cerrar-monitorias"
              className={({ isActive }) => (isActive ? "active" : "")}
              onClick={handleNavItemClick}
            >
              <Lock {...navIconProps} />
              <span className="nav-label">Cerrar Monitorias</span>
            </NavLink>

            <NavLink
              to="/crear-monitoria"
              className={({ isActive }) => (isActive ? "active" : "")}
              onClick={handleNavItemClick}
            >
              <FolderInput {...navIconProps} />
              <span className="nav-label">Crear Monitorias CSV</span>
            </NavLink>

            <NavLink
              to="/GenerateSimonFile"
              className={({ isActive }) => (isActive ? "active" : "")}
              onClick={handleNavItemClick}
            >
              <FileText {...navIconProps} />
              <span className="nav-label">Generar Archivo SIMON</span>
            </NavLink>

            <NavLink
              to="/evaluacion-monitoria"
              className={({ isActive }) => (isActive ? "active" : "")}
              onClick={handleNavItemClick}
            >
              <BarChart3 {...navIconProps} />
              <span className="nav-label">Resultados monitoria</span>
            </NavLink>

            <NavLink
              to="/gestion-encuesta-monitores"
              className={({ isActive }) => (isActive ? "active" : "")}
              onClick={handleNavItemClick}
            >
              <ClipboardCheck {...navIconProps} />
              <span className="nav-label">Gestion encuesta monitores</span>
            </NavLink>

            <NavLink
              to="/gestion-encuesta-profesores"
              className={({ isActive }) => (isActive ? "active" : "")}
              onClick={handleNavItemClick}
            >
              <ClipboardList {...navIconProps} />
              <span className="nav-label">Gestion encuesta profesores</span>
            </NavLink>
          </>
        )}

        {(role === "professor" || role === "jfedpto") && (
          <>
            <NavLink
              to="/Reports"
              className={({ isActive }) => (isActive ? "active" : "")}
              onClick={handleNavItemClick}
            >
              <PieChart {...navIconProps} />
              <span className="nav-label">Reportes</span>
            </NavLink>
          </>
        )}
  
        {/* Botón de cerrar sesión */}
        <div className="logout-container">
          <NavLink
            to="/"
            className={({ isActive }) =>
              isActive ? "logout-button btn-danger active" : "logout-button btn-danger"
            }
            onClick={handleCloseLogout}
            aria-label="Cerrar sesion"
            title="Cerrar sesion"
          >
            <LogOut {...navIconProps} />
            <span className="nav-label">Cerrar sesion</span>
          </NavLink>
        </div>
      </div>
      </div>
      {isMobileView && isMobileOpen && <div className="navbar-backdrop" onClick={handleToggleMobile} />}
    </>
  );  
}

export default VerticalNavbar;
