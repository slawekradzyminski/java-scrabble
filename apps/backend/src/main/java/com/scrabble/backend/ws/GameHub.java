package com.scrabble.backend.ws;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Sinks;

@Component
public class GameHub {
  private final Map<String, Sinks.Many<WsMessage>> rooms = new ConcurrentHashMap<>();

  public Sinks.Many<WsMessage> sinkForRoom(String roomId) {
    return rooms.computeIfAbsent(roomId, ignored ->
        Sinks.many().multicast().onBackpressureBuffer());
  }
}
