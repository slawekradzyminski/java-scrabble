import React from 'react';
import { useDroppable, useDraggable } from '@dnd-kit/core';
import type { BoardTile } from '../types';
import Tile from './Tile';
import { buildBoardIds, cellId } from '../utils';

interface BoardProps {
  tiles: BoardTile[];
  placements: Record<string, BoardTile>;
  onCellClick?: (id: string) => void;
}

const premiumMap: Record<string, string> = {
  A1: 'tw',
  A8: 'tw',
  A15: 'tw',
  H1: 'tw',
  H15: 'tw',
  O1: 'tw',
  O8: 'tw',
  O15: 'tw',
  B2: 'dw',
  B14: 'dw',
  C3: 'dw',
  C13: 'dw',
  D4: 'dw',
  D12: 'dw',
  E5: 'dw',
  E11: 'dw',
  H8: 'dw',
  K5: 'dw',
  K11: 'dw',
  L4: 'dw',
  L12: 'dw',
  M3: 'dw',
  M13: 'dw',
  N2: 'dw',
  N14: 'dw',
  B6: 'tl',
  B10: 'tl',
  F2: 'tl',
  F6: 'tl',
  F10: 'tl',
  F14: 'tl',
  J2: 'tl',
  J6: 'tl',
  J10: 'tl',
  J14: 'tl',
  N6: 'tl',
  N10: 'tl',
  A4: 'dl',
  A12: 'dl',
  C7: 'dl',
  C9: 'dl',
  D1: 'dl',
  D8: 'dl',
  D15: 'dl',
  G3: 'dl',
  G7: 'dl',
  G9: 'dl',
  G13: 'dl',
  H4: 'dl',
  H12: 'dl',
  I3: 'dl',
  I7: 'dl',
  I9: 'dl',
  I13: 'dl',
  L1: 'dl',
  L8: 'dl',
  L15: 'dl',
  M7: 'dl',
  M9: 'dl',
  O4: 'dl',
  O12: 'dl'
};

function DraggablePendingTile({
  tile,
  coordinate
}: {
  tile: BoardTile;
  coordinate: string;
}) {
  const { attributes, listeners, setNodeRef, transform, isDragging } = useDraggable({
    id: `placement-${coordinate}`
  });

  const style = transform
    ? { transform: `translate3d(${transform.x}px, ${transform.y}px, 0)` }
    : undefined;

  return (
    <div
      ref={setNodeRef}
      style={style}
      {...listeners}
      {...attributes}
      className="board-cell__pending-tile"
    >
      <Tile
        tile={{
          letter: tile.letter,
          points: tile.points,
          blank: tile.blank
        }}
        label={tile.assignedLetter}
        pending
        dragging={isDragging}
      />
    </div>
  );
}

function DroppableCell({
  id,
  tile,
  isPending,
  onClick
}: {
  id: string;
  tile?: BoardTile;
  isPending?: boolean;
  onClick?: (id: string) => void;
}) {
  const { setNodeRef, isOver } = useDroppable({ id: cellId(id) });
  const premium = premiumMap[id];
  return (
    <div
      ref={setNodeRef}
      className={`board-cell ${premium ? `board-cell--${premium}` : ''} ${isOver ? 'board-cell--highlight' : ''}`}
      onClick={() => onClick?.(id)}
    >
      {premium && <span className="board-cell__premium">{premium.toUpperCase()}</span>}
      {tile && !isPending && (
        <Tile
          tile={{
            letter: tile.letter,
            points: tile.points,
            blank: tile.blank
          }}
          label={tile.assignedLetter}
        />
      )}
      {tile && isPending && (
        <DraggablePendingTile tile={tile} coordinate={id} />
      )}
    </div>
  );
}

export default function Board({ tiles, placements, onCellClick }: BoardProps) {
  const ids = buildBoardIds();
  const byId = new Map<string, BoardTile>();
  tiles.forEach((tile) => byId.set(tile.coordinate, tile));
  const pendingCoordinates = new Set(Object.keys(placements));
  Object.values(placements).forEach((tile) => byId.set(tile.coordinate, tile));

  return (
    <div className="board">
      {ids.map((id) => (
        <DroppableCell
          key={id}
          id={id}
          tile={byId.get(id)}
          isPending={pendingCoordinates.has(id)}
          onClick={onCellClick}
        />
      ))}
    </div>
  );
}
