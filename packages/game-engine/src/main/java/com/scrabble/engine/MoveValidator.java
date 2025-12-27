package com.scrabble.engine;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class MoveValidator {
  private MoveValidator() { }

  public static void validatePlacement(BoardState board, MovePlacement move) {
    ensureSquaresEmpty(board, move);

    List<Coordinate> coords = new ArrayList<>(move.placements().keySet());
    boolean sameRow = coords.stream().map(Coordinate::rowIndex).distinct().count() == 1;
    boolean sameCol = coords.stream().map(Coordinate::colIndex).distinct().count() == 1;

    if (!sameRow && !sameCol) {
      throw new IllegalArgumentException("Move must be in a single row or column");
    }

    if (board.isEmpty()) {
      validateFirstMove(move);
    } else {
      if (!touchesExisting(board, coords)) {
        throw new IllegalArgumentException("Move must connect to existing tiles");
      }
    }

    ensureContiguous(board, coords, sameRow);
  }

  private static void ensureSquaresEmpty(BoardState board, MovePlacement move) {
    for (Coordinate coordinate : move.placements().keySet()) {
      if (board.hasTile(coordinate)) {
        throw new IllegalArgumentException("Square already occupied: " + coordinate);
      }
    }
  }

  private static void validateFirstMove(MovePlacement move) {
    if (move.size() < 2) {
      throw new IllegalArgumentException("First move must place at least 2 tiles");
    }
    Coordinate center = Coordinate.parse("H8");
    if (!move.placements().containsKey(center)) {
      throw new IllegalArgumentException("First move must cover H8");
    }
  }

  private static boolean touchesExisting(BoardState board, List<Coordinate> coords) {
    for (Coordinate coordinate : coords) {
      if (hasNeighbor(board, coordinate)) {
        return true;
      }
    }
    return false;
  }

  private static boolean hasNeighbor(BoardState board, Coordinate coordinate) {
    int row = coordinate.rowIndex();
    int col = coordinate.colIndex();
    return hasTileAt(board, row - 1, col)
        || hasTileAt(board, row + 1, col)
        || hasTileAt(board, row, col - 1)
        || hasTileAt(board, row, col + 1);
  }

  private static boolean hasTileAt(BoardState board, int row, int col) {
    if (row < 0 || row >= Coordinate.SIZE || col < 0 || col >= Coordinate.SIZE) {
      return false;
    }
    return board.hasTile(new Coordinate(row, col));
  }

  private static void ensureContiguous(BoardState board, List<Coordinate> coords, boolean sameRow) {
    if (sameRow) {
      int row = coords.get(0).rowIndex();
      int min = coords.stream().map(Coordinate::colIndex).min(Comparator.naturalOrder()).orElseThrow();
      int max = coords.stream().map(Coordinate::colIndex).max(Comparator.naturalOrder()).orElseThrow();
      for (int col = min; col <= max; col++) {
        Coordinate coordinate = new Coordinate(row, col);
        if (!board.hasTile(coordinate) && !coords.contains(coordinate)) {
          throw new IllegalArgumentException("Move must be contiguous");
        }
      }
    } else {
      int col = coords.get(0).colIndex();
      int min = coords.stream().map(Coordinate::rowIndex).min(Comparator.naturalOrder()).orElseThrow();
      int max = coords.stream().map(Coordinate::rowIndex).max(Comparator.naturalOrder()).orElseThrow();
      for (int row = min; row <= max; row++) {
        Coordinate coordinate = new Coordinate(row, col);
        if (!board.hasTile(coordinate) && !coords.contains(coordinate)) {
          throw new IllegalArgumentException("Move must be contiguous");
        }
      }
    }
  }
}
