import React from 'react';
import { render, screen, waitFor, fireEvent } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import EvaluarSupervisorHU021 from '../EvaluarSupervisorHU021';

jest.mock('../VerticalNavbar', () => () => <nav data-testid="navbar" />);
jest.mock('../LoadingSpinner', () => ({ message }) => <div data-testid="spinner">{message}</div>);
jest.mock('../PopUp', () => ({
  PopUp: ({ show, onClose, children }) => (
    show ? (
      <div data-testid="popup">
        <button data-testid="popup-close" onClick={onClose}>×</button>
        {children}
      </div>
    ) : null
  )
}));
jest.mock('../config/ApiBackend', () => ({ BACKEND_URL: 'http://localhost:8080' }));

const pendingAssignment = {
  monitoringId: 10,
  monitoringName: 'Algoritmos - 2025-1',
  courseName: 'Algoritmos',
  semester: '2025-1',
  professorName: 'Profesor Uno',
  evaluated: false,
  status: 'PENDIENTE'
};

const activeQuestionsPayload = {
  questions: [
    {
      id: 201,
      statement: 'El profesor brindó orientación clara',
      displayOrder: 1
    },
    {
      id: 202,
      statement: 'El profesor fomentó un ambiente de confianza',
      displayOrder: 2
    }
  ]
};

