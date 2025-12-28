package com.scrabble.backend.game;

import com.scrabble.backend.ws.GameCommandException;
import com.scrabble.backend.ws.WsMessageType;
import java.util.Map;

final class GameCommandErrors {
  private GameCommandErrors() { }

  static GameCommandException rejected(String reason) {
    return new GameCommandException(WsMessageType.MOVE_REJECTED, Map.of("reason", reason));
  }
}
