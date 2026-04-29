import './GenerateSimonFile.css';
import React, { useEffect, useMemo, useState } from 'react';
import VerticalNavbar from './VerticalNavbar';
import { PopUp } from "./PopUp";
import { BACKEND_URL } from './config/ApiBackend';
import LoadingSpinner from './LoadingSpinner';
import { generateAcademicPeriodOptions, getCurrentAcademicPeriod } from './globalFix';

function GenerateSimonFile() {
    const academicPeriodOptions = useMemo(() => generateAcademicPeriodOptions(), []);
    const currentAcademicPeriod = useMemo(() => getCurrentAcademicPeriod(), []);
    const [previewData, setPreviewData] = useState(null);
    const [history, setHistory] = useState([]);
    const [isLoading, setIsLoading] = useState(false);
    const [isOpen, setIsOpen] = useState(false);
    const [message, setMessage] = useState("");
    const [semester, setSemester] = useState(currentAcademicPeriod);
    const [showHistory, setShowHistory] = useState(false);

    const coordinatorId = localStorage.getItem('userId');

    useEffect(() => {
        loadPreview();
        loadHistory();
    }, []);

    const loadPreview = async () => {
        setIsLoading(true);
        try {
            console.log("Cargando vista previa de monitorías aprobadas...");
            const response = await fetch(`${BACKEND_URL}/simon/preview`, {
                method: 'GET',
                headers: {
                    'Content-Type': 'application/json',
                    'Authorization': localStorage.getItem('token')
                },
            });

            if (!response.ok) {
                throw new Error('Error al cargar vista previa');
            }

            const data = await response.json();
            console.log("Datos recibidos:", data);
            console.log("Total monitorías:", data.totalMonitorings);
            setPreviewData(data);
        } catch (error) {
            console.error('❌ Error:', error);
            setMessage("Error al cargar vista previa de datos");
            setIsOpen(true);
        } finally {
            setIsLoading(false);
        }
    };

    const loadHistory = async () => {
        try {
            const response = await fetch(`${BACKEND_URL}/simon/history`, {
                method: 'GET',
                headers: {
                    'Content-Type': 'application/json',
                    'Authorization': localStorage.getItem('token')
                },
            });

            if (!response.ok) {
                throw new Error('Error al cargar historial');
            }

            const data = await response.json();
            setHistory(data);
        } catch (error) {
            console.error('Error al cargar historial:', error);
        }
    };

    const handleGenerateFile = async () => {
        if (!previewData || !previewData.canGenerate) {
            setMessage("No hay monitorías aprobadas para generar el archivo");
            setIsOpen(true);
            return;
        }

        setIsLoading(true);
        try {
            const response = await fetch(
                `${BACKEND_URL}/simon/generate?generatedBy=${coordinatorId}&semester=${semester}`,
                {
                    method: 'GET',
                    headers: {
                        'Authorization': localStorage.getItem('token')
                    },
                }
            );

            if (!response.ok) {
                if (response.status === 204) {
                    throw new Error('No hay monitorías aprobadas disponibles');
                }
                throw new Error('Error al generar el archivo');
            }

            // Descargar el archivo
            const blob = await response.blob();
            const url = window.URL.createObjectURL(blob);
            const a = document.createElement('a');
            a.href = url;
            a.download = `SIMON_${semester}_${new Date().toISOString().slice(0,10)}.xlsx`;
            document.body.appendChild(a);
            a.click();
            window.URL.revokeObjectURL(url);
            document.body.removeChild(a);

            setMessage("Archivo SIMON generado y descargado exitosamente");
            setIsOpen(true);

            // Recargar historial
            loadHistory();
        } catch (error) {
            console.error('Error:', error);
            setMessage(error.message || "Error al generar el archivo");
            setIsOpen(true);
        } finally {
            setIsLoading(false);
        }
    };

    const formatDate = (dateString) => {
        if (!dateString) return '';
        const date = new Date(dateString);
        return date.toLocaleString('es-CO', {
            year: 'numeric',
            month: '2-digit',
            day: '2-digit',
            hour: '2-digit',
            minute: '2-digit'
        });
    };

    return (
        <div className="generate-simon-container">
            <VerticalNavbar />
            
            <div className="generate-simon-content">
                <h1 className="generate-simon-title app-page-header app-page-title">Generar Archivo SIMON</h1>

                {isLoading && <LoadingSpinner />}

                {/* Sección de Resumen */}
                <div className="simon-summary-card">
                    <h2>Resumen</h2>
                    {previewData && (
                        <div className="summary-content">
                            <div className="summary-item">
                                <span className="summary-label">Monitorías aprobadas:</span>
                                <span className="summary-value">{previewData.totalMonitorings}</span>
                            </div>
                            <div className="summary-item">
                                <span className="summary-label">Periodo:</span>
                                <select
                                    value={semester}
                                    onChange={(e) => setSemester(e.target.value)}
                                    className="semester-input"
                                >
                                    {academicPeriodOptions.map((period) => (
                                        <option key={period} value={period}>{period}</option>
                                    ))}
                                </select>
                            </div>
                            <div className="summary-item">
                                <span className="summary-label">Estado:</span>
                                <span className={`summary-status ${previewData.canGenerate ? 'status-ready' : 'status-not-ready'}`}>
                                    {previewData.canGenerate ? 'Listo para generar' : 'Sin datos disponibles'}
                                </span>
                            </div>
                        </div>
                    )}
                </div>

                {/* Botones de Acción */}
                <div className="simon-actions">
                    <button 
                        className="btn-generate-simon"
                        onClick={handleGenerateFile}
                        disabled={!previewData || !previewData.canGenerate || isLoading}
                    >
                        {isLoading ? 'Generando...' : 'Generar y Descargar Archivo SIMON'}
                    </button>
                    <button 
                        className="btn-toggle-history"
                        onClick={() => {
                            loadPreview();
                            setMessage("Datos actualizados");
                            setIsOpen(true);
                        }}
                        disabled={isLoading}
                    >
                        Refrescar Datos
                    </button>
                    <button 
                        className="btn-toggle-history"
                        onClick={() => setShowHistory(!showHistory)}
                    >
                        {showHistory ? 'Ocultar Historial' : 'Ver Historial'}
                    </button>
                </div>

                {/* Vista Previa de Datos */}
                {previewData && previewData.monitorings && previewData.monitorings.length > 0 && (
                    <div className="simon-preview-card">
                        <h2>Vista Previa de Datos ({previewData.totalMonitorings} monitorías)</h2>
                        <div className="preview-table-container">
                            <table className="preview-table">
                                <thead>
                                    <tr>
                                        <th>Estudiante</th>
                                        <th>Código</th>
                                        <th>Email</th>
                                        <th>Curso</th>
                                        <th>Profesor</th>
                                        <th>Fecha Inicio</th>
                                        <th>Fecha Fin</th>
                                        <th>Horas</th>
                                    </tr>
                                </thead>
                                <tbody>
                                    {previewData.monitorings.slice(0, 5).map((monitoring, index) => (
                                        <tr key={index}>
                                            <td>{monitoring.nombre} {monitoring.apellido}</td>
                                            <td>{monitoring.codigoEstudiante}</td>
                                            <td>{monitoring.email}</td>
                                            <td>{monitoring.nombreCurso}</td>
                                            <td>{monitoring.profesorSolicita}</td>
                                            <td>{monitoring.fechaInicio}</td>
                                            <td>{monitoring.fechaFin}</td>
                                            <td>{monitoring.totalHoras}</td>
                                        </tr>
                                    ))}
                                </tbody>
                            </table>
                            {previewData.monitorings.length > 5 && (
                                <p className="more-records">
                                    ... y {previewData.monitorings.length - 5} registro(s) más
                                </p>
                            )}
                        </div>
                    </div>
                )}

                {/* Historial de Generaciones */}
                {showHistory && (
                    <div className="simon-history-card">
                        <h2>Historial de Generaciones</h2>
                        {history.length === 0 ? (
                            <p className="no-history">No hay archivos generados anteriormente</p>
                        ) : (
                            <div className="history-table-container">
                                <table className="history-table">
                                    <thead>
                                        <tr>
                                            <th>Fecha y Hora</th>
                                            <th>Generado Por</th>
                                            <th>Periodo</th>
                                            <th>Total Monitorías</th>
                                            <th>Nombre Archivo</th>
                                        </tr>
                                    </thead>
                                    <tbody>
                                        {history.map((record) => (
                                            <tr key={record.id}>
                                                <td>{formatDate(record.generatedAt)}</td>
                                                <td>{record.generatedBy}</td>
                                                <td>{record.semester}</td>
                                                <td>{record.totalMonitorings}</td>
                                                <td className="file-name">{record.fileName}</td>
                                            </tr>
                                        ))}
                                    </tbody>
                                </table>
                            </div>
                        )}
                    </div>
                )}

                {/* Información Adicional */}
                <div className="simon-info-card">
                    <h3>Información Importante</h3>
                    <ul>
                        <li>El archivo generado está en formato Excel (.xlsx)</li>
                        <li>Solo se incluyen monitorías <strong>aprobadas por el jefe de departamento</strong></li>
                        <li>El formato cumple con los requisitos exactos de SIMON</li>
                        <li>Se genera un registro de auditoría por cada archivo generado</li>
                        <li>El archivo incluye 17 columnas con toda la información requerida</li>
                    </ul>
                </div>
            </div>

            {isOpen && <PopUp message={message} setTrigger={setIsOpen} />}
        </div>
    );
}

export default GenerateSimonFile;


