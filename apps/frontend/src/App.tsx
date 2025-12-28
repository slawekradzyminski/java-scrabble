import { useEffect, useState } from 'react';
import Lobby from './pages/Lobby';
import Game from './pages/Game';

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

const clearRoomUrl = () => {
  if (typeof window === 'undefined') {
    return;
  }
  if (window.location.pathname !== '/') {
    window.history.pushState({}, '', '/');
  }
};

export default function App() {
  const [roomId, setRoomId] = useState(getRoomIdFromPath);
  const [player, setPlayer] = useState(() => {
    if (typeof window === 'undefined') {
      return '';
    }
    return window.localStorage.getItem('scrabble.player') ?? '';
  });

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

  useEffect(() => {
    const handlePopState = () => {
      const newRoomId = getRoomIdFromPath();
      setRoomId(newRoomId);
    };

    window.addEventListener('popstate', handlePopState);
    return () => window.removeEventListener('popstate', handlePopState);
  }, []);

  const handleJoinRoom = (id: string, name?: string) => {
    setRoomId(id);
    updateRoomUrl(id, name);
  };

  const handleLeaveRoom = () => {
    setRoomId('');
    clearRoomUrl();
  };

  const isInGame = roomId && player;

  return (
    <div className="app">
      {isInGame ? (
        <Game
          roomId={roomId}
          player={player}
          onLeave={handleLeaveRoom}
        />
      ) : (
        <>
          <header className="topbar">
            <div>
              <div className="eyebrow">SCRABBLE LIVE</div>
              <h1>Realtime Board</h1>
            </div>
          </header>
          <Lobby
            player={player}
            onPlayerChange={setPlayer}
            onJoinRoom={handleJoinRoom}
          />
        </>
      )}
    </div>
  );
}
