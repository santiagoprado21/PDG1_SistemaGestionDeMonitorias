import React from 'react';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { BrowserRouter } from 'react-router-dom';
import Reports from '../Reports';
import GenerateSimonFile from '../GenerateSimonFile';
import UpdateButton from '../UpdateButton';

jest.mock('../VerticalNavbar', () => () => <nav data-testid="mock-navbar" />);
jest.mock('../LoadingSpinner', () => () => <div data-testid="mock-spinner">Loading</div>);
jest.mock('../PopUp', () => ({
  PopUp: ({ show = true, children, message }) => {
    if (!show && !message) return null;
    return <div data-testid="mock-popup">{children || message}</div>;
  }
}));

jest.mock('../config/ApiBackend', () => ({
  BACKEND_URL: 'http://localhost:5435',
  getApiUrl: (path) => `http://localhost:5435${path}`
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
    LabelList: Mock
  };
});

const renderWithRouter = (component) => render(<BrowserRouter>{component}</BrowserRouter>);

describe('ReportesExportacionDatos', () => {
  beforeEach(() => {
    jest.clearAllMocks();
    window.localStorage.clear();
    window.localStorage.setItem('userId', 'COOR-1');
    window.localStorage.setItem('role', 'jfedpto');
    window.localStorage.setItem('token', 'Bearer token');
    global.fetch = jest.fn();
  });

  afterEach(() => {
    jest.restoreAllMocks();
    window.localStorage.clear();
  });

  test('Reports renderiza datos simulados y aplica filtros básicos', async () => {
    global.fetch.mockImplementation(async (url) => {
      if (url.includes('/monitoring/getMonitorsReport/')) {
        return {
          ok: true,
          json: async () => ([
            {
              idProfessor: 'PROF-1',
              semester: '2025-1',
              program: 'Ingenieria',
              course: 'POO',
              professor: 'Ana Perez',
              name: 'Monitor Uno',
              nameAndCourse: 'Monitor Uno POO',
              completed: 6,
              pending: 2,
              late: 1
            },
            {
              idProfessor: 'PROF-1',
              semester: '2025-2',
              program: 'Matematicas',
              course: 'Calculo',
              professor: 'Ana Perez',
              name: 'Monitor Dos',
              nameAndCourse: 'Monitor Dos Calculo',
              completed: 4,
              pending: 1,
              late: 0
            }
          ])
        };
      }

      if (url.includes('/monitoring/getProfessorReport/')) {
        return {
          ok: true,
          json: async () => ([
            {
              idProfessor: 'PROF-1',
              name: 'Dr. Ana Perez',
              course: 'POO',
              completed: 4,
              pending: 1,
              late: 0
            }
          ])
        };
      }

      if (url.includes('/monitoring/getCategoriesReport/')) {
        return {
          ok: true,
          json: async () => ({
            detalle_por_curso: [],
            totales_por_categoria: []
          })
        };
      }

      return { ok: true, json: async () => [] };
    });

    renderWithRouter(<Reports />);

    expect(await screen.findByText('Monitor Uno')).toBeInTheDocument();
    expect(screen.getByText('Monitor Dos')).toBeInTheDocument();

    const selects = screen.getAllByRole('combobox');
    fireEvent.change(selects[0], { target: { value: '2025-2' } });

    await waitFor(() => {
      expect(screen.queryByText('Monitor Uno')).not.toBeInTheDocument();
      expect(screen.getByText('Monitor Dos')).toBeInTheDocument();
    });
  });

  test('GenerateSimonFile invoca la generación con mocks sin crear archivo real', async () => {
    const clickSpy = jest.spyOn(HTMLAnchorElement.prototype, 'click').mockImplementation(() => {});
    if (!window.URL.createObjectURL) {
      window.URL.createObjectURL = () => 'blob:fake';
    }
    if (!window.URL.revokeObjectURL) {
      window.URL.revokeObjectURL = () => {};
    }
    const createObjectURLSpy = jest.spyOn(window.URL, 'createObjectURL').mockImplementation(() => 'blob:fake');
    const revokeObjectURLSpy = jest.spyOn(window.URL, 'revokeObjectURL').mockImplementation(() => {});

    global.fetch.mockImplementation(async (url) => {
      if (url.includes('/simon/preview')) {
        return {
          ok: true,
          json: async () => ({
            totalMonitorings: 1,
            canGenerate: true,
            monitorings: [
              {
                nombre: 'Ana',
                apellido: 'Paz',
                codigoEstudiante: '2020123',
                email: 'ana@uni.edu',
                nombreCurso: 'Algebra',
                profesorSolicita: 'Docente X',
                fechaInicio: '2026-01-10',
                fechaFin: '2026-05-10',
                totalHoras: 96
              }
            ]
          })
        };
      }
      if (url.includes('/simon/history')) {
        return { ok: true, json: async () => [] };
      }
      if (url.includes('/simon/generate')) {
        return { ok: true, blob: async () => new Blob(['xlsx']) };
      }
      return { ok: true, json: async () => [] };
    });

    renderWithRouter(<GenerateSimonFile />);

    expect(await screen.findByText(/Listo para generar/i)).toBeInTheDocument();

    fireEvent.click(screen.getByRole('button', { name: /Generar y Descargar Archivo SIMON/i }));

    await waitFor(() => {
      expect(global.fetch).toHaveBeenCalledWith(
        expect.stringContaining('/simon/generate?generatedBy=COOR-1&semester='),
        expect.any(Object)
      );
      expect(createObjectURLSpy).toHaveBeenCalled();
      expect(clickSpy).toHaveBeenCalled();
      expect(revokeObjectURLSpy).toHaveBeenCalled();
    });

    clickSpy.mockRestore();
    createObjectURLSpy.mockRestore();
    revokeObjectURLSpy.mockRestore();
  });

  test('UpdateButton muestra estados y ejecuta acción al confirmar actualización', async () => {
    global.fetch.mockResolvedValue({
      text: async () => 'OK'
    });

    render(<UpdateButton role="professor" userId="PROF-1" />);

    const updateButton = screen.getByRole('button', { name: /Actualizar/i });
    expect(updateButton).toBeEnabled();
    expect(screen.queryByRole('button', { name: /Confirmar actualización/i })).not.toBeInTheDocument();

    fireEvent.click(updateButton);

    const confirmButton = screen.getByRole('button', { name: /Confirmar actualización/i });
    expect(confirmButton).toBeEnabled();

    fireEvent.click(confirmButton);

    await waitFor(() => {
      expect(global.fetch).toHaveBeenCalledTimes(1);
      expect(screen.getByTestId('mock-popup')).toHaveTextContent(/Estado : OK/i);
    });
  });
});
