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

describe('CreateMonitoriaGestionRubricasSimple', () => {
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
            expect(fetch).toHaveBeenCalledWith('http://localhost:5435/monitoring/getAllByProfessor/PROF-1', expect.anything());
        });
    });

    test('CreateMonitoria valida horas solicitadas faltantes al crear', async () => {
        fetch.mockImplementation(async (url) => {
            if (url.includes('/school/getSchools')) {
                return { ok: true, json: async () => [{ name: 'Ingeniería' }] };
            }
            if (url.includes('/monitoring/getAllByProfessor/')) {
                return { ok: true, json: async () => [] };
            }
            if (url.includes('/program/getProgramsSchool')) {
                return { ok: true, json: async () => [{ name: 'Sistemas' }] };
            }
            if (url.includes('/course/getCoursesProgram')) {
                return { ok: true, json: async () => [{ name: 'POO' }] };
            }
            if (url.includes('/department-head/')) {
                return { ok: true, json: async () => [] };
            }
            return { ok: true, json: async () => [] };
        });

        renderWithRouter(<CreateMonitoria />);
        await screen.findByText(/Nueva Monitoría/i);
        await screen.findByText('Ingeniería');

        const selects = document.querySelectorAll('select');
        selects[0].value = 'Ingeniería';
        fireEvent.change(selects[0]);
        await screen.findByText('Sistemas');

        const progSelect = document.querySelectorAll('select')[1];
        progSelect.value = 'Sistemas';
        fireEvent.change(progSelect);
        await screen.findByText('POO');

        const courseSelect = document.querySelectorAll('select')[2];
        courseSelect.value = 'POO';
        fireEvent.change(courseSelect);

        const dateInputs = document.querySelectorAll('input[type="date"]');
        fireEvent.change(dateInputs[0], { target: { value: '2026-01-15' } });
        fireEvent.change(dateInputs[1], { target: { value: '2026-06-15' } });

        const submit = screen.getByRole('button', { name: /Crear Monitoría/i });
        fireEvent.click(submit);

        expect(await screen.findByText(/Debes ingresar las horas solicitadas/i)).toBeInTheDocument();
    });

    test('CreateMonitoria envia formulario exitosamente', async () => {
        let postBody = null;
        fetch.mockImplementation(async (url, options) => {
            if (url.includes('/school/getSchools')) {
                return { ok: true, json: async () => [{ name: 'Ingeniería' }] };
            }
            if (url.includes('/monitoring/getAllByProfessor/')) {
                return { ok: true, json: async () => [] };
            }
            if (url.includes('/program/getProgramsSchool')) {
                return { ok: true, json: async () => [{ name: 'Sistemas' }] };
            }
            if (url.includes('/course/getCoursesProgram')) {
                return { ok: true, json: async () => [{ name: 'POO' }] };
            }
            if (url.includes('/monitoring/create')) {
                postBody = JSON.parse(options.body);
                return { ok: true, text: async () => 'Monitoría creada exitosamente' };
            }
            if (url.includes('/department-head/')) {
                return { ok: true, json: async () => [] };
            }
            return { ok: true, json: async () => [] };
        });

        renderWithRouter(<CreateMonitoria />);
        await screen.findByText(/Nueva Monitoría/i);
        await screen.findByText('Ingeniería');

        const selects = document.querySelectorAll('select');
        selects[0].value = 'Ingeniería';
        fireEvent.change(selects[0]);
        await screen.findByText('Sistemas');

        const progSelect = document.querySelectorAll('select')[1];
        progSelect.value = 'Sistemas';
        fireEvent.change(progSelect);
        await screen.findByText('POO');

        const courseSelect = document.querySelectorAll('select')[2];
        courseSelect.value = 'POO';
        fireEvent.change(courseSelect);

        const hoursInput = screen.getByPlaceholderText(/Ej: 80/i);
        fireEvent.change(hoursInput, { target: { value: '80' } });

        const dateInputs = document.querySelectorAll('input[type="date"]');
        fireEvent.change(dateInputs[0], { target: { value: '2026-01-15' } });
        fireEvent.change(dateInputs[1], { target: { value: '2026-06-15' } });

        const submit = screen.getByRole('button', { name: /Crear Monitoría/i });
        fireEvent.click(submit);

        await waitFor(() => {
            expect(postBody).not.toBeNull();
            expect(postBody.courseName).toBe('POO');
            expect(postBody.programName).toBe('Sistemas');
            expect(postBody.schoolName).toBe('Ingeniería');
            expect(postBody.estimatedHours).toBe(80);
        });
    });

    test('CreateMonitoria muestra tabla con monitorías y paginación', async () => {
        const mockRecords = Array.from({ length: 7 }, (_, i) => ({
            id: i + 1,
            school: { name: 'Ingeniería' },
            program: { name: 'Sistemas' },
            course: { name: `Curso ${i + 1}` },
            semester: '2026-1',
            start: '2026-01-15T00:00:00',
            finish: '2026-06-15T00:00:00',
            estimatedHours: 40 + i * 10,
            hourlyRate: 15000,
            startFormatted: '2026-01-15',
            endFormatted: '2026-06-15',
            totalCost: (40 + i * 10) * 15000
        }));

        fetch.mockImplementation(async (url) => {
            if (url.includes('/school/getSchools')) {
                return { ok: true, json: async () => [{ name: 'Ingeniería' }] };
            }
            if (url.includes('/monitoring/getAllByProfessor/')) {
                return { ok: true, json: async () => mockRecords };
            }
            if (url.includes('/program/getProgramsSchool')) {
                return { ok: true, json: async () => [] };
            }
            if (url.includes('/course/getCoursesProgram')) {
                return { ok: true, json: async () => [] };
            }
            if (url.includes('/department-head/')) {
                return { ok: true, json: async () => [] };
            }
            return { ok: true, json: async () => [] };
        });

        renderWithRouter(<CreateMonitoria />);
        expect(await screen.findByText(/Mis Monitorías \(7\)/i)).toBeInTheDocument();

        expect(screen.getByText(/Página 1 de 2/i)).toBeInTheDocument();
        expect(screen.getByText('Curso 1')).toBeInTheDocument();
        expect(screen.queryByText('Curso 6')).not.toBeInTheDocument();

        fireEvent.click(screen.getByRole('button', { name: /Siguiente/i }));
        expect(screen.getByText(/Página 2 de 2/i)).toBeInTheDocument();
        expect(screen.getByText('Curso 6')).toBeInTheDocument();
        expect(screen.queryByText('Curso 1')).not.toBeInTheDocument();

        fireEvent.click(screen.getByRole('button', { name: /Anterior/i }));
        expect(screen.getByText(/Página 1 de 2/i)).toBeInTheDocument();
    });

    test('CreateMonitoria elimina monitoría existente', async () => {
        const mockRecord = {
            id: 1,
            school: { name: 'Ingeniería' },
            program: { name: 'Sistemas' },
            course: { name: 'POO' },
            semester: '2026-1',
            start: '2026-01-15T00:00:00',
            finish: '2026-06-15T00:00:00',
            estimatedHours: 80,
            hourlyRate: 15000,
        };

        fetch.mockImplementation(async (url, options) => {
            if (url.includes('/school/getSchools')) {
                return { ok: true, json: async () => [{ name: 'Ingeniería' }] };
            }
            if (url.includes('/monitoring/getAllByProfessor/')) {
                return { ok: true, json: async () => [mockRecord] };
            }
            if (url.includes('/monitoring/deleteMonitoring/')) {
                return { ok: true, json: async () => true };
            }
            if (url.includes('/program/getProgramsSchool')) {
                return { ok: true, json: async () => [] };
            }
            if (url.includes('/course/getCoursesProgram')) {
                return { ok: true, json: async () => [] };
            }
            if (url.includes('/department-head/')) {
                return { ok: true, json: async () => [] };
            }
            return { ok: true, json: async () => [] };
        });

        renderWithRouter(<CreateMonitoria />);
        expect(await screen.findByText(/Mis Monitorías \(1\)/i)).toBeInTheDocument();

        fireEvent.click(screen.getByRole('button', { name: /Eliminar/i }));

        await waitFor(() => {
            expect(fetch).toHaveBeenCalledWith(
                expect.stringContaining('/monitoring/deleteMonitoring/1'),
                expect.objectContaining({ method: 'DELETE' })
            );
        });
        expect(await screen.findByText(/Se ha eliminado la monitoría/i)).toBeInTheDocument();
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

    test('CreateMonitoria cierra popup con handleClose', async () => {
        fetch.mockImplementation(async (url) => {
            if (url.includes('/school/getSchools')) {
                return { ok: true, json: async () => [{ name: 'Ingeniería' }] };
            }
            if (url.includes('/monitoring/getAllByProfessor/')) {
                return { ok: true, json: async () => [] };
            }
            return { ok: true, json: async () => [] };
        });

        renderWithRouter(<CreateMonitoria />);
        const submit = await screen.findByRole('button', { name: /Crear Monitoría/i });
        fireEvent.click(submit);
        expect(await screen.findByText(/Por favor completa todos los campos requeridos/i)).toBeInTheDocument();

        fireEvent.click(screen.getByText(/Cerrar/i));
        await waitFor(() => {
            expect(screen.queryByText(/Por favor completa todos los campos requeridos/i)).not.toBeInTheDocument();
        });
    });

    test('CreateMonitoria cambia periodo académico', async () => {
        fetch.mockImplementation(async (url) => {
            if (url.includes('/school/getSchools')) {
                return { ok: true, json: async () => [{ name: 'Ingeniería' }] };
            }
            if (url.includes('/monitoring/getAllByProfessor/')) {
                return { ok: true, json: async () => [] };
            }
            return { ok: true, json: async () => [] };
        });

        renderWithRouter(<CreateMonitoria />);
        await screen.findByText(/Nueva Monitoría/i);

        const semesterSelect = document.querySelectorAll('select')[3];
        fireEvent.change(semesterSelect, { target: { value: '2026-2' } });
        expect(semesterSelect.value).toBe('2026-2');
    });

    test('CreateMonitoria maneja error de fetch de facultades', async () => {
        fetch.mockImplementation(async (url) => {
            if (url.includes('/school/getSchools')) {
                return { ok: false, status: 500 };
            }
            if (url.includes('/monitoring/getAllByProfessor/')) {
                return { ok: true, json: async () => [] };
            }
            return { ok: true, json: async () => [] };
        });

        renderWithRouter(<CreateMonitoria />);
        await screen.findByText(/Nueva Monitoría/i);
    });

    test('CreateMonitoria maneja error de fetch de programas', async () => {
        fetch.mockImplementation(async (url) => {
            if (url.includes('/school/getSchools')) {
                return { ok: true, json: async () => [{ name: 'Ingeniería' }] };
            }
            if (url.includes('/monitoring/getAllByProfessor/')) {
                return { ok: true, json: async () => [] };
            }
            if (url.includes('/program/getProgramsSchool')) {
                return { ok: false, status: 500 };
            }
            return { ok: true, json: async () => [] };
        });

        renderWithRouter(<CreateMonitoria />);
        await screen.findByText('Ingeniería');

        const facultySelect = document.querySelectorAll('select')[0];
        facultySelect.value = 'Ingeniería';
        fireEvent.change(facultySelect);
        await screen.findByText(/Nueva Monitoría/i);
    });

    test('CreateMonitoria maneja error de fetch de cursos', async () => {
        fetch.mockImplementation(async (url) => {
            if (url.includes('/school/getSchools')) {
                return { ok: true, json: async () => [{ name: 'Ingeniería' }] };
            }
            if (url.includes('/monitoring/getAllByProfessor/')) {
                return { ok: true, json: async () => [] };
            }
            if (url.includes('/program/getProgramsSchool')) {
                return { ok: true, json: async () => [{ name: 'Sistemas' }] };
            }
            if (url.includes('/course/getCoursesProgram')) {
                return { ok: false, status: 500 };
            }
            return { ok: true, json: async () => [] };
        });

        renderWithRouter(<CreateMonitoria />);
        await screen.findByText('Ingeniería');

        const facultySelect = document.querySelectorAll('select')[0];
        facultySelect.value = 'Ingeniería';
        fireEvent.change(facultySelect);
        await screen.findByText('Sistemas');

        const progSelect = document.querySelectorAll('select')[1];
        progSelect.value = 'Sistemas';
        fireEvent.change(progSelect);
        await screen.findByText(/Nueva Monitoría/i);
    });

    test('CreateMonitoria maneja error al crear monitoría', async () => {
        fetch.mockImplementation(async (url) => {
            if (url.includes('/school/getSchools')) {
                return { ok: true, json: async () => [{ name: 'Ingeniería' }] };
            }
            if (url.includes('/monitoring/getAllByProfessor/')) {
                return { ok: true, json: async () => [] };
            }
            if (url.includes('/program/getProgramsSchool')) {
                return { ok: true, json: async () => [{ name: 'Sistemas' }] };
            }
            if (url.includes('/course/getCoursesProgram')) {
                return { ok: true, json: async () => [{ name: 'POO' }] };
            }
            if (url.includes('/monitoring/create')) {
                throw new Error('Error de red');
            }
            return { ok: true, json: async () => [] };
        });

        renderWithRouter(<CreateMonitoria />);
        await screen.findByText(/Nueva Monitoría/i);
        await screen.findByText('Ingeniería');

        const selects = document.querySelectorAll('select');
        selects[0].value = 'Ingeniería';
        fireEvent.change(selects[0]);
        await screen.findByText('Sistemas');

        const progSelect = document.querySelectorAll('select')[1];
        progSelect.value = 'Sistemas';
        fireEvent.change(progSelect);
        await screen.findByText('POO');

        const courseSelect = document.querySelectorAll('select')[2];
        courseSelect.value = 'POO';
        fireEvent.change(courseSelect);

        const hoursInput = screen.getByPlaceholderText(/Ej: 80/i);
        fireEvent.change(hoursInput, { target: { value: '80' } });

        const dateInputs = document.querySelectorAll('input[type="date"]');
        fireEvent.change(dateInputs[0], { target: { value: '2026-01-15' } });
        fireEvent.change(dateInputs[1], { target: { value: '2026-06-15' } });

        const submit = screen.getByRole('button', { name: /Crear Monitoría/i });
        fireEvent.click(submit);

        expect(await screen.findByText(/Error al crear la monitoría/i)).toBeInTheDocument();
    });

    test('CreateMonitoria maneja error al eliminar monitoría', async () => {
        const mockRecord = {
            id: 99, school: { name: 'Ingeniería' }, program: { name: 'Sistemas' },
            course: { name: 'POO' }, semester: '2026-1',
            start: '2026-01-15T00:00:00', finish: '2026-06-15T00:00:00',
            estimatedHours: 80, hourlyRate: 15000,
        };

        fetch.mockImplementation(async (url) => {
            if (url.includes('/school/getSchools')) {
                return { ok: true, json: async () => [{ name: 'Ingeniería' }] };
            }
            if (url.includes('/monitoring/getAllByProfessor/')) {
                return { ok: true, json: async () => [mockRecord] };
            }
            if (url.includes('/monitoring/deleteMonitoring/')) {
                throw new Error('Error de red');
            }
            return { ok: true, json: async () => [] };
        });

        renderWithRouter(<CreateMonitoria />);
        expect(await screen.findByText(/Mis Monitorías \(1\)/i)).toBeInTheDocument();

        fireEvent.click(screen.getByRole('button', { name: /Eliminar/i }));
        expect(await screen.findByText(/Error en el servidor/i)).toBeInTheDocument();
    });

    test('CreateMonitoria renderiza con rol jfedpto y valida selección de profesor', async () => {
        setupLocalStorage({ role: 'jfedpto', userId: 'JEFE-1' });

        fetch.mockImplementation(async (url) => {
            if (url.includes('/school/getSchools')) {
                return { ok: true, json: async () => [{ name: 'Ingeniería' }] };
            }
            if (url.includes('/monitoring/getAllByProfessor/')) {
                return { ok: true, json: async () => [] };
            }
            if (url.includes('/program/getProgramsSchool')) {
                return { ok: true, json: async () => [{ name: 'Sistemas' }] };
            }
            if (url.includes('/course/getCoursesProgram')) {
                return { ok: true, json: async () => [{ name: 'POO' }] };
            }
            if (url.includes('/department-head/JEFE-1/professors')) {
                return { ok: true, json: async () => [{ id: 'PROF-1', name: 'Profesor Uno' }] };
            }
            return { ok: true, json: async () => [] };
        });

        renderWithRouter(<CreateMonitoria />);
        expect(await screen.findByText(/Profesor responsable/i)).toBeInTheDocument();
        expect(await screen.findByText(/Profesor Uno/i)).toBeInTheDocument();

        // Fill all required fields to bypass the "completa todos los campos" validation
        const facultySelect = document.querySelectorAll('select')[1];
        facultySelect.value = 'Ingeniería';
        fireEvent.change(facultySelect);
        await screen.findByText('Sistemas');

        const progSelect = document.querySelectorAll('select')[2];
        progSelect.value = 'Sistemas';
        fireEvent.change(progSelect);
        await screen.findByText('POO');

        const courseSelect = document.querySelectorAll('select')[3];
        courseSelect.value = 'POO';
        fireEvent.change(courseSelect);

        const hoursInput = screen.getByPlaceholderText(/Ej: 80/i);
        fireEvent.change(hoursInput, { target: { value: '80' } });

        const dateInputs = document.querySelectorAll('input[type="date"]');
        fireEvent.change(dateInputs[0], { target: { value: '2026-01-15' } });
        fireEvent.change(dateInputs[1], { target: { value: '2026-06-15' } });

        const submit = screen.getByRole('button', { name: /Crear Monitoría/i });
        fireEvent.click(submit);
        expect(await screen.findByText(/Selecciona un profesor responsable antes de confirmar/i)).toBeInTheDocument();
    });

    test('CreateMonitoria maneja error de red al cargar archivo CSV', async () => {
        fetch.mockImplementation(async (url) => {
            if (url.includes('/school/getSchools')) {
                return { ok: true, json: async () => [{ name: 'Ingeniería' }] };
            }
            if (url.includes('/monitoring/getAllByProfessor/PROF-1')) {
                return { ok: true, json: async () => [] };
            }
            if (url.includes('/monitoring/createAll/')) {
                throw new Error('Error de red');
            }
            return { ok: true, json: async () => [] };
        });

        renderWithRouter(<CreateMonitoria />);
        await screen.findByText(/Cargar Datos/i);

        const fileInput = document.querySelector('input[type="file"]');
        const file = new File(['test content'], 'test.csv', { type: 'text/csv' });
        fireEvent.change(fileInput, { target: { files: [file] } });

        expect(await screen.findByText(/Error al conectar con el servidor/i)).toBeInTheDocument();
    });

    test('CreateMonitoria maneja error al refrescar monitorías', async () => {
        fetch.mockImplementation(async (url) => {
            if (url.includes('/school/getSchools')) {
                return { ok: true, json: async () => [{ name: 'Ingeniería' }] };
            }
            if (url.includes('/monitoring/getAllByProfessor/PROF-1')) {
                throw new Error('Error de red');
            }
            return { ok: true, json: async () => [] };
        });

        renderWithRouter(<CreateMonitoria />);
        await screen.findByText(/Nueva Monitoría/i);
    });

    test('CreateMonitoria valida carga CSV sin profesor para jfedpto', async () => {
        setupLocalStorage({ role: 'jfedpto', userId: 'JEFE-1' });

        fetch.mockImplementation(async (url) => {
            if (url.includes('/school/getSchools')) {
                return { ok: true, json: async () => [{ name: 'Ingeniería' }] };
            }
            if (url.includes('/monitoring/getAllByProfessor/')) {
                return { ok: true, json: async () => [] };
            }
            if (url.includes('/department-head/JEFE-1/professors')) {
                return { ok: true, json: async () => [] };
            }
            return { ok: true, json: async () => [] };
        });

        renderWithRouter(<CreateMonitoria />);
        await screen.findByText(/Profesor responsable/i);

        fireEvent.click(screen.getByRole('button', { name: /Cargar Datos/i }));
        expect(await screen.findByText(/Por favor selecciona un profesor responsable/i)).toBeInTheDocument();
    });

    test('CreateMonitoria actualiza campos de formulario restantes', async () => {
        fetch.mockImplementation(async (url) => {
            if (url.includes('/school/getSchools')) {
                return { ok: true, json: async () => [{ name: 'Ingeniería' }] };
            }
            if (url.includes('/monitoring/getAllByProfessor/')) {
                return { ok: true, json: async () => [] };
            }
            return { ok: true, json: async () => [] };
        });

        renderWithRouter(<CreateMonitoria />);
        await screen.findByText(/Nueva Monitoría/i);

        const rateInput = screen.getByPlaceholderText(/Ej: 15000/i);
        fireEvent.change(rateInput, { target: { value: '20000' } });
        expect(rateInput.value).toBe('20000');

        const gradeInputs = screen.getAllByPlaceholderText(/Ej: 4.0/i);
        fireEvent.change(gradeInputs[0], { target: { value: '4.5' } });
        expect(gradeInputs[0].value).toBe('4.5');

        fireEvent.change(gradeInputs[1], { target: { value: '3.5' } });
        expect(gradeInputs[1].value).toBe('3.5');
    });

    describe('GestionRubricas', () => {

        test('renderiza estado vacío cuando no hay rúbricas', async () => {
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

        test('GestionRubricas permite agregar y quitar criterios', async () => {
            fetch.mockResolvedValue({ ok: true, json: async () => [] });
            renderWithRouter(<GestionRubricas />);

            fireEvent.click(await screen.findByRole('button', { name: /Crear Nueva Rúbrica/i }));
            expect(await screen.findByText(/Criterio 1/i)).toBeInTheDocument();

            fireEvent.click(screen.getByRole('button', { name: /Agregar Criterio/i }));
            expect(screen.getByText(/Criterio 2/i)).toBeInTheDocument();
        });

        test('GestionRubricas muestra mensaje al eliminar con un solo criterio', async () => {
            fetch.mockResolvedValue({ ok: true, json: async () => [] });
            renderWithRouter(<GestionRubricas />);

            fireEvent.click(await screen.findByRole('button', { name: /Crear Nueva Rúbrica/i }));
            const removeBtn = screen.getAllByTitle(/Eliminar criterio/i)[0];
            fireEvent.click(removeBtn);

            expect(await screen.findByText(/Debe haber al menos un criterio/i)).toBeInTheDocument();
        });

        test('GestionRubricas permite crear rubrica exitosamente', async () => {
            fetch.mockResolvedValue({ ok: true, json: async () => [] });
            const { container } = renderWithRouter(<GestionRubricas />);

            fireEvent.click(await screen.findByRole('button', { name: /Crear Nueva Rúbrica/i }));

            fireEvent.change(screen.getByPlaceholderText(/Ej: Evaluación de Tutoría/i), { target: { value: 'Mi Rúbrica' } });
            fireEvent.change(screen.getByPlaceholderText(/Describe el propósito/i), { target: { value: 'Descripción de prueba' } });

            fireEvent.change(screen.getByPlaceholderText(/Ej: Puntualidad/i), { target: { value: 'Calidad' } });
            fireEvent.change(screen.getByPlaceholderText(/¿Qué se evalúa/i), { target: { value: 'Evaluación de calidad' } });
            fireEvent.change(container.querySelector('.points-group input'), { target: { value: '10' } });

            const createBtn = screen.getByRole('button', { name: /Crear Rúbrica/i });
            const form = createBtn.closest('form');
            fireEvent.submit(form);

            await waitFor(() => {
                expect(fetch).toHaveBeenCalledWith(
                    expect.stringContaining('/api/rubric'),
                    expect.objectContaining({ method: 'POST', body: expect.stringContaining('Mi Rúbrica') })
                );
            });
        });

        test('GestionRubricas no se rompe cuando falla la carga', async () => {
            fetch.mockRejectedValue(new Error('Error de red'));

            renderWithRouter(<GestionRubricas />);

            expect(await screen.findByText(/Gestión de Rúbricas/i)).toBeInTheDocument();
        });

        test('GestionRubricas renderiza rúbricas en tarjetas y abre modal de edición', async () => {
            const mockRubrics = [
                {
                    id: 1,
                    name: 'Rúbrica Uno',
                    description: 'Primera rúbrica',
                    totalPoints: 15,
                    criteria: [
                        { criterion: 'Calidad', description: 'Calidad del trabajo', points: 10 },
                        { criterion: 'Puntualidad', description: 'Entrega a tiempo', points: 5 }
                    ]
                }
            ];

            fetch.mockResolvedValue({ ok: true, json: async () => mockRubrics });

            renderWithRouter(<GestionRubricas />);

            expect(await screen.findByText('Rúbrica Uno')).toBeInTheDocument();
            expect(screen.getByText('Primera rúbrica')).toBeInTheDocument();
            expect(screen.getByText('15')).toBeInTheDocument();
            expect(screen.getByText('pts')).toBeInTheDocument();
            expect(screen.getByRole('button', { name: /Editar/i })).toBeInTheDocument();
            expect(screen.getByRole('button', { name: /Eliminar/i })).toBeInTheDocument();
            expect(screen.getByRole('button', { name: /Ir a Plan de Actividades/i })).toBeInTheDocument();

            fireEvent.click(screen.getByRole('button', { name: /Editar/i }));

            expect(await screen.findByRole('heading', { name: /Editar Rúbrica/i })).toBeInTheDocument();
            expect(screen.getByDisplayValue('Rúbrica Uno')).toBeInTheDocument();
            expect(screen.getByDisplayValue('Primera rúbrica')).toBeInTheDocument();
            expect(screen.getByDisplayValue('Calidad')).toBeInTheDocument();
            expect(screen.getByDisplayValue('Puntualidad')).toBeInTheDocument();
        });

        test('GestionRubricas elimina criterio cuando hay varios', async () => {
            fetch.mockResolvedValue({ ok: true, json: async () => [] });
            renderWithRouter(<GestionRubricas />);

            fireEvent.click(await screen.findByRole('button', { name: /Crear Nueva Rúbrica/i }));
            expect(screen.getByText(/Criterio 1/i)).toBeInTheDocument();

            fireEvent.click(screen.getByRole('button', { name: /Agregar Criterio/i }));
            expect(screen.getByText(/Criterio 2/i)).toBeInTheDocument();

            const removeButtons = screen.getAllByTitle(/Eliminar criterio/i);
            fireEvent.click(removeButtons[0]);

            expect(screen.queryByText(/Criterio 2/i)).not.toBeInTheDocument();
        });

        test('GestionRubricas muestra error del servidor al crear rúbrica', async () => {
            fetch
                .mockResolvedValueOnce({ ok: true, json: async () => [] })
                .mockResolvedValueOnce({ ok: false, text: async () => 'Error interno' });

            const { container } = renderWithRouter(<GestionRubricas />);

            fireEvent.click(await screen.findByRole('button', { name: /Crear Nueva Rúbrica/i }));

            fireEvent.change(screen.getByPlaceholderText(/Ej: Evaluación de Tutoría/i), { target: { value: 'Test' } });
            fireEvent.change(screen.getByPlaceholderText(/Describe el propósito/i), { target: { value: 'Desc' } });
            fireEvent.change(screen.getByPlaceholderText(/Ej: Puntualidad/i), { target: { value: 'Calidad' } });
            fireEvent.change(screen.getByPlaceholderText(/¿Qué se evalúa/i), { target: { value: 'Eval' } });
            fireEvent.change(container.querySelector('.points-group input'), { target: { value: '10' } });

            const createBtn = screen.getByRole('button', { name: /Crear Rúbrica/i });
            fireEvent.submit(createBtn.closest('form'));

            expect(await screen.findByText(/Error al guardar la rúbrica: Error interno/i)).toBeInTheDocument();
        });

        test('GestionRubricas muestra error de red al crear rúbrica', async () => {
            fetch
                .mockResolvedValueOnce({ ok: true, json: async () => [] })
                .mockRejectedValueOnce(new Error('Error de conexión'));

            const { container } = renderWithRouter(<GestionRubricas />);

            fireEvent.click(await screen.findByRole('button', { name: /Crear Nueva Rúbrica/i }));

            fireEvent.change(screen.getByPlaceholderText(/Ej: Evaluación de Tutoría/i), { target: { value: 'Test' } });
            fireEvent.change(screen.getByPlaceholderText(/Describe el propósito/i), { target: { value: 'Desc' } });
            fireEvent.change(screen.getByPlaceholderText(/Ej: Puntualidad/i), { target: { value: 'Calidad' } });
            fireEvent.change(screen.getByPlaceholderText(/¿Qué se evalúa/i), { target: { value: 'Eval' } });
            fireEvent.change(container.querySelector('.points-group input'), { target: { value: '10' } });

            const createBtn = screen.getByRole('button', { name: /Crear Rúbrica/i });
            fireEvent.submit(createBtn.closest('form'));

            expect(await screen.findByText(/Error al guardar la rúbrica: Error de conexión/i)).toBeInTheDocument();
        });

        test('GestionRubricas elimina rúbrica exitosamente', async () => {
            const mockRubric = {
                id: 5,
                name: 'Rúbrica a eliminar',
                description: 'Se eliminará',
                totalPoints: 10,
                criteria: [{ criterion: 'Criterio 1', description: 'Desc', points: 10 }]
            };

            window.confirm = jest.fn(() => true);

            fetch
                .mockResolvedValueOnce({ ok: true, json: async () => [mockRubric] })
                .mockResolvedValueOnce({ ok: true, json: async () => true })
                .mockResolvedValue({ ok: true, json: async () => [] });

            renderWithRouter(<GestionRubricas />);
            expect(await screen.findByText('Rúbrica a eliminar')).toBeInTheDocument();

            fireEvent.click(screen.getByRole('button', { name: /Eliminar/i }));

            await waitFor(() => {
                expect(fetch).toHaveBeenCalledWith(
                    expect.stringContaining('/api/rubric/delete/5'),
                    expect.objectContaining({ method: 'DELETE' })
                );
            });
        });

        test('GestionRubricas no elimina si usuario cancela confirmación', async () => {
            const mockRubric = {
                id: 10,
                name: 'No eliminar',
                description: 'Des',
                totalPoints: 5,
                criteria: [{ criterion: 'C', description: 'D', points: 5 }]
            };

            window.confirm = jest.fn(() => false);

            fetch
                .mockResolvedValueOnce({ ok: true, json: async () => [mockRubric] })
                .mockResolvedValue({ ok: true, json: async () => [] });

            renderWithRouter(<GestionRubricas />);
            expect(await screen.findByText('No eliminar')).toBeInTheDocument();

            fireEvent.click(screen.getByRole('button', { name: /Eliminar/i }));

            await waitFor(() => {
                expect(fetch).not.toHaveBeenCalledWith(
                    expect.stringContaining('/api/rubric/delete/'),
                    expect.anything()
                );
            });
        });
    });
});