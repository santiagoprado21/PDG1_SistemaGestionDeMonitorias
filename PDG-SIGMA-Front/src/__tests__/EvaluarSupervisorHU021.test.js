import React from 'react';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import EvaluarSupervisorHU021 from '../EvaluarSupervisorHU021';

jest.mock('../VerticalNavbar', () => () => <nav data-testid="navbar" />);
jest.mock('../LoadingSpinner', () => ({ message }) => <div data-testid="spinner">{message}</div>);
jest.mock('../PopUp', () => ({
  PopUp: ({ show, children }) => (show ? <div data-testid="popup">{children}</div> : null)
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
});
