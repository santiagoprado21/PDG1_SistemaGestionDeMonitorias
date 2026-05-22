import React from 'react';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { BrowserRouter } from 'react-router-dom';
import VerticalNavbar from '../VerticalNavbar';

jest.mock('../NotificationIcon', () => () => <div data-testid="mock-notification">Noti</div>);
jest.mock('../PopUp', () => ({
    PopUp: ({ show, children }) => (show ? <div data-testid="mock-popup">{children}</div> : null)
}));

const setAuth = (role) => {
    const store = {
        role,
        userId: 'USR-1',
        token: 'Bearer token'
    };

    Object.defineProperty(window, 'localStorage', {
        value: {
            getItem: jest.fn((key) => store[key] || null),
            setItem: jest.fn((key, value) => {
                store[key] = value;
            }),
            clear: jest.fn()
        },
        writable: true
    });
};

const renderNavbar = () => render(<BrowserRouter><VerticalNavbar /></BrowserRouter>);

describe('VerticalNavbarSimple', () => {
    beforeEach(() => {
        jest.clearAllMocks();
        window.innerWidth = 1200;
        global.fetch = jest.fn(async (url) => {
            if (url.includes('/professor/profile/')) {
                return { ok: true, json: async () => ({ name: 'Profesor Uno' }) };
            }
            if (url.includes('/monitor/profile/')) {
                return { ok: true, json: async () => ({ name: 'Monitor Uno' }) };
            }
            if (url.includes('/department-head/profile/')) {
                return { ok: true, json: async () => ({ name: 'Jefe Uno' }) };
            }
            return { ok: true, json: async () => ({}) };
        });
    });

    test('student ve opciones de convocatorias y evaluación', () => {
        setAuth('student');
        renderNavbar();

        expect(screen.getByText(/Convocatorias Abiertas/i)).toBeInTheDocument();
        expect(screen.getByText(/Evaluacion de monitoria/i)).toBeInTheDocument();
        expect(screen.queryByText(/Reportes/i)).not.toBeInTheDocument();
    });

    test('professor ve menú de gestión y reportes', async () => {
        setAuth('professor');
        renderNavbar();

        expect(await screen.findByText(/Crear Convocatoria/i)).toBeInTheDocument();
        expect(screen.getByText(/Gestion de Rubricas/i)).toBeInTheDocument();
        expect(screen.getByText(/Mis Convocatorias/i)).toBeInTheDocument();
        expect(screen.getByText(/Plan de Actividades/i)).toBeInTheDocument();
        expect(screen.getByText(/Reportes/i)).toBeInTheDocument();
        await waitFor(() => expect(fetch).toHaveBeenCalledWith(expect.stringContaining('/professor/profile/USR-1'), expect.any(Object)));
    });

    test('jfedpto ve menú de aprobación/cierre y SIMON', async () => {
        setAuth('jfedpto');
        renderNavbar();

        expect(await screen.findByText(/Aprobar Convocatorias/i)).toBeInTheDocument();
        expect(screen.getByText(/Cerrar Monitorias/i)).toBeInTheDocument();
        expect(screen.getByText(/Crear Monitorias CSV/i)).toBeInTheDocument();
        expect(screen.getByText(/Generar Archivo SIMON/i)).toBeInTheDocument();
        expect(screen.getByText(/Resultados monitoria/i)).toBeInTheDocument();
        await waitFor(() => expect(fetch).toHaveBeenCalledWith(expect.stringContaining('/department-head/profile/USR-1'), expect.any(Object)));
    });

    test('monitor ve opciones operativas y puede cerrar sesión', async () => {
        setAuth('monitor');
        renderNavbar();

        expect(await screen.findByText(/Evaluar profesor/i)).toBeInTheDocument();
        expect(screen.getByText(/Mis Actividades/i)).toBeInTheDocument();
        expect(screen.getByText(/Mis Postulaciones/i)).toBeInTheDocument();

        fireEvent.click(screen.getByText(/Cerrar sesion/i));

        expect(window.localStorage.setItem).toHaveBeenCalledWith('role', '');
        expect(window.localStorage.setItem).toHaveBeenCalledWith('userId', '');
        expect(await screen.findByText(/Has cerrado sesión exitosamente/i)).toBeInTheDocument();
    });
});