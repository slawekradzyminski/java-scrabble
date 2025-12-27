package com.scrabble.backend.ws;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.scrabble.backend.game.GameService;
import com.scrabble.backend.game.GameSnapshot;
import com.scrabble.engine.Coordinate;
import com.scrabble.engine.LetterTile;
import com.scrabble.engine.PlacedTile;
import com.scrabble.engine.Tile;
import java.net.URI;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.WebSocketMessage;
import org.springframework.web.reactive.socket.WebSocketSession;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

@Component
public class GameWebSocketHandler implements WebSocketHandler {
  private final ObjectMapper objectMapper;
  private final GameService gameService;
  private final GameHub gameHub;

  public GameWebSocketHandler(ObjectMapper objectMapper, GameService gameService, GameHub gameHub) {
    this.objectMapper = objectMapper;
    this.gameService = gameService;
    this.gameHub = gameHub;
  }

  @Override
  public Mono<Void> handle(WebSocketSession session) {
    Optional<String> roomId = findQueryParam(session.getHandshakeInfo().getUri(), "roomId");
    Optional<String> player = findQueryParam(session.getHandshakeInfo().getUri(), "player");
    String roomKey = roomId.orElse(null);

    Sinks.Many<WsMessage> direct = Sinks.many().unicast().onBackpressureBuffer();
    Flux<WebSocketMessage> outbound = Flux.merge(
        direct.asFlux(),
        roomKey == null ? Flux.empty() : gameHub.sinkForRoom(roomKey).asFlux()
    ).map(message -> session.textMessage(toJson(message)));

    direct.tryEmitNext(snapshotFor(roomKey));

    Mono<Void> inbound = session.receive()
        .map(WebSocketMessage::getPayloadAsText)
        .doOnNext(text -> handleIncoming(text, roomKey, player.orElse(null), direct))
        .then();

    return session.send(outbound).and(inbound);
  }

  private void handleIncoming(
      String text,
      String roomId,
      String playerFromQuery,
      Sinks.Many<WsMessage> direct) {
    JsonNode node;
    try {
      node = objectMapper.readTree(text);
    } catch (JsonProcessingException e) {
      direct.tryEmitNext(new WsMessage("ERROR", Map.of("reason", "invalid_json")));
      return;
    }

    String type = node.path("type").asText();
    JsonNode payload = node.path("payload");
    if ("SYNC".equals(type)) {
      direct.tryEmitNext(snapshotFor(roomId));
      return;
    }
    if ("PING".equals(type)) {
      direct.tryEmitNext(new WsMessage("PONG", Map.of("serverTime", Instant.now().toString())));
      return;
    }
    if (roomId == null || roomId.isBlank()) {
      direct.tryEmitNext(new WsMessage("ERROR", Map.of("reason", "missing_room")));
      return;
    }

    String player = payload.path("player").asText(null);
    if (player == null || player.isBlank()) {
      player = playerFromQuery;
    }
    if (player == null || player.isBlank()) {
      direct.tryEmitNext(new WsMessage("MOVE_REJECTED", Map.of("reason", "missing_player")));
      return;
    }

    try {
      GameCommandResult result = switch (type) {
        case "PLAY_TILES" -> gameService.playTiles(roomId, player, parsePlacements(payload));
        case "EXCHANGE" -> gameService.exchange(roomId, player, parseTiles(payload));
        case "PASS" -> gameService.pass(roomId, player);
        case "CHALLENGE" -> gameService.challenge(roomId, player);
        case "RESIGN" -> gameService.resign(roomId, player);
        default -> throw new GameCommandException("ERROR", Map.of("reason", "unknown_command"));
      };
      emitAll(result.direct(), direct);
      emitAll(result.broadcast(), gameHub.sinkForRoom(roomId));
    } catch (GameCommandException e) {
      direct.tryEmitNext(new WsMessage(e.type(), e.payload()));
    } catch (RuntimeException e) {
      direct.tryEmitNext(new WsMessage("ERROR", Map.of("reason", "server_error")));
    }
  }

  private WsMessage snapshotFor(String roomId) {
    if (roomId == null) {
      return new WsMessage("STATE_SNAPSHOT", Map.of("message", "missing_room"));
    }
    GameSnapshot snapshot = gameService.snapshot(roomId);
    Map<String, Object> payload = new HashMap<>(snapshot.toPayload());
    payload.put("serverTime", Instant.now().toString());
    return new WsMessage("STATE_SNAPSHOT", payload);
  }

  private String toJson(WsMessage message) {
    try {
      return objectMapper.writeValueAsString(message);
    } catch (JsonProcessingException e) {
      return "{}";
    }
  }

  private Optional<String> findQueryParam(URI uri, String key) {
    String query = uri.getQuery();
    if (query == null || query.isBlank()) {
      return Optional.empty();
    }
    for (String part : query.split("&")) {
      String[] pair = part.split("=", 2);
      if (pair.length == 2 && pair[0].equals(key)) {
        return Optional.of(pair[1]);
      }
    }
    return Optional.empty();
  }

  private void emitAll(List<WsMessage> messages, Sinks.Many<WsMessage> sink) {
    for (WsMessage message : messages) {
      sink.tryEmitNext(message);
    }
  }

  private Map<Coordinate, PlacedTile> parsePlacements(JsonNode payload) {
    JsonNode placementsNode = payload.path("placements");
    if (!placementsNode.isArray()) {
      throw new GameCommandException("MOVE_REJECTED", Map.of("reason", "missing_placements"));
    }
    Map<Coordinate, PlacedTile> placements = new HashMap<>();
    for (JsonNode node : placementsNode) {
      String coordinateText = node.path("coordinate").asText(null);
      String letterText = node.path("letter").asText(null);
      boolean blank = node.path("blank").asBoolean(false);
      if (coordinateText == null || letterText == null || letterText.isBlank()) {
        throw new GameCommandException("MOVE_REJECTED", Map.of("reason", "invalid_placement"));
      }
      char letter = normalizeLetter(letterText);
      Tile tile = blank ? Tile.blankTile() : LetterTile.fromLetter(letter).toTile();
      PlacedTile placed = new PlacedTile(tile, letter);
      Coordinate coordinate = Coordinate.parse(coordinateText);
      if (placements.putIfAbsent(coordinate, placed) != null) {
        throw new GameCommandException("MOVE_REJECTED", Map.of("reason", "duplicate_coordinate"));
      }
    }
    return placements;
  }

  private List<Tile> parseTiles(JsonNode payload) {
    JsonNode tilesNode = payload.path("tiles");
    if (!tilesNode.isArray()) {
      throw new GameCommandException("MOVE_REJECTED", Map.of("reason", "missing_tiles"));
    }
    List<Tile> tiles = new ArrayList<>();
    for (JsonNode node : tilesNode) {
      String letterText = node.path("letter").asText(null);
      boolean blank = node.path("blank").asBoolean(false);
      if (blank) {
        tiles.add(Tile.blankTile());
        continue;
      }
      if (letterText == null || letterText.isBlank()) {
        throw new GameCommandException("MOVE_REJECTED", Map.of("reason", "invalid_tile"));
      }
      char letter = normalizeLetter(letterText);
      tiles.add(LetterTile.fromLetter(letter).toTile());
    }
    return tiles;
  }

  private char normalizeLetter(String text) {
    String trimmed = text.trim();
    if (trimmed.length() != 1) {
      throw new GameCommandException("MOVE_REJECTED", Map.of("reason", "invalid_letter"));
    }
    return trimmed.toUpperCase(Locale.forLanguageTag("pl-PL")).charAt(0);
  }
}
