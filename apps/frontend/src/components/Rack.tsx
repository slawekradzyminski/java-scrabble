import React from 'react';
import { useDraggable } from '@dnd-kit/core';
import type { RackTile } from '../types';
import Tile from './Tile';

interface RackProps {
  tiles: RackTile[];
}

function DraggableTile({ tile, index }: { tile: RackTile; index: number }) {
  const { attributes, listeners, setNodeRef, transform, isDragging } = useDraggable({
    id: `rack-${index}`
  });

  const style = transform
    ? { transform: `translate3d(${transform.x}px, ${transform.y}px, 0)` }
    : undefined;

  return (
    <button
      ref={setNodeRef}
      className="rack-tile"
      style={style}
      {...listeners}
      {...attributes}
      type="button"
      aria-label={`Tile ${tile.letter ?? 'blank'}`}
    >
      <Tile tile={tile} dragging={isDragging} />
    </button>
  );
}

export default function Rack({ tiles }: RackProps) {
  return (
    <div className="rack">
      {tiles.map((tile, index) => (
        <DraggableTile key={`${tile.letter ?? 'blank'}-${index}`} tile={tile} index={index} />
      ))}
    </div>
  );
}
