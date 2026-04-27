import React from 'react';
import { createEvent, fireEvent, render, screen, waitFor } from '@testing-library/react';
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

describe('LoginHU212.monitor', () => {
  beforeEach(() => {
    jest.clearAllMocks();
    global.fetch = jest.fn();
    localStorage.clear();
  });

  afterEach(() => {
    jest.restoreAllMocks();
  });

  const fillCredentialsAndSubmit = async (userId = 'MON001', password = 'pass123') => {
    fireEvent.change(screen.getByLabelText(/usuario/i), { target: { value: userId } });
    fireEvent.change(screen.getByLabelText(/^Contraseña$/i), { target: { value: password } });
    fireEvent.click(screen.getByRole('button', { name: /iniciar sesi.n/i }));
  };

  test('logs in as monitor and redirects to open calls view', async () => {
    global.fetch.mockResolvedValueOnce({
      ok: true,
      json: async () => ({ role: 'monitor', token: 'token-monitor' })
    });
    render(<Login />);

    await fillCredentialsAndSubmit('MON001', 'pass123');

    await waitFor(() => expect(global.fetch).toHaveBeenCalledTimes(1));
    expect(global.fetch).toHaveBeenCalledWith(
      'http://localhost:5435/auth/login',
      expect.objectContaining({ method: 'POST' })
    );

    await waitFor(() => {
      expect(localStorage.getItem('userId')).toBe('MON001');
      expect(localStorage.getItem('role')).toBe('monitor');
      expect(localStorage.getItem('token')).toBe('Bearer token-monitor');
    });

    expect(await screen.findByTestId('login-alert')).toHaveTextContent(/iniciaste sesi.n como estudiante/i);
    await waitFor(() => {
      expect(mockNavigate).toHaveBeenCalledWith('/ver-convocatorias');
    });
  });

  test('shows invalid credentials message when backend rejects login', async () => {
    global.fetch.mockResolvedValueOnce({ ok: false });
    render(<Login />);

    await fillCredentialsAndSubmit('MON001', 'wrong-pass');

    await waitFor(() => {
      expect(screen.getByText(/contraseña o usuario inválido/i)).toBeInTheDocument();
    });

    expect(mockNavigate).not.toHaveBeenCalled();
  });

  test('logs in as professor and redirects to professor calls', async () => {
    global.fetch.mockResolvedValueOnce({
      ok: true,
      json: async () => ({ role: 'professor', token: 'token-prof' })
    });
    render(<Login />);

    await fillCredentialsAndSubmit('PROF001', 'pass123');

    await waitFor(() => {
      expect(localStorage.getItem('role')).toBe('professor');
      expect(localStorage.getItem('token')).toBe('Bearer token-prof');
      expect(mockNavigate).toHaveBeenCalledWith('/mis-convocatorias');
    });

    expect(screen.getByTestId('login-alert')).toHaveTextContent(/iniciaste sesi.n como profesor/i);
  });

  test('logs in as head department and redirects to approvals', async () => {
    global.fetch.mockResolvedValueOnce({
      ok: true,
      json: async () => ({ role: 'jfedpto', token: 'token-head' })
    });
    render(<Login />);

    await fillCredentialsAndSubmit('HEAD001', 'pass123');

    await waitFor(() => {
      expect(localStorage.getItem('role')).toBe('jfedpto');
      expect(localStorage.getItem('token')).toBe('Bearer token-head');
      expect(mockNavigate).toHaveBeenCalledWith('/aprobar-monitorias-hu010');
    });

    expect(screen.getByTestId('login-alert')).toHaveTextContent(/jefe de departamento/i);
  });

  test('logs in as student and uses default student route', async () => {
    global.fetch.mockResolvedValueOnce({
      ok: true,
      json: async () => ({ role: 'student', token: 'token-student' })
    });
    render(<Login />);

    await fillCredentialsAndSubmit('STD001', 'pass123');

    await waitFor(() => {
      expect(localStorage.getItem('role')).toBe('student');
      expect(localStorage.getItem('token')).toBe('Bearer token-student');
      expect(mockNavigate).toHaveBeenCalledWith('/ver-convocatorias');
    });

    expect(screen.getByTestId('login-alert')).toHaveTextContent(/iniciaste sesi.n como estudiante/i);
  });

  test('uses generic message for unknown roles', async () => {
    global.fetch.mockResolvedValueOnce({
      ok: true,
      json: async () => ({ role: 'guest', token: 'token-guest' })
    });
    render(<Login />);

    await fillCredentialsAndSubmit('GST001', 'pass123');

    await waitFor(() => {
      expect(localStorage.getItem('role')).toBe('guest');
      expect(mockNavigate).toHaveBeenCalledWith('/ver-convocatorias');
    });

    expect(screen.getByTestId('login-alert')).toHaveTextContent(/^iniciaste sesi.n$/i);
  });

  test('toggles password visibility and caps lock warning', async () => {
    render(<Login />);

    const passwordInput = screen.getByLabelText(/^Contraseña$/i);
    expect(passwordInput).toHaveAttribute('type', 'password');

    fireEvent.click(screen.getByRole('button', { name: /mostrar contraseña/i }));
    expect(passwordInput).toHaveAttribute('type', 'text');

    const capsEvent = createEvent.keyDown(passwordInput, { key: 'A', code: 'KeyA' });
    capsEvent.getModifierState = (key) => key === 'CapsLock';
    fireEvent(passwordInput, capsEvent);

    await waitFor(() => {
      expect(screen.getByText(/bloq mayus esta activado/i)).toBeInTheDocument();
    });

    fireEvent.blur(passwordInput);
    await waitFor(() => {
      expect(screen.queryByText(/bloq mayus esta activado/i)).not.toBeInTheDocument();
    });

    fireEvent.click(screen.getByRole('button', { name: /ocultar contraseña/i }));
    expect(passwordInput).toHaveAttribute('type', 'password');
  });

  test('handles fetch errors without navigating', async () => {
    const consoleErrorSpy = jest.spyOn(console, 'error').mockImplementation(() => {});
    global.fetch.mockRejectedValueOnce(new Error('network down'));

    render(<Login />);
    await fillCredentialsAndSubmit('MON001', 'pass123');

    await waitFor(() => {
      expect(consoleErrorSpy).toHaveBeenCalled();
    });
    expect(mockNavigate).not.toHaveBeenCalled();
  });
});
