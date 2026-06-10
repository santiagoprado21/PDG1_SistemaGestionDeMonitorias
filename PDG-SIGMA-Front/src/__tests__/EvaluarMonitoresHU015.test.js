import React from 'react';
import { render, screen, waitFor, fireEvent } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import EvaluarMonitoresHU015 from '../EvaluarMonitoresHU015';

jest.mock('../VerticalNavbar', () => () => <nav data-testid="navbar" />);
jest.mock('../LoadingSpinner', () => ({ message }) => <div data-testid="spinner">{message}</div>);
jest.mock('../PopUp', () => ({
  PopUp: ({ show, onClose, children }) => (show ? <div data-testid="popup">{children}<button onClick={onClose}>Cerrar</button></div> : null)
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

  it('filtra asignaciones al escribir en el buscador', async () => {
    render(<EvaluarMonitoresHU015 />);
    await screen.findByText('María López Martínez');
    const searchInput = screen.getByPlaceholderText(/buscar por estudiante, curso o monitoría/i);
    await userEvent.type(searchInput, 'María');
    await waitFor(() => {
      expect(global.fetch).toHaveBeenCalledWith(
        expect.stringContaining('search=Mar%C3%ADa'),
        expect.anything()
      );
    }, { timeout: 2000 });
  });

  it('navega puntuaciones con teclado', async () => {
    render(<EvaluarMonitoresHU015 />);
    await screen.findByText('María López Martínez');
    await userEvent.click(screen.getByRole('button', { name: /maría lópez martínez/i }));
    const allChips = screen.getAllByRole('radio');
    const taskChips = allChips.slice(0, 5);
    fireEvent.keyDown(taskChips[2], { key: 'ArrowRight' });
    await waitFor(() => expect(taskChips[3]).toHaveClass('is-selected'));
    fireEvent.keyDown(taskChips[3], { key: 'ArrowLeft' });
    await waitFor(() => expect(taskChips[2]).toHaveClass('is-selected'));
    fireEvent.keyDown(taskChips[2], { key: 'Home' });
    await waitFor(() => expect(taskChips[0]).toHaveClass('is-selected'));
    fireEvent.keyDown(taskChips[0], { key: 'End' });
    await waitFor(() => expect(taskChips[4]).toHaveClass('is-selected'));

    const commChips = screen.getAllByRole('radio').slice(5, 10);
    fireEvent.keyDown(commChips[2], { key: 'ArrowRight' });
    await waitFor(() => expect(commChips[3]).toHaveClass('is-selected'));

    const planChips = screen.getAllByRole('radio').slice(10, 15);
    fireEvent.keyDown(planChips[2], { key: 'ArrowRight' });
    await waitFor(() => expect(planChips[3]).toHaveClass('is-selected'));

    const attChips = screen.getAllByRole('radio').slice(15, 20);
    fireEvent.keyDown(attChips[2], { key: 'ArrowRight' });
    await waitFor(() => expect(attChips[3]).toHaveClass('is-selected'));
  });

  it('selecciona una evaluacion existente', async () => {
    render(<EvaluarMonitoresHU015 />);
    await screen.findByText('Juan Pérez');
    await userEvent.click(screen.getByRole('button', { name: /juan pérez/i }));
    expect(screen.getByText(/actualizar evaluación/i)).toBeInTheDocument();
    expect(screen.getByText(/4.50/)).toBeInTheDocument();
  });

  it('bloquea edicion para evaluaciones con mas de un año', async () => {
    const oldDate = new Date('2020-06-01');
    global.fetch = jest.fn().mockResolvedValue({
      ok: true,
      json: () => Promise.resolve([
        {
          monitoringId: 10,
          monitorCode: 'MON-010',
          monitorFullName: 'Ana García',
          monitoringName: 'Física',
          courseName: 'Física',
          semester: '2024-1',
          evaluated: true,
          evaluatedAt: oldDate.toISOString(),
          performanceLevel: 'ADECUADO',
          totalScore: 3.5,
          visibleToMonitor: true
        }
      ])
    });
    render(<EvaluarMonitoresHU015 />);
    await screen.findByText('Ana García');
    await userEvent.click(screen.getByRole('button', { name: /ana garcía/i }));
    expect(await screen.findByText(/no puede modificarse/i)).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /actualizar evaluación/i })).toBeDisabled();

    fireEvent.keyDown(screen.getAllByRole('radio')[0], { key: 'ArrowRight' });
    fireEvent.submit(screen.getByRole('button', { name: /actualizar evaluación/i }).closest('form'));
    expect(screen.getAllByText(/no puede modificarse/i).length).toBeGreaterThanOrEqual(2);
  });

  it('muestra rendimiento EN_RIESGO y alerta de penalización', async () => {
    render(<EvaluarMonitoresHU015 />);
    await screen.findByText('María López Martínez');
    await userEvent.click(screen.getByRole('button', { name: /maría lópez martínez/i }));
    const getChips = () => screen.getAllByRole('radio');
    await userEvent.click(getChips()[0]);
    await userEvent.click(getChips()[5]);
    await userEvent.click(getChips()[10]);
    await userEvent.click(getChips()[15]);
    await waitFor(() => {
      expect(screen.getByText(/en riesgo/i)).toBeInTheDocument();
    });
    expect(screen.getByText(/puntaje por debajo de 3\.0/i)).toBeInTheDocument();
    await userEvent.type(
      screen.getByPlaceholderText(/comparte retroalimentación/i),
      'Necesita mejorar.'
    );
    expect(screen.getByPlaceholderText(/comparte retroalimentación/i)).toHaveValue('Necesita mejorar.');
  });

  it('maneja error HTTP en la carga de asignaciones', async () => {
    global.fetch = jest.fn().mockResolvedValue({
      ok: false,
      status: 500,
      json: () => Promise.resolve({ error: 'Error interno del servidor' })
    });
    render(<EvaluarMonitoresHU015 />);
    expect(await screen.findByText(/Error interno del servidor/i)).toBeInTheDocument();
  });

  it('cierra el popup al hacer clic en cerrar', async () => {
    global.fetch = jest.fn().mockRejectedValue(new Error('Error de prueba'));
    render(<EvaluarMonitoresHU015 />);
    expect(await screen.findByText(/Error de prueba/i)).toBeInTheDocument();
    expect(screen.getByTestId('popup')).toBeInTheDocument();
    await userEvent.click(screen.getByText('Cerrar'));
    await waitFor(() => {
      expect(screen.queryByTestId('popup')).not.toBeInTheDocument();
    });
  });

  it('muestra asignacion sin nombre de monitoría', async () => {
    global.fetch = jest.fn().mockResolvedValue({
      ok: true,
      json: () => Promise.resolve([
        {
          monitoringId: 20,
          monitorCode: 'MON-020',
          monitorFullName: 'Luis Torres',
          monitoringName: null,
          courseName: 'Cálculo',
          semester: '2025-2',
          evaluated: false
        }
      ])
    });
    render(<EvaluarMonitoresHU015 />);
    expect(await screen.findByText(/Monitoría sin nombre/i)).toBeInTheDocument();
  });

  it('muestra asignacion sin curso ni semestre', async () => {
    global.fetch = jest.fn().mockResolvedValue({
      ok: true,
      json: () => Promise.resolve([
        {
          monitoringId: 30,
          monitorCode: 'MON-030',
          monitorFullName: 'Sofía Martínez',
          monitoringName: 'Matemáticas',
          courseName: null,
          semester: null,
          evaluated: false
        }
      ])
    });
    render(<EvaluarMonitoresHU015 />);
    await screen.findByText('Sofía Martínez');
  });

  it('maneja fecha de evaluacion invalida', async () => {
    global.fetch = jest.fn().mockResolvedValue({
      ok: true,
      json: () => Promise.resolve([
        {
          monitoringId: 40,
          monitorCode: 'MON-040',
          monitorFullName: 'Test Invalid Date',
          monitoringName: 'Test',
          courseName: 'Test',
          semester: '2024-1',
          evaluated: true,
          evaluatedAt: 'not-a-valid-date',
          performanceLevel: 'ADECUADO',
          totalScore: 3,
          visibleToMonitor: true
        }
      ])
    });
    render(<EvaluarMonitoresHU015 />);
    await screen.findByText('Test Invalid Date');
    await userEvent.click(screen.getByRole('button', { name: /test invalid date/i }));
    expect(screen.getByRole('button', { name: /actualizar evaluación/i })).not.toBeDisabled();
  });

  it('permite cambiar visibilidad de la evaluacion', async () => {
    render(<EvaluarMonitoresHU015 />);
    await screen.findByText('María López Martínez');
    await userEvent.click(screen.getByRole('button', { name: /maría lópez martínez/i }));
    const checkbox = screen.getByRole('checkbox');
    expect(checkbox).toBeChecked();
    await userEvent.click(checkbox);
    expect(checkbox).not.toBeChecked();
  });
});
