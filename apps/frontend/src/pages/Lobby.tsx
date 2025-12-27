import React, { useCallback, useEffect, useState } from 'react';
import type { RoomSummary } from '../types';
import { createRoom, listRooms } from '../api';

interface LobbyProps {
  player: string;
  onPlayerChange: (name: string) => void;
  onJoinRoom: (roomId: string, roomName?: string) => void;
}

export default function Lobby({ player, onPlayerChange, onJoinRoom }: LobbyProps) {
  const [rooms, setRooms] = useState<RoomSummary[]>([]);
  const [roomName, setRoomName] = useState('');
  const [roomQuery, setRoomQuery] = useState('');
  const [playWithAi, setPlayWithAi] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  const refreshRooms = useCallback(async (options?: { silent?: boolean }) => {
    const shouldSetBusy = !options?.silent;
    if (shouldSetBusy) {
      setBusy(true);
    }
    setError(null);
    try {
      const nextRooms = await listRooms();
      setRooms(nextRooms);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to load rooms');
    } finally {
      if (shouldSetBusy) {
        setBusy(false);
      }
    }
  }, []);

  useEffect(() => {
    void refreshRooms({ silent: true });
  }, [refreshRooms]);

  const handleCreateRoom = async () => {
    if (!roomName || !player) {
      return;
    }
    setBusy(true);
    setError(null);
    try {
      const created = await createRoom(roomName, player, playWithAi);
      setRoomName('');
      onJoinRoom(created.id, created.name);
      await refreshRooms();
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to create room');
    } finally {
      setBusy(false);
    }
  };

  const handleJoinRoom = (roomId: string, name?: string) => {
    if (!player) {
      setError('Enter a player name before joining a room.');
      return;
    }
    onJoinRoom(roomId, name);
  };

  const filteredRooms = rooms.filter((room) => {
    if (!roomQuery) {
      return true;
    }
    const query = roomQuery.trim().toLowerCase();
    return room.name.toLowerCase().includes(query) || room.id.toLowerCase().includes(query);
  });

  return (
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
              onChange={(e) => onPlayerChange(e.target.value)}
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
                onChange={(e) => setRoomName(e.target.value)}
              />
            </label>
            <label className="lobby-checkbox">
              <input
                type="checkbox"
                checked={playWithAi}
                onChange={(e) => setPlayWithAi(e.target.checked)}
              />
              Add computer opponent
            </label>
            <button type="button" onClick={handleCreateRoom} disabled={busy || !player || !roomName}>
              Create &amp; connect
            </button>
            {error && <p className="error">{error}</p>}
          </div>

          <div className="lobby-card">
            <div className="room-list-header">
              <div>
                <h3>Open rooms</h3>
                <span>{filteredRooms.length} of {rooms.length}</span>
              </div>
              <button type="button" onClick={() => void refreshRooms()} disabled={busy}>
                Refresh
              </button>
            </div>
            <label>
              Search rooms
              <input
                type="text"
                placeholder="Filter by name or id"
                value={roomQuery}
                onChange={(e) => setRoomQuery(e.target.value)}
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
                    <button type="button" onClick={() => handleJoinRoom(room.id, room.name)} disabled={busy || !player}>
                      Join
                    </button>
                  </li>
                ))
              )}
            </ul>
            {error && <p className="error">{error}</p>}
          </div>
        </div>
      </section>
    </main>
  );
}

