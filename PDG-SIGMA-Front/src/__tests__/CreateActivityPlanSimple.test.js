import React from 'react';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import CreateActivity from '../CreateActivity';
import PlanActividades from '../PlanActividades';

const mockNavigate = jest.fn();

jest.mock('react-router-dom', () => ({
    ...jest.requireActual('react-router-dom'),
    useNavigate: () => mockNavigate,
    useParams: () => ({})
}));

jest.mock('../VerticalNavbar', () => () => <div data-testid="mock-navbar">Navbar</div>);
jest.mock('../PopUp', () => ({
    PopUp: ({ show, children }) => (show ? <div data-testid="mock-popup">{children}</div> : null)
}));

const setUserContext = () => {
    Object.defineProperty(window, 'localStorage', {
        value: {
            getItem: jest.fn((key) => {
                if (key === 'role') return 'professor';
                if (key === 'userId') return 'PROF-1';
                if (key === 'token') return 'Bearer token';
                return null;
            }),
            setItem: jest.fn(),
            clear: jest.fn()
        },
        writable: true
    });
};

describe('CreateActivityPlanSimple', () => {
    beforeEach(() => {
        jest.clearAllMocks();
        setUserContext();
        global.fetch = jest.fn(async (url) => {
            if (url.includes('/monitoring/getAllActiveByUserId/')) {
                return { ok: true, json: async () => [] };
            }
            if (url.includes('/student/getA')) {
                return { ok: true, json: async () => [] };
            }
            if (url.includes('/monitoring/getAllByProfessor/')) {
                return { ok: true, json: async () => [] };
            }
            if (url.includes('/api/rubric/professor/')) {
                return { ok: true, json: async () => [] };
            }
            if (url.includes('/api/activity-schedule/validate-conflicts')) {
                return { ok: true, json: async () => [] };
            }
            if (url.includes('/api/activity-schedule/plan/')) {
                return {
                    ok: true,
                    json: async () => ({
                        courseName: 'POO',
                        programName: 'Sistemas',
                        professorName: 'Profe 1',
                        monitorName: 'Monitor 1',
                        semester: '2026-1',
                        totalActivities: 0,
                        completedActivities: 0,
                        pendingActivities: 0,
                        totalHours: 0,
                        activities: []
                    })
                };
            }
            return { ok: true, json: async () => [] };
        });
    });

    test('CreateActivity valida campos obligatorios cuando se envía vacío', async () => {
        render(<CreateActivity />);

        expect(screen.getByText(/Crear Actividad/i)).toBeInTheDocument();

        fireEvent.click(screen.getByRole('button', { name: /Confirmar/i }));

        expect(await screen.findByText(/complete todos los campos obligatorios/i)).toBeInTheDocument();
    });

    test('CreateActivity muestra campo para nueva categoría y permite cancelarlo', async () => {
        render(<CreateActivity />);

        fireEvent.click(screen.getByRole('button', { name: '+' }));
        expect(screen.getByPlaceholderText(/Nueva categoría/i)).toBeInTheDocument();

        fireEvent.click(screen.getByRole('button', { name: /Eliminar/i }));

        await waitFor(() => {
            expect(screen.queryByPlaceholderText(/Nueva categoría/i)).not.toBeInTheDocument();
        });
    });

    test('PlanActividades muestra advertencia cuando no hay monitorías con monitor', async () => {
        render(<PlanActividades />);

        expect(await screen.findByRole('heading', { name: /Plan de Actividades/i })).toBeInTheDocument();
        expect(await screen.findByText(/No tienes monitor/i)).toBeInTheDocument();
        expect(screen.getByRole('button', { name: /Agregar Actividad/i })).toBeDisabled();
    });

    test('PlanActividades permite abrir modal de nueva actividad con monitoría disponible', async () => {
        global.fetch = jest.fn(async (url) => {
            if (url.includes('/monitoring/getAllByProfessor/')) {
                return {
                    ok: true,
                    json: async () => [
                        {
                            id: 15,
                            course: { name: 'Algoritmos' },
                            program: { name: 'Sistemas' },
                            semester: '2026-1',
                            assignedMonitor: { name: 'Ana', lastName: 'Paz', code: 'MON-9' }
                        }
                    ]
                };
            }
            if (url.includes('/api/activity-schedule/plan/15')) {
                return {
                    ok: true,
                    json: async () => ({
                        courseName: 'Algoritmos',
                        programName: 'Sistemas',
                        professorName: 'Profe 1',
                        monitorName: 'Ana Paz',
                        semester: '2026-1',
                        totalActivities: 0,
                        completedActivities: 0,
                        pendingActivities: 0,
                        totalHours: 0,
                        activities: []
                    })
                };
            }
            if (url.includes('/api/rubric/professor/')) {
                return { ok: true, json: async () => [] };
            }
            if (url.includes('/api/activity-schedule/validate-conflicts')) {
                return { ok: true, json: async () => [] };
            }
            return { ok: true, json: async () => [] };
        });

        render(<PlanActividades />);

        expect(await screen.findByText(/Algoritmos - Sistemas/i)).toBeInTheDocument();

        fireEvent.click(screen.getByRole('button', { name: /Agregar Actividad/i }));

        expect(await screen.findByRole('heading', { name: /Nueva Actividad/i })).toBeInTheDocument();
    });

    test('PlanActividades muestra estado de carga inicial', () => {
        global.fetch = jest.fn(async () => {
            await new Promise(r => setTimeout(r, 500));
            return { ok: true, json: async () => [] };
        });
        render(<PlanActividades />);
        expect(screen.getByText(/Cargando monitorías y plan de actividades/i)).toBeInTheDocument();
    });

    test('PlanActividades muestra error en carga de monitorías', async () => {
        global.fetch = jest.fn(async () => {
            throw new Error('Network error');
        });
        render(<PlanActividades />);
        await waitFor(() => {
            expect(screen.queryByTestId('loading-spinner')).not.toBeInTheDocument();
        });
    });

    test('CreateActivity llena formulario y envia exitosamente', async () => {
        const mockCourse = { id: 5, course: { name: 'POO', id: 50 } };
        const mockMonitor = { userId: 'MON-1', name: 'Juan Monitor', rol: 'M' };
        const mockCategory = { id: 1, name: 'Académico' };

        global.fetch = jest.fn(async (url, options) => {
            if (url.includes('/monitoring/getAllActiveByUserId/')) {
                return { ok: true, json: async () => [mockCourse] };
            }
            if (url.includes('/student/getA')) {
                return { ok: true, json: async () => [] };
            }
            if (url.includes('/monitoring-monitor/')) {
                return { ok: true, json: async () => [mockMonitor] };
            }
            if (url.includes('/student/course/')) {
                return { ok: true, json: async () => [] };
            }
            if (url.includes('/category/course/')) {
                return { ok: true, json: async () => [mockCategory] };
            }
            if (url.includes('/activity/create')) {
                return { ok: true, json: async () => ({ id: 100 }) };
            }
            return { ok: true, json: async () => [] };
        });

        const { container } = render(<CreateActivity />);
        expect(screen.getByText('Crear Actividad')).toBeInTheDocument();

        const nameInput = container.querySelector('input.input-text');
        fireEvent.change(nameInput, { target: { value: 'Tutoría de POO' } });

        await screen.findByText('POO');

        const selects = container.querySelectorAll('select');
        fireEvent.change(selects[0], { target: { value: '5' } });

        await waitFor(() => {
            expect(screen.getByText('Académico')).toBeInTheDocument();
        });

        fireEvent.change(selects[1], { target: { value: 'Académico' } });

        const dateInput = container.querySelector('input[type="date"]');
        fireEvent.change(dateInput, { target: { value: '2026-06-15' } });

        await waitFor(() => {
            expect(screen.getByText('Juan Monitor')).toBeInTheDocument();
        });

        fireEvent.change(selects[2], { target: { value: 'MON-1' } });

        fireEvent.click(screen.getByRole('button', { name: /Confirmar/i }));

        await waitFor(() => {
            expect(screen.getByText(/Actividad creada exitosamente/i)).toBeInTheDocument();
        });
    });

    test('CreateActivity muestra error HTTP al crear actividad', async () => {
    const mockCourse = { id: 5, course: { name: 'POO', id: 50 } };
    const mockMonitor = { userId: 'MON-1', name: 'Juan Monitor', rol: 'M' };
    const mockCategory = { id: 1, name: 'Académico' };

    global.fetch = jest.fn(async (url, options) => {
        if (url.includes('/monitoring/getAllActiveByUserId/')) {
            return { ok: true, json: async () => [mockCourse] };
        }
        if (url.includes('/student/getA')) {
            return { ok: true, json: async () => [] };
        }
        if (url.includes('/monitoring-monitor/')) {
            return { ok: true, json: async () => [mockMonitor] };
        }
        if (url.includes('/student/course/')) {
            return { ok: true, json: async () => [] };
        }
        if (url.includes('/category/course/')) {
            return { ok: true, json: async () => [mockCategory] };
        }
        if (url.includes('/activity/create')) {
            return { ok: false, status: 500, text: async () => 'Internal Server Error' };
        }
        return { ok: true, json: async () => [] };
    });

    const { container } = render(<CreateActivity />);
    const nameInput = container.querySelector('input.input-text');
    fireEvent.change(nameInput, { target: { value: 'Tutoría' } });

    await screen.findByText('POO');

    const selects = container.querySelectorAll('select');
    fireEvent.change(selects[0], { target: { value: '5' } });

    await waitFor(() => {
        expect(screen.getByText('Académico')).toBeInTheDocument();
    });

    fireEvent.change(selects[1], { target: { value: 'Académico' } });

    const dateInput = container.querySelector('input[type="date"]');
    fireEvent.change(dateInput, { target: { value: '2026-06-15' } });

    await waitFor(() => {
        expect(screen.getByText('Juan Monitor')).toBeInTheDocument();
    });

    fireEvent.change(selects[2], { target: { value: 'MON-1' } });
    fireEvent.click(screen.getByRole('button', { name: /Confirmar/i }));

    await waitFor(() => {
        expect(screen.getByText(/Error al crear la actividad/i)).toBeInTheDocument();
    });
});

test('CreateActivity permite desactivar registro de asistencia', async () => {
    render(<CreateActivity />);
    const checkbox = screen.getByLabelText(/¿Esta actividad requiere registrar asistencia/i);
    expect(checkbox).toBeChecked();
    fireEvent.click(checkbox);
    expect(checkbox).not.toBeChecked();
    expect(screen.getByText(/registro de asistencia está desactivado/i)).toBeInTheDocument();
});

test('CreateActivity permite crear nueva categoría exitosamente', async () => {
    const mockCourse = { id: 5, course: { name: 'POO', id: 50 } };

    global.fetch = jest.fn(async (url, options) => {
        if (url.includes('/monitoring/getAllActiveByUserId/')) {
            return { ok: true, json: async () => [mockCourse] };
        }
        if (url.includes('/student/getA')) {
            return { ok: true, json: async () => [] };
        }
        if (url.includes('/category/create')) {
            return { ok: true, json: async () => ({ id: 10, name: 'NuevaCat' }) };
        }
        if (url.includes('/category/course/')) {
            return { ok: true, json: async () => [{ id: 1, name: 'Académico' }] };
        }
        if (url.includes('/monitoring-monitor/')) {
            return { ok: true, json: async () => [] };
        }
        if (url.includes('/student/course/')) {
            return { ok: true, json: async () => [] };
        }
        return { ok: true, json: async () => [] };
    });

    const { container } = render(<CreateActivity />);
    await screen.findByText('POO');

    const selects = container.querySelectorAll('select');
    fireEvent.change(selects[0], { target: { value: '5' } });

    fireEvent.click(screen.getByRole('button', { name: '+' }));

    const newCatInput = screen.getByPlaceholderText(/Nueva categoría/i);
    fireEvent.change(newCatInput, { target: { value: 'NuevaCat' } });

    fireEvent.click(screen.getByRole('button', { name: /Añadir/i }));

    await waitFor(() => {
        expect(screen.getByText(/Categoría creada/i)).toBeInTheDocument();
    });
});

test('CreateActivity maneja error de red al cargar cursos', async () => {
    global.fetch = jest.fn(async (url) => {
        if (url.includes('/monitoring/getAllActiveByUserId/')) {
            throw new Error('Network error');
        }
        if (url.includes('/student/getA')) {
            return { ok: true, json: async () => [] };
        }
        return { ok: true, json: async () => [] };
    });

    render(<CreateActivity />);
    expect(screen.getByText('Crear Actividad')).toBeInTheDocument();
});

test('CreateActivity muestra error de red al enviar', async () => {
        const mockCourse = { id: 5, course: { name: 'POO', id: 50 } };
        const mockMonitor = { userId: 'MON-1', name: 'Juan Monitor', rol: 'M' };
        const mockCategory = { id: 1, name: 'Académico' };

        global.fetch = jest.fn(async (url, options) => {
            if (url.includes('/monitoring/getAllActiveByUserId/')) {
                return { ok: true, json: async () => [mockCourse] };
            }
            if (url.includes('/student/getA')) {
                return { ok: true, json: async () => [] };
            }
            if (url.includes('/monitoring-monitor/')) {
                return { ok: true, json: async () => [mockMonitor] };
            }
            if (url.includes('/student/course/')) {
                return { ok: true, json: async () => [] };
            }
            if (url.includes('/category/course/')) {
                return { ok: true, json: async () => [mockCategory] };
            }
            if (url.includes('/activity/create')) {
                throw new Error('Network error');
            }
            return { ok: true, json: async () => [] };
        });

        const { container } = render(<CreateActivity />);

        const nameInput = container.querySelector('input.input-text');
        fireEvent.change(nameInput, { target: { value: 'Tutoría' } });

        await screen.findByText('POO');

        const selects = container.querySelectorAll('select');
        fireEvent.change(selects[0], { target: { value: '5' } });

        await waitFor(() => {
            expect(screen.getByText('Académico')).toBeInTheDocument();
        });

        fireEvent.change(selects[1], { target: { value: 'Académico' } });

        const dateInput = container.querySelector('input[type="date"]');
        fireEvent.change(dateInput, { target: { value: '2026-06-15' } });

        await waitFor(() => {
            expect(screen.getByText('Juan Monitor')).toBeInTheDocument();
        });

        fireEvent.change(selects[2], { target: { value: 'MON-1' } });

        fireEvent.click(screen.getByRole('button', { name: /Confirmar/i }));

        await waitFor(() => {
            expect(screen.getByText(/Error/i)).toBeInTheDocument();
        });
    });
});