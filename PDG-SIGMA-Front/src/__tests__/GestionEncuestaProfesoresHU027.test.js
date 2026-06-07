import React from 'react';
import { render, screen, waitFor, fireEvent } from '@testing-library/react';
import { BrowserRouter } from 'react-router-dom';
import GestionEncuestaProfesoresHU027 from '../GestionEncuestaProfesoresHU027';

global.fetch = jest.fn();

jest.mock('../VerticalNavbar', () => () => <div data-testid="mock-navbar">Navbar</div>);
jest.mock('../LoadingSpinner', () => ({ message }) => <div data-testid="loading-spinner">{message}</div>);
jest.mock('../PopUp', () => ({
    PopUp: ({ show, onClose, children }) => {
        if (!show) return null;
        return (
            <div data-testid="mock-popup">
                <span data-testid="popup-message">{children}</span>
                <button onClick={onClose}>Cerrar</button>
            </div>
        );
    }
}));
jest.mock('../config/ApiBackend', () => ({
    BACKEND_URL: 'http://localhost:5435'
}));

const mockQuestions = [
    { id: 1, statement: 'El profesor explica claramente', category: 'Didáctica', bankActive: true, displayOrder: 1 },
    { id: 2, statement: 'El profesor es puntual', category: 'Puntualidad', bankActive: false, displayOrder: 2 }
];

const mockConfig = { semester: '2026-1', questions: [] };
const mockTemplates = [
    { id: 10, name: 'Evaluación Docente', description: 'Plantilla de evaluación', createdForSemester: '2026-1', questions: [] }
];

const mockFetch = () => {
    fetch.mockImplementation(async (url) => {
        if (url.includes('/professor-survey/admin/questions')) {
            return { ok: true, json: async () => mockQuestions };
        }
        if (url.includes('/professor-survey/admin/current-config')) {
            return { ok: true, json: async () => mockConfig };
        }
        if (url.includes('/professor-survey/admin/templates')) {
            return { ok: true, json: async () => mockTemplates };
        }
        return { ok: true, json: async () => [] };
    });
};

const renderComponent = () => render(
    <BrowserRouter><GestionEncuestaProfesoresHU027 /></BrowserRouter>
);

