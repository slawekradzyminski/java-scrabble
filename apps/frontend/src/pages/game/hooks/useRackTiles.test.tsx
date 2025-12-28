import { renderHook } from '@testing-library/react';
import type { GameSnapshot } from '../../../types';
import { useRackTiles } from './useRackTiles';

describe('useRackTiles', () => {
  it('pads rack to 7 tiles and hides staged tiles', () => {
    // given
    const snapshot: GameSnapshot = {
      roomId: 'room-1',
      status: 'active',
      players: [
        {
          name: 'Alice',
          score: 0,
          rackSize: 1,
          rackCapacity: 7,
          rack: [{ letter: 'A', points: 1, blank: false }]
        }
      ],
      bagCount: 90,
      boardTiles: 0,
      board: [],
      currentPlayerIndex: 0,
      pendingMove: false,
      pending: null,
      winner: null
    };

    // when
    const { result } = renderHook(() => useRackTiles(snapshot, 'Alice', { 'rack-0': {
      coordinate: 'rack-0',
      letter: 'A',
      points: 1,
      blank: false,
      assignedLetter: 'A'
    } }));

    // then
    expect(result.current).toHaveLength(7);
    expect(result.current[0]).toEqual({ letter: null, points: 0, blank: false });
  });
});
