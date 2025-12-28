import type { BoardTile } from '../../../types';
import type { PlacementState } from '../types';
import { coordToId, idToCoord } from '../../../utils';
import { premiumMap } from '../../../utils/premiumMap';

const LETTER_MULTIPLIERS: Record<string, number> = {
  dl: 2,
  tl: 3
};

const WORD_MULTIPLIERS: Record<string, number> = {
  dw: 2,
  tw: 3
};

type TileInfo = {
  letter: string;
  points: number;
  blank: boolean;
  assignedLetter: string;
};

type Direction = { dr: number; dc: number };

const HORIZONTAL: Direction = { dr: 0, dc: 1 };
const VERTICAL: Direction = { dr: 1, dc: 0 };

const isPlacementCoord = (coord: string, placements: PlacementState) => Boolean(placements[coord]);

const isBoardPlacement = (coord: string) => !coord.startsWith('rack-');

const toTileInfo = (tile: BoardTile): TileInfo => ({
  letter: tile.letter ?? '',
  points: tile.points,
  blank: tile.blank,
  assignedLetter: tile.assignedLetter
});

const buildTileMap = (board: BoardTile[], placements: PlacementState) => {
  const map = new Map<string, TileInfo>();
  board.forEach((tile) => {
    map.set(tile.coordinate, toTileInfo(tile));
  });
  Object.values(placements)
    .filter((tile) => isBoardPlacement(tile.coordinate))
    .forEach((tile) => {
      map.set(tile.coordinate, toTileInfo(tile));
    });
  return map;
};

const collectWord = (start: string, dir: Direction, tiles: Map<string, TileInfo>) => {
  let { row, col } = idToCoord(start);
  while (true) {
    const prev = coordToId({ row: row - dir.dr, col: col - dir.dc });
    if (!tiles.has(prev)) {
      break;
    }
    row -= dir.dr;
    col -= dir.dc;
  }

  const coords: string[] = [];
  while (true) {
    const id = coordToId({ row, col });
    const tile = tiles.get(id);
    if (!tile) {
      break;
    }
    coords.push(id);
    row += dir.dr;
    col += dir.dc;
  }
  return coords;
};

const scoreWord = (coords: string[], tiles: Map<string, TileInfo>, placements: PlacementState) => {
  let sum = 0;
  let multiplier = 1;
  coords.forEach((coord) => {
    const tile = tiles.get(coord);
    if (!tile) {
      return;
    }
    const isNew = isPlacementCoord(coord, placements);
    const premium = isNew ? premiumMap[coord] : undefined;
    const letterMultiplier = premium ? (LETTER_MULTIPLIERS[premium] ?? 1) : 1;
    const wordMultiplier = premium ? (WORD_MULTIPLIERS[premium] ?? 1) : 1;
    const basePoints = tile.blank ? 0 : tile.points;
    sum += basePoints * letterMultiplier;
    multiplier *= wordMultiplier;
  });
  return sum * multiplier;
};

const wordText = (coords: string[], tiles: Map<string, TileInfo>) =>
  coords.map((coord) => tiles.get(coord)?.assignedLetter ?? '').join('');

export type MovePreview = {
  score: number;
  words: string[];
};

export function scoreMove(board: BoardTile[], placements: PlacementState): MovePreview | null {
  const placementCoords = Object.values(placements)
    .filter((tile) => isBoardPlacement(tile.coordinate))
    .map((tile) => tile.coordinate);

  if (placementCoords.length === 0) {
    return null;
  }

  const tiles = buildTileMap(board, placements);
  const rows = new Set(placementCoords.map((coord) => idToCoord(coord).row));
  const cols = new Set(placementCoords.map((coord) => idToCoord(coord).col));

  let direction: Direction | null = null;
  if (rows.size === 1) {
    direction = HORIZONTAL;
  } else if (cols.size === 1) {
    direction = VERTICAL;
  } else if (placementCoords.length === 1) {
    const coord = placementCoords[0];
    const { row, col } = idToCoord(coord);
    const left = coordToId({ row, col: col - 1 });
    const right = coordToId({ row, col: col + 1 });
    const up = coordToId({ row: row - 1, col });
    const down = coordToId({ row: row + 1, col });
    if (tiles.has(left) || tiles.has(right)) {
      direction = HORIZONTAL;
    } else if (tiles.has(up) || tiles.has(down)) {
      direction = VERTICAL;
    } else {
      direction = HORIZONTAL;
    }
  }

  if (!direction) {
    return null;
  }

  const mainWordCoords = collectWord(placementCoords[0], direction, tiles);
  const words: string[] = [];
  let total = 0;

  if (mainWordCoords.length > 0) {
    total += scoreWord(mainWordCoords, tiles, placements);
    words.push(wordText(mainWordCoords, tiles));
  }

  const perpendicular = direction === HORIZONTAL ? VERTICAL : HORIZONTAL;
  placementCoords.forEach((coord) => {
    const crossCoords = collectWord(coord, perpendicular, tiles);
    if (crossCoords.length > 1) {
      total += scoreWord(crossCoords, tiles, placements);
      words.push(wordText(crossCoords, tiles));
    }
  });

  if (placementCoords.length === 7) {
    total += 50;
  }

  return {
    score: total,
    words
  };
}
