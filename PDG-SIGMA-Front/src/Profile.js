import "./Profile.css";
import React, { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import VerticalNavbar from "./VerticalNavbar";
import profilePic from "./img/profile-pic.png";
import UpdateButton from "./UpdateButton"; 
import { BACKEND_URL, getApiUrl } from './config/ApiBackend';

function Profile() {
  console.log("Profile se está renderizando");
  const navigate = useNavigate();
  const [user, setUser] = useState(null); 
  const [cursosAsignados, setCursosAsignados] = useState([]);

useEffect(() => {
        const id = localStorage.getItem('userId')
        const role = localStorage.getItem('role')
        if(role === 'professor'){
          fetch(`${BACKEND_URL}/professor/profile/${id}`,{
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
                  setUser(data)
              } else {
                  console.error("No data.");
              }
          })
          .catch(error => console.error('Error fetching faculty data:', error));
        }
        else if(role === 'monitor'){
          fetch(`${BACKEND_URL}/monitor/profile/${id}`,{
            method: 'GET',
            headers: { 'Content-Type': 'application/json' ,
                'Authorization':localStorage.getItem('token')
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
                  setUser(data)
              } else {
                  console.error("No data.");
              }
          })
          .catch(error => console.error('Error fetching faculty data:', error));
        }
        else{
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
                  setUser(data)
              } else {
                  console.error("No data.");
              }
          })
          .catch(error => console.error('Error fetching faculty data:', error));
        }
        
        
           fetch(`${BACKEND_URL}/monitoring/profile/${id}/${role}`,{
            method: 'GET',
            headers: { 'Content-Type': 'application/json' ,
                'Authorization':localStorage.getItem('token')
            },
            })
            .then(res => {
                if (!res.ok) {
                    throw new Error(`HTTP error! Status: ${res.json()}`);
                }
                return res.json();
            })
            .then(data => {
                if (data) {
                    setCursosAsignados(data); 
                } else {
                    console.error("Data format is incorrect or 'monitoria' is empty.");
                }
            })
            .catch(error => console.error('Error fetching data:', error));
    }, []);

  const [role, setRole] = useState("");
  const [userId, setUserId] = useState("");

  useEffect(() => {
    const storedRole = localStorage.getItem("role");
    const storedId = localStorage.getItem("userId");
    setRole(storedRole || "");
    setUserId(storedId || "");
  }, []);

  const [semestreSeleccionado, setSemestreSeleccionado] = useState("Seleccionar periodo");

  const semestresDisponibles = [
    "Seleccionar periodo",
    ...new Set(cursosAsignados? cursosAsignados.map((curso) => curso.semester): []),
  ];

  const cursosFiltrados =
    semestreSeleccionado === "Seleccionar periodo"
      ? cursosAsignados
      : cursosAsignados.filter((curso) => curso.semester === semestreSeleccionado);

  return (
    <div className="profile-container">
      <VerticalNavbar />
      {(role === "professor" || role === "jfedpto") && (
        <UpdateButton role={role} userId={userId} />
      )}

      {/* Contenedor de perfil */}
      <div className="profile-content">

        {/* Encabezado institucional */}
        <div className="profile-page-header">
          <h1>Mi Perfil</h1>
          <p>Información personal y cursos asignados</p>
        </div>

        <div className="profile-main">
          {/* Tarjeta de usuario */}
          <div className="profile-card">
            <div className="profile-card-header">
              <img src={profilePic} alt="Foto de perfil" className="profile-pic" />
              {user ? <h2>{user.name}</h2> : <h2>Cargando...</h2>}
            </div>
            {user && (
              <div className="profile-card-body">
                <div className="profile-info-row">
                  <span className="profile-info-label">Facultad</span>
                  <span className="profile-info-value">{user.school}</span>
                </div>
                <div className="profile-info-row">
                  <span className="profile-info-label">Programa</span>
                  <span className="profile-info-value">{user.program}</span>
                </div>
                <div className="profile-info-row">
                  <span className="profile-info-label">Rol</span>
                  <span className="profile-info-value">{user.rol}</span>
                </div>
              </div>
            )}
          </div>

          {/* Contenedor de cursos asignados */}
          <div className="courses-container">
            <div className="courses-container-header">
              <h3>Cursos Asignados</h3>
            </div>
            <div className="courses-container-body">
              {/* Filtro de semestre */}
              <div className="filter-container">
                <select className="select-semester-filter"
                  id="semestre"
                  value={semestreSeleccionado}
                  onChange={(e) => setSemestreSeleccionado(e.target.value)}
                >
                  {semestresDisponibles.map((semestre) => (
                    <option key={semestre} value={semestre}>{semestre}</option>
                  ))}
                </select>
              </div>

              <table className="courses-table">
                <thead>
                  <tr>
                    <th>Periodo</th>
                    <th>Curso</th>
                    {role === "professor" && <th>Monitor Asignado</th>}
                    {role === "monitor" && <th>Profesor Asignado</th>}
                    {role === "jfedpto" && (
                      <>
                        <th>Profesor Asignado</th>
                        <th>Monitor Asignado</th>
                      </>
                    )}
                    {role === "professor" && <th>Acciones</th>}
                  </tr>
                </thead>
                <tbody>
                  {cursosFiltrados.map((curso) => (
                    <tr key={curso.id? curso.id:"N/A"}>
                      <td>{curso.semester? curso.semester:"N/A"}</td>
                      <td>{curso.courseName? curso.courseName:"N/A"}</td>
                      {role === "professor" && <td>{curso.monitor? curso.monitor: "No hay monitores"}</td>}
                      {role === "monitor" && <td>{curso.monitor? curso.monitor:"N/A"}</td>}
                      {role === "jfedpto" && (
                        <>
                          <td>{curso.professorName? curso.professorName:"N/A"}</td>
                          <td>{curso.monitor? curso.monitor: "No hay monitores"}</td>
                        </>
                      )}
                      {role === "professor" && (
                        <td>
                          {curso.id && curso.monitor !== "No hay monitores" ? (
                            <button
                              className="btn-plan-actividades"
                              onClick={() => navigate(`/plan-actividades/${curso.id}`)}
                              title="Ver plan de actividades"
                            >
                              Plan
                            </button>
                          ) : (
                            <span style={{color: '#88898c', fontSize: '0.9em'}}>-</span>
                          )}
                        </td>
                      )}
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}

export default Profile;

