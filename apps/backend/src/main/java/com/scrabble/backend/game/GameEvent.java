package com.scrabble.backend.game;

import com.scrabble.backend.ws.WsMessageType;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GameEvent {
  private long eventId;
  private WsMessageType type;
  private Map<String, Object> payload;
  private String time;

  public Map<String, Object> toMap() {
    Map<String, Object> entry = new LinkedHashMap<>();
    entry.put("eventId", eventId);
    entry.put("type", type.name());
    entry.put("payload", payload);
    entry.put("time", time);
    return entry;
  }
}
