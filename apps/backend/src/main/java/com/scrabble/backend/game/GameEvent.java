package com.scrabble.backend.game;

import com.scrabble.backend.ws.WsMessageType;
import java.util.LinkedHashMap;
import java.util.Map;

public record GameEvent(long eventId, WsMessageType type, Map<String, Object> payload, String time) {

  public Map<String, Object> toMap() {
    Map<String, Object> entry = new LinkedHashMap<>();
    entry.put("eventId", eventId);
    entry.put("type", type.name());
    entry.put("payload", payload);
    entry.put("time", time);
    return entry;
  }
}
