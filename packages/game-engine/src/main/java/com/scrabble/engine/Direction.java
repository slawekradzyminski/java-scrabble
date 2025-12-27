package com.scrabble.engine;

public enum Direction {
  HORIZONTAL(0, 1),
  VERTICAL(1, 0);

  private final int rowDelta;
  private final int colDelta;

  Direction(int rowDelta, int colDelta) {
    this.rowDelta = rowDelta;
    this.colDelta = colDelta;
  }

  public int rowDelta() {
    return rowDelta;
  }

  public int colDelta() {
    return colDelta;
  }

  public Direction perpendicular() {
    return this == HORIZONTAL ? VERTICAL : HORIZONTAL;
  }
}
