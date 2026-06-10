import React from 'react';
import { render, screen, waitFor, fireEvent } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import Chat from '../Chat';

jest.mock('../VerticalNavbar', () => () => <nav data-testid="navbar" />);
jest.mock('../config/ApiBackend', () => ({ BACKEND_URL: 'http://localhost:5435' }));

describe('ChatHU', () => {
  beforeEach(() => {
    jest.clearAllMocks();
    URL.createObjectURL = jest.fn(() => 'blob:mock');
    URL.revokeObjectURL = jest.fn();
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

  it('jefe de departamento puede ver chat con profesor y enviar mensaje', async () => {
    window.localStorage.setItem('role', 'jfedpto');
    window.localStorage.setItem('userId', 'H1');

    global.fetch = jest.fn((url, options = {}) => {
      const method = (options.method || 'GET').toUpperCase();

      if (url.includes('/chat/conversations/H1/jfedpto')) {
        return Promise.resolve({
          ok: true,
          json: () => Promise.resolve([
            { id: 'head-H1__prof-P1', title: 'Profesor: Pedro Ruiz', subtitle: 'Chat directo', otherUserId: 'P1' }
          ])
        });
      }

      if (url.includes('/chat/messages/head-H1__prof-P1') && method === 'GET') {
        return Promise.resolve({
          ok: true,
          json: () => Promise.resolve([])
        });
      }

      if (url.endsWith('/chat/messages') && method === 'POST') {
        return Promise.resolve({
          ok: true,
          json: () => Promise.resolve({
            id: 7,
            conversationId: 'head-H1__prof-P1',
            senderId: 'H1',
            senderRole: 'jfedpto',
            receiverId: 'P1',
            message: 'Mensaje desde jefatura',
            createdAt: '2026-03-07T10:00:00.000Z',
            attachments: []
          })
        });
      }

      return Promise.resolve({ ok: true, json: () => Promise.resolve([]) });
    });

    render(<Chat />);

    expect(await screen.findByText('Comunicacion directa con profesores')).toBeInTheDocument();
    const professorTitles = await screen.findAllByText('Profesor: Pedro Ruiz');
    expect(professorTitles.length).toBeGreaterThan(0);

    const textarea = screen.getByPlaceholderText('Escribe un mensaje relacionado con la actividad...');
    await userEvent.type(textarea, 'Mensaje desde jefatura');
    await userEvent.click(screen.getByRole('button', { name: 'Enviar' }));

    await waitFor(() => {
      const postCall = global.fetch.mock.calls.find(
        call => call[0] === 'http://localhost:5435/chat/messages' && call[1]?.method === 'POST'
      );
      expect(postCall).toBeTruthy();
      const sentBody = JSON.parse(postCall[1].body);
      expect(sentBody.senderRole).toBe('jfedpto');
      expect(sentBody.receiverId).toBe('P1');
    });
  });

  it('muestra mensaje cuando no hay conversaciones', async () => {
    global.fetch = jest.fn((url) => {
      if (url.includes('/chat/conversations/')) {
        return Promise.resolve({
          ok: true,
          json: () => Promise.resolve([])
        });
      }
      return Promise.resolve({ ok: true, json: () => Promise.resolve([]) });
    });
    render(<Chat />);
    expect(await screen.findByText('No tienes chats disponibles.')).toBeInTheDocument();
  });

  it('enfoca conversación específica desde localStorage', async () => {
    window.localStorage.setItem('focusChatConversationId', 'prof-P1__mon-M1');
    render(<Chat />);
    await waitFor(async () => {
      const titles = await screen.findAllByText('Monitor: Ana Gómez');
      expect(titles.length).toBeGreaterThan(0);
    });
    expect(window.localStorage.getItem('focusChatConversationId')).toBeNull();
  });

  it('muestra error al fallar carga de conversaciones', async () => {
    global.fetch = jest.fn((url) => {
      if (url.includes('/chat/conversations/')) {
        return Promise.resolve({
          ok: false,
          status: 500,
          text: () => Promise.resolve('Error al cargar conversaciones')
        });
      }
      return Promise.resolve({ ok: true, json: () => Promise.resolve([]) });
    });
    render(<Chat />);
    expect(await screen.findByText('Error al cargar conversaciones')).toBeInTheDocument();
  });

  it('muestra error al fallar carga de mensajes', async () => {
    global.fetch = jest.fn((url, options = {}) => {
      const method = (options.method || 'GET').toUpperCase();
      if (url.includes('/chat/conversations/')) {
        return Promise.resolve({
          ok: true,
          json: () => Promise.resolve([
            { id: 'conv1', title: 'Conv 1', subtitle: 'Chat 1', otherUserId: 'U1' }
          ])
        });
      }
      if (url.includes('/chat/messages/conv1') && method === 'GET') {
        return Promise.resolve({
          ok: false,
          status: 500,
          text: () => Promise.resolve('Error al cargar mensajes')
        });
      }
      return Promise.resolve({ ok: true, json: () => Promise.resolve([]) });
    });
    render(<Chat />);
    expect(await screen.findByText('Error al cargar mensajes')).toBeInTheDocument();
  });

  it('maneja error de red al cargar mensajes', async () => {
    global.fetch = jest.fn((url, options = {}) => {
      const method = (options.method || 'GET').toUpperCase();
      if (url.includes('/chat/conversations/')) {
        return Promise.resolve({
          ok: true,
          json: () => Promise.resolve([
            { id: 'conv1', title: 'Conv 1', subtitle: 'Chat 1', otherUserId: 'U1' }
          ])
        });
      }
      if (url.includes('/chat/messages/conv1') && method === 'GET') {
        return Promise.reject(new Error('Error de red'));
      }
      return Promise.resolve({ ok: true, json: () => Promise.resolve([]) });
    });
    render(<Chat />);
    expect(await screen.findByText('Error de red')).toBeInTheDocument();
  });

  it('selecciona otra conversación al hacer clic', async () => {
    global.fetch = jest.fn((url, options = {}) => {
      const method = (options.method || 'GET').toUpperCase();
      if (url.includes('/chat/conversations/')) {
        return Promise.resolve({
          ok: true,
          json: () => Promise.resolve([
            { id: 'conv1', title: 'Conv 1', subtitle: 'Chat 1', otherUserId: 'U1' },
            { id: 'conv2', title: 'Conv 2', subtitle: 'Chat 2', otherUserId: 'U2' }
          ])
        });
      }
      if (url.includes('/chat/messages/conv1') && method === 'GET') {
        return Promise.resolve({
          ok: true,
          json: () => Promise.resolve([
            { id: 1, conversationId: 'conv1', senderId: 'U1', senderRole: 'monitor', receiverId: 'P1', message: 'Msg conv1', createdAt: '2026-01-01T00:00:00.000Z', attachments: [] }
          ])
        });
      }
      if (url.includes('/chat/messages/conv2') && method === 'GET') {
        return Promise.resolve({
          ok: true,
          json: () => Promise.resolve([
            { id: 2, conversationId: 'conv2', senderId: 'U2', senderRole: 'monitor', receiverId: 'P1', message: 'Msg conv2', createdAt: '2026-01-01T00:00:00.000Z', attachments: [] }
          ])
        });
      }
      return Promise.resolve({ ok: true, json: () => Promise.resolve([]) });
    });
    render(<Chat />);
    await waitFor(async () => {
      const c1 = await screen.findAllByText('Conv 1');
      expect(c1.length).toBeGreaterThan(0);
    });
    expect(await screen.findByText('Conv 2')).toBeInTheDocument();
    expect(await screen.findByText('Msg conv1')).toBeInTheDocument();
    await userEvent.click(screen.getByText('Conv 2'));
    await waitFor(async () => {
      const titles = await screen.findAllByText('Msg conv2');
      expect(titles.length).toBeGreaterThan(0);
    });
  });

  it('adjunta y remueve archivos', async () => {
    render(<Chat />);
    await waitFor(async () => {
      const titles = await screen.findAllByText('Monitor: Ana Gómez');
      expect(titles.length).toBeGreaterThan(0);
    });
    const attachBtn = screen.getByTitle('Adjuntar archivos');
    await userEvent.click(attachBtn);
    const file = new File(['demo'], 'test.png', { type: 'image/png' });
    const fileInput = document.querySelector('input[type="file"]');
    fireEvent.change(fileInput, { target: { files: [file] } });
    await waitFor(() => {
      expect(screen.getByText('test.png')).toBeInTheDocument();
    });
    const removeBtn = document.querySelector('.remove-attachment-btn');
    await userEvent.click(removeBtn);
    await waitFor(() => {
      expect(screen.queryByText('test.png')).toBeNull();
    });
  });

  it('muestra mensaje con adjuntos', async () => {
    global.fetch = jest.fn((url, options = {}) => {
      const method = (options.method || 'GET').toUpperCase();
      if (url.includes('/chat/conversations/')) {
        return Promise.resolve({
          ok: true,
          json: () => Promise.resolve([
            { id: 'conv1', title: 'Conv 1', subtitle: 'Chat 1', otherUserId: 'U1' }
          ])
        });
      }
      if (url.includes('/chat/messages/conv1') && method === 'GET') {
        return Promise.resolve({
          ok: true,
          json: () => Promise.resolve([
            {
              id: 1,
              conversationId: 'conv1',
              senderId: 'U1',
              senderRole: 'monitor',
              receiverId: 'P1',
              message: 'Mensaje con adjunto',
              createdAt: '2026-01-01T00:00:00.000Z',
              attachments: [
                { name: 'doc.pdf', size: 2048, contentType: 'application/pdf', url: '/uploads/doc.pdf' },
                { name: 'image.png', size: 1024, contentType: 'image/png', url: 'http://example.com/img.png' }
              ]
            }
          ])
        });
      }
      return Promise.resolve({ ok: true, json: () => Promise.resolve([]) });
    });
    render(<Chat />);
    expect(await screen.findByText('Mensaje con adjunto')).toBeInTheDocument();
    expect(await screen.findByText('doc.pdf')).toBeInTheDocument();
    expect(await screen.findByText('image.png')).toBeInTheDocument();
    expect(await screen.findByText('2 KB')).toBeInTheDocument();
  });

  it('muestra error al fallar envío de mensaje', async () => {
    render(<Chat />);
    await waitFor(async () => {
      const titles = await screen.findAllByText('Monitor: Ana Gómez');
      expect(titles.length).toBeGreaterThan(0);
    });
    const originalFetch = global.fetch;
    global.fetch = jest.fn((url, options = {}) => {
      const method = (options.method || 'GET').toUpperCase();
      if (method === 'POST' && (url.endsWith('/chat/messages') || url.endsWith('/chat/messages/with-attachments'))) {
        return Promise.resolve({
          ok: false,
          status: 500,
          text: () => Promise.resolve('Error al enviar mensaje')
        });
      }
      return originalFetch(url, options);
    });
    const textarea = screen.getByPlaceholderText('Escribe un mensaje relacionado con la actividad...');
    await userEvent.type(textarea, 'Test message');
    await userEvent.click(screen.getByRole('button', { name: 'Enviar' }));
    expect(await screen.findByText('Error al enviar mensaje')).toBeInTheDocument();
  });

  it('maneja scroll en los mensajes', async () => {
    render(<Chat />);
    await waitFor(async () => {
      const titles = await screen.findAllByText('Monitor: Ana Gómez');
      expect(titles.length).toBeGreaterThan(0);
    });
    const container = document.querySelector('.chat-messages');
    fireEvent.scroll(container);
  });

  it('inicia polling al seleccionar conversación', async () => {
    jest.spyOn(global, 'setInterval');
    global.fetch = jest.fn((url, options = {}) => {
      const method = (options.method || 'GET').toUpperCase();
      if (url.includes('/chat/conversations/')) {
        return Promise.resolve({
          ok: true,
          json: () => Promise.resolve([
            { id: 'conv1', title: 'Conv 1', subtitle: 'Chat 1', otherUserId: 'U1' }
          ])
        });
      }
      if (url.includes('/chat/messages/conv1') && method === 'GET') {
        return Promise.resolve({
          ok: true,
          json: () => Promise.resolve([
            { id: 1, conversationId: 'conv1', senderId: 'U1', senderRole: 'monitor', receiverId: 'P1', message: 'Msg conv1', createdAt: '2026-01-01T00:00:00.000Z', attachments: [] }
          ])
        });
      }
      return Promise.resolve({ ok: true, json: () => Promise.resolve([]) });
    });
    render(<Chat />);
    await waitFor(async () => {
      const titles = await screen.findAllByText('Conv 1');
      expect(titles.length).toBeGreaterThan(0);
    });
    expect(setInterval).toHaveBeenCalledWith(expect.any(Function), 10000);
    global.fetch.mockClear();
    const intervalCallback = setInterval.mock.calls.find(call => call[1] === 10000)[0];
    intervalCallback();
    await waitFor(() => {
      expect(global.fetch).toHaveBeenCalledWith(
        'http://localhost:5435/chat/messages/conv1',
        expect.objectContaining({ method: 'GET' })
      );
    });
    expect(global.fetch).toHaveBeenCalledWith(
      'http://localhost:5435/chat/conversations/P1/professor',
      expect.objectContaining({ method: 'GET' })
    );
  });
});
