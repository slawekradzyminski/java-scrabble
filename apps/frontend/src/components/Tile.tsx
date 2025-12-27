import React from 'react';
import type { RackTile } from '../types';

interface TileProps {
  tile: RackTile;
  label?: string;
  dragging?: boolean;
  pending?: boolean;
}

export default function Tile({ tile, label, dragging, pending }: TileProps) {
  const letter = label ?? tile.letter ?? '';
  return (
    <div className={`tile ${dragging ? 'tile--dragging' : ''} ${pending ? 'tile--pending' : ''}`}>
      <span className="tile__letter">{letter}</span>
      {!tile.blank && <span className="tile__points">{tile.points}</span>}
    </div>
  );
}
