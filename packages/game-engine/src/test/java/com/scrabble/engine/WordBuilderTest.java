package com.scrabble.engine;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class WordBuilderTest {

  @Test
  void buildsMainWordForFirstMove() {
    BoardState board = BoardState.empty();
    MovePlacement move = new MovePlacement(Map.of(
        Coordinate.parse("H8"), PlacedTile.fromTile(Tile.of('A', 1)),
        Coordinate.parse("H9"), PlacedTile.fromTile(Tile.of('B', 3))));

    List<Word> words = WordBuilder.buildWords(board, move);
    assertEquals(1, words.size());
    assertEquals("AB", words.get(0).text());
  }

  @Test
  void buildsCrossWords() {
    BoardState board = BoardState.empty().withPlaced(Map.of(
        Coordinate.parse("H8"), PlacedTile.fromTile(Tile.of('A', 1)),
        Coordinate.parse("I8"), PlacedTile.fromTile(Tile.of('B', 3))));

    MovePlacement move = new MovePlacement(Map.of(
        Coordinate.parse("H9"), PlacedTile.fromTile(Tile.of('C', 2)),
        Coordinate.parse("H10"), PlacedTile.fromTile(Tile.of('D', 2))));

    List<Word> words = WordBuilder.buildWords(board, move);
    assertEquals(1, words.size());
    assertEquals("ACD", words.get(0).text());
  }

  @Test
  void singleTileRequiresDirection() {
    BoardState board = BoardState.empty();
    MovePlacement move = new MovePlacement(Map.of(
        Coordinate.parse("H8"), PlacedTile.fromTile(Tile.of('A', 1))));

    assertThrows(IllegalArgumentException.class, () -> WordBuilder.buildWords(board, move));
  }
}
