package com.scrabble.backend.game;

import com.scrabble.backend.ws.WsMessage;
import com.scrabble.backend.ws.WsMessageType;
import com.scrabble.engine.Coordinate;
import com.scrabble.engine.GameState;
import com.scrabble.engine.PlacedTile;
import com.scrabble.engine.Player;
import com.scrabble.engine.ScoringResult;
import com.scrabble.engine.Tile;
import com.scrabble.engine.Word;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public final class GameMessageFactory {

  WsMessage moveAccepted(Player player, ScoringResult scoring, Map<Coordinate, PlacedTile> placements, int stateVersion) {
    return new WsMessage(WsMessageType.MOVE_ACCEPTED, Map.of(
        "player", player.name(),
        "score", scoring.totalScore(),
        "words", wordsToPayload(scoring.words()),
        "placements", placementsToPayload(placements),
        "stateVersion", stateVersion));
  }

  WsMessage moveRejected(String player, String reason, List<String> invalidWords, int stateVersion) {
    return new WsMessage(WsMessageType.MOVE_REJECTED, Map.of(
        "player", player,
        "reason", reason,
        "invalidWords", invalidWords,
        "stateVersion", stateVersion));
  }

  WsMessage pass(String player, int stateVersion) {
    return new WsMessage(WsMessageType.PASS, Map.of(
        "player", player,
        "stateVersion", stateVersion));
  }

  WsMessage exchange(String player, int count, int stateVersion) {
    return new WsMessage(WsMessageType.EXCHANGE, Map.of(
        "player", player,
        "count", count,
        "stateVersion", stateVersion));
  }

  WsMessage turnAdvanced(GameState state, int stateVersion) {
    Map<String, Object> payload = new LinkedHashMap<>();
    int index = state.currentPlayerIndex();
    String name = state.players().get(index).name();
    payload.put("currentPlayerIndex", index);
    payload.put("currentPlayer", name);
    payload.put("stateVersion", stateVersion);
    return new WsMessage(WsMessageType.TURN_ADVANCED, payload);
  }

  WsMessage gameEnded(String winner, int stateVersion) {
    return new WsMessage(WsMessageType.GAME_ENDED, Map.of(
        "winner", winner,
        "stateVersion", stateVersion));
  }

  WsMessage gameEndedByResign(String winner, String resigned, int stateVersion) {
    return new WsMessage(WsMessageType.GAME_ENDED, Map.of(
        "winner", winner,
        "resigned", resigned,
        "stateVersion", stateVersion));
  }

  WsMessage snapshot(GameSession session) {
    return new WsMessage(WsMessageType.STATE_SNAPSHOT,
        GameSnapshot.from(session, session.status()).toPayload());
  }

  private List<Map<String, Object>> wordsToPayload(List<Word> words) {
    return words.stream()
        .map(word -> Map.<String, Object>of(
            "text", word.text(),
            "coordinates", word.coordinates().stream()
                .map(Coordinate::format)
                .collect(Collectors.toList())))
        .collect(Collectors.toList());
  }

  private List<Map<String, Object>> placementsToPayload(Map<Coordinate, PlacedTile> placements) {
    return placements.entrySet().stream()
        .map(entry -> placedTileToPayload(entry.getKey(), entry.getValue()))
        .collect(Collectors.toList());
  }

  private Map<String, Object> placedTileToPayload(Coordinate coordinate, PlacedTile placed) {
    Map<String, Object> tile = tileToPayload(placed.tile());
    Map<String, Object> payload = new LinkedHashMap<>();
    payload.put("coordinate", coordinate.format());
    payload.putAll(tile);
    payload.put("assignedLetter", Character.toString(placed.assignedLetter()));
    return payload;
  }

  private Map<String, Object> tileToPayload(Tile tile) {
    if (tile.blank()) {
      Map<String, Object> payload = new LinkedHashMap<>();
      payload.put("letter", null);
      payload.put("points", tile.points());
      payload.put("blank", true);
      return payload;
    }
    return Map.of(
        "letter", Character.toString(tile.letter()),
        "points", tile.points(),
        "blank", false);
  }
}
