import React, { useCallback, useEffect, useMemo, useState } from 'react';
import { closestCenter, DndContext, DragEndEvent, DragOverlay, DragStartEvent, PointerSensor, useSensor, useSensors } from '@dnd-kit/core';
import Board from './components/Board';
import PlayerList from './components/PlayerList';
import Rack from './components/Rack';
import Tile from './components/Tile';
import type { BoardTile, GameSnapshot, RackTile, RoomSummary, WsMessage } from './types';
import { createRoom, GameSocket, joinRoom, listRooms, startGame } from './api';

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

const getRoomIdFromPath = () => {
  if (typeof window === 'undefined') {
    return '';
  }
  const match = window.location.pathname.match(/^\/room\/([^/]+)$/);
  if (!match) {
    return '';
  }
  return match[1].split('-')[0] ?? '';
};

const slugify = (name: string) =>
  name
    .trim()
    .toLowerCase()
    .replace(/[^a-z0-9]+/g, '-')
    .replace(/^-+|-+$/g, '');

const updateRoomUrl = (id: string, name?: string) => {
  if (typeof window === 'undefined' || !id) {
    return;
  }
  const slug = name ? slugify(name) : '';
  const path = slug ? `/room/${id}-${slug}` : `/room/${id}`;
  if (window.location.pathname === path) {
    return;
  }
  window.history.pushState({}, '', path);
};

