import React, { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { closestCenter, DndContext, DragEndEvent, DragOverlay, DragStartEvent, PointerSensor, useSensor, useSensors } from '@dnd-kit/core';
import Board from '../components/Board';
import PlayerList from '../components/PlayerList';
import Rack from '../components/Rack';
import Tile from '../components/Tile';
import type { BoardTile, GameSnapshot, RackTile, WsMessage } from '../types';
import { ConnectionState, GameSocket, joinRoom, startGame } from '../api';
import { cellId } from '../utils';

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

type PlacementState = Record<string, BoardTile>;
type EventLogEntry = {
  id: number;
  time: string;
  type: WsMessage['type'];
  summary: string;
};

interface GameProps {
  roomId: string;
  player: string;
  onLeave: () => void;
}

export default function Game({ roomId, player, onLeave }: GameProps) {
  const [connectionState, setConnectionState] = useState<ConnectionState>('disconnected');
  const [hasSynced, setHasSynced] = useState(false);
  const [snapshot, setSnapshot] = useState<GameSnapshot>(emptySnapshot);
  const [placements, setPlacements] = useState<PlacementState>({});
  const [activeTile, setActiveTile] = useState<RackTile | null>(null);
  const [activeTileSource, setActiveTileSource] = useState<string | null>(null);
  const [activeTileLabel, setActiveTileLabel] = useState<string | undefined>(undefined);
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);
  const [eventLog, setEventLog] = useState<EventLogEntry[]>([]);
  const [lastEventAt, setLastEventAt] = useState<number | null>(null);
  const [historyExpanded, setHistoryExpanded] = useState(false);
  const [headerExpanded, setHeaderExpanded] = useState(false);

  const socketRef = useRef<GameSocket | null>(null);
  const eventCounterRef = useRef(0);

  const sensors = useSensors(useSensor(PointerSensor, { activationConstraint: { distance: 8 } }));

  const currentPlayer = snapshot.players.find(p => p.name === player);
  const serverRackTiles = currentPlayer?.rack ?? [];

  const rackTiles = useMemo(() => {
    const placedTiles = Object.values(placements);
    if (placedTiles.length === 0) {
      return serverRackTiles;
    }
    const remaining = [...serverRackTiles];
    for (const placed of placedTiles) {
      const idx = remaining.findIndex(
        (t) => t.letter === placed.letter && t.blank === placed.blank && t.points === placed.points
      );
      if (idx !== -1) {
        remaining.splice(idx, 1);
      }
    }
    return remaining;
  }, [serverRackTiles, placements]);

  const resetLocalState = useCallback(() => {
    setPlacements({});
    setActiveTile(null);
    setActiveTileSource(null);
    setActiveTileLabel(undefined);
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

  const appendEventLog = useCallback((message: WsMessage) => {
    if (message.type === 'STATE_SNAPSHOT' || message.type === 'PONG') {
      return;
    }
    const entry: EventLogEntry = {
      id: (eventCounterRef.current += 1),
      time: new Date().toLocaleTimeString(),
      type: message.type,
      summary: summarizeEvent(message)
    };
    setEventLog((prev) => [entry, ...prev].slice(0, 50));
    setLastEventAt(Date.now());
  }, [summarizeEvent]);

  const getSocket = useCallback(() => {
    if (!socketRef.current) {
      socketRef.current = new GameSocket();
    }
    return socketRef.current;
  }, []);

  const connectSocket = useCallback((targetRoomId: string, targetPlayer: string) => {
    const socket = getSocket();

    socket.disconnect();
    setHasSynced(false);
    setEventLog([]);
    eventCounterRef.current = 0;
    setSnapshot({ ...emptySnapshot, roomId: targetRoomId });
    resetLocalState();

    socket.onConnectionState((state) => {
      setConnectionState(state);
      if (state === 'reconnecting') {
        resetLocalState();
      }
    });

    socket.onSnapshot((next) => {
      setSnapshot((prev) => mergeSnapshot(prev, next));
      resetLocalState();
      setHasSynced(true);
    });

    socket.onEvent((message: WsMessage) => {
      appendEventLog(message);
      if (message.type !== 'STATE_SNAPSHOT' && message.type !== 'PONG') {
        resetLocalState();
        socket.requestSync();
      }
    });

    socket.connect(targetRoomId, targetPlayer);
  }, [getSocket, resetLocalState]);

  useEffect(() => {
    if (!roomId || !player) {
      return;
    }

    let active = true;
    setError(null);

    joinRoom(roomId, player)
      .then(() => {
        if (!active) {
          return;
        }
        connectSocket(roomId, player);
      })
      .catch((err) => {
        if (!active) {
          return;
        }
        setError(err instanceof Error ? err.message : 'Failed to join room');
      });

    return () => {
      active = false;
    };
  }, [roomId, player, connectSocket]);

  useEffect(() => {
    return () => {
      socketRef.current?.disconnect();
    };
  }, []);

  const handleStartGame = async () => {
    if (!roomId) {
      return;
    }
    setBusy(true);
    setError(null);
    try {
      await startGame(roomId);
      const socket = socketRef.current;
      if (socket && player) {
        socket.requestSync();
      }
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to start game');
    } finally {
      setBusy(false);
    }
  };

  const handleDragStart = (event: DragStartEvent) => {
    const activeId = String(event.active.id);
    if (activeId.startsWith('rack-')) {
      const rackIndex = parseInt(activeId.replace('rack-', ''), 10);
      setActiveTile(rackTiles[rackIndex] ?? null);
      setActiveTileSource(null);
      setActiveTileLabel(undefined);
    } else if (activeId.startsWith('placement-')) {
      const coordinate = activeId.replace('placement-', '');
      const placement = placements[coordinate];
      if (placement) {
        setActiveTile({
          letter: placement.letter,
          points: placement.points,
          blank: placement.blank
        });
        setActiveTileSource(coordinate);
        setActiveTileLabel(placement.assignedLetter);
      }
    }
  };

  const handleTileSelect = (tile: RackTile) => {
    setActiveTile(tile);
  };

  const handleCellClick = (id: string) => {
    applyDrop(activeTile, cellId(id));
  };

  const applyDrop = (tile: RackTile | null, dropTarget: string | null) => {
    if (!tile) {
      setActiveTile(null);
      setActiveTileSource(null);
      setActiveTileLabel(undefined);
      return;
    }

    if (!dropTarget) {
      setActiveTile(null);
      setActiveTileSource(null);
      setActiveTileLabel(undefined);
      return;
    }

    const dropId = dropTarget.toString();

    if (dropId === 'rack-drop') {
      if (activeTileSource) {
        setPlacements((prev) => {
          const next = { ...prev };
          delete next[activeTileSource];
          return next;
        });
      }
      setActiveTile(null);
      setActiveTileSource(null);
      setActiveTileLabel(undefined);
      return;
    }

    if (dropId.startsWith('rack-')) {
      setActiveTile(null);
      setActiveTileSource(null);
      setActiveTileLabel(undefined);
      return;
    }

    if (!dropId.startsWith('cell-')) {
      setActiveTile(null);
      setActiveTileSource(null);
      setActiveTileLabel(undefined);
      return;
    }

    const coordinate = dropId.replace('cell-', '');

    if (activeTileSource && activeTileSource !== coordinate) {
      setPlacements((prev) => {
        const next = { ...prev };
        delete next[activeTileSource];
        const boardTile: BoardTile = {
          coordinate,
          letter: tile.letter,
          points: tile.points,
          blank: tile.blank,
          assignedLetter: prev[activeTileSource]?.assignedLetter ?? tile.letter ?? ''
        };
        next[coordinate] = boardTile;
        return next;
      });
      setActiveTile(null);
      setActiveTileSource(null);
      setActiveTileLabel(undefined);
      return;
    }

    if (placements[coordinate] && !activeTileSource) {
      setPlacements((prev) => {
        const next = { ...prev };
        delete next[coordinate];
        return next;
      });
      setActiveTile(null);
      setActiveTileSource(null);
      setActiveTileLabel(undefined);
      return;
    }

    let assignedLetter = tile.letter ?? '';
    if (tile.blank && !placements[coordinate]) {
      const existingPlacement = activeTileSource ? placements[activeTileSource] : null;
      if (!existingPlacement) {
        const promptValue = window.prompt('Blank tile: choose a letter (A-Z)');
        if (!promptValue || promptValue.trim().length !== 1) {
          setActiveTile(null);
          setActiveTileSource(null);
          setActiveTileLabel(undefined);
          return;
        }
        assignedLetter = promptValue.trim().toUpperCase();
      } else {
        assignedLetter = existingPlacement.assignedLetter;
      }
    } else if (activeTileSource && placements[activeTileSource]) {
      assignedLetter = placements[activeTileSource].assignedLetter;
    }

    const boardTile: BoardTile = {
      coordinate,
      letter: tile.letter,
      points: tile.points,
      blank: tile.blank,
      assignedLetter
    };
    setPlacements((prev) => {
      const next = { ...prev };
      if (activeTileSource && activeTileSource !== coordinate) {
        delete next[activeTileSource];
      }
      next[coordinate] = boardTile;
      return next;
    });
    setActiveTile(null);
    setActiveTileSource(null);
    setActiveTileLabel(undefined);
  };

  const handleDragEnd = (event: DragEndEvent) => {
    applyDrop(activeTile, event.over?.id?.toString() ?? null);
  };

  const commitMove = () => {
    const socket = socketRef.current;
    if (!socket?.isConnected() || !roomId) {
      return;
    }
    const placementsPayload = Object.values(placements).map((tile) => ({
      coordinate: tile.coordinate,
      letter: tile.assignedLetter,
      blank: tile.blank
    }));
    socket.send({
      type: 'PLAY_TILES',
      payload: {
        player,
        placements: placementsPayload
      }
    });
  };

  const sendPass = () => {
    socketRef.current?.send({ type: 'PASS', payload: { player } });
  };

  const sendChallenge = () => {
    socketRef.current?.send({ type: 'CHALLENGE', payload: { player } });
  };

  const sendResign = () => {
    socketRef.current?.send({ type: 'RESIGN', payload: { player } });
  };


  const isSocketReady = connectionState === 'connected' && hasSynced;
  const isConnecting = connectionState === 'connecting' || connectionState === 'reconnecting';

  return (
    <>
      {headerExpanded ? (
        <header className="topbar topbar--expanded">
          <div>
            <div className="eyebrow">SCRABBLE LIVE</div>
            <h1>Realtime Board</h1>
          </div>
          <div className="topbar-actions">
            <button type="button" className="topbar-toggle" onClick={() => setHeaderExpanded(false)}>
              Hide header
            </button>
            <button type="button" className="leave-btn" onClick={onLeave}>
              Leave Room
            </button>
          </div>
        </header>
      ) : (
        <div className="compact-header">
          <button type="button" className="topbar-toggle" onClick={() => setHeaderExpanded(true)}>
            Show header
          </button>
          <button type="button" className="leave-btn" onClick={onLeave}>
            Leave Room
          </button>
        </div>
      )}

      <DndContext
        sensors={sensors}
        collisionDetection={closestCenter}
        onDragStart={handleDragStart}
        onDragEnd={handleDragEnd}
      >
        <main className="layout">
          <section className="game-column">
            <div className="board-wrap">
              {isConnecting && !hasSynced ? (
                <div className="connecting-overlay">
                  <p>{connectionState === 'reconnecting' ? 'Reconnecting…' : 'Connecting…'}</p>
                </div>
              ) : (
                <>
                  <Board tiles={snapshot.board} placements={placements} onCellClick={handleCellClick} />
                  <DragOverlay>
                    {activeTile ? <Tile tile={activeTile} dragging label={activeTileLabel} /> : null}
                  </DragOverlay>
                </>
              )}
            </div>
            <div className="panel rack-panel">
              <h2>Your rack</h2>
              <Rack tiles={rackTiles} onSelect={handleTileSelect} />
              <div className="rack-hint">Drag tiles onto the board. Snapshots are scoped to the connected player.</div>
              {import.meta.env.MODE === 'test' && (
                <div className="test-controls">
                  <button
                    type="button"
                    className="test-only"
                    onClick={() => applyDrop(rackTiles[0] ?? null, 'cell-H8')}
                  >
                    Test add placement
                  </button>
                  <button
                    type="button"
                    className="test-only"
                    onClick={() => applyDrop(null, null)}
                  >
                    Test drop empty
                  </button>
                  <button
                    type="button"
                    className="test-only"
                    onClick={() => applyDrop(rackTiles[0] ?? null, 'rack-1')}
                  >
                    Test drop invalid target
                  </button>
                  <button
                    type="button"
                    className="test-only"
                    onClick={() => applyDrop({ letter: null, points: 0, blank: true }, 'cell-H8')}
                  >
                    Test drop blank
                  </button>
                </div>
              )}
            </div>
          </section>

          <aside className="sidebar">
            <div className="panel game-info">
              <div className="info-section">
                <div className="info-header">
                  <h2>Game</h2>
                  <button type="button" onClick={handleStartGame} disabled={busy || !roomId || !isSocketReady}>
                    Start
                  </button>
                </div>
                {error && <p className="error">{error}</p>}
                <div className="info-meta">
                  <div>
                    <span>Room</span>
                    <strong>{snapshot.roomId || '—'}</strong>
                  </div>
                  <div>
                    <span>Player</span>
                    <strong>{player || '—'}</strong>
                  </div>
                  <div>
                    <span>Status</span>
                    <strong>
                      {hasSynced
                        ? snapshot.status
                        : connectionState}
                    </strong>
                  </div>
                  <div>
                    <span>Bag</span>
                    <strong>{snapshot.bagCount}</strong>
                  </div>
                  {snapshot.winner && (
                    <div>
                      <span>Winner</span>
                      <strong>{snapshot.winner}</strong>
                    </div>
                  )}
                </div>
              </div>

              <div className="info-section">
                <h3>Players</h3>
                <PlayerList players={snapshot.players} currentIndex={snapshot.currentPlayerIndex} />
              </div>

              <div className="info-section">
                <h3>Pending</h3>
                {snapshot.pending ? (
                  <div className="pending-details">
                    <p>Score: {snapshot.pending.score}</p>
                    <p>Words: {snapshot.pending.words.map((word) => word.text).join(', ') || '—'}</p>
                  </div>
                ) : (
                  <p className="muted">None</p>
                )}
              </div>

              <div className="info-section">
                <div className="info-header">
                  <h3>Live history</h3>
                  <button
                    type="button"
                    className="history-toggle"
                    onClick={() => setHistoryExpanded((prev) => !prev)}
                  >
                    {historyExpanded ? 'Hide' : 'Show'}
                  </button>
                </div>
                {historyExpanded ? (
                  <>
                    {lastEventAt ? (
                      <p className="muted">Last update: {new Date(lastEventAt).toLocaleTimeString()}</p>
                    ) : null}
                    <div className="event-log">
                      {eventLog.length === 0 ? (
                        <p className="muted">No events yet.</p>
                      ) : (
                        <ul>
                          {eventLog.map((entry) => (
                            <li key={entry.id} className={`event-log__entry event-log__entry--${entry.type.toLowerCase()}`}>
                              <span className="event-log__time">{entry.time}</span>
                              <span className="event-log__summary">{entry.summary}</span>
                            </li>
                          ))}
                        </ul>
                      )}
                    </div>
                  </>
                ) : (
                  <p className="muted">History hidden ({eventLog.length} events).</p>
                )}
              </div>

              <div className="info-section actions">
                <button type="button" onClick={commitMove} disabled={!Object.keys(placements).length || !isSocketReady}>Play tiles</button>
                <button type="button" onClick={() => setPlacements({})} disabled={!Object.keys(placements).length}>Reset</button>
                <button type="button" onClick={sendPass} disabled={!isSocketReady}>Pass</button>
                <button type="button" onClick={sendChallenge} disabled={!isSocketReady}>Challenge</button>
                <button type="button" onClick={sendResign} disabled={!isSocketReady}>Resign</button>
              </div>
            </div>
          </aside>
        </main>
      </DndContext>
    </>
  );
}
