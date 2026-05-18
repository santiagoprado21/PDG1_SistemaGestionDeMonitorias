import React from 'react';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import EvaluacionMonitoriaEstudiante from '../EvaluacionMonitoriaEstudiante';

jest.mock('../VerticalNavbar', () => () => <div data-testid="mock-navbar">Navbar</div>);

jest.mock('../PopUp', () => ({
    PopUp: ({ show, children }) => (show ? <div data-testid="mock-popup">{children}</div> : null)
}));

jest.mock('lucide-react', () => {
    const Icon = () => <span data-testid="mock-icon" />;
    return {
        Link2: Icon,
        Copy: Icon,
        Check: Icon,
        ClipboardList: Icon,
        ShieldCheck: Icon,
        Share2: Icon
    };
});

const setRole = (role) => {
    Object.defineProperty(window, 'localStorage', {
        value: {
            getItem: jest.fn((key) => {
                if (key === 'role') return role;
                if (key === 'userId') return 'MON-001';
                if (key === 'token') return 'mock-token';
                return null;
            }),
            setItem: jest.fn(),
            clear: jest.fn()
        },
        writable: true
    });
};

const mockReport = {
    totalResponses: 1,
    averageScore: 5.2,
    totalAnswers: 2,
    questionStats: [
        {
            questionId: 1,
            statement: 'El monitor demostro dominio de los temas tratados.',
            category: 'Apoyo Pedagogico',
            averageScore: 5.2,
            responsesCount: 1,
            minScore: 5,
            maxScore: 6
        }
    ],
    responses: [
        {
            responseId: 10,
            semester: '2024-2',
            monitoringId: '1024',
            monitorCode: 'MON-001',
            monitorName: 'Juan Perez',
            averageScore: 5.2,
            createdAt: '2024-10-22T10:30:00'
        }
    ]
};

describe('EvaluacionMonitoriaEstudianteSimple', () => {
    beforeEach(() => {
        jest.clearAllMocks();
        global.fetch = jest.fn((url) => {
            if (String(url).includes('/monitor-survey/public/current-config')) {
                return Promise.resolve({
                    ok: true,
                    json: async () => ({ questions: [] })
                });
            }
            if (String(url).includes('/monitor-survey/admin/report')) {
                return Promise.resolve({
                    ok: true,
                    json: async () => mockReport
                });
            }
            return Promise.resolve({ ok: true, json: async () => ({}) });
        });
        Object.defineProperty(navigator, 'clipboard', {
            value: { writeText: jest.fn().mockResolvedValue(undefined) },
            writable: true
        });
    });

    test('rol student renderiza formulario y valida monitoria requerida', async () => {
        setRole('student');

        render(<EvaluacionMonitoriaEstudiante />);

        expect(screen.getByText(/Encuesta de experiencia con monitores/i)).toBeInTheDocument();

        const monitorInput = screen.getByPlaceholderText(/Ej: M-045/i);
        fireEvent.change(monitorInput, { target: { value: 'MON-001' } });

        fireEvent.click(screen.getByRole('button', { name: /Enviar evaluacion/i }));

        expect(await screen.findByText(/Debes ingresar el codigo de la monitoria/i)).toBeInTheDocument();
    });

    test('rol student permite enviar evaluacion con datos mínimos', async () => {
        setRole('student');

        render(<EvaluacionMonitoriaEstudiante />);

        fireEvent.change(screen.getByPlaceholderText(/Ej: 1024/i), { target: { value: '1024' } });
        fireEvent.change(screen.getByPlaceholderText(/Ej: M-045/i), { target: { value: 'MON-001' } });

        fireEvent.click(screen.getByRole('button', { name: /Enviar evaluacion/i }));

        await waitFor(() => {
            expect(fetch).toHaveBeenCalledWith(
                expect.stringContaining('/monitor-survey/public/responses'),
                expect.objectContaining({ method: 'POST' })
            );
        });

        expect(await screen.findByText(/Tu evaluacion fue registrada/i)).toBeInTheDocument();
    });

    test('rol monitor muestra panel de compartir y permite copiar enlace', async () => {
        setRole('monitor');

        render(<EvaluacionMonitoriaEstudiante />);

        expect(screen.getByText(/Generar enlace de evaluacion/i)).toBeInTheDocument();

        fireEvent.change(screen.getByPlaceholderText(/Ej: 1024/i), { target: { value: '999' } });
        fireEvent.click(screen.getByRole('button', { name: /Copiar/i }));

        await waitFor(() => {
            expect(navigator.clipboard.writeText).toHaveBeenCalled();
        });
    });

    test('rol jfedpto muestra panel de resultados internos', async () => {
        setRole('jfedpto');

        render(<EvaluacionMonitoriaEstudiante />);

        expect(screen.getByText(/Resultados de encuestas/i)).toBeInTheDocument();
        expect(await screen.findByText(/Resultados por pregunta/i)).toBeInTheDocument();
        expect(screen.getByText(/Exportar CSV/i)).toBeInTheDocument();
    });
});