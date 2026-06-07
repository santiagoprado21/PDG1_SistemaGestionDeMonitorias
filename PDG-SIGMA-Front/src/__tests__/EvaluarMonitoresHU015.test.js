import React from 'react';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import EvaluarMonitoresHU015 from '../EvaluarMonitoresHU015';

jest.mock('../VerticalNavbar', () => () => <nav data-testid="navbar" />);
jest.mock('../LoadingSpinner', () => ({ message }) => <div data-testid="spinner">{message}</div>);
jest.mock('../PopUp', () => ({
  PopUp: ({ show, children }) => (show ? <div data-testid="popup">{children}</div> : null)
}));
jest.mock('../config/ApiBackend', () => ({ BACKEND_URL: 'http://localhost:8080' }));

describe('EvaluarMonitoresHU015', () => {
  beforeEach(() => {
    global.fetch = jest.fn().mockResolvedValue({
      ok: true,
      json: () => Promise.resolve([
        {
          monitoringId: 1,
          monitorCode: 'MON-001',
          monitorFullName: 'María López Martínez',
          monitoringName: 'Desarrollo Web',
          courseName: 'Desarrollo Web',
          semester: '2025-2',
          evaluated: false,
          performanceLevel: 'ADECUADO',
          totalScore: 3,
          visibleToMonitor: true
        },
        {
          monitoringId: 2,
          monitorCode: 'MON-002',
          monitorFullName: 'Juan Pérez',
          monitoringName: 'Algoritmos',
          courseName: 'Algoritmos',
          semester: '2025-1',
          evaluated: true,
          performanceLevel: 'EXCELENTE',
          totalScore: 4.5,
          visibleToMonitor: true
        }
      ])
    });
    window.localStorage.setItem('userId', 'PROF-1');
    window.localStorage.setItem('token', 'Bearer token');
  });

  afterEach(() => {
    jest.resetAllMocks();
    window.localStorage.clear();
  });

  it('permite seleccionar una asignación y muestra formulario de evaluación', async () => {
  render(<EvaluarMonitoresHU015 />);

    await waitFor(() => expect(global.fetch).toHaveBeenCalled());
    await screen.findByText('María López Martínez');

  const assignmentButton = screen.getByRole('button', { name: /maría lópez martínez/i });
  await userEvent.click(assignmentButton);

    expect(screen.getAllByText('María López Martínez')[0]).toBeInTheDocument();
    expect(screen.getAllByText(/Desarrollo Web/i).length).toBeGreaterThan(0);

    expect(screen.getAllByRole('radiogroup').length).toBeGreaterThan(0);
    expect(screen.getByRole('button', { name: /Guardar evaluación/i })).toBeInTheDocument();
  });

  it('muestra estado vacio cuando no hay asignaciones', async () => {
    global.fetch = jest.fn().mockResolvedValue({
      ok: true,
      json: () => Promise.resolve([])
    });
    render(<EvaluarMonitoresHU015 />);

    await waitFor(() => expect(global.fetch).toHaveBeenCalled());
    expect(await screen.findByText(/No hay monitorías pendientes de evaluación/i)).toBeInTheDocument();
    expect(screen.getByText(/Aún no has registrado evaluaciones/i)).toBeInTheDocument();
  });

  it('muestra error cuando falta professorId', async () => {
    window.localStorage.removeItem('userId');
    global.fetch = jest.fn().mockResolvedValue({ ok: true, json: () => Promise.resolve([]) });
    render(<EvaluarMonitoresHU015 />);

    expect(await screen.findByText(/No se pudo identificar al profesor autenticado/i)).toBeInTheDocument();
  });

  it('maneja error en la carga de asignaciones', async () => {
    global.fetch = jest.fn().mockRejectedValue(new Error('Error de red'));
    render(<EvaluarMonitoresHU015 />);

    expect(await screen.findByText(/Error de red/i)).toBeInTheDocument();
  });

  it('envia evaluacion POST correctamente', async () => {
    let fetchCallCount = 0;
    global.fetch = jest.fn().mockImplementation(async (url, options) => {
      fetchCallCount++;
      if (fetchCallCount === 1) {
        return {
          ok: true,
          json: () => Promise.resolve([
            {
              monitoringId: 1,
              monitorCode: 'MON-001',
              monitorFullName: 'María López Martínez',
              monitoringName: 'Desarrollo Web',
              courseName: 'Desarrollo Web',
              semester: '2025-2',
              evaluated: false
            }
          ])
        };
      }
      if (options?.method === 'POST') {
        return { ok: true, json: () => Promise.resolve({}) };
      }
      return { ok: true, json: () => Promise.resolve([]) };
    });

    render(<EvaluarMonitoresHU015 />);
    await screen.findByText('María López Martínez');
    await userEvent.click(screen.getByRole('button', { name: /maría lópez martínez/i }));
    await userEvent.click(screen.getByRole('button', { name: /Guardar evaluación/i }));

    await waitFor(() => {
      expect(global.fetch).toHaveBeenCalledWith(
        'http://localhost:8080/monitor-evaluations',
        expect.objectContaining({ method: 'POST' })
      );
    });
    expect(await screen.findByText(/La evaluación se guardó correctamente/i)).toBeInTheDocument();
  });

  it('cambia puntuacion al hacer clic en un score chip', async () => {
    global.fetch = jest.fn().mockResolvedValue({
      ok: true,
      json: () => Promise.resolve([
        {
          monitoringId: 1,
          monitorCode: 'MON-001',
          monitorFullName: 'María López Martínez',
          monitoringName: 'Desarrollo Web',
          courseName: 'Desarrollo Web',
          semester: '2025-2',
          evaluated: false
        }
      ])
    });
    render(<EvaluarMonitoresHU015 />);
    await screen.findByText('María López Martínez');
    await userEvent.click(screen.getByRole('button', { name: /maría lópez martínez/i }));

    const scoreChips = screen.getAllByRole('radio');
    await userEvent.click(scoreChips[4]);
    await waitFor(() => {
      expect(scoreChips[4]).toHaveClass('is-selected');
    });
  });

  it('maneja error HTTP en el envio', async () => {
    let fetchCallCount = 0;
    global.fetch = jest.fn().mockImplementation(async (url, options) => {
      fetchCallCount++;
      if (fetchCallCount === 1) {
        return {
          ok: true,
          json: () => Promise.resolve([
            {
              monitoringId: 1,
              monitorCode: 'MON-001',
              monitorFullName: 'María López Martínez',
              monitoringName: 'Desarrollo Web',
              courseName: 'Desarrollo Web',
              semester: '2025-2',
              evaluated: false
            }
          ])
        };
      }
      if (options?.method === 'POST') {
        return { ok: false, json: () => Promise.resolve({ error: 'Error del servidor' }) };
      }
      return { ok: true, json: () => Promise.resolve([]) };
    });

    render(<EvaluarMonitoresHU015 />);
    await screen.findByText('María López Martínez');
    await userEvent.click(screen.getByRole('button', { name: /maría lópez martínez/i }));
    await userEvent.click(screen.getByRole('button', { name: /Guardar evaluación/i }));

    expect(await screen.findByText(/Error del servidor/i)).toBeInTheDocument();
  });
});
