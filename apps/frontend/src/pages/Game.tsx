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

type PlacementTile = BoardTile & { rackIndex?: number };
type PlacementState = Record<string, PlacementTile>;
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
  const [activeTile, setActiveTile] = useState<(RackTile & { rackIndex: number }) | null>(null);
  const [activeTileSource, setActiveTileSource] = useState<string | null>(null);
  const [activeTileLabel, setActiveTileLabel] = useState<string | undefined>(undefined);
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);
  const [eventLog, setEventLog] = useState<EventLogEntry[]>([]);
  const [lastEventAt, setLastEventAt] = useState<number | null>(null);
  const [historyExpanded, setHistoryExpanded] = useState(false);
  const [headerExpanded, setHeaderExpanded] = useState(false);
  const [bagModalOpen, setBagModalOpen] = useState(false);

  const socketRef = useRef<GameSocket | null>(null);
  const eventCounterRef = useRef(0);
  const historyHydratedRef = useRef(false);
  const lastServerStateRef = useRef<{ boardTiles: number; currentPlayerIndex: number | null; status: string } | null>(null);
  const placementsRef = useRef<PlacementState>({});

  const sensors = useSensors(useSensor(PointerSensor, { activationConstraint: { distance: 8 } }));

  const rackTiles = useMemo(() => {
    const currentPlayer = snapshot.players.find(p => p.name === player);
    const serverRackTiles = currentPlayer?.rack ?? [];
    const filled = serverRackTiles.map((tile, index) => {
      const staging = placements[`rack-${index}`];
      return staging ? { letter: null, points: 0, blank: false } : tile;
    });
    while (filled.length < 7) {
      filled.push({ letter: null, points: 0, blank: false });
    }
    return filled.slice(0, 7);
  }, [snapshot.players, player, placements]);

  useEffect(() => {
    placementsRef.current = placements;
  }, [placements]);

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

  const hydrateEventLog = useCallback((history: GameSnapshot['history']) => {
    if (!history || history.length === 0) {
      return;
    }
    eventCounterRef.current = 0;
    const entries = [...history].reverse().map((item) => {
      const message = { type: item.type, payload: item.payload } as WsMessage;
      const time = item.time ? new Date(item.time).toLocaleTimeString() : new Date().toLocaleTimeString();
      return {
        id: (eventCounterRef.current += 1),
        time,
        type: message.type,
        summary: summarizeEvent(message)
      } as EventLogEntry;
    });
    setEventLog(entries);
    const lastEntry = history[history.length - 1];
    const lastTime = lastEntry?.time ? Date.parse(lastEntry.time) : Date.now();
    setLastEventAt(Number.isNaN(lastTime) ? Date.now() : lastTime);
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
    historyHydratedRef.current = false;
    setSnapshot({ ...emptySnapshot, roomId: targetRoomId });
    resetLocalState();

    socket.onConnectionState((state) => {
      setConnectionState(state);
      if (state === 'reconnecting') {
        setHasSynced(false);
        historyHydratedRef.current = false;
      }
    });

    socket.onSnapshot((next) => {
      setSnapshot((prev) => mergeSnapshot(prev, next));
      const playerIndex = next.players.findIndex((p) => p.name === targetPlayer);
      const previousState = lastServerStateRef.current;
      const serverStateChanged = !previousState
        || previousState.boardTiles !== next.boardTiles
        || previousState.currentPlayerIndex !== next.currentPlayerIndex
        || previousState.status !== next.status;
      const hasPlacements = Object.keys(placementsRef.current).length > 0;
      const notPlayersTurn = playerIndex === -1 || next.currentPlayerIndex !== playerIndex;
      if (serverStateChanged || (hasPlacements && notPlayersTurn)) {
        resetLocalState();
      }
      lastServerStateRef.current = {
        boardTiles: next.boardTiles,
        currentPlayerIndex: next.currentPlayerIndex,
        status: next.status
      };
      setHasSynced(true);
      if (!historyHydratedRef.current) {
        hydrateEventLog(next.history);
        historyHydratedRef.current = true;
      }
    });

    socket.onEvent((message: WsMessage) => {
      appendEventLog(message);
      if (message.type !== 'STATE_SNAPSHOT' && message.type !== 'PONG') {
        resetLocalState();
        socket.requestSync();
      }
    });

    socket.connect(targetRoomId, targetPlayer);
  }, [appendEventLog, getSocket, hydrateEventLog, mergeSnapshot, resetLocalState]);

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
      const tile = rackTiles[rackIndex];
      if (tile && placements[`rack-${rackIndex}`]) {
        setActiveTile(null);
        return;
      }
      setActiveTile(tile ? { ...tile, rackIndex } : null);
      setActiveTileSource(null);
      setActiveTileLabel(undefined);
    } else if (activeId.startsWith('placement-')) {
      const coordinate = activeId.replace('placement-', '');
          const placement = placements[coordinate];
          if (placement) {
            setActiveTile({
              letter: placement.letter,
              points: placement.points,
          blank: placement.blank,
          rackIndex: placement.rackIndex ?? -1
            });
            setActiveTileSource(coordinate);
            setActiveTileLabel(placement.assignedLetter);
          }
    }
  };

  const handleTileSelect = (tile: RackTile, index: number) => {
    if (placements[`rack-${index}`]) {
      return;
    }
    setActiveTile({ ...tile, rackIndex: index });
  };

  const handleCellClick = (id: string) => {
    applyDrop(activeTile, cellId(id));
  };

  const applyDrop = (tile: (RackTile & { rackIndex: number }) | null, dropTarget: string | null) => {
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
          const rackIndex = next[activeTileSource]?.rackIndex;
          if (typeof rackIndex === 'number' && rackIndex >= 0) {
            delete next[`rack-${rackIndex}`];
          }
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
        const rackIndex = next[coordinate]?.rackIndex;
        if (typeof rackIndex === 'number' && rackIndex >= 0) {
          delete next[`rack-${rackIndex}`];
        }
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
      const rackKey = tile.rackIndex >= 0 ? `rack-${tile.rackIndex}` : null;
      if (rackKey) {
        Object.keys(next).forEach((key) => {
          if (next[key].coordinate === rackKey) {
            delete next[key];
          }
        });
        next[rackKey] = { ...boardTile, coordinate: rackKey, rackIndex: tile.rackIndex };
      }
      next[coordinate] = { ...boardTile, rackIndex: tile.rackIndex };
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
    const placementsPayload = Object.values(placements)
      .filter((tile) => !tile.coordinate.startsWith('rack-'))
      .map((tile) => ({
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

  const sendResign = () => {
    socketRef.current?.send({ type: 'RESIGN', payload: { player } });
  };


  const isSocketReady = connectionState === 'connected' && hasSynced;
  const isConnecting = connectionState === 'connecting' || connectionState === 'reconnecting';
  const hasBoardPlacements = Object.values(placements).some((tile) => !tile.coordinate.startsWith('rack-'));
  const bagTotals = useMemo(() => {
    const allTiles: Array<{ letter: string | null; points: number; blank: boolean; key: string }> = [];
    const addTiles = (letter: string | null, points: number, count: number, blank = false) => {
      for (let i = 0; i < count; i += 1) {
        allTiles.push({ letter, points, blank, key: blank ? 'BLANK' : letter ?? '' });
      }
    };
    addTiles('A', 1, 9);
    addTiles('Ą', 5, 1);
    addTiles('B', 3, 2);
    addTiles('C', 2, 3);
    addTiles('Ć', 6, 1);
    addTiles('D', 2, 3);
    addTiles('E', 1, 7);
    addTiles('Ę', 5, 1);
    addTiles('F', 5, 1);
    addTiles('G', 3, 2);
    addTiles('H', 3, 2);
    addTiles('I', 1, 8);
    addTiles('J', 3, 2);
    addTiles('K', 2, 3);
    addTiles('L', 2, 3);
    addTiles('Ł', 3, 2);
    addTiles('M', 2, 3);
    addTiles('N', 1, 5);
    addTiles('Ń', 7, 1);
    addTiles('O', 1, 6);
    addTiles('Ó', 5, 1);
    addTiles('P', 2, 3);
    addTiles('R', 1, 4);
    addTiles('S', 1, 4);
    addTiles('Ś', 5, 1);
    addTiles('T', 2, 3);
    addTiles('U', 3, 2);
    addTiles('W', 1, 4);
    addTiles('Y', 2, 4);
    addTiles('Z', 1, 5);
    addTiles('Ź', 9, 1);
    addTiles('Ż', 5, 1);
    addTiles(null, 0, 2, true);
    return allTiles;
  }, []);

  const usedTiles = useMemo(() => {
    const counts: Record<string, number> = {};
    const add = (key: string) => {
      counts[key] = (counts[key] ?? 0) + 1;
    };
    snapshot.board.forEach((tile) => {
      add(tile.blank ? 'BLANK' : tile.assignedLetter);
    });
    Object.values(placements)
      .filter((tile) => !tile.coordinate.startsWith('rack-'))
      .forEach((tile) => {
        add(tile.blank ? 'BLANK' : tile.assignedLetter);
      });
    rackTiles.forEach((tile) => {
      if (tile.blank) {
        add('BLANK');
      } else if (tile.letter) {
        add(tile.letter);
      }
    });
    return counts;
  }, [snapshot.board, placements, rackTiles]);

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
              <Rack tiles={rackTiles} onSelect={handleTileSelect} />
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
                  {snapshot.status === 'not_started' && (
                    <button type="button" onClick={handleStartGame} disabled={busy || !roomId || !isSocketReady}>
                      Start
                    </button>
                  )}
                </div>
                {error && <p className="error">{error}</p>}
                <div className="info-meta">
                  <div>
                    <span>Player</span>
                    <strong>{player || '—'}</strong>
                  </div>
                  <div>
                    <span>Bag</span>
                    <strong>
                      <button
                        type="button"
                        className="bag-button"
                        onClick={() => setBagModalOpen(true)}
                        disabled={!isSocketReady}
                      >
                        {snapshot.bagCount}
                      </button>
                    </strong>
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
                <button type="button" onClick={commitMove} disabled={!hasBoardPlacements || !isSocketReady}>Play tiles</button>
                <button type="button" onClick={() => setPlacements({})} disabled={!Object.keys(placements).length}>Reset</button>
                <button type="button" onClick={sendPass} disabled={!isSocketReady}>Pass</button>
                <button type="button" onClick={sendResign} disabled={!isSocketReady}>Resign</button>
              </div>
            </div>
          </aside>
        </main>
      </DndContext>
      {bagModalOpen && (
        <div className="modal-backdrop" role="presentation" onClick={() => setBagModalOpen(false)}>
          <div className="modal" role="dialog" aria-modal="true" aria-label="Bag contents" onClick={(event) => event.stopPropagation()}>
            <div className="modal-header">
              <h2>Bag contents</h2>
              <button type="button" className="modal-close" onClick={() => setBagModalOpen(false)}>
                Close
              </button>
            </div>
            <div className="modal-meta">
              <span>Bag: {snapshot.bagCount} tiles</span>
              <span>Opponent rack: {Math.max(0, (snapshot.players.find(p => p.name !== player)?.rackSize ?? 0))}</span>
            </div>
            <div className="modal-grid">
              {(() => {
                const remaining = { ...usedTiles };
                return bagTotals.map((tile, index) => {
                  const key = tile.blank ? 'BLANK' : tile.letter ?? '';
                  const isUsed = (remaining[key] ?? 0) > 0;
                  if (isUsed) {
                    remaining[key] -= 1;
                  }
                  return (
                    <div key={`${key}-${index}`} className={`modal-tile ${isUsed ? 'modal-tile--used' : ''}`}>
                      <Tile
                        tile={{ letter: tile.letter, points: tile.points, blank: tile.blank }}
                        label={tile.blank ? '?' : undefined}
                      />
                    </div>
                  );
                });
              })()}
            </div>
          </div>
        </div>
      )}
    </>
  );
}
