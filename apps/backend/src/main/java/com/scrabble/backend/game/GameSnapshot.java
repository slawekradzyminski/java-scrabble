package com.scrabble.backend.game;

import com.scrabble.engine.BoardState;
import com.scrabble.engine.Coordinate;
import com.scrabble.engine.GameState;
import com.scrabble.engine.PendingMove;
import com.scrabble.engine.PlacedTile;
import com.scrabble.engine.Player;
import com.scrabble.engine.Rack;
import com.scrabble.engine.ScoringResult;
import com.scrabble.engine.Tile;
import com.scrabble.engine.Word;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public record GameSnapshot(
    String roomId,
    String status,
    List<Map<String, Object>> players,
    int bagCount,
    int boardTiles,
    Integer currentPlayerIndex,
    boolean pendingMove,
    List<Map<String, Object>> board,
    Map<String, Object> pending,
    String winner) {

  public static GameSnapshot from(GameSession session, String status) {
    GameState state = session.state();
    BoardState board = state.board();
    List<Map<String, Object>> players = state.players().stream()
        .map(GameSnapshot::playerToMap)
        .collect(Collectors.toList());
    List<Map<String, Object>> boardTiles = boardTiles(board);
    Map<String, Object> pending = pendingToMap(state.pendingMove(), state.currentPlayerIndex());
    return new GameSnapshot(
        session.roomId(),
        status,
        players,
        state.bag().size(),
        board.tiles().size(),
        state.currentPlayerIndex(),
        state.pendingMove() != null,
        boardTiles,
        pending,
        session.winner());
  }

  public static GameSnapshot missing(String roomId) {
    return new GameSnapshot(roomId, "not_started", List.of(), 0, 0, null, false, List.of(), null, null);
  }

  private static Map<String, Object> playerToMap(Player player) {
    List<Map<String, Object>> rackTiles = player.rack().tiles().stream()
        .map(GameSnapshot::tileToMap)
        .collect(Collectors.toList());
    return Map.of(
        "name", player.name(),
        "score", player.score(),
        "rackSize", player.rack().size(),
        "rackCapacity", Rack.CAPACITY,
        "rack", rackTiles);
  }

  public Map<String, Object> toPayload() {
    Map<String, Object> payload = new LinkedHashMap<>();
    payload.put("roomId", roomId);
    payload.put("status", status);
    payload.put("players", players);
    payload.put("bagCount", bagCount);
    payload.put("boardTiles", boardTiles);
    payload.put("board", board);
    payload.put("currentPlayerIndex", currentPlayerIndex);
    payload.put("pendingMove", pendingMove);
    payload.put("pending", pending);
    payload.put("winner", winner);
    return payload;
  }

  private static List<Map<String, Object>> boardTiles(BoardState board) {
    List<Map<String, Object>> tiles = new ArrayList<>();
    for (Map.Entry<Coordinate, PlacedTile> entry : board.tiles().entrySet()) {
      tiles.add(placedTileToMap(entry.getKey(), entry.getValue()));
    }
    return tiles;
  }

  private static Map<String, Object> pendingToMap(PendingMove pendingMove, int playerIndex) {
    if (pendingMove == null) {
      return null;
    }
    ScoringResult scoring = pendingMove.scoringResult();
    List<Map<String, Object>> words = scoring.words().stream()
        .map(GameSnapshot::wordToMap)
        .collect(Collectors.toList());
    List<Map<String, Object>> placements = pendingMove.placement().placements().entrySet().stream()
        .map(entry -> placedTileToMap(entry.getKey(), entry.getValue()))
        .collect(Collectors.toList());
    return Map.of(
        "playerIndex", playerIndex,
        "score", scoring.totalScore(),
        "words", words,
        "placements", placements);
  }

  private static Map<String, Object> wordToMap(Word word) {
    List<String> coords = word.coordinates().stream()
        .map(Coordinate::format)
        .collect(Collectors.toList());
    return Map.of(
        "text", word.text(),
        "coordinates", coords);
  }

  private static Map<String, Object> placedTileToMap(Coordinate coordinate, PlacedTile placed) {
    Map<String, Object> tile = tileToMap(placed.tile());
    Map<String, Object> map = new LinkedHashMap<>();
    map.put("coordinate", coordinate.format());
    map.putAll(tile);
    map.put("assignedLetter", Character.toString(placed.assignedLetter()));
    return map;
  }

  private static Map<String, Object> tileToMap(Tile tile) {
    if (tile.blank()) {
      Map<String, Object> map = new LinkedHashMap<>();
      map.put("letter", null);
      map.put("points", tile.points());
      map.put("blank", true);
      return map;
    }
    return Map.of(
        "letter", Character.toString(tile.letter()),
        "points", tile.points(),
        "blank", false);
  }
}
