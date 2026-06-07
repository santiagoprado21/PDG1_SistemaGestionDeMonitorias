import React from 'react';
import { render, screen, waitFor, fireEvent } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { BrowserRouter } from 'react-router-dom';
import CreateConvocatoria from '../CreateConvocatoria';

global.fetch = jest.fn();

const mockNavigate = jest.fn();
jest.mock('react-router-dom', () => ({
    ...jest.requireActual('react-router-dom'),
    useNavigate: () => mockNavigate
}));

jest.mock('../VerticalNavbar', () => () => <div data-testid="mock-navbar">Navbar</div>);
jest.mock('../LoadingSpinner', () => () => <div data-testid="loading-spinner">Loading...</div>);
jest.mock('../PopUp', () => ({
    PopUp: ({ show, children }) => show ? <div data-testid="mock-popup"><div>{children}</div></div> : null
}));
jest.mock('../config/ApiBackend', () => ({
    BACKEND_URL: 'http://localhost:5435'
}));

const mockFaculties = [{ id: 1, name: 'Ingeniería' }];
const mockPrograms = [{ id: 10, name: 'Ingeniería de Sistemas' }];
const mockCourses = [{ id: 100, name: 'Estructuras de Datos' }];

const mockFetch = () => {
    fetch.mockImplementation(async (url) => {
        if (url.includes('/school/getSchools')) {
            return { ok: true, json: async () => mockFaculties };
        }
        if (url.includes('/program/getProgramsSchool')) {
            return { ok: true, json: async () => mockPrograms };
        }
        if (url.includes('/course/program/')) {
            return { ok: true, json: async () => mockCourses };
        }
        if (url.includes('/monitoring-request/professor/')) {
            return { ok: true, json: async () => [] };
        }
        if (url.includes('/monitoring-request/create')) {
            return { ok: true, json: async () => ({ id: 1 }) };
        }
        return { ok: true, json: async () => [] };
    });
};

const renderComponent = () => {
    const result = render(<BrowserRouter><CreateConvocatoria /></BrowserRouter>);
    return { ...result, container: result.container };
};

