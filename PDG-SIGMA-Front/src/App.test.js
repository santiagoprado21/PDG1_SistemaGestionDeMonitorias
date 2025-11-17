import { render, screen } from '@testing-library/react';
import App from './App';

test('muestra el formulario de inicio de sesión', () => {
  render(<App />);
  expect(screen.getByRole('button', { name: /iniciar sesión/i })).toBeInTheDocument();
});
