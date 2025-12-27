package com.scrabble.backend.ws;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.scrabble.backend.lobby.Room;
import com.scrabble.backend.lobby.RoomService;
import java.net.URI;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.WebSocketMessage;
import org.springframework.web.reactive.socket.WebSocketSession;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Component
public class GameWebSocketHandler implements WebSocketHandler {
  private final ObjectMapper objectMapper;
  private final RoomService roomService;

  public GameWebSocketHandler(ObjectMapper objectMapper, RoomService roomService) {
    this.objectMapper = objectMapper;
    this.roomService = roomService;
  }

  @Override
  public Mono<Void> handle(WebSocketSession session) {
    Optional<String> roomId = findQueryParam(session.getHandshakeInfo().getUri(), "roomId");

    Mono<WebSocketMessage> greeting = Mono.fromSupplier(() ->
        session.textMessage(toJson(snapshotFor(roomId.orElse(null)))));

    Flux<WebSocketMessage> incoming = session.receive()
        .map(WebSocketMessage::getPayloadAsText)
        .map(text -> session.textMessage(toJson(handleIncoming(text, roomId.orElse(null)))));

    return session.send(Flux.concat(greeting, incoming));
  }

  private WsMessage handleIncoming(String text, String roomId) {
    JsonNode node;
    try {
      node = objectMapper.readTree(text);
    } catch (JsonProcessingException e) {
      return new WsMessage("ERROR", Map.of("reason", "invalid_json"));
    }

    String type = node.path("type").asText();
    if ("SYNC".equals(type)) {
      return snapshotFor(roomId);
    }
    if ("PING".equals(type)) {
      return new WsMessage("PONG", Map.of("serverTime", Instant.now().toString()));
    }
    return new WsMessage("MOVE_REJECTED", Map.of("reason", "not_implemented"));
  }

  private WsMessage snapshotFor(String roomId) {
    if (roomId == null) {
      return new WsMessage("STATE_SNAPSHOT", Map.of("message", "missing_room"));
    }
    Optional<Room> room = roomService.find(roomId);
    if (room.isEmpty()) {
      return new WsMessage("STATE_SNAPSHOT", Map.of("message", "room_not_found", "roomId", roomId));
    }
    Room found = room.get();
    return new WsMessage("STATE_SNAPSHOT", Map.of(
        "roomId", found.id(),
        "roomName", found.name(),
        "players", found.players(),
        "serverTime", Instant.now().toString()
    ));
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
}
