package com.scrabble.backend;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.web.reactive.server.WebTestClient;

@SpringBootTest(
    classes = {BackendApplication.class, TestDictionaryConfig.class},
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = "spring.main.allow-bean-definition-overriding=true")
class DictionaryControllerTest {

  @LocalServerPort
  private int port;

  @Test
  void containsEndpointReturnsTrueForKnownWord() {
    // given
    WebTestClient client = buildClient();
    // when + then
    client.get()
        .uri(uriBuilder -> uriBuilder.path("/api/dictionary/contains")
            .queryParam("word", "zajawiałeś")
            .build())
        .exchange()
        .expectStatus().isOk()
        .expectBody()
        .jsonPath("$.word").isEqualTo("zajawiałeś")
        .jsonPath("$.contains").isEqualTo(true);
  }

  @Test
  void containsEndpointReturnsFalseForUnknownWord() {
    // given
    WebTestClient client = buildClient();
    // when + then
    client.get()
        .uri(uriBuilder -> uriBuilder.path("/api/dictionary/contains")
            .queryParam("word", "nieistniejace")
            .build())
        .exchange()
        .expectStatus().isOk()
        .expectBody()
        .jsonPath("$.contains").isEqualTo(false);
  }

  @Test
  void healthEndpointIsUp() {
    // given
    WebTestClient client = buildClient();
    // when + then
    client.get()
        .uri("/actuator/health")
        .exchange()
        .expectStatus().isOk()
        .expectBody()
        .jsonPath("$.status").isEqualTo("UP");
  }

  private WebTestClient buildClient() {
    return WebTestClient.bindToServer()
        .baseUrl("http://localhost:" + port)
        .build();
  }
}
