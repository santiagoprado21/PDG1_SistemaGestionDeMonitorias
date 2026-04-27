import React from 'react';
import { render, screen } from '@testing-library/react';
import { Bell, Card, CardContent } from '../CustomComponents';

describe('CustomComponents', () => {
  test('Bell renderiza con atributos por defecto', () => {
    const { container } = render(<Bell />);
    const svg = container.querySelector('svg.bell-icon');

    expect(svg).toBeInTheDocument();
    expect(svg).toHaveAttribute('width', '24');
    expect(svg).toHaveAttribute('height', '24');
    expect(svg).toHaveAttribute('stroke', 'var(--color-primary)');
    expect(container.querySelectorAll('path')).toHaveLength(2);
  });

  test('Bell aplica propiedades personalizadas', () => {
    const { container } = render(<Bell size={32} color="#123456" />);
    const svg = container.querySelector('svg.bell-icon');

    expect(svg).toHaveAttribute('width', '32');
    expect(svg).toHaveAttribute('height', '32');
    expect(svg).toHaveAttribute('stroke', '#123456');
  });

  test('Card y CardContent renderizan children', () => {
    render(
      <Card>
        <CardContent>
          <p>Contenido interno</p>
        </CardContent>
      </Card>
    );

    expect(screen.getByText('Contenido interno')).toBeInTheDocument();
  });
});
