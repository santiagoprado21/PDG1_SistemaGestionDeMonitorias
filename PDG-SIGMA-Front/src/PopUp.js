
import React from "react";
import "./PopUp.css";

const PopUp = ({show, onClose, children }) => {
  if (!show) return null;
  return (
    <div className="overlay">
      <div className="popup">
        <div className="content">{children}</div>
        <button className="button" onClick={onClose}>
          OK
        </button>
      </div>
    </div>
  );
};

const PopupDelete = ({ show, onClose, onApply }) => {
  if(!show) return null;
  return (
    <div className="overlay">
      <div className="popup">
        <div className="content">
          <p>¿Estás seguro de la acción a realizar?</p>
          Eliminar
        </div>
        <button
          className={`button button-apply`}
          onClick={onApply}
        >
          Sí, Eliminar
        </button>
        <button className="button button-close" onClick={onClose}>
          Cancelar
        </button>
      </div>
    </div>
  );
};

const PopUpUpdateBudget = ({ show, onClose, onSubmit, initialHours, initialRate }) => {
  const [hours, setHours] = React.useState(initialHours || 0);
  const [rate, setRate] = React.useState(initialRate || 0);

  React.useEffect(() => {
    setHours(initialHours || 0);
    setRate(initialRate || 0);
  }, [initialHours, initialRate, show]);

  if (!show) return null;
  return (
    <div className="overlay">
      <div className="popup">
        <div className="content">
          <label>Horas estimadas:</label>
          <input type="number" min="0" value={hours} onChange={e => setHours(e.target.value)} />
          <label>Valor por hora:</label>
          <input type="number" min="0" step="0.01" value={rate} onChange={e => setRate(e.target.value)} />
        </div>
        <button className="button button-apply" onClick={() => onSubmit(Number(hours), Number(rate))}>
          Actualizar presupuesto
        </button>
        <button className="button button-close" onClick={onClose}>
          Cancelar
        </button>
      </div>
    </div>
  );
};

export { PopUp, PopupDelete, PopUpUpdateBudget };
