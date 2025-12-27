import React, { useMemo, useState } from 'react';
import { DndContext, DragEndEvent, DragOverlay, PointerSensor, useSensor, useSensors } from '@dnd-kit/core';
import Board from './components/Board';
import PlayerList from './components/PlayerList';
import Rack from './components/Rack';
import Tile from './components/Tile';
import type { BoardTile, GameSnapshot, RackTile, WsMessage } from './types';
import { GameSocket } from './api';

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

export default function App() {
  const [roomId, setRoomId] = useState('');
  const [player, setPlayer] = useState('');
  const [connected, setConnected] = useState(false);
  const [snapshot, setSnapshot] = useState<GameSnapshot>(emptySnapshot);
  const [placements, setPlacements] = useState<PlacementState>({});
  const [activeTile, setActiveTile] = useState<RackTile | null>(null);
  const socket = useMemo(() => new GameSocket(), []);

  const sensors = useSensors(useSensor(PointerSensor, { activationConstraint: { distance: 8 } }));

  const currentPlayer = snapshot.players[snapshot.currentPlayerIndex ?? -1];
  const rackTiles = currentPlayer?.rack ?? [];

  const connect = () => {
    if (!roomId || !player) {
      return;
    }
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
    socket.connect(roomId, player);
  };

  const handleDragStart = (event: { active: { id: string } }) => {
    const rackIndex = parseInt(event.active.id.replace('rack-', ''), 10);
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

  return (
    <div className="app">
      <header className="topbar">
        <div>
          <div className="eyebrow">SCRABBLE LIVE</div>
          <h1>Realtime Board</h1>
        </div>
        <div className="room">
          <input
            type="text"
            placeholder="Room id"
            value={roomId}
            onChange={(event) => setRoomId(event.target.value)}
          />
          <input
            type="text"
            placeholder="Player name"
            value={player}
            onChange={(event) => setPlayer(event.target.value)}
          />
          <button type="button" onClick={connect}>Connect</button>
        </div>
      </header>

      <main className="layout">
        <section className="board-wrap">
          <DndContext
            sensors={sensors}
            onDragStart={handleDragStart}
            onDragEnd={handleDragEnd}
          >
            <Board tiles={snapshot.board} placements={placements} />
            <DragOverlay>
              {activeTile ? <Tile tile={activeTile} dragging /> : null}
            </DragOverlay>
          </DndContext>
        </section>

        <aside className="sidebar">
          <div className="panel">
            <h2>Room</h2>
            <p>Room: <strong>{snapshot.roomId || '—'}</strong></p>
            <p>Status: <strong>{snapshot.status}</strong></p>
            <p>Bag: <strong>{snapshot.bagCount}</strong></p>
            {snapshot.winner && <p>Winner: <strong>{snapshot.winner}</strong></p>}
          </div>

          <div className="panel">
            <h2>Players</h2>
            <PlayerList players={snapshot.players} currentIndex={snapshot.currentPlayerIndex} />
          </div>

          <div className="panel">
            <h2>Pending</h2>
            {snapshot.pending ? (
              <>
                <p>Score: {snapshot.pending.score}</p>
                <p>Words: {snapshot.pending.words.map((word) => word.text).join(', ') || '—'}</p>
              </>
            ) : (
              <p>None</p>
            )}
          </div>

          <div className="panel actions">
            <button type="button" onClick={commitMove} disabled={!Object.keys(placements).length}>Play tiles</button>
            <button type="button" onClick={clearPlacements}>Clear</button>
            <button type="button" onClick={sendPass}>Pass</button>
            <button type="button" onClick={sendChallenge}>Challenge</button>
            <button type="button" onClick={sendResign}>Resign</button>
          </div>
        </aside>
      </main>

      <footer className="rack-wrap">
        <div className="rack-title">Your rack</div>
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
      </footer>
    </div>
  );
}
