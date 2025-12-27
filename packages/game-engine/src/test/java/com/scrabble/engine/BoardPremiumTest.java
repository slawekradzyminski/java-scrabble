package com.scrabble.engine;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class BoardPremiumTest {

  @Test
  void premiumLookupMatchesSpec() {
    Board board = Board.standard();

    assertEquals(Premium.TW, board.premiumAt(Coordinate.parse("A1")).orElseThrow());
    assertEquals(Premium.DW, board.premiumAt(Coordinate.parse("H8")).orElseThrow());
    assertEquals(Premium.TL, board.premiumAt(Coordinate.parse("F2")).orElseThrow());
    assertEquals(Premium.DL, board.premiumAt(Coordinate.parse("O12")).orElseThrow());

    assertFalse(board.premiumAt(Coordinate.parse("A2")).isPresent());
  }
}
