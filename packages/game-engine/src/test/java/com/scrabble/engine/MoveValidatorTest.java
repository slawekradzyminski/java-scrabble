package com.scrabble.engine;

import java.util.Map;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MoveValidatorTest {

  @Test
  void firstMoveMustCoverCenterAndUseTwoTiles() {
    BoardState board = BoardState.empty();
    MovePlacement single = new MovePlacement(Map.of(
        Coordinate.parse("H8"), PlacedTile.fromTile(Tile.of('A', 1))));

    assertThrows(IllegalArgumentException.class, () -> MoveValidator.validatePlacement(board, single));

    MovePlacement missingCenter = new MovePlacement(Map.of(
        Coordinate.parse("H7"), PlacedTile.fromTile(Tile.of('A', 1)),
        Coordinate.parse("H9"), PlacedTile.fromTile(Tile.of('B', 3))));
    assertThrows(IllegalArgumentException.class, () -> MoveValidator.validatePlacement(board, missingCenter));
  }

  @Test
  void firstMoveAllowsValidLine() {
    BoardState board = BoardState.empty();
    MovePlacement move = new MovePlacement(Map.of(
        Coordinate.parse("H8"), PlacedTile.fromTile(Tile.of('A', 1)),
        Coordinate.parse("H9"), PlacedTile.fromTile(Tile.of('B', 3))));

    assertDoesNotThrow(() -> MoveValidator.validatePlacement(board, move));
  }

  @Test
  void moveMustBeCollinear() {
    BoardState board = BoardState.empty();
    MovePlacement move = new MovePlacement(Map.of(
        Coordinate.parse("H8"), PlacedTile.fromTile(Tile.of('A', 1)),
        Coordinate.parse("I9"), PlacedTile.fromTile(Tile.of('B', 3))));

    assertThrows(IllegalArgumentException.class, () -> MoveValidator.validatePlacement(board, move));
  }

  @Test
  void moveMustBeContiguousUnlessExistingTilesFillGap() {
    BoardState board = BoardState.empty();
    MovePlacement gap = new MovePlacement(Map.of(
        Coordinate.parse("H8"), PlacedTile.fromTile(Tile.of('A', 1)),
        Coordinate.parse("H10"), PlacedTile.fromTile(Tile.of('B', 3))));

    assertThrows(IllegalArgumentException.class, () -> MoveValidator.validatePlacement(board, gap));

    BoardState withExisting = board.withPlaced(Map.of(
        Coordinate.parse("H9"), PlacedTile.fromTile(Tile.of('C', 2))));
    assertDoesNotThrow(() -> MoveValidator.validatePlacement(withExisting, gap));
  }

  @Test
  void subsequentMoveMustTouchExistingTiles() {
    BoardState board = BoardState.empty().withPlaced(Map.of(
        Coordinate.parse("H8"), PlacedTile.fromTile(Tile.of('A', 1))));

    MovePlacement isolated = new MovePlacement(Map.of(
        Coordinate.parse("A1"), PlacedTile.fromTile(Tile.of('B', 3)),
        Coordinate.parse("A2"), PlacedTile.fromTile(Tile.of('C', 2))));

    assertThrows(IllegalArgumentException.class, () -> MoveValidator.validatePlacement(board, isolated));
  }
}
