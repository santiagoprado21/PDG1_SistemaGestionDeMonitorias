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
});
