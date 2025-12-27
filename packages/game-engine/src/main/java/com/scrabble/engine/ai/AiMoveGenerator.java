package com.scrabble.engine.ai;

import com.scrabble.engine.Board;
import com.scrabble.engine.BoardState;
import com.scrabble.engine.Coordinate;
import com.scrabble.engine.Direction;
import com.scrabble.engine.LetterTile;
import com.scrabble.engine.MovePlacement;
import com.scrabble.engine.MoveValidator;
import com.scrabble.engine.PlacedTile;
import com.scrabble.engine.Player;
import com.scrabble.engine.ScoringResult;
import com.scrabble.engine.Scorer;
import com.scrabble.engine.Tile;
import com.scrabble.engine.Word;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public final class AiMoveGenerator {
  public static final int DEFAULT_MAX_CANDIDATES = 1500;
  private static final char[] LETTER_POOL = buildLetterPool();

  public Optional<AiMove> bestMove(
      BoardState board,
      Player player,
      Board premiums,
      WordDictionary dictionary) {
    return bestMove(board, player, premiums, dictionary, DEFAULT_MAX_CANDIDATES);
  }

  public Optional<AiMove> bestMove(
      BoardState board,
      Player player,
      Board premiums,
      WordDictionary dictionary,
      int maxCandidates) {
    if (player.rack().tiles().isEmpty()) {
      return Optional.empty();
    }

    List<Tile> rack = new ArrayList<>(player.rack().tiles());
    Set<String> seen = new HashSet<>();
    BestMove best = new BestMove();
    CandidateCounter counter = new CandidateCounter(maxCandidates);
    boolean[][] anchors = buildAnchors(board);

    for (Direction direction : EnumSet.of(Direction.HORIZONTAL, Direction.VERTICAL)) {
      for (int line = 0; line < Coordinate.SIZE && !counter.exhausted(); line++) {
        evaluateLine(board, rack, premiums, dictionary, direction, line, best, seen, counter, anchors);
      }
    }

    return Optional.ofNullable(best.result);
  }

  private void evaluateLine(
      BoardState board,
      List<Tile> rack,
      Board premiums,
      WordDictionary dictionary,
      Direction direction,
      int line,
      BestMove best,
      Set<String> seen,
      CandidateCounter counter,
      boolean[][] anchors) {
    PlacedTile[] lineTiles = new PlacedTile[Coordinate.SIZE];
    boolean[][] crossChecks = new boolean[Coordinate.SIZE][];
    for (int index = 0; index < Coordinate.SIZE; index++) {
      Coordinate coord = coordinateFor(direction, line, index);
      lineTiles[index] = board.tileAt(coord).orElse(null);
      if (lineTiles[index] == null) {
        crossChecks[index] = allowedLettersFor(board, coord, direction, dictionary);
      }
    }

    for (int start = 0; start < Coordinate.SIZE && !counter.exhausted(); start++) {
      for (int end = start; end < Coordinate.SIZE && !counter.exhausted(); end++) {
        int length = end - start + 1;
        if (length < 1) {
          continue;
        }
        int empty = 0;
        for (int i = start; i <= end; i++) {
          if (lineTiles[i] == null) {
            empty++;
          }
        }
        if (empty == 0 || empty > rack.size()) {
          continue;
        }

        Map<Coordinate, PlacedTile> placements = new HashMap<>();
        boolean[] used = new boolean[rack.size()];
        StringBuilder word = new StringBuilder();
        fillWindow(board, rack, premiums, dictionary, direction, line, lineTiles,
            start, end, start, used, placements, word, best, seen, counter, anchors, crossChecks, false);
      }
    }
  }

  private void fillWindow(
      BoardState board,
      List<Tile> rack,
      Board premiums,
      WordDictionary dictionary,
      Direction direction,
      int line,
      PlacedTile[] lineTiles,
      int start,
      int end,
      int position,
      boolean[] used,
      Map<Coordinate, PlacedTile> placements,
      StringBuilder word,
      BestMove best,
      Set<String> seen,
      CandidateCounter counter,
      boolean[][] anchors,
      boolean[][] crossChecks,
      boolean hasAnchor) {
    if (counter.exhausted()) {
      return;
    }
    if (position > end) {
      evaluateCandidate(board, premiums, dictionary, placements, best, seen, counter, anchors, hasAnchor);
      return;
    }

    PlacedTile existing = lineTiles[position];
    if (existing != null) {
      word.append(existing.assignedLetter());
      if (!dictionary.containsPrefix(word.toString())) {
        word.setLength(word.length() - 1);
        return;
      }
      fillWindow(board, rack, premiums, dictionary, direction, line, lineTiles, start, end,
          position + 1, used, placements, word, best, seen, counter, anchors, crossChecks, hasAnchor);
      word.setLength(word.length() - 1);
      return;
    }

    Coordinate coord = coordinateFor(direction, line, position);
    boolean[] allowed = crossChecks[position];
    for (int i = 0; i < rack.size() && !counter.exhausted(); i++) {
      if (used[i]) {
        continue;
      }
      Tile tile = rack.get(i);
      used[i] = true;
      if (tile.blank()) {
        for (char letter : LETTER_POOL) {
          if (allowed != null && !allowed[letter]) {
            continue;
          }
          placements.put(coord, new PlacedTile(tile, letter));
          word.append(letter);
          if (!dictionary.containsPrefix(word.toString())) {
            word.setLength(word.length() - 1);
            placements.remove(coord);
            continue;
          }
          fillWindow(board, rack, premiums, dictionary, direction, line, lineTiles, start, end,
              position + 1, used, placements, word, best, seen, counter, anchors, crossChecks,
              hasAnchor || anchors[coord.rowIndex()][coord.colIndex()]);
          word.setLength(word.length() - 1);
          placements.remove(coord);
          if (counter.exhausted()) {
            break;
          }
        }
      } else {
        char letter = tile.letter();
        if (allowed != null && !allowed[letter]) {
          used[i] = false;
          continue;
        }
        placements.put(coord, new PlacedTile(tile, letter));
        word.append(letter);
        if (!dictionary.containsPrefix(word.toString())) {
          word.setLength(word.length() - 1);
          placements.remove(coord);
          used[i] = false;
          continue;
        }
        fillWindow(board, rack, premiums, dictionary, direction, line, lineTiles, start, end,
            position + 1, used, placements, word, best, seen, counter, anchors, crossChecks,
            hasAnchor || anchors[coord.rowIndex()][coord.colIndex()]);
        word.setLength(word.length() - 1);
        placements.remove(coord);
      }
      used[i] = false;
    }
  }

  private void evaluateCandidate(
      BoardState board,
      Board premiums,
      WordDictionary dictionary,
      Map<Coordinate, PlacedTile> placements,
      BestMove best,
      Set<String> seen,
      CandidateCounter counter,
      boolean[][] anchors,
      boolean hasAnchor) {
    if (placements.isEmpty()) {
      return;
    }
    if (!hasAnchor && !placementsTouchAnchor(placements, anchors)) {
      return;
    }
    String key = buildKey(placements);
    if (!seen.add(key)) {
      return;
    }

    try {
      MovePlacement move = new MovePlacement(Map.copyOf(placements));
      MoveValidator.validatePlacement(board, move);
      ScoringResult scoring = Scorer.score(board, move, premiums);
      if (!allWordsValid(dictionary, scoring.words())) {
        return;
      }
      counter.increment();
      best.update(move, scoring);
    } catch (RuntimeException ignored) {
      // ignore illegal candidates
    }
  }

  private static boolean allWordsValid(WordDictionary dictionary, List<Word> words) {
    for (Word word : words) {
      if (!dictionary.contains(word.text())) {
        return false;
      }
    }
    return true;
  }

  private static boolean[][] buildAnchors(BoardState board) {
    boolean[][] anchors = new boolean[Coordinate.SIZE][Coordinate.SIZE];
    if (board.isEmpty()) {
      int center = Coordinate.SIZE / 2;
      anchors[center][center] = true;
      return anchors;
    }
    for (Coordinate coord : board.tiles().keySet()) {
      markAnchor(board, anchors, coord.rowIndex() - 1, coord.colIndex());
      markAnchor(board, anchors, coord.rowIndex() + 1, coord.colIndex());
      markAnchor(board, anchors, coord.rowIndex(), coord.colIndex() - 1);
      markAnchor(board, anchors, coord.rowIndex(), coord.colIndex() + 1);
    }
    return anchors;
  }

  private static void markAnchor(BoardState board, boolean[][] anchors, int row, int col) {
    if (row < 0 || row >= Coordinate.SIZE || col < 0 || col >= Coordinate.SIZE) {
      return;
    }
    Coordinate coord = new Coordinate(row, col);
    if (!board.hasTile(coord)) {
      anchors[row][col] = true;
    }
  }

  private static boolean placementsTouchAnchor(Map<Coordinate, PlacedTile> placements, boolean[][] anchors) {
    for (Coordinate coordinate : placements.keySet()) {
      if (anchors[coordinate.rowIndex()][coordinate.colIndex()]) {
        return true;
      }
    }
    return false;
  }

  private static boolean[] allowedLettersFor(
      BoardState board,
      Coordinate coord,
      Direction direction,
      WordDictionary dictionary) {
    Direction cross = direction == Direction.HORIZONTAL ? Direction.VERTICAL : Direction.HORIZONTAL;
    String prefix = readCross(board, coord, cross, -1);
    String suffix = readCross(board, coord, cross, 1);
    if (prefix.isEmpty() && suffix.isEmpty()) {
      return null;
    }
    boolean[] allowed = new boolean[Character.MAX_VALUE + 1];
    for (char letter : LETTER_POOL) {
      String word = prefix + letter + suffix;
      if (dictionary.contains(word)) {
        allowed[letter] = true;
      }
    }
    return allowed;
  }

  private static String readCross(BoardState board, Coordinate origin, Direction direction, int step) {
    StringBuilder builder = new StringBuilder();
    int row = origin.rowIndex();
    int col = origin.colIndex();
    while (true) {
      row += direction == Direction.VERTICAL ? step : 0;
      col += direction == Direction.HORIZONTAL ? step : 0;
      if (row < 0 || row >= Coordinate.SIZE || col < 0 || col >= Coordinate.SIZE) {
        break;
      }
      Coordinate coord = new Coordinate(row, col);
      Optional<PlacedTile> tile = board.tileAt(coord);
      if (tile.isEmpty()) {
        break;
      }
      if (step < 0) {
        builder.insert(0, tile.get().assignedLetter());
      } else {
        builder.append(tile.get().assignedLetter());
      }
    }
    return builder.toString();
  }

  private static Coordinate coordinateFor(Direction direction, int line, int index) {
    if (direction == Direction.HORIZONTAL) {
      return new Coordinate(line, index);
    }
    return new Coordinate(index, line);
  }

  private static char[] buildLetterPool() {
    List<Character> letters = new ArrayList<>();
    for (LetterTile tile : LetterTile.values()) {
      if (!tile.isBlank()) {
        letters.add(tile.letter());
      }
    }
    char[] pool = new char[letters.size()];
    for (int i = 0; i < letters.size(); i++) {
      pool[i] = letters.get(i);
    }
    return pool;
  }

  private static String buildKey(Map<Coordinate, PlacedTile> placements) {
    return placements.entrySet().stream()
        .sorted(Map.Entry.comparingByKey((a, b) -> {
          int row = Integer.compare(a.rowIndex(), b.rowIndex());
          if (row != 0) {
            return row;
          }
          return Integer.compare(a.colIndex(), b.colIndex());
        }))
        .map(entry -> entry.getKey().format() + entry.getValue().assignedLetter())
        .reduce("", (left, right) -> left + "|" + right);
  }

  private static final class CandidateCounter {
    private final int max;
    private int count;

    CandidateCounter(int max) {
      this.max = max;
    }

    void increment() {
      count++;
    }

    boolean exhausted() {
      return count >= max;
    }
  }

  private static final class BestMove {
    private AiMove result;
    private int score;
    private int tilesUsed;

    void update(MovePlacement move, ScoringResult scoring) {
      int nextScore = scoring.totalScore();
      int nextTiles = move.size();
      if (result == null || nextScore > score || (nextScore == score && nextTiles > tilesUsed)) {
        result = new AiMove(move, scoring);
        score = nextScore;
        tilesUsed = nextTiles;
      }
    }
  }
}
