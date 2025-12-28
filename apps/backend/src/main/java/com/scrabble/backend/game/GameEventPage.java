package com.scrabble.backend.game;

import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GameEventPage {
  private List<Map<String, Object>> events;
  private long lastEventId;
}
