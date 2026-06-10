import React from 'react';
import { render, screen, fireEvent } from '@testing-library/react';
import VerConvocatorias from '../VerConvocatorias';

global.fetch = jest.fn();

jest.mock('../VerticalNavbar', () => () => <div data-testid="mock-navbar">Navbar</div>);
jest.mock('../LoadingSpinner', () => () => <div data-testid="loading-spinner">Loading...</div>);
jest.mock('../PopUp', () => ({
    PopUp: ({ show, children }) => show ? <div data-testid="mock-popup">{children}</div> : null
}));
jest.mock('../config/ApiBackend', () => ({
    BACKEND_URL: 'http://localhost:5435'
}));

const mockConvocatorias = [
    { id: 1, courseName: 'Estructuras de Datos', professorName: 'Prof. Juan', programName: 'Ingeniería de Sistemas', schoolName: 'Ingeniería', semester: '2026-1', requestedHours: 80, applicationCount: 2 },
    { id: 2, courseName: 'Cálculo I', professorName: 'Prof. Ana', programName: 'Matemáticas', schoolName: 'Ciencias', semester: '2026-1', requestedHours: 60, applicationCount: 1 }
];

const mockApplications = [
    { monitoringRequestId: 1, id: 100, status: 'POSTULADO' }
];

const defaultFetch = (url) => {
    if (url.includes('/monitoring-request/open')) {
        return Promise.resolve({ ok: true, json: () => Promise.resolve(mockConvocatorias) });
    }
    if (url.includes('/monitor-application/monitor/')) {
        return Promise.resolve({ ok: true, json: () => Promise.resolve(mockApplications) });
    }
    return Promise.resolve({ ok: true, json: () => Promise.resolve([]) });
};

function setLocalStorage(getItemImpl) {
    Object.defineProperty(window, 'localStorage', {
        value: {
            getItem: jest.fn(getItemImpl),
            setItem: jest.fn(),
            clear: jest.fn()
        },
        writable: true
    });
}

