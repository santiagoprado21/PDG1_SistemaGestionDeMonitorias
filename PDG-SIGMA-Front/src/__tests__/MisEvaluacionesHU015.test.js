import React from 'react';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import MisEvaluacionesHU015 from '../MisEvaluacionesHU015';

jest.mock('../VerticalNavbar', () => () => <nav data-testid="navbar" />);
jest.mock('../LoadingSpinner', () => ({ message }) => <div data-testid="spinner">{message}</div>);
jest.mock('../PopUp', () => ({
  PopUp: ({ show, children }) => (show ? <div data-testid="popup">{children}</div> : null)
}));
jest.mock('../config/ApiBackend', () => ({ BACKEND_URL: 'http://localhost:8080' }));

describe('MisEvaluacionesHU015', () => {
  beforeEach(() => {
    window.localStorage.setItem('userId', 'MON-1');
    window.localStorage.setItem('token', 'Bearer token');

    const initialEvaluations = [
      {
        evaluationId: 100,
        monitoringName: 'Algoritmos - 2025-1',
        courseName: 'Algoritmos',
        semester: '2025-1',
        professorId: 'PROF-1',
        totalScore: 3.5,
        performanceLevel: 'ADECUADO',
        taskCompliance: 4,
        timelyCommunication: 3,
        planFulfillment: 4,
        attitude: 3,
        comments: 'Buen desempeño',
        acknowledgedByMonitor: false,
        penaltyFlag: false
      }
    ];

    const acknowledgedEvaluations = [
      {
        ...initialEvaluations[0],
        acknowledgedByMonitor: true
      }
    ];

    global.fetch = jest
      .fn()
      .mockResolvedValueOnce({
        ok: true,
        json: () => Promise.resolve(initialEvaluations)
      })
      .mockResolvedValueOnce({
        ok: true,
        json: () => Promise.resolve({ acknowledged: true })
      })
      .mockResolvedValueOnce({
        ok: true,
        json: () => Promise.resolve(acknowledgedEvaluations)
      });
  });

  afterEach(() => {
    jest.resetAllMocks();
    window.localStorage.clear();
  });

  it('permite registrar la lectura de una evaluación', async () => {
  render(<MisEvaluacionesHU015 />);

    await waitFor(() => expect(global.fetch).toHaveBeenCalled());
    await screen.findByText('Algoritmos - 2025-1');

  const acknowledgeButton = screen.getByRole('button', { name: /Marcar como revisada/i });
  await userEvent.click(acknowledgeButton);

    await waitFor(() => expect(global.fetch).toHaveBeenCalledTimes(3));

    await screen.findByText('¡Gracias! Registramos que revisaste la evaluación.');
    expect(screen.getByText('Retroalimentación revisada')).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: /Marcar como revisada/i })).not.toBeInTheDocument();
  });

  it('muestra historial para profesor con el mismo estilo de tarjetas', async () => {
    window.localStorage.setItem('userId', 'PROF-10');
    window.localStorage.setItem('role', 'professor');

    global.fetch = jest.fn().mockResolvedValueOnce({
      ok: true,
      json: () => Promise.resolve([
        {
          monitoringId: 1,
          monitorCode: 'A001',
          monitorFullName: 'Laura Ruiz',
          monitoringName: 'Calculo I - 2026-1',
          courseName: 'Calculo I',
          semester: '2026-1',
          evaluated: true,
          totalScore: 4.25,
          performanceLevel: 'DESTACADO',
          taskCompliance: 4,
          timelyCommunication: 4,
          planFulfillment: 5,
          attitude: 4,
          comments: 'Buen avance y participacion constante.',
          visibleToMonitor: true
        },
        {
          monitoringId: 2,
          monitorCode: 'A002',
          monitorFullName: 'Juan Peña',
          evaluated: false
        }
      ])
    });

    render(<MisEvaluacionesHU015 />);

    expect(await screen.findByText('Historial de evaluaciones registradas')).toBeInTheDocument();
    expect(await screen.findByText(/Monitor evaluado: Laura Ruiz/i)).toBeInTheDocument();
    expect(await screen.findByText('Visible para monitor')).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: /Marcar como revisada/i })).not.toBeInTheDocument();
    expect(screen.queryByText(/Juan Peña/i)).not.toBeInTheDocument();
  });
});
