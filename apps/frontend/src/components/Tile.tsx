import React from 'react';
import type { RackTile } from '../types';

interface TileProps {
  tile: RackTile;
  label?: string;
  dragging?: boolean;
}

export default function Tile({ tile, label, dragging }: TileProps) {
  const letter = label ?? tile.letter ?? '';
  return (
    <div className={`tile ${dragging ? 'tile--dragging' : ''}`}>
      <span className="tile__letter">{letter}</span>
      {!tile.blank && <span className="tile__points">{tile.points}</span>}
    </div>
  );
}
