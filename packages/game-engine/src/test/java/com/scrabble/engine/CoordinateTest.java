package com.scrabble.engine;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CoordinateTest {

  @Test
  void parseAndFormatRoundTrip() {
    // given
    Coordinate coordinate = Coordinate.parse("H8");
    // when
    assertThat(coordinate.rowIndex()).isEqualTo(7);
    assertThat(coordinate.colIndex()).isEqualTo(7);
    // then
    assertThat(coordinate.format()).isEqualTo("H8");
  }

  @Test
  void parseLowercase() {
    // given
    Coordinate coordinate = Coordinate.parse("b14");
    // when
    assertThat(coordinate.format()).isEqualTo("B14");
    // then
  }

  @Test
  void rejectsOutOfRange() {
    // given
    // when
    assertThatThrownBy(() -> Coordinate.parse("P1"))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> Coordinate.parse("A16"))
        .isInstanceOf(IllegalArgumentException.class);
    // then
  }

  @Test
  void rejectsBlankOrMalformed() {
    // given
    // when
    assertThatThrownBy(() -> Coordinate.parse(" "))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> Coordinate.parse("A0"))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> Coordinate.parse("A-1"))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> Coordinate.parse("AA"))
        .isInstanceOf(IllegalArgumentException.class);
    // then
  }
}
