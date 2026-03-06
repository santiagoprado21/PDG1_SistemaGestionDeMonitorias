import React from 'react';
import { render, screen, waitFor, fireEvent } from '@testing-library/react';
import { BrowserRouter } from 'react-router-dom';
import MisPostulaciones from '../MisPostulaciones';

// ===== MOCKS =====
global.fetch = jest.fn();

jest.mock('../VerticalNavbar', () => () => <div data-testid="vertical-navbar">Navbar</div>);
jest.mock('../LoadingSpinner', () => ({ message }) => <div data-testid="loading-spinner">{message}</div>);
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

// ===== DATOS DE PRUEBA =====
const mockPostulaciones = [
    {
        id: 1,
        monitoringRequestId: 101,
        courseName: 'Cálculo Diferencial',
        professorName: 'Juan García',
        requestedHours: 10,
        status: 'POSTULADO',
        requestStatus: 'CONVOCATORIA_ABIERTA',
        applicationDate: '2025-02-10T10:00:00',
        updatedAt: '2025-02-10T10:00:00',
        motivationLetter: 'Me apasiona la matemática y quiero compartir ese conocimiento.',
        notes: null
    },
    {
        id: 2,
        monitoringRequestId: 102,
        courseName: 'Programación I',
        professorName: 'Ana López',
        requestedHours: 8,
        status: 'SELECCIONADO',
        requestStatus: 'APROBADA',
        applicationDate: '2025-01-15T09:00:00',
        updatedAt: '2025-01-20T14:00:00',
        motivationLetter: 'Tengo experiencia en desarrollo de software.',
        notes: 'Monitor comprometido y con buenas notas.'
    },
    {
        id: 3,
        monitoringRequestId: 103,
        courseName: 'Física Mecánica',
        professorName: 'Carlos Ruiz',
        requestedHours: 12,
        status: 'NO_SELECCIONADO',
        requestStatus: 'MONITOR_SELECCIONADO',
        applicationDate: '2024-08-01T11:00:00',
        updatedAt: '2024-08-20T16:00:00',
        motivationLetter: 'Me gustan los retos de la física.',
        notes: null
    }
];

// ===== HELPERS =====
const renderComponent = () =>
    render(
        <BrowserRouter>
            <MisPostulaciones />
        </BrowserRouter>
    );

