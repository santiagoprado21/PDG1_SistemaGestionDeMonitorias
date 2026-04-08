import React from 'react';
import { render, screen, fireEvent } from '@testing-library/react';
import { PopUp, PopupDelete } from '../PopUp';
import PopupCheck from '../PopUpCheck';

function ChainedPopupsHarness() {
    const [firstOpen, setFirstOpen] = React.useState(true);
    const [secondOpen, setSecondOpen] = React.useState(false);

    return (
        <>
            <PopUp show={firstOpen} onClose={() => { setFirstOpen(false); setSecondOpen(true); }}>
                Primer popup
            </PopUp>
            <PopupDelete
                show={secondOpen}
                onClose={() => setSecondOpen(false)}
                onApply={() => setSecondOpen(false)}
            />
        </>
    );
}

describe('Frontend: Popups y confirmación', () => {
    test('Debe permitir cancelar acción crítica en PopupDelete', () => {
        const onClose = jest.fn();
        const onApply = jest.fn();

        render(<PopupDelete show={true} onClose={onClose} onApply={onApply} />);

        fireEvent.click(screen.getByRole('button', { name: /Cancelar/i }));

        expect(onClose).toHaveBeenCalledTimes(1);
        expect(onApply).not.toHaveBeenCalled();
    });

    test('Debe prevenir confirmación accidental cuando no se acepta checkbox', () => {
        const onApply = jest.fn();

        render(<PopupCheck show={true} onClose={jest.fn()} onApply={onApply} />);

        const applyButton = screen.getByRole('button', { name: /Aplicar/i });
        expect(applyButton).toBeDisabled();

        fireEvent.click(applyButton);
        fireEvent.doubleClick(applyButton);
        expect(onApply).not.toHaveBeenCalled();
    });

    test('Debe soportar popups encadenados (cerrar uno y abrir el siguiente)', () => {
        render(<ChainedPopupsHarness />);

        expect(screen.getByText('Primer popup')).toBeInTheDocument();
        fireEvent.click(screen.getByRole('button', { name: 'OK' }));

        expect(screen.queryByText('Primer popup')).not.toBeInTheDocument();
        expect(screen.getByText(/¿Estás seguro de la acción a realizar?/i)).toBeInTheDocument();
    });
});

