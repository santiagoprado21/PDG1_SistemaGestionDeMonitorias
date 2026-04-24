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

describe('LoginHU212.jefe', () => {
  beforeEach(() => {
    jest.clearAllMocks();
    global.fetch = jest.fn();
    localStorage.clear();
  });

  test('logs in as head department and redirects to approvals view', async () => {
    global.fetch.mockResolvedValueOnce({
      ok: true,
      json: async () => ({ role: 'jfedpto', token: 'token-head' })
    });

    render(<Login />);

    fireEvent.change(screen.getByLabelText(/usuario/i), { target: { value: 'HEAD001' } });
    fireEvent.change(screen.getByLabelText(/contrasena/i), { target: { value: 'pass123' } });
    fireEvent.click(screen.getByRole('button', { name: /iniciar sesi.n/i }));

    await waitFor(() => {
      expect(localStorage.getItem('userId')).toBe('HEAD001');
      expect(localStorage.getItem('role')).toBe('jfedpto');
      expect(localStorage.getItem('token')).toBe('Bearer token-head');
      expect(mockNavigate).toHaveBeenCalledWith('/aprobar-monitorias-hu010');
    });

    expect(await screen.findByTestId('login-alert')).toHaveTextContent(/jefe de departamento/i);
  });

  test('handles network error in jfedpto login without navigation', async () => {
    const consoleErrorSpy = jest.spyOn(console, 'error').mockImplementation(() => {});
    global.fetch.mockRejectedValueOnce(new Error('network down'));

    render(<Login />);

    fireEvent.change(screen.getByLabelText(/usuario/i), { target: { value: 'HEAD001' } });
    fireEvent.change(screen.getByLabelText(/contrasena/i), { target: { value: 'pass123' } });
    fireEvent.click(screen.getByRole('button', { name: /iniciar sesi.n/i }));

    await waitFor(() => {
      expect(consoleErrorSpy).toHaveBeenCalled();
    });

    expect(mockNavigate).not.toHaveBeenCalled();
  });
});
