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

describe('CreateActivity and PlanActividades simple coverage', () => {
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
});