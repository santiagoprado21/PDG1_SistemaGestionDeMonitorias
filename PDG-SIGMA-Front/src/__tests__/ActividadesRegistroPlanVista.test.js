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

    test('PlanActividades debe manejar monitorías con flujo antiguo', async () => {
        fetch.mockImplementation(async (url) => {
            if (url.includes('/monitoring/getAllByProfessor/')) {
                return {
                    ok: true,
                    json: async () => ([{
                        id: 601,
                        semester: '2026-1',
                        course: { name: 'Programación II' },
                        program: { name: 'Ing Sistemas' },
                        assignedMonitor: null,
                        monitoringMonitors: [
                            {
                                estadoSeleccion: 'seleccionado',
                                monitor: { name: 'Carlos', lastName: 'Lopez', code: 'M002' }
                            }
                        ]
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

        await waitFor(() => {
            expect(screen.getByText(/No hay actividades en el plan/i)).toBeInTheDocument();
        });
        expect(screen.getByText(/Programación II/i)).toBeInTheDocument();
    });

    test('PlanActividades debe usar monitoringId desde la URL', async () => {
        mockedParams = { monitoringId: '501' };

        fetch.mockImplementation(async (url) => {
            if (url.includes('/monitoring/getAllByProfessor/')) {
                return {
                    ok: true,
                    json: async () => ([{
                        id: 501,
                        semester: '2026-1',
                        course: { name: 'Matemáticas' },
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

        await waitFor(() => {
            expect(screen.getByText(/No hay actividades en el plan/i)).toBeInTheDocument();
        });
    });

    test('PlanActividades debe mostrar error si falla carga del plan', async () => {
        fetch.mockImplementation(async (url) => {
            if (url.includes('/monitoring/getAllByProfessor/')) {
                return {
                    ok: true,
                    json: async () => ([{
                        id: 501,
                        semester: '2026-1',
                        course: { name: 'Test' },
                        program: { name: 'Test' },
                        assignedMonitor: { name: 'Ana', lastName: 'García', code: 'M001' }
                    }])
                };
            }
            if (url.includes('/api/activity-schedule/plan/')) {
                return { ok: false };
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

        await waitFor(() => {
            expect(screen.getByTestId('popup')).toBeInTheDocument();
            expect(screen.getByText(/Error al cargar el plan de actividades/i)).toBeInTheDocument();
        });
    });

    test('PlanActividades debe calcular duración automáticamente al cambiar horario', async () => {
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
            if (url.includes('/validate-conflicts')) {
                return { ok: true, json: async () => [] };
            }
            return { ok: true, json: async () => ({}) };
        });

        const { container } = render(
            <BrowserRouter>
                <PlanActividades />
            </BrowserRouter>
        );

        await waitFor(() => {
            expect(screen.getByText(/No hay actividades en el plan/i)).toBeInTheDocument();
        });

        fireEvent.click(screen.getByRole('button', { name: /Agregar Actividad/i }));
        await screen.findByRole('heading', { name: /Nueva Actividad/i });

        const startInput = container.querySelector('input[name="startTime"]');
        const endInput = container.querySelector('input[name="endTime"]');

        fireEvent.change(startInput, { target: { value: '08:00', name: 'startTime' } });
        fireEvent.change(endInput, { target: { value: '10:00', name: 'endTime' } });

        const durationInput = container.querySelector('input[name="durationHours"]');
        await waitFor(() => {
            expect(durationInput.value).toBe('2.00');
        });
    });

    test('PlanActividades debe validar monitoría requerida al enviar formulario', async () => {
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
            if (url.includes('/validate-conflicts')) {
                return { ok: true, json: async () => [] };
            }
            return { ok: true, json: async () => ({}) };
        });

        const { container } = render(
            <BrowserRouter>
                <PlanActividades />
            </BrowserRouter>
        );

        await waitFor(() => {
            expect(screen.getByText(/No hay actividades en el plan/i)).toBeInTheDocument();
        });

        fireEvent.click(screen.getByRole('button', { name: /Agregar Actividad/i }));
        await screen.findByRole('heading', { name: /Nueva Actividad/i });

        const monitoringSelect = container.querySelector('select[name="monitoringId"]');
        fireEvent.change(monitoringSelect, { target: { value: '', name: 'monitoringId' } });

        fireEvent.click(screen.getByRole('button', { name: /Crear/i }));

        await waitFor(() => {
            expect(screen.getByTestId('popup')).toBeInTheDocument();
            expect(screen.getByText(/Debe seleccionar una monitoría/i)).toBeInTheDocument();
        });
    });

    test('PlanActividades debe cancelar eliminación de actividad', async () => {
        const originalConfirm = window.confirm;
        window.confirm = jest.fn(() => false);

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
                return {
                    ok: true,
                    json: async () => ({
                        activities: [{
                            id: 1,
                            name: 'Actividad de prueba',
                            category: 'Tutoría',
                            finish: '2099-12-31',
                            startTime: '08:00',
                            endTime: '10:00',
                            durationHours: 2,
                            priority: 'MEDIA',
                            state: 'PENDIENTE'
                        }]
                    })
                };
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

        await screen.findByText('Actividad de prueba');

        fireEvent.click(screen.getByRole('button', { name: /Eliminar actividad/i }));

        expect(window.confirm).toHaveBeenCalled();

        window.confirm = originalConfirm;
    });

    test('PlanActividades debe mostrar error si falla eliminación de actividad', async () => {
        const originalConfirm = window.confirm;
        window.confirm = jest.fn(() => true);

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
                return {
                    ok: true,
                    json: async () => ({
                        activities: [{
                            id: 1,
                            name: 'Actividad a eliminar',
                            category: 'Tutoría',
                            finish: '2099-12-31',
                            startTime: '08:00',
                            endTime: '10:00',
                            durationHours: 2,
                            priority: 'MEDIA',
                            state: 'PENDIENTE'
                        }]
                    })
                };
            }
            if (url.includes('/api/rubric/professor/')) {
                return { ok: true, json: async () => [] };
            }
            if (url.includes('/activity/')) {
                return { ok: false };
            }
            return { ok: true, json: async () => ({}) };
        });

        render(
            <BrowserRouter>
                <PlanActividades />
            </BrowserRouter>
        );

        await screen.findByText('Actividad a eliminar');

        fireEvent.click(screen.getByRole('button', { name: /Eliminar actividad/i }));

        await waitFor(() => {
            expect(screen.getByTestId('popup')).toBeInTheDocument();
            expect(screen.getByText(/Error al eliminar la actividad/i)).toBeInTheDocument();
        });

        window.confirm = originalConfirm;
    });

    test('PlanActividades debe navegar a gestión de rúbricas', async () => {
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

        await waitFor(() => {
            expect(screen.getByText(/No hay actividades en el plan/i)).toBeInTheDocument();
        });

        fireEvent.click(screen.getByRole('button', { name: /Gestionar Rúbricas/i }));

        expect(mockNavigate).toHaveBeenCalledWith('/gestion-rubricas');
    });

    test('PlanActividades debe mostrar rúbricas en el formulario de actividad', async () => {
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
                return { ok: true, json: async () => [
                    { id: 1, name: 'Rúbrica de evaluación', totalPoints: 100 }
                ]};
            }
            if (url.includes('/validate-conflicts')) {
                return { ok: true, json: async () => [] };
            }
            return { ok: true, json: async () => ({}) };
        });

        render(
            <BrowserRouter>
                <PlanActividades />
            </BrowserRouter>
        );

        await waitFor(() => {
            expect(screen.getByText(/No hay actividades en el plan/i)).toBeInTheDocument();
        });

        fireEvent.click(screen.getByRole('button', { name: /Agregar Actividad/i }));
        await screen.findByRole('heading', { name: /Nueva Actividad/i });

        expect(screen.getByText(/Rúbrica de evaluación/i)).toBeInTheDocument();
    });

    test('PlanActividades debe mostrar conflictos de horario en el modal', async () => {
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
            if (url.includes('/validate-conflicts')) {
                return { ok: true, json: async () => [
                    { activityName: 'Conflicto de prueba', startTime: '08:00', endTime: '10:00' }
                ]};
            }
            return { ok: true, json: async () => ({}) };
        });

        const { container } = render(
            <BrowserRouter>
                <PlanActividades />
            </BrowserRouter>
        );

        await waitFor(() => {
            expect(screen.getByText(/No hay actividades en el plan/i)).toBeInTheDocument();
        });

        fireEvent.click(screen.getByRole('button', { name: /Agregar Actividad/i }));
        await screen.findByRole('heading', { name: /Nueva Actividad/i });

        const finishInput = container.querySelector('input[name="finish"]');
        const startInput = container.querySelector('input[name="startTime"]');
        const endInput = container.querySelector('input[name="endTime"]');

        fireEvent.change(finishInput, { target: { value: '2026-12-31', name: 'finish' } });
        fireEvent.change(startInput, { target: { value: '08:00', name: 'startTime' } });
        fireEvent.change(endInput, { target: { value: '10:00', name: 'endTime' } });

        await waitFor(() => {
            expect(screen.getByText(/Conflictos de horarios detectados/i)).toBeInTheDocument();
            expect(screen.getByText(/Conflicto de prueba/i)).toBeInTheDocument();
        });
    });

    test('PlanActividades debe cambiar monitoría desde el selector', async () => {
        fetch.mockImplementation(async (url) => {
            if (url.includes('/monitoring/getAllByProfessor/')) {
                return {
                    ok: true,
                    json: async () => ([
                        {
                            id: 501,
                            semester: '2026-1',
                            course: { name: 'Ingeniería de Software' },
                            program: { name: 'Ing Sistemas' },
                            assignedMonitor: { name: 'Ana', lastName: 'García', code: 'M001' }
                        },
                        {
                            id: 502,
                            semester: '2026-2',
                            course: { name: 'Programación Avanzada' },
                            program: { name: 'Ing Sistemas' },
                            assignedMonitor: { name: 'Luis', lastName: 'Pérez', code: 'M002' }
                        }
                    ])
                };
            }
            if (url.includes('/api/activity-schedule/plan/')) {
                return {
                    ok: true,
                    json: async () => ({
                        activities: [{
                            id: 10,
                            name: 'Nueva actividad cargada',
                            category: 'Tutoría',
                            finish: '2099-12-31',
                            startTime: '08:00',
                            endTime: '10:00',
                            durationHours: 2,
                            priority: 'MEDIA',
                            state: 'PENDIENTE'
                        }]
                    })
                };
            }
            if (url.includes('/api/rubric/professor/')) {
                return { ok: true, json: async () => [] };
            }
            if (url.includes('/validate-conflicts')) {
                return { ok: true, json: async () => [] };
            }
            return { ok: true, json: async () => ({}) };
        });

        render(
            <BrowserRouter>
                <PlanActividades />
            </BrowserRouter>
        );

        await screen.findByText(/Nueva actividad cargada/i);

        const selector = screen.getByDisplayValue(/Ingeniería de Software/i);
        fireEvent.change(selector, { target: { value: '502' } });

        await waitFor(() => {
            expect(screen.getByDisplayValue(/Programación Avanzada/i)).toBeInTheDocument();
        });
    });

    test('PlanActividades debe mostrar error si falla creación de actividad', async () => {
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
            if (url.includes('/validate-conflicts')) {
                return { ok: true, json: async () => [] };
            }
            if (url.includes('/api/activity-schedule/create')) {
                return { ok: false, text: async () => 'Error del servidor' };
            }
            return { ok: true, json: async () => ({}) };
        });

        const { container } = render(
            <BrowserRouter>
                <PlanActividades />
            </BrowserRouter>
        );

        await waitFor(() => {
            expect(screen.getByText(/No hay actividades en el plan/i)).toBeInTheDocument();
        });

        fireEvent.click(screen.getByRole('button', { name: /Agregar Actividad/i }));
        await screen.findByRole('heading', { name: /Nueva Actividad/i });

        const nameInput = container.querySelector('input[name="name"]');
        const descInput = container.querySelector('textarea[name="description"]');
        const categorySelect = container.querySelector('select[name="category"]');
        const finishInput = container.querySelector('input[name="finish"]');

        fireEvent.change(nameInput, { target: { value: 'Nueva actividad', name: 'name' } });
        fireEvent.change(descInput, { target: { value: 'Descripción', name: 'description' } });
        fireEvent.change(categorySelect, { target: { value: 'Tutoría', name: 'category' } });
        fireEvent.change(finishInput, { target: { value: '2026-12-31', name: 'finish' } });

        fireEvent.click(screen.getByRole('button', { name: /Crear/i }));

        await waitFor(() => {
            expect(screen.getByTestId('popup')).toBeInTheDocument();
            expect(screen.getByText(/Error al guardar la actividad/i)).toBeInTheDocument();
        });
    });

    test('PlanActividades debe cerrar modal al cancelar', async () => {
        fetch.mockImplementation(async (url) => {
            if (url.includes('/monitoring/getAllByProfessor/')) {
                return { ok: true, json: async () => ([{ id: 501, semester: '2026-1', course: { name: 'Test' }, program: { name: 'Test' }, assignedMonitor: { name: 'Ana', lastName: 'García', code: 'M001' } }]) };
            }
            if (url.includes('/api/activity-schedule/plan/')) {
                return { ok: true, json: async () => ({ activities: [] }) };
            }
            if (url.includes('/api/rubric/professor/')) {
                return { ok: true, json: async () => [] };
            }
            if (url.includes('/validate-conflicts')) {
                return { ok: true, json: async () => [] };
            }
            return { ok: true, json: async () => ({}) };
        });

        render(
            <BrowserRouter>
                <PlanActividades />
            </BrowserRouter>
        );

        await waitFor(() => {
            expect(screen.getByText(/No hay actividades en el plan/i)).toBeInTheDocument();
        });

        fireEvent.click(screen.getByRole('button', { name: /Agregar Actividad/i }));
        await screen.findByRole('heading', { name: /Nueva Actividad/i });

        fireEvent.click(screen.getByRole('button', { name: /Cancelar/i }));

        await waitFor(() => {
            expect(screen.queryByRole('heading', { name: /Nueva Actividad/i })).not.toBeInTheDocument();
        });
    });

    test('PlanActividades debe crear actividad exitosamente', async () => {
        fetch.mockImplementation(async (url) => {
            if (url.includes('/monitoring/getAllByProfessor/')) {
                return { ok: true, json: async () => ([{ id: 501, semester: '2026-1', course: { name: 'Test' }, program: { name: 'Test' }, assignedMonitor: { name: 'Ana', lastName: 'García', code: 'M001' } }]) };
            }
            if (url.includes('/api/activity-schedule/plan/')) {
                return { ok: true, json: async () => ({ activities: [] }) };
            }
            if (url.includes('/api/rubric/professor/')) {
                return { ok: true, json: async () => [] };
            }
            if (url.includes('/validate-conflicts')) {
                return { ok: true, json: async () => [] };
            }
            if (url.includes('/api/activity-schedule/create')) {
                return { ok: true, json: async () => ({ id: 1 }) };
            }
            return { ok: true, json: async () => ({}) };
        });

        const { container } = render(
            <BrowserRouter>
                <PlanActividades />
            </BrowserRouter>
        );

        await waitFor(() => {
            expect(screen.getByText(/No hay actividades en el plan/i)).toBeInTheDocument();
        });

        fireEvent.click(screen.getByRole('button', { name: /Agregar Actividad/i }));
        await screen.findByRole('heading', { name: /Nueva Actividad/i });

        const nameInput = container.querySelector('input[name="name"]');
        const descInput = container.querySelector('textarea[name="description"]');
        const categorySelect = container.querySelector('select[name="category"]');
        const finishInput = container.querySelector('input[name="finish"]');

        fireEvent.change(nameInput, { target: { value: 'Nueva actividad', name: 'name' } });
        fireEvent.change(descInput, { target: { value: 'Descripción', name: 'description' } });
        fireEvent.change(categorySelect, { target: { value: 'Tutoría', name: 'category' } });
        fireEvent.change(finishInput, { target: { value: '2026-12-31', name: 'finish' } });

        fireEvent.click(screen.getByRole('button', { name: /Crear/i }));

        await waitFor(() => {
            expect(screen.getByTestId('popup')).toBeInTheDocument();
            expect(screen.getByText(/Actividad creada exitosamente/i)).toBeInTheDocument();
        });
    });

    test('PlanActividades debe mostrar error al cargar monitorías', async () => {
        fetch.mockImplementation(async (url) => {
            if (url.includes('/monitoring/getAllByProfessor/')) {
                return { ok: false };
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

        await waitFor(() => {
            expect(screen.getByTestId('popup')).toBeInTheDocument();
            expect(screen.getByText(/Error al cargar monitorías/i)).toBeInTheDocument();
        });
    });

    test('PlanActividades debe eliminar actividad exitosamente', async () => {
        const originalConfirm = window.confirm;
        window.confirm = jest.fn(() => true);

        fetch.mockImplementation(async (url) => {
            if (url.includes('/monitoring/getAllByProfessor/')) {
                return { ok: true, json: async () => ([{ id: 501, semester: '2026-1', course: { name: 'Test' }, program: { name: 'Test' }, assignedMonitor: { name: 'Ana', lastName: 'García', code: 'M001' } }]) };
            }
            if (url.includes('/api/activity-schedule/plan/')) {
                return { ok: true, json: async () => ({ activities: [{ id: 1, name: 'Actividad eliminar', category: 'Tutoría', finish: '2099-12-31', startTime: '08:00', endTime: '10:00', durationHours: 2, priority: 'MEDIA', state: 'PENDIENTE' }] }) };
            }
            if (url.includes('/api/rubric/professor/')) {
                return { ok: true, json: async () => [] };
            }
            if (url.includes('/activity/')) {
                return { ok: true, json: async () => ({}) };
            }
            return { ok: true, json: async () => ({}) };
        });

        render(
            <BrowserRouter>
                <PlanActividades />
            </BrowserRouter>
        );

        await screen.findByText('Actividad eliminar');

        fireEvent.click(screen.getByRole('button', { name: /Eliminar actividad/i }));

        await waitFor(() => {
            expect(screen.getByTestId('popup')).toBeInTheDocument();
            expect(screen.getByText(/Actividad eliminada exitosamente/i)).toBeInTheDocument();
        });

        window.confirm = originalConfirm;
    });

    test('PlanActividades debe editar actividad existente', async () => {
        fetch.mockImplementation(async (url) => {
            if (url.includes('/monitoring/getAllByProfessor/')) {
                return { ok: true, json: async () => ([{ id: 501, semester: '2026-1', course: { name: 'Test' }, program: { name: 'Test' }, assignedMonitor: { name: 'Ana', lastName: 'García', code: 'M001' } }]) };
            }
            if (url.includes('/api/activity-schedule/plan/')) {
                return { ok: true, json: async () => ({ activities: [{ id: 1, name: 'Actividad a editar', category: 'Tutoría', finish: '2099-12-31', startTime: '08:00', endTime: '10:00', durationHours: 2, priority: 'MEDIA', state: 'PENDIENTE', description: 'Desc' }] }) };
            }
            if (url.includes('/api/rubric/professor/')) {
                return { ok: true, json: async () => [] };
            }
            if (url.includes('/validate-conflicts')) {
                return { ok: true, json: async () => [] };
            }
            if (url.includes('/api/activity-schedule/create')) {
                return { ok: true, json: async () => ({ id: 1 }) };
            }
            return { ok: true, json: async () => ({}) };
        });

        const { container } = render(
            <BrowserRouter>
                <PlanActividades />
            </BrowserRouter>
        );

        await screen.findByText('Actividad a editar');

        fireEvent.click(screen.getByRole('button', { name: /Editar actividad/i }));
        await screen.findByRole('heading', { name: /Editar Actividad/i });

        const nameInput = container.querySelector('input[name="name"]');
        expect(nameInput.value).toBe('Actividad a editar');

        fireEvent.click(screen.getByRole('button', { name: /Actualizar/i }));

        await waitFor(() => {
            expect(screen.getByTestId('popup')).toBeInTheDocument();
            expect(screen.getByText(/Actividad actualizada exitosamente/i)).toBeInTheDocument();
        });
    });

    test('PlanActividades debe rechazar estado COMPLETADO con progreso menor a 100%', async () => {
        fetch.mockImplementation(async (url) => {
            if (url.includes('/monitoring/getAllByProfessor/')) {
                return { ok: true, json: async () => ([{ id: 501, semester: '2026-1', course: { name: 'Test' }, program: { name: 'Test' }, assignedMonitor: { name: 'Ana', lastName: 'García', code: 'M001' } }]) };
            }
            if (url.includes('/api/activity-schedule/plan/')) {
                return { ok: true, json: async () => ({ activities: [{ id: 1, name: 'Actividad progreso', category: 'Tutoría', finish: '2099-12-31', startTime: '08:00', endTime: '10:00', durationHours: 2, priority: 'MEDIA', state: 'PENDIENTE', progressPercentage: 50 }] }) };
            }
            if (url.includes('/api/rubric/professor/')) {
                return { ok: true, json: async () => [] };
            }
            if (url.includes('/validate-conflicts')) {
                return { ok: true, json: async () => [] };
            }
            return { ok: true, json: async () => ({}) };
        });

        const { container } = render(
            <BrowserRouter>
                <PlanActividades />
            </BrowserRouter>
        );

        await screen.findByText('Actividad progreso');

        fireEvent.click(screen.getByRole('button', { name: /Editar actividad/i }));
        await screen.findByRole('heading', { name: /Editar Actividad/i });

        const stateSelect = container.querySelector('select[name="state"]');
        fireEvent.change(stateSelect, { target: { value: 'COMPLETADO', name: 'state' } });

        const form = container.querySelector('form');
        fireEvent.submit(form);

        await waitFor(() => {
            expect(screen.getByTestId('popup')).toBeInTheDocument();
            expect(screen.getByText(/progreso es 100%/i)).toBeInTheDocument();
        });
    });

    test('PlanActividades debe rechazar envío cuando hay conflictos de horario', async () => {
        fetch.mockImplementation(async (url) => {
            if (url.includes('/monitoring/getAllByProfessor/')) {
                return { ok: true, json: async () => ([{ id: 501, semester: '2026-1', course: { name: 'Test' }, program: { name: 'Test' }, assignedMonitor: { name: 'Ana', lastName: 'García', code: 'M001' } }]) };
            }
            if (url.includes('/api/activity-schedule/plan/')) {
                return { ok: true, json: async () => ({ activities: [] }) };
            }
            if (url.includes('/api/rubric/professor/')) {
                return { ok: true, json: async () => [] };
            }
            if (url.includes('/validate-conflicts')) {
                return { ok: true, json: async () => [{ activityName: 'Conflicto existente', startTime: '08:00', endTime: '10:00' }] };
            }
            return { ok: true, json: async () => ({}) };
        });

        const { container } = render(
            <BrowserRouter>
                <PlanActividades />
            </BrowserRouter>
        );

        await waitFor(() => {
            expect(screen.getByText(/No hay actividades en el plan/i)).toBeInTheDocument();
        });

        fireEvent.click(screen.getByRole('button', { name: /Agregar Actividad/i }));
        await screen.findByRole('heading', { name: /Nueva Actividad/i });

        const finishInput = container.querySelector('input[name="finish"]');
        const startInput = container.querySelector('input[name="startTime"]');
        const endInput = container.querySelector('input[name="endTime"]');

        fireEvent.change(finishInput, { target: { value: '2026-12-31', name: 'finish' } });
        fireEvent.change(startInput, { target: { value: '08:00', name: 'startTime' } });
        fireEvent.change(endInput, { target: { value: '10:00', name: 'endTime' } });

        await waitFor(() => {
            expect(screen.getByText(/Conflictos de horarios detectados/i)).toBeInTheDocument();
        });

        const form = container.querySelector('form');
        fireEvent.submit(form);

        await waitFor(() => {
            expect(screen.getByTestId('popup')).toBeInTheDocument();
            expect(screen.getByText(/No se puede guardar: hay conflictos/i)).toBeInTheDocument();
        });
    });

    test('PlanActividades debe rechazar creación en monitoría cerrada', async () => {
        fetch.mockImplementation(async (url) => {
            if (url.includes('/monitoring/getAllByProfessor/')) {
                return { ok: true, json: async () => ([
                    { id: 501, semester: '2026-1', course: { name: 'Monitoría Abierta' }, program: { name: 'Test' }, assignedMonitor: { name: 'Ana', lastName: 'García', code: 'M001' }, approvalStatus: null },
                    { id: 502, semester: '2026-2', course: { name: 'Monitoría Cerrada' }, program: { name: 'Test' }, assignedMonitor: { name: 'Luis', lastName: 'Pérez', code: 'M002' }, approvalStatus: 'CERRADA' }
                ]) };
            }
            if (url.includes('/api/activity-schedule/plan/')) {
                return { ok: true, json: async () => ({ activities: [] }) };
            }
            if (url.includes('/api/rubric/professor/')) {
                return { ok: true, json: async () => [] };
            }
            if (url.includes('/validate-conflicts')) {
                return { ok: true, json: async () => [] };
            }
            return { ok: true, json: async () => ({}) };
        });

        const { container } = render(
            <BrowserRouter>
                <PlanActividades />
            </BrowserRouter>
        );

        await waitFor(() => {
            expect(screen.getByText(/No hay actividades en el plan/i)).toBeInTheDocument();
        });

        const addBtn = screen.getByRole('button', { name: /Agregar Actividad/i });
        addBtn.disabled = false;
        fireEvent.click(addBtn);
        await screen.findByRole('heading', { name: /Nueva Actividad/i });

        const monitoringSelect = container.querySelector('select[name="monitoringId"]');
        fireEvent.change(monitoringSelect, { target: { value: '502', name: 'monitoringId' } });

        const nameInput = container.querySelector('input[name="name"]');
        const descInput = container.querySelector('textarea[name="description"]');
        const categorySelect = container.querySelector('select[name="category"]');
        const finishInput = container.querySelector('input[name="finish"]');

        fireEvent.change(nameInput, { target: { value: 'Test', name: 'name' } });
        fireEvent.change(descInput, { target: { value: 'Desc', name: 'description' } });
        fireEvent.change(categorySelect, { target: { value: 'Tutoría', name: 'category' } });
        fireEvent.change(finishInput, { target: { value: '2026-12-31', name: 'finish' } });

        const form = container.querySelector('form');
        fireEvent.submit(form);

        await waitFor(() => {
            expect(screen.getByTestId('popup')).toBeInTheDocument();
            expect(screen.getByText(/No se pueden crear actividades en una monitoría cerrada/i)).toBeInTheDocument();
        });
    });

    test('PlanActividades debe manejar error al cargar rúbricas', async () => {
        fetch.mockImplementation(async (url) => {
            if (url.includes('/monitoring/getAllByProfessor/')) {
                return { ok: true, json: async () => ([{ id: 501, semester: '2026-1', course: { name: 'Test' }, program: { name: 'Test' }, assignedMonitor: { name: 'Ana', lastName: 'García', code: 'M001' } }]) };
            }
            if (url.includes('/api/rubric/professor/')) {
                return { ok: false };
            }
            if (url.includes('/api/activity-schedule/plan/')) {
                return { ok: true, json: async () => ({ activities: [] }) };
            }
            return { ok: true, json: async () => ({}) };
        });

        render(
            <BrowserRouter>
                <PlanActividades />
            </BrowserRouter>
        );

        await waitFor(() => {
            expect(screen.getByText(/No hay actividades en el plan/i)).toBeInTheDocument();
        });
    });

    test('PlanActividades debe manejar error al validar conflictos', async () => {
        fetch.mockImplementation(async (url) => {
            if (url.includes('/monitoring/getAllByProfessor/')) {
                return { ok: true, json: async () => ([{ id: 501, semester: '2026-1', course: { name: 'Test' }, program: { name: 'Test' }, assignedMonitor: { name: 'Ana', lastName: 'García', code: 'M001' } }]) };
            }
            if (url.includes('/api/activity-schedule/plan/')) {
                return { ok: true, json: async () => ({ activities: [] }) };
            }
            if (url.includes('/api/rubric/professor/')) {
                return { ok: true, json: async () => [] };
            }
            if (url.includes('/validate-conflicts')) {
                return { ok: false };
            }
            return { ok: true, json: async () => ({}) };
        });

        const { container } = render(
            <BrowserRouter>
                <PlanActividades />
            </BrowserRouter>
        );

        await waitFor(() => {
            expect(screen.getByText(/No hay actividades en el plan/i)).toBeInTheDocument();
        });

        fireEvent.click(screen.getByRole('button', { name: /Agregar Actividad/i }));
        await screen.findByRole('heading', { name: /Nueva Actividad/i });

        const finishInput = container.querySelector('input[name="finish"]');
        const startInput = container.querySelector('input[name="startTime"]');
        const endInput = container.querySelector('input[name="endTime"]');

        fireEvent.change(finishInput, { target: { value: '2026-12-31', name: 'finish' } });
        fireEvent.change(startInput, { target: { value: '08:00', name: 'startTime' } });
        fireEvent.change(endInput, { target: { value: '10:00', name: 'endTime' } });

        await waitFor(() => {
            expect(screen.getByRole('button', { name: /Crear/i })).toBeInTheDocument();
        });
    });
});

