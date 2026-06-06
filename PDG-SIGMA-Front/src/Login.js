import './Login.css';
import React, { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import Alert from './Alert';
import { BACKEND_URL } from './config/ApiBackend';

function Login() {
    const navigate = useNavigate();
    const [password, setPassword] = useState('')
    const [userId, setUserId] = useState('')
    const [errorMessage, setErrorMessage] = useState('');
    const [showLoginAlert, setShowLoginAlert] = useState(false);
    const [loginAlertMessage, setLoginAlertMessage] = useState('Iniciaste sesión como estudiante');
    const [isCapsLockOn, setIsCapsLockOn] = useState(false);
    const [showPassword, setShowPassword] = useState(false);

    const getLoginMessageByRole = (role) => {
      if (role === 'professor') {
        return 'Iniciaste sesión como profesor';
      }
      if (role === 'jfedpto') {
        return 'Iniciaste sesión como jefe de departamento';
      }
      if (role === 'monitor' || role === 'student') {
        return 'Iniciaste sesión como estudiante';
      }
      return 'Iniciaste sesión';
    };

    const handleChangeUser = (event) => {
      setUserId(event.target.value);
    };
  
    const handleChangePassword = (event) => {
      setPassword(event.target.value);
    };

    const handlePasswordKeyState = (event) => {
      if (typeof event.getModifierState === 'function') {
        setIsCapsLockOn(event.getModifierState('CapsLock'));
      }
    };

    const handleLoginClick = async(event) => {
      event.preventDefault(); 
      const data = {
        userId:userId,
        password:password
      }
      console.log('Data to send:', data);

      const apiUrl = `${BACKEND_URL}/auth/login`;
      
      try {
        const response = await fetch(apiUrl, {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json'
          },
          body: JSON.stringify(data),
        });
        console.log(response);
        if(response.ok){
          localStorage.setItem('userId',userId)
          setErrorMessage('');
          
          const res =  await response.json()
          console.log(res.role)
          setLoginAlertMessage(getLoginMessageByRole(res.role));
          setShowLoginAlert(true);

          if(res.role === 'professor'){
            localStorage.setItem('role',res.role)
            localStorage.setItem('token',`Bearer ${res.token}`)
            setTimeout(() => navigate('/mis-convocatorias'), 700);// Redirige a ver sus convocatorias
          }
          else if(res.role === 'monitor') {
            localStorage.setItem('role',res.role)
            localStorage.setItem('token',`Bearer ${res.token}`)
            setTimeout(() => navigate('/ver-convocatorias'), 700)// Redirige a convocatorias abiertas
          }
          else if(res.role === 'jfedpto') {
            localStorage.setItem('role',res.role)
            localStorage.setItem('token',`Bearer ${res.token}`)
            setTimeout(() => navigate('/aprobar-monitorias-hu010'), 700)// Redirige a aprobar monitorías
          }
          else{
            localStorage.setItem('role',res.role)
            localStorage.setItem('token',`Bearer ${res.token}`)
            setTimeout(() => navigate('/ver-convocatorias'), 700)// Redirige a ver convocatorias abiertas
          }
        }else{
          console.log("Can't find it")
          setErrorMessage('Contraseña o usuario inválido');
        }

      }catch (error){
        console.error('Error fetching data:', error);
        }
    };

    useEffect(() => {
        // Añadir clase al body cuando el componente Login se monta
        document.body.classList.add('login-background');
        
        // Remover la clase cuando el componente Login se desmonta
        return () => {
          document.body.classList.remove('login-background');
        };
      }, []);

  return (
    <div className="login-page">
      <Alert
        show={showLoginAlert}
        onClose={() => setShowLoginAlert(false)}
        message={loginAlertMessage}
      />

      <main className="login-layout">
        <section className="login-hero" aria-hidden="true">
          <div className="login-hero-overlay" />
          <div className="login-hero-caption">
            <p>Llega mas lejos</p>
            <a href="https://www.icesi.edu.co" target="_blank" rel="noreferrer">
              icesi.edu.co
            </a>
          </div>
        </section>

        <section className="login-panel">
          <div className="login-panel-header">
            <a href="https://banner.icesi.edu.co" target="_blank" rel="noreferrer">
              Guia Banner
            </a>
          </div>

          <div className="main-login">
            <h2>Inicia sesión</h2>

            <form className="login-form" onSubmit={handleLoginClick}>
              <div className="field-group">
                <label htmlFor="user">Usuario</label>
                <input
                  id="user"
                  className="inputs-login"
                  type="text"
                  name="user"
                  value={userId}
                  onChange={handleChangeUser}
                  autoComplete="username"
                  required
                />
              </div>

              <div className="field-group">
                <label htmlFor="pswd">Contraseña</label>
                <div className="password-wrapper">
                  <input
                    id="pswd"
                    className="inputs-login password-input"
                    type={showPassword ? 'text' : 'password'}
                    name="pswd"
                    value={password}
                    onChange={handleChangePassword}
                    onKeyUp={handlePasswordKeyState}
                    onKeyDown={handlePasswordKeyState}
                    onBlur={() => setIsCapsLockOn(false)}
                    autoComplete="current-password"
                    required
                  />
                  <button
                    type="button"
                    className="password-toggle-btn"
                    onClick={() => setShowPassword((prev) => !prev)}
                    aria-label={showPassword ? 'Ocultar contraseña' : 'Mostrar contraseña'}
                    title={showPassword ? 'Ocultar contraseña' : 'Mostrar contraseña'}
                  >
                    {showPassword ? (
                      <svg viewBox="0 0 24 24" aria-hidden="true" focusable="false">
                        <path d="M3 4.27L4.28 3 21 19.72 19.73 21l-2.09-2.08A11.38 11.38 0 0 1 12 20C7 20 2.73 16.89 1 12c.72-2.03 1.97-3.83 3.58-5.28L3 4.27zm5.5 5.5A3.5 3.5 0 0 0 12 15.5c.53 0 1.04-.12 1.49-.33l-4.66-4.66c-.21.45-.33.96-.33 1.49zm3.47-5.74A11.58 11.58 0 0 1 12 4c5 0 9.27 3.11 11 8a11.82 11.82 0 0 1-3.35 4.67l-2.38-2.38A3.5 3.5 0 0 0 12.5 9.7l-2.82-2.82a11.87 11.87 0 0 1 2.29-1.85z" />
                      </svg>
                    ) : (
                      <svg viewBox="0 0 24 24" aria-hidden="true" focusable="false">
                        <path d="M12 4c5 0 9.27 3.11 11 8-1.73 4.89-6 8-11 8S2.73 16.89 1 12c1.73-4.89 6-8 11-8zm0 3a5 5 0 1 0 0 10 5 5 0 0 0 0-10zm0 2.2a2.8 2.8 0 1 1 0 5.6 2.8 2.8 0 0 1 0-5.6z" />
                      </svg>
                    )}
                  </button>
                </div>
              </div>

              {isCapsLockOn && (
                <p className="caps-lock-warning">Bloq Mayus esta activado</p>
              )}

              {errorMessage && (
                <p className="login-error-message">{errorMessage}</p>
              )}

              <button className="login-btn-login" id="login-btn-login" type="submit">
                Iniciar sesión
              </button>
            </form>

            <div className="login-footer">
              <p>Universidad Icesi, Calle 18 No. 122-135</p>
              <p>Cali-Colombia | Telefono: 555 2334 | Fax: 555 1441</p>
              <p>
                Copyright c 2026{' '}
                <a href="https://www.icesi.edu.co" target="_blank" rel="noreferrer">
                  www.icesi.edu.co
                </a>
              </p>
            </div>
          </div>
        </section>
      </main>
    </div>
  );
}

export default Login;

