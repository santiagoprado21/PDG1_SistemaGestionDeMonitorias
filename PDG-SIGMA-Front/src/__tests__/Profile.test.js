import React from 'react';
import { render, screen, waitFor, fireEvent } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import Profile from '../Profile';

const mockNavigate = jest.fn();

jest.mock('react-router-dom', () => ({
  ...jest.requireActual('react-router-dom'),
  useNavigate: () => mockNavigate
}));

jest.mock('../VerticalNavbar', () => () => <nav data-testid="navbar" />);
jest.mock('../UpdateButton', () => ({ role, userId }) => (
  <div data-testid="update-button">{`${role}:${userId}`}</div>
));

jest.mock('../config/ApiBackend', () => ({
  BACKEND_URL: 'http://localhost:5433',
  getApiUrl: (path) => `http://localhost:5433${path}`
}));

describe('Profile', () => {
  beforeEach(() => {
    localStorage.clear();
    localStorage.setItem('token', 'Bearer token-test');
    global.fetch = jest.fn((url) => {
      if (url.includes('/professor/profile/')) {
        return Promise.resolve({
          ok: true,
          json: () => Promise.resolve({
            name: 'Ana Perez',
            school: 'Ingenieria',
            program: 'Sistemas',
            rol: 'Profesor'
          })
        });
      }

      if (url.includes('/monitoring/profile/')) {
        return Promise.resolve({
          ok: true,
          json: () => Promise.resolve([
            {
              id: 10,
              semester: '2025-1',
              courseName: 'POO',
              monitor: 'Monitor Uno',
              professorName: 'Ana Perez'
            },
            {
              id: 11,
              semester: '2025-2',
              courseName: 'Estructuras',
              monitor: 'No hay monitores',
              professorName: 'Ana Perez'
            }
          ])
        });
      }

      if (url.includes('/monitor/profile/')) {
        return Promise.resolve({
          ok: true,
          json: () => Promise.resolve({
            name: 'Monitor Uno',
            school: 'Ingenieria',
            program: 'Sistemas',
            rol: 'Monitor'
          })
        });
      }

      return Promise.resolve({ ok: true, json: () => Promise.resolve([]) });
    });
  });

  afterEach(() => {
    jest.resetAllMocks();
    localStorage.clear();
  });

  it('renderiza perfil de profesor, muestra update button y filtra cursos por periodo', async () => {
    localStorage.setItem('role', 'professor');
    localStorage.setItem('userId', 'PROF-1');

    render(
      <MemoryRouter>
        <Profile />
      </MemoryRouter>
    );

    expect(await screen.findByText('Ana Perez')).toBeInTheDocument();
    expect(screen.getByTestId('update-button')).toHaveTextContent('professor:PROF-1');

    const select = screen.getByRole('combobox');
    fireEvent.change(select, { target: { value: '2025-1' } });

    expect(screen.getByText('POO')).toBeInTheDocument();
    expect(screen.queryByText('Estructuras')).not.toBeInTheDocument();

    fireEvent.click(screen.getByRole('button', { name: /plan/i }));
    expect(mockNavigate).toHaveBeenCalledWith('/plan-actividades/10');
  });

  it('en rol monitor no muestra UpdateButton y muestra columna de profesor asignado', async () => {
    localStorage.setItem('role', 'monitor');
    localStorage.setItem('userId', 'MON-1');

    render(
      <MemoryRouter>
        <Profile />
      </MemoryRouter>
    );

    await waitFor(() => {
      expect(global.fetch).toHaveBeenCalled();
    });

    expect(screen.queryByTestId('update-button')).not.toBeInTheDocument();
    expect(screen.getByText(/profesor asignado/i)).toBeInTheDocument();
  });

  it('en rol jfedpto muestra update button y ambas columnas', async () => {
    localStorage.setItem('role', 'jfedpto');
    localStorage.setItem('userId', 'JEF-1');

    fetch.mockImplementation((url) => {
      if (url.includes('/department-head/profile/')) {
        return Promise.resolve({ ok: true, json: () => Promise.resolve({ name: 'Jefe Dpto', school: 'Ingenieria', program: 'Sistemas', rol: 'Jefe Depto' }) });
      }
      if (url.includes('/monitoring/profile/')) {
        return Promise.resolve({ ok: true, json: () => Promise.resolve([{ id: 20, semester: '2025-1', courseName: 'POO', monitor: 'Monitor X', professorName: 'Prof Y' }]) });
      }
      return Promise.resolve({ ok: true, json: () => Promise.resolve([]) });
    });

    render(<MemoryRouter><Profile /></MemoryRouter>);

    expect(await screen.findByText('Jefe Dpto')).toBeInTheDocument();
    expect(screen.getByTestId('update-button')).toHaveTextContent('jfedpto:JEF-1');
    expect(screen.getByText(/profesor asignado/i)).toBeInTheDocument();
    expect(screen.getByText(/monitor asignado/i)).toBeInTheDocument();
    expect(screen.getByText('Monitor X')).toBeInTheDocument();
    expect(screen.getByText('Prof Y')).toBeInTheDocument();
  });

  it('maneja error HTTP en fetch de perfil de profesor', async () => {
    localStorage.setItem('role', 'professor');
    localStorage.setItem('userId', 'PROF-1');

    fetch.mockImplementation((url) => {
      if (url.includes('/professor/profile/')) {
        return Promise.resolve({ ok: false, json: () => Promise.resolve({ error: 'Not found' }) });
      }
      if (url.includes('/monitoring/profile/')) {
        return Promise.resolve({ ok: true, json: () => Promise.resolve([]) });
      }
      return Promise.resolve({ ok: true, json: () => Promise.resolve([]) });
    });

    render(<MemoryRouter><Profile /></MemoryRouter>);

    await waitFor(() => {
      expect(screen.getByText('Cargando...')).toBeInTheDocument();
    });
  });

  it('maneja data null en fetch de perfil de profesor', async () => {
    localStorage.setItem('role', 'professor');
    localStorage.setItem('userId', 'PROF-1');

    fetch.mockImplementation((url) => {
      if (url.includes('/professor/profile/')) {
        return Promise.resolve({ ok: true, json: () => Promise.resolve(null) });
      }
      if (url.includes('/monitoring/profile/')) {
        return Promise.resolve({ ok: true, json: () => Promise.resolve([]) });
      }
      return Promise.resolve({ ok: true, json: () => Promise.resolve([]) });
    });

    render(<MemoryRouter><Profile /></MemoryRouter>);

    await waitFor(() => {
      expect(screen.getByText('Cargando...')).toBeInTheDocument();
    });
  });

  it('maneja error HTTP en fetch de cursos asignados', async () => {
    localStorage.setItem('role', 'professor');
    localStorage.setItem('userId', 'PROF-1');

    fetch.mockImplementation((url) => {
      if (url.includes('/professor/profile/')) {
        return Promise.resolve({ ok: true, json: () => Promise.resolve({ name: 'Ana', school: 'Ingenieria', program: 'Sistemas', rol: 'Profesor' }) });
      }
      if (url.includes('/monitoring/profile/')) {
        return Promise.resolve({ ok: false });
      }
      return Promise.resolve({ ok: true, json: () => Promise.resolve([]) });
    });

    render(<MemoryRouter><Profile /></MemoryRouter>);

    expect(await screen.findByText('Ana')).toBeInTheDocument();
  });

  it('maneja data null en fetch de cursos asignados', async () => {
    localStorage.setItem('role', 'professor');
    localStorage.setItem('userId', 'PROF-1');

    fetch.mockImplementation((url) => {
      if (url.includes('/professor/profile/')) {
        return Promise.resolve({ ok: true, json: () => Promise.resolve({ name: 'Ana', school: 'Ingenieria', program: 'Sistemas', rol: 'Profesor' }) });
      }
      if (url.includes('/monitoring/profile/')) {
        return Promise.resolve({ ok: true, json: () => Promise.resolve(null) });
      }
      return Promise.resolve({ ok: true, json: () => Promise.resolve([]) });
    });

    render(<MemoryRouter><Profile /></MemoryRouter>);

    expect(await screen.findByText('Ana')).toBeInTheDocument();
  });

  it('maneja error HTTP en fetch de perfil de monitor', async () => {
    localStorage.setItem('role', 'monitor');
    localStorage.setItem('userId', 'MON-1');

    fetch.mockImplementation((url) => {
      if (url.includes('/monitor/profile/')) {
        return Promise.resolve({ ok: false, json: () => Promise.resolve({ error: 'Not found' }) });
      }
      if (url.includes('/monitoring/profile/')) {
        return Promise.resolve({ ok: true, json: () => Promise.resolve([]) });
      }
      return Promise.resolve({ ok: true, json: () => Promise.resolve([]) });
    });

    render(<MemoryRouter><Profile /></MemoryRouter>);

    await waitFor(() => {
      expect(screen.getByText('Cargando...')).toBeInTheDocument();
    });
  });

  it('maneja data null en fetch de perfil de monitor', async () => {
    localStorage.setItem('role', 'monitor');
    localStorage.setItem('userId', 'MON-1');

    fetch.mockImplementation((url) => {
      if (url.includes('/monitor/profile/')) {
        return Promise.resolve({ ok: true, json: () => Promise.resolve(null) });
      }
      if (url.includes('/monitoring/profile/')) {
        return Promise.resolve({ ok: true, json: () => Promise.resolve([]) });
      }
      return Promise.resolve({ ok: true, json: () => Promise.resolve([]) });
    });

    render(<MemoryRouter><Profile /></MemoryRouter>);

    await waitFor(() => {
      expect(screen.getByText('Cargando...')).toBeInTheDocument();
    });
  });

  it('maneja error HTTP en fetch de perfil de jefe de departamento', async () => {
    localStorage.setItem('role', 'jfedpto');
    localStorage.setItem('userId', 'JEF-1');

    fetch.mockImplementation((url) => {
      if (url.includes('/department-head/profile/')) {
        return Promise.resolve({ ok: false, json: () => Promise.resolve({ error: 'Not found' }) });
      }
      if (url.includes('/monitoring/profile/')) {
        return Promise.resolve({ ok: true, json: () => Promise.resolve([]) });
      }
      return Promise.resolve({ ok: true, json: () => Promise.resolve([]) });
    });

    render(<MemoryRouter><Profile /></MemoryRouter>);

    await waitFor(() => {
      expect(screen.getByText('Cargando...')).toBeInTheDocument();
    });
  });

  it('maneja data null en fetch de perfil de jefe de departamento', async () => {
    localStorage.setItem('role', 'jfedpto');
    localStorage.setItem('userId', 'JEF-1');

    fetch.mockImplementation((url) => {
      if (url.includes('/department-head/profile/')) {
        return Promise.resolve({ ok: true, json: () => Promise.resolve(null) });
      }
      if (url.includes('/monitoring/profile/')) {
        return Promise.resolve({ ok: true, json: () => Promise.resolve([]) });
      }
      return Promise.resolve({ ok: true, json: () => Promise.resolve([]) });
    });

    render(<MemoryRouter><Profile /></MemoryRouter>);

    await waitFor(() => {
      expect(screen.getByText('Cargando...')).toBeInTheDocument();
    });
  });
});
