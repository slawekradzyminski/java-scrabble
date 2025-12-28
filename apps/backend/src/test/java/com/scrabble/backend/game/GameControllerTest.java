package com.scrabble.backend.game;

import com.scrabble.backend.BackendApplication;
import com.scrabble.backend.TestDictionaryConfig;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.web.reactive.server.WebTestClient;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(
    classes = {BackendApplication.class, TestDictionaryConfig.class},
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = "spring.main.allow-bean-definition-overriding=true")
class GameControllerTest {

  @LocalServerPort
  private int port;

  @Test
  void startsGameAndReturnsSnapshot() {
    // given
    WebTestClient client = buildClient();
    AtomicReference<String> roomId = new AtomicReference<>();

    // when
    client.post()
        .uri("/api/rooms")
        .bodyValue(Map.of("name", "Room 3", "owner", "Dana"))
        .exchange()
        .expectStatus().isOk()
        .expectBody()
        .jsonPath("$.id").value(value -> roomId.set(value.toString()));

    // then
    client.post()
        .uri("/api/rooms/{roomId}/game/start", roomId.get())
        .exchange()
        .expectStatus().isOk()
        .expectBody()
        .jsonPath("$.status").isEqualTo("active")
        .jsonPath("$.players.length()").isEqualTo(1)
        .jsonPath("$.players[0].rackSize").isEqualTo(7)
        .jsonPath("$.bagCount").isEqualTo(93);
  }

  @Test
  void commandEndpointAutoRejectsInvalidMove() {
    // given
    WebTestClient client = buildClient();
    String roomId = createRoom(client, "Room 4", "Alice");
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

    // when
    client.post()
        .uri("/api/rooms/{roomId}/game/command", roomId)
        .bodyValue(Map.of(
            "type", "PLAY_TILES",
            "player", "Alice",
            "payload", Map.of(
                "placements", List.of(
                    Map.of("coordinate", "H8", "letter", letters.get(0), "blank", false),
                    Map.of("coordinate", "H9", "letter", letters.get(1), "blank", false)
                )
            )
        ))
        .exchange()
        .expectStatus().isOk();

    // then
    Map<String, Object> after = fetchState(client, roomId, "Alice");
    assertThat(((Number) after.get("boardTiles")).intValue()).isEqualTo(0);
    assertThat(((Number) after.get("currentPlayerIndex")).intValue()).isEqualTo(1);
  }

  @Test
  void eventsEndpointReturnsHistoryWithIds() {
    // given
    WebTestClient client = buildClient();
    String roomId = createRoom(client, "Room Events", "Alice");
    client.post()
        .uri("/api/rooms/{roomId}/join", roomId)
        .bodyValue(Map.of("player", "Bob"))
        .exchange()
        .expectStatus().isOk();

    client.post()
        .uri("/api/rooms/{roomId}/game/start", roomId)
        .exchange()
        .expectStatus().isOk();

    // when
    client.post()
        .uri("/api/rooms/{roomId}/game/command", roomId)
        .bodyValue(Map.of(
            "type", "PASS",
            "player", "Alice",
            "payload", Map.of()
        ))
        .exchange()
        .expectStatus().isOk();

    Map<String, Object> events = fetchEvents(client, roomId, 0);

    // then
    @SuppressWarnings("unchecked")
    List<Map<String, Object>> entries = (List<Map<String, Object>>) events.get("events");
    assertThat(entries).isNotEmpty();
    assertThat(entries.get(0)).containsKey("eventId");
    assertThat(entries).anyMatch(entry -> "PASS".equals(entry.get("type")));
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
  private Map<String, Object> fetchEvents(WebTestClient client, String roomId, long after) {
    return client.get()
        .uri(uriBuilder -> uriBuilder.path("/api/rooms/{roomId}/game/events")
            .queryParam("after", after)
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
      if (Boolean.TRUE.equals(tile.get("blank"))) {
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
