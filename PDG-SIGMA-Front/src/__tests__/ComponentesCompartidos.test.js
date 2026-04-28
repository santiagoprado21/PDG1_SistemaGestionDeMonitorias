import React from 'react';
import { render, screen, fireEvent } from '@testing-library/react';
import { BrowserRouter } from 'react-router-dom';
import Alert from '../Alert';
import AlertElect from '../AlertElect';
import { PopUp } from '../PopUp';
import VerticalNavbar from '../VerticalNavbar';
import { getApiUrl } from '../config/ApiBackend';

global.fetch = jest.fn();

jest.mock('../NotificationIcon', () => () => <div data-testid="notification-icon">Notifs</div>);
jest.mock('../img/logo2.png', () => 'logo-mock');

describe('ComponentesCompartidos', () => {
    beforeEach(() => {
        jest.clearAllMocks();
        fetch.mockImplementation(() =>
            Promise.resolve({ ok: true, json: async () => ({ name: 'Usuario Test' }) })
        );
        Object.defineProperty(window, 'localStorage', {
            value: {
                getItem: jest.fn((key) => {
                    if (key === 'role') return 'student';
                    if (key === 'userId') return 'M-1';
                    if (key === 'token') return 'Bearer test-token';
                    return null;
                }),
                setItem: jest.fn(),
                clear: jest.fn()
            },
            writable: true
        });
    });

    test('Alert debe mostrar mensaje por defecto y cerrar automáticamente', () => {
        jest.useFakeTimers();
        const onClose = jest.fn();
        render(<Alert show={true} onClose={onClose} />);

        expect(screen.getByRole('status')).toHaveTextContent('Iniciaste sesion como estudiante');
        jest.advanceTimersByTime(3000);
        expect(onClose).toHaveBeenCalledTimes(1);
        jest.useRealTimers();
    });

    test('AlertElect debe mostrar mensaje esperado', () => {
        render(<AlertElect show={true} onClose={jest.fn()} />);
        expect(screen.getByRole('status')).toHaveTextContent('La selección terminó. Los estudiantes serán notificados');
    });

    test('PopUp no renderiza contenido cuando show es false', () => {
        render(<PopUp show={false} onClose={jest.fn()}>Oculto</PopUp>);
        expect(screen.queryByText('Oculto')).not.toBeInTheDocument();
    });

    test('PopUp debe ejecutar onClose al confirmar', () => {
        const onClose = jest.fn();
        render(<PopUp show={true} onClose={onClose}>Contenido popup</PopUp>);

        fireEvent.click(screen.getByRole('button', { name: 'OK' }));
        expect(onClose).toHaveBeenCalledTimes(1);
    });

    test('VerticalNavbar debe mostrar accesos base y notificaciones', async () => {
        render(
            <BrowserRouter>
                <VerticalNavbar />
            </BrowserRouter>
        );

        expect(await screen.findByText(/Convocatorias Abiertas/i)).toBeInTheDocument();
        expect(screen.getByText(/Evaluacion de monitoria/i)).toBeInTheDocument();
        expect(screen.getByTestId('notification-icon')).toBeInTheDocument();
    });

    test('getApiUrl debe construir URL con y sin slash inicial', () => {
        expect(getApiUrl('/health')).toBe('http://localhost:5433/health');
        expect(getApiUrl('monitoring-request/open')).toBe('http://localhost:5433/monitoring-request/open');
    });

    test('getApiUrl debe manejar path vacío', () => {
        expect(getApiUrl('')).toBe('http://localhost:5433/');
    });
});

