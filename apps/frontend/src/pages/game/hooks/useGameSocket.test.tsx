import { act, renderHook } from '@testing-library/react';
import type { ConnectionState } from '../../../api';
import { useGameSocket } from './useGameSocket';

const connectMock = vi.fn();
const disconnectMock = vi.fn();
const requestSyncMock = vi.fn();
const fetchEventsMock = vi.fn();

let snapshotHandler: ((snapshot: unknown) => void) | null = null;
let eventHandler: ((message: unknown) => void) | null = null;
let connectionStateHandler: ((state: ConnectionState) => void) | null = null;

vi.mock('../../../api', () => ({
  GameSocket: class {
    onSnapshot(handler: (snapshot: unknown) => void) {
      snapshotHandler = handler;
    }
    onEvent(handler: (message: unknown) => void) {
      eventHandler = handler;
    }
    onConnectionState(handler: (state: ConnectionState) => void) {
      connectionStateHandler = handler;
    }
    connect(roomId: string, player: string) {
      connectMock(roomId, player);
      connectionStateHandler?.('connected');
    }
    disconnect() {
      disconnectMock();
    }
    requestSync() {
      requestSyncMock();
    }
    isConnected() {
      return true;
    }
  },
  fetchEvents: (...args: unknown[]) => fetchEventsMock(...args)
}));

describe('useGameSocket', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    snapshotHandler = null;
    eventHandler = null;
    connectionStateHandler = null;
  });

  it('connects and hydrates snapshot', () => {
    // given
    const resetLocal = vi.fn();
    const { result } = renderHook(() => useGameSocket({ onResetLocal: resetLocal }));

    // when
    act(() => {
      result.current.connect('room-1', 'Alice');
    });
    act(() => {
      snapshotHandler?.({
        roomId: 'room-1',
        status: 'active',
        players: [],
        bagCount: 0,
        boardTiles: 0,
        board: [],
        currentPlayerIndex: 0,
        pendingMove: false,
        pending: null,
        winner: null,
        stateVersion: 1,
        lastEventId: 3,
        history: [
          { eventId: 3, type: 'PASS', payload: { player: 'Alice' }, time: new Date().toISOString() }
        ]
      });
    });

    // then
    expect(connectMock).toHaveBeenCalledWith('room-1', 'Alice');
    expect(result.current.snapshot.roomId).toBe('room-1');
  });

  it('fetches event delta after reconnect snapshot', async () => {
    // given
    const resetLocal = vi.fn();
    fetchEventsMock.mockResolvedValue({
      events: [
        { eventId: 4, type: 'PASS', payload: { player: 'Bob' }, time: new Date().toISOString() },
        { eventId: 5, type: 'EXCHANGE', payload: { player: 'Alice', count: 2 }, time: new Date().toISOString() }
      ],
      lastEventId: 5
    });
    const { result } = renderHook(() => useGameSocket({ onResetLocal: resetLocal }));

    // when
    act(() => {
      result.current.connect('room-1', 'Alice');
    });
    act(() => {
      snapshotHandler?.({
        roomId: 'room-1',
        status: 'active',
        players: [],
        bagCount: 0,
        boardTiles: 0,
        board: [],
        currentPlayerIndex: 0,
        pendingMove: false,
        pending: null,
        winner: null,
        stateVersion: 1,
        lastEventId: 3,
        history: [
          { eventId: 3, type: 'PASS', payload: { player: 'Alice' }, time: new Date().toISOString() }
        ]
      });
    });
    act(() => {
      connectionStateHandler?.('reconnecting');
    });
    await act(async () => {
      snapshotHandler?.({
        roomId: 'room-1',
        status: 'active',
        players: [],
        bagCount: 0,
        boardTiles: 0,
        board: [],
        currentPlayerIndex: 0,
        pendingMove: false,
        pending: null,
        winner: null,
        stateVersion: 2,
        lastEventId: 5,
        history: [
          { eventId: 5, type: 'EXCHANGE', payload: { player: 'Alice', count: 2 }, time: new Date().toISOString() }
        ]
      });
    });

    // then
    expect(fetchEventsMock).toHaveBeenCalledWith('room-1', 3);
    expect(result.current.eventLog[0]?.summary).toContain('passed');
    expect(result.current.eventLog.some((entry) => entry.summary.includes('exchanged'))).toBe(true);
  });

  it('requests sync when event version is ahead of snapshot', () => {
    // given
    vi.useFakeTimers();
    const resetLocal = vi.fn();
    const { result } = renderHook(() => useGameSocket({ onResetLocal: resetLocal }));

    // when
    act(() => {
      result.current.connect('room-1', 'Alice');
    });
    act(() => {
      eventHandler?.({ type: 'MOVE_ACCEPTED', payload: { stateVersion: 2 } });
    });
    act(() => {
      vi.advanceTimersByTime(220);
    });

    // then
    expect(requestSyncMock).toHaveBeenCalled();
    vi.useRealTimers();
  });

  it('resets local placements when server state changes and local placements exist', () => {
    // given
    const resetLocal = vi.fn();
    const hasLocalPlacements = vi.fn(() => true);
    const { result } = renderHook(() => useGameSocket({ onResetLocal: resetLocal, hasLocalPlacements }));

    // when
    act(() => {
      result.current.connect('room-1', 'Alice');
    });
    act(() => {
      snapshotHandler?.({
        roomId: 'room-1',
        status: 'active',
        players: [
          { name: 'Alice', score: 0, rackSize: 0, rackCapacity: 7, rack: [] }
        ],
        bagCount: 0,
        boardTiles: 1,
        board: [],
        currentPlayerIndex: 0,
        pendingMove: false,
        pending: null,
        winner: null,
        stateVersion: 1
      });
    });

    // then
    expect(resetLocal).toHaveBeenCalled();
  });
});
