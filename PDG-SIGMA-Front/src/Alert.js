import React, {useEffect } from "react";
import "./Alert.css"; // Archivo de estilos separado

const Alert = ({ show, onClose, message }) => {
  useEffect(() => {
    if (show) {
      const timer = setTimeout(() => {
        onClose(); // Llama a la función onClose después de 3 segundos
      }, 3000);
      return () => clearTimeout(timer); // Limpia el temporizador al desmontar
    }
  }, [show, onClose]);

  return (
    <div
      className={`alert alert-info ${show ? "alert-show" : ""}`}
      role="status"
      aria-live="polite"
    >
      {message || "Iniciaste sesion como estudiante"}
    </div>
  );
};

export default Alert;
