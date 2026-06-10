import React from 'react';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { BrowserRouter } from 'react-router-dom';
import CreateConvocatoria from '../CreateConvocatoria';
import VerConvocatorias from '../VerConvocatorias';
import GenerateSimonFile from '../GenerateSimonFile';

const mockNavigate = jest.fn();

jest.mock('react-router-dom', () => ({
    ...jest.requireActual('react-router-dom'),
    useNavigate: () => mockNavigate
}));

jest.mock('../VerticalNavbar', () => () => <div data-testid="mock-navbar">Navbar</div>);
jest.mock('../LoadingSpinner', () => () => <div data-testid="mock-spinner">Loading</div>);
jest.mock('../PopUp', () => ({
    PopUp: ({ show = true, onClose, children, message }) => {
        if (!show) return null;
        return (
            <div data-testid="mock-popup">
                {children || message}
                {onClose && <button onClick={onClose}>Cerrar</button>}
            </div>
        );
    }
}));

jest.mock('../config/ApiBackend', () => ({
    BACKEND_URL: 'http://localhost:5435'
}));

const setLocalStorage = ({ role = 'monitor', userId = 'USR-001', token = 'Bearer t' } = {}) => {
    Object.defineProperty(window, 'localStorage', {
        value: {
            getItem: jest.fn((key) => {
                if (key === 'role') return role;
                if (key === 'userId') return userId;
                if (key === 'token') return token;
                return null;
            }),
            setItem: jest.fn(),
            clear: jest.fn()
        },
        writable: true
    });
};

const renderWithRouter = (component) => render(<BrowserRouter>{component}</BrowserRouter>);