// ===== TESTS =====
describe('HU2-273: Mis Postulaciones - Vista del Monitor', () => {

    beforeEach(() => {
        jest.clearAllMocks();
        Object.defineProperty(window, 'localStorage', {
            value: {
                getItem: jest.fn((key) => {
                    if (key === 'role') return 'monitor';
                    if (key === 'userId') return 'MON-001';
                    if (key === 'token') return 'Bearer test-token';
                    return null;
                }),
                setItem: jest.fn(),
                clear: jest.fn()
            },
            writable: true
        });
    });

    afterEach(() => {
        jest.resetAllMocks();
    });

    test('Debe redirigir a "/" si el rol no es monitor', () => {
        window.localStorage.getItem.mockImplementation((key) => {
            if (key === 'role') return 'professor';
            return null;
        });

        fetch.mockResolvedValueOnce({ ok: true, json: async () => [] });
        renderComponent();

        expect(mockNavigate).toHaveBeenCalledWith('/');
    });

    test('Debe renderizar el título y el navbar correctamente', async () => {
        fetch.mockResolvedValueOnce({ ok: true, json: async () => [] });
        renderComponent();

        expect(screen.getByTestId('vertical-navbar')).toBeInTheDocument();
        expect(screen.getByText('📋 Mis Postulaciones')).toBeInTheDocument();
    });

    test('Debe mostrar el estado vacío cuando no hay postulaciones', async () => {
        fetch.mockResolvedValueOnce({ ok: true, json: async () => [] });
        renderComponent();

        await waitFor(() => {
            expect(screen.getByText(/No tienes postulaciones/i)).toBeInTheDocument();
        });
    });

    test('Debe mostrar las postulaciones correctamente con sus datos', async () => {
        fetch.mockResolvedValueOnce({ ok: true, json: async () => mockPostulaciones });
        renderComponent();

        await waitFor(() => {
            expect(screen.getByText('Cálculo Diferencial')).toBeInTheDocument();
            expect(screen.getByText('Programación I')).toBeInTheDocument();
            expect(screen.getByText('Física Mecánica')).toBeInTheDocument();
        });
    });

    test('Debe mostrar los badges de estado de postulación correctamente', async () => {
        fetch.mockResolvedValueOnce({ ok: true, json: async () => mockPostulaciones });
        renderComponent();

        await waitFor(() => {
            // "En revisión" aparece en badge, tab y stat → getAllByText
            expect(screen.getAllByText('En revisión').length).toBeGreaterThan(0);
            expect(screen.getByText('¡Seleccionado!')).toBeInTheDocument();
            // "No seleccionado" aparece en badge y tab → getAllByText
            expect(screen.getAllByText('No seleccionado').length).toBeGreaterThan(0);
        });
    });

    test('Debe mostrar los estados de la convocatoria correctamente', async () => {
        fetch.mockResolvedValueOnce({ ok: true, json: async () => mockPostulaciones });
        renderComponent();

        await waitFor(() => {
            expect(screen.getByText('Convocatoria abierta')).toBeInTheDocument();
            expect(screen.getByText('Aprobada')).toBeInTheDocument();
            expect(screen.getByText('Monitor seleccionado')).toBeInTheDocument();
        });
    });

    test('Debe mostrar las estadísticas de resumen correctamente', async () => {
        fetch.mockResolvedValueOnce({ ok: true, json: async () => mockPostulaciones });
        renderComponent();

        await waitFor(() => {
            expect(screen.getByText('Total')).toBeInTheDocument();
            // Estos textos aparecen en stats Y en tabs de filtro → getAllByText
            expect(screen.getAllByText('En revisión').length).toBeGreaterThan(0);
            expect(screen.getAllByText('Seleccionado').length).toBeGreaterThan(0);
            expect(screen.getAllByText('No seleccionado').length).toBeGreaterThan(0);
        });
    });

    test('Debe filtrar por estado "En revisión" correctamente', async () => {
        fetch.mockResolvedValueOnce({ ok: true, json: async () => mockPostulaciones });
        renderComponent();

        await waitFor(() => {
            expect(screen.getByText('Cálculo Diferencial')).toBeInTheDocument();
        });

        const filterButtons = screen.getAllByRole('button');
        const filtroEnRevision = filterButtons.find(btn => btn.textContent.includes('En revisión'));
        fireEvent.click(filtroEnRevision);

        await waitFor(() => {
            expect(screen.getByText('Cálculo Diferencial')).toBeInTheDocument();
            expect(screen.queryByText('Programación I')).not.toBeInTheDocument();
            expect(screen.queryByText('Física Mecánica')).not.toBeInTheDocument();
        });
    });

    test('Debe abrir el modal de detalle al hacer clic en una tarjeta', async () => {
        fetch.mockResolvedValueOnce({ ok: true, json: async () => mockPostulaciones });
        renderComponent();

        await waitFor(() => {
            expect(screen.getByText('Cálculo Diferencial')).toBeInTheDocument();
        });

        const cards = screen.getAllByText('Cálculo Diferencial');
        fireEvent.click(cards[0]);

        await waitFor(() => {
            expect(screen.getByText('Información de la convocatoria')).toBeInTheDocument();
            expect(screen.getByText('Tu postulación')).toBeInTheDocument();
            expect(screen.getByText('Tu carta de motivación')).toBeInTheDocument();
            expect(screen.getByText('Me apasiona la matemática y quiero compartir ese conocimiento.')).toBeInTheDocument();
        });
    });

    test('Debe mostrar el banner de seleccionado en el modal si la postulación fue exitosa', async () => {
        fetch.mockResolvedValueOnce({ ok: true, json: async () => mockPostulaciones });
        renderComponent();

        await waitFor(() => {
            expect(screen.getByText('Programación I')).toBeInTheDocument();
        });

        const cards = screen.getAllByText('Programación I');
        fireEvent.click(cards[0]);

        await waitFor(() => {
            expect(screen.getByText(/Felicitaciones/i)).toBeInTheDocument();
        });
    });

    test('Debe cerrar el modal al hacer clic en el botón cerrar', async () => {
        fetch.mockResolvedValueOnce({ ok: true, json: async () => mockPostulaciones });
        renderComponent();

        await waitFor(() => {
            expect(screen.getByText('Cálculo Diferencial')).toBeInTheDocument();
        });

        const cards = screen.getAllByText('Cálculo Diferencial');
        fireEvent.click(cards[0]);

        await waitFor(() => {
            expect(screen.getByText('Información de la convocatoria')).toBeInTheDocument();
        });

        fireEvent.click(screen.getByText('Cerrar'));

        await waitFor(() => {
            expect(screen.queryByText('Información de la convocatoria')).not.toBeInTheDocument();
        });
    });

    test('Debe mostrar un PopUp de error cuando falla la carga de postulaciones', async () => {
        fetch.mockRejectedValueOnce(new Error('Network error'));
        renderComponent();

        await waitFor(() => {
            expect(screen.getByTestId('popup')).toBeInTheDocument();
            expect(screen.getByText(/Error al cargar tus postulaciones/i)).toBeInTheDocument();
        });
    });

    test('Debe llamar al endpoint correcto con el monitorId del localStorage', async () => {
        fetch.mockResolvedValueOnce({ ok: true, json: async () => mockPostulaciones });
        renderComponent();

        await waitFor(() => {
            expect(fetch).toHaveBeenCalledWith(
                'http://localhost:5435/monitor-application/monitor/MON-001',
                expect.objectContaining({
                    method: 'GET',
                    headers: expect.objectContaining({
                        'Authorization': 'Bearer test-token'
                    })
                })
            );
        });
    });
});
