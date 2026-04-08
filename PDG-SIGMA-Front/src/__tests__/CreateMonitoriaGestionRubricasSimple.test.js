import React from 'react';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { BrowserRouter } from 'react-router-dom';
import CreateMonitoria from '../CreateMonitoria';
import GestionRubricas from '../GestionRubricas';

const mockNavigate = jest.fn();

jest.mock('react-router-dom', () => ({
    ...jest.requireActual('react-router-dom'),
    useNavigate: () => mockNavigate,
    Link: ({ children }) => <span>{children}</span>
}));

jest.mock('../VerticalNavbar', () => () => <div data-testid="mock-navbar">Navbar</div>);

jest.mock('../PopUp', () => ({
    PopUp: ({ show = true, onClose, children, message }) => {
        if (!show && !message) return null;
        return (
            <div data-testid="mock-popup">
                {children || message}
                {onClose && <button onClick={onClose}>Cerrar</button>}
            </div>
        );
    },
    PopUpUpdateBudget: ({ show }) => (show ? <div data-testid="popup-budget">Budget</div> : null)
}));

jest.mock('lucide-react', () => {
    const Icon = () => <span data-testid="mock-icon" />;
    return {
        BarChart3: Icon,
        Info: Icon,
        Plus: Icon,
        ClipboardList: Icon,
        Pencil: Icon,
        Trash2: Icon
    };
});

jest.mock('../config/ApiBackend', () => ({
    BACKEND_URL: 'http://localhost:5435',
    getApiUrl: (path) => `http://localhost:5435${path?.startsWith('/') ? path : `/${path || ''}`}`
}));

const renderWithRouter = (component) => render(<BrowserRouter>{component}</BrowserRouter>);

const setupLocalStorage = ({ role = 'professor', userId = 'PROF-1', token = 'Bearer test' } = {}) => {
    Object.defineProperty(window, 'localStorage', {
        value: {
            getItem: jest.fn((key) => {
                if (key === 'role') return role;
                if (key === 'userId') return userId;
                if (key === 'token') return token;
                return null;
            }),
            setItem: jest.fn(),
            clear: jest.fn()
        },
        writable: true
    });
};

describe('Simple tests for uncovered admin components', () => {
    beforeEach(() => {
        jest.clearAllMocks();
        global.fetch = jest.fn();
        setupLocalStorage();
    });

    test('CreateMonitoria renderiza formulario y carga datos iniciales', async () => {
        fetch.mockImplementation(async (url) => {
            if (url.includes('/school/getSchools')) {
                return { ok: true, json: async () => [{ name: 'Ingeniería' }] };
            }
            if (url.includes('/monitoring/getAllByProfessor/')) {
                return { ok: true, json: async () => [] };
            }
            if (url.includes('/program/getProgramsSchool')) {
                return { ok: true, json: async () => [] };
            }
            if (url.includes('/course/getCoursesProgram')) {
                return { ok: true, json: async () => [] };
            }
            return { ok: true, json: async () => [] };
        });

        renderWithRouter(<CreateMonitoria />);

        expect(screen.getByText(/Crear\/Cargar Monitorías/i)).toBeInTheDocument();
        expect(screen.getByText(/Nueva Monitoría/i)).toBeInTheDocument();

        await waitFor(() => {
            expect(fetch).toHaveBeenCalledWith('http://localhost:5435/school/getSchools');
            expect(fetch).toHaveBeenCalledWith('http://localhost:5435/monitoring/getAllByProfessor/PROF-1');
        });
    });

    test('CreateMonitoria valida requeridos al intentar crear vacío', async () => {
        fetch.mockImplementation(async (url) => {
            if (url.includes('/school/getSchools')) {
                return { ok: true, json: async () => [] };
            }
            if (url.includes('/monitoring/getAllByProfessor/')) {
                return { ok: true, json: async () => [] };
            }
            if (url.includes('/program/getProgramsSchool')) {
                return { ok: true, json: async () => [] };
            }
            if (url.includes('/course/getCoursesProgram')) {
                return { ok: true, json: async () => [] };
            }
            return { ok: true, json: async () => [] };
        });

        renderWithRouter(<CreateMonitoria />);

        const submit = await screen.findByRole('button', { name: /Crear Monitoría/i });
        fireEvent.click(submit);

        expect(await screen.findByText(/Por favor completa todos los campos requeridos/i)).toBeInTheDocument();
    });

    test('GestionRubricas renderiza estado vacío cuando no hay rúbricas', async () => {
        fetch.mockResolvedValue({ ok: true, json: async () => [] });

        renderWithRouter(<GestionRubricas />);

        expect(await screen.findByText(/Gestión de Rúbricas/i)).toBeInTheDocument();
        expect(await screen.findByText(/No hay rúbricas creadas/i)).toBeInTheDocument();
        expect(fetch).toHaveBeenCalledWith(
            'http://localhost:5435/api/rubric/professor/PROF-1',
            expect.any(Object)
        );
    });

    test('GestionRubricas abre modal y valida criterios incompletos al guardar', async () => {
        fetch.mockResolvedValue({ ok: true, json: async () => [] });

        renderWithRouter(<GestionRubricas />);

        fireEvent.click(await screen.findByRole('button', { name: /Crear Nueva Rúbrica/i }));
        expect(await screen.findByRole('heading', { name: /Nueva Rúbrica/i })).toBeInTheDocument();

        const createButton = screen.getByRole('button', { name: /Crear Rúbrica/i });
        const form = createButton.closest('form');
        fireEvent.submit(form);

        expect(await screen.findByText(/Todos los criterios deben tener nombre/i)).toBeInTheDocument();
    });
});