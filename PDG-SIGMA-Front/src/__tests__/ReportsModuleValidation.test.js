import React from 'react';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import Reports from '../Reports';

const mockSave = jest.fn();
const mockSetFontSize = jest.fn();
const mockText = jest.fn();
const mockAutoTable = jest.fn();

jest.mock('../VerticalNavbar', () => {
  return function MockVerticalNavbar() {
    return <div data-testid="vertical-navbar">Vertical Navbar</div>;
  };
});

jest.mock('jspdf', () => {
  const jsPDFMock = jest.fn().mockImplementation(() => ({
    setFontSize: mockSetFontSize,
    text: mockText,
    save: mockSave
  }));

  return {
    __esModule: true,
    jsPDF: jsPDFMock,
    default: { jsPDF: jsPDFMock }
  };
});

jest.mock('jspdf-autotable', () => jest.fn((...args) => mockAutoTable(...args)));

jest.mock('recharts', () => {
  const MockComponent = ({ children }) => <div>{children}</div>;
  return {
    BarChart: MockComponent,
    Bar: MockComponent,
    XAxis: MockComponent,
    YAxis: MockComponent,
    CartesianGrid: MockComponent,
    Tooltip: MockComponent,
    Legend: MockComponent,
    PieChart: MockComponent,
    Pie: MockComponent,
    Cell: MockComponent,
    LineChart: MockComponent,
    Line: MockComponent
  };
});

describe('HU Reportes - Pruebas y validación del módulo', () => {
  beforeEach(() => {
    jest.clearAllMocks();

    global.fetch = jest.fn();
    global.URL.createObjectURL = jest.fn(() => 'blob:mock-url');

    Object.defineProperty(window, 'localStorage', {
      value: {
        getItem: jest.fn((key) => {
          if (key === 'userId') return '5001';
          if (key === 'role') return 'jfedpto';
          if (key === 'token') return 'Bearer fake-token';
          return null;
        }),
        setItem: jest.fn(),
        clear: jest.fn()
      },
      writable: true
    });
  });

  const queueReportsFetchSuccess = () => {
    const monitorReport = [
      {
        idProfessor: 'P100',
        semester: '2026-1',
        program: 'Ingeniería de Sistemas',
        course: 'POO',
        professor: 'Prof Uno',
        name: 'Monitor Uno',
        nameAndCourse: 'Monitor Uno POO',
        completed: 3,
        pending: 1,
        late: 0
      }
    ];

    const professorReport = [
      {
        name: 'Prof Uno',
        semester: '2026-1',
        program: 'Ingeniería de Sistemas',
        course: 'POO',
        completed: 4,
        pending: 0,
        late: 0
      }
    ];

    const attendanceReport = [
      {
        mes: 'Enero',
        semestre: '2026-1',
        total_mes: 20,
        asistencia_por_curso: [
          {
            curso: 'POO',
            cantidad: 20,
            estudiantes: []
          }
        ]
      }
    ];

    const categoriesReport = {
      detalle_por_curso: [
        {
          curso: 'POO',
          categorias: [{ categoria: 'Tutoría', cantidad: 2 }]
        }
      ],
      totales_por_categoria: [{ categoria: 'Tutoría', cantidad_total: 2 }]
    };

    fetch
      .mockResolvedValueOnce({ ok: true, json: async () => monitorReport })
      .mockResolvedValueOnce({ ok: true, json: async () => professorReport })
      .mockResolvedValueOnce({ ok: true, json: async () => attendanceReport })
      .mockResolvedValueOnce({ ok: true, json: async () => categoriesReport });
  };

  const closeInitialPopupIfPresent = () => {
    const popupOkButton = screen.queryByRole('button', { name: 'OK' });
    if (popupOkButton) {
      fireEvent.click(popupOkButton);
    }
  };

  test('renderiza dashboard sin auto-actualizar hasta pulsar botón', () => {
    render(<Reports />);
    closeInitialPopupIfPresent();

    expect(screen.getByText('Dashboard de Reportes de Cumplimiento')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /Actualizar ahora/i })).toBeInTheDocument();
    expect(fetch).not.toHaveBeenCalled();
  });

  test('actualiza datos cuando se hace clic en "Actualizar ahora"', async () => {
    queueReportsFetchSuccess();

    render(<Reports />);
    closeInitialPopupIfPresent();
    fireEvent.click(screen.getByRole('button', { name: /Actualizar ahora/i }));

    await waitFor(() => {
      expect(fetch).toHaveBeenCalledTimes(4);
    });

    expect(fetch.mock.calls[0][0]).toContain('/monitoring/getMonitorsReport/5001/jfedpto');
    expect(fetch.mock.calls[1][0]).toContain('/monitoring/getProfessorReport/P100');
    expect(fetch.mock.calls[2][0]).toContain('/monitoring/getAttendanceReport/jfedpto/5001');
    expect(fetch.mock.calls[3][0]).toContain('/monitoring/getCategoriesReport/jfedpto/5001');
  });

  test('permite exportar reportes en CSV y muestra opción PDF', async () => {
    queueReportsFetchSuccess();

    render(<Reports />);
    closeInitialPopupIfPresent();
    fireEvent.click(screen.getByRole('button', { name: /Actualizar ahora/i }));

    await waitFor(() => {
      expect(fetch).toHaveBeenCalledTimes(4);
    });

    const csvButtons = screen.getAllByRole('button', { name: 'Descargar CSV' });
    const pdfButtons = screen.getAllByRole('button', { name: 'Descargar PDF' });

    fireEvent.click(csvButtons[0]);
    expect(pdfButtons.length).toBeGreaterThan(0);

    await waitFor(() => {
      expect(global.URL.createObjectURL).toHaveBeenCalled();
    });
  });
});
