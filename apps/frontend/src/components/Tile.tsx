import type { RackTile } from '../types';

interface TileProps {
  tile: RackTile;
  label?: string;
  dragging?: boolean;
  pending?: boolean;
}

export default function Tile({ tile, label, dragging, pending }: TileProps) {
  if (tile.letter === null && !tile.blank) {
    return <div className="tile tile--empty" />;
  }
  const letter = label ?? tile.letter ?? '';
  const blankClass = tile.blank ? 'tile--blank' : '';
  return (
    <div className={`tile ${blankClass} ${dragging ? 'tile--dragging' : ''} ${pending ? 'tile--pending' : ''}`}>
      <span className="tile__letter">{letter}</span>
      {!tile.blank && <span className="tile__points">{tile.points}</span>}
    </div>
  );
}