describe('GestionEncuestaProfesoresHU027', () => {
    beforeEach(() => {
        jest.clearAllMocks();
        mockFetch();
        Object.defineProperty(window, 'localStorage', {
            value: {
                getItem: jest.fn((key) => {
                    if (key === 'token') return 'Bearer test-token';
                    if (key === 'userId') return 'ADMIN-1';
                    return null;
                }),
                setItem: jest.fn(),
                clear: jest.fn()
            },
            writable: true
        });
    });

    test('renderiza el titulo principal', async () => {
        renderComponent();
        expect(screen.getByText(/Gestión de encuesta de evaluación de profesores/i)).toBeInTheDocument();
    });

    test('carga preguntas, configuracion y plantillas al montar', async () => {
        renderComponent();

        expect(await screen.findByText(/El profesor explica claramente/i)).toBeInTheDocument();
        expect(await screen.findByText(/Evaluación Docente/i)).toBeInTheDocument();
    });

    test('permite crear una nueva pregunta', async () => {
        renderComponent();

        const textarea = await screen.findByPlaceholderText(/Texto de la afirmación/i);
        const categoryInput = screen.getByPlaceholderText(/Categoría/i);

        fireEvent.change(textarea, { target: { value: 'Nueva pregunta para profesores' } });
        fireEvent.change(categoryInput, { target: { value: 'Evaluación' } });

        const submitBtn = screen.getByRole('button', { name: /Agregar pregunta/i });
        fireEvent.click(submitBtn);

        await waitFor(() => {
            expect(fetch).toHaveBeenCalledWith(
                expect.stringContaining('/professor-survey/admin/questions'),
                expect.objectContaining({
                    method: 'POST',
                    body: expect.stringContaining('Nueva pregunta para profesores')
                })
            );
        });
    });

    test('valida campos obligatorios al crear pregunta', async () => {
        renderComponent();

        const submitBtn = await screen.findByRole('button', { name: /Agregar pregunta/i });
        fireEvent.click(submitBtn);

        await waitFor(() => {
            expect(screen.getByTestId('popup-message')).toHaveTextContent(/Debes ingresar el texto/i);
        });
    });

    test('muestra preguntas seleccionadas por defecto', async () => {
        const { container } = renderComponent();

        await screen.findByText(/El profesor explica claramente/i);

        const summaryDiv = container.querySelector('.hu027-selected-summary');
        expect(summaryDiv.textContent).toContain('preguntas seleccionadas');
    });

    test('permite agregar pregunta a la configuracion', async () => {
        const { container } = renderComponent();
        await screen.findByText(/El profesor explica claramente/i);

        const addBtns = screen.getAllByRole('button', { name: /Agregar a configuración/i });
        fireEvent.click(addBtns[0]);

        await waitFor(() => {
            expect(screen.getByText(/Ya agregada/i)).toBeInTheDocument();
        });
        const summaryDiv = container.querySelector('.hu027-selected-summary');
        expect(summaryDiv.textContent).toContain('1 preguntas seleccionadas');
    });

    test('permite desactivar pregunta del banco', async () => {
        renderComponent();
        await screen.findByText(/El profesor explica claramente/i);

        const toggleBtn = screen.getByRole('button', { name: /Desactivar/i });
        fireEvent.click(toggleBtn);

        await waitFor(() => {
            expect(fetch).toHaveBeenCalledWith(
                expect.stringContaining('/professor-survey/admin/questions/1/status'),
                expect.objectContaining({ method: 'PATCH' })
            );
        });
    });

    test('permite cancelar edicion de pregunta', async () => {
        renderComponent();
        await screen.findByText(/El profesor explica claramente/i);

        const editBtns = screen.getAllByRole('button', { name: /Editar/i });
        fireEvent.click(editBtns[0]);

        expect(screen.getAllByText(/Guardar/)).toHaveLength(2);

        const cancelBtns = screen.getAllByText('Cancelar');
        fireEvent.click(cancelBtns[0]);

        await waitFor(() => {
            expect(screen.getAllByText(/Guardar/)).toHaveLength(1);
        });
    });

    test('valida nombre de plantilla no vacio', async () => {
        renderComponent();
        await screen.findByText(/El profesor explica claramente/i);

        const saveTemplateBtn = screen.getByRole('button', { name: /Guardar plantilla/i });
        fireEvent.click(saveTemplateBtn);

        await waitFor(() => {
            expect(screen.getByTestId('popup-message')).toHaveTextContent(/Debes ingresar el nombre de la plantilla/i);
        });
    });

    test('permite expandir preguntas de plantilla', async () => {
        renderComponent();
        await screen.findByText(/Evaluación Docente/i);

        const showBtn = screen.getByRole('button', { name: /Ver preguntas/i });
        fireEvent.click(showBtn);

        await waitFor(() => {
            expect(screen.getByRole('button', { name: /Ocultar preguntas/i })).toBeInTheDocument();
        });
    });

    test('no se rompe cuando falla la carga', async () => {
        fetch.mockRejectedValue(new Error('Error al cargar'));
        renderComponent();

        expect(await screen.findByText(/Gestión de encuesta de evaluación de profesores/i)).toBeInTheDocument();
        await waitFor(() => {
            expect(screen.getByTestId('popup-message')).toHaveTextContent(/Error al cargar/i);
        });
    });

    test('permite cambiar de periodo academico', async () => {
        renderComponent();

        await screen.findByText(/Banco de preguntas/i);

        const semesterSelect = await screen.findByLabelText(/Periodo activo/i);
        fireEvent.change(semesterSelect, { target: { value: '2026-2' } });
    });

    test('renderiza las secciones principales', async () => {
        renderComponent();

        expect(await screen.findByText(/Banco de preguntas/i)).toBeInTheDocument();
        expect(await screen.findByText(/Crear plantilla/i)).toBeInTheDocument();
        expect(await screen.findByText(/Plantillas creadas/i)).toBeInTheDocument();
    });

    test('permite editar y guardar pregunta', async () => {
        renderComponent();
        await screen.findByText(/El profesor explica claramente/i);

        const editBtns = screen.getAllByRole('button', { name: /Editar/i });
        fireEvent.click(editBtns[0]);

        await screen.findByText('Guardar');

        const saveBtns = screen.getAllByRole('button', { name: /Guardar/i });
        fireEvent.click(saveBtns[0]);

        await waitFor(() => {
            expect(fetch).toHaveBeenCalledWith(
                expect.stringContaining('/professor-survey/admin/questions/1'),
                expect.objectContaining({ method: 'PUT' })
            );
        });
    });

    test('permite editar plantilla y cancelar', async () => {
        renderComponent();
        await screen.findByText(/Evaluación Docente/i);

        const editBtns = screen.getAllByRole('button', { name: /Editar/i });
        fireEvent.click(editBtns[2]);

        await waitFor(() => {
            expect(screen.getByText(/Actualizar plantilla/i)).toBeInTheDocument();
        });

        const cancelBtn = screen.getByRole('button', { name: /Cancelar edición/i });
        fireEvent.click(cancelBtn);

        await waitFor(() => {
            expect(screen.getByText(/Guardar plantilla/i)).toBeInTheDocument();
        });
    });

    test('permite eliminar plantilla', async () => {
        window.confirm = jest.fn(() => true);

        renderComponent();
        await screen.findByText(/Evaluación Docente/i);

        const deleteBtn = screen.getByRole('button', { name: /Eliminar/i });
        fireEvent.click(deleteBtn);

        await waitFor(() => {
            expect(fetch).toHaveBeenCalledWith(
                expect.stringContaining('/professor-survey/admin/templates/10'),
                expect.objectContaining({ method: 'DELETE' })
            );
        });
    });

    test('permite aplicar plantilla al periodo activo', async () => {
        renderComponent();
        await screen.findByText(/Evaluación Docente/i);

        const applyBtn = screen.getByRole('button', { name: /Aplicar al periodo/i });
        fireEvent.click(applyBtn);

        await waitFor(() => {
            expect(fetch).toHaveBeenCalledWith(
                expect.stringContaining('/professor-survey/admin/apply-template'),
                expect.objectContaining({ method: 'POST' })
            );
        });
    });

    test('crea plantilla con nombre y preguntas seleccionadas', async () => {
        renderComponent();
        await screen.findByText(/El profesor explica claramente/i);

        const addBtns = screen.getAllByRole('button', { name: /Agregar a configuración/i });
        fireEvent.click(addBtns[0]);
        await screen.findByText(/Ya agregada/i);

        const nameInput = screen.getByPlaceholderText(/Nombre de la plantilla/i);
        fireEvent.change(nameInput, { target: { value: 'Mi plantilla' } });

        const saveBtn = screen.getByRole('button', { name: /Guardar plantilla/i });
        fireEvent.click(saveBtn);

        await waitFor(() => {
            expect(fetch).toHaveBeenCalledWith(
                expect.stringContaining('/professor-survey/admin/templates'),
                expect.objectContaining({ method: 'POST' })
            );
        });
    });
});
