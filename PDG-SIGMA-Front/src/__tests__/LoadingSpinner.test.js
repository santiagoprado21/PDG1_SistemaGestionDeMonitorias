import React from 'react';
import { render, screen } from '@testing-library/react';
import LoadingSpinner from '../LoadingSpinner';

describe('LoadingSpinner', () => {
  test('renderiza overlay, spinner y punto animado', () => {
    const { container } = render(<LoadingSpinner />);

    expect(container.querySelector('.spinner-overlay')).toBeInTheDocument();
    expect(screen.getByLabelText('Cargando')).toBeInTheDocument();
    expect(container.querySelector('.spinner-dot')).toBeInTheDocument();
  });
});