export default function App() {
  const initialRoomId = getRoomIdFromPath();
  const [roomId, setRoomId] = useState(initialRoomId);
  const [roomName, setRoomName] = useState('');
  const [player, setPlayer] = useState(() => {
    if (typeof window === 'undefined') {
      return '';
    }
    return window.localStorage.getItem('scrabble.player') ?? '';
  });
  const [rooms, setRooms] = useState<RoomSummary[]>([]);
  const [roomQuery, setRoomQuery] = useState(initialRoomId);
  const [lobbyError, setLobbyError] = useState<string | null>(null);
  const [lobbyBusy, setLobbyBusy] = useState(false);
  const [connected, setConnected] = useState(false);
  const [snapshot, setSnapshot] = useState<GameSnapshot>(emptySnapshot);
  const [placements, setPlacements] = useState<PlacementState>({});
  const [activeTile, setActiveTile] = useState<RackTile | null>(null);
  const socket = useMemo(() => new GameSocket(), []);

  const sensors = useSensors(useSensor(PointerSensor, { activationConstraint: { distance: 8 } }));

  const currentPlayer = snapshot.players[snapshot.currentPlayerIndex ?? -1];
  const rackTiles = currentPlayer?.rack ?? [];

  const refreshRooms = useCallback(async (options?: { silent?: boolean }) => {
    const shouldSetBusy = !options?.silent;
    if (shouldSetBusy) {
      setLobbyBusy(true);
    }
    setLobbyError(null);
    try {
      const nextRooms = await listRooms();
      setRooms(nextRooms);
    } catch (error) {
      setLobbyError(error instanceof Error ? error.message : 'Failed to load rooms');
    } finally {
      if (shouldSetBusy) {
        setLobbyBusy(false);
      }
    }
  }, []);

  useEffect(() => {
    void refreshRooms({ silent: true });
  }, [refreshRooms]);

  useEffect(() => {
    if (typeof window === 'undefined') {
      return;
    }
    if (!player) {
      window.localStorage.removeItem('scrabble.player');
      return;
    }
    window.localStorage.setItem('scrabble.player', player);
  }, [player]);

  const connectSocket = (targetRoomId: string, targetPlayer: string) => {
    socket.disconnect();
    socket.onSnapshot((next) => {
      setSnapshot(next);
      setPlacements({});
      setActiveTile(null);
      setConnected(true);
    });
    socket.onEvent((message: WsMessage) => {
      if (message.type === 'MOVE_ACCEPTED' || message.type === 'MOVE_REJECTED') {
        setPlacements({});
        setActiveTile(null);
      }
    });
    socket.connect(targetRoomId, targetPlayer);
  };

  const createAndConnect = async () => {
    if (!roomName || !player) {
      return;
    }
    setLobbyBusy(true);
    setLobbyError(null);
    try {
      const created = await createRoom(roomName, player);
      setRoomId(created.id);
      setRoomName('');
      updateRoomUrl(created.id, created.name);
      connectSocket(created.id, player);
      await refreshRooms();
    } catch (error) {
      setLobbyError(error instanceof Error ? error.message : 'Failed to create room');
    } finally {
      setLobbyBusy(false);
    }
  };

  const joinAndConnect = async (targetRoomId: string, targetRoomName?: string) => {
    if (!player || !targetRoomId) {
      if (!player) {
        setLobbyError('Enter a player name before joining a room.');
      }
      return;
    }
    setRoomId(targetRoomId);
    setLobbyBusy(true);
    setLobbyError(null);
    try {
      const joined = await joinRoom(targetRoomId, player);
      connectSocket(targetRoomId, player);
      updateRoomUrl(targetRoomId, targetRoomName ?? joined.name);
      await refreshRooms();
    } catch (error) {
      setLobbyError(error instanceof Error ? error.message : 'Failed to join room');
    } finally {
      setLobbyBusy(false);
    }
  };

  const startRoomGame = async () => {
    if (!roomId) {
      return;
    }
    setLobbyBusy(true);
    setLobbyError(null);
    try {
      await startGame(roomId);
      socket.send({ type: 'SYNC', payload: { player } });
    } catch (error) {
      setLobbyError(error instanceof Error ? error.message : 'Failed to start game');
    } finally {
      setLobbyBusy(false);
    }
  };

  const handleDragStart = (event: DragStartEvent) => {
    const rackIndex = parseInt(String(event.active.id).replace('rack-', ''), 10);
    setActiveTile(rackTiles[rackIndex] ?? null);
  };

  const applyDrop = (tile: RackTile | null, dropTarget: string | null) => {
    if (!dropTarget || !tile) {
      setActiveTile(null);
      return;
    }
    const dropId = dropTarget.toString();
    if (!dropId.startsWith('cell-')) {
      setActiveTile(null);
      return;
    }
    const coordinate = dropId.replace('cell-', '');
    let assignedLetter = tile.letter ?? '';
    if (tile.blank) {
      const promptValue = window.prompt('Blank tile: choose a letter (A-Z)');
      if (!promptValue || promptValue.trim().length !== 1) {
        setActiveTile(null);
        return;
      }
      assignedLetter = promptValue.trim().toUpperCase();
    }
    const boardTile: BoardTile = {
      coordinate,
      letter: tile.letter,
      points: tile.points,
      blank: tile.blank,
      assignedLetter
    };
    setPlacements((prev) => ({ ...prev, [coordinate]: boardTile }));
    setActiveTile(null);
  };

  const handleDragEnd = (event: DragEndEvent) => {
    applyDrop(activeTile, event.over?.id?.toString() ?? null);
  };
  const commitMove = () => {
    if (!connected || !roomId) {
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
    socket.send({ type: 'PASS', payload: { player } });
  };

  const sendChallenge = () => {
    socket.send({ type: 'CHALLENGE', payload: { player } });
  };

  const sendResign = () => {
    socket.send({ type: 'RESIGN', payload: { player } });
  };

  const clearPlacements = () => setPlacements({});

  const filteredRooms = rooms.filter((room) => {
    if (!roomQuery) {
      return true;
    }
    const query = roomQuery.trim().toLowerCase();
    return room.name.toLowerCase().includes(query) || room.id.toLowerCase().includes(query);
  });

  if (!connected) {
    return (
      <div className="app">
        <header className="topbar">
          <div>
            <div className="eyebrow">SCRABBLE LIVE</div>
            <h1>Realtime Board</h1>
          </div>
        </header>

        <main className="lobby-layout">
          <section className="panel lobby lobby-view">
            <div className="lobby-header">
              <h2>Lobby</h2>
              <p>Set your player name once, then create or join rooms.</p>
              <label className="lobby-name">
                Player name
                <input
                  type="text"
                  placeholder="Player name"
                  value={player}
                  onChange={(event) => setPlayer(event.target.value)}
                />
              </label>
            </div>

            <div className="lobby-grid">
              <div className="lobby-card">
                <h3>Create a room</h3>
                <label>
                  New room name
                  <input
                    type="text"
                    placeholder="Room name"
                    value={roomName}
                    onChange={(event) => setRoomName(event.target.value)}
                  />
                </label>
                <button type="button" onClick={createAndConnect} disabled={lobbyBusy || !player || !roomName}>
                  Create &amp; connect
                </button>
                {lobbyError && <p className="error">{lobbyError}</p>}
              </div>

              <div className="lobby-card">
                <div className="room-list-header">
                  <div>
                    <h3>Open rooms</h3>
                    <span>{filteredRooms.length} of {rooms.length}</span>
                  </div>
                  <button type="button" onClick={() => void refreshRooms()} disabled={lobbyBusy}>
                    Refresh
                  </button>
                </div>
                <label>
                  Search rooms
                  <input
                    type="text"
                    placeholder="Filter by name or id"
                    value={roomQuery}
                    onChange={(event) => setRoomQuery(event.target.value)}
                  />
                </label>
                <ul className="room-list">
                  {filteredRooms.length === 0 ? (
                    <li>No rooms yet.</li>
                  ) : (
                    filteredRooms.map((room) => (
                      <li key={room.id}>
                        <div>
                          <strong>{room.name}</strong>
                          <span>#{room.id} · {room.players.length} players</span>
                        </div>
                        <button type="button" onClick={() => joinAndConnect(room.id, room.name)} disabled={lobbyBusy || !player}>
                          Join
                        </button>
                      </li>
                    ))
                  )}
                </ul>
              </div>
            </div>
          </section>
        </main>
      </div>
    );
  }

  return (
    <div className="app">
      <header className="topbar">
        <div>
          <div className="eyebrow">SCRABBLE LIVE</div>
          <h1>Realtime Board</h1>
        </div>
      </header>

      <DndContext
        sensors={sensors}
        collisionDetection={closestCenter}
        onDragStart={handleDragStart}
        onDragEnd={handleDragEnd}
      >
        <main className="layout">
          <section className="game-column">
            <div className="board-wrap">
              <Board tiles={snapshot.board} placements={placements} />
              <DragOverlay>
                {activeTile ? <Tile tile={activeTile} dragging /> : null}
              </DragOverlay>
            </div>
            <div className="panel rack-panel">
              <h2>Your rack</h2>
              <Rack tiles={rackTiles} />
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
                  <button type="button" onClick={startRoomGame} disabled={lobbyBusy || !roomId}>Start</button>
                </div>
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
                    <strong>{snapshot.status}</strong>
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

              <div className="info-section actions">
                <button type="button" onClick={commitMove} disabled={!Object.keys(placements).length}>Play tiles</button>
                <button type="button" onClick={clearPlacements}>Clear</button>
                <button type="button" onClick={sendPass}>Pass</button>
                <button type="button" onClick={sendChallenge}>Challenge</button>
                <button type="button" onClick={sendResign}>Resign</button>
              </div>
            </div>
          </aside>
        </main>
      </DndContext>
    </div>
  );
}
