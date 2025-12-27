package com.scrabble.engine;

import java.util.Map;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MoveValidatorTest {

  @Test
  void firstMoveMustCoverCenterAndUseTwoTiles() {
    // given
    BoardState board = BoardState.empty();
    MovePlacement single = new MovePlacement(Map.of(
        Coordinate.parse("H8"), PlacedTile.fromTile(Tile.of('A', 1))));

    // when
    assertThatThrownBy(() -> MoveValidator.validatePlacement(board, single))
        .isInstanceOf(IllegalArgumentException.class);

    MovePlacement missingCenter = new MovePlacement(Map.of(
        Coordinate.parse("H7"), PlacedTile.fromTile(Tile.of('A', 1)),
        Coordinate.parse("H9"), PlacedTile.fromTile(Tile.of('B', 3))));
    assertThatThrownBy(() -> MoveValidator.validatePlacement(board, missingCenter))
        .isInstanceOf(IllegalArgumentException.class);
    // then
  }

  @Test
  void firstMoveAllowsValidLine() {
    // given
    BoardState board = BoardState.empty();
    MovePlacement move = new MovePlacement(Map.of(
        Coordinate.parse("H8"), PlacedTile.fromTile(Tile.of('A', 1)),
        Coordinate.parse("H9"), PlacedTile.fromTile(Tile.of('B', 3))));

    // when
    assertThatCode(() -> MoveValidator.validatePlacement(board, move))
        .doesNotThrowAnyException();
    // then
  }

  @Test
  void moveMustBeCollinear() {
    // given
    BoardState board = BoardState.empty();
    MovePlacement move = new MovePlacement(Map.of(
        Coordinate.parse("H8"), PlacedTile.fromTile(Tile.of('A', 1)),
        Coordinate.parse("I9"), PlacedTile.fromTile(Tile.of('B', 3))));

    // when
    assertThatThrownBy(() -> MoveValidator.validatePlacement(board, move))
        .isInstanceOf(IllegalArgumentException.class);
    // then
  }

  @Test
  void moveMustBeContiguousUnlessExistingTilesFillGap() {
    // given
    BoardState board = BoardState.empty();
    MovePlacement gap = new MovePlacement(Map.of(
        Coordinate.parse("H8"), PlacedTile.fromTile(Tile.of('A', 1)),
        Coordinate.parse("H10"), PlacedTile.fromTile(Tile.of('B', 3))));

    // when
    assertThatThrownBy(() -> MoveValidator.validatePlacement(board, gap))
        .isInstanceOf(IllegalArgumentException.class);

    BoardState withExisting = board.withPlaced(Map.of(
        Coordinate.parse("H9"), PlacedTile.fromTile(Tile.of('C', 2))));
    assertThatCode(() -> MoveValidator.validatePlacement(withExisting, gap))
        .doesNotThrowAnyException();
    // then
  }

  @Test
  void subsequentMoveMustTouchExistingTiles() {
    // given
    BoardState board = BoardState.empty().withPlaced(Map.of(
        Coordinate.parse("H8"), PlacedTile.fromTile(Tile.of('A', 1))));

    MovePlacement isolated = new MovePlacement(Map.of(
        Coordinate.parse("A1"), PlacedTile.fromTile(Tile.of('B', 3)),
        Coordinate.parse("A2"), PlacedTile.fromTile(Tile.of('C', 2))));

    // when
    assertThatThrownBy(() -> MoveValidator.validatePlacement(board, isolated))
        .isInstanceOf(IllegalArgumentException.class);
    // then
  }
}
