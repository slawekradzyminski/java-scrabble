import { useDroppable, useDraggable } from '@dnd-kit/core';
import type { BoardTile } from '../types';
import Tile from './Tile';
import { buildBoardIds, cellId } from '../utils';
import { premiumMap } from '../utils/premiumMap';

interface BoardProps {
  tiles: BoardTile[];
  placements: Record<string, BoardTile>;
  onCellClick?: (id: string) => void;
}

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
  const showPremium = Boolean(premium) && !tile;
  return (
    <div
      ref={setNodeRef}
      className={`board-cell ${premium && !tile ? `board-cell--${premium}` : ''} ${isOver ? 'board-cell--highlight' : ''}`}
      onClick={() => onClick?.(id)}
    >
      {showPremium && <span className="board-cell__premium">{premium.toUpperCase()}</span>}
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
