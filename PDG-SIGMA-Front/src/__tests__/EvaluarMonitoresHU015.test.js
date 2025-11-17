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

  it('permite seleccionar una asignación y muestra la alerta de penalización', async () => {
  render(<EvaluarMonitoresHU015 />);

    await waitFor(() => expect(global.fetch).toHaveBeenCalled());
    await screen.findByText('María López Martínez');

  const assignmentButton = screen.getByRole('button', { name: /maría lópez martínez/i });
  await userEvent.click(assignmentButton);

    expect(screen.getAllByText('María López Martínez')[0]).toBeInTheDocument();
    expect(screen.getAllByText(/Desarrollo Web/i).length).toBeGreaterThan(0);

    const selects = screen.getAllByRole('combobox');
    for (const select of selects) {
      await userEvent.selectOptions(select, '1');
    }

    await screen.findByText(/Puntaje por debajo de 3.0/i);
    expect(screen.getByRole('button', { name: /Guardar evaluación/i })).toBeInTheDocument();
  });
});