describe('VerConvocatorias', () => {
    beforeEach(() => {
        jest.clearAllMocks();
        fetch.mockImplementation(defaultFetch);
        setLocalStorage((key) => {
            if (key === 'userId') return 'MON-001';
            if (key === 'token') return 'Bearer token';
            if (key === 'role') return 'student';
            return null;
        });
    });

    test('renderiza titulo y navbar', async () => {
        render(<VerConvocatorias />);
        expect(screen.getByTestId('mock-navbar')).toBeInTheDocument();
        expect(await screen.findByText('Convocatorias de Monitoría Abiertas')).toBeInTheDocument();
    });

    test('carga y muestra lista de convocatorias', async () => {
        render(<VerConvocatorias />);
        expect(await screen.findByText('Estructuras de Datos')).toBeInTheDocument();
        expect(screen.getByText('Cálculo I')).toBeInTheDocument();
        expect(screen.getByText('2 convocatorias')).toBeInTheDocument();
    });

    test('muestra estado vacio cuando no hay convocatorias', async () => {
        fetch.mockImplementation(async (url) => {
            if (url.includes('/monitoring-request/open')) {
                return { ok: true, json: () => Promise.resolve([]) };
            }
            return { ok: true, json: () => Promise.resolve([]) };
        });
        render(<VerConvocatorias />);
        expect(await screen.findByText('No hay convocatorias disponibles')).toBeInTheDocument();
    });

    test('filtra convocatorias por programa', async () => {
        render(<VerConvocatorias />);
        await screen.findByText('Estructuras de Datos');
        fireEvent.change(screen.getByRole('combobox'), { target: { value: 'Matemáticas' } });
        expect(screen.queryByText('Estructuras de Datos')).not.toBeInTheDocument();
        expect(screen.getByText('Cálculo I')).toBeInTheDocument();
    });

    test('filtro Todos muestra todas las convocatorias', async () => {
        render(<VerConvocatorias />);
        await screen.findByText('Estructuras de Datos');
        fireEvent.change(screen.getByRole('combobox'), { target: { value: 'Matemáticas' } });
        expect(screen.queryByText('Estructuras de Datos')).not.toBeInTheDocument();
        fireEvent.change(screen.getByRole('combobox'), { target: { value: 'Todos' } });
        expect(screen.getByText('Estructuras de Datos')).toBeInTheDocument();
        expect(screen.getByText('Cálculo I')).toBeInTheDocument();
    });

    test('muestra Ya te postulaste cuando ya aplico', async () => {
        render(<VerConvocatorias />);
        expect(await screen.findByText(/Ya te postulaste/)).toBeInTheDocument();
    });

    test('abre modal de postulacion', async () => {
        fetch.mockImplementation(async (url) => {
            if (url.includes('/monitoring-request/open')) {
                return { ok: true, json: () => Promise.resolve(mockConvocatorias) };
            }
            if (url.includes('/monitor-application/monitor/')) {
                return { ok: true, json: () => Promise.resolve([]) };
            }
            return { ok: true, json: () => Promise.resolve([]) };
        });
        render(<VerConvocatorias />);
        await screen.findByText('Estructuras de Datos');
        fireEvent.click(screen.getAllByRole('button', { name: /Postularse/i })[0]);
        expect(screen.getByText('Postularse a Monitoría')).toBeInTheDocument();
    });

    test('cierra modal con boton Cancelar', async () => {
        fetch.mockImplementation(async (url) => {
            if (url.includes('/monitoring-request/open')) {
                return { ok: true, json: () => Promise.resolve(mockConvocatorias) };
            }
            if (url.includes('/monitor-application/monitor/')) {
                return { ok: true, json: () => Promise.resolve([]) };
            }
            return { ok: true, json: () => Promise.resolve([]) };
        });
        render(<VerConvocatorias />);
        await screen.findByText('Estructuras de Datos');
        fireEvent.click(screen.getAllByRole('button', { name: /Postularse/i })[0]);
        expect(screen.getByText('Postularse a Monitoría')).toBeInTheDocument();
        fireEvent.click(screen.getByRole('button', { name: /Cancelar/i }));
        expect(screen.queryByText('Postularse a Monitoría')).not.toBeInTheDocument();
    });

    test('validacion de 50 caracteres en carta de motivacion', async () => {
        fetch.mockImplementation(async (url) => {
            if (url.includes('/monitoring-request/open')) {
                return { ok: true, json: () => Promise.resolve(mockConvocatorias) };
            }
            if (url.includes('/monitor-application/monitor/')) {
                return { ok: true, json: () => Promise.resolve([]) };
            }
            return { ok: true, json: () => Promise.resolve([]) };
        });
        render(<VerConvocatorias />);
        await screen.findByText('Estructuras de Datos');
        fireEvent.click(screen.getAllByRole('button', { name: /Postularse/i })[0]);
        const textarea = screen.getByPlaceholderText(/Explica por qué/i);
        fireEvent.change(textarea, { target: { value: 'Corta' } });
        const submitBtn = screen.getByRole('button', { name: /Enviar Postulación/i });
        expect(submitBtn).toBeDisabled();
        fireEvent.change(textarea, { target: { value: 'A'.repeat(60) } });
        expect(submitBtn).not.toBeDisabled();
    });

    test('envia postulacion exitosamente', async () => {
        fetch.mockImplementation(async (url) => {
            if (url.includes('/monitoring-request/open')) {
                return { ok: true, json: () => Promise.resolve(mockConvocatorias) };
            }
            if (url.includes('/monitor-application/monitor/')) {
                return { ok: true, json: () => Promise.resolve([]) };
            }
            if (url.includes('/monitor-application/apply')) {
                return { ok: true, json: () => Promise.resolve({ message: 'ok' }) };
            }
            return { ok: true, json: () => Promise.resolve([]) };
        });
        render(<VerConvocatorias />);
        await screen.findByText('Estructuras de Datos');
        fireEvent.click(screen.getAllByRole('button', { name: /Postularse/i })[0]);
        const textarea = screen.getByPlaceholderText(/Explica por qué/i);
        fireEvent.change(textarea, { target: { value: 'A'.repeat(50) } });
        fireEvent.click(screen.getByRole('button', { name: /Enviar Postulación/i }));
        expect(await screen.findByText(/Has enviado tu postulación/i)).toBeInTheDocument();
    });

    test('error cuando rol no es estudiante', async () => {
        setLocalStorage((key) => {
            if (key === 'userId') return 'PROF-001';
            if (key === 'token') return 'Bearer token';
            if (key === 'role') return 'professor';
            return null;
        });
        render(<VerConvocatorias />);
        await screen.findByText('Estructuras de Datos');
        fireEvent.click(screen.getAllByRole('button', { name: /Postularse/i })[0]);
        expect(await screen.findByText(/Solo los estudiantes pueden postularse/i)).toBeInTheDocument();
    });

    test('error en carga de convocatorias', async () => {
        fetch.mockImplementation(async (url) => {
            if (url.includes('/monitoring-request/open')) {
                throw new Error('Network error');
            }
            return { ok: true, json: () => Promise.resolve([]) };
        });
        render(<VerConvocatorias />);
        expect(await screen.findByText(/Error al cargar convocatorias/i)).toBeInTheDocument();
    });

    test('muestra paginacion con 8 convocatorias', async () => {
        const many = [];
        for (let i = 0; i < 8; i++) {
            many.push({ id: i + 1, courseName: 'Curso ' + (i + 1), professorName: 'Prof', programName: 'Ingeniería de Sistemas', schoolName: 'Ingeniería', semester: '2026-1', requestedHours: 80, applicationCount: 0 });
        }
        fetch.mockImplementation(async (url) => {
            if (url.includes('/monitoring-request/open')) {
                return { ok: true, json: () => Promise.resolve(many) };
            }
            if (url.includes('/monitor-application/monitor/')) {
                return { ok: true, json: () => Promise.resolve([]) };
            }
            return { ok: true, json: () => Promise.resolve([]) };
        });
        render(<VerConvocatorias />);
        expect(await screen.findByText('Página 1 de 2')).toBeInTheDocument();
        fireEvent.click(screen.getByText(/Siguiente/i));
        expect(screen.getByText('Página 2 de 2')).toBeInTheDocument();
        fireEvent.click(screen.getByText(/Anterior/i));
        expect(screen.getByText('Página 1 de 2')).toBeInTheDocument();
    });

    test('error HTTP en POST de postulacion', async () => {
        fetch.mockImplementation(async (url) => {
            if (url.includes('/monitoring-request/open')) {
                return { ok: true, json: () => Promise.resolve(mockConvocatorias) };
            }
            if (url.includes('/monitor-application/monitor/')) {
                return { ok: true, json: () => Promise.resolve([]) };
            }
            if (url.includes('/monitor-application/apply')) {
                return { ok: false, text: () => Promise.resolve('Ya existe') };
            }
            return { ok: true, json: () => Promise.resolve([]) };
        });
        render(<VerConvocatorias />);
        await screen.findByText('Estructuras de Datos');
        fireEvent.click(screen.getAllByRole('button', { name: /Postularse/i })[0]);
        const textarea = screen.getByPlaceholderText(/Explica por qué/i);
        fireEvent.change(textarea, { target: { value: 'A'.repeat(50) } });
        fireEvent.click(screen.getByRole('button', { name: /Enviar Postulación/i }));
        expect(await screen.findByText(/Error/i)).toBeInTheDocument();
    });

    test('error de red en POST de postulacion', async () => {
        fetch.mockImplementation(async (url) => {
            if (url.includes('/monitoring-request/open')) {
                return { ok: true, json: () => Promise.resolve(mockConvocatorias) };
            }
            if (url.includes('/monitor-application/monitor/')) {
                return { ok: true, json: () => Promise.resolve([]) };
            }
            if (url.includes('/monitor-application/apply')) {
                throw new Error('Network error');
            }
            return { ok: true, json: () => Promise.resolve([]) };
        });
        render(<VerConvocatorias />);
        await screen.findByText('Estructuras de Datos');
        fireEvent.click(screen.getAllByRole('button', { name: /Postularse/i })[0]);
        const textarea = screen.getByPlaceholderText(/Explica por qué/i);
        fireEvent.change(textarea, { target: { value: 'A'.repeat(50) } });
        fireEvent.click(screen.getByRole('button', { name: /Enviar Postulación/i }));
        expect(await screen.findByText(/Error al enviar postulación/i)).toBeInTheDocument();
    });
});
