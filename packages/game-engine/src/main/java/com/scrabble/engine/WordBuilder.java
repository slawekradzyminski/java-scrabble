package com.scrabble.engine;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class WordBuilder {
  private WordBuilder() { }

  public static List<Word> buildWords(BoardState board, MovePlacement move) {
    BoardState next = board.withPlaced(move.placements());
    Set<Coordinate> newCoords = move.placements().keySet();

    Direction direction = determineDirection(next, move);
    List<Word> words = new ArrayList<>();

    Coordinate anchor = newCoords.iterator().next();
    Word main = buildWord(next, anchor, direction);
    if (main.length() < 2) {
      throw new IllegalArgumentException("Main word must have length >= 2");
    }
    words.add(main);

    Direction crossDirection = direction.perpendicular();
    for (Coordinate coordinate : newCoords) {
      Word cross = buildWord(next, coordinate, crossDirection);
      if (cross.length() >= 2) {
        words.add(cross);
      }
    }

    return distinctWords(words);
  }

  private static Direction determineDirection(BoardState board, MovePlacement move) {
    Set<Coordinate> coords = move.placements().keySet();
    boolean sameRow = coords.stream().map(Coordinate::rowIndex).distinct().count() == 1;
    boolean sameCol = coords.stream().map(Coordinate::colIndex).distinct().count() == 1;

    if (sameRow && !sameCol) {
      return Direction.HORIZONTAL;
    }
    if (sameCol && !sameRow) {
      return Direction.VERTICAL;
    }
    if (coords.size() == 1) {
      Coordinate only = coords.iterator().next();
      if (hasNeighbor(board, only, Direction.HORIZONTAL)) {
        return Direction.HORIZONTAL;
      }
      if (hasNeighbor(board, only, Direction.VERTICAL)) {
        return Direction.VERTICAL;
      }
    }
    throw new IllegalArgumentException("Unable to determine word direction");
  }

  private static boolean hasNeighbor(BoardState board, Coordinate coordinate, Direction direction) {
    int row = coordinate.rowIndex();
    int col = coordinate.colIndex();
    return hasTileAt(board, row - direction.rowDelta(), col - direction.colDelta())
        || hasTileAt(board, row + direction.rowDelta(), col + direction.colDelta());
  }

  private static boolean hasTileAt(BoardState board, int row, int col) {
    if (row < 0 || row >= Coordinate.SIZE || col < 0 || col >= Coordinate.SIZE) {
      return false;
    }
    return board.hasTile(new Coordinate(row, col));
  }

  private static Word buildWord(BoardState board, Coordinate start, Direction direction) {
    int row = start.rowIndex();
    int col = start.colIndex();

    while (hasTileAt(board, row - direction.rowDelta(), col - direction.colDelta())) {
      row -= direction.rowDelta();
      col -= direction.colDelta();
    }

    List<Coordinate> coordinates = new ArrayList<>();
    StringBuilder text = new StringBuilder();
    int currentRow = row;
    int currentCol = col;
    while (hasTileAt(board, currentRow, currentCol)) {
      Coordinate coordinate = new Coordinate(currentRow, currentCol);
      PlacedTile placedTile = board.tileAt(coordinate).orElseThrow();
      text.append(placedTile.assignedLetter());
      coordinates.add(coordinate);
      currentRow += direction.rowDelta();
      currentCol += direction.colDelta();
    }

    return new Word(text.toString(), coordinates);
  }

  private static List<Word> distinctWords(List<Word> words) {
    Set<String> seen = new HashSet<>();
    List<Word> result = new ArrayList<>();
    for (Word word : words) {
      String key = word.text() + word.coordinates();
      if (seen.add(key)) {
        result.add(word);
      }
    }
    return result;
  }
}
