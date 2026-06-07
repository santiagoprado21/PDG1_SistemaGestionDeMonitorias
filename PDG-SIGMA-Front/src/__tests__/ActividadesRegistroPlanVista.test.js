import React from 'react';
import { render, screen, waitFor, fireEvent } from '@testing-library/react';
import { BrowserRouter } from 'react-router-dom';
import CreateActivity from '../CreateActivity';
import PlanActividades from '../PlanActividades';
import VistaMonitorActividades from '../VistaMonitorActividades';

global.fetch = jest.fn();

jest.mock('../VerticalNavbar', () => () => <div data-testid="vertical-navbar">Navbar</div>);
jest.mock('../PopUp', () => ({
    PopUp: ({ show, children }) => show ? <div data-testid="popup">{children}</div> : null
}));
jest.mock('../config/ApiBackend', () => ({
    BACKEND_URL: 'http://localhost:5435',
    getApiUrl: (path) => `http://localhost:5435/${path.startsWith('/') ? path.slice(1) : path}`
}));

const mockNavigate = jest.fn();
let mockedParams = { monitoringId: null };
jest.mock('react-router-dom', () => ({
    ...jest.requireActual('react-router-dom'),
    useNavigate: () => mockNavigate,
    useParams: () => mockedParams
}));

describe('ActividadesRegistroPlanVista', () => {
    beforeEach(() => {
        jest.clearAllMocks();
        mockedParams = { monitoringId: null };
        Object.defineProperty(window, 'localStorage', {
            value: {
                getItem: jest.fn((key) => {
                    if (key === 'role') return 'professor';
                    if (key === 'userId') return 'PROF-1';
                    if (key === 'token') return 'Bearer test-token';
                    return null;
                }),
                setItem: jest.fn(),
                clear: jest.fn()
            },
            writable: true
        });
    });

    test('CreateActivity debe validar campos obligatorios al registrar actividad', async () => {
        fetch.mockImplementation(async (url) => {
            if (url.includes('/monitoring/getAllActiveByUserId/')) {
                return {
                    ok: true,
                    json: async () => ([{ id: 10, course: { id: 100, name: 'Programación I' } }])
                };
            }
            if (url.includes('/student/getA')) {
                return { ok: true, json: async () => [] };
            }
            return { ok: true, json: async () => [] };
        });

        render(
            <BrowserRouter>
                <CreateActivity />
            </BrowserRouter>
        );

        fireEvent.click(screen.getByRole('button', { name: /Confirmar/i }));

        await waitFor(() => {
            expect(screen.getByTestId('popup')).toBeInTheDocument();
            expect(screen.getByText(/complete todos los campos obligatorios/i)).toBeInTheDocument();
        });
    });

    test('PlanActividades debe renderizar y mostrar estado sin actividades del plan', async () => {
        fetch.mockImplementation(async (url) => {
            if (url.includes('/monitoring/getAllByProfessor/')) {
                return {
                    ok: true,
                    json: async () => ([{
                        id: 501,
                        semester: '2026-1',
                        course: { name: 'Ingeniería de Software' },
                        program: { name: 'Ing Sistemas' },
                        assignedMonitor: { name: 'Ana', lastName: 'García', code: 'M001' }
                    }])
                };
            }
            if (url.includes('/api/activity-schedule/plan/')) {
                return { ok: true, json: async () => ({ activities: [] }) };
            }
            if (url.includes('/api/rubric/professor/')) {
                return { ok: true, json: async () => [] };
            }
            return { ok: true, json: async () => ({}) };
        });

        render(
            <BrowserRouter>
                <PlanActividades />
            </BrowserRouter>
        );

        const planTitles = await screen.findAllByText(/Plan de Actividades/i);
        expect(planTitles.length).toBeGreaterThan(0);
        await waitFor(() => {
            expect(screen.getByText(/No hay actividades en el plan/i)).toBeInTheDocument();
        });
    });

    test('VistaMonitorActividades debe mostrar actividades asignadas y filtros base', async () => {
        window.localStorage.getItem.mockImplementation((key) => {
            if (key === 'role') return 'monitor';
            if (key === 'userId') return 'MON-01';
            if (key === 'token') return 'Bearer test-token';
            return null;
        });

        fetch.mockImplementation(async (url) => {
            if (url.includes('/activity/findAll/MON-01/monitor')) {
                return {
                    ok: true,
                    json: async () => ([
                        {
                            id: 1,
                            name: 'Asesoría semanal',
                            description: 'Resolver dudas',
                            state: 'PENDIENTE',
                            finish: '2099-12-31T00:00:00',
                            monitoring: {
                                id: 88,
                                semester: '2026-1',
                                course: { name: 'Programación Avanzada' },
                                program: { name: 'Ing. Sistemas' },
                                professor: { name: 'Profesor Demo' }
                            },
                            monitor: { name: 'Monitor', lastName: 'Uno', code: 'MON-01' }
                        }
                    ])
                };
            }
            return { ok: true, json: async () => [] };
        });

        render(
            <BrowserRouter>
                <VistaMonitorActividades />
            </BrowserRouter>
        );

        expect(await screen.findByText(/Mis Actividades/i)).toBeInTheDocument();
        expect(screen.getByText(/Revisa y gestiona las actividades asignadas/i)).toBeInTheDocument();
        await waitFor(() => {
            expect(screen.queryByText(/Cargando tus actividades/i)).not.toBeInTheDocument();
        });
        expect(await screen.findByText(/Asesoría semanal/i)).toBeInTheDocument();
        expect(screen.getByRole('button', { name: /Limpiar filtros/i })).toBeInTheDocument();
    });

    test('VistaMonitorActividades muestra estado vacio sin actividades', async () => {
        window.localStorage.getItem.mockImplementation((key) => {
            if (key === 'role') return 'monitor';
            if (key === 'userId') return 'MON-01';
            if (key === 'token') return 'Bearer test-token';
            return null;
        });

        fetch.mockImplementation(async () => ({ ok: true, json: async () => [] }));

        render(
            <BrowserRouter>
                <VistaMonitorActividades />
            </BrowserRouter>
        );

        expect(await screen.findByText(/Mis Actividades/i)).toBeInTheDocument();
        await waitFor(() => {
            expect(screen.queryByText(/Cargando tus actividades/i)).not.toBeInTheDocument();
        });
    });

    test('VistaMonitorActividades no se rompe cuando falla la API', async () => {
        window.localStorage.getItem.mockImplementation((key) => {
            if (key === 'role') return 'monitor';
            if (key === 'userId') return 'MON-01';
            if (key === 'token') return 'Bearer test-token';
            return null;
        });

        fetch.mockRejectedValue(new Error('Error de red'));

        render(
            <BrowserRouter>
                <VistaMonitorActividades />
            </BrowserRouter>
        );

        expect(await screen.findByText(/Mis Actividades/i)).toBeInTheDocument();
    });

    test('VistaMonitorActividades muestra loading mientras carga', async () => {
        window.localStorage.getItem.mockImplementation((key) => {
            if (key === 'role') return 'monitor';
            if (key === 'userId') return 'MON-01';
            if (key === 'token') return 'Bearer test-token';
            return null;
        });

        fetch.mockImplementation(() => new Promise(() => {}));

        render(
            <BrowserRouter>
                <VistaMonitorActividades />
            </BrowserRouter>
        );

        expect(await screen.findByText(/Cargando tus actividades/i)).toBeInTheDocument();
    });

    test('VistaMonitorActividades filtra por estado pendientes', async () => {
        window.localStorage.getItem.mockImplementation((key) => {
            if (key === 'role') return 'monitor';
            if (key === 'userId') return 'MON-01';
            if (key === 'token') return 'Bearer test-token';
            return null;
        });

        fetch.mockImplementation(async (url) => {
            if (url.includes('/activity/findAll/MON-01/monitor')) {
                return { ok: true, json: async () => [
                    { id: 1, name: 'Tarea pendiente', description: 'Desc', state: 'PENDIENTE', finish: '2099-12-31T00:00:00', monitoring: { id: 1, course: { name: 'Curso' }, professor: { name: 'Profe' }, semester: '2026-1' } },
                    { id: 2, name: 'Tarea completada', description: 'Desc', state: 'COMPLETADO', finish: '2020-01-01T00:00:00', monitoring: { id: 1, course: { name: 'Curso' }, professor: { name: 'Profe' }, semester: '2026-1' } }
                ]};
            }
            return { ok: true, json: async () => [] };
        });

        render(
            <BrowserRouter>
                <VistaMonitorActividades />
            </BrowserRouter>
        );

        await screen.findByText(/Mis Actividades/i);
        await waitFor(() => expect(screen.queryByText(/Cargando tus actividades/i)).not.toBeInTheDocument());

        fireEvent.change(screen.getByDisplayValue(/Todas las actividades/i), { target: { value: 'pendientes' } });

        await waitFor(() => {
            expect(screen.getByText(/Tarea pendiente/i)).toBeInTheDocument();
        });
        expect(screen.queryByText(/Tarea completada/i)).not.toBeInTheDocument();
    });

    test('VistaMonitorActividades abre modal de confirmacion al hacer clic en Completar', async () => {
        window.localStorage.getItem.mockImplementation((key) => {
            if (key === 'role') return 'monitor';
            if (key === 'userId') return 'MON-01';
            if (key === 'token') return 'Bearer test-token';
            return null;
        });

        fetch.mockImplementation(async (url) => {
            if (url.includes('/activity/findAll/MON-01/monitor')) {
                return { ok: true, json: async () => [
                    { id: 1, name: 'Tarea a completar', description: 'Desc', state: 'PENDIENTE', finish: '2099-12-31T00:00:00', monitoring: { id: 1, course: { name: 'Curso' }, professor: { name: 'Profe' }, semester: '2026-1' } }
                ]};
            }
            return { ok: true, json: async () => [] };
        });

        render(
            <BrowserRouter>
                <VistaMonitorActividades />
            </BrowserRouter>
        );

        await screen.findByText(/Mis Actividades/i);
        await waitFor(() => expect(screen.queryByText(/Cargando tus actividades/i)).not.toBeInTheDocument());

        fireEvent.click(screen.getByRole('button', { name: /Completar/i }));

        expect(await screen.findByText(/¿Estás seguro de marcar esta actividad como completada/i)).toBeInTheDocument();
        expect(screen.getByRole('button', { name: /Marcar como Completada/i })).toBeInTheDocument();
    });

    test('VistaMonitorActividades cierra modal de confirmacion al cancelar', async () => {
        window.localStorage.getItem.mockImplementation((key) => {
            if (key === 'role') return 'monitor';
            if (key === 'userId') return 'MON-01';
            if (key === 'token') return 'Bearer test-token';
            return null;
        });

        fetch.mockImplementation(async (url) => {
            if (url.includes('/activity/findAll/MON-01/monitor')) {
                return { ok: true, json: async () => [
                    { id: 1, name: 'Tarea', description: 'Desc', state: 'PENDIENTE', finish: '2099-12-31T00:00:00', monitoring: { id: 1, course: { name: 'Curso' }, professor: { name: 'Profe' }, semester: '2026-1' } }
                ]};
            }
            return { ok: true, json: async () => [] };
        });

        render(
            <BrowserRouter>
                <VistaMonitorActividades />
            </BrowserRouter>
        );

        await screen.findByText(/Mis Actividades/i);
        await waitFor(() => expect(screen.queryByText(/Cargando tus actividades/i)).not.toBeInTheDocument());

        fireEvent.click(screen.getByRole('button', { name: /Completar/i }));
        expect(await screen.findByText(/¿Estás seguro/i)).toBeInTheDocument();

        fireEvent.click(screen.getByRole('button', { name: /Cancelar/i }));

        await waitFor(() => {
            expect(screen.queryByText(/¿Estás seguro/i)).not.toBeInTheDocument();
        });
    });
});

