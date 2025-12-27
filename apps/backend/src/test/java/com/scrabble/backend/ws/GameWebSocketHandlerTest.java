package com.scrabble.backend.ws;

import com.scrabble.backend.BackendApplication;
import com.scrabble.backend.TestDictionaryConfig;
import java.net.URI;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.socket.client.ReactorNettyWebSocketClient;
import reactor.core.publisher.Mono;

import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(
    classes = {BackendApplication.class, TestDictionaryConfig.class},
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = "spring.main.allow-bean-definition-overriding=true")
class GameWebSocketHandlerTest {

  @LocalServerPort
  private int port;

  @Test
  void returnsSnapshotForMissingRoom() {
    ReactorNettyWebSocketClient client = new ReactorNettyWebSocketClient();
    AtomicReference<String> payload = new AtomicReference<>();
    client.execute(URI.create("ws://localhost:" + port + "/ws"), session ->
        session.receive()
            .map(message -> message.getPayloadAsText())
            .next()
            .doOnNext(payload::set)
            .then()
    ).block(Duration.ofSeconds(5));

    assertTrue(payload.get().contains("STATE_SNAPSHOT"));
    assertTrue(payload.get().contains("missing_room"));
  }

  @Test
  void respondsToPing() {
    ReactorNettyWebSocketClient client = new ReactorNettyWebSocketClient();
    AtomicReference<List<String>> payloads = new AtomicReference<>();
    client.execute(URI.create("ws://localhost:" + port + "/ws?roomId=1"), session ->
        session.send(Mono.just(session.textMessage("{\"type\":\"PING\"}")))
            .thenMany(session.receive().map(message -> message.getPayloadAsText()).take(2).collectList())
            .doOnNext(payloads::set)
            .then()
    ).block(Duration.ofSeconds(5));

    assertTrue(payloads.get().stream().anyMatch(payload -> payload.contains("PONG")));
  }

  @Test
  void playTilesBroadcastsMoveProposed() {
    WebTestClient client = buildClient();
    String roomId = createRoom(client, "Room Ws", "Alice");
    client.post()
        .uri("/api/rooms/{roomId}/join", roomId)
        .bodyValue(Map.of("player", "Bob"))
        .exchange()
        .expectStatus().isOk();

    client.post()
        .uri("/api/rooms/{roomId}/game/start", roomId)
        .exchange()
        .expectStatus().isOk();

    Map<String, Object> state = fetchState(client, roomId);
    List<String> letters = pickNonBlankLetters(state, 2);

    String message = "{\"type\":\"PLAY_TILES\",\"payload\":{"
        + "\"player\":\"Alice\",\"placements\":["
        + "{\"coordinate\":\"H8\",\"letter\":\"" + letters.get(0) + "\",\"blank\":false},"
        + "{\"coordinate\":\"H9\",\"letter\":\"" + letters.get(1) + "\",\"blank\":false}"
        + "]}}";

    ReactorNettyWebSocketClient wsClient = new ReactorNettyWebSocketClient();
    AtomicReference<List<String>> payloads = new AtomicReference<>();
    wsClient.execute(URI.create("ws://localhost:" + port + "/ws?roomId=" + roomId + "&player=Alice"), session ->
        session.send(Mono.just(session.textMessage(message)))
            .thenMany(session.receive().map(msg -> msg.getPayloadAsText()).take(3).collectList())
            .doOnNext(payloads::set)
            .then()
    ).block(Duration.ofSeconds(5));

    assertTrue(payloads.get().stream().anyMatch(payload -> payload.contains("MOVE_PROPOSED")));
  }

  private WebTestClient buildClient() {
    return WebTestClient.bindToServer()
        .baseUrl("http://localhost:" + port)
        .build();
  }

  private String createRoom(WebTestClient client, String name, String owner) {
    AtomicReference<String> roomId = new AtomicReference<>();
    client.post()
        .uri("/api/rooms")
        .bodyValue(Map.of("name", name, "owner", owner))
        .exchange()
        .expectStatus().isOk()
        .expectBody()
        .jsonPath("$.id").value(value -> roomId.set(value.toString()));
    return roomId.get();
  }

  @SuppressWarnings("unchecked")
  private Map<String, Object> fetchState(WebTestClient client, String roomId) {
    return client.get()
        .uri("/api/rooms/{roomId}/game/state", roomId)
        .exchange()
        .expectStatus().isOk()
        .expectBody(Map.class)
        .returnResult()
        .getResponseBody();
  }

  @SuppressWarnings("unchecked")
  private List<String> pickNonBlankLetters(Map<String, Object> state, int count) {
    List<Map<String, Object>> players = (List<Map<String, Object>>) state.get("players");
    Map<String, Object> first = players.get(0);
    List<Map<String, Object>> rack = (List<Map<String, Object>>) first.get("rack");
    List<String> letters = new ArrayList<>();
    for (Map<String, Object> tile : rack) {
      Object blank = tile.get("blank");
      if (Boolean.TRUE.equals(blank)) {
        continue;
      }
      Object letter = tile.get("letter");
      if (letter != null) {
        letters.add(letter.toString());
      }
      if (letters.size() == count) {
        return letters;
      }
    }
    throw new IllegalStateException("Not enough non-blank tiles");
  }
}
