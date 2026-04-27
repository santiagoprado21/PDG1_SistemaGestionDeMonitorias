import React, { useEffect } from "react";
import "./Alert.css";

const ALLOWED_TYPES = ["info", "success", "error"];

const Alert = ({
  show,
  onClose,
  message,
  type = "info",
  autoCloseMs = 3000,
  closable = true,
}) => {
  const safeType = ALLOWED_TYPES.includes(type) ? type : "info";

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
      <span>{message || "Iniciaste sesion como estudiante"}</span>
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

export default Alert;
