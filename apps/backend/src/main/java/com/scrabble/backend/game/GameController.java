package com.scrabble.backend.game;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.scrabble.backend.ws.GameCommandException;
import com.scrabble.backend.ws.GameCommandParser;
import com.scrabble.backend.ws.GameCommandResult;
import com.scrabble.backend.ws.WsMessage;
import com.scrabble.backend.ws.WsMessageType;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/rooms/{roomId}/game")
@RequiredArgsConstructor
public class GameController {
  private final GameService gameService;
  private final ObjectMapper objectMapper;
  private final GameCommandParser parser;

  @PostMapping("/start")
  public GameSnapshot start(@PathVariable String roomId) {
    return GameSnapshot.from(gameService.start(roomId), "active");
  }

  @GetMapping("/state")
  public GameSnapshot state(@PathVariable String roomId, @RequestParam(value = "player", required = false) String player) {
    return gameService.snapshotForPlayer(roomId, player);
  }

  @GetMapping("/events")
  public GameEventPage events(
      @PathVariable String roomId,
      @RequestParam(value = "after", defaultValue = "0") long after,
      @RequestParam(value = "limit", defaultValue = "50") int limit) {
    return gameService.eventsAfter(roomId, after, limit);
  }

  @PostMapping("/command")
  public ResponseEntity<?> command(
      @PathVariable String roomId,
      @RequestBody GameCommandRequest request) {
    JsonNode payload = objectMapper.valueToTree(request.getPayload());
    try {
      GameCommandResult result = switch (request.getType()) {
        case "PLAY_TILES" -> gameService.playTiles(roomId, request.getPlayer(), parser.parsePlacements(payload));
        case "EXCHANGE" -> gameService.exchange(roomId, request.getPlayer(), parser.parseTiles(payload));
        case "PASS" -> gameService.pass(roomId, request.getPlayer());
        case "RESIGN" -> gameService.resign(roomId, request.getPlayer());
        default -> throw new GameCommandException(WsMessageType.ERROR, Map.of("reason", "unknown_command"));
      };
      return ResponseEntity.ok(new GameCommandResponse(result.getBroadcast(), result.getDirect()));
    } catch (GameCommandException e) {
      return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new WsMessage(e.getType(), e.getPayload()));
    }
  }

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  public static class GameCommandRequest {
    private String type;
    private String player;
    private Map<String, Object> payload;
  }

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  public static class GameCommandResponse {
    private List<WsMessage> broadcast;
    private List<WsMessage> direct;
  }
}
