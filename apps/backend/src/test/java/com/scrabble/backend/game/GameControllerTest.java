package com.scrabble.backend.game;

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
class GameControllerTest {

  @LocalServerPort
  private int port;

  @Test
  void startsGameAndReturnsSnapshot() {
    WebTestClient client = buildClient();
    AtomicReference<String> roomId = new AtomicReference<>();

    client.post()
        .uri("/api/rooms")
        .bodyValue(Map.of("name", "Room 3", "owner", "Dana"))
        .exchange()
        .expectStatus().isOk()
        .expectBody()
        .jsonPath("$.id").value(value -> roomId.set(value.toString()));

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

  private WebTestClient buildClient() {
    return WebTestClient.bindToServer()
        .baseUrl("http://localhost:" + port)
        .build();
  }
}
