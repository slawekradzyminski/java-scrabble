package com.scrabble.backend.ws;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GameCommandResult {
  private List<WsMessage> broadcast;
  private List<WsMessage> direct;

  public static GameCommandResult broadcastOnly(WsMessage... messages) {
    return new GameCommandResult(List.of(messages), List.of());
  }

  public static GameCommandResult directOnly(WsMessage... messages) {
    return new GameCommandResult(List.of(), List.of(messages));
  }
}
