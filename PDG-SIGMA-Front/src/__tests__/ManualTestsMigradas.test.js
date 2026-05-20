import React from 'react';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { BrowserRouter } from 'react-router-dom';
import CreateActivity from '../CreateActivity';
import PlanActividades from '../PlanActividades';
import CreateMonitoria from '../CreateMonitoria';
import VistaMonitorActividades from '../VistaMonitorActividades';

const mockNavigate = jest.fn();

jest.mock('react-router-dom', () => ({
    ...jest.requireActual('react-router-dom'),
    useNavigate: () => mockNavigate,
    useParams: () => ({})
}));

jest.mock('../VerticalNavbar', () => () => <div data-testid="mock-navbar">Navbar</div>);

jest.mock('../PopUp', () => ({
    PopUp: ({ show, onClose, children, message }) => {
        if (show === false) return null;
        return (
            <div data-testid="mock-popup">
                {children || message}
                {onClose && <button onClick={onClose}>Cerrar</button>}
            </div>
        );
    },
    PopUpUpdateBudget: ({ show }) => (show ? <div data-testid="popup-budget">Budget</div> : null)
}));

jest.mock('../config/ApiBackend', () => ({
    BACKEND_URL: 'http://localhost:5435',
    getApiUrl: (path) => `http://localhost:5435/${path?.startsWith('/') ? path.slice(1) : path || ''}`
}));

const setupLocalStorage = (overrides = {}) => {
    Object.defineProperty(window, 'localStorage', {
        value: {
            getItem: jest.fn((key) => {
                const store = { role: 'professor', userId: '1234567', token: 'Bearer test-token', ...overrides };
                return store[key] || null;
            }),
            setItem: jest.fn(),
            clear: jest.fn()
        },
        writable: true
    });
};

function mockFetch(responses) {
    global.fetch = jest.fn(async (url) => {
        for (const [pattern, response] of responses) {
            if (url.includes(pattern)) return response;
        }
        return { ok: true, json: async () => [] };
    });
}

async function selectDropdownOption(select, value) {
    await userEvent.selectOptions(select, value);
}

