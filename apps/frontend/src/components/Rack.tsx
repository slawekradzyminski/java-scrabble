import { useDraggable, useDroppable } from '@dnd-kit/core';
import type { RackTile } from '../types';
import Tile from './Tile';

interface RackProps {
  tiles: RackTile[];
  onSelect?: (tile: RackTile, index: number) => void;
}

function DraggableTile({
  tile,
  index,
  onSelect
}: {
  tile: RackTile;
  index: number;
  onSelect?: (tile: RackTile, index: number) => void;
}) {
  const isPlaceholder = tile.letter === null && !tile.blank;
  const { attributes, listeners, setNodeRef, transform, isDragging } = useDraggable({
    id: `rack-${index}`,
    disabled: isPlaceholder
  });

  const style = transform
    ? { transform: `translate3d(${transform.x}px, ${transform.y}px, 0)` }
    : undefined;

  if (isPlaceholder) {
    return <div className="rack-slot" aria-hidden="true" />;
  }

  return (
    <button
      ref={setNodeRef}
      className="rack-tile"
      style={style}
      {...listeners}
      {...attributes}
      type="button"
      aria-label={`Tile ${tile.letter ?? 'blank'}`}
      onClick={() => onSelect?.(tile, index)}
    >
      <Tile tile={tile} dragging={isDragging} />
    </button>
  );
}

export default function Rack({ tiles, onSelect }: RackProps) {
  const { setNodeRef, isOver } = useDroppable({ id: 'rack-drop' });
  return (
    <div ref={setNodeRef} className={`rack ${isOver ? 'rack--drop-target' : ''}`}>
      {tiles.map((tile, index) => (
        <DraggableTile
          key={`${tile.letter ?? 'blank'}-${index}`}
          tile={tile}
          index={index}
          onSelect={onSelect}
        />
      ))}
    </div>
  );
}
