package com.scrabble.backend.ws;

import java.util.Map;

public class GameCommandException extends RuntimeException {
  private final WsMessageType type;
  private final Map<String, Object> payload;

  public GameCommandException(WsMessageType type, Map<String, Object> payload) {
    super(type.name());
    this.type = type;
    this.payload = payload;
  }

  public WsMessageType type() {
    return type;
  }

  public Map<String, Object> payload() {
    return payload;
  }
}
