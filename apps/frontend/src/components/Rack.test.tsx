import React from 'react';
import { render, screen } from '@testing-library/react';
import { DndContext } from '@dnd-kit/core';
import Rack from './Rack';

describe('Rack', () => {
  it('renders tiles', () => {
    // given
    const tiles = [
      { letter: 'A', points: 1, blank: false },
      { letter: 'B', points: 3, blank: false }
    ];

    // when
    render(
      <DndContext>
        <Rack tiles={tiles} />
      </DndContext>
    );

    // then
    expect(screen.getByText('A')).toBeInTheDocument();
    expect(screen.getByText('B')).toBeInTheDocument();
  });
});
