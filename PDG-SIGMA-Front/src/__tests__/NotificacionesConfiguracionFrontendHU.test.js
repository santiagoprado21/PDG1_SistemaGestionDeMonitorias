import React from 'react';
import { render, screen, waitFor, fireEvent } from '@testing-library/react';
import { BrowserRouter } from 'react-router-dom';
import NotificationIcon from '../NotificationIcon';

global.fetch = jest.fn();

jest.mock('../config/ApiBackend', () => ({
    BACKEND_URL: 'http://localhost:5435'
}));

const mockNavigate = jest.fn();
jest.mock('react-router-dom', () => ({
    ...jest.requireActual('react-router-dom'),
    useNavigate: () => mockNavigate
}));

global.Audio = jest.fn(() => ({ play: jest.fn() }));

const renderComponent = () =>
    render(
        <BrowserRouter>
            <NotificationIcon />
        </BrowserRouter>
    );

describe('NotificacionesConfiguracionFrontendHU', () => {
    beforeEach(() => {
        jest.clearAllMocks();

        Object.defineProperty(window, 'localStorage', {
            value: {
                getItem: jest.fn((key) => {
                    if (key === 'userId') return 'PROF-001';
                    if (key === 'role') return 'monitor'; // evita flujo SSE para estabilizar test
                    if (key === 'token') return 'Bearer token-test';
                    if (key === 'sigmaNotif.types') {
                        return JSON.stringify({
                            PROGRESS_UPDATE: true,
                            COMPLETED: true,
                            OVERDUE: true,
                            DUE_SOON: true,
                            CHAT: true
                        });
                    }
                    if (key === 'sigmaNotif.sound') return JSON.stringify(true);
                    return null;
                }),
                setItem: jest.fn(),
                clear: jest.fn()
            },
            writable: true
        });
    });

    test('Debe mostrar notificaciones visibles en el panel', async () => {
        const tomorrow = new Date();
        tomorrow.setDate(tomorrow.getDate() + 1);
        tomorrow.setHours(12, 0, 0, 0);

        fetch.mockImplementation(async (url) => {
            if (url.includes('/activity/findAll/PROF-001/monitor')) {
                return {
                    ok: true,
                    json: async () => [
                        { id: 99, name: 'Actividad 1', finish: tomorrow.toISOString(), state: 'PENDIENTE' }
                    ]
                };
            }
            if (url.includes('/chat/conversations/')) {
                return { ok: true, json: async () => [] };
            }
            if (url.includes('/chat/messages/')) {
                return { ok: true, json: async () => [] };
            }
            return { ok: true, json: async () => [] };
        });

        renderComponent();

        await waitFor(() => {
            expect(fetch).toHaveBeenCalledWith(
                'http://localhost:5435/activity/findAll/PROF-001/monitor',
                expect.any(Object)
            );
        });

        fireEvent.click(screen.getByTitle('Notificaciones'));

        await waitFor(() => {
            expect(screen.getByRole('heading', { name: /Notificaciones/i })).toBeInTheDocument();
        });
        expect(
            screen.queryByText(/Actividad 1/i) || screen.queryByText(/No hay notificaciones pendientes/i)
        ).toBeInTheDocument();
    });

    test('Debe permitir cambiar y guardar preferencias localmente', async () => {
        fetch.mockImplementation(async (url) => {
            if (url.includes('/activity/findAll/PROF-001/monitor')) {
                return { ok: true, json: async () => [] };
            }
            if (url.includes('/chat/conversations/')) {
                return { ok: true, json: async () => [] };
            }
            if (url.includes('/chat/messages/')) {
                return { ok: true, json: async () => [] };
            }
            return { ok: true, json: async () => [] };
        });

        renderComponent();

        fireEvent.click(screen.getByTitle('Notificaciones'));
        fireEvent.click(screen.getByRole('button', { name: /Preferencias/i }));

        await waitFor(() => {
            expect(screen.getByText(/Tipos a mostrar/i)).toBeInTheDocument();
        });

        fireEvent.click(screen.getByLabelText(/Activar sonido en nuevas notificaciones/i));
        fireEvent.click(screen.getByLabelText(/Atrasos/i));
        fireEvent.click(screen.getByRole('button', { name: /Guardar/i }));

        await waitFor(() => {
            expect(window.localStorage.setItem).toHaveBeenCalledWith('sigmaNotif.sound', 'false');
            expect(window.localStorage.setItem).toHaveBeenCalledWith(
                'sigmaNotif.types',
                expect.stringContaining('"OVERDUE":false')
            );
            expect(screen.getByText(/Preferencias guardadas/i)).toBeInTheDocument();
        });
    });

    test('Debe mostrar el icono de notificaciones sin alertas', async () => {
        fetch.mockImplementation(async () => ({ ok: true, json: async () => [] }));

        renderComponent();

        expect(await screen.findByTitle('Notificaciones')).toBeInTheDocument();
    });

    test('Debe mostrar mensaje de sin notificaciones pendientes', async () => {
        fetch.mockImplementation(async () => ({ ok: true, json: async () => [] }));

        renderComponent();

        fireEvent.click(screen.getByTitle('Notificaciones'));

        expect(await screen.findByText(/No hay notificaciones pendientes/i)).toBeInTheDocument();
    });

    test('Debe mostrar badge con cantidad de notificaciones', async () => {
        const tomorrow = new Date();
        tomorrow.setDate(tomorrow.getDate() + 1);
        fetch.mockImplementation(async (url) => {
            if (url.includes('/activity/findAll/PROF-001/monitor')) {
                return { ok: true, json: async () => [
                    { id: 1, name: 'Act 1', finish: tomorrow.toISOString(), state: 'PENDIENTE' },
                    { id: 2, name: 'Act 2', finish: tomorrow.toISOString(), state: 'PENDIENTE' }
                ] };
            }
            return { ok: true, json: async () => [] };
        });

        renderComponent();

        await waitFor(() => {
            expect(screen.getByText('2')).toBeInTheDocument();
        });
    });

    test('Profesor: carga notificaciones desde backend', async () => {
        window.localStorage.getItem.mockImplementation((key) => {
            if (key === 'userId') return 'PROF-001';
            if (key === 'role') return 'professor';
            if (key === 'token') return 'Bearer token-test';
            if (key === 'sigmaNotif.types') return JSON.stringify({ PROGRESS_UPDATE: true, COMPLETED: true, OVERDUE: true, DUE_SOON: true, CHAT: true });
            if (key === 'sigmaNotif.sound') return JSON.stringify(true);
            return null;
        });

        fetch.mockImplementation(async (url) => {
            if (url.includes('/notifications/unread/PROF-001')) {
                return { ok: true, json: async () => [
                    { id: 10, type: 'PROGRESS_UPDATE', message: 'Progreso actualizado', createdAt: new Date().toISOString(), activityId: 5 }
                ] };
            }
            return { ok: true, json: async () => [] };
        });

        renderComponent();

        await waitFor(() => {
            expect(fetch).toHaveBeenCalledWith(
                expect.stringContaining('/notifications/unread/PROF-001'),
                expect.any(Object)
            );
        });

        fireEvent.click(screen.getByTitle('Notificaciones'));

        expect(await screen.findByText(/Progreso actualizado/i)).toBeInTheDocument();
    });

    test('Profesor: muestra boton marcar todas como leidas', async () => {
        window.localStorage.getItem.mockImplementation((key) => {
            if (key === 'userId') return 'PROF-001';
            if (key === 'role') return 'professor';
            if (key === 'token') return 'Bearer token-test';
            if (key === 'sigmaNotif.types') return JSON.stringify({ PROGRESS_UPDATE: true, COMPLETED: true, OVERDUE: true, DUE_SOON: true, CHAT: true });
            if (key === 'sigmaNotif.sound') return JSON.stringify(true);
            return null;
        });

        fetch.mockImplementation(async (url) => {
            if (url.includes('/notifications/unread/PROF-001')) {
                return { ok: true, json: async () => [
                    { id: 10, type: 'PROGRESS_UPDATE', message: 'Progreso actualizado', createdAt: new Date().toISOString(), activityId: 5 }
                ] };
            }
            if (url.includes('/notifications/read-all/')) {
                return { ok: true, json: async () => ({}) };
            }
            return { ok: true, json: async () => [] };
        });

        renderComponent();
        fireEvent.click(screen.getByTitle('Notificaciones'));

        const markBtn = await screen.findByRole('button', { name: /Marcar todas como leídas/i });
        expect(markBtn).toBeInTheDocument();
        fireEvent.click(markBtn);

        await waitFor(() => {
            expect(fetch).toHaveBeenCalledWith(
                expect.stringContaining('/notifications/read-all/PROF-001'),
                expect.objectContaining({ method: 'PUT' })
            );
        });
    });

    test('Profesor: carga preferencias desde backend al abrir ajustes', async () => {
        window.localStorage.getItem.mockImplementation((key) => {
            if (key === 'userId') return 'PROF-001';
            if (key === 'role') return 'professor';
            if (key === 'token') return 'Bearer token-test';
            if (key === 'sigmaNotif.types') return JSON.stringify({ PROGRESS_UPDATE: true, COMPLETED: true, OVERDUE: true, DUE_SOON: true, CHAT: true });
            if (key === 'sigmaNotif.sound') return JSON.stringify(true);
            return null;
        });

        let prefFetchDone = false;
        fetch.mockImplementation(async (url) => {
            if (url.includes('/notifications/prefs/PROF-001')) {
                prefFetchDone = true;
                return { ok: true, json: async () => ({
                    enableSound: false,
                    enableProgressUpdate: false,
                    enableCompleted: false,
                    enableOverdue: true
                }) };
            }
            return { ok: true, json: async () => [] };
        });

        renderComponent();
        fireEvent.click(screen.getByTitle('Notificaciones'));
        fireEvent.click(screen.getByRole('button', { name: /Preferencias/i }));

        await waitFor(() => {
            expect(prefFetchDone).toBe(true);
        });
    });

    test('Chat: muestra alerta de chat cuando hay mensajes nuevos', async () => {
        window.localStorage.getItem.mockImplementation((key) => {
            if (key === 'userId') return 'PROF-001';
            if (key === 'role') return 'professor';
            if (key === 'token') return 'Bearer token-test';
            if (key === 'sigmaNotif.types') return JSON.stringify({ PROGRESS_UPDATE: true, COMPLETED: true, OVERDUE: true, DUE_SOON: true, CHAT: true });
            if (key === 'sigmaNotif.sound') return JSON.stringify(false);
            return null;
        });

        fetch.mockImplementation(async (url) => {
            if (url.includes('/chat/conversations/PROF-001/professor')) {
                return { ok: true, json: async () => [
                    { id: 'conv-1', title: 'Monitor: Juan Pérez' }
                ] };
            }
            if (url.includes('/chat/messages/conv-1')) {
                return { ok: true, json: async () => [
                    { id: 'msg-1', senderId: 'MON-001', createdAt: new Date().toISOString(), message: 'Hola profe' }
                ] };
            }
            return { ok: true, json: async () => [] };
        });

        renderComponent();

        await waitFor(() => {
            expect(fetch).toHaveBeenCalledWith(
                expect.stringContaining('/chat/conversations/PROF-001/professor'),
                expect.any(Object)
            );
        });

        fireEvent.click(screen.getByTitle('Notificaciones'));

        expect(await screen.findByText(/Nuevo mensaje en chat/i)).toBeInTheDocument();
    });
});

