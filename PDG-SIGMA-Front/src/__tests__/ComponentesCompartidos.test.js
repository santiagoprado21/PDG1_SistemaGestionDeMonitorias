import React from 'react';
import { render, screen, fireEvent } from '@testing-library/react';
import { BrowserRouter } from 'react-router-dom';
import Alert from '../Alert';
import AlertElect from '../AlertElect';
import { PopUp, PopupDelete, PopUpUpdateBudget } from '../PopUp';
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
        expect(screen.getByText('La seleccion termino. Los estudiantes seran notificados')).toBeInTheDocument();
    });

    test('AlertElect no se renderiza cuando show es false', () => {
        render(<AlertElect show={false} onClose={jest.fn()} />);
        expect(screen.queryByRole('status')).not.toBeInTheDocument();
    });

    test('AlertElect acepta mensaje personalizado y tipo', () => {
        render(<AlertElect show={true} onClose={jest.fn()} message="Mensaje personalizado" type="error" />);
        expect(screen.getByText('Mensaje personalizado')).toBeInTheDocument();
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

    test('PopupDelete debe mostrar confirmacion y ejecutar onApply', () => {
        const onApply = jest.fn();
        const onClose = jest.fn();
        render(<PopupDelete show={true} onClose={onClose} onApply={onApply} />);

        expect(screen.getByText(/¿Estás seguro de la acción a realizar/i)).toBeInTheDocument();
        fireEvent.click(screen.getByRole('button', { name: /Sí, Eliminar/i }));
        expect(onApply).toHaveBeenCalledTimes(1);
    });

    test('PopupDelete no se renderiza cuando show es false', () => {
        render(<PopupDelete show={false} onClose={jest.fn()} onApply={jest.fn()} />);
        expect(screen.queryByText(/¿Estás seguro/i)).not.toBeInTheDocument();
    });

    test('PopUpUpdateBudget debe renderizar y actualizar presupuesto', () => {
        const onSubmit = jest.fn();
        const onClose = jest.fn();
        render(<PopUpUpdateBudget show={true} onClose={onClose} onSubmit={onSubmit} initialHours={10} initialRate={15000} />);

        expect(screen.getByText(/Editar presupuesto/i)).toBeInTheDocument();
        expect(screen.getByText(/10 horas/i)).toBeInTheDocument();

        fireEvent.click(screen.getByRole('button', { name: /Actualizar presupuesto/i }));
        expect(onSubmit).toHaveBeenCalledWith(10, 15000);
    });

    test('PopUpUpdateBudget no se renderiza cuando show es false', () => {
        render(<PopUpUpdateBudget show={false} onClose={jest.fn()} onSubmit={jest.fn()} />);
        expect(screen.queryByText(/Editar presupuesto/i)).not.toBeInTheDocument();
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

