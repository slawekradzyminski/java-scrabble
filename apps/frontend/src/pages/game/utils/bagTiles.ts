import type { BoardTile, RackTile } from '../../../types';
import type { PlacementState } from '../types';

export type BagTile = {
  letter: string | null;
  points: number;
  blank: boolean;
  key: string;
};

const BAG_DISTRIBUTION: Array<{ letter: string | null; points: number; count: number; blank?: boolean }> = [
  { letter: 'A', points: 1, count: 9 },
  { letter: 'Ą', points: 5, count: 1 },
  { letter: 'B', points: 3, count: 2 },
  { letter: 'C', points: 2, count: 3 },
  { letter: 'Ć', points: 6, count: 1 },
  { letter: 'D', points: 2, count: 3 },
  { letter: 'E', points: 1, count: 7 },
  { letter: 'Ę', points: 5, count: 1 },
  { letter: 'F', points: 5, count: 1 },
  { letter: 'G', points: 3, count: 2 },
  { letter: 'H', points: 3, count: 2 },
  { letter: 'I', points: 1, count: 8 },
  { letter: 'J', points: 3, count: 2 },
  { letter: 'K', points: 2, count: 3 },
  { letter: 'L', points: 2, count: 3 },
  { letter: 'Ł', points: 3, count: 2 },
  { letter: 'M', points: 2, count: 3 },
  { letter: 'N', points: 1, count: 5 },
  { letter: 'Ń', points: 7, count: 1 },
  { letter: 'O', points: 1, count: 6 },
  { letter: 'Ó', points: 5, count: 1 },
  { letter: 'P', points: 2, count: 3 },
  { letter: 'R', points: 1, count: 4 },
  { letter: 'S', points: 1, count: 4 },
  { letter: 'Ś', points: 5, count: 1 },
  { letter: 'T', points: 2, count: 3 },
  { letter: 'U', points: 3, count: 2 },
  { letter: 'W', points: 1, count: 4 },
  { letter: 'Y', points: 2, count: 4 },
  { letter: 'Z', points: 1, count: 5 },
  { letter: 'Ź', points: 9, count: 1 },
  { letter: 'Ż', points: 5, count: 1 },
  { letter: null, points: 0, count: 2, blank: true }
];

export const BAG_TILES: BagTile[] = (() => {
  const allTiles: BagTile[] = [];
  for (const tile of BAG_DISTRIBUTION) {
    for (let i = 0; i < tile.count; i += 1) {
      allTiles.push({
        letter: tile.letter,
        points: tile.points,
        blank: Boolean(tile.blank),
        key: tile.blank ? 'BLANK' : tile.letter ?? ''
      });
    }
  }
  return allTiles;
})();

export function buildUsedTileCounts(
  board: BoardTile[],
  placements: PlacementState,
  rackTiles: RackTile[]
) {
  const counts: Record<string, number> = {};
  const add = (key: string) => {
    counts[key] = (counts[key] ?? 0) + 1;
  };
  board.forEach((tile) => {
    add(tile.blank ? 'BLANK' : tile.assignedLetter);
  });
  Object.values(placements)
    .filter((tile) => !tile.coordinate.startsWith('rack-'))
    .forEach((tile) => {
      add(tile.blank ? 'BLANK' : tile.assignedLetter);
    });
  rackTiles.forEach((tile) => {
    if (tile.blank) {
      add('BLANK');
    } else if (tile.letter) {
      add(tile.letter);
    }
  });
  return counts;
}
