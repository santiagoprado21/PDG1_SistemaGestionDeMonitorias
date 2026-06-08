import React from 'react';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import EvaluacionMonitoriaEstudiante from '../EvaluacionMonitoriaEstudiante';
import * as globalFix from '../globalFix';

jest.mock('../VerticalNavbar', () => () => <div data-testid="mock-navbar">Navbar</div>);

jest.mock('../PopUp', () => ({
    PopUp: ({ show, onClose, children }) => (show ? <div data-testid="mock-popup" onClick={onClose}>{children}</div> : null)
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
            if (String(url).includes('/monitor-survey/admin/report/csv')) {
                return Promise.resolve({
                    ok: true,
                    blob: async () => new Blob()
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
        window.URL.createObjectURL = jest.fn(() => 'blob:mock-url');
        window.URL.revokeObjectURL = jest.fn();
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

    test('URL parameters autofill fields and lock identifiers', () => {
        const originalLocation = window.location;
        delete window.location;
        window.location = new URL('http://localhost?monitoringId=999&monitorCode=MON-999&monitorName=TestName&semester=2026-1');

        setRole('student');
        render(<EvaluacionMonitoriaEstudiante />);

        expect(screen.getByPlaceholderText(/Ej: 1024/i).value).toBe('999');
        expect(screen.getByPlaceholderText(/Ej: M-045/i).value).toBe('MON-999');
        expect(screen.getByPlaceholderText(/Nombre completo/i).value).toBe('TestName');

        window.location = originalLocation;
    });

    test('student can change score by clicking a chip', () => {
        setRole('student');
        render(<EvaluacionMonitoriaEstudiante />);

        const radios = screen.getAllByRole('radio');
        fireEvent.click(radios[6]);
        expect(radios[6]).toHaveAttribute('aria-checked', 'true');
        expect(radios[3]).toHaveAttribute('aria-checked', 'false');
    });

    test('student can navigate scores with keyboard arrows and Home/End', () => {
        setRole('student');
        render(<EvaluacionMonitoriaEstudiante />);

        const radios = screen.getAllByRole('radio');
        const default4 = radios[3];

        fireEvent.keyDown(default4, { key: 'ArrowRight' });
        expect(radios[4]).toHaveAttribute('aria-checked', 'true');

        fireEvent.keyDown(radios[4], { key: 'ArrowLeft' });
        expect(radios[3]).toHaveAttribute('aria-checked', 'true');

        fireEvent.keyDown(radios[3], { key: 'Home' });
        expect(radios[0]).toHaveAttribute('aria-checked', 'true');

        fireEvent.keyDown(radios[0], { key: 'End' });
        expect(radios[6]).toHaveAttribute('aria-checked', 'true');
    });

    test('reset form clears scores and feedback', () => {
        setRole('student');
        render(<EvaluacionMonitoriaEstudiante />);

        const radios = screen.getAllByRole('radio');

        fireEvent.click(radios[6]);
        expect(radios[6]).toHaveAttribute('aria-checked', 'true');

        const textareas = screen.getAllByPlaceholderText(/Escribe tu comentario/i);
        fireEvent.change(textareas[0], { target: { value: 'Great' } });

        fireEvent.click(screen.getByRole('button', { name: /Limpiar/i }));

        expect(radios[3]).toHaveAttribute('aria-checked', 'true');
        expect(textareas[0].value).toBe('');
    });

    test('semester change updates value', () => {
        setRole('student');
        render(<EvaluacionMonitoriaEstudiante />);

        const select = screen.getByDisplayValue('2026-1');
        fireEvent.change(select, { target: { value: '2026-2' } });
        expect(select.value).toBe('2026-2');
    });

    test('monitor name and feedback inputs can be changed in student form', () => {
        setRole('student');
        render(<EvaluacionMonitoriaEstudiante />);

        const nameInput = screen.getByPlaceholderText(/Nombre completo/i);
        fireEvent.change(nameInput, { target: { value: 'Monitor Name' } });
        expect(nameInput.value).toBe('Monitor Name');

        const textareas = screen.getAllByPlaceholderText(/Escribe tu comentario/i);
        fireEvent.change(textareas[0], { target: { value: 'Positive feedback' } });
        fireEvent.change(textareas[1], { target: { value: 'Constructive feedback' } });
        expect(textareas[0].value).toBe('Positive feedback');
        expect(textareas[1].value).toBe('Constructive feedback');
    });

    test('close popup dismisses message', async () => {
        setRole('student');
        render(<EvaluacionMonitoriaEstudiante />);

        fireEvent.click(screen.getByRole('button', { name: /Enviar evaluacion/i }));
        expect(await screen.findByTestId('mock-popup')).toBeInTheDocument();

        fireEvent.click(screen.getByTestId('mock-popup'));
        expect(screen.queryByTestId('mock-popup')).not.toBeInTheDocument();
    });

    test('validate form requires monitor code', async () => {
        setRole('student');
        render(<EvaluacionMonitoriaEstudiante />);

        fireEvent.change(screen.getByPlaceholderText(/Ej: 1024/i), { target: { value: '1024' } });

        fireEvent.click(screen.getByRole('button', { name: /Enviar evaluacion/i }));

        expect(await screen.findByText(/Debes ingresar el codigo del monitor/i)).toBeInTheDocument();
    });

    test('survey config API failure uses fallback questions', async () => {
        global.fetch = jest.fn((url) => {
            if (String(url).includes('/monitor-survey/public/current-config')) {
                return Promise.resolve({
                    ok: false,
                    json: async () => ({ error: 'Config not found' })
                });
            }
            return Promise.resolve({ ok: true, json: async () => ({}) });
        });

        setRole('student');
        render(<EvaluacionMonitoriaEstudiante />);

        expect(await screen.findByText(/No se pudo cargar la configuración activa/i)).toBeInTheDocument();
    });

    test('survey config API with questions uses them', async () => {
        const customQuestions = [
            { id: 10, questionKey: 'custom_q', statement: 'Custom question?', category: 'Custom', displayOrder: 2 },
            { id: 11, questionKey: 'custom_q2', statement: 'Second custom?', category: 'Custom', displayOrder: 1 }
        ];
        global.fetch = jest.fn((url) => {
            if (String(url).includes('/monitor-survey/public/current-config')) {
                return Promise.resolve({
                    ok: true,
                    json: async () => ({ questions: customQuestions, semester: '2026-1' })
                });
            }
            return Promise.resolve({ ok: true, json: async () => ({}) });
        });

        setRole('student');
        render(<EvaluacionMonitoriaEstudiante />);

        expect(await screen.findByText(/Custom question/i)).toBeInTheDocument();
        expect(screen.getByText(/Second custom/i)).toBeInTheDocument();
    });

    test('handleCopyLink failure shows message', async () => {
        navigator.clipboard.writeText = jest.fn().mockRejectedValue(new Error('Clipboard denied'));

        setRole('monitor');
        render(<EvaluacionMonitoriaEstudiante />);

        fireEvent.click(screen.getByRole('button', { name: /Copiar/i }));

        expect(await screen.findByText(/No se pudo copiar el enlace/i)).toBeInTheDocument();
    });

    test('jfedpto can change report filters', () => {
        setRole('jfedpto');
        render(<EvaluacionMonitoriaEstudiante />);

        const periodSelect = screen.getAllByRole('combobox')[0];
        fireEvent.change(periodSelect, { target: { value: '' } });
        expect(periodSelect.value).toBe('');

        const monitoringIdInput = screen.getByPlaceholderText(/Ej: 1024/i);
        fireEvent.change(monitoringIdInput, { target: { value: '555' } });
        expect(monitoringIdInput.value).toBe('555');

        const monitorCodeInput = screen.getByPlaceholderText(/Ej: M-045/i);
        fireEvent.change(monitorCodeInput, { target: { value: 'MON-555' } });
        expect(monitorCodeInput.value).toBe('MON-555');
    });

    test('jfedpto report API error shows error message', async () => {
        global.fetch = jest.fn((url) => {
            if (String(url).includes('/monitor-survey/public/current-config')) {
                return Promise.resolve({ ok: true, json: async () => ({ questions: [] }) });
            }
            if (String(url).includes('/monitor-survey/admin/report')) {
                return Promise.resolve({ ok: false, json: async () => ({ error: 'Server error' }) });
            }
            return Promise.resolve({ ok: true, json: async () => ({}) });
        });

        setRole('jfedpto');
        render(<EvaluacionMonitoriaEstudiante />);

        expect(await screen.findByText(/Server error/i)).toBeInTheDocument();
    });

    test('jfedpto export CSV calls export endpoint', async () => {
        setRole('jfedpto');
        render(<EvaluacionMonitoriaEstudiante />);

        await screen.findByText(/El monitor demostro dominio/i);

        fireEvent.click(screen.getByRole('button', { name: /Exportar CSV/i }));

        await waitFor(() => {
            expect(fetch).toHaveBeenCalledWith(
                expect.stringContaining('/monitor-survey/admin/report/csv'),
                expect.any(Object)
            );
        });
    });

    test('jfedpto renders questionStats and responses in tables', async () => {
        setRole('jfedpto');
        render(<EvaluacionMonitoriaEstudiante />);

        expect(await screen.findByText(/El monitor demostro dominio de los temas tratados/i)).toBeInTheDocument();
        expect(screen.getByText('Apoyo Pedagogico')).toBeInTheDocument();
        expect(screen.getAllByText('5.20').length).toBeGreaterThanOrEqual(2);
        expect(screen.getByText('2024-2')).toBeInTheDocument();
        expect(screen.getByText('Juan Perez')).toBeInTheDocument();
        expect(screen.getByText(/22\/10\/2024/)).toBeInTheDocument();
    });

    test('handleSubmit failure shows error message', async () => {
        global.fetch = jest.fn((url) => {
            if (String(url).includes('/monitor-survey/public/current-config')) {
                return Promise.resolve({ ok: true, json: async () => ({ questions: [] }) });
            }
            if (String(url).includes('/monitor-survey/public/responses')) {
                return Promise.reject(new Error('Network error'));
            }
            return Promise.resolve({ ok: true, json: async () => ({}) });
        });

        setRole('student');
        render(<EvaluacionMonitoriaEstudiante />);

        fireEvent.change(screen.getByPlaceholderText(/Ej: 1024/i), { target: { value: '1024' } });
        fireEvent.change(screen.getByPlaceholderText(/Ej: M-045/i), { target: { value: 'MON-001' } });

        fireEvent.click(screen.getByRole('button', { name: /Enviar evaluacion/i }));

        expect(await screen.findByText(/Network error/i)).toBeInTheDocument();
    });

    test('monitor view fields can be changed', () => {
        setRole('monitor');
        render(<EvaluacionMonitoriaEstudiante />);

        const monitorCodeInput = screen.getByPlaceholderText(/Ej: 2220001/i);
        fireEvent.change(monitorCodeInput, { target: { value: 'MON-888' } });
        expect(monitorCodeInput.value).toBe('MON-888');

        const nameInput = screen.getAllByPlaceholderText(/Nombre completo/i)[0];
        fireEvent.change(nameInput, { target: { value: 'New Name' } });
        expect(nameInput.value).toBe('New Name');
    });

    test('validate form requires semester when empty', async () => {
        jest.spyOn(globalFix, 'getCurrentAcademicPeriod').mockReturnValue('');

        setRole('student');
        render(<EvaluacionMonitoriaEstudiante />);

        fireEvent.click(screen.getByRole('button', { name: /Enviar evaluacion/i }));

        expect(await screen.findByText(/Debes indicar el periodo de la evaluación/i)).toBeInTheDocument();
    });

    test('jfedpto without token shows login required messages', async () => {
        Object.defineProperty(window, 'localStorage', {
            value: {
                getItem: jest.fn((key) => {
                    if (key === 'role') return 'jfedpto';
                    if (key === 'userId') return 'MON-001';
                    return null;
                }),
                setItem: jest.fn(),
                clear: jest.fn()
            },
            writable: true
        });

        render(<EvaluacionMonitoriaEstudiante />);

        expect(await screen.findByText(/Debes iniciar sesión para consultar los resultados/i)).toBeInTheDocument();

        fireEvent.click(screen.getByRole('button', { name: /Exportar CSV/i }));

        expect(await screen.findByText(/Debes iniciar sesión para exportar el reporte/i)).toBeInTheDocument();
    });

    test('jfedpto export CSV API error shows error', async () => {
        setRole('jfedpto');

        global.fetch = jest.fn((url) => {
            if (String(url).includes('/monitor-survey/public/current-config')) {
                return Promise.resolve({ ok: true, json: async () => ({ questions: [] }) });
            }
            if (String(url).includes('/monitor-survey/admin/report/csv')) {
                return Promise.resolve({ ok: false, json: async () => ({ error: 'Export failed' }) });
            }
            if (String(url).includes('/monitor-survey/admin/report')) {
                return Promise.resolve({ ok: true, json: async () => mockReport });
            }
            return Promise.resolve({ ok: true, json: async () => ({}) });
        });

        window.URL.createObjectURL = jest.fn(() => 'blob:mock-url');
        window.URL.revokeObjectURL = jest.fn();

        render(<EvaluacionMonitoriaEstudiante />);

        await screen.findByText(/El monitor demostro dominio/i);

        fireEvent.click(screen.getByRole('button', { name: /Exportar CSV/i }));

        expect(await screen.findByText(/Export failed/i)).toBeInTheDocument();
    });
});