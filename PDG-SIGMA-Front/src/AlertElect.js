import React, { useEffect } from "react";
import "./Alert.css";

const ALLOWED_TYPES = ["info", "success", "error"];

const AlertElect = ({
  show,
  onClose,
  message = "La seleccion termino. Los estudiantes seran notificados",
  type = "success",
  autoCloseMs = 3000,
  closable = true,
}) => {
  const safeType = ALLOWED_TYPES.includes(type) ? type : "success";

  useEffect(() => {
    if (!show || typeof onClose !== "function" || autoCloseMs <= 0) {
      return undefined;
    }

    const timer = setTimeout(() => {
      onClose();
    }, autoCloseMs);

    return () => clearTimeout(timer);
  }, [show, onClose, autoCloseMs]);

  if (!show) {
    return null;
  }

  return (
    <div
      className={`alert alert-${safeType} alert-show`}
      role="status"
      aria-live="polite"
    >
      <span>{message}</span>
      {closable && (
        <button
          type="button"
          className="alert-close"
          aria-label="Cerrar alerta"
          onClick={onClose}
        >
          x
        </button>
      )}
    </div>
  );
};

export default AlertElect;
