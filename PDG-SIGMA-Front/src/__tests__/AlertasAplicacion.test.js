import React from 'react';
import { render, screen, fireEvent, act } from '@testing-library/react';
import Alert from '../Alert';
import AlertElect from '../AlertElect';

describe('[TEST FRONT-END] Alertas de la Aplicacion', () => {
  describe('Alert.js', () => {
    test.each([
      ['success', 'Mensaje de exito', 'alert-success'],
      ['error', 'Mensaje de error', 'alert-error'],
      ['info', 'Mensaje informativo', 'alert-info'],
    ])('debe mostrar alerta tipo %s con estilo correcto', (type, message, className) => {
      render(<Alert show={true} onClose={jest.fn()} type={type} message={message} />);

      const alert = screen.getByRole('status');
      expect(alert).toHaveTextContent(message);
      expect(alert).toHaveClass(className);
    });

    test('debe cerrar la alerta por accion del usuario', () => {
      const onClose = jest.fn();
      render(<Alert show={true} onClose={onClose} message="Cerrar manualmente" />);

      fireEvent.click(screen.getByRole('button', { name: /cerrar alerta/i }));
      expect(onClose).toHaveBeenCalledTimes(1);
    });

    test('debe cerrar la alerta automaticamente despues del tiempo configurado', () => {
      jest.useFakeTimers();
      const onClose = jest.fn();

      render(<Alert show={true} onClose={onClose} autoCloseMs={3000} />);

      act(() => {
        jest.advanceTimersByTime(3000);
      });

      expect(onClose).toHaveBeenCalledTimes(1);
      jest.useRealTimers();
    });
  });

  describe('AlertElect.js', () => {
    test('debe mostrar mensaje del modulo y mantener patron visual', () => {
      render(<AlertElect show={true} onClose={jest.fn()} />);

      const alert = screen.getByRole('status');
      expect(alert).toHaveTextContent('La seleccion termino. Los estudiantes seran notificados');
      expect(alert).toHaveClass('alert-success');
      expect(screen.getByRole('button', { name: /cerrar alerta/i })).toBeInTheDocument();
    });

    test.each([
      ['success', 'alert-success'],
      ['error', 'alert-error'],
      ['info', 'alert-info'],
    ])('debe soportar variante visual %s', (type, className) => {
      render(<AlertElect show={true} onClose={jest.fn()} type={type} />);
      expect(screen.getByRole('status')).toHaveClass(className);
    });

    test('debe cerrar la alerta del modulo por accion del usuario', () => {
      const onClose = jest.fn();
      render(<AlertElect show={true} onClose={onClose} />);

      fireEvent.click(screen.getByRole('button', { name: /cerrar alerta/i }));
      expect(onClose).toHaveBeenCalledTimes(1);
    });
  });
});
