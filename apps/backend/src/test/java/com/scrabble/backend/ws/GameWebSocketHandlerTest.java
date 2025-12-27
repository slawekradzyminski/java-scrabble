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

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(
    classes = {BackendApplication.class, TestDictionaryConfig.class},
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = "spring.main.allow-bean-definition-overriding=true")
class GameWebSocketHandlerTest {

  @LocalServerPort
  private int port;

  @Test
  void returnsSnapshotForMissingRoom() {
    // given
    ReactorNettyWebSocketClient client = new ReactorNettyWebSocketClient();
    AtomicReference<String> payload = new AtomicReference<>();

    // when
    client.execute(URI.create("ws://localhost:" + port + "/ws"), session ->
        session.receive()
            .map(message -> message.getPayloadAsText())
            .next()
            .doOnNext(payload::set)
            .then()
    ).block(Duration.ofSeconds(5));

    // then
    assertThat(payload.get()).contains("STATE_SNAPSHOT");
    assertThat(payload.get()).contains("missing_room");
  }

  @Test
  void respondsToPing() {
    // given
    ReactorNettyWebSocketClient client = new ReactorNettyWebSocketClient();
    AtomicReference<List<String>> payloads = new AtomicReference<>();

    // when
    client.execute(URI.create("ws://localhost:" + port + "/ws?roomId=1"), session ->
        session.send(Mono.just(session.textMessage("{\"type\":\"PING\"}")))
            .thenMany(session.receive().map(message -> message.getPayloadAsText()).take(2).collectList())
            .doOnNext(payloads::set)
            .then()
    ).block(Duration.ofSeconds(5));

    // then
    assertThat(payloads.get()).anyMatch(payload -> payload.contains("PONG"));
  }

  @Test
  void playTilesViaWebSocketUpdatesState() {
    // given
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

    Map<String, Object> state = fetchState(client, roomId, "Alice");
    List<String> letters = pickNonBlankLetters(state, 2);

    String message = "{\"type\":\"PLAY_TILES\",\"payload\":{"
        + "\"player\":\"Alice\",\"placements\":["
        + "{\"coordinate\":\"H8\",\"letter\":\"" + letters.get(0) + "\",\"blank\":false},"
        + "{\"coordinate\":\"H9\",\"letter\":\"" + letters.get(1) + "\",\"blank\":false}"
        + "]}}";

    ReactorNettyWebSocketClient wsClient = new ReactorNettyWebSocketClient();

    // when
    wsClient.execute(URI.create("ws://localhost:" + port + "/ws?roomId=" + roomId + "&player=Alice"), session ->
        session.send(Mono.just(session.textMessage(message))).then()
    ).block(Duration.ofSeconds(5));

    // then
    Map<String, Object> after = waitForCurrentPlayerIndex(client, roomId, "Alice", 1);
    assertThat(((Number) after.get("currentPlayerIndex")).intValue()).isEqualTo(1);
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
  private Map<String, Object> fetchState(WebTestClient client, String roomId, String player) {
    return client.get()
        .uri(uriBuilder -> uriBuilder.path("/api/rooms/{roomId}/game/state")
            .queryParam("player", player)
            .build(roomId))
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

  private Map<String, Object> waitForCurrentPlayerIndex(
      WebTestClient client,
      String roomId,
      String player,
      int expectedIndex) {
    Map<String, Object> state = null;
    for (int attempt = 0; attempt < 20; attempt += 1) {
      state = fetchState(client, roomId, player);
      int currentIndex = ((Number) state.get("currentPlayerIndex")).intValue();
      if (currentIndex == expectedIndex) {
        return state;
      }
      try {
        Thread.sleep(50);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        throw new IllegalStateException("Interrupted while waiting for state update", e);
      }
    }
    return state;
  }
}
