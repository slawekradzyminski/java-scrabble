import React from 'react';
import { fireEvent, render, screen } from '@testing-library/react';
import GameActions from './GameActions';

describe('GameActions', () => {
  it('fires action handlers', () => {
    // given
    const onPlay = vi.fn();
    const onReset = vi.fn();
    const onPass = vi.fn();
    const onResign = vi.fn();

    // when
    render(
      <GameActions
        canPlay
        canReset
        isSocketReady
        onPlay={onPlay}
        onReset={onReset}
        onPass={onPass}
        onResign={onResign}
      />
    );
    fireEvent.click(screen.getByRole('button', { name: 'Play tiles' }));
    fireEvent.click(screen.getByRole('button', { name: 'Reset' }));
    fireEvent.click(screen.getByRole('button', { name: 'Pass' }));
    fireEvent.click(screen.getByRole('button', { name: 'Resign' }));

    // then
    expect(onPlay).toHaveBeenCalled();
    expect(onReset).toHaveBeenCalled();
    expect(onPass).toHaveBeenCalled();
    expect(onResign).toHaveBeenCalled();
  });

  it('disables buttons when not ready', () => {
    // given
    render(
      <GameActions
        canPlay={false}
        canReset={false}
        isSocketReady={false}
        onPlay={vi.fn()}
        onReset={vi.fn()}
        onPass={vi.fn()}
        onResign={vi.fn()}
      />
    );

    // then
    expect(screen.getByRole('button', { name: 'Play tiles' })).toBeDisabled();
    expect(screen.getByRole('button', { name: 'Reset' })).toBeDisabled();
    expect(screen.getByRole('button', { name: 'Pass' })).toBeDisabled();
    expect(screen.getByRole('button', { name: 'Resign' })).toBeDisabled();
  });
});
