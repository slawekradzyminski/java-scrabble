package com.scrabble.engine.ai;

import static org.assertj.core.api.Assertions.assertThat;

import com.scrabble.dictionary.FstDictionary;
import com.scrabble.engine.Board;
import com.scrabble.engine.BoardState;
import com.scrabble.engine.EndgameScoring;
import com.scrabble.engine.GameState;
import com.scrabble.engine.PlacedTile;
import com.scrabble.engine.Player;
import com.scrabble.engine.Rack;
import com.scrabble.engine.Tile;
import com.scrabble.engine.TileBag;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

class AiSelfPlaySimulationTest {

  @Test
  @EnabledIfSystemProperty(named = "ai.simulation", matches = "true")
  void simulatesFullGameUsingAllTiles() throws Exception {
    // given
    Path repoRoot = findRepoRoot();
    Path fstPath = repoRoot.resolve(Path.of("artifacts", "osps.fst"));
    Path metaPath = repoRoot.resolve(Path.of("artifacts", "osps.fst.meta.json"));
    if (!Files.exists(fstPath) || !Files.exists(metaPath)) {
      throw new IllegalStateException("Dictionary artifacts missing: " + fstPath + " / " + metaPath);
    }
    Path reportPath = buildReportPath(repoRoot);
    Random random = new Random(42);
    TileBag bag = TileBag.standard(random);
    Player botA = new Player("Bot A");
    Player botB = new Player("Bot B");
    botA.rack().addAll(bag.draw(Rack.CAPACITY));
    botB.rack().addAll(bag.draw(Rack.CAPACITY));
    GameState state = new GameState(BoardState.empty(), List.of(botA, botB), bag);
    AiMoveGenerator generator = new AiMoveGenerator();
    FstDictionary fstDictionary = FstDictionary.load(fstPath, metaPath);
    WordDictionary dictionary = new WordDictionary() {
      @Override
      public boolean contains(String word) {
        return fstDictionary.contains(word);
      }

      @Override
      public boolean containsPrefix(String prefix) {
        return fstDictionary.containsPrefix(prefix);
      }
    };

    int totalTiles = bag.size() + botA.rack().size() + botB.rack().size();
    int maxTurns = Integer.parseInt(System.getProperty("ai.simulation.maxTurns", "400"));
    int maxCandidates = Integer.parseInt(System.getProperty("ai.simulation.maxCandidates", "900"));

    // when
    int turns = 0;
    int passes = 0;
    try (BufferedWriter writer = Files.newBufferedWriter(reportPath)) {
      logLine(writer, "AI self-play simulation");
      logLine(writer, "Dictionary: " + fstPath.toAbsolutePath());
      logLine(writer, "Total tiles: " + totalTiles);
      logLine(writer, "Max candidates: " + maxCandidates);
      while (turns < maxTurns && boardTileCount(state) < totalTiles) {
        Player current = state.players().get(state.currentPlayerIndex());
        long startedAt = System.nanoTime();
        Optional<AiMove> move =
            generator.bestMove(state.board(), current, Board.standard(), dictionary, maxCandidates);
        long computeMs = (System.nanoTime() - startedAt) / 1_000_000;
        if (move.isPresent()) {
          applyMove(writer, state, current, move.get(), random, turns, totalTiles, computeMs);
          passes = 0;
        } else {
          if (!state.bag().isEmpty()) {
            exchangeOneTile(writer, state, current, random, turns, totalTiles);
          } else {
            state.advanceTurn();
            passes++;
            logPass(writer, current, state, turns, totalTiles, passes);
          }
        }
        assertStateInvariants(state, totalTiles);
        turns++;
        if (passes >= 4) {
          break;
        }
      }

      EndgameScoring.applyFinalAdjustments(state.players(), null);
      logSummary(writer, state, turns, passes, totalTiles);
    }

    // then
    assertThat(boardTileCount(state) == totalTiles || passes >= 4).isTrue();
    if (boardTileCount(state) == totalTiles) {
      assertThat(state.bag().isEmpty()).isTrue();
      assertThat(state.players()).allSatisfy(player -> assertThat(player.rack().size()).isZero());
    }
  }

  private int boardTileCount(GameState state) {
    return state.board().tiles().size();
  }

  private void assertStateInvariants(GameState state, int totalTiles) {
    int rackTiles = state.players().stream().mapToInt(player -> player.rack().size()).sum();
    int counted = state.bag().size() + boardTileCount(state) + rackTiles;
    assertThat(counted).isEqualTo(totalTiles);
    state.players().forEach(player -> assertThat(player.rack().size()).isBetween(0, Rack.CAPACITY));
  }

  private void applyMove(
      BufferedWriter writer,
      GameState state,
      Player player,
      AiMove move,
      Random random,
      int turn,
      int totalTiles,
      long computeMs) throws IOException {
    logMove(writer, player, state, move, turn, totalTiles, computeMs);
    List<Tile> removed = new ArrayList<>();
    for (PlacedTile placed : move.placement().placements().values()) {
      removed.add(removeTileFromRack(player.rack(), placed.tile()));
    }
    state.applyPendingMove(move.placement(), move.scoringResult());
    state.resolveChallenge(true);
    refillRack(player.rack(), state.bag());
  }

  private void exchangeOneTile(
      BufferedWriter writer,
      GameState state,
      Player player,
      Random random,
      int turn,
      int totalTiles) throws IOException {
    if (player.rack().tiles().isEmpty()) {
      state.advanceTurn();
      return;
    }
    Tile tile = player.rack().tiles().get(0);
    player.rack().remove(tile);
    state.bag().addAll(List.of(tile), random);
    player.rack().addAll(state.bag().draw(1));
    state.advanceTurn();
    logExchange(writer, player, tile, state, turn, totalTiles);
  }

