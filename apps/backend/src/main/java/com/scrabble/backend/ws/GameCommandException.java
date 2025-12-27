package com.scrabble.backend.ws;

import java.util.Map;

public class GameCommandException extends RuntimeException {
  private final String type;
  private final Map<String, Object> payload;

  public GameCommandException(String type, Map<String, Object> payload) {
    super(type);
    this.type = type;
    this.payload = payload;
  }

  public String type() {
    return type;
  }

  public Map<String, Object> payload() {
    return payload;
  }
}
