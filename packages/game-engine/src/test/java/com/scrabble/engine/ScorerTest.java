package com.scrabble.engine;

import java.util.Map;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ScorerTest {

  @Test
  void appliesWordPremiumOnCenter() {
    // given
    BoardState board = BoardState.empty();
    MovePlacement move = new MovePlacement(Map.of(
        Coordinate.parse("H8"), PlacedTile.fromTile(Tile.of('H', 3)),
        Coordinate.parse("H9"), PlacedTile.fromTile(Tile.of('I', 1))));

    // when
    ScoringResult result = Scorer.score(board, move, Board.standard());
    // then
    assertThat(result.totalScore()).isEqualTo(8);
  }

  @Test
  void appliesLetterPremium() {
    // given
    BoardState board = BoardState.empty();
    MovePlacement move = new MovePlacement(Map.of(
        Coordinate.parse("A4"), PlacedTile.fromTile(Tile.of('A', 1)),
        Coordinate.parse("A5"), PlacedTile.fromTile(Tile.of('B', 3))));

    // when
    ScoringResult result = Scorer.score(board, move, Board.standard());
    // then
    assertThat(result.totalScore()).isEqualTo(5);
  }

  @Test
  void appliesBingoBonus() {
    // given
    BoardState board = BoardState.empty();
    MovePlacement move = new MovePlacement(Map.of(
        Coordinate.parse("H8"), PlacedTile.fromTile(Tile.of('A', 1)),
        Coordinate.parse("H9"), PlacedTile.fromTile(Tile.of('B', 3)),
        Coordinate.parse("H10"), PlacedTile.fromTile(Tile.of('C', 2)),
        Coordinate.parse("H11"), PlacedTile.fromTile(Tile.of('D', 2)),
        Coordinate.parse("H12"), PlacedTile.fromTile(Tile.of('E', 1)),
        Coordinate.parse("H13"), PlacedTile.fromTile(Tile.of('F', 5)),
        Coordinate.parse("H14"), PlacedTile.fromTile(Tile.of('G', 3))));

    // when
    ScoringResult result = Scorer.score(board, move, Board.standard());
    // then
    assertThat(result.totalScore()).isEqualTo(86);
  }

  @Test
  void blanksScoreZeroButWordMultipliersApply() {
    // given
    BoardState board = BoardState.empty();
    MovePlacement move = new MovePlacement(Map.of(
        Coordinate.parse("H8"), new PlacedTile(Tile.blankTile(), 'A'),
        Coordinate.parse("H9"), PlacedTile.fromTile(Tile.of('B', 3))));

    // when
    ScoringResult result = Scorer.score(board, move, Board.standard());
    // then
    assertThat(result.totalScore()).isEqualTo(6);
  }
}
