import React from 'react';
import type { PlayerSnapshot } from '../types';

interface PlayerListProps {
  players: PlayerSnapshot[];
  currentIndex: number | null;
}

export default function PlayerList({ players, currentIndex }: PlayerListProps) {
  return (
    <div className="player-list">
      {players.map((player, index) => (
        <div key={player.name} className={`player-card ${currentIndex === index ? 'player-card--active' : ''}`}>
          <div className="player-card__name">{player.name}</div>
          <div className="player-card__score">{player.score} pts</div>
          <div className="player-card__rack">Tiles: {player.rackSize}/{player.rackCapacity}</div>
        </div>
      ))}
    </div>
  );
}
