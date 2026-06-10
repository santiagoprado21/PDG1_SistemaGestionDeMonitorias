import React, { act } from 'react';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { BrowserRouter, MemoryRouter } from 'react-router-dom';
import VerticalNavbar from '../VerticalNavbar';

jest.mock('../NotificationIcon', () => () => <div data-testid="mock-notification">Noti</div>);
jest.mock('../PopUp', () => ({
    PopUp: ({ show, onClose, children }) => (show ? <div data-testid="mock-popup"><button data-testid="popup-close-btn" onClick={onClose}>X</button>{children}</div> : null)
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

    test('no hay sesion no causa error', () => {
        const store = {};
        Object.defineProperty(window, 'localStorage', {
            value: {
                getItem: jest.fn((key) => store[key] || null),
                setItem: jest.fn(),
                clear: jest.fn()
            },
            writable: true
        });
        renderNavbar();
        expect(screen.queryByText(/Convocatorias Abiertas/i)).not.toBeInTheDocument();
    });

    test('professor maneja error HTTP en fetch de perfil', async () => {
        global.fetch = jest.fn(async () => ({ ok: false, json: async () => 'Not Found' }));
        setAuth('professor');
        renderNavbar();
        await waitFor(() =>
            expect(fetch).toHaveBeenCalledWith(expect.stringContaining('/professor/profile/USR-1'), expect.any(Object))
        );
    });

    test('professor recibe respuesta vacia', async () => {
        global.fetch = jest.fn(async () => ({ ok: true, json: async () => null }));
        setAuth('professor');
        renderNavbar();
        await waitFor(() =>
            expect(fetch).toHaveBeenCalledWith(expect.stringContaining('/professor/profile/USR-1'), expect.any(Object))
        );
    });

    test('monitor maneja error HTTP en fetch de perfil', async () => {
        global.fetch = jest.fn(async () => ({ ok: false, json: async () => 'Not Found' }));
        setAuth('monitor');
        renderNavbar();
        await waitFor(() =>
            expect(fetch).toHaveBeenCalledWith(expect.stringContaining('/monitor/profile/USR-1'), expect.any(Object))
        );
    });

    test('monitor recibe respuesta vacia', async () => {
        global.fetch = jest.fn(async () => ({ ok: true, json: async () => null }));
        setAuth('monitor');
        renderNavbar();
        await waitFor(() =>
            expect(fetch).toHaveBeenCalledWith(expect.stringContaining('/monitor/profile/USR-1'), expect.any(Object))
        );
    });

    test('jfedpto maneja error HTTP en fetch de perfil', async () => {
        global.fetch = jest.fn(async () => ({ ok: false, json: async () => 'Not Found' }));
        setAuth('jfedpto');
        renderNavbar();
        await waitFor(() =>
            expect(fetch).toHaveBeenCalledWith(expect.stringContaining('/department-head/profile/USR-1'), expect.any(Object))
        );
    });

    test('jfedpto recibe respuesta vacia', async () => {
        global.fetch = jest.fn(async () => ({ ok: true, json: async () => null }));
        setAuth('jfedpto');
        renderNavbar();
        await waitFor(() =>
            expect(fetch).toHaveBeenCalledWith(expect.stringContaining('/department-head/profile/USR-1'), expect.any(Object))
        );
    });

    test('resize handler actualiza vista mobile', () => {
        setAuth('student');
        renderNavbar();

        act(() => {
            window.innerWidth = 500;
            window.dispatchEvent(new Event('resize'));
        });
        expect(screen.getByLabelText(/Abrir menu lateral/i)).toBeInTheDocument();

        act(() => {
            window.innerWidth = 1200;
            window.dispatchEvent(new Event('resize'));
        });
        expect(screen.queryByLabelText(/Abrir menu lateral/i)).not.toBeInTheDocument();
    });

    test('sidebar hover en desktop', () => {
        setAuth('student');
        renderNavbar();
        const sidebar = document.querySelector('.vertical-navbar');
        fireEvent.mouseEnter(sidebar);
        expect(sidebar.className).toContain('expanded');
        fireEvent.mouseLeave(sidebar);
        expect(sidebar.className).toContain('collapsed');
    });

    test('mobile toggle y click en item', () => {
        window.innerWidth = 500;
        setAuth('student');
        renderNavbar();

        fireEvent.click(screen.getByLabelText(/Abrir menu lateral/i));
        expect(screen.getByLabelText(/Cerrar menu lateral/i)).toBeInTheDocument();

        fireEvent.click(screen.getByText(/Convocatorias Abiertas/i));
        expect(screen.getByLabelText(/Abrir menu lateral/i)).toBeInTheDocument();
    });

    test('mobile logout cierra menu', async () => {
        window.innerWidth = 500;
        setAuth('professor');
        renderNavbar();

        fireEvent.click(screen.getByLabelText(/Abrir menu lateral/i));
        fireEvent.click(screen.getByText(/Cerrar sesion/i));

        expect(screen.getByLabelText(/Abrir menu lateral/i)).toBeInTheDocument();
        expect(await screen.findByText(/Has cerrado sesión exitosamente/i)).toBeInTheDocument();
    });

    test('nombre compuesto genera iniciales correctas', async () => {
        global.fetch = jest.fn(async () => ({ ok: true, json: async () => ({ name: 'Ana Maria Lopez' }) }));
        setAuth('professor');
        renderNavbar();
        await waitFor(() => {
            expect(screen.getByText('AL')).toBeInTheDocument();
        });
    });

    test('popup se puede cerrar', async () => {
        setAuth('professor');
        renderNavbar();

        fireEvent.click(screen.getByText(/Cerrar sesion/i));
        expect(await screen.findByTestId('mock-popup')).toBeInTheDocument();

        fireEvent.click(screen.getByTestId('popup-close-btn'));
        expect(screen.queryByTestId('mock-popup')).not.toBeInTheDocument();
    });

    test('monitor hover expand/collapse en desktop', () => {
        window.innerWidth = 1200;
        setAuth('monitor');
        render(<BrowserRouter><VerticalNavbar /></BrowserRouter>);
        const sidebar = document.querySelector('.vertical-navbar');
        fireEvent.mouseEnter(sidebar);
        expect(sidebar.className).toContain('expanded');
        fireEvent.mouseLeave(sidebar);
        expect(sidebar.className).toContain('collapsed');
    });

    test('professor chat link activo en ruta /chat', () => {
        setAuth('professor');
        render(<MemoryRouter initialEntries={['/chat']}><VerticalNavbar /></MemoryRouter>);
        const chatLink = screen.getByText('Chat');
        expect(chatLink.closest('a').className).toContain('active');
    });

    test('jfedpto renderiza todos los links del departamento', async () => {
        setAuth('jfedpto');
        renderNavbar();
        expect(await screen.findByText(/Aprobar Convocatorias/i)).toBeInTheDocument();
        expect(screen.getByText(/Cerrar Monitorias/i)).toBeInTheDocument();
        expect(screen.getByText(/Crear Monitorias CSV/i)).toBeInTheDocument();
        expect(screen.getByText(/Generar Archivo SIMON/i)).toBeInTheDocument();
        expect(screen.getByText(/Resultados monitoria/i)).toBeInTheDocument();
        expect(screen.getByText(/Gestion encuesta monitores/i)).toBeInTheDocument();
        expect(screen.getByText(/Gestion encuesta profesores/i)).toBeInTheDocument();
    });
});