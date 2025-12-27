package com.scrabble.backend.lobby;

import static org.assertj.core.api.Assertions.assertThat;

import com.scrabble.backend.BackendApplication;
import com.scrabble.backend.TestDictionaryConfig;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.web.reactive.server.WebTestClient;

@SpringBootTest(
    classes = {BackendApplication.class, TestDictionaryConfig.class},
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = "spring.main.allow-bean-definition-overriding=true")
class RoomControllerTest {

  @LocalServerPort
  private int port;

  @Test
  void createsAndListsRooms() {
    // given
    WebTestClient client = buildClient();
    // when + then
    client.post()
        .uri("/api/rooms")
        .bodyValue(Map.of("name", "Room 1", "owner", "Alice"))
        .exchange()
        .expectStatus().isOk()
        .expectBody()
        .jsonPath("$.name").isEqualTo("Room 1")
        .jsonPath("$.players.length()").isEqualTo(1);

    client.get()
        .uri("/api/rooms")
        .exchange()
        .expectStatus().isOk()
        .expectBody()
        .jsonPath("$[?(@.name=='Room 1')]").exists();
  }

  @Test
  void createsRoomWithAiOpponent() {
    // given
    WebTestClient client = buildClient();

    // when + then
    client.post()
        .uri("/api/rooms")
        .bodyValue(Map.of("name", "Room AI", "owner", "Alice", "ai", true))
        .exchange()
        .expectStatus().isOk()
        .expectBody()
        .jsonPath("$.players.length()").isEqualTo(2)
        .jsonPath("$.players[1]").value(value -> assertThat(value.toString()).startsWith("Computer"));
  }

  @Test
  void joinsRoom() {
    // given
    AtomicReference<String> roomId = new AtomicReference<>();
    WebTestClient client = buildClient();

    // when + then
    client.post()
        .uri("/api/rooms")
        .bodyValue(Map.of("name", "Room 2", "owner", "Bob"))
        .exchange()
        .expectStatus().isOk()
        .expectBody()
        .jsonPath("$.id").value(value -> roomId.set(value.toString()));

    client.post()
        .uri("/api/rooms/{roomId}/join", roomId.get())
        .bodyValue(Map.of("player", "Carol"))
        .exchange()
        .expectStatus().isOk()
        .expectBody()
        .jsonPath("$.players.length()").isEqualTo(2);

    client.post()
        .uri("/api/rooms/{roomId}/join", roomId.get())
        .bodyValue(Map.of("player", "Carol"))
        .exchange()
        .expectStatus().isOk()
        .expectBody()
        .jsonPath("$.players.length()").isEqualTo(2);
  }

  private WebTestClient buildClient() {
    return WebTestClient.bindToServer()
        .baseUrl("http://localhost:" + port)
        .build();
  }
}
