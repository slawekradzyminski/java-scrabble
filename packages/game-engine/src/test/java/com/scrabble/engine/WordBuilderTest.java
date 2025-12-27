package com.scrabble.engine;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class WordBuilderTest {

  @Test
  void buildsMainWordForFirstMove() {
    // given
    BoardState board = BoardState.empty();
    MovePlacement move = new MovePlacement(Map.of(
        Coordinate.parse("H8"), PlacedTile.fromTile(Tile.of('A', 1)),
        Coordinate.parse("H9"), PlacedTile.fromTile(Tile.of('B', 3))));

    // when
    List<Word> words = WordBuilder.buildWords(board, move);
    // then
    assertThat(words).hasSize(1);
    assertThat(words.get(0).text()).isEqualTo("AB");
  }

  @Test
  void buildsCrossWords() {
    // given
    BoardState board = BoardState.empty().withPlaced(Map.of(
        Coordinate.parse("H8"), PlacedTile.fromTile(Tile.of('A', 1)),
        Coordinate.parse("I8"), PlacedTile.fromTile(Tile.of('B', 3))));

    MovePlacement move = new MovePlacement(Map.of(
        Coordinate.parse("H9"), PlacedTile.fromTile(Tile.of('C', 2)),
        Coordinate.parse("H10"), PlacedTile.fromTile(Tile.of('D', 2))));

    // when
    List<Word> words = WordBuilder.buildWords(board, move);
    // then
    assertThat(words).hasSize(1);
    assertThat(words.get(0).text()).isEqualTo("ACD");
  }

  @Test
  void singleTileRequiresDirection() {
    // given
    BoardState board = BoardState.empty();
    MovePlacement move = new MovePlacement(Map.of(
        Coordinate.parse("H8"), PlacedTile.fromTile(Tile.of('A', 1))));

    // when
    assertThatThrownBy(() -> WordBuilder.buildWords(board, move))
        .isInstanceOf(IllegalArgumentException.class);
    // then
  }
}