describe('Manual: Actividad CRUD', () => {
    beforeEach(() => {
        jest.clearAllMocks();
        setupLocalStorage();
        global.fetch = jest.fn();
        window.confirm = jest.fn(() => true);
    });

    test('[CreateActivity] Creación exitosa - createActivity (POST /activity/create → 201)', async () => {
        mockFetch([
            ['/monitoring/getAllActiveByUserId/', {
                ok: true, json: async () => [{
                    id: 7, course: { id: 100, name: 'Programación I' },
                    program: { id: 5, name: 'Ingeniería de Sistemas' },
                    semester: '2025-1', professor: { id: '1234567', name: 'Juan Esteban Caldas' }
                }]
            }],
            ['/student/getA', { ok: true, json: async () => [] }],
            ['/monitoring-monitor/', {
                ok: true, json: async () => [{ userId: '12345678', name: 'Sofia Martinez', rol: 'M' }]
            }],
            ['/category/course/', {
                ok: true, json: async () => [{ id: 1, name: 'CategoriaValida' }]
            }],
            ['/student/course/', { ok: true, json: async () => [] }],
            ['/activity/create', {
                ok: true, json: async () => ({ id: 79, name: 'Nombre de Actividad de Prueba', state: 'PENDIENTE' })
            }]
        ]);

        const { container } = render(
            <BrowserRouter><CreateActivity /></BrowserRouter>
        );

        await screen.findByText('Crear Actividad');

        const nombreInput = container.querySelector('.input-text');
        fireEvent.change(nombreInput, { target: { value: 'Nombre de Actividad de Prueba' } });

        const cursoSelect = container.querySelector('.activity-select');
        await waitFor(() => expect(cursoSelect.children.length).toBeGreaterThan(1));
        await selectDropdownOption(cursoSelect, '7');

        await waitFor(() => {
            expect(screen.getByText('CategoriaValida')).toBeInTheDocument();
        });
        const categoriaSelect = container.querySelector('#categoria');
        await selectDropdownOption(categoriaSelect, 'CategoriaValida');

        const fechaInput = container.querySelector('#fechaFinalizacion');
        fireEvent.change(fechaInput, { target: { value: '2025-06-30' } });

        const asignarSelect = container.querySelector('.asign-to-select');
        await waitFor(() => expect(asignarSelect.children.length).toBeGreaterThan(1));
        await selectDropdownOption(asignarSelect, '12345678');

        fireEvent.click(screen.getByRole('button', { name: /Confirmar/i }));

        await waitFor(() => {
            expect(screen.getByText('¡Actividad creada exitosamente!')).toBeInTheDocument();
        }, { timeout: 8000 });
    }, 15000);

    test('[PlanActividades] Eliminación exitosa - deleteActivityByIdSuccess (DELETE /activity/{id} → 200)', async () => {
        mockFetch([
            ['/monitoring/getAllByProfessor/', {
                ok: true, json: async () => [{
                    id: 15, course: { name: 'Algoritmos' },
                    program: { name: 'Ingeniería de Sistemas' },
                    semester: '2026-1',
                    assignedMonitor: { name: 'Ana', lastName: 'Paz', code: 'MON-9' }
                }]
            }],
            ['/api/activity-schedule/plan/', {
                ok: true, json: async () => ({
                    courseName: 'Algoritmos', programName: 'Ingeniería de Sistemas',
                    professorName: 'Profe 1', monitorName: 'Ana Paz',
                    semester: '2026-1', totalActivities: 1,
                    completedActivities: 0, pendingActivities: 1, totalHours: 2,
                    activities: [{
                        id: 47, name: 'Actividad de Prueba', category: 'Académico',
                        finish: '2025-06-10T18:00:00Z', state: 'PENDIENTE',
                        priority: 'MEDIA', durationHours: 2, startTime: '10:00', endTime: '12:00'
                    }]
                })
            }],
            ['/api/rubric/professor/', { ok: true, json: async () => [] }],
            ['/activity/', { ok: true, json: async () => 'Actividad eliminada' }]
        ]);

        render(<BrowserRouter><PlanActividades /></BrowserRouter>);

        await waitFor(() => {
            expect(screen.getByText('Actividad de Prueba')).toBeInTheDocument();
        });

        const deleteBtn = screen.getByRole('button', { name: /eliminar actividad/i });
        fireEvent.click(deleteBtn);

        await waitFor(() => {
            expect(screen.getByText('Actividad eliminada exitosamente')).toBeInTheDocument();
        }, { timeout: 5000 });
    });

    test('[VistaMonitorActividades] Búsqueda de actividad por ID - findActivityById (GET /activity/{id} → 200)', async () => {
        setupLocalStorage({ role: 'monitor', userId: '1234567' });
        mockFetch([
            ['/activity/findAll/', {
                ok: true, json: async () => [{
                    id: 47,
                    name: 'Actividad Actualizada',
                    description: 'Descripción actualizada de la actividad.',
                    category: 'NuevaCategoria',
                    finish: '2025-06-10T18:00:00.000+00:00',
                    state: 'PENDIENTE',
                    priority: 'MEDIA',
                    startTime: '10:00',
                    endTime: '12:00',
                    durationHours: 2,
                    monitoring: {
                        id: 15,
                        course: { id: 2, name: 'Histología - A' },
                        program: { id: 1, name: 'Medicina' },
                        professor: { id: '78912345', name: 'Luis Guerra C.' },
                        semester: '2025-1'
                    },
                    monitor: { name: 'Sofia', lastName: 'Martinez', code: 'A00379504' }
                }]
            }]
        ]);

        const { container } = render(<BrowserRouter><VistaMonitorActividades /></BrowserRouter>);

        await waitFor(() => {
            expect(screen.getByText('Actividad Actualizada')).toBeInTheDocument();
        });

        fireEvent.click(screen.getByText('Actividad Actualizada'));

        await screen.findByText('Información General');
        expect(screen.getByRole('heading', { name: /Actividad Actualizada/, level: 2 })).toBeInTheDocument();
    });

    test('[VistaMonitorActividades] Búsqueda de actividad no encontrada - findActivityByIdNotFound (GET /activity/{id} → 404)', async () => {
        setupLocalStorage({ role: 'monitor', userId: '1234567' });
        mockFetch([
            ['/activity/findAll/', {
                ok: false, status: 404,
                text: async () => 'Activity not found',
                json: async () => ({ message: 'Activity not found' })
            }]
        ]);

        render(<BrowserRouter><VistaMonitorActividades /></BrowserRouter>);

        await waitFor(() => {
            expect(screen.getByText(/Error al cargar tus actividades/i)).toBeInTheDocument();
        });
    });

    test('[PlanActividades] Actualización de actividad - updateActivity (POST /api/activity-schedule/create → 200)', async () => {
        const mockData = {
            id: 15,
            course: { name: 'Histología - A' },
            program: { name: 'Medicina' },
            semester: '2025-1',
            assignedMonitor: { name: 'Sofia', lastName: 'Martinez', code: 'A00379504' }
        };

        const planData = {
            courseName: 'Histología - A',
            programName: 'Medicina',
            professorName: 'Luis Guerra C.',
            monitorName: 'Sofia Martinez',
            semester: '2025-1',
            totalActivities: 1,
            completedActivities: 0,
            pendingActivities: 1,
            totalHours: 2,
            activities: [{
                id: 47,
                name: 'Actividad Original',
                category: 'Tutoría',
                description: 'Descripción original',
                finish: '2025-06-10T18:00:00Z',
                state: 'PENDIENTE',
                priority: 'MEDIA',
                durationHours: 2,
                startTime: '10:00',
                endTime: '12:00',
                monitoringId: 15
            }]
        };

        mockFetch([
            ['/monitoring/getAllByProfessor/', { ok: true, json: async () => [mockData] }],
            ['/api/activity-schedule/plan/', { ok: true, json: async () => planData }],
            ['/api/rubric/professor/', { ok: true, json: async () => [] }],
            ['/api/activity-schedule/validate-conflicts', { ok: true, json: async () => [] }],
            ['/api/activity-schedule/create', { ok: true, json: async () => ({ id: 47, name: 'Actividad Actualizada', state: 'PENDIENTE' }) }]
        ]);

        render(<BrowserRouter><PlanActividades /></BrowserRouter>);

        await waitFor(() => {
            expect(screen.getByText('Actividad Original')).toBeInTheDocument();
        }, { timeout: 10000 });

        const editBtn = screen.getByRole('button', { name: /editar actividad/i });
        fireEvent.click(editBtn);

        await waitFor(() => {
            expect(screen.getByText('Editar Actividad')).toBeInTheDocument();
        });

        const nameInput = screen.getByDisplayValue('Actividad Original');
        fireEvent.change(nameInput, { target: { value: 'Actividad Actualizada' } });

        const submitBtn = screen.getByRole('button', { name: /actualizar/i });
        fireEvent.click(submitBtn);

        await waitFor(() => {
            expect(screen.getByText('Actividad actualizada exitosamente')).toBeInTheDocument();
        }, { timeout: 10000 });
    }, 20000);
});

