package com.scrabble.engine;

import java.util.List;
import java.util.Map;

public final class Scorer {
  private Scorer() { }

  public static ScoringResult score(BoardState board, MovePlacement move, Board premiums) {
    BoardState next = board.withPlaced(move.placements());
    List<Word> words = WordBuilder.buildWords(board, move);
    int total = 0;

    for (Word word : words) {
      int wordScore = scoreWord(word, next, move.placements(), premiums);
      total += wordScore;
    }

    if (move.size() == Rack.CAPACITY) {
      total += 50;
    }

    return new ScoringResult(total, words);
  }

  private static int scoreWord(
      Word word,
      BoardState board,
      Map<Coordinate, PlacedTile> newlyPlaced,
      Board premiums) {
    int wordMultiplier = 1;
    int sum = 0;

    for (Coordinate coordinate : word.coordinates()) {
      PlacedTile placed = board.tileAt(coordinate).orElseThrow();
      int letterScore = placed.tile().points();
      if (newlyPlaced.containsKey(coordinate)) {
        Premium premium = premiums.premiumAt(coordinate).orElse(null);
        if (premium == Premium.DL) {
          letterScore *= 2;
        } else if (premium == Premium.TL) {
          letterScore *= 3;
        } else if (premium == Premium.DW) {
          wordMultiplier *= 2;
        } else if (premium == Premium.TW) {
          wordMultiplier *= 3;
        }
      }
      sum += letterScore;
    }

    return sum * wordMultiplier;
  }
}
