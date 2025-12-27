package com.scrabble.engine;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class BoardPremiumTest {

  @Test
  void premiumLookupMatchesSpec() {
    // given
    Board board = Board.standard();

    // when
    assertThat(board.premiumAt(Coordinate.parse("A1"))).contains(Premium.TW);
    assertThat(board.premiumAt(Coordinate.parse("H8"))).contains(Premium.DW);
    assertThat(board.premiumAt(Coordinate.parse("F2"))).contains(Premium.TL);
    assertThat(board.premiumAt(Coordinate.parse("O12"))).contains(Premium.DL);

    // then
    assertThat(board.premiumAt(Coordinate.parse("A2"))).isEmpty();
  }
}
