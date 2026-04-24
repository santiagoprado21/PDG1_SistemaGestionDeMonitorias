import React from 'react';
import { render, screen } from '@testing-library/react';
import App from '../App';

jest.mock('../Login', () => () => <div data-testid="login-page">Login Mock</div>);
jest.mock('../CreateActivity', () => () => <div data-testid="create-activity-page">CreateActivity Mock</div>);
jest.mock('../VistaMonitorActividades', () => () => <div data-testid="mis-actividades-page">Mis Actividades Mock</div>);

jest.mock('../Profile', () => () => <div />);
jest.mock('../Reports', () => () => <div />);
jest.mock('../GenerateSimonFile', () => () => <div />);
jest.mock('../NotificationSettings', () => () => <div />);
jest.mock('../EvaluarMonitoresHU015', () => () => <div />);
jest.mock('../EvaluarSupervisorHU021', () => () => <div />);
jest.mock('../MisEvaluacionesHU015', () => () => <div />);
jest.mock('../EvaluacionMonitoriaEstudiante', () => () => <div />);
jest.mock('../Chat', () => () => <div />);
jest.mock('../CreateConvocatoria', () => () => <div />);
jest.mock('../MisConvocatorias', () => () => <div />);
jest.mock('../VerConvocatorias', () => () => <div />);
jest.mock('../SeleccionarMonitor', () => () => <div />);
jest.mock('../AprobarMonitoriasHU010', () => () => <div />);
jest.mock('../CreateMonitoria', () => () => <div />);
jest.mock('../PlanActividades', () => () => <div />);
jest.mock('../GestionRubricas', () => () => <div />);
jest.mock('../MisPostulaciones', () => () => <div />);
jest.mock('../CerrarMonitorias', () => () => <div />);

describe('InfraestructuraArranqueApp', () => {
    beforeEach(() => {
        window.history.pushState({}, '', '/');
    });

    test('debe redirigir de "/" a "/Login" y montar pantalla de login', async () => {
        render(<App />);
        expect(await screen.findByTestId('login-page')).toBeInTheDocument();
    });

    test('debe montar rutas principales sin crash', async () => {
        window.history.pushState({}, '', '/mis-actividades');
        render(<App />);
        expect(await screen.findByTestId('mis-actividades-page')).toBeInTheDocument();
    });
});

