import { useCallback, useRef, useState } from 'react';
import type { GameSnapshot, HistoryEntry, WsMessage } from '../../../types';
import { ConnectionState, GameSocket, fetchEvents } from '../../../api';
import type { EventLogEntry } from '../types';

const emptySnapshot: GameSnapshot = {
  roomId: '',
  status: 'not_started',
  players: [],
  bagCount: 0,
  boardTiles: 0,
  board: [],
  currentPlayerIndex: null,
  pendingMove: false,
  pending: null,
  winner: null
};

interface UseGameSocketOptions {
  onResetLocal: () => void;
  hasLocalPlacements?: () => boolean;
}

export function useGameSocket({ onResetLocal, hasLocalPlacements }: UseGameSocketOptions) {
  const [connectionState, setConnectionState] = useState<ConnectionState>('disconnected');
  const [hasSynced, setHasSynced] = useState(false);
  const [snapshot, setSnapshot] = useState<GameSnapshot>(emptySnapshot);
  const [eventLog, setEventLog] = useState<EventLogEntry[]>([]);
  const [lastEventAt, setLastEventAt] = useState<number | null>(null);

  const socketRef = useRef<GameSocket | null>(null);
  const eventCounterRef = useRef(0);
  const historyHydratedRef = useRef(false);
  const lastServerStateRef = useRef<{ boardTiles: number; currentPlayerIndex: number | null; status: string } | null>(null);
  const lastStateVersionRef = useRef<number | null>(null);
  const lastHistoryVersionRef = useRef<number | null>(null);
  const lastSnapshotVersionRef = useRef<number | null>(null);
  const pendingSyncRef = useRef<number | null>(null);
  const playerRef = useRef<string | null>(null);
  const lastEventIdRef = useRef<number | null>(null);
  const eventIdsRef = useRef<Set<number>>(new Set());

  const clearPendingSync = useCallback(() => {
    if (pendingSyncRef.current !== null) {
      window.clearTimeout(pendingSyncRef.current);
      pendingSyncRef.current = null;
    }
  }, []);

  const mergeSnapshot = useCallback((prev: GameSnapshot, next: GameSnapshot) => {
    const mergedPlayers = next.players.map((playerSnapshot) => {
      const previousPlayer = prev.players.find(p => p.name === playerSnapshot.name);
      if (!previousPlayer) {
        return playerSnapshot;
      }
      if (playerSnapshot.rack.length === 0 && playerSnapshot.rackSize > 0 && previousPlayer.rack.length > 0) {
        return { ...playerSnapshot, rack: previousPlayer.rack };
      }
      return playerSnapshot;
    });
    return { ...next, players: mergedPlayers };
  }, []);

  const summarizeEvent = useCallback((message: WsMessage) => {
    switch (message.type) {
      case 'MOVE_PROPOSED': {
        const playerName = String(message.payload.player ?? 'Unknown');
        const score = message.payload.score;
        return `Move proposed by ${playerName} (${score ?? '—'} pts)`;
      }
      case 'MOVE_ACCEPTED': {
        const playerName = String(message.payload.player ?? 'Unknown');
        const score = message.payload.score;
        return `Move accepted for ${playerName} (${score ?? '—'} pts)`;
      }
      case 'MOVE_REJECTED': {
        const playerName = String(message.payload.player ?? 'Unknown');
        const reason = message.payload.reason ?? 'rejected';
        return `Move rejected for ${playerName} (${reason})`;
      }
      case 'TURN_ADVANCED': {
        const current = message.payload.currentPlayer ?? 'Unknown';
        return `Turn advanced → ${current}`;
      }
      case 'PASS': {
        const playerName = String(message.payload.player ?? 'Unknown');
        return `${playerName} passed`;
      }
      case 'EXCHANGE': {
        const playerName = String(message.payload.player ?? 'Unknown');
        const count = message.payload.count ?? 'some';
        return `${playerName} exchanged ${count} tile${count === 1 ? '' : 's'}`;
      }
      case 'GAME_ENDED': {
        const winner = message.payload.winner ?? '—';
        return `Game ended (winner: ${winner})`;
      }
      case 'STATE_SNAPSHOT':
        return 'State snapshot';
      case 'ERROR':
        return `Error: ${message.payload.reason ?? 'unknown'}`;
      case 'PONG':
        return 'PONG';
      default:
        return message.type;
    }
  }, []);

  const nextSyntheticId = useCallback(() => -(++eventCounterRef.current), []);

  const appendEventEntries = useCallback((entries: HistoryEntry[], prepend = true) => {
    if (!entries || entries.length === 0) {
      return;
    }
    const items = [...entries];
    if (prepend) {
      items.reverse();
    }
    const mapped: EventLogEntry[] = [];
    for (const item of items) {
      const eventId = typeof item.eventId === 'number' ? item.eventId : null;
      if (eventId !== null && eventIdsRef.current.has(eventId)) {
        continue;
      }
      if (eventId !== null) {
        eventIdsRef.current.add(eventId);
      }
      const message = { type: item.type, payload: item.payload } as WsMessage;
      const time = item.time ? new Date(item.time).toLocaleTimeString() : new Date().toLocaleTimeString();
      mapped.push({
        id: eventId ?? nextSyntheticId(),
        time,
        type: message.type,
        summary: summarizeEvent(message)
      });
    }
    if (mapped.length === 0) {
      return;
    }
    setEventLog((prev) => {
      const combined = prepend ? [...mapped, ...prev] : [...prev, ...mapped];
      return combined.slice(0, 50);
    });
    const lastEntry = mapped[0];
    if (lastEntry) {
      const parsed = Date.parse(lastEntry.time);
      setLastEventAt(Number.isNaN(parsed) ? Date.now() : parsed);
    }
  }, [nextSyntheticId, summarizeEvent]);

  const appendEventLog = useCallback((message: WsMessage) => {
    if (message.type === 'STATE_SNAPSHOT' || message.type === 'PONG') {
      return;
    }
    const versionPayload = message.payload?.stateVersion;
    const nextVersion = typeof versionPayload === 'number' ? versionPayload : null;
    if (nextVersion !== null) {
      const previousVersion = lastStateVersionRef.current;
      if (previousVersion === null || nextVersion > previousVersion) {
        lastStateVersionRef.current = nextVersion;
      }
      // If an event arrives without a newer snapshot, request a one-off sync shortly after.
      if ((lastSnapshotVersionRef.current ?? 0) < nextVersion && pendingSyncRef.current === null) {
        pendingSyncRef.current = window.setTimeout(() => {
          pendingSyncRef.current = null;
          if ((lastSnapshotVersionRef.current ?? 0) < nextVersion) {
            socketRef.current?.requestSync();
          }
        }, 200);
      }
    }
    const entry: EventLogEntry = {
      id: nextSyntheticId(),
      time: new Date().toLocaleTimeString(),
      type: message.type,
      summary: summarizeEvent(message)
    };
    setEventLog((prev) => [entry, ...prev].slice(0, 50));
    setLastEventAt(Date.now());
  }, [nextSyntheticId, summarizeEvent]);

  const hydrateEventLog = useCallback((history: GameSnapshot['history']) => {
    if (!history || history.length === 0) {
      return;
    }
    eventCounterRef.current = 0;
    eventIdsRef.current = new Set();
    const entries: EventLogEntry[] = [];
    for (const item of [...history].reverse()) {
      const eventId = typeof item.eventId === 'number' ? item.eventId : null;
      if (eventId !== null) {
        eventIdsRef.current.add(eventId);
      }
      const message = { type: item.type, payload: item.payload } as WsMessage;
      const time = item.time ? new Date(item.time).toLocaleTimeString() : new Date().toLocaleTimeString();
      entries.push({
        id: eventId ?? nextSyntheticId(),
        time,
        type: message.type,
        summary: summarizeEvent(message)
      });
    }
    setEventLog(entries.slice(0, 50));
    const lastEntry = history[history.length - 1];
    const lastTime = lastEntry?.time ? Date.parse(lastEntry.time) : Date.now();
    setLastEventAt(Number.isNaN(lastTime) ? Date.now() : lastTime);
  }, [nextSyntheticId, summarizeEvent]);

  const syncEventsAfter = useCallback(async (roomId: string, afterEventId: number) => {
    try {
      const response = await fetchEvents(roomId, afterEventId);
      if (response.events.length > 0) {
        appendEventEntries(response.events, true);
      }
      if (typeof response.lastEventId === 'number') {
        lastEventIdRef.current = response.lastEventId;
      }
    } catch (error) {
      if (import.meta.env.DEV) {
        console.warn('Failed to fetch event delta', error);
      }
    }
  }, [appendEventEntries]);

  const getSocket = useCallback(() => {
    if (!socketRef.current) {
      socketRef.current = new GameSocket();
    }
    return socketRef.current;
  }, []);

  const connect = useCallback((roomId: string, player: string) => {
    const socket = getSocket();

    socket.disconnect();
    setHasSynced(false);
    setEventLog([]);
    eventCounterRef.current = 0;
    historyHydratedRef.current = false;
    lastStateVersionRef.current = null;
    lastHistoryVersionRef.current = null;
    lastServerStateRef.current = null;
    lastSnapshotVersionRef.current = null;
    lastEventIdRef.current = null;
    eventIdsRef.current = new Set();
    clearPendingSync();
    playerRef.current = player;
    setSnapshot({ ...emptySnapshot, roomId });
    onResetLocal();

    socket.onConnectionState((state) => {
      setConnectionState(state);
      if (state === 'reconnecting') {
        setHasSynced(false);
        historyHydratedRef.current = false;
      }
    });

    socket.onSnapshot((next) => {
      const nextVersion = next.stateVersion ?? 0;
      const previousVersion = lastStateVersionRef.current;
      const previousEventId = lastEventIdRef.current;
      const snapshotEventId = typeof next.lastEventId === 'number' ? next.lastEventId : null;
      const playerIndex = next.players.findIndex((p) => p.name === player);
      const previousState = lastServerStateRef.current;
      const serverStateChanged = !previousState
        || previousState.boardTiles !== next.boardTiles
        || previousState.currentPlayerIndex !== next.currentPlayerIndex
        || previousState.status !== next.status;
      if (previousVersion !== null && nextVersion < previousVersion) {
        setHasSynced(true);
        return;
      }
      setSnapshot((prev) => mergeSnapshot(prev, next));
      lastSnapshotVersionRef.current = nextVersion;
      clearPendingSync();
      const notPlayersTurn = playerIndex === -1 || next.currentPlayerIndex !== playerIndex;
      const hasPlacements = hasLocalPlacements?.() ?? false;
      if (serverStateChanged || (hasPlacements && notPlayersTurn)) {
        onResetLocal();
      }
      lastServerStateRef.current = {
        boardTiles: next.boardTiles,
        currentPlayerIndex: next.currentPlayerIndex,
        status: next.status
      };
      lastStateVersionRef.current = previousVersion === null ? nextVersion : Math.max(previousVersion, nextVersion);
      setHasSynced(true);
      if (!historyHydratedRef.current || (lastHistoryVersionRef.current !== null && nextVersion > lastHistoryVersionRef.current)) {
        hydrateEventLog(next.history);
        historyHydratedRef.current = true;
        lastHistoryVersionRef.current = nextVersion;
      }
      if (snapshotEventId !== null) {
        lastEventIdRef.current = snapshotEventId;
      }
      if (previousEventId !== null && snapshotEventId !== null && snapshotEventId > previousEventId) {
        void syncEventsAfter(roomId, previousEventId);
      }
    });

    socket.onEvent((message: WsMessage) => {
      appendEventLog(message);
      if (message.type === 'MOVE_REJECTED') {
        const messagePlayer = message.payload?.player;
        if (typeof messagePlayer === 'string' && messagePlayer === playerRef.current) {
          onResetLocal();
        }
      }
    });

    socket.connect(roomId, player);
  }, [
    appendEventLog,
    clearPendingSync,
    getSocket,
    hasLocalPlacements,
    hydrateEventLog,
    mergeSnapshot,
    onResetLocal,
    syncEventsAfter
  ]);

  const disconnect = useCallback(() => {
    clearPendingSync();
    socketRef.current?.disconnect();
  }, [clearPendingSync]);

  const requestSync = useCallback(() => {
    socketRef.current?.requestSync();
  }, []);

  const socket = socketRef;

  return {
    snapshot,
    connectionState,
    hasSynced,
    eventLog,
    lastEventAt,
    connect,
    disconnect,
    requestSync,
    socket
  };
}
