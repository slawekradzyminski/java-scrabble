package com.scrabble.backend.ws;

import com.scrabble.backend.BackendApplication;
import com.scrabble.backend.TestDictionaryConfig;
import java.net.URI;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
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
}
