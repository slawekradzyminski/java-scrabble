import { boardTileId, buildBoardIds, cellId, coordToId, idToCoord, tileId } from './utils';

describe('utils', () => {
  it('converts coordinates to ids and back', () => {
    // given
    const id = coordToId({ row: 7, col: 7 });

    // when
    const coord = idToCoord(id);

    // then
    expect(id).toBe('H8');
    expect(coord).toEqual({ row: 7, col: 7 });
  });

  it('builds full board ids', () => {
    // given
    const ids = buildBoardIds();

    // when
    const first = ids[0];
    const last = ids[ids.length - 1];

    // then
    expect(ids).toHaveLength(225);
    expect(first).toBe('A1');
    expect(last).toBe('O15');
  });

  it('creates cell ids', () => {
    // given
    const id = 'H8';

    // when
    const value = cellId(id);

    // then
    expect(value).toBe('cell-H8');
  });

  it('creates tile ids', () => {
    // given
    const index = 3;

    // when
    const rack = tileId(index);
    const board = boardTileId(index);

    // then
    expect(rack).toBe('rack-3');
    expect(board).toBe('board-3');
  });
});
