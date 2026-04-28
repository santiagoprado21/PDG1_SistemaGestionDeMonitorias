import React from 'react';

describe('index bootstrap', () => {
  afterEach(() => {
    jest.resetModules();
    jest.clearAllMocks();
    document.body.innerHTML = '';
  });

  test('monta App en el root con React.StrictMode', () => {
    const renderMock = jest.fn();
    const createRootMock = jest.fn(() => ({ render: renderMock }));

    const rootElement = document.createElement('div');
    rootElement.id = 'root';
    document.body.appendChild(rootElement);

    jest.isolateModules(() => {
      jest.doMock('react-dom/client', () => ({
        __esModule: true,
        default: {
          createRoot: createRootMock
        }
      }));

      jest.doMock('../App', () => ({
        __esModule: true,
        default: () => React.createElement('div', null, 'AppMock')
      }));

      require('../index');
    });

    expect(createRootMock).toHaveBeenCalledWith(rootElement);
    expect(renderMock).toHaveBeenCalledTimes(1);

    const renderedTree = renderMock.mock.calls[0][0];
    expect(renderedTree.type).toBe(React.StrictMode);
  });
});
