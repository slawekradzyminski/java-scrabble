package com.scrabble.engine;

import java.util.Locale;

public record Coordinate(int rowIndex, int colIndex) {
  public static final int SIZE = 15;

  public Coordinate {
    if (rowIndex < 0 || rowIndex >= SIZE || colIndex < 0 || colIndex >= SIZE) {
      throw new IllegalArgumentException("Coordinate out of bounds: " + rowIndex + "," + colIndex);
    }
  }

  public static Coordinate parse(String value) {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException("Coordinate is blank");
    }

    String trimmed = value.trim().toUpperCase(Locale.ROOT);
    char rowChar = trimmed.charAt(0);
    if (rowChar < 'A' || rowChar > 'O') {
      throw new IllegalArgumentException("Row must be A-O: " + value);
    }

    String colPart = trimmed.substring(1);
    int col;
    try {
      col = Integer.parseInt(colPart);
    } catch (NumberFormatException e) {
      throw new IllegalArgumentException("Column must be 1-15: " + value, e);
    }

    if (col < 1 || col > SIZE) {
      throw new IllegalArgumentException("Column must be 1-15: " + value);
    }

    int rowIndex = rowChar - 'A';
    int colIndex = col - 1;
    return new Coordinate(rowIndex, colIndex);
  }

  public String format() {
    char row = (char) ('A' + rowIndex);
    return row + Integer.toString(colIndex + 1);
  }

  @Override
  public String toString() {
    return format();
  }
}
