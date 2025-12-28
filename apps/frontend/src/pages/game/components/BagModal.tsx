import Tile from '../../../components/Tile';
import type { BagTile } from '../utils/bagTiles';

interface BagModalProps {
  bagCount: number;
  opponentRackSize: number;
  bagTiles: BagTile[];
  usedTiles: Record<string, number>;
  onClose: () => void;
}

export default function BagModal({ bagCount, opponentRackSize, bagTiles, usedTiles, onClose }: BagModalProps) {
  return (
    <div className="modal-backdrop" role="presentation" onClick={onClose}>
      <div className="modal" role="dialog" aria-modal="true" aria-label="Bag contents" onClick={(event) => event.stopPropagation()}>
        <div className="modal-header">
          <h2>Bag contents</h2>
          <button type="button" className="modal-close" onClick={onClose}>
            Close
          </button>
        </div>
        <div className="modal-meta">
          <span>Bag: {bagCount} tiles</span>
          <span>Opponent rack: {Math.max(0, opponentRackSize)}</span>
        </div>
        <div className="modal-grid">
          {(() => {
            const remaining = { ...usedTiles };
            return bagTiles.map((tile, index) => {
              const key = tile.blank ? 'BLANK' : tile.letter ?? '';
              const isUsed = (remaining[key] ?? 0) > 0;
              if (isUsed) {
                remaining[key] -= 1;
              }
              return (
                <div key={`${key}-${index}`} className={`modal-tile ${isUsed ? 'modal-tile--used' : ''}`}>
                  <Tile
                    tile={{ letter: tile.letter, points: tile.points, blank: tile.blank }}
                    label={tile.blank ? '?' : undefined}
                  />
                </div>
              );
            });
          })()}
        </div>
      </div>
    </div>
  );
}
