import { act, renderHook } from '@testing-library/react';
import type { RackTile } from '../../../types';
import { usePlacements } from './usePlacements';

describe('usePlacements', () => {
  const rackTiles: RackTile[] = [
    { letter: 'A', points: 1, blank: false },
    { letter: null, points: 0, blank: false }
  ];

  it('creates placements and payload', () => {
    // given
    const { result } = renderHook(() => usePlacements({
      getRackTile: (index) => rackTiles[index]
    }));

    // when
    act(() => {
      result.current.applyDrop({ ...rackTiles[0], rackIndex: 0 }, 'cell-H8');
    });

    // then
    expect(result.current.hasBoardPlacements).toBe(true);
    expect(result.current.placementsPayload).toEqual([
      { coordinate: 'H8', letter: 'A', blank: false }
    ]);
  });

  it('resets local state', () => {
    // given
    const { result } = renderHook(() => usePlacements({
      getRackTile: (index) => rackTiles[index]
    }));

    // when
    act(() => {
      result.current.applyDrop({ ...rackTiles[0], rackIndex: 0 }, 'cell-H8');
    });
    act(() => {
      result.current.resetLocalState();
    });

    // then
    expect(result.current.hasBoardPlacements).toBe(false);
    expect(result.current.placementsPayload).toEqual([]);
  });

  it('prompts for blank tile and assigns letter', () => {
    // given
    const { result } = renderHook(() => usePlacements({
      getRackTile: (index) => rackTiles[index]
    }));

    // when
    act(() => {
      result.current.applyDrop({ letter: null, points: 0, blank: true, rackIndex: 0 }, 'cell-H8');
    });
    act(() => {
      result.current.confirmBlank('Z');
    });

    // then
    expect(result.current.placementsPayload).toEqual([
      { coordinate: 'H8', letter: 'Z', blank: true }
    ]);
  });
});
