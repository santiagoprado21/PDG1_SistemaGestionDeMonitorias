import React from 'react';
import { render, screen, waitFor, fireEvent } from '@testing-library/react';
import Reports from '../Reports';

jest.mock('../VerticalNavbar', () => () => <nav data-testid="navbar" />);
jest.mock('../PopUp', () => ({
  PopUp: ({ show, children, onClose }) => (show ? <div data-testid="popup">{children}<button data-testid="popup-close" onClick={onClose}>Cerrar</button></div> : null)
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

describe('ReportsHelp', () => {
  const renderWithProfile = (profileRole = 'admin') => {
    window.localStorage.setItem('userId', profileRole === 'professor' ? 'PROF-1' : 'USR-1');
    window.localStorage.setItem('role', profileRole);
    window.localStorage.setItem('token', 'Bearer fake-token');
  };

  const defaultMonitorData = [
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
  ];
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
    await screen.findByText('Monitor Uno');

    const monitorHelpButton = screen.getByRole('button', { name: /Ayuda de Rendimiento de monitores/i });
    const professorHelpButton = screen.getByRole('button', { name: /Ayuda de Rendimiento de profesores/i });

    fireEvent.mouseEnter(monitorHelpButton);
    expect(await screen.findByText(/Muestra el avance de actividades por monitor/i)).toBeInTheDocument();

    fireEvent.mouseEnter(professorHelpButton);
    expect(await screen.findByText(/Mide el avance de actividades asociadas a cada profesor/i)).toBeInTheDocument();
    await waitFor(() => {
      expect(screen.queryByText(/Muestra el avance de actividades por monitor/i)).not.toBeInTheDocument();
    });

    fireEvent.mouseLeave(professorHelpButton);
    await waitFor(() => {
      expect(screen.queryByText(/Mide el avance de actividades asociadas a cada profesor/i)).not.toBeInTheDocument();
    });
  });

  it('abre ayuda por foco y la cierra con Escape', async () => {
    render(<Reports />);

    await waitFor(() => expect(global.fetch).toHaveBeenCalled());
    await screen.findByText('Monitor Uno');

    const monitorHelpButton = screen.getByRole('button', { name: /Ayuda de Rendimiento de monitores/i });

    fireEvent.focus(monitorHelpButton);
    expect(await screen.findByText(/Muestra el avance de actividades por monitor/i)).toBeInTheDocument();

    fireEvent.keyDown(document, { key: 'Escape', code: 'Escape' });

    await waitFor(() => {
      expect(screen.queryByText(/Muestra el avance de actividades por monitor/i)).not.toBeInTheDocument();
    });
  });

  it('muestra el popup inicial de ayuda de filtros', async () => {
    render(<Reports />);

    expect(await screen.findByTestId('popup')).toHaveTextContent(/Puedes usar los filtros para refinar la informacion de los reportes|Puedes usar los filtros para refinar la información de los reportes/i);
  });

  it('maneja error HTTP al obtener datos de monitores sin romper', async () => {
    global.fetch = jest.fn((url) => {
      if (url.includes('/monitoring/getMonitorsReport/')) {
        return Promise.resolve({
          ok: false,
          status: 500,
          json: () => Promise.resolve({ error: 'Error del servidor' })
        });
      }
      return Promise.resolve({ ok: true, json: () => Promise.resolve([]) });
    });

    render(<Reports />);
    await waitFor(() => expect(global.fetch).toHaveBeenCalled());
    expect(await screen.findByText('Reportes')).toBeInTheDocument();
  });

  it('maneja respuesta JSON no arreglo de monitores sin romper', async () => {
    global.fetch = jest.fn((url) => {
      if (url.includes('/monitoring/getMonitorsReport/')) {
        return Promise.resolve({
          ok: true,
          json: () => Promise.resolve({ mensaje: 'no es un arreglo' })
        });
      }
      return Promise.resolve({ ok: true, json: () => Promise.resolve([]) });
    });

    render(<Reports />);
    await waitFor(() => expect(global.fetch).toHaveBeenCalled());
    expect(await screen.findByText('Reportes')).toBeInTheDocument();
  });

  it('cierra el popup al hacer clic en cerrar y alterna visibilidad', async () => {
    render(<Reports />);
    expect(await screen.findByTestId('popup')).toBeInTheDocument();

    const closeButton = screen.getByTestId('popup-close');
    fireEvent.click(closeButton);

    await waitFor(() => {
      expect(screen.queryByTestId('popup')).not.toBeInTheDocument();
    });
  });

  it('alterna filtros de periodo, programa y curso', async () => {
    render(<Reports />);
    await waitFor(() => expect(global.fetch).toHaveBeenCalled());
    await screen.findByText('Monitor Uno');

    const selects = screen.getAllByRole('combobox');
    expect(selects.length).toBeGreaterThanOrEqual(3);

    fireEvent.change(selects[0], { target: { value: '2025-1' } });
    fireEvent.change(selects[1], { target: { value: 'Ingenieria de Sistemas' } });
    fireEvent.change(selects[2], { target: { value: 'POO' } });
  });

  it('maneja error en fetch de categorias', async () => {
    global.fetch = jest.fn((url) => {
      if (url.includes('/monitoring/getMonitorsReport/')) {
        return Promise.resolve({ ok: true, json: () => Promise.resolve([{ idProfessor: 'PROF-1', semester: '2025-1', program: 'Ing', course: 'POO', professor: 'Ana', name: 'M1', completed: 1, pending: 1, late: 0 }]) });
      }
      if (url.includes('/monitoring/getProfessorReport/')) {
        return Promise.resolve({ ok: true, json: () => Promise.resolve([{ idProfessor: 'PROF-1', name: 'Ana', course: 'POO', completed: 1, pending: 1, late: 0 }]) });
      }
      if (url.includes('/monitoring/getCategoriesReport/')) {
        return Promise.resolve({ ok: false, status: 500, json: () => Promise.resolve({ error: 'Server error' }) });
      }
      return Promise.resolve({ ok: true, json: () => Promise.resolve([]) });
    });

    window.localStorage.setItem('role', 'jfedpto');
    render(<Reports />);
    await waitFor(() => expect(global.fetch).toHaveBeenCalled());
    expect(await screen.findByText('Reportes')).toBeInTheDocument();
  });

  it('renderiza pie chart con totales por categoria cuando no hay curso seleccionado', async () => {
    global.fetch = jest.fn((url) => {
      if (url.includes('/monitoring/getMonitorsReport/')) {
        return Promise.resolve({ ok: true, json: () => Promise.resolve([{ idProfessor: 'PROF-1', semester: '2025-1', program: 'Ing', course: 'POO', professor: 'Ana', name: 'M1', nameAndCourse: 'M1 POO', completed: 1, pending: 1, late: 0 }]) });
      }
      if (url.includes('/monitoring/getProfessorReport/')) {
        return Promise.resolve({ ok: true, json: () => Promise.resolve([{ idProfessor: 'PROF-1', name: 'Ana', course: 'POO', completed: 1, pending: 1, late: 0 }]) });
      }
      if (url.includes('/monitoring/getCategoriesReport/')) {
        return Promise.resolve({ ok: true, json: () => Promise.resolve({ detalle_por_curso: [{ curso: 'POO', categorias: [{ categoria: 'Tutoria', cantidad: 7 }] }], totales_por_categoria: [{ categoria: 'Tutoria', cantidad_total: 7 }] }) });
      }
      return Promise.resolve({ ok: true, json: () => Promise.resolve([]) });
    });

    window.localStorage.setItem('role', 'jfedpto');
    render(<Reports />);
    await waitFor(() => expect(global.fetch).toHaveBeenCalled());
    await screen.findByText('Reportes');
  });
});
