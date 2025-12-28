import React from 'react';
import { fireEvent, render, screen } from '@testing-library/react';
import BagModal from './BagModal';

const bagTiles = [
  { letter: 'A', points: 1, blank: false, key: 'A' },
  { letter: null, points: 0, blank: true, key: 'BLANK' }
];

describe('BagModal', () => {
  it('renders bag info and closes', () => {
    // given
    const onClose = vi.fn();

    // when
    render(
      <BagModal
        bagCount={5}
        opponentRackSize={3}
        bagTiles={bagTiles}
        usedTiles={{ A: 1 }}
        onClose={onClose}
      />
    );
    fireEvent.click(screen.getByRole('button', { name: 'Close' }));

    // then
    expect(screen.getByText('Bag: 5 tiles')).toBeInTheDocument();
    expect(screen.getByText('Opponent rack: 3')).toBeInTheDocument();
    expect(onClose).toHaveBeenCalled();
  });
});
