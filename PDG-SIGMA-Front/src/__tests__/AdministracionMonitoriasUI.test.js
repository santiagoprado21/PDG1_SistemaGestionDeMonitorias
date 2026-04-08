import React from 'react';
import { render, screen, fireEvent, waitFor, within } from '@testing-library/react';
import { BrowserRouter } from 'react-router-dom';
import SeleccionarMonitor from '../SeleccionarMonitor';
import AprobarMonitoriasHU010 from '../AprobarMonitoriasHU010';

global.fetch = jest.fn();

jest.mock('../VerticalNavbar', () => () => <div data-testid="vertical-navbar">Navbar</div>);
jest.mock('../LoadingSpinner', () => () => <div data-testid="loading-spinner">Loading...</div>);
jest.mock('../PopUp', () => ({
    PopUp: ({ show, children }) => show ? <div data-testid="popup">{children}</div> : null
}));
jest.mock('../config/ApiBackend', () => ({
    BACKEND_URL: 'http://localhost:5435'
}));

const mockNavigate = jest.fn();
jest.mock('react-router-dom', () => ({
    ...jest.requireActual('react-router-dom'),
    useNavigate: () => mockNavigate,
    useParams: () => ({ requestId: '501' })
}));

describe('Frontend: Administración de monitorías', () => {
    beforeEach(() => {
        jest.clearAllMocks();
        Object.defineProperty(window, 'localStorage', {
            value: {
                getItem: jest.fn((key) => {
                    if (key === 'userId') return 'USER-001';
                    if (key === 'token') return 'Bearer test-token';
                    return null;
                }),
                setItem: jest.fn(),
                clear: jest.fn()
            },
            writable: true
        });
    });

    test('Debe cancelar reasignación en UI sin llamar endpoint de selección', async () => {
        fetch.mockImplementation(async (url) => {
            if (url.includes('/monitoring-request/501')) {
                return {
                    ok: true,
                    json: async () => ({
                        id: 501,
                        courseName: 'Programación II',
                        semester: '2026-1',
                        requestedHours: 8,
                        requiredAverageGrade: 3.5,
                        requiredCourseGrade: 3.8
                    })
                };
            }
            if (url.includes('/monitor-application/request/501')) {
                return {
                    ok: true,
                    json: async () => ([
                        {
                            id: 9001,
                            monitorName: 'Laura Pérez',
                            monitorId: 'A001',
                            status: 'POSTULADO',
                            applicationDate: '2026-01-20T10:00:00',
                            motivationLetter: 'Estoy motivada para apoyar el curso.'
                        }
                    ])
                };
            }
            if (url.includes('/monitor-application/select')) {
                return { ok: true, json: async () => ({ message: 'Seleccionado' }) };
            }
            return { ok: true, json: async () => ({}) };
        });

        render(
            <BrowserRouter>
                <SeleccionarMonitor />
            </BrowserRouter>
        );

        await waitFor(() => {
            expect(screen.getByText('Laura Pérez')).toBeInTheDocument();
        });

        fireEvent.click(screen.getByRole('button', { name: /Seleccionar/i }));
        await waitFor(() => {
            expect(screen.getByText(/Confirmar Selección de Monitor/i)).toBeInTheDocument();
        });

        fireEvent.click(screen.getByRole('button', { name: /Cancelar/i }));
        expect(screen.queryByText(/Confirmar Selección de Monitor/i)).not.toBeInTheDocument();
        expect(fetch).not.toHaveBeenCalledWith(
            'http://localhost:5435/monitor-application/select',
            expect.any(Object)
        );
    });

    test('Debe habilitar acción administrativa solo al ingresar comentario y enviar endpoint correcto', async () => {
        fetch.mockImplementation(async (url) => {
            if (url.includes('/pending-head-approval/')) {
                return {
                    ok: true,
                    json: async () => ([
                        {
                            id: 77,
                            courseName: 'Base de Datos',
                            programName: 'Ing. Sistemas',
                            professorName: 'Profesor Uno',
                            requestedHours: 10,
                            startDate: '2026-01-10',
                            finishDate: '2026-05-20',
                            justification: 'Necesidad académica'
                        }
                    ])
                };
            }
            if (url.includes('/monitoring-request/77/approve-by-head')) {
                return { ok: true, json: async () => ({ message: 'Aprobada correctamente' }) };
            }
            return { ok: true, json: async () => ({}) };
        });

        render(
            <BrowserRouter>
                <AprobarMonitoriasHU010 />
            </BrowserRouter>
        );

        await waitFor(() => {
            expect(screen.getByText('Gestión de Convocatorias')).toBeInTheDocument();
        });

        await waitFor(() => {
            expect(screen.queryByTestId('loading-spinner')).not.toBeInTheDocument();
        });

        const table = await screen.findByRole('table');
        expect(table).toHaveTextContent('Base de Datos');
        fireEvent.click(within(table).getByRole('button', { name: /Aprobar/i }));

        const confirmButton = await screen.findByRole('button', { name: /Confirmar Aprobación/i });
        expect(confirmButton).toBeDisabled();

        fireEvent.change(screen.getByPlaceholderText(/Ingrese el motivo de aprobación/i), {
            target: { value: 'Aprobación validada por el jefe.' }
        });

        expect(confirmButton).not.toBeDisabled();
        fireEvent.click(confirmButton);

        await waitFor(() => {
            expect(fetch).toHaveBeenCalledWith(
                'http://localhost:5435/monitoring-request/77/approve-by-head',
                expect.objectContaining({
                    method: 'POST'
                })
            );
        });
    });
});

