package com.scrabble.engine;

import java.util.Objects;

public enum LetterTile {
  A('A', 1, 9),
  Ą('Ą', 5, 1),
  B('B', 3, 2),
  C('C', 2, 3),
  Ć('Ć', 6, 1),
  D('D', 2, 3),
  E('E', 1, 7),
  Ę('Ę', 5, 1),
  F('F', 5, 1),
  G('G', 3, 2),
  H('H', 3, 2),
  I('I', 1, 8),
  J('J', 3, 2),
  K('K', 2, 3),
  L('L', 2, 3),
  Ł('Ł', 3, 2),
  M('M', 2, 3),
  N('N', 1, 5),
  Ń('Ń', 7, 1),
  O('O', 1, 6),
  Ó('Ó', 5, 1),
  P('P', 2, 3),
  R('R', 1, 4),
  S('S', 1, 4),
  Ś('Ś', 5, 1),
  T('T', 2, 3),
  U('U', 3, 2),
  W('W', 1, 4),
  Y('Y', 2, 4),
  Z('Z', 1, 5),
  Ź('Ź', 9, 1),
  Ż('Ż', 5, 1),
  BLANK('\0', 0, 2, true);

  private final char letter;
  private final int points;
  private final int count;
  private final boolean blank;

  LetterTile(char letter, int points, int count) {
    this(letter, points, count, false);
  }

  LetterTile(char letter, int points, int count, boolean blank) {
    this.letter = letter;
    this.points = points;
    this.count = count;
    this.blank = blank;
  }

  public char letter() {
    return letter;
  }

  public int points() {
    return points;
  }

  public int count() {
    return count;
  }

  public boolean isBlank() {
    return blank;
  }

  public Tile toTile() {
    if (blank) {
      return Tile.blankTile();
    }
    return Tile.of(letter, points);
  }

  public static LetterTile fromLetter(char letter) {
    for (LetterTile tile : values()) {
      if (tile.letter == letter && !tile.blank) {
        return tile;
      }
    }
    throw new IllegalArgumentException("Unknown letter: " + letter);
  }

  public static int totalTiles() {
    int sum = 0;
    for (LetterTile tile : values()) {
      sum += tile.count;
    }
    return sum;
  }

  @Override
  public String toString() {
    return blank ? "BLANK" : Objects.toString(letter);
  }
}