  private void refillRack(Rack rack, TileBag bag) {
    int missing = rack.remainingCapacity();
    if (missing > 0) {
      rack.addAll(bag.draw(missing));
    }
  }

  private Tile removeTileFromRack(Rack rack, Tile needed) {
    for (Tile tile : rack.tiles()) {
      if (tile.blank() == needed.blank()
          && tile.letter() == needed.letter()
          && tile.points() == needed.points()) {
        rack.remove(tile);
        return tile;
      }
    }
    throw new IllegalStateException("Tile missing from rack");
  }

  private void logMove(
      BufferedWriter writer,
      Player player,
      GameState state,
      AiMove move,
      int turn,
      int totalTiles,
      long computeMs) throws IOException {
    String rack = formatRack(player.rack().tiles());
    String placements = move.placement().placements().entrySet().stream()
        .map(entry -> entry.getKey().format() + "=" + entry.getValue().assignedLetter())
        .sorted()
        .reduce((left, right) -> left + ", " + right)
        .orElse("-");
    String words = move.scoringResult().words().stream()
        .map(word -> word.text())
        .reduce((left, right) -> left + ", " + right)
        .orElse("-");
    logLine(
        writer,
        String.format(
            "Turn %d | %s | Rack [%s] | Move [%s] | Words [%s] | Points %d | Score %d | Board %d/%d | Bag %d",
            turn,
            player.name(),
            rack,
            placements,
            words,
            move.scoringResult().totalScore(),
            player.score(),
            boardTileCount(state),
            totalTiles,
            state.bag().size()
        )
    );
    logLine(writer, "Turn " + turn + " | " + player.name() + " | Compute ms " + computeMs);
  }

  private void logExchange(BufferedWriter writer, Player player, Tile tile, GameState state, int turn, int totalTiles)
      throws IOException {
    logLine(
        writer,
        String.format(
            "Turn %d | %s | Exchange [%s] | Score %d | Board %d/%d | Bag %d",
            turn,
            player.name(),
            formatTile(tile),
            player.score(),
            boardTileCount(state),
            totalTiles,
            state.bag().size()
        )
    );
  }

  private void logPass(BufferedWriter writer, Player player, GameState state, int turn, int totalTiles, int passes)
      throws IOException {
    logLine(
        writer,
        String.format(
            "Turn %d | %s | PASS (%d) | Score %d | Board %d/%d | Bag %d",
            turn,
            player.name(),
            passes,
            player.score(),
            boardTileCount(state),
            totalTiles,
            state.bag().size()
        )
    );
  }

  private void logSummary(BufferedWriter writer, GameState state, int turns, int passes, int totalTiles)
      throws IOException {
    logLine(
        writer,
        String.format(
            "Summary | Turns %d | Passes %d | Board %d/%d | Bag %d | Scores: %s",
            turns,
            passes,
            boardTileCount(state),
            totalTiles,
            state.bag().size(),
            state.players().stream()
                .map(player -> player.name() + "=" + player.score())
                .reduce((left, right) -> left + ", " + right)
                .orElse("-")
        )
    );
  }

  private String formatRack(List<Tile> tiles) {
    StringBuilder builder = new StringBuilder();
    for (int i = 0; i < tiles.size(); i++) {
      builder.append(formatTile(tiles.get(i)));
      if (i < tiles.size() - 1) {
        builder.append(' ');
      }
    }
    return builder.toString();
  }

  private String formatTile(Tile tile) {
    if (tile.blank()) {
      return "_";
    }
    return tile.letter() + Integer.toString(tile.points());
  }

  private Path buildReportPath(Path repoRoot) throws IOException {
    String configured = System.getProperty("ai.report.dir");
    Path reports = configured == null || configured.isBlank()
        ? repoRoot.resolve("reports")
        : resolveReportDir(repoRoot, configured);
    Files.createDirectories(reports);
    String timestamp = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")
        .withZone(ZoneOffset.UTC)
        .format(Instant.now());
    return reports.resolve("ai-self-play-" + timestamp + ".log");
  }

  private Path resolveReportDir(Path repoRoot, String configured) {
    Path path = Path.of(configured);
    return path.isAbsolute() ? path : repoRoot.resolve(path);
  }

  private Path findRepoRoot() {
    Path candidate = Path.of(System.getProperty("user.dir"));
    Path fallback = null;
    while (candidate != null) {
      if (Files.exists(candidate.resolve("settings.gradle"))) {
        return candidate;
      }
      if (fallback == null && Files.exists(candidate.resolve("build.gradle"))) {
        fallback = candidate;
      }
      candidate = candidate.getParent();
    }
    if (fallback != null) {
      return fallback;
    }
    throw new IllegalStateException("Unable to locate repo root from " + System.getProperty("user.dir"));
  }

  private void logLine(BufferedWriter writer, String line) throws IOException {
    writer.write(line);
    writer.newLine();
    writer.flush();
    System.out.println(sanitizeForConsole(line));
  }

  private String sanitizeForConsole(String line) {
    StringBuilder builder = new StringBuilder(line.length());
    for (int i = 0; i < line.length(); i++) {
      char ch = line.charAt(i);
      builder.append(ch <= 0x7F ? ch : '?');
    }
    return builder.toString();
  }
}