describe('Manual: Monitoría CRUD', () => {
    beforeEach(() => {
        jest.clearAllMocks();
        setupLocalStorage();
        global.fetch = jest.fn();
    });

    test('[CreateMonitoria] Creación exitosa - createMonitoria (POST /monitoring/create → 200)', async () => {
        mockFetch([
            ['/school/getSchools', {
                ok: true, json: async () => [{ name: 'Ciencias Humanas' }]
            }],
            ['/monitoring/getAllByProfessor/', { ok: true, json: async () => [] }],
            ['/program/getProgramsSchool', {
                ok: true, json: async () => [{ name: 'Ciencia Política' }]
            }],
            ['/course/getCoursesProgram', {
                ok: true, json: async () => [{ name: 'Analisis Político I' }]
            }],
            ['/monitoring/create', {
                ok: true, json: async () => 'Se ha creado una monitoria',
                text: async () => 'Se ha creado una monitoria'
            }]
        ]);

        const { container } = render(
            <BrowserRouter><CreateMonitoria /></BrowserRouter>
        );

        await screen.findByText('Crear/Cargar Monitorías');

        const allSelects = () => container.querySelectorAll('select');

        const facultySelect = allSelects()[0];
        await waitFor(() => expect(facultySelect.children.length).toBeGreaterThan(1));
        await selectDropdownOption(facultySelect, 'Ciencias Humanas');

        await waitFor(() => expect(allSelects()[1].children.length).toBeGreaterThan(1));

        const programSelect = allSelects()[1];
        await selectDropdownOption(programSelect, 'Ciencia Política');

        await waitFor(() => expect(allSelects()[2].children.length).toBeGreaterThan(1));

        const courseSelect = allSelects()[2];
        await selectDropdownOption(courseSelect, 'Analisis Político I');

        const hoursInput = container.querySelector('input[placeholder="Ej: 80"]');
        fireEvent.change(hoursInput, { target: { value: '80' } });

        const dateInputs = container.querySelectorAll('input[type="date"]');
        fireEvent.change(dateInputs[0], { target: { value: '2025-05-30' } });
        fireEvent.change(dateInputs[1], { target: { value: '2025-06-18' } });

        fireEvent.click(screen.getByRole('button', { name: /Crear Monitoría/i }));

        await waitFor(() => {
            expect(screen.getByText('Se ha creado una monitoria')).toBeInTheDocument();
        }, { timeout: 8000 });
    }, 15000);

    test('[CreateMonitoria] Error al crear datos inválidos - Create_Monitoria_Not_Sucess (POST /monitoring/create → 400/403)', async () => {
        mockFetch([
            ['/school/getSchools', {
                ok: true, json: async () => [{ name: 'Ciencias Humanas' }]
            }],
            ['/monitoring/getAllByProfessor/', { ok: true, json: async () => [] }],
            ['/program/getProgramsSchool', {
                ok: true, json: async () => [{ name: 'Ciencia Política' }]
            }],
            ['/course/getCoursesProgram', {
                ok: true, json: async () => [{ name: 'Analisis Político I' }]
            }],
            ['/monitoring/create', {
                ok: false, status: 400,
                text: async () => 'Datos inválidos'
            }]
        ]);

        const { container } = render(
            <BrowserRouter><CreateMonitoria /></BrowserRouter>
        );

        await screen.findByText('Crear/Cargar Monitorías');

        const allSelects = () => container.querySelectorAll('select');

        await waitFor(() => expect(allSelects()[0].children.length).toBeGreaterThan(1));
        await selectDropdownOption(allSelects()[0], 'Ciencias Humanas');

        await waitFor(() => expect(allSelects()[1].children.length).toBeGreaterThan(1));
        await selectDropdownOption(allSelects()[1], 'Ciencia Política');

        await waitFor(() => expect(allSelects()[2].children.length).toBeGreaterThan(1));
        await selectDropdownOption(allSelects()[2], 'Analisis Político I');

        const hoursInput = container.querySelector('input[placeholder="Ej: 80"]');
        fireEvent.change(hoursInput, { target: { value: '80' } });

        const dateInputs = container.querySelectorAll('input[type="date"]');
        fireEvent.change(dateInputs[0], { target: { value: '2025-05-30' } });
        fireEvent.change(dateInputs[1], { target: { value: '2025-06-18' } });

        fireEvent.click(screen.getByRole('button', { name: /Crear Monitoría/i }));

        await waitFor(() => {
            expect(screen.getByText('Datos inválidos')).toBeInTheDocument();
        }, { timeout: 8000 });
    }, 15000);

    test('[CreateMonitoria] Eliminación fallida - deleteMonitoriaByIdNotSuccess (DELETE /monitoring/deleteMonitoring/{id} → 403)', async () => {
        mockFetch([
            ['/school/getSchools', {
                ok: true, json: async () => [{ name: 'Ciencias Humanas' }]
            }],
            ['/monitoring/getAllByProfessor/', {
                ok: true, json: async () => [{
                    id: 15, school: { name: 'Ciencias Humanas' },
                    program: { name: 'Ciencia Política' },
                    course: { name: 'Analisis Político I' },
                    semester: '2025-1', start: '2025-05-30T00:00:00Z',
                    finish: '2025-06-18T00:00:00Z', estimatedHours: 80, hourlyRate: 15000
                }]
            }],
            ['/program/getProgramsSchool', { ok: true, json: async () => [] }],
            ['/course/getCoursesProgram', { ok: true, json: async () => [] }],
            ['/monitoring/deleteMonitoring/', {
                ok: true,
                json: async () => false
            }]
        ]);

        render(<BrowserRouter><CreateMonitoria /></BrowserRouter>);

        const deleteBtn = await screen.findByText('Eliminar');
        fireEvent.click(deleteBtn);

        await waitFor(() => {
            expect(screen.getByText(/La monitoria no pudo ser eliminada/i)).toBeInTheDocument();
        }, { timeout: 5000 });
    });

    test('[CreateMonitoria] Búsqueda por facultad - findMonitoriaByFaculty (POST /program/getProgramsSchool → 200)', async () => {
        mockFetch([
            ['/school/getSchools', {
                ok: true, json: async () => [
                    { name: 'Ciencias de la Salud' },
                    { name: 'Ciencias Humanas' },
                    { name: 'Ingeniería' }
                ]
            }],
            ['/monitoring/getAllByProfessor/', { ok: true, json: async () => [] }],
            ['/program/getProgramsSchool', {
                ok: true, json: async () => [
                    { name: 'Medicina' },
                    { name: 'Enfermería' }
                ]
            }],
            ['/course/getCoursesProgram', {
                ok: true, json: async () => [{ name: 'Fisiología - A' }]
            }]
        ]);

        const { container } = render(
            <BrowserRouter><CreateMonitoria /></BrowserRouter>
        );

        await screen.findByText('Crear/Cargar Monitorías');

        const allSelects = () => container.querySelectorAll('select');
        const facultySelect = allSelects()[0];

        await waitFor(() => expect(facultySelect.children.length).toBeGreaterThan(2));

        expect(facultySelect).toHaveTextContent('Ciencias de la Salud');
        expect(facultySelect).toHaveTextContent('Ciencias Humanas');
        expect(facultySelect).toHaveTextContent('Ingeniería');

        await selectDropdownOption(facultySelect, 'Ciencias de la Salud');

        await waitFor(() => {
            expect(allSelects()[1].children.length).toBeGreaterThan(1);
        });

        const programSelect = allSelects()[1];
        expect(programSelect).toHaveTextContent('Medicina');
        expect(programSelect).toHaveTextContent('Enfermería');

        await selectDropdownOption(programSelect, 'Medicina');

        await waitFor(() => {
            expect(allSelects()[2].children.length).toBeGreaterThan(1);
        });

        const courseSelect = allSelects()[2];
        expect(courseSelect).toHaveTextContent('Fisiología - A');
    });

    test('[CreateMonitoria] Listado de monitorías - findMonitorias (GET /monitoring/getAllByProfessor/ → 200)', async () => {
        const mockMonitorias = [
            {
                id: 3,
                school: { name: 'Ingeniería' },
                program: { name: 'Ingeniería de Sistemas' },
                course: { name: 'Ingeniería de Software I' },
                semester: '2025-1',
                start: '2025-02-19T00:00:00.000+00:00',
                finish: '2025-03-27T00:00:00.000+00:00',
                estimatedHours: 64,
                hourlyRate: 15000
            },
            {
                id: 4,
                school: { name: 'Ciencias Humanas' },
                program: { name: 'Antropología' },
                course: { name: 'Mundos Posibles 2' },
                semester: '2025-1',
                start: '2025-02-21T00:00:00.000+00:00',
                finish: '2025-03-27T00:00:00.000+00:00',
                estimatedHours: 48,
                hourlyRate: 15000
            }
        ];

        mockFetch([
            ['/school/getSchools', {
                ok: true, json: async () => [{ name: 'Ingeniería' }]
            }],
            ['/monitoring/getAllByProfessor/', {
                ok: true, json: async () => mockMonitorias
            }],
            ['/program/getProgramsSchool', { ok: true, json: async () => [] }],
            ['/course/getCoursesProgram', { ok: true, json: async () => [] }]
        ]);

        render(<BrowserRouter><CreateMonitoria /></BrowserRouter>);

        await waitFor(() => {
            expect(screen.getByText('Ingeniería de Software I')).toBeInTheDocument();
        });

        expect(screen.getByText('Mundos Posibles 2')).toBeInTheDocument();
        expect(screen.getByText('Ingeniería de Sistemas')).toBeInTheDocument();
        expect(screen.getByText('Antropología')).toBeInTheDocument();

        expect(screen.getByText('Mis Monitorías (2)')).toBeInTheDocument();
    });
});
