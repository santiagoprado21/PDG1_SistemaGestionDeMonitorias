import React from 'react';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import Reports from '../Reports';

jest.mock('../VerticalNavbar', () => () => <nav data-testid="navbar" />);
jest.mock('../PopUp', () => ({
  PopUp: ({ show, children }) => (show ? <div data-testid="popup">{children}</div> : null)
}));
jest.mock('../config/ApiBackend', () => ({
  BACKEND_URL: 'http://localhost:8080',
  getApiUrl: (path) => `http://localhost:8080${path}`
}));

jest.mock('recharts', () => {
  const Mock = ({ children }) => <div>{children}</div>;
  return {
    BarChart: Mock,
    Bar: Mock,
    XAxis: Mock,
    YAxis: Mock,
    CartesianGrid: Mock,
    Tooltip: Mock,
    Legend: Mock,
    PieChart: Mock,
    Pie: Mock,
    Cell: Mock,
    LineChart: Mock,
    Line: Mock,
    LabelList: Mock
  };
});

describe('Reports contextual help', () => {
  beforeEach(() => {
    window.localStorage.setItem('userId', 'USR-1');
    window.localStorage.setItem('role', 'admin');
    window.localStorage.setItem('token', 'Bearer fake-token');

    global.fetch = jest.fn((url) => {
      if (url.includes('/monitoring/getMonitorsReport/')) {
        return Promise.resolve({
          ok: true,
          json: () => Promise.resolve([
            {
              idProfessor: 'PROF-1',
              semester: '2025-1',
              program: 'Ingenieria de Sistemas',
              course: 'POO',
              professor: 'Ana Perez',
              name: 'Monitor Uno',
              nameAndCourse: 'Monitor Uno POO',
              completed: 6,
              pending: 2,
              late: 1
            }
          ])
        });
      }

      if (url.includes('/monitoring/getProfessorReport/')) {
        return Promise.resolve({
          ok: true,
          json: () => Promise.resolve([
            {
              idProfessor: 'PROF-1',
              name: 'Dr. Ana Perez',
              course: 'POO',
              completed: 4,
              pending: 1,
              late: 1
            }
          ])
        });
      }

      if (url.includes('/monitoring/getAttendanceReport/')) {
        return Promise.resolve({
          ok: true,
          json: () => Promise.resolve([
            {
              semestre: '2025-1',
              mes: 'Enero',
              total_mes: 15,
              asistencia_por_curso: [{ curso: 'POO', cantidad: 15, estudiantes: [] }]
            }
          ])
        });
      }

      if (url.includes('/monitoring/getCategoriesReport/')) {
        return Promise.resolve({
          ok: true,
          json: () => Promise.resolve({
            detalle_por_curso: [
              {
                curso: 'POO',
                categorias: [{ categoria: 'Tutoria', cantidad: 7 }]
              }
            ],
            totales_por_categoria: [{ categoria: 'Tutoria', cantidad_total: 7 }]
          })
        });
      }

      return Promise.resolve({ ok: true, json: () => Promise.resolve([]) });
    });
  });

  afterEach(() => {
    jest.resetAllMocks();
    window.localStorage.clear();
  });

  it('muestra y alterna ayudas contextuales por hover en cada reporte', async () => {
    render(<Reports />);

    await waitFor(() => expect(global.fetch).toHaveBeenCalled());

    const monitorHelpButton = screen.getByRole('button', { name: /Ayuda de Rendimiento de monitores/i });
    const professorHelpButton = screen.getByRole('button', { name: /Ayuda de Rendimiento de profesores/i });

    await userEvent.hover(monitorHelpButton);
    expect(await screen.findByText(/Muestra el avance de actividades por monitor/i)).toBeInTheDocument();

    await userEvent.hover(professorHelpButton);
    expect(await screen.findByText(/Mide el avance de actividades asociadas a cada profesor/i)).toBeInTheDocument();
    await waitFor(() => {
      expect(screen.queryByText(/Muestra el avance de actividades por monitor/i)).not.toBeInTheDocument();
    });

    await userEvent.unhover(professorHelpButton);
    await waitFor(() => {
      expect(screen.queryByText(/Mide el avance de actividades asociadas a cada profesor/i)).not.toBeInTheDocument();
    });
  });
});
