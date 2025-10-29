
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

const PopUpUpdateBudget = ({ show, onClose, onSubmit, initialHours, initialRate, remainingHours = 0, currentHours = 0 }) => {
  const [hours, setHours] = React.useState(initialHours || 0);
  const [rate, setRate] = React.useState(initialRate || 0);

  const formatCurrency = (value) => {
    const n = Number(value);
    if (isNaN(n)) return 0;
    try {
      return new Intl.NumberFormat('es-CO', { style: 'currency', currency: 'COP', maximumFractionDigits: 0 }).format(n);
    } catch (e) {
      return n;
    }
  };

  React.useEffect(() => {
    setHours(initialHours || 0);
    setRate(initialRate || 0);
  }, [initialHours, initialRate, show]);

  const predictedRemaining = (Number(remainingHours) + Number(currentHours)) - Number(hours);
  const cost = Number(hours) * Number(rate);

  if (!show) return null;
  return (
    <div className="overlay">
      <div className="popup">
        <div className="content">
          <h3>Editar presupuesto</h3>
          <label>Horas estimadas</label>
          <input type="number" min="0" value={hours} onChange={e => setHours(e.target.value)} />

          <label>Valor por hora</label>
          <input type="number" min="0" step="0.01" value={rate} onChange={e => setRate(e.target.value)} />

          <div style={{marginTop: '10px'}}>
            <div><strong>Resumen:</strong> {Number(hours) || 0} horas × {formatCurrency(rate)} = <strong>{formatCurrency(cost)}</strong></div>
            <div><strong>Horas restantes (estimado):</strong> {isNaN(predictedRemaining) ? 0 : predictedRemaining}</div>
          </div>
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
