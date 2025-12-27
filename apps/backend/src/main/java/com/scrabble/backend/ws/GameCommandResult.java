package com.scrabble.backend.ws;

import java.util.List;

public record GameCommandResult(List<WsMessage> broadcast, List<WsMessage> direct) {
  public static GameCommandResult broadcastOnly(WsMessage... messages) {
    return new GameCommandResult(List.of(messages), List.of());
  }

  public static GameCommandResult directOnly(WsMessage... messages) {
    return new GameCommandResult(List.of(), List.of(messages));
  }
}
