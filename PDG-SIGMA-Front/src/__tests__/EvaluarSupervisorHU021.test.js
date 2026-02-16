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

describe('EvaluarSupervisorHU021', () => {
  beforeEach(() => {
    global.fetch = jest.fn()
      .mockResolvedValueOnce({
        ok: true,
        json: () => Promise.resolve([
          {
            monitoringId: 10,
            monitoringName: 'Algoritmos - 2025-1',
            courseName: 'Algoritmos',
            semester: '2025-1',
            professorName: 'Profesor Uno',
            evaluated: false,
            status: 'PENDIENTE'
          }
        ])
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
        json: () => Promise.resolve([
          {
            monitoringId: 10,
            monitoringName: 'Algoritmos - 2025-1',
            courseName: 'Algoritmos',
            semester: '2025-1',
            professorName: 'Profesor Uno',
            evaluated: true,
            status: 'ENVIADA'
          }
        ])
      });

    window.localStorage.setItem('userId', 'MON-10');
    window.localStorage.setItem('token', 'Bearer token');
  });

  afterEach(() => {
    jest.resetAllMocks();
    window.localStorage.clear();
  });

  it('permite enviar una evaluacion con escala Likert 1-7', async () => {
    render(<EvaluarSupervisorHU021 />);

    await waitFor(() => expect(global.fetch).toHaveBeenCalled());
    const assignmentButton = await screen.findByRole('button', { name: /profesor uno/i });
    await userEvent.click(assignmentButton);

    const selects = screen.getAllByRole('combobox');
    expect(selects.length).toBe(8);

    for (const select of selects) {
      await userEvent.selectOptions(select, '7');
    }

    const submitButton = screen.getByRole('button', { name: /Enviar/i });
    await userEvent.click(submitButton);

    await waitFor(() => expect(global.fetch).toHaveBeenCalledTimes(3));

    const [, postCall] = global.fetch.mock.calls;
    const [, postConfig] = postCall;
    const payload = JSON.parse(postConfig.body);

    expect(payload.monitorIdentifier).toBe('MON-10');
    expect(payload.guidanceClarity).toBe(7);
    expect(payload.trustEnvironment).toBe(7);
  });
});
