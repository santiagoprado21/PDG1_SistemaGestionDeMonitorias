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
                return null;
            }),
            setItem: jest.fn(),
            clear: jest.fn()
        },
        writable: true
    });
};

describe('EvaluacionMonitoriaEstudiante simple coverage', () => {
    beforeEach(() => {
        jest.clearAllMocks();
        global.fetch = jest.fn().mockResolvedValue({ ok: true });
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
                expect.stringContaining('script.google.com/macros'),
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

    test('rol jfedpto muestra panel de resultados con iframe', () => {
        setRole('jfedpto');

        render(<EvaluacionMonitoriaEstudiante />);

        expect(screen.getByText(/Resultados de encuestas/i)).toBeInTheDocument();
        expect(screen.getByTitle(/Resultados de monitoria/i)).toBeInTheDocument();
    });
});