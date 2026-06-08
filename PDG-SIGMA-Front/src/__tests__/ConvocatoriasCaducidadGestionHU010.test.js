import React from 'react';
import { render, screen, waitFor, fireEvent, within } from '@testing-library/react';
import { BrowserRouter } from 'react-router-dom';
import MisConvocatorias from '../MisConvocatorias';

global.fetch = jest.fn();

jest.mock('../VerticalNavbar', () => () => <div data-testid="vertical-navbar">Navbar</div>);
jest.mock('../LoadingSpinner', () => () => <div data-testid="loading-spinner">Loading...</div>);
jest.mock('../PopUp', () => ({
    PopUp: ({ show, children }) => show ? <div data-testid="popup">{children}</div> : null
}));
jest.mock('../config/ApiBackend', () => ({
    BACKEND_URL: 'http://localhost:5435'
}));

const mockNavigate = jest.fn();
jest.mock('react-router-dom', () => ({
    ...jest.requireActual('react-router-dom'),
    useNavigate: () => mockNavigate
}));

const convocatoriasMock = [
    {
        id: 101,
        courseName: 'Bases de Datos',
        programName: 'Ingeniería de Sistemas',
        semester: '2026-1',
        requestedHours: 8,
        status: 'CONVOCATORIA_ABIERTA',
        applicationCount: 3,
        createdAt: '2026-01-10T08:00:00'
    },
    {
        id: 102,
        courseName: 'Estructuras de Datos',
        programName: 'Ingeniería de Sistemas',
        semester: '2026-1',
        requestedHours: 6,
        status: 'APROBADA',
        applicationCount: 2,
        createdAt: '2026-01-05T08:00:00'
    }
];

const renderComponent = () =>
    render(
        <BrowserRouter>
            <MisConvocatorias />
        </BrowserRouter>
    );

describe('ConvocatoriasCaducidadGestionHU010', () => {
    beforeEach(() => {
        jest.clearAllMocks();
        fetch.mockImplementation(async () => ({
            ok: true,
            json: async () => convocatoriasMock
        }));
        Object.defineProperty(window, 'localStorage', {
            value: {
                getItem: jest.fn((key) => {
                    if (key === 'userId') return 'PROF-001';
                    if (key === 'token') return 'Bearer token-test';
                    return null;
                }),
                setItem: jest.fn(),
                clear: jest.fn()
            },
            writable: true
        });
    });

    test('Debe reflejar convocatorias cerradas (APROBADA) sin accion de postulantes', async () => {
        renderComponent();

        const statusSelect = screen.getAllByRole('combobox')[0];
        fireEvent.change(statusSelect, { target: { value: 'Todos' } });

        const table = await screen.findByRole('table');
        await waitFor(() => {
            expect(within(table).getByText('Estructuras de Datos')).toBeInTheDocument();
        });

        const closedRow = within(table).getByText('Estructuras de Datos').closest('tr');
        expect(within(closedRow).getAllByText('Cerrada').length).toBeGreaterThan(0);
        expect(within(closedRow).queryByRole('button')).not.toBeInTheDocument();

        const openRow = within(table).getByText('Bases de Datos').closest('tr');
        expect(within(openRow).getByText('Abierta')).toBeInTheDocument();
    });

    test('Debe permitir filtrar por estado cerrado y mostrar solo convocatorias APROBADAS', async () => {
        renderComponent();
        const table = await screen.findByRole('table');

        await waitFor(() => {
            expect(within(table).getByText('Bases de Datos')).toBeInTheDocument();
        });

        const statusSelect = screen.getAllByRole('combobox')[0];
        fireEvent.change(statusSelect, { target: { value: 'APROBADA' } });

        await waitFor(() => {
            const tableRows = within(table).getAllByRole('row');
            expect(tableRows.some((row) => row.textContent.includes('Estructuras de Datos'))).toBe(true);
            expect(tableRows.some((row) => row.textContent.includes('Bases de Datos'))).toBe(false);
        });
    });

    test('Debe mostrar popup de error ante fallo de carga (caso extremo de gestion)', async () => {
        fetch.mockRejectedValueOnce(new Error('Fallo de red'));
        renderComponent();

        await waitFor(() => {
            expect(screen.getByTestId('popup')).toBeInTheDocument();
            expect(screen.getByText(/Error cargando convocatorias/i)).toBeInTheDocument();
        });
    });

    test('Debe filtrar por curso correctamente', async () => {
        renderComponent();

        const table = await screen.findByRole('table');
        await waitFor(() => {
            expect(within(table).getByText('Bases de Datos')).toBeInTheDocument();
        });

        const courseSelect = screen.getAllByRole('combobox')[2];
        fireEvent.change(courseSelect, { target: { value: 'Bases de Datos' } });

        await waitFor(() => {
            expect(within(table).getByText('Bases de Datos')).toBeInTheDocument();
            expect(within(table).queryByText('Estructuras de Datos')).not.toBeInTheDocument();
        });
    });

    test('Debe mostrar "No se encontraron convocatorias" cuando filtro no coincide', async () => {
        renderComponent();

        const table = await screen.findByRole('table');
        await waitFor(() => {
            expect(within(table).getByText('Bases de Datos')).toBeInTheDocument();
        });

        const statusSelect = screen.getAllByRole('combobox')[0];
        fireEvent.change(statusSelect, { target: { value: 'RECHAZADA' } });

        await waitFor(() => {
            expect(screen.getByText(/No se encontraron convocatorias/i)).toBeInTheDocument();
        });
    });

    test('Debe mostrar estado RECHAZADA correctamente', async () => {
        fetch.mockImplementation(async () => ({
            ok: true,
            json: async () => [{
                id: 201,
                courseName: 'Rechazado Course',
                programName: 'Ingenieria',
                semester: '2026-1',
                requestedHours: 8,
                status: 'RECHAZADA',
                applicationCount: 0,
                createdAt: '2026-01-10T08:00:00'
            }]
        }));

        renderComponent();

        await waitFor(() => {
            expect(screen.getByText('Rechazado Course')).toBeInTheDocument();
        });
    });

    test('Debe manejar error de respuesta del servidor en carga de convocatorias', async () => {
        fetch.mockImplementation(async () => ({
            ok: false,
            status: 500,
            json: async () => ({ error: 'Internal server error' })
        }));

        renderComponent();

        await waitFor(() => {
            expect(screen.getByTestId('popup')).toBeInTheDocument();
        });
    });

    test('Debe cambiar filtro de semestre', async () => {
        renderComponent();

        const table = await screen.findByRole('table');
        await waitFor(() => {
            expect(within(table).getByText('Bases de Datos')).toBeInTheDocument();
        });

        const semesterSelect = screen.getAllByRole('combobox')[1];
        fireEvent.change(semesterSelect, { target: { value: '2026-1' } });

        await waitFor(() => {
            expect(within(table).getByText('Bases de Datos')).toBeInTheDocument();
        });
    });

    test('Debe cambiar registros por pagina', async () => {
        renderComponent();

        const table = await screen.findByRole('table');
        await waitFor(() => {
            expect(within(table).getByText('Bases de Datos')).toBeInTheDocument();
        });

        const pageSizeSelect = screen.getAllByRole('combobox')[3];
        fireEvent.change(pageSizeSelect, { target: { value: '10' } });
    });
});

