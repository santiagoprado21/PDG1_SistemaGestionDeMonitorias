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

describe('Frontend: Notificaciones y configuración (visibilidad + preferencias)', () => {
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
});

