package com.scrabble.engine;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TileTest {

  @Test
  void createsBlankTile() {
    Tile tile = Tile.blankTile();
    assertEquals(0, tile.points());
    assertEquals('\0', tile.letter());
    assertEquals(true, tile.blank());
  }

  @Test
  void rejectsBlankWithPoints() {
    assertThrows(IllegalArgumentException.class, () -> new Tile(' ', 1, true));
  }

  @Test
  void rejectsMissingLetterForNonBlank() {
    assertThrows(IllegalArgumentException.class, () -> new Tile('\0', 1, false));
  }
}
