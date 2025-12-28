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

  it('removes a placement when dropped back to rack', () => {
    // given
    const { result } = renderHook(() => usePlacements({
      getRackTile: (index) => rackTiles[index]
    }));

    // when
    act(() => {
      result.current.applyDrop({ ...rackTiles[0], rackIndex: 0 }, 'cell-H8');
    });
    act(() => {
      result.current.handleDragStart({ active: { id: 'placement-H8' } } as never);
    });
    act(() => {
      result.current.applyDrop(result.current.activeTile, 'rack-drop');
    });

    // then
    expect(result.current.hasBoardPlacements).toBe(false);
    expect(result.current.placementsPayload).toEqual([]);
  });

  it('removes an existing placement when clicking the same cell', () => {
    // given
    const { result } = renderHook(() => usePlacements({
      getRackTile: (index) => rackTiles[index]
    }));

    // when
    act(() => {
      result.current.applyDrop({ ...rackTiles[0], rackIndex: 0 }, 'cell-H8');
    });
    act(() => {
      result.current.applyDrop({ ...rackTiles[0], rackIndex: 0 }, 'cell-H8');
    });

    // then
    expect(result.current.hasBoardPlacements).toBe(false);
    expect(result.current.placementsPayload).toEqual([]);
  });

  it('reuses assigned blank letter when moving an existing blank placement', () => {
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
    act(() => {
      result.current.handleDragStart({ active: { id: 'placement-H8' } } as never);
    });
    act(() => {
      result.current.applyDrop(result.current.activeTile, 'cell-H9');
    });

    // then
    expect(result.current.pendingBlank).toBeNull();
    expect(result.current.placementsPayload).toEqual([
      { coordinate: 'H9', letter: 'Z', blank: true }
    ]);
  });
});
