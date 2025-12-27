package com.scrabble.engine;

public record Tile(char letter, int points, boolean blank) {
  public Tile {
    if (blank && points != 0) {
      throw new IllegalArgumentException("Blank tiles must be 0 points");
    }
    if (!blank && letter == '\0') {
      throw new IllegalArgumentException("Letter tile must have a letter");
    }
  }

  public static Tile blankTile() {
    return new Tile('\0', 0, true);
  }

  public static Tile of(char letter, int points) {
    return new Tile(letter, points, false);
  }
}
