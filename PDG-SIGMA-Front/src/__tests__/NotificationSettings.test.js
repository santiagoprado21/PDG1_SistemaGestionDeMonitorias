import React from 'react';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import NotificationSettings from '../NotificationSettings';

jest.mock('../VerticalNavbar', () => () => <nav data-testid="navbar" />);

describe('NotificationSettings', () => {
  beforeEach(() => {
    jest.useFakeTimers();
    localStorage.clear();
  });

  afterEach(() => {
    jest.runOnlyPendingTimers();
    jest.useRealTimers();
    jest.resetAllMocks();
    localStorage.clear();
  });

  it('carga preferencias guardadas desde localStorage', async () => {
    localStorage.setItem('sigmaNotif.sound', JSON.stringify(false));
    localStorage.setItem('sigmaNotif.types', JSON.stringify({
      PROGRESS_UPDATE: false,
      COMPLETED: true,
      OVERDUE: false,
      DUE_SOON: true
    }));

    render(<NotificationSettings />);

    const soundCheckbox = await screen.findByLabelText(/activar sonido en nuevas notificaciones/i);
    expect(soundCheckbox).not.toBeChecked();

    expect(screen.getByLabelText(/progreso/i)).not.toBeChecked();
    expect(screen.getByLabelText(/completadas/i)).toBeChecked();
    expect(screen.getByLabelText(/atrasos/i)).not.toBeChecked();
    expect(screen.getByLabelText(/pr[oó]ximas a vencer/i)).toBeChecked();
  });

  it('guarda cambios y muestra confirmacion temporal', async () => {
    render(<NotificationSettings />);

    const soundCheckbox = await screen.findByLabelText(/activar sonido en nuevas notificaciones/i);
    fireEvent.click(soundCheckbox);
    fireEvent.click(screen.getByLabelText(/completadas/i));
    fireEvent.click(screen.getByRole('button', { name: /guardar/i }));

    expect(await screen.findByText(/preferencias guardadas/i)).toBeInTheDocument();

    expect(JSON.parse(localStorage.getItem('sigmaNotif.sound'))).toBe(false);
    const savedTypes = JSON.parse(localStorage.getItem('sigmaNotif.types'));
    expect(savedTypes.COMPLETED).toBe(false);

    jest.advanceTimersByTime(1500);
    await waitFor(() => {
      expect(screen.queryByText(/preferencias guardadas/i)).not.toBeInTheDocument();
    });
  });
});