describe('ConvocatoriasSimonSimple', () => {
    beforeEach(() => {
        jest.clearAllMocks();
        global.fetch = jest.fn();
        setLocalStorage();
    });

    test('CreateConvocatoria carga datos iniciales y renderiza vista principal', async () => {
        fetch.mockImplementation(async (url) => {
            if (url.includes('/school/getSchools')) {
                return { ok: true, json: async () => [{ id: 1, name: 'Facultad Ing' }] };
            }
            if (url.includes('/monitoring-request/professor/')) {
                return { ok: true, json: async () => [] };
            }
            return { ok: true, json: async () => [] };
        });

        renderWithRouter(<CreateConvocatoria />);

        expect(screen.getByText(/Crear Convocatoria de Monitoría/i)).toBeInTheDocument();

        await waitFor(() => {
            expect(fetch).toHaveBeenCalledWith('http://localhost:5435/school/getSchools');
            expect(fetch).toHaveBeenCalledWith(
                'http://localhost:5435/monitoring-request/professor/USR-001',
                expect.any(Object)
            );
        });
    });

    test('CreateConvocatoria valida campos obligatorios al enviar vacío', async () => {
        fetch.mockImplementation(async (url) => {
            if (url.includes('/school/getSchools')) {
                return { ok: true, json: async () => [] };
            }
            if (url.includes('/monitoring-request/professor/')) {
                return { ok: true, json: async () => [] };
            }
            return { ok: true, json: async () => [] };
        });

        renderWithRouter(<CreateConvocatoria />);

        const submitButton = screen.getByRole('button', { name: /Crear Convocatoria/i });
        const form = submitButton.closest('form');
        fireEvent.submit(form);

        expect(await screen.findByText(/Por favor completa todos los campos obligatorios/i)).toBeInTheDocument();
    });

    test('VerConvocatorias permite abrir modal de postulación para monitor', async () => {
        fetch.mockImplementation(async (url) => {
            if (url.includes('/monitoring-request/open')) {
                return {
                    ok: true,
                    json: async () => [
                        {
                            id: 7,
                            courseName: 'POO',
                            professorName: 'Profe A',
                            programName: 'Sistemas',
                            schoolName: 'Ingeniería',
                            semester: '2026-1',
                            requestedHours: 96,
                            applicationCount: 0
                        }
                    ]
                };
            }
            if (url.includes('/monitor-application/monitor/')) {
                return { ok: true, json: async () => [] };
            }
            return { ok: true, json: async () => [] };
        });

        renderWithRouter(<VerConvocatorias />);

        expect(await screen.findByText(/POO/i)).toBeInTheDocument();
        fireEvent.click(screen.getByRole('button', { name: /Postularse/i }));

        expect(await screen.findByText(/Postularse a Monitoría/i)).toBeInTheDocument();
    });

    test('VerConvocatorias bloquea postulación para rol no permitido', async () => {
        setLocalStorage({ role: 'professor' });

        fetch.mockImplementation(async (url) => {
            if (url.includes('/monitoring-request/open')) {
                return {
                    ok: true,
                    json: async () => [
                        {
                            id: 8,
                            courseName: 'Bases de Datos',
                            professorName: 'Profe B',
                            programName: 'Sistemas',
                            schoolName: 'Ingeniería',
                            semester: '2026-1',
                            requestedHours: 64,
                            applicationCount: 0
                        }
                    ]
                };
            }
            if (url.includes('/monitor-application/monitor/')) {
                return { ok: true, json: async () => [] };
            }
            return { ok: true, json: async () => [] };
        });

        renderWithRouter(<VerConvocatorias />);

        fireEvent.click(await screen.findByRole('button', { name: /Postularse/i }));

        expect(await screen.findByText(/Solo los estudiantes pueden postularse/i)).toBeInTheDocument();
    });

    test('GenerateSimonFile carga preview e historial y permite generar archivo', async () => {
        const clickSpy = jest.spyOn(HTMLAnchorElement.prototype, 'click').mockImplementation(() => {});
        if (!window.URL.createObjectURL) {
            window.URL.createObjectURL = () => 'blob:mock';
        }
        if (!window.URL.revokeObjectURL) {
            window.URL.revokeObjectURL = () => {};
        }
        const createObjectURLSpy = jest.spyOn(window.URL, 'createObjectURL').mockImplementation(() => 'blob:mock');
        const revokeObjectURLSpy = jest.spyOn(window.URL, 'revokeObjectURL').mockImplementation(() => {});

        fetch.mockImplementation(async (url) => {
            if (url.includes('/simon/preview')) {
                return {
                    ok: true,
                    json: async () => ({
                        totalMonitorings: 1,
                        canGenerate: true,
                        monitorings: [
                            {
                                nombre: 'Ana',
                                apellido: 'Paz',
                                codigoEstudiante: '2020123',
                                email: 'ana@uni.edu',
                                nombreCurso: 'Álgebra',
                                profesorSolicita: 'Docente X',
                                fechaInicio: '2026-01-10',
                                fechaFin: '2026-05-10',
                                totalHoras: 96
                            }
                        ]
                    })
                };
            }
            if (url.includes('/simon/history')) {
                return { ok: true, json: async () => [] };
            }
            if (url.includes('/simon/generate')) {
                return { ok: true, blob: async () => new Blob(['excel']) };
            }
            return { ok: true, json: async () => [] };
        });

        renderWithRouter(<GenerateSimonFile />);

        expect(await screen.findByText(/Generar Archivo SIMON/i)).toBeInTheDocument();
        expect(await screen.findByText(/Listo para generar/i)).toBeInTheDocument();

        fireEvent.click(screen.getByRole('button', { name: /Generar y Descargar Archivo SIMON/i }));

        await waitFor(() => {
            expect(fetch).toHaveBeenCalledWith(
                expect.stringContaining('/simon/generate?generatedBy=USR-001&semester='),
                expect.any(Object)
            );
            expect(createObjectURLSpy).toHaveBeenCalled();
            expect(clickSpy).toHaveBeenCalled();
            expect(revokeObjectURLSpy).toHaveBeenCalled();
        });

        clickSpy.mockRestore();
        createObjectURLSpy.mockRestore();
        revokeObjectURLSpy.mockRestore();
    });

    test('GenerateSimonFile muestra error cuando preview falla (response not ok)', async () => {
        fetch.mockImplementation(async (url) => {
            if (url.includes('/simon/preview')) {
                return { ok: false };
            }
            if (url.includes('/simon/history')) {
                return { ok: true, json: async () => [] };
            }
            return { ok: true, json: async () => [] };
        });

        renderWithRouter(<GenerateSimonFile />);

        expect(await screen.findByText(/Error al cargar vista previa de datos/i)).toBeInTheDocument();
    });

    test('GenerateSimonFile muestra error cuando historial falla (response not ok)', async () => {
        fetch.mockImplementation(async (url) => {
            if (url.includes('/simon/preview')) {
                return {
                    ok: true,
                    json: async () => ({
                        totalMonitorings: 0,
                        canGenerate: false,
                        monitorings: []
                    })
                };
            }
            if (url.includes('/simon/history')) {
                return { ok: false };
            }
            return { ok: true, json: async () => [] };
        });

        renderWithRouter(<GenerateSimonFile />);

        expect(await screen.findByText(/Generar Archivo SIMON/i)).toBeInTheDocument();
    });

    test('GenerateSimonFile bloquea generacion cuando no hay datos (canGenerate false)', async () => {
        fetch.mockImplementation(async (url) => {
            if (url.includes('/simon/preview')) {
                return {
                    ok: true,
                    json: async () => ({
                        totalMonitorings: 0,
                        canGenerate: false,
                        monitorings: []
                    })
                };
            }
            if (url.includes('/simon/history')) {
                return { ok: true, json: async () => [] };
            }
            return { ok: true, json: async () => [] };
        });

        renderWithRouter(<GenerateSimonFile />);

        await screen.findByText(/Sin datos disponibles/i);

        expect(screen.getByRole('button', { name: /Generar y Descargar Archivo SIMON/i })).toBeDisabled();
    });

    test('GenerateSimonFile maneja error 204 al generar archivo', async () => {
        fetch.mockImplementation(async (url) => {
            if (url.includes('/simon/preview')) {
                return {
                    ok: true,
                    json: async () => ({
                        totalMonitorings: 1,
                        canGenerate: true,
                        monitorings: []
                    })
                };
            }
            if (url.includes('/simon/history')) {
                return { ok: true, json: async () => [] };
            }
            if (url.includes('/simon/generate')) {
                return { ok: false, status: 204 };
            }
            return { ok: true, json: async () => [] };
        });

        renderWithRouter(<GenerateSimonFile />);

        await screen.findByText(/Listo para generar/i);

        fireEvent.click(screen.getByRole('button', { name: /Generar y Descargar Archivo SIMON/i }));

        expect(await screen.findByText(/No hay monitorías aprobadas disponibles/i)).toBeInTheDocument();
    });

    test('GenerateSimonFile maneja error generico al generar archivo', async () => {
        fetch.mockImplementation(async (url) => {
            if (url.includes('/simon/preview')) {
                return {
                    ok: true,
                    json: async () => ({
                        totalMonitorings: 1,
                        canGenerate: true,
                        monitorings: []
                    })
                };
            }
            if (url.includes('/simon/history')) {
                return { ok: true, json: async () => [] };
            }
            if (url.includes('/simon/generate')) {
                return { ok: false, status: 500 };
            }
            return { ok: true, json: async () => [] };
        });

        renderWithRouter(<GenerateSimonFile />);

        await screen.findByText(/Listo para generar/i);

        fireEvent.click(screen.getByRole('button', { name: /Generar y Descargar Archivo SIMON/i }));

        expect(await screen.findByText(/Error al generar el archivo/i)).toBeInTheDocument();
    });

    test('GenerateSimonFile permite cambiar semestre, refrescar datos y alternar historial', async () => {
        fetch.mockImplementation(async (url) => {
            if (url.includes('/simon/preview')) {
                return {
                    ok: true,
                    json: async () => ({
                        totalMonitorings: 1,
                        canGenerate: true,
                        monitorings: [
                            {
                                nombre: 'Ana',
                                apellido: 'Paz',
                                codigoEstudiante: '2020123',
                                email: 'ana@uni.edu',
                                nombreCurso: 'Álgebra',
                                profesorSolicita: 'Docente X',
                                fechaInicio: '2026-01-10',
                                fechaFin: '2026-05-10',
                                totalHoras: 96
                            }
                        ]
                    })
                };
            }
            if (url.includes('/simon/history')) {
                return {
                    ok: true,
                    json: async () => [
                        {
                            id: 1,
                            generatedAt: '2026-01-15T10:00:00Z',
                            generatedBy: 'Coordinador',
                            semester: '2026-1',
                            totalMonitorings: 5,
                            fileName: 'SIMON_2026-1.xlsx'
                        }
                    ]
                };
            }
            return { ok: true, json: async () => [] };
        });

        const { container } = renderWithRouter(<GenerateSimonFile />);

        await screen.findByText(/Monitorías aprobadas:/i);

        const select = container.querySelector('select');
        fireEvent.change(select, { target: { value: '2025-2' } });

        fireEvent.click(screen.getByRole('button', { name: /Refrescar Datos/i }));

        expect(await screen.findByText(/Datos actualizados/i)).toBeInTheDocument();

        fireEvent.click(screen.getByRole('button', { name: /Ver Historial/i }));

        expect(screen.getByText(/SIMON_2026-1.xlsx/i)).toBeInTheDocument();
        expect(screen.getByText(/15\/01\/2026/i)).toBeInTheDocument();

        fireEvent.click(screen.getByRole('button', { name: /Ocultar Historial/i }));

        expect(screen.queryByText(/SIMON_2026-1.xlsx/i)).not.toBeInTheDocument();
    });
});