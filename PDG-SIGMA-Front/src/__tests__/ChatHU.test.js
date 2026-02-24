import React from 'react';
import { render, screen, waitFor, fireEvent } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import Chat from '../Chat';

jest.mock('../VerticalNavbar', () => () => <nav data-testid="navbar" />);
jest.mock('../config/ApiBackend', () => ({ BACKEND_URL: 'http://localhost:5435' }));

describe('Chat HU tests', () => {
  beforeEach(() => {
    jest.clearAllMocks();
    window.localStorage.clear();
    window.localStorage.setItem('role', 'professor');
    window.localStorage.setItem('userId', 'P1');
    window.localStorage.setItem('token', 'Bearer token');

    global.fetch = jest.fn((url, options = {}) => {
      const method = (options.method || 'GET').toUpperCase();

      if (url.includes('/chat/conversations/')) {
        return Promise.resolve({
          ok: true,
          json: () => Promise.resolve([
            { id: 'prof-P1__mon-M1', title: 'Monitor: Ana Gómez', subtitle: 'Chat directo', otherUserId: 'M1' }
          ])
        });
      }

      if (url.includes('/chat/messages/prof-P1__mon-M1') && method === 'GET') {
        return Promise.resolve({
          ok: true,
          json: () => Promise.resolve([
            {
              id: 1,
              conversationId: 'prof-P1__mon-M1',
              senderId: 'M1',
              senderRole: 'monitor',
              receiverId: 'P1',
              activityId: 88,
              message: 'Profe, duda de la actividad',
              createdAt: '2026-02-23T10:00:00.000Z',
              attachments: []
            }
          ])
        });
      }

      if (url.endsWith('/chat/messages') && method === 'POST') {
        return Promise.resolve({
          ok: true,
          json: () => Promise.resolve({
            id: 2,
            conversationId: 'prof-P1__mon-M1',
            senderId: 'P1',
            senderRole: 'professor',
            receiverId: 'M1',
            message: 'Respuesta profesor',
            createdAt: '2026-02-23T10:01:00.000Z',
            attachments: []
          })
        });
      }

      if (url.endsWith('/chat/messages/with-attachments') && method === 'POST') {
        return Promise.resolve({
          ok: true,
          json: () => Promise.resolve({
            id: 3,
            conversationId: 'prof-P1__mon-M1',
            senderId: 'P1',
            senderRole: 'professor',
            receiverId: 'M1',
            message: '',
            createdAt: '2026-02-23T10:02:00.000Z',
            attachments: [{ id: 10, name: 'evidencia.pdf', size: 2048, contentType: 'application/pdf', url: '/chat/attachments/10' }]
          })
        });
      }

      return Promise.resolve({ ok: true, json: () => Promise.resolve([]) });
    });
  });

  it('muestra historial de mensajes de la conversación', async () => {
    render(<Chat />);

    await waitFor(async () => {
      const titles = await screen.findAllByText('Monitor: Ana Gómez');
      expect(titles.length).toBeGreaterThan(0);
    });

    expect(await screen.findByText('Profe, duda de la actividad')).toBeInTheDocument();
  });

  it('envía mensaje de texto al endpoint de chat', async () => {
    render(<Chat />);

    await waitFor(async () => {
      const titles = await screen.findAllByText('Monitor: Ana Gómez');
      expect(titles.length).toBeGreaterThan(0);
    });

    const textarea = screen.getByPlaceholderText('Escribe un mensaje relacionado con la actividad...');
    await userEvent.type(textarea, 'Respuesta profesor');

    await userEvent.click(screen.getByRole('button', { name: 'Enviar' }));

    await waitFor(() => {
      expect(global.fetch).toHaveBeenCalledWith(
        'http://localhost:5435/chat/messages',
        expect.objectContaining({
          method: 'POST',
          headers: expect.objectContaining({ 'Content-Type': 'application/json' })
        })
      );
    });
  });

  it('envía mensaje con adjunto al endpoint multipart', async () => {
    render(<Chat />);

    await waitFor(async () => {
      const titles = await screen.findAllByText('Monitor: Ana Gómez');
      expect(titles.length).toBeGreaterThan(0);
    });

    const file = new File(['demo'], 'evidencia.pdf', { type: 'application/pdf' });
    const fileInput = document.querySelector('input[type="file"]');
    fireEvent.change(fileInput, { target: { files: [file] } });

    await userEvent.click(screen.getByRole('button', { name: 'Enviar' }));

    await waitFor(() => {
      expect(global.fetch).toHaveBeenCalledWith(
        'http://localhost:5435/chat/messages/with-attachments',
        expect.objectContaining({ method: 'POST' })
      );
    });
  });
});
