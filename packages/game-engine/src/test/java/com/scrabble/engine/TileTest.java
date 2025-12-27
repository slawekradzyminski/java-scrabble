package com.scrabble.engine;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TileTest {

  @Test
  void createsBlankTile() {
    // given
    Tile tile = Tile.blankTile();
    // when
    assertThat(tile.points()).isZero();
    assertThat(tile.letter()).isEqualTo('\0');
    // then
    assertThat(tile.blank()).isTrue();
  }

  @Test
  void rejectsBlankWithPoints() {
    // given
    // when
    assertThatThrownBy(() -> new Tile(' ', 1, true))
        .isInstanceOf(IllegalArgumentException.class);
    // then
  }

  @Test
  void rejectsMissingLetterForNonBlank() {
    // given
    // when
    assertThatThrownBy(() -> new Tile('\0', 1, false))
        .isInstanceOf(IllegalArgumentException.class);
    // then
  }
}
