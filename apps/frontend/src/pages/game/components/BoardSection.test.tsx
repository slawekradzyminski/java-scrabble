import React from 'react';
import { render, screen } from '@testing-library/react';
import { DndContext } from '@dnd-kit/core';
import BoardSection from './BoardSection';

const baseProps = {
  isConnecting: false,
  connectionLabel: 'Connecting…',
  board: [],
  placements: {},
  activeTile: null,
  activeTileLabel: undefined,
  rackTiles: [{ letter: 'A', points: 1, blank: false }],
  onCellClick: vi.fn(),
  onTileSelect: vi.fn(),
  applyDrop: vi.fn()
};

describe('BoardSection', () => {
  it('shows connecting overlay', () => {
    // given
    render(
      <DndContext>
        <BoardSection {...baseProps} isConnecting connectionLabel="Reconnecting…" />
      </DndContext>
    );

    // then
    expect(screen.getByText('Reconnecting…')).toBeInTheDocument();
  });

  it('renders rack when connected', () => {
    // given
    render(
      <DndContext>
        <BoardSection {...baseProps} />
      </DndContext>
    );

    // then
    expect(screen.getByText('Test add placement')).toBeInTheDocument();
  });
});
