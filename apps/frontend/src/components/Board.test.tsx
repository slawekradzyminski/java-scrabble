
import { render, screen } from '@testing-library/react';
import { DndContext } from '@dnd-kit/core';
import Board from './Board';

describe('Board', () => {
  it('renders placed tile', () => {
    // given
    const tiles = [
      {
        coordinate: 'H8',
        letter: 'A',
        points: 1,
        blank: false,
        assignedLetter: 'A'
      }
    ];

    // when
    render(
      <DndContext>
        <Board tiles={tiles} placements={{}} />
      </DndContext>
    );

    // then
    expect(screen.getByText('A')).toBeInTheDocument();
  });
});
