export type Coord = { row: number; col: number };

const ALPHABET = 'ABCDEFGHIJKLMNO';

export function coordToId(coord: Coord) {
  return `${ALPHABET[coord.row]}${coord.col + 1}`;
}

export function idToCoord(id: string): Coord {
  const row = ALPHABET.indexOf(id[0]);
  const col = Number(id.slice(1)) - 1;
  return { row, col };
}

export function buildBoardIds() {
  const ids: string[] = [];
  for (let r = 0; r < 15; r += 1) {
    for (let c = 0; c < 15; c += 1) {
      ids.push(coordToId({ row: r, col: c }));
    }
  }
  return ids;
}

export function tileId(index: number) {
  return `rack-${index}`;
}

export function boardTileId(index: number) {
  return `board-${index}`;
}

export function cellId(id: string) {
  return `cell-${id}`;
}
