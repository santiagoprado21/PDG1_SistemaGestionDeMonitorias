import React from 'react';
import { render, screen, waitFor, fireEvent } from '@testing-library/react';
import { BrowserRouter } from 'react-router-dom';
import GestionEncuestaMonitoresHU026 from '../GestionEncuestaMonitoresHU026';
import * as globalFix from '../globalFix';

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
    { id: 1, statement: 'El monitor cumple con el horario', category: 'Puntualidad', bankActive: true, displayOrder: 1 },
    { id: 2, statement: 'El monitor domina los temas', category: 'Conocimiento', bankActive: false, displayOrder: 2 }
];

const mockConfig = { semester: '2026-1', questions: [] };
const mockTemplates = [
    { id: 10, name: 'Plantilla Base', description: 'Descripción base', createdForSemester: '2026-1', questions: [] }
];

const mockFetch = () => {
    fetch.mockImplementation(async (url) => {
        if (url.includes('/monitor-survey/admin/questions')) {
            return { ok: true, json: async () => mockQuestions };
        }
        if (url.includes('/monitor-survey/admin/current-config')) {
            return { ok: true, json: async () => mockConfig };
        }
        if (url.includes('/monitor-survey/admin/templates')) {
            return { ok: true, json: async () => mockTemplates };
        }
        return { ok: true, json: async () => [] };
    });
};

const renderComponent = () => render(
    <BrowserRouter><GestionEncuestaMonitoresHU026 /></BrowserRouter>
);

