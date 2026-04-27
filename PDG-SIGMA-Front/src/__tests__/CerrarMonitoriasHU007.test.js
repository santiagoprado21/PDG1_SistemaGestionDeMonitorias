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
});

