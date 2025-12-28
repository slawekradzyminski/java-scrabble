package com.scrabble.backend.ws;

import java.util.Map;
import lombok.Getter;

@Getter
public class GameCommandException extends RuntimeException {
  private final WsMessageType type;
  private final Map<String, Object> payload;

  public GameCommandException(WsMessageType type, Map<String, Object> payload) {
    super(type.name());
    this.type = type;
    this.payload = payload;
  }
}
