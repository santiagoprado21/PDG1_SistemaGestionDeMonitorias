import React from 'react';
import './LoadingSpinner.css'; 

const LoadingSpinner = () => {
    return (
        <div className="spinner-overlay">
            <div className="spinner" aria-label="Cargando">
                <span className="spinner-dot" />
            </div>
        </div>
    );
};

export default LoadingSpinner;