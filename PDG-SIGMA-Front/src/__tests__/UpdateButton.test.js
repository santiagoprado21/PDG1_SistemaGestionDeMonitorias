import React from 'react';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import UpdateButton from '../UpdateButton';

jest.mock('../PopUp', () => ({
  PopUp: ({ show, children }) => (show ? <div data-testid="popup">{children}</div> : null)
}));

jest.mock('../config/ApiBackend', () => ({
  BACKEND_URL: 'http://localhost:5433'
}));

describe('UpdateButton', () => {
  beforeEach(() => {
    global.fetch = jest.fn(() =>
      Promise.resolve({
        text: () => Promise.resolve('OK')
      })
    );
    localStorage.setItem('token', 'Bearer token-test');
  });

  afterEach(() => {
    jest.resetAllMocks();
    localStorage.clear();
  });

  it('envia actualizacion para profesor en el mismo semestre', async () => {
    render(<UpdateButton role="professor" userId="PROF-1" />);

    fireEvent.click(screen.getByRole('button', { name: /actualizar/i }));
    fireEvent.click(screen.getByRole('button', { name: /confirmar actualizaci[oó]n/i }));

    await waitFor(() => expect(global.fetch).toHaveBeenCalledTimes(1));

    const [, options] = global.fetch.mock.calls[0];
    const payload = JSON.parse(options.body);

    expect(payload).toEqual({
      updateType: 'sameSemester',
      professorId: 'PROF-1',
      departmentHeadId: null,
      removeMonitors: false
    });

    expect(await screen.findByTestId('popup')).toHaveTextContent(/estado : ok/i);
  });

  it('permite nuevo semestre para jefe cuando confirma y envia removeMonitors=true', async () => {
    const confirmSpy = jest.spyOn(window, 'confirm').mockReturnValue(true);

    render(<UpdateButton role="jfedpto" userId="JFE-1" />);

    fireEvent.click(screen.getByRole('button', { name: /actualizar/i }));
    fireEvent.click(screen.getByRole('radio', { name: /reiniciar para nuevo semestre/i }));
    fireEvent.click(screen.getByRole('button', { name: /confirmar actualizaci[oó]n/i }));

    await waitFor(() => expect(global.fetch).toHaveBeenCalledTimes(1));

    const [, options] = global.fetch.mock.calls[0];
    const payload = JSON.parse(options.body);

    expect(payload).toEqual({
      updateType: 'newSemester',
      professorId: null,
      departmentHeadId: 'JFE-1',
      removeMonitors: true
    });

    confirmSpy.mockRestore();
  });

  it('si el jefe cancela confirmacion, mantiene sameSemester', async () => {
    const confirmSpy = jest.spyOn(window, 'confirm').mockReturnValue(false);

    render(<UpdateButton role="jfedpto" userId="JFE-1" />);

    fireEvent.click(screen.getByRole('button', { name: /actualizar/i }));
    fireEvent.click(screen.getByRole('radio', { name: /reiniciar para nuevo semestre/i }));
    fireEvent.click(screen.getByRole('button', { name: /confirmar actualizaci[oó]n/i }));

    await waitFor(() => expect(global.fetch).toHaveBeenCalledTimes(1));

    const [, options] = global.fetch.mock.calls[0];
    const payload = JSON.parse(options.body);
    expect(payload.updateType).toBe('sameSemester');
    expect(payload.removeMonitors).toBe(false);

    confirmSpy.mockRestore();
  });
});
