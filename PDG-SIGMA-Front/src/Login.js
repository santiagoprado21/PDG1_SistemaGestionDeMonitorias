import './Login.css';
import React, { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import logo from './img/logo.png';
import Alert from './Alert';
import { BACKEND_URL, getApiUrl } from './config/ApiBackend';

function Login() {
    const navigate = useNavigate();
    const [password, setPassword] = useState('')
    const [userId, setUserId] = useState('')
    const [errorMessage, setErrorMessage] = useState('');
    const [showLoginAlert, setShowLoginAlert] = useState(false);
    const [loginAlertMessage, setLoginAlertMessage] = useState('Iniciaste sesion como estudiante');
    const [isCapsLockOn, setIsCapsLockOn] = useState(false);

    const getLoginMessageByRole = (role) => {
      if (role === 'professor') {
        return 'Iniciaste sesion como profesor';
      }
      if (role === 'jfedpto') {
        return 'Iniciaste sesion como jefe de departamento';
      }
      if (role === 'monitor' || role === 'student') {
        return 'Iniciaste sesion como estudiante';
      }
      return 'Iniciaste sesion';
    };

    const handleReturnClick = () => {
        navigate('/'); // Redirige a la página principal
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
    <div>
      <Alert
        show={showLoginAlert}
        onClose={() => setShowLoginAlert(false)}
        message={loginAlertMessage}
      />

      {/* Navigation Bar */}
      <nav className="navbar-login" id="navbar-login">
        <button className="return-btn-login" id="return-btn-login" onClick={handleReturnClick}> ← </button>
      </nav>
      
      {/* Main Login Form */}
      <div className="login-container">
        <div className="main-login">
            <form>
                <img src={logo} alt="Logo" className="logo-login"/>
                <div className="inputs-container">
                    <input className="inputs-login" type="text" name="user" placeholder=" Usuario" value={userId} onChange={handleChangeUser} required />
                    <input
                      className="inputs-login"
                      type="password"
                      name="pswd"
                      placeholder=" Clave"
                      value={password}
                      onChange={handleChangePassword}
                      onKeyUp={handlePasswordKeyState}
                      onKeyDown={handlePasswordKeyState}
                      onBlur={() => setIsCapsLockOn(false)}
                      required
                    />
                    {isCapsLockOn && (
                      <p style={{ color: '#b45309', marginTop: '6px', fontSize: '0.9rem' }}>
                        Bloq Mayus esta activado
                      </p>
                    )}
                </div>
                {errorMessage && (
                  <p style={{ color: 'red', marginTop: '10px' }}>{errorMessage}</p>
                )}

                <div className="login-btn-container-login">
                    <button className="login-btn-login" id="login-btn-login" type="submit" onClick={handleLoginClick}>Iniciar sesión</button>
                </div>
            </form>
        </div>
       </div>
    </div>
  );
}

export default Login;