describe('GestionEncuestaMonitoresHU026', () => {
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

    test('renderiza el titulo principal y muestra loading inicial', async () => {
        renderComponent();
        expect(screen.getByText(/Gestión de encuesta de evaluación de monitores/i)).toBeInTheDocument();
    });

    test('carga preguntas, configuracion y plantillas al montar', async () => {
        renderComponent();

        expect(await screen.findByText(/El monitor cumple con el horario/i)).toBeInTheDocument();
        expect(await screen.findByText(/Plantilla Base/i)).toBeInTheDocument();
    });

    test('permite crear una nueva pregunta', async () => {
        renderComponent();

        const textarea = await screen.findByPlaceholderText(/Texto de la afirmación/i);
        const categoryInput = screen.getByPlaceholderText(/Categoría/i);

        fireEvent.change(textarea, { target: { value: 'Nueva pregunta de prueba' } });
        fireEvent.change(categoryInput, { target: { value: 'Nueva categoría' } });

        const submitBtn = screen.getByRole('button', { name: /Agregar pregunta/i });
        fireEvent.click(submitBtn);

        await waitFor(() => {
            expect(fetch).toHaveBeenCalledWith(
                expect.stringContaining('/monitor-survey/admin/questions'),
                expect.objectContaining({
                    method: 'POST',
                    body: expect.stringContaining('Nueva pregunta de prueba')
                })
            );
        });
    });

    test('valida que los campos de pregunta no esten vacios', async () => {
        renderComponent();

        const submitBtn = await screen.findByRole('button', { name: /Agregar pregunta/i });
        fireEvent.click(submitBtn);

        await waitFor(() => {
            expect(screen.getByTestId('popup-message')).toHaveTextContent(/Debes ingresar el texto/i);
        });
    });

    test('muestra preguntas seleccionadas por defecto', async () => {
        const { container } = renderComponent();

        await screen.findByText(/El monitor cumple con el horario/i);

        const summaryDiv = container.querySelector('.hu026-selected-summary');
        expect(summaryDiv.textContent).toContain('preguntas seleccionadas');
    });

    test('permite agregar pregunta a la configuracion', async () => {
        const { container } = renderComponent();
        await screen.findByText(/El monitor cumple con el horario/i);

        const addBtns = screen.getAllByRole('button', { name: /Agregar a configuración/i });
        fireEvent.click(addBtns[0]);

        await waitFor(() => {
            expect(screen.getByText(/Ya agregada/i)).toBeInTheDocument();
        });
        const summaryDiv = container.querySelector('.hu026-selected-summary');
        expect(summaryDiv.textContent).toContain('1 preguntas seleccionadas');
    });

    test('permite desactivar pregunta del banco', async () => {
        renderComponent();
        await screen.findByText(/El monitor cumple con el horario/i);

        const toggleBtn = screen.getByRole('button', { name: /Desactivar/i });
        fireEvent.click(toggleBtn);

        await waitFor(() => {
            expect(fetch).toHaveBeenCalledWith(
                expect.stringContaining('/monitor-survey/admin/questions/1/status'),
                expect.objectContaining({ method: 'PATCH' })
            );
        });
    });

    test('permite cancelar edicion de pregunta', async () => {
        renderComponent();
        await screen.findByText(/El monitor cumple con el horario/i);

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
        await screen.findByText(/El monitor cumple con el horario/i);

        const saveTemplateBtn = screen.getByRole('button', { name: /Guardar plantilla/i });
        fireEvent.click(saveTemplateBtn);

        await waitFor(() => {
            expect(screen.getByTestId('popup-message')).toHaveTextContent(/Debes ingresar el nombre de la plantilla/i);
        });
    });

    test('permite expandir preguntas de plantilla', async () => {
        renderComponent();
        await screen.findByText(/Plantilla Base/i);

        const showBtn = screen.getByRole('button', { name: /Ver preguntas/i });
        fireEvent.click(showBtn);

        await waitFor(() => {
            expect(screen.getByRole('button', { name: /Ocultar preguntas/i })).toBeInTheDocument();
        });
    });

    test('no se rompe cuando falla la carga', async () => {
        fetch.mockRejectedValue(new Error('Error de red'));
        renderComponent();

        expect(await screen.findByText(/Gestión de encuesta de evaluación de monitores/i)).toBeInTheDocument();
        await waitFor(() => {
            expect(screen.getByTestId('popup-message')).toHaveTextContent(/Error de red/i);
        });
    });

    test('permite filtrar plantillas por periodo', async () => {
        renderComponent();

        const filterSelect = await screen.findByLabelText(/Filtrar por periodo/i);
        expect(filterSelect).toBeInTheDocument();
    });

    test('muestra el boton de crear plantilla', async () => {
        renderComponent();

        expect(await screen.findByRole('button', { name: /Guardar plantilla/i })).toBeInTheDocument();
    });

    test('permite cambiar de periodo academico', async () => {
        renderComponent();

        await screen.findByText(/Banco de preguntas/i);

        const semesterSelect = await screen.findByLabelText(/Periodo activo/i);
        fireEvent.change(semesterSelect, { target: { value: '2026-2' } });
    });

    test('permite editar y guardar pregunta', async () => {
        renderComponent();
        await screen.findByText(/El monitor cumple con el horario/i);

        const editBtns = screen.getAllByRole('button', { name: /Editar/i });
        fireEvent.click(editBtns[0]);

        await screen.findByText('Guardar');

        const saveBtns = screen.getAllByRole('button', { name: /Guardar/i });
        fireEvent.click(saveBtns[0]);

        await waitFor(() => {
            expect(fetch).toHaveBeenCalledWith(
                expect.stringContaining('/monitor-survey/admin/questions/1'),
                expect.objectContaining({ method: 'PUT' })
            );
        });
    });

    test('permite editar plantilla y cancelar', async () => {
        renderComponent();
        await screen.findByText(/Plantilla Base/i);

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
        await screen.findByText(/Plantilla Base/i);

        const deleteBtn = screen.getByRole('button', { name: /Eliminar/i });
        fireEvent.click(deleteBtn);

        await waitFor(() => {
            expect(fetch).toHaveBeenCalledWith(
                expect.stringContaining('/monitor-survey/admin/templates/10'),
                expect.objectContaining({ method: 'DELETE' })
            );
        });
    });

    test('permite aplicar plantilla al periodo activo', async () => {
        renderComponent();
        await screen.findByText(/Plantilla Base/i);

        const applyBtn = screen.getByRole('button', { name: /Aplicar al periodo/i });
        fireEvent.click(applyBtn);

        await waitFor(() => {
            expect(fetch).toHaveBeenCalledWith(
                expect.stringContaining('/monitor-survey/admin/apply-template'),
                expect.objectContaining({ method: 'POST' })
            );
        });
    });

    test('crea plantilla con nombre y preguntas seleccionadas', async () => {
        renderComponent();
        await screen.findByText(/El monitor cumple con el horario/i);

        const addBtns = screen.getAllByRole('button', { name: /Agregar a configuración/i });
        fireEvent.click(addBtns[0]);
        await screen.findByText(/Ya agregada/i);

        const nameInput = screen.getByPlaceholderText(/Nombre de la plantilla/i);
        fireEvent.change(nameInput, { target: { value: 'Mi plantilla' } });

        const saveBtn = screen.getByRole('button', { name: /Guardar plantilla/i });
        fireEvent.click(saveBtn);

        await waitFor(() => {
            expect(fetch).toHaveBeenCalledWith(
                expect.stringContaining('/monitor-survey/admin/templates'),
                expect.objectContaining({ method: 'POST' })
            );
        });
    });

    test('maneja error de parseo JSON en config y usa periodo actual como fallback', async () => {
        fetch.mockImplementation(async (url) => {
            if (url.includes('/monitor-survey/admin/questions')) return { ok: true, json: async () => mockQuestions };
            if (url.includes('/monitor-survey/admin/current-config')) return { ok: true, json: async () => { throw new Error('Parse error'); } };
            if (url.includes('/monitor-survey/admin/templates')) return { ok: true, json: async () => mockTemplates };
            return { ok: true, json: async () => [] };
        });
        renderComponent();
        await waitFor(() => {
            expect(screen.getByText(/El monitor cumple con el horario/i)).toBeInTheDocument();
        });
    });

    test('filtra plantillas por periodo', async () => {
        const templatesMultiPeriod = [
            { id: 10, name: 'Periodo 1', description: '', createdForSemester: '2026-1', questions: [] },
            { id: 11, name: 'Periodo 2', description: '', createdForSemester: '2026-2', questions: [] }
        ];
        fetch.mockImplementation(async (url) => {
            if (url.includes('/monitor-survey/admin/questions')) return { ok: true, json: async () => mockQuestions };
            if (url.includes('/monitor-survey/admin/current-config')) return { ok: true, json: async () => mockConfig };
            if (url.includes('/monitor-survey/admin/templates')) return { ok: true, json: async () => templatesMultiPeriod };
            return { ok: true, json: async () => [] };
        });
        renderComponent();
        await screen.findByText(/Periodo 2/i);
        const filterSelect = screen.getByLabelText(/Filtrar por periodo/i);
        fireEvent.change(filterSelect, { target: { value: '2026-2' } });
        await waitFor(() => {
            expect(screen.queryByText(/Periodo 1/i)).not.toBeInTheDocument();
        });
    });

    test('permite reordenar preguntas seleccionadas en la plantilla', async () => {
        const activeQuestions = [
            { id: 1, statement: 'Pregunta A', category: 'CatA', bankActive: true, displayOrder: 1 },
            { id: 2, statement: 'Pregunta B', category: 'CatB', bankActive: true, displayOrder: 2 }
        ];
        fetch.mockImplementation(async (url) => {
            if (url.includes('/monitor-survey/admin/questions')) return { ok: true, json: async () => activeQuestions };
            if (url.includes('/monitor-survey/admin/current-config')) return { ok: true, json: async () => ({ semester: '', questions: [] }) };
            if (url.includes('/monitor-survey/admin/templates')) return { ok: true, json: async () => [] };
            return { ok: true, json: async () => [] };
        });
        const { container } = renderComponent();
        await screen.findByText(/Pregunta A/i);
        const addBtns = screen.getAllByRole('button', { name: /Agregar a configuración/i });
        fireEvent.click(addBtns[0]);
        await screen.findByText(/Ya agregada/i);
        fireEvent.click(addBtns[1]);
        await waitFor(() => {
            const summaryDiv = container.querySelector('.hu026-selected-summary');
            expect(summaryDiv.textContent).toContain('2 preguntas seleccionadas');
            expect(summaryDiv.textContent).toContain('2');
        });
        const subirBtns = screen.getAllByRole('button', { name: /Subir/i });
        fireEvent.click(subirBtns[0]);
        await waitFor(() => {
            expect(screen.getByText(/Orden 1/i)).toBeInTheDocument();
            expect(screen.getByText(/Orden 2/i)).toBeInTheDocument();
        });
    });

    test('valida que haya preguntas seleccionadas antes de crear plantilla', async () => {
        renderComponent();
        await screen.findByText(/El monitor cumple con el horario/i);
        const nameInput = screen.getByPlaceholderText(/Nombre de la plantilla/i);
        fireEvent.change(nameInput, { target: { value: 'Mi plantilla' } });
        const saveBtn = screen.getByRole('button', { name: /Guardar plantilla/i });
        fireEvent.click(saveBtn);
        await waitFor(() => {
            expect(screen.getByTestId('popup-message')).toHaveTextContent(/Selecciona preguntas antes/i);
        });
    });

    test('muestra error al fallar guardado de edicion de pregunta', async () => {
        let saveAttempted = false;
        fetch.mockImplementation(async (url, options = {}) => {
            if ((options.method || 'GET') === 'PUT' && url.includes('/questions/') && !url.includes('/status')) {
                saveAttempted = true;
                return { ok: false, json: async () => ({ error: 'Error al editar' }) };
            }
            if (url.includes('/monitor-survey/admin/questions')) return { ok: true, json: async () => mockQuestions };
            if (url.includes('/monitor-survey/admin/current-config')) return { ok: true, json: async () => mockConfig };
            if (url.includes('/monitor-survey/admin/templates')) return { ok: true, json: async () => mockTemplates };
            return { ok: true, json: async () => [] };
        });
        renderComponent();
        await screen.findByText(/El monitor cumple con el horario/i);
        const editBtns = screen.getAllByRole('button', { name: /Editar/i });
        fireEvent.click(editBtns[0]);
        await screen.findByText('Guardar');
        const saveBtns = screen.getAllByRole('button', { name: /Guardar/i });
        fireEvent.click(saveBtns[0]);
        await waitFor(() => {
            expect(saveAttempted).toBe(true);
            expect(screen.getByTestId('popup-message')).toHaveTextContent(/Error al editar/i);
        });
    });

    test('muestra error al fallar cambio de estado de pregunta', async () => {
        let patchAttempted = false;
        fetch.mockImplementation(async (url, options = {}) => {
            if ((options.method || 'GET') === 'PATCH') {
                patchAttempted = true;
                return { ok: false, json: async () => ({ error: 'Error al cambiar estado' }) };
            }
            if (url.includes('/monitor-survey/admin/questions')) return { ok: true, json: async () => mockQuestions };
            if (url.includes('/monitor-survey/admin/current-config')) return { ok: true, json: async () => mockConfig };
            if (url.includes('/monitor-survey/admin/templates')) return { ok: true, json: async () => mockTemplates };
            return { ok: true, json: async () => [] };
        });
        renderComponent();
        await screen.findByText(/El monitor cumple con el horario/i);
        const toggleBtn = screen.getByRole('button', { name: /Desactivar/i });
        fireEvent.click(toggleBtn);
        await waitFor(() => {
            expect(patchAttempted).toBe(true);
            expect(screen.getByTestId('popup-message')).toHaveTextContent(/Error al cambiar estado/i);
        });
    });

    test('elimina plantilla mientras se edita y reinicia formulario', async () => {
        window.confirm = jest.fn(() => true);
        renderComponent();
        await screen.findByText(/Plantilla Base/i);
        const editBtns = screen.getAllByRole('button', { name: /Editar/i });
        fireEvent.click(editBtns[2]);
        await screen.findByText(/Actualizar plantilla/i);
        const deleteBtn = screen.getByRole('button', { name: /Eliminar/i });
        fireEvent.click(deleteBtn);
        await waitFor(() => {
            expect(fetch).toHaveBeenCalledWith(
                expect.stringContaining('/monitor-survey/admin/templates/10'),
                expect.objectContaining({ method: 'DELETE' })
            );
        });
        await waitFor(() => {
            expect(screen.getByText(/Guardar plantilla/i)).toBeInTheDocument();
        });
    });

    test('valida periodo academico antes de aplicar plantilla', async () => {
        const spy = jest.spyOn(globalFix, 'isSelectableAcademicPeriod').mockReturnValue(false);
        renderComponent();
        await screen.findByText(/Plantilla Base/i);
        const applyBtn = screen.getByRole('button', { name: /Aplicar al periodo/i });
        fireEvent.click(applyBtn);
        await waitFor(() => {
            expect(screen.getByTestId('popup-message')).toHaveTextContent(/Debes seleccionar un periodo válido/i);
        });
        spy.mockRestore();
    });

    test('guarda edicion de plantilla exitosamente', async () => {
        const templateWithQuestions = [
            { id: 10, name: 'Plantilla Base', description: 'Descripción base', createdForSemester: '2026-1', questions: [{ id: 1, statement: 'Q', category: 'C', displayOrder: 1 }] }
        ];
        fetch.mockImplementation(async (url, options = {}) => {
            if (options.method === 'PUT' && url.includes('/admin/templates/')) {
                return { ok: true, json: async () => ({ success: true }) };
            }
            if (url.includes('/monitor-survey/admin/questions')) return { ok: true, json: async () => mockQuestions };
            if (url.includes('/monitor-survey/admin/current-config')) return { ok: true, json: async () => mockConfig };
            if (url.includes('/monitor-survey/admin/templates')) return { ok: true, json: async () => templateWithQuestions };
            return { ok: true, json: async () => [] };
        });
        renderComponent();
        await screen.findByText(/Plantilla Base/i);
        const editBtns = screen.getAllByRole('button', { name: /Editar/i });
        fireEvent.click(editBtns[2]);
        await screen.findByText(/Actualizar plantilla/i);
        const updateBtn = screen.getByRole('button', { name: /Actualizar plantilla/i });
        fireEvent.click(updateBtn);
        await waitFor(() => {
            expect(fetch).toHaveBeenCalledWith(
                expect.stringContaining('/admin/templates/10'),
                expect.objectContaining({ method: 'PUT' })
            );
        });
    });

    test('muestra error al fallar creacion de pregunta', async () => {
        let postAttempted = false;
        fetch.mockImplementation(async (url, options = {}) => {
            if ((options.method || 'GET') === 'POST' && url.includes('/admin/questions')) {
                postAttempted = true;
                return { ok: false, json: async () => ({ error: 'Error al crear' }) };
            }
            if (url.includes('/monitor-survey/admin/questions')) return { ok: true, json: async () => mockQuestions };
            if (url.includes('/monitor-survey/admin/current-config')) return { ok: true, json: async () => mockConfig };
            if (url.includes('/monitor-survey/admin/templates')) return { ok: true, json: async () => mockTemplates };
            return { ok: true, json: async () => [] };
        });
        renderComponent();
        const textarea = await screen.findByPlaceholderText(/Texto de la afirmación/i);
        const categoryInput = screen.getByPlaceholderText(/Categoría/i);
        fireEvent.change(textarea, { target: { value: 'Nueva pregunta' } });
        fireEvent.change(categoryInput, { target: { value: 'Nueva categoría' } });
        const submitBtn = screen.getByRole('button', { name: /Agregar pregunta/i });
        fireEvent.click(submitBtn);
        await waitFor(() => {
            expect(postAttempted).toBe(true);
            expect(screen.getByTestId('popup-message')).toHaveTextContent(/Error al crear/i);
        });
    });

    test('muestra error al fallar creacion de plantilla', async () => {
        let postAttempted = false;
        fetch.mockImplementation(async (url, options = {}) => {
            if ((options.method || 'GET') === 'POST' && url.includes('/admin/templates')) {
                postAttempted = true;
                return { ok: false, json: async () => ({ error: 'Error al crear plantilla' }) };
            }
            if (url.includes('/monitor-survey/admin/questions')) return { ok: true, json: async () => mockQuestions };
            if (url.includes('/monitor-survey/admin/current-config')) return { ok: true, json: async () => mockConfig };
            if (url.includes('/monitor-survey/admin/templates')) return { ok: true, json: async () => mockTemplates };
            return { ok: true, json: async () => [] };
        });
        renderComponent();
        await screen.findByText(/Plantilla Base/i);
        const addBtns = screen.getAllByRole('button', { name: /Agregar a configuración/i });
        fireEvent.click(addBtns[0]);
        await screen.findByText(/Ya agregada/i);
        const nameInput = screen.getByPlaceholderText(/Nombre de la plantilla/i);
        fireEvent.change(nameInput, { target: { value: 'Mi plantilla' } });
        const saveBtn = screen.getByRole('button', { name: /Guardar plantilla/i });
        fireEvent.click(saveBtn);
        await waitFor(() => {
            expect(postAttempted).toBe(true);
            expect(screen.getByTestId('popup-message')).toHaveTextContent(/Error al crear plantilla/i);
        });
    });

    test('muestra error al fallar aplicacion de plantilla', async () => {
        let postAttempted = false;
        fetch.mockImplementation(async (url, options = {}) => {
            if ((options.method || 'GET') === 'POST' && url.includes('/apply-template')) {
                postAttempted = true;
                return { ok: false, json: async () => ({ error: 'Error al aplicar' }) };
            }
            if (url.includes('/monitor-survey/admin/questions')) return { ok: true, json: async () => mockQuestions };
            if (url.includes('/monitor-survey/admin/current-config')) return { ok: true, json: async () => mockConfig };
            if (url.includes('/monitor-survey/admin/templates')) return { ok: true, json: async () => mockTemplates };
            return { ok: true, json: async () => [] };
        });
        renderComponent();
        await screen.findByText(/Plantilla Base/i);
        const applyBtn = screen.getByRole('button', { name: /Aplicar al periodo/i });
        fireEvent.click(applyBtn);
        await waitFor(() => {
            expect(postAttempted).toBe(true);
            expect(screen.getByTestId('popup-message')).toHaveTextContent(/Error al aplicar/i);
        });
    });

    test('muestra error al fallar eliminacion de plantilla', async () => {
        window.confirm = jest.fn(() => true);
        let deleteAttempted = false;
        fetch.mockImplementation(async (url, options = {}) => {
            if (options.method === 'DELETE') {
                deleteAttempted = true;
                return { ok: false, json: async () => ({ error: 'Error al eliminar' }) };
            }
            if (url.includes('/monitor-survey/admin/questions')) return { ok: true, json: async () => mockQuestions };
            if (url.includes('/monitor-survey/admin/current-config')) return { ok: true, json: async () => mockConfig };
            if (url.includes('/monitor-survey/admin/templates')) return { ok: true, json: async () => mockTemplates };
            return { ok: true, json: async () => [] };
        });
        renderComponent();
        await screen.findByText(/Plantilla Base/i);
        const deleteBtn = screen.getByRole('button', { name: /Eliminar/i });
        fireEvent.click(deleteBtn);
        await waitFor(() => {
            expect(deleteAttempted).toBe(true);
            expect(screen.getByTestId('popup-message')).toHaveTextContent(/Error al eliminar/i);
        });
    });

    test('edita campos de pregunta al iniciar edicion', async () => {
        renderComponent();
        await screen.findByText(/El monitor cumple con el horario/i);
        const editBtns = screen.getAllByRole('button', { name: /Editar/i });
        fireEvent.click(editBtns[0]);
        const textareas = screen.getAllByRole('textbox');
        const inputs = screen.getAllByRole('textbox');
        const editableTextarea = textareas[1];
        fireEvent.change(editableTextarea, { target: { value: 'Texto editado' } });
        const categoryInputs = screen.getAllByRole('textbox');
        fireEvent.change(categoryInputs[2], { target: { value: 'Categoría editada' } });
        await waitFor(() => {
            expect(editableTextarea.value).toBe('Texto editado');
        });
    });
});
