import React from 'react';
import { DragOverlay } from '@dnd-kit/core';
import Board from '../../../components/Board';
import Rack from '../../../components/Rack';
import Tile from '../../../components/Tile';
import type { BoardTile, RackTile } from '../../../types';
import type { ActiveTile, PlacementState } from '../types';

interface BoardSectionProps {
  isConnecting: boolean;
  connectionLabel: string;
  board: BoardTile[];
  placements: PlacementState;
  activeTile: ActiveTile | null;
  activeTileLabel: string | undefined;
  rackTiles: RackTile[];
  onCellClick: (id: string) => void;
  onTileSelect: (tile: RackTile, index: number) => void;
  applyDrop: (tile: ActiveTile | null, dropTarget: string | null) => void;
}

export default function BoardSection({
  isConnecting,
  connectionLabel,
  board,
  placements,
  activeTile,
  activeTileLabel,
  rackTiles,
  onCellClick,
  onTileSelect,
  applyDrop
}: BoardSectionProps) {
  return (
    <section className="game-column">
      <div className="board-wrap">
        {isConnecting ? (
          <div className="connecting-overlay">
            <p>{connectionLabel}</p>
          </div>
        ) : (
          <>
            <Board tiles={board} placements={placements} onCellClick={onCellClick} />
            <DragOverlay dropAnimation={null} adjustScale={false} className="drag-overlay">
              {activeTile ? <Tile tile={activeTile} dragging label={activeTileLabel} /> : null}
            </DragOverlay>
          </>
        )}
      </div>
      <div className="panel rack-panel">
        <Rack tiles={rackTiles} onSelect={onTileSelect} />
        {import.meta.env.MODE === 'test' && (
          <div className="test-controls">
            <button
              type="button"
              className="test-only"
              onClick={() => applyDrop(rackTiles[0] ? { ...rackTiles[0], rackIndex: 0 } : null, 'cell-H8')}
            >
              Test add placement
            </button>
            <button
              type="button"
              className="test-only"
              onClick={() => applyDrop(null, null)}
            >
              Test drop empty
            </button>
            <button
              type="button"
              className="test-only"
              onClick={() => applyDrop(rackTiles[0] ? { ...rackTiles[0], rackIndex: 0 } : null, 'rack-1')}
            >
              Test drop invalid target
            </button>
            <button
              type="button"
              className="test-only"
              onClick={() => applyDrop({ letter: null, points: 0, blank: true, rackIndex: 0 }, 'cell-H8')}
            >
              Test drop blank
            </button>
          </div>
        )}
      </div>
    </section>
  );
}
