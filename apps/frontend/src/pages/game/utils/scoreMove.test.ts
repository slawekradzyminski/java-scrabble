import type { BoardTile } from '../../../types';
import { scoreMove } from './scoreMove';

describe('scoreMove', () => {
  it('scores a simple horizontal word with center double word', () => {
    // given
    const board: BoardTile[] = [];
    const placements = {
      H8: { coordinate: 'H8', letter: 'A', points: 1, blank: false, assignedLetter: 'A', rackIndex: 0 },
      H9: { coordinate: 'H9', letter: 'B', points: 3, blank: false, assignedLetter: 'B', rackIndex: 1 }
    };

    // when
    const preview = scoreMove(board, placements);

    // then
    expect(preview).not.toBeNull();
    expect(preview?.score).toBe(8);
    expect(preview?.words).toEqual(['AB']);
  });
});