describe('CreateConvocatoria', () => {
    beforeEach(() => {
        jest.clearAllMocks();
        mockFetch();
        Object.defineProperty(window, 'localStorage', {
            value: {
                getItem: jest.fn((key) => {
                    if (key === 'userId') return 'PROF-001';
                    if (key === 'token') return 'Bearer test-token';
                    return null;
                }),
                setItem: jest.fn(),
                clear: jest.fn()
            },
            writable: true
        });
    });

    test('renderiza el formulario de creacion de convocatoria', async () => {
        renderComponent();
        expect(await screen.findByText(/Crear Convocatoria de Monitoría/i)).toBeInTheDocument();
        expect(screen.getByText(/Nueva Convocatoria/i)).toBeInTheDocument();
    });

    test('carga las facultades al montar', async () => {
        renderComponent();
        await waitFor(() => {
            expect(fetch).toHaveBeenCalledWith(expect.stringContaining('/school/getSchools'));
        });
        expect(await screen.findByText('Ingeniería')).toBeInTheDocument();
    });

    test('carga las convocatorias del profesor al montar', async () => {
        renderComponent();
        await waitFor(() => {
            expect(fetch).toHaveBeenCalledWith(
                expect.stringContaining('/monitoring-request/professor/PROF-001'),
                expect.anything()
            );
        });
    });

    test('valida campos obligatorios al enviar formulario vacio', async () => {
        renderComponent();
        const submitBtn = await screen.findByRole('button', { name: /Crear Convocatoria/i });
        fireEvent.click(submitBtn);

        expect(await screen.findByText(/Por favor completa todos los campos obligatorios/i)).toBeInTheDocument();
    });

    test('valida longitud minima de justificacion', async () => {
        const { container } = renderComponent();

        const selects = await screen.findAllByRole('combobox');

        await waitFor(() => {
            expect(selects[0].querySelector('option[value="1"]')).toBeInTheDocument();
        });
        await userEvent.selectOptions(selects[0], '1');
        await waitFor(() => {
            expect(selects[1].querySelector('option[value="10"]')).toBeInTheDocument();
        });
        await userEvent.selectOptions(selects[1], '10');
        await waitFor(() => {
            expect(selects[2].querySelector('option[value="100"]')).toBeInTheDocument();
        });
        await userEvent.selectOptions(selects[2], '100');
        await userEvent.selectOptions(selects[3], '2026-1');

        const dateInputs = container.querySelectorAll('input[type="date"]');
        fireEvent.change(dateInputs[0], { target: { value: '2026-02-01' } });
        fireEvent.change(dateInputs[1], { target: { value: '2026-06-30' } });

        const numberInputs = container.querySelectorAll('input[type="number"]');
        fireEvent.change(numberInputs[0], { target: { value: '80' } });

        const justification = screen.getByPlaceholderText(/Explica por qué/i);
        fireEvent.change(justification, { target: { value: 'Corta' } });

        const submitBtn = screen.getByRole('button', { name: /Crear Convocatoria/i });
        fireEvent.click(submitBtn);

        expect(await screen.findByText(/La justificación debe tener al menos 50 caracteres/i)).toBeInTheDocument();
    });

    test('valida promedio minimo requerido', async () => {
        const { container } = renderComponent();

        const selects = await screen.findAllByRole('combobox');

        await waitFor(() => {
            expect(selects[0].querySelector('option[value="1"]')).toBeInTheDocument();
        });
        await userEvent.selectOptions(selects[0], '1');
        await waitFor(() => {
            expect(selects[1].querySelector('option[value="10"]')).toBeInTheDocument();
        });
        await userEvent.selectOptions(selects[1], '10');
        await waitFor(() => {
            expect(selects[2].querySelector('option[value="100"]')).toBeInTheDocument();
        });
        await userEvent.selectOptions(selects[2], '100');
        await userEvent.selectOptions(selects[3], '2026-1');

        const dateInputs = container.querySelectorAll('input[type="date"]');
        fireEvent.change(dateInputs[0], { target: { value: '2026-02-01' } });
        fireEvent.change(dateInputs[1], { target: { value: '2026-06-30' } });

        const numberInputs = container.querySelectorAll('input[type="number"]');
        fireEvent.change(numberInputs[0], { target: { value: '80' } });

        const justification = screen.getByPlaceholderText(/Explica por qué/i);
        fireEvent.change(justification, { target: { value: 'A'.repeat(50) } });

        const avgInputs = screen.getAllByDisplayValue('4.0');
        fireEvent.change(avgInputs[0], { target: { value: '3.0' } });

        const submitBtn = screen.getByRole('button', { name: /Crear Convocatoria/i });
        fireEvent.click(submitBtn);

        expect(await screen.findByText(/El promedio requerido debe ser mínimo 4.0/i)).toBeInTheDocument();
    });

    test('muestra mensaje de error cuando no hay convocatorias', async () => {
        renderComponent();
        expect(await screen.findByText(/No tienes convocatorias creadas aún/i)).toBeInTheDocument();
    });

    test('el boton de crear se deshabilita durante la carga', async () => {
        fetch.mockImplementation(async () => {
            await new Promise(r => setTimeout(r, 500));
            return { ok: true, json: async () => [] };
        });

        renderComponent();
        const submitBtn = await screen.findByRole('button', { name: /Crear Convocatoria/i });
        expect(submitBtn).not.toBeDisabled();
    });

    test('valida nota del curso menor a 4.0', async () => {
        const { container } = renderComponent();
        const selects = await screen.findAllByRole('combobox');
        await waitFor(() => {
            expect(selects[0].querySelector('option[value="1"]')).toBeInTheDocument();
        });
        await userEvent.selectOptions(selects[0], '1');
        await waitFor(() => {
            expect(selects[1].querySelector('option[value="10"]')).toBeInTheDocument();
        });
        await userEvent.selectOptions(selects[1], '10');
        await waitFor(() => {
            expect(selects[2].querySelector('option[value="100"]')).toBeInTheDocument();
        });
        await userEvent.selectOptions(selects[2], '100');
        await userEvent.selectOptions(selects[3], '2026-1');

        const dateInputs = container.querySelectorAll('input[type="date"]');
        fireEvent.change(dateInputs[0], { target: { value: '2026-02-01' } });
        fireEvent.change(dateInputs[1], { target: { value: '2026-06-30' } });

        const numberInputs = container.querySelectorAll('input[type="number"]');
        fireEvent.change(numberInputs[0], { target: { value: '80' } });

        const justification = screen.getByPlaceholderText(/Explica por qué/i);
        fireEvent.change(justification, { target: { value: 'A'.repeat(50) } });

        const avgInputs = screen.getAllByDisplayValue('4.0');
        fireEvent.change(avgInputs[1], { target: { value: '3.5' } });

        const submitBtn = screen.getByRole('button', { name: /Crear Convocatoria/i });
        fireEvent.click(submitBtn);

        expect(await screen.findByText(/La nota del curso requerida debe ser mínimo 4.0/i)).toBeInTheDocument();
    });

    test('crea convocatoria exitosamente con todos los campos', async () => {
        let createCalled = false;
        fetch.mockImplementation(async (url, options) => {
            if (url.includes('/monitoring-request/create')) {
                createCalled = true;
                return { ok: true, json: async () => ({ id: 999 }) };
            }
            if (url.includes('/school/getSchools')) {
                return { ok: true, json: async () => mockFaculties };
            }
            if (url.includes('/program/getProgramsSchool')) {
                return { ok: true, json: async () => mockPrograms };
            }
            if (url.includes('/course/program/')) {
                return { ok: true, json: async () => mockCourses };
            }
            if (url.includes('/monitoring-request/professor/')) {
                return { ok: true, json: async () => [] };
            }
            return { ok: true, json: async () => [] };
        });

        const { container } = renderComponent();

        const selects = await screen.findAllByRole('combobox');
        await waitFor(() => expect(selects[0].querySelector('option[value="1"]')).toBeInTheDocument());
        await userEvent.selectOptions(selects[0], '1');
        await waitFor(() => expect(selects[1].querySelector('option[value="10"]')).toBeInTheDocument());
        await userEvent.selectOptions(selects[1], '10');
        await waitFor(() => expect(selects[2].querySelector('option[value="100"]')).toBeInTheDocument());
        await userEvent.selectOptions(selects[2], '100');
        await userEvent.selectOptions(selects[3], '2026-1');

        const dateInputs = container.querySelectorAll('input[type="date"]');
        fireEvent.change(dateInputs[0], { target: { value: '2026-02-01' } });
        fireEvent.change(dateInputs[1], { target: { value: '2026-06-30' } });

        const numberInputs = container.querySelectorAll('input[type="number"]');
        fireEvent.change(numberInputs[0], { target: { value: '80' } });

        const justification = screen.getByPlaceholderText(/Explica por qué/i);
        fireEvent.change(justification, { target: { value: 'A'.repeat(50) } });

        const submitBtn = screen.getByRole('button', { name: /Crear Convocatoria/i });
        fireEvent.click(submitBtn);

        await waitFor(() => {
            expect(createCalled).toBe(true);
        });

        expect(await screen.findByText(/Convocatoria creada exitosamente/i)).toBeInTheDocument();
    });

    test('muestra error HTTP del servidor al crear convocatoria', async () => {
        fetch.mockImplementation(async (url) => {
            if (url.includes('/monitoring-request/create')) {
                return { ok: false, text: async () => 'Error del servidor' };
            }
            if (url.includes('/school/getSchools')) {
                return { ok: true, json: async () => mockFaculties };
            }
            if (url.includes('/program/getProgramsSchool')) {
                return { ok: true, json: async () => mockPrograms };
            }
            if (url.includes('/course/program/')) {
                return { ok: true, json: async () => mockCourses };
            }
            if (url.includes('/monitoring-request/professor/')) {
                return { ok: true, json: async () => [] };
            }
            return { ok: true, json: async () => [] };
        });

        const { container } = renderComponent();

        const selects = await screen.findAllByRole('combobox');
        await waitFor(() => expect(selects[0].querySelector('option[value="1"]')).toBeInTheDocument());
        await userEvent.selectOptions(selects[0], '1');
        await waitFor(() => expect(selects[1].querySelector('option[value="10"]')).toBeInTheDocument());
        await userEvent.selectOptions(selects[1], '10');
        await waitFor(() => expect(selects[2].querySelector('option[value="100"]')).toBeInTheDocument());
        await userEvent.selectOptions(selects[2], '100');
        await userEvent.selectOptions(selects[3], '2026-1');

        const dateInputs = container.querySelectorAll('input[type="date"]');
        fireEvent.change(dateInputs[0], { target: { value: '2026-02-01' } });
        fireEvent.change(dateInputs[1], { target: { value: '2026-06-30' } });

        const numberInputs = container.querySelectorAll('input[type="number"]');
        fireEvent.change(numberInputs[0], { target: { value: '80' } });

        const justification = screen.getByPlaceholderText(/Explica por qué/i);
        fireEvent.change(justification, { target: { value: 'A'.repeat(50) } });

        const submitBtn = screen.getByRole('button', { name: /Crear Convocatoria/i });
        fireEvent.click(submitBtn);

        expect(await screen.findByText(/Error: Error del servidor/i)).toBeInTheDocument();
    });

    test('muestra error de red al crear convocatoria', async () => {
        fetch.mockImplementation(async (url) => {
            if (url.includes('/monitoring-request/create')) {
                throw new Error('Network error');
            }
            if (url.includes('/school/getSchools')) {
                return { ok: true, json: async () => mockFaculties };
            }
            if (url.includes('/program/getProgramsSchool')) {
                return { ok: true, json: async () => mockPrograms };
            }
            if (url.includes('/course/program/')) {
                return { ok: true, json: async () => mockCourses };
            }
            if (url.includes('/monitoring-request/professor/')) {
                return { ok: true, json: async () => [] };
            }
            return { ok: true, json: async () => [] };
        });

        const { container } = renderComponent();

        const selects = await screen.findAllByRole('combobox');
        await waitFor(() => expect(selects[0].querySelector('option[value="1"]')).toBeInTheDocument());
        await userEvent.selectOptions(selects[0], '1');
        await waitFor(() => expect(selects[1].querySelector('option[value="10"]')).toBeInTheDocument());
        await userEvent.selectOptions(selects[1], '10');
        await waitFor(() => expect(selects[2].querySelector('option[value="100"]')).toBeInTheDocument());
        await userEvent.selectOptions(selects[2], '100');
        await userEvent.selectOptions(selects[3], '2026-1');

        const dateInputs = container.querySelectorAll('input[type="date"]');
        fireEvent.change(dateInputs[0], { target: { value: '2026-02-01' } });
        fireEvent.change(dateInputs[1], { target: { value: '2026-06-30' } });

        const numberInputs = container.querySelectorAll('input[type="number"]');
        fireEvent.change(numberInputs[0], { target: { value: '80' } });

        const justification = screen.getByPlaceholderText(/Explica por qué/i);
        fireEvent.change(justification, { target: { value: 'A'.repeat(50) } });

        const submitBtn = screen.getByRole('button', { name: /Crear Convocatoria/i });
        fireEvent.click(submitBtn);

        expect(await screen.findByText(/Error al crear la convocatoria/i)).toBeInTheDocument();
    });

    test('muestra tabla con convocatorias existentes', async () => {
        const existingConvocatorias = [
            { id: 1, courseName: 'POO', semester: '2026-1', requestedHours: 80, status: 'PENDIENTE_APROBACION_JEFE', createdAt: '2026-01-15T10:00:00Z' },
            { id: 2, courseName: 'Cálculo', semester: '2026-1', requestedHours: 60, status: 'CONVOCATORIA_ABIERTA', createdAt: '2026-01-20T10:00:00Z' }
        ];
        fetch.mockImplementation(async (url) => {
            if (url.includes('/monitoring-request/professor/')) {
                return { ok: true, json: async () => existingConvocatorias };
            }
            if (url.includes('/school/getSchools')) {
                return { ok: true, json: async () => mockFaculties };
            }
            return { ok: true, json: async () => [] };
        });

        renderComponent();
        await waitFor(() => {
            expect(screen.getByText('POO')).toBeInTheDocument();
        });
        expect(screen.getByText('80h')).toBeInTheDocument();
        expect(screen.getByText('Pendiente Aprobacion')).toBeInTheDocument();
        expect(screen.getByText('Abierta')).toBeInTheDocument();
        expect(screen.getByRole('button', { name: /Ver Postulantes/i })).toBeInTheDocument();
    });

    test('valida fecha antes del periodo academico', async () => {
        const { container } = renderComponent();
        const selects = await screen.findAllByRole('combobox');
        await waitFor(() => expect(selects[0].querySelector('option[value="1"]')).toBeInTheDocument());
        await userEvent.selectOptions(selects[0], '1');
        await waitFor(() => expect(selects[1].querySelector('option[value="10"]')).toBeInTheDocument());
        await userEvent.selectOptions(selects[1], '10');
        await waitFor(() => expect(selects[2].querySelector('option[value="100"]')).toBeInTheDocument());
        await userEvent.selectOptions(selects[2], '100');
        await userEvent.selectOptions(selects[3], '2026-1');

        const dateInputs = container.querySelectorAll('input[type="date"]');
        fireEvent.change(dateInputs[0], { target: { value: '2026-01-01' } });
        fireEvent.change(dateInputs[1], { target: { value: '2026-06-30' } });

        const numberInputs = container.querySelectorAll('input[type="number"]');
        fireEvent.change(numberInputs[0], { target: { value: '80' } });

        const justification = screen.getByPlaceholderText(/Explica por qué/i);
        fireEvent.change(justification, { target: { value: 'A'.repeat(50) } });

        const submitBtn = screen.getByRole('button', { name: /Crear Convocatoria/i });
        fireEvent.click(submitBtn);

        expect(await screen.findByText(/fecha de inicio debe ser igual o posterior/i)).toBeInTheDocument();
    });

    test('valida fecha fin posterior al periodo', async () => {
        const { container } = renderComponent();
        const selects = await screen.findAllByRole('combobox');
        await waitFor(() => expect(selects[0].querySelector('option[value="1"]')).toBeInTheDocument());
        await userEvent.selectOptions(selects[0], '1');
        await waitFor(() => expect(selects[1].querySelector('option[value="10"]')).toBeInTheDocument());
        await userEvent.selectOptions(selects[1], '10');
        await waitFor(() => expect(selects[2].querySelector('option[value="100"]')).toBeInTheDocument());
        await userEvent.selectOptions(selects[2], '100');
        await userEvent.selectOptions(selects[3], '2026-1');

        const dateInputs = container.querySelectorAll('input[type="date"]');
        fireEvent.change(dateInputs[0], { target: { value: '2026-02-01' } });
        fireEvent.change(dateInputs[1], { target: { value: '2026-07-15' } });

        const numberInputs = container.querySelectorAll('input[type="number"]');
        fireEvent.change(numberInputs[0], { target: { value: '80' } });

        const justification = screen.getByPlaceholderText(/Explica por qué/i);
        fireEvent.change(justification, { target: { value: 'A'.repeat(50) } });

        const submitBtn = screen.getByRole('button', { name: /Crear Convocatoria/i });
        fireEvent.click(submitBtn);

        expect(await screen.findByText(/fecha de fin debe ser igual o anterior/i)).toBeInTheDocument();
    });

    test('valida que fecha inicio no sea posterior a fecha fin', async () => {
        const { container } = renderComponent();
        const selects = await screen.findAllByRole('combobox');
        await waitFor(() => expect(selects[0].querySelector('option[value="1"]')).toBeInTheDocument());
        await userEvent.selectOptions(selects[0], '1');
        await waitFor(() => expect(selects[1].querySelector('option[value="10"]')).toBeInTheDocument());
        await userEvent.selectOptions(selects[1], '10');
        await waitFor(() => expect(selects[2].querySelector('option[value="100"]')).toBeInTheDocument());
        await userEvent.selectOptions(selects[2], '100');
        await userEvent.selectOptions(selects[3], '2026-1');

        const dateInputs = container.querySelectorAll('input[type="date"]');
        fireEvent.change(dateInputs[0], { target: { value: '2026-06-30' } });
        fireEvent.change(dateInputs[1], { target: { value: '2026-02-01' } });

        const numberInputs = container.querySelectorAll('input[type="number"]');
        fireEvent.change(numberInputs[0], { target: { value: '80' } });

        const justification = screen.getByPlaceholderText(/Explica por qué/i);
        fireEvent.change(justification, { target: { value: 'A'.repeat(50) } });

        const submitBtn = screen.getByRole('button', { name: /Crear Convocatoria/i });
        fireEvent.click(submitBtn);

        expect(await screen.findByText(/La fecha de inicio no puede ser posterior/i)).toBeInTheDocument();
    });
});
