import React from 'react';
import { fireEvent, render, screen } from '@testing-library/react';
import GameInfoPanel from './GameInfoPanel';
import type { GameSnapshot } from '../../../types';

describe('GameInfoPanel', () => {
  const snapshot: GameSnapshot = {
    roomId: 'room-1',
    status: 'not_started',
    players: [],
    bagCount: 90,
    boardTiles: 0,
    board: [],
    currentPlayerIndex: 0,
    pendingMove: false,
    pending: null,
    winner: null
  };

  it('shows start button and triggers handler', () => {
    // given
    const onStart = vi.fn();

    // when
    render(
      <GameInfoPanel
        snapshot={snapshot}
        player="Alice"
        error={null}
        busy={false}
        isSocketReady
        onStartGame={onStart}
        onOpenBag={vi.fn()}
      />
    );
    fireEvent.click(screen.getByRole('button', { name: 'Start' }));

    // then
    expect(onStart).toHaveBeenCalled();
  });

  it('disables start when socket not ready', () => {
    // given
    render(
      <GameInfoPanel
        snapshot={snapshot}
        player="Alice"
        error={null}
        busy={false}
        isSocketReady={false}
        onStartGame={vi.fn()}
        onOpenBag={vi.fn()}
      />
    );

    // then
    expect(screen.getByRole('button', { name: 'Start' })).toBeDisabled();
  });
});
