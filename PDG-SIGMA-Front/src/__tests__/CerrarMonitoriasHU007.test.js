import React from 'react';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { BrowserRouter } from 'react-router-dom';
import CerrarMonitorias from '../CerrarMonitorias';

// Mock del backend
global.fetch = jest.fn();

// Mock del VerticalNavbar
jest.mock('../VerticalNavbar', () => {
    return function MockVerticalNavbar() {
        return <div data-testid="vertical-navbar">Vertical Navbar</div>;
    };
});

jest.mock('../PopUp', () => ({
    PopUp: ({ show, children, onClose }) => (show ? <div data-testid="popup">{children}<button onClick={onClose}>OK</button></div> : null)
}));

describe('CerrarMonitoriasHU007', () => {
    beforeEach(() => {
        jest.clearAllMocks();
        
        // Configurar localStorage con valores fijos
        Object.defineProperty(window, 'localStorage', {
            value: {
                getItem: jest.fn((key) => {
                    if (key === 'userId') return '5001';
                    if (key === 'token') return 'Bearer fake-token';
                    return null;
                }),
                setItem: jest.fn(),
                clear: jest.fn()
            },
            writable: true
        });
    });

    const mockMonitorings = [
        {
            id: 27,
            semester: '2026-1',
            course: { name: 'Programación Avanzada' },
            program: { name: 'Ingeniería de Sistemas' },
            professor: { name: 'Juan Pérez' },
            assignedMonitor: { name: 'Ana García' },
            estimatedHours: 40
        }
    ];

    const mockClosedMonitoring = {
        id: 27,
        semester: '2026-1',
        course: { name: 'Programación Avanzada' },
        program: { name: 'Ingeniería de Sistemas' },
        professor: { name: 'Juan Pérez' },
        assignedMonitor: { name: 'Ana García' },
        compliancePercentage: 85,
        closureDate: '2026-01-21T17:30:00'
    };

    test('Debe renderizar el componente correctamente', async () => {
        fetch.mockResolvedValueOnce({
            ok: true,
            json: async () => mockMonitorings
        });

        render(
            <BrowserRouter>
                <CerrarMonitorias />
            </BrowserRouter>
        );

        expect(screen.getByText('Cierre de Monitorías')).toBeInTheDocument();
        expect(screen.getByText('Cierre de monitorías al final del periodo')).toBeInTheDocument();
    });

    test('Debe cargar y mostrar monitorías listas para cerrar', async () => {
        fetch.mockResolvedValueOnce({
            ok: true,
            json: async () => mockMonitorings
        });

        render(
            <BrowserRouter>
                <CerrarMonitorias />
            </BrowserRouter>
        );

        await waitFor(() => {
            expect(screen.getByText('Programación Avanzada')).toBeInTheDocument();
        });

        expect(screen.getByText(/Ingeniería de Sistemas/)).toBeInTheDocument();
        expect(screen.getByText(/Juan Pérez/)).toBeInTheDocument();
        expect(screen.getByText(/Ana García/)).toBeInTheDocument();
    });

    test('Debe mostrar mensaje cuando no hay monitorías para cerrar', async () => {
        fetch.mockResolvedValueOnce({
            ok: true,
            json: async () => []
        });

        render(
            <BrowserRouter>
                <CerrarMonitorias />
            </BrowserRouter>
        );

        await waitFor(() => {
            expect(screen.getByText(/No hay monitorías listas para cerrar/)).toBeInTheDocument();
        });
    });

    test('Debe permitir seleccionar monitorías', async () => {
        fetch.mockResolvedValueOnce({
            ok: true,
            json: async () => mockMonitorings
        });

        render(
            <BrowserRouter>
                <CerrarMonitorias />
            </BrowserRouter>
        );

        await waitFor(() => {
            expect(screen.getByText('Programación Avanzada')).toBeInTheDocument();
        });

        const checkbox = screen.getAllByRole('checkbox')[1]; // El segundo checkbox (el primero es "Seleccionar todas")
        fireEvent.click(checkbox);

        await waitFor(() => {
            expect(screen.getByText(/Cerrar Seleccionadas \(1\)/)).toBeInTheDocument();
        });
    });

    test('Debe abrir modal de cierre al hacer clic en "Cerrar Seleccionadas"', async () => {
        fetch.mockResolvedValueOnce({
            ok: true,
            json: async () => mockMonitorings
        });

        render(
            <BrowserRouter>
                <CerrarMonitorias />
            </BrowserRouter>
        );

        await waitFor(() => {
            expect(screen.getByText('Programación Avanzada')).toBeInTheDocument();
        });

        // Seleccionar monitoría
        const checkbox = screen.getAllByRole('checkbox')[1]; // El segundo checkbox (el primero es "Seleccionar todas")
        fireEvent.click(checkbox);

        // Abrir modal
        const closeButton = screen.getByText(/Cerrar Seleccionadas/);
        fireEvent.click(closeButton);

        await waitFor(() => {
            expect(screen.getByText(/Va a cerrar/)).toBeInTheDocument();
            expect(screen.getByPlaceholderText(/Ingrese un comentario/)).toBeInTheDocument();
        });
    });

    test('Debe cerrar monitoría exitosamente', async () => {
        // Mock inicial para cargar monitorías
        fetch.mockResolvedValueOnce({
            ok: true,
            json: async () => mockMonitorings
        });

        render(
            <BrowserRouter>
                <CerrarMonitorias />
            </BrowserRouter>
        );

        await waitFor(() => {
            expect(screen.getByText('Programación Avanzada')).toBeInTheDocument();
        });

        // Seleccionar monitoría (el segundo checkbox, el primero es "Seleccionar todas")
        const checkbox = screen.getAllByRole('checkbox')[1];
        fireEvent.click(checkbox);

        // Abrir modal
        const openModalButton = screen.getByText(/Cerrar Seleccionadas/);
        fireEvent.click(openModalButton);

        await waitFor(() => {
            expect(screen.getByPlaceholderText(/Ingrese un comentario/)).toBeInTheDocument();
        });

        // Ingresar comentario
        const commentInput = screen.getByPlaceholderText(/Ingrese un comentario/);
        fireEvent.change(commentInput, { target: { value: 'Cierre de prueba' } });

        // Mock para cerrar monitoría
        fetch.mockResolvedValueOnce({
            ok: true,
            json: async () => ({
                monitoringId: 27,
                compliancePercentage: 85
            })
        });

        // Mock para recargar lista
        fetch.mockResolvedValueOnce({
            ok: true,
            json: async () => []
        });

        // Confirmar cierre (solo hay un botón con este texto exacto: el del modal)
        const confirmButton = screen.getByRole('button', { name: /Cerrar Monitorías/ });
        fireEvent.click(confirmButton);

        await waitFor(() => {
            expect(fetch).toHaveBeenCalledWith(
                expect.stringContaining('/monitoring-closure/27/close?directorId=5001'),
                expect.objectContaining({
                    method: 'POST',
                    body: expect.stringContaining('Cierre de prueba')
                })
            );
        });
    });

    test('Debe cambiar a tab de monitorías cerradas', async () => {
        fetch.mockResolvedValueOnce({
            ok: true,
            json: async () => mockMonitorings
        });

        render(
            <BrowserRouter>
                <CerrarMonitorias />
            </BrowserRouter>
        );

        // Cambiar a tab cerradas
        fetch.mockResolvedValueOnce({
            ok: true,
            json: async () => [mockClosedMonitoring]
        });

        const closedTab = screen.getByText(/Monitorías Cerradas/);
        fireEvent.click(closedTab);

        await waitFor(() => {
            expect(screen.getByText('Ver Reporte')).toBeInTheDocument();
        });
    });

    test('Debe mostrar error si el cierre falla', async () => {
        fetch.mockResolvedValueOnce({
            ok: true,
            json: async () => mockMonitorings
        });

        render(
            <BrowserRouter>
                <CerrarMonitorias />
            </BrowserRouter>
        );

        await waitFor(() => {
            expect(screen.getByText('Programación Avanzada')).toBeInTheDocument();
        });

        // Seleccionar y abrir modal
        const checkbox = screen.getAllByRole('checkbox')[1]; // El segundo checkbox (el primero es "Seleccionar todas")
        fireEvent.click(checkbox);
        
        const openModalButton = screen.getByText(/Cerrar Seleccionadas/);
        fireEvent.click(openModalButton);

        await waitFor(() => {
            expect(screen.getByPlaceholderText(/Ingrese un comentario/)).toBeInTheDocument();
        });

        const commentInput = screen.getByPlaceholderText(/Ingrese un comentario/);
        fireEvent.change(commentInput, { target: { value: 'Cierre de prueba' } });

        // Mock error
        fetch.mockResolvedValueOnce({
            ok: false,
            text: async () => 'Error al cerrar'
        });

        const confirmButton = screen.getByRole('button', { name: /Cerrar Monitorías/ });
        fireEvent.click(confirmButton);

        await waitFor(() => {
            expect(screen.getByText(/Error al cerrar monitorías/)).toBeInTheDocument();
        });
    });

    test('Debe validar que se ingrese un comentario', async () => {
        fetch.mockResolvedValueOnce({
            ok: true,
            json: async () => mockMonitorings
        });

        render(
            <BrowserRouter>
                <CerrarMonitorias />
            </BrowserRouter>
        );

        await waitFor(() => {
            expect(screen.getByText('Programación Avanzada')).toBeInTheDocument();
        });

        // Seleccionar y abrir modal
        const checkbox = screen.getAllByRole('checkbox')[1]; // El segundo checkbox (el primero es "Seleccionar todas")
        fireEvent.click(checkbox);
        
        const openModalButton = screen.getByText(/Cerrar Seleccionadas/);
        fireEvent.click(openModalButton);

        await waitFor(() => {
            expect(screen.getByPlaceholderText(/Ingrese un comentario/)).toBeInTheDocument();
        });

        // Intentar cerrar sin comentario
        const confirmButton = screen.getByRole('button', { name: /Cerrar Monitorías/ });
        fireEvent.click(confirmButton);

        await waitFor(() => {
            expect(screen.getByText(/Debe ingresar un comentario/)).toBeInTheDocument();
        });
    });

    test('Debe mostrar error al cargar monitorías', async () => {
        fetch.mockResolvedValueOnce({ ok: false });

        render(
            <BrowserRouter>
                <CerrarMonitorias />
            </BrowserRouter>
        );

        await waitFor(() => {
            expect(screen.getByText(/Error al cargar las monitorías/)).toBeInTheDocument();
        });
    });

    test('Debe permitir seleccionar todas y desseleccionar', async () => {
        const multipleMonitorings = [
            { id: 1, semester: '2026-1', course: { name: 'Curso A' }, program: { name: 'Prog A' }, professor: { name: 'Prof A' }, assignedMonitor: { name: 'Mon A' }, estimatedHours: 40 },
            { id: 2, semester: '2026-1', course: { name: 'Curso B' }, program: { name: 'Prog B' }, professor: { name: 'Prof B' }, assignedMonitor: { name: 'Mon B' }, estimatedHours: 30 }
        ];

        fetch.mockResolvedValueOnce({ ok: true, json: async () => multipleMonitorings });

        render(<BrowserRouter><CerrarMonitorias /></BrowserRouter>);

        await screen.findByText('Curso A');

        fireEvent.click(screen.getByText(/Seleccionar todas/i));
        await waitFor(() => expect(screen.getByText(/Cerrar Seleccionadas \(2\)/)).toBeInTheDocument());

        fireEvent.click(screen.getByText(/Seleccionar todas/i));
        await waitFor(() => expect(screen.getByText(/Cerrar Seleccionadas \(0\)/)).toBeInTheDocument());
    });



    test('Debe cerrar dos monitorías en lote', async () => {
        const batchMonitorings = [
            { id: 1, semester: '2026-1', course: { name: 'Curso A' }, program: { name: 'Prog A' }, professor: { name: 'Prof A' }, assignedMonitor: { name: 'Mon A' }, estimatedHours: 40 },
            { id: 2, semester: '2026-1', course: { name: 'Curso B' }, program: { name: 'Prog B' }, professor: { name: 'Prof B' }, assignedMonitor: { name: 'Mon B' }, estimatedHours: 30 }
        ];

        fetch.mockResolvedValueOnce({ ok: true, json: async () => batchMonitorings });

        render(<BrowserRouter><CerrarMonitorias /></BrowserRouter>);

        await screen.findByText('Curso A');

        const checkboxes = screen.getAllByRole('checkbox');
        fireEvent.click(checkboxes[1]);
        fireEvent.click(checkboxes[2]);

        await waitFor(() => expect(screen.getByText(/Cerrar Seleccionadas \(2\)/)).toBeInTheDocument());

        fireEvent.click(screen.getByText(/Cerrar Seleccionadas/));
        await screen.findByPlaceholderText(/Ingrese un comentario/);

        fireEvent.change(screen.getByPlaceholderText(/Ingrese un comentario/), { target: { value: 'Cierre lote' } });

        fetch.mockResolvedValueOnce({ ok: true, json: async () => ({}) });
        fetch.mockResolvedValueOnce({ ok: true, json: async () => [] });

        fireEvent.click(screen.getByRole('button', { name: /Cerrar Monitorías/ }));

        await waitFor(() => {
            expect(fetch).toHaveBeenCalledWith(
                expect.stringContaining('/monitoring-closure/close-batch'),
                expect.objectContaining({ method: 'POST' })
            );
        });
    });

    test('Debe ver reporte de monitoría cerrada con cumplimiento bajo', async () => {
        const lowComplianceClosed = {
            id: 27, semester: '2026-1', course: { name: 'Programación Avanzada' }, program: { name: 'Ingeniería de Sistemas' },
            professor: { name: 'Juan Pérez' }, assignedMonitor: { name: 'Ana García' },
            compliancePercentage: 50, closureDate: '2026-01-21T17:30:00'
        };

        fetch.mockResolvedValueOnce({ ok: true, json: async () => [] }); // pendientes vacías
        fetch.mockResolvedValueOnce({ ok: true, json: async () => [lowComplianceClosed] });

        render(<BrowserRouter><CerrarMonitorias /></BrowserRouter>);

        fireEvent.click(screen.getByText(/Monitorías Cerradas/));
        await screen.findByText('Ver Reporte');

        fetch.mockResolvedValueOnce({ ok: true, json: async () => ({
            courseName: 'Programación Avanzada', programName: 'Ingeniería de Sistemas',
            professorName: 'Juan Pérez', monitorName: 'Ana García',
            semester: '2026-1', compliancePercentage: 50,
            completedActivities: 3, totalActivities: 6,
            actualHours: 20, estimatedHours: 40,
            startDate: '2026-01-15', finishDate: '2026-06-15',
            closureDate: '2026-01-21', closedBy: 'Admin',
            closureComment: 'Cierre por bajo rendimiento'
        }) });

        fireEvent.click(screen.getByText('Ver Reporte'));

        expect(await screen.findByText('Reporte de Cumplimiento')).toBeInTheDocument();
        expect(screen.getAllByText('50%').length).toBeGreaterThanOrEqual(1);
        expect(screen.getByText('3 / 6')).toBeInTheDocument();
        expect(screen.getByText('20h / 40h')).toBeInTheDocument();
    });

    test('Debe abrir modal de cierre, desactivar autoCalculate y mostrar texto de métricas manuales', async () => {
        fetch.mockResolvedValueOnce({
            ok: true,
            json: async () => mockMonitorings
        });

        render(
            <BrowserRouter>
                <CerrarMonitorias />
            </BrowserRouter>
        );

        await screen.findByText('Programación Avanzada');

        const checkbox = screen.getAllByRole('checkbox')[1];
        fireEvent.click(checkbox);

        fireEvent.click(screen.getByText(/Cerrar Seleccionadas/));

        await screen.findByPlaceholderText(/Ingrese un comentario/);

        const autoCalcCheckbox = screen.getByLabelText(/Calcular automáticamente/i);
        expect(screen.getByText(/El sistema calculará automáticamente/i)).toBeInTheDocument();

        fireEvent.click(autoCalcCheckbox);

        expect(screen.getByText(/Deberá ingresar manualmente/i)).toBeInTheDocument();

        fireEvent.click(screen.getByText('×'));

        await waitFor(() => {
            expect(screen.queryByPlaceholderText(/Ingrese un comentario/)).not.toBeInTheDocument();
        });
    });

    test('Debe ver reporte con fechas nulas, presupuesto y tarifa nula, cumplimiento ámbar', async () => {
        const closedWithNullDates = {
            id: 27, semester: '2026-1', course: { name: 'Programación Avanzada' }, program: { name: 'Ingeniería de Sistemas' },
            professor: { name: 'Juan Pérez' }, assignedMonitor: { name: 'Ana García' },
            compliancePercentage: 75, closureDate: '2026-01-21T17:30:00'
        };

        fetch.mockResolvedValueOnce({ ok: true, json: async () => [] });
        fetch.mockResolvedValueOnce({ ok: true, json: async () => [closedWithNullDates] });

        render(
            <BrowserRouter>
                <CerrarMonitorias />
            </BrowserRouter>
        );

        fireEvent.click(screen.getByText(/Monitorías Cerradas/));
        await screen.findByText('Ver Reporte');

        fetch.mockResolvedValueOnce({ ok: true, json: async () => ({
            courseName: 'Programación Avanzada', programName: 'Ingeniería de Sistemas',
            professorName: 'Juan Pérez', monitorName: 'Ana García',
            semester: '2026-1', compliancePercentage: 75,
            completedActivities: 3, totalActivities: 6,
            actualHours: 20, estimatedHours: 40,
            startDate: null, finishDate: null,
            closureDate: null, closedBy: 'Admin',
            closureComment: 'Cierre de prueba',
            totalBudgetUsed: 5000, hourlyRate: null
        }) });

        fireEvent.click(screen.getByText('Ver Reporte'));

        expect(await screen.findByText('Reporte de Cumplimiento')).toBeInTheDocument();
        expect(screen.getByText(/Monto total usado:/)).toBeInTheDocument();
        expect(screen.getAllByText('N/A').length).toBeGreaterThanOrEqual(3);
        expect(screen.getByText('Cumplimiento General')).toBeInTheDocument();
    });

    test('Debe mostrar error al hacer clic en Cerrar sin seleccionar monitorias', async () => {
        fetch.mockResolvedValueOnce({
            ok: true,
            json: async () => mockMonitorings
        });

        render(
            <BrowserRouter>
                <CerrarMonitorias />
            </BrowserRouter>
        );

        await screen.findByText('Programación Avanzada');

        const cerrarButton = screen.getByText(/Cerrar Seleccionadas/i);
        await waitFor(() => {
            expect(cerrarButton).toBeDisabled();
        });
    });

    test('Debe mostrar error al cargar reporte fallido', async () => {
        fetch.mockResolvedValueOnce({ ok: true, json: async () => [] });
        fetch.mockResolvedValueOnce({ ok: true, json: async () => [mockClosedMonitoring] });

        render(
            <BrowserRouter>
                <CerrarMonitorias />
            </BrowserRouter>
        );

        fireEvent.click(screen.getByText(/Monitorías Cerradas/));
        await screen.findByText('Ver Reporte');

        fetch.mockResolvedValueOnce({
            ok: false,
            status: 500,
            json: async () => ({ error: 'Server error' })
        });

        fireEvent.click(screen.getByText('Ver Reporte'));

        await waitFor(() => {
            expect(screen.getByTestId('popup')).toBeInTheDocument();
        });
    });

    test('Debe cambiar de periodo usando el select', async () => {
        fetch.mockResolvedValueOnce({
            ok: true,
            json: async () => mockMonitorings
        });

        render(
            <BrowserRouter>
                <CerrarMonitorias />
            </BrowserRouter>
        );

        await screen.findByText('Programación Avanzada');
        const semesterSelect = screen.getByRole('combobox');
        fireEvent.change(semesterSelect, { target: { value: '2025-2' } });
    });

    test('Debe hacer clic en tab Listas para Cerrar', async () => {
        fetch.mockResolvedValueOnce({
            ok: true,
            json: async () => mockMonitorings
        });

        render(
            <BrowserRouter>
                <CerrarMonitorias />
            </BrowserRouter>
        );

        await screen.findByText('Programación Avanzada');
        fireEvent.click(screen.getByText(/Listas para Cerrar/));
    });
});

