import React from 'react';
import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import Login from '../Login';

const mockNavigate = jest.fn();

jest.mock('react-router-dom', () => ({
  ...jest.requireActual('react-router-dom'),
  useNavigate: () => mockNavigate
}));

jest.mock('../Alert', () => ({
  __esModule: true,
  default: ({ show, message }) => (show ? <div data-testid="login-alert">{message}</div> : null)
}));

jest.mock('../config/ApiBackend', () => ({
  BACKEND_URL: 'http://localhost:5435'
}));

describe('LoginHU212.professor', () => {
  beforeEach(() => {
    jest.clearAllMocks();
    global.fetch = jest.fn();
    localStorage.clear();
  });

  test('logs in as professor and redirects to own calls view', async () => {
    global.fetch.mockResolvedValueOnce({
      ok: true,
      json: async () => ({ role: 'professor', token: 'token-prof' })
    });

    render(<Login />);

    fireEvent.change(screen.getByLabelText(/usuario/i), { target: { value: 'PROF001' } });
    fireEvent.change(screen.getByLabelText(/contrasena/i), { target: { value: 'pass123' } });
    fireEvent.click(screen.getByRole('button', { name: /iniciar sesi.n/i }));

    await waitFor(() => {
      expect(localStorage.getItem('userId')).toBe('PROF001');
      expect(localStorage.getItem('role')).toBe('professor');
      expect(localStorage.getItem('token')).toBe('Bearer token-prof');
      expect(mockNavigate).toHaveBeenCalledWith('/mis-convocatorias');
    });

    expect(await screen.findByTestId('login-alert')).toHaveTextContent(/iniciaste sesion como profesor/i);
  });

  test('shows invalid credentials for professor login failure', async () => {
    global.fetch.mockResolvedValueOnce({ ok: false });

    render(<Login />);

    fireEvent.change(screen.getByLabelText(/usuario/i), { target: { value: 'PROF001' } });
    fireEvent.change(screen.getByLabelText(/contrasena/i), { target: { value: 'wrong-pass' } });
    fireEvent.click(screen.getByRole('button', { name: /iniciar sesi.n/i }));

    await waitFor(() => {
      expect(screen.getByText(/contraseña o usuario inválido/i)).toBeInTheDocument();
    });

    expect(mockNavigate).not.toHaveBeenCalled();
  });
});