describe('EvaluarSupervisorHU021', () => {
  beforeEach(() => {
    window.localStorage.setItem('userId', 'MON-10');
    window.localStorage.setItem('token', 'Bearer token');
  });

  afterEach(() => {
    jest.resetAllMocks();
    window.localStorage.clear();
  });

  it('permite enviar una evaluacion con preguntas y escala Likert 1-7', async () => {
    global.fetch = jest.fn()
      .mockResolvedValueOnce({
        ok: true,
        json: () => Promise.resolve([pendingAssignment])
      })
      .mockResolvedValueOnce({
        ok: true,
        json: () => Promise.resolve(activeQuestionsPayload)
      })
      .mockResolvedValueOnce({
        ok: true,
        json: () => Promise.resolve({
          evaluationId: 55,
          performanceLevel: 'EXCELENTE'
        })
      })
      .mockResolvedValueOnce({
        ok: true,
        json: () => Promise.resolve([{ ...pendingAssignment, evaluated: true, status: 'ENVIADA' }])
      });

    render(<EvaluarSupervisorHU021 />);

    await waitFor(() => expect(global.fetch).toHaveBeenCalled());
    const assignmentButton = await screen.findByRole('button', { name: /profesor uno/i });
    await userEvent.click(assignmentButton);

    await waitFor(() => {
      expect(screen.getAllByRole('radiogroup').length).toBeGreaterThan(0);
    });

    expect(screen.getByText('Adecuado')).toBeInTheDocument();
    expect(screen.getByText('4.00')).toBeInTheDocument();

    const topScoreButtons = screen.getAllByRole('radio', { name: '7' });
    await userEvent.click(topScoreButtons[0]);
    await userEvent.click(topScoreButtons[1]);

    expect(screen.getByText('Excelente')).toBeInTheDocument();
    expect(screen.getByText('7.00')).toBeInTheDocument();

    const submitButton = screen.getByRole('button', { name: /Enviar evaluación/i });
    await userEvent.click(submitButton);

    await waitFor(() => expect(global.fetch).toHaveBeenCalledTimes(4));

    const postCall = global.fetch.mock.calls[2];
    const [, postConfig] = postCall;
    const payload = JSON.parse(postConfig.body);

    expect(payload.monitorIdentifier).toBe('MON-10');
    expect(payload.answers).toHaveLength(2);
    expect(payload.answers[0].questionId).toBe(201);
    expect(payload.answers[1].questionId).toBe(202);
    expect(Number(payload.answers[0].score)).toBeGreaterThanOrEqual(1);
    expect(Number(payload.answers[1].score)).toBeGreaterThanOrEqual(1);
  });

  it('valida el flujo obligatorio cuando no hay preguntas activas', async () => {
    global.fetch = jest.fn()
      .mockResolvedValueOnce({
        ok: true,
        json: () => Promise.resolve([pendingAssignment])
      })
      .mockResolvedValueOnce({
        ok: true,
        json: () => Promise.resolve({ questions: [] })
      });

    render(<EvaluarSupervisorHU021 />);

    const assignmentButton = await screen.findByRole('button', { name: /profesor uno/i });
    await userEvent.click(assignmentButton);

    expect(await screen.findByTestId('popup')).toHaveTextContent(/No hay preguntas activas configuradas/i);
    expect(screen.getByText(/No hay preguntas configuradas para este periodo/i)).toBeInTheDocument();

    const submitButton = screen.getByRole('button', { name: /Enviar evaluación/i });
    expect(submitButton).toBeDisabled();
    expect(global.fetch).toHaveBeenCalledTimes(2);
  });

  it('muestra mensaje cuando no hay monitor autenticado', async () => {
    window.localStorage.clear();

    render(<EvaluarSupervisorHU021 />);

    expect(await screen.findByTestId('popup')).toHaveTextContent(/No se pudo identificar al monitor/i);
  });

  it('maneja error al cargar asignaciones', async () => {
    global.fetch = jest.fn().mockResolvedValueOnce({
      ok: false,
      json: () => Promise.resolve({ error: 'Error del servidor' })
    });

    render(<EvaluarSupervisorHU021 />);

    expect(await screen.findByTestId('popup')).toHaveTextContent('Error del servidor');
  });

  it('cierra el popup al hacer clic en cerrar', async () => {
    global.fetch = jest.fn().mockResolvedValueOnce({
      ok: false,
      json: () => Promise.resolve({ error: 'Error de prueba' })
    });

    render(<EvaluarSupervisorHU021 />);

    expect(await screen.findByTestId('popup')).toBeInTheDocument();

    const closeButton = screen.getByTestId('popup-close');
    await userEvent.click(closeButton);

    expect(screen.queryByTestId('popup')).not.toBeInTheDocument();
  });

  it('filtra asignaciones por texto de busqueda', async () => {
    const assignment2 = {
      monitoringId: 11,
      monitoringName: 'Redes - 2025-1',
      courseName: 'Redes',
      semester: '2025-1',
      professorName: 'Profesor Dos',
      evaluated: false,
      status: 'PENDIENTE'
    };

    global.fetch = jest.fn().mockResolvedValueOnce({
      ok: true,
      json: () => Promise.resolve([pendingAssignment, assignment2])
    });

    render(<EvaluarSupervisorHU021 />);

    await waitFor(() => expect(global.fetch).toHaveBeenCalled());

    const searchInput = screen.getByPlaceholderText(/Buscar por profesor/i);

    await userEvent.type(searchInput, 'Uno');

    expect(await screen.findByRole('button', { name: /profesor uno/i })).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: /profesor dos/i })).not.toBeInTheDocument();

    await userEvent.clear(searchInput);
    await userEvent.type(searchInput, 'Redes');

    expect(await screen.findByRole('button', { name: /profesor dos/i })).toBeInTheDocument();
    await waitFor(() => {
      expect(screen.queryByRole('button', { name: /profesor uno/i })).not.toBeInTheDocument();
    });
  });

  it('maneja error al cargar preguntas de evaluacion', async () => {
    global.fetch = jest.fn()
      .mockResolvedValueOnce({
        ok: true,
        json: () => Promise.resolve([pendingAssignment])
      })
      .mockResolvedValueOnce({
        ok: false,
        json: () => Promise.resolve({ error: 'Error cargando preguntas' })
      });

    render(<EvaluarSupervisorHU021 />);

    const assignmentButton = await screen.findByRole('button', { name: /profesor uno/i });
    await userEvent.click(assignmentButton);

    expect(await screen.findByTestId('popup')).toHaveTextContent('Error cargando preguntas');
  });

  it('maneja error al enviar evaluacion', async () => {
    global.fetch = jest.fn()
      .mockResolvedValueOnce({
        ok: true,
        json: () => Promise.resolve([pendingAssignment])
      })
      .mockResolvedValueOnce({
        ok: true,
        json: () => Promise.resolve(activeQuestionsPayload)
      })
      .mockResolvedValueOnce({
        ok: false,
        json: () => Promise.resolve({ error: 'Error al guardar' })
      });

    render(<EvaluarSupervisorHU021 />);

    const assignmentButton = await screen.findByRole('button', { name: /profesor uno/i });
    await userEvent.click(assignmentButton);

    await waitFor(() => {
      expect(screen.getAllByRole('radiogroup').length).toBeGreaterThan(0);
    });

    const submitButton = screen.getByRole('button', { name: /Enviar evaluación/i });
    await userEvent.click(submitButton);

    expect(await screen.findByTestId('popup')).toHaveTextContent('Error al guardar');
  });

  it('navega opciones de puntuacion con teclado', async () => {
    global.fetch = jest.fn()
      .mockResolvedValueOnce({
        ok: true,
        json: () => Promise.resolve([pendingAssignment])
      })
      .mockResolvedValueOnce({
        ok: true,
        json: () => Promise.resolve(activeQuestionsPayload)
      });

    render(<EvaluarSupervisorHU021 />);

    const assignmentButton = await screen.findByRole('button', { name: /profesor uno/i });
    await userEvent.click(assignmentButton);

    await waitFor(() => {
      expect(screen.getAllByRole('radiogroup').length).toBeGreaterThan(0);
    });

    const allScoreButtons = screen.getAllByRole('radio');

    expect(allScoreButtons[3]).toHaveClass('active');

    fireEvent.keyDown(allScoreButtons[3], { key: 'ArrowRight' });
    expect(allScoreButtons[4]).toHaveClass('active');

    fireEvent.keyDown(allScoreButtons[4], { key: 'ArrowLeft' });
    expect(allScoreButtons[3]).toHaveClass('active');

    fireEvent.keyDown(allScoreButtons[3], { key: 'Home' });
    expect(allScoreButtons[0]).toHaveClass('active');

    fireEvent.keyDown(allScoreButtons[0], { key: 'End' });
    expect(allScoreButtons[6]).toHaveClass('active');

    fireEvent.keyDown(allScoreButtons[6], { key: 'ArrowUp' });
    expect(allScoreButtons[5]).toHaveClass('active');

    fireEvent.keyDown(allScoreButtons[5], { key: 'ArrowDown' });
    expect(allScoreButtons[6]).toHaveClass('active');

    fireEvent.keyDown(allScoreButtons[6], { key: 'Tab' });
    expect(allScoreButtons[6]).toHaveClass('active');
  });

  it('no navega con teclado si la asignacion ya fue evaluada', async () => {
    const evaluatedAssignment = {
      monitoringId: 15,
      monitoringName: 'Evaluada - 2025-1',
      courseName: 'Evaluada',
      semester: '2025-1',
      professorName: 'Profesor Evaluado',
      evaluated: true,
      status: 'ENVIADA'
    };

    global.fetch = jest.fn()
      .mockResolvedValueOnce({
        ok: true,
        json: () => Promise.resolve([evaluatedAssignment])
      })
      .mockResolvedValueOnce({
        ok: true,
        json: () => Promise.resolve(activeQuestionsPayload)
      });

    render(<EvaluarSupervisorHU021 />);

    const assignmentButton = await screen.findByRole('button', { name: /profesor evaluado/i });
    await userEvent.click(assignmentButton);

    await waitFor(() => {
      expect(screen.getAllByRole('radiogroup').length).toBeGreaterThan(0);
    });

    const allScoreButtons = screen.getAllByRole('radio');

    expect(allScoreButtons[0]).toBeDisabled();

    fireEvent.keyDown(allScoreButtons[3], { key: 'ArrowRight' });
    expect(allScoreButtons[3]).toHaveClass('active');
  });

  it('muestra el subtitulo de monitoria cuando isDuplicateCourseLabel retorna false por monitoringName nulo', async () => {
    const nullMonitoringAssignment = {
      monitoringId: 20,
      monitoringName: null,
      courseName: 'Fisica',
      semester: '2025-1',
      professorName: 'Profesor Tres',
      evaluated: false,
      status: 'PENDIENTE'
    };

    global.fetch = jest.fn()
      .mockResolvedValueOnce({
        ok: true,
        json: () => Promise.resolve([nullMonitoringAssignment])
      })
      .mockResolvedValueOnce({
        ok: true,
        json: () => Promise.resolve(activeQuestionsPayload)
      });

    render(<EvaluarSupervisorHU021 />);

    const assignmentButton = await screen.findByRole('button', { name: /profesor tres/i });
    await userEvent.click(assignmentButton);

    await waitFor(() => {
      expect(screen.getAllByRole('radiogroup').length).toBeGreaterThan(0);
    });

    expect(screen.getByText('Monitoría sin nombre')).toBeInTheDocument();

    const strengthsTextarea = screen.getByPlaceholderText(/claridad en los objetivos/i);
    await userEvent.type(strengthsTextarea, 'Buena comunicacion');

    const improvementTextarea = screen.getByPlaceholderText(/espacios de retroalimentacion/i);
    await userEvent.type(improvementTextarea, 'Mas reuniones');

    expect(strengthsTextarea).toHaveValue('Buena comunicacion');
    expect(improvementTextarea).toHaveValue('Mas reuniones');
  });

  it('muestra "Periodo sin registrar" cuando el semestre es invalido', async () => {
    const invalidSemesterAssignment = {
      monitoringId: 30,
      monitoringName: 'Calculo - 2025-A',
      courseName: 'Calculo',
      semester: '2025-A',
      professorName: 'Profesor Cuatro',
      evaluated: false,
      status: 'PENDIENTE'
    };

    global.fetch = jest.fn()
      .mockResolvedValueOnce({
        ok: true,
        json: () => Promise.resolve([invalidSemesterAssignment])
      })
      .mockResolvedValueOnce({
        ok: true,
        json: () => Promise.resolve(activeQuestionsPayload)
      });

    render(<EvaluarSupervisorHU021 />);

    const assignmentButton = await screen.findByRole('button', { name: /profesor cuatro/i });
    await userEvent.click(assignmentButton);

    await waitFor(() => {
      expect(screen.getAllByRole('radiogroup').length).toBeGreaterThan(0);
    });

    expect(screen.getAllByText(/Periodo sin registrar/i).length).toBeGreaterThanOrEqual(1);
  });

  it('maneja isDuplicateCourseLabel cuando courseLine esta vacio', async () => {
    const emptyCourseAssignment = {
      monitoringId: 40,
      monitoringName: 'Algebra - 2025-1',
      courseName: '',
      semester: '',
      professorName: 'Profesor Cinco',
      evaluated: false,
      status: 'PENDIENTE'
    };

    global.fetch = jest.fn()
      .mockResolvedValueOnce({
        ok: true,
        json: () => Promise.resolve([emptyCourseAssignment])
      })
      .mockResolvedValueOnce({
        ok: true,
        json: () => Promise.resolve(activeQuestionsPayload)
      });

    render(<EvaluarSupervisorHU021 />);

    const assignmentButton = await screen.findByRole('button', { name: /profesor cinco/i });
    await userEvent.click(assignmentButton);

    await waitFor(() => {
      expect(screen.getAllByRole('radiogroup').length).toBeGreaterThan(0);
    });

    const card = screen.getByRole('button', { name: /profesor cinco/i });
    expect(card.textContent).toMatch(/Algebra/i);
  });
});
