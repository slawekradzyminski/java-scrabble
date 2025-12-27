package com.scrabble.engine;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CoordinateTest {

  @Test
  void parseAndFormatRoundTrip() {
    Coordinate coordinate = Coordinate.parse("H8");
    assertEquals(7, coordinate.rowIndex());
    assertEquals(7, coordinate.colIndex());
    assertEquals("H8", coordinate.format());
  }

  @Test
  void parseLowercase() {
    Coordinate coordinate = Coordinate.parse("b14");
    assertEquals("B14", coordinate.format());
  }

  @Test
  void rejectsOutOfRange() {
    assertThrows(IllegalArgumentException.class, () -> Coordinate.parse("P1"));
    assertThrows(IllegalArgumentException.class, () -> Coordinate.parse("A16"));
  }

  @Test
  void rejectsBlankOrMalformed() {
    assertThrows(IllegalArgumentException.class, () -> Coordinate.parse(" "));
    assertThrows(IllegalArgumentException.class, () -> Coordinate.parse("A0"));
    assertThrows(IllegalArgumentException.class, () -> Coordinate.parse("A-1"));
    assertThrows(IllegalArgumentException.class, () -> Coordinate.parse("AA"));
  }
}
