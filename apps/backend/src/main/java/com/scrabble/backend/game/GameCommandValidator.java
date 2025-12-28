package com.scrabble.backend.game;

import com.scrabble.engine.GameState;
import org.springframework.stereotype.Component;

@Component
public final class GameCommandValidator {

  void ensureActive(GameSession session) {
    if (!"active".equals(session.status())) {
      throw GameCommandErrors.rejected(GameCommandReasons.GAME_ENDED);
    }
  }

  void ensureNoPendingMove(GameState state) {
    if (state.pendingMove() != null) {
      throw GameCommandErrors.rejected(GameCommandReasons.PENDING_MOVE);
    }
  }

  int requirePlayerIndex(GameState state, String playerName) {
    for (int i = 0; i < state.players().size(); i++) {
      if (state.players().get(i).name().equals(playerName)) {
        return i;
      }
    }
    throw GameCommandErrors.rejected(GameCommandReasons.UNKNOWN_PLAYER);
  }

  void ensureCurrentPlayer(GameState state, int playerIndex) {
    if (state.currentPlayerIndex() != playerIndex) {
      throw GameCommandErrors.rejected(GameCommandReasons.NOT_YOUR_TURN);
    }
  }

  String determineWinner(GameState state, int resigningIndex) {
    if (state.players().isEmpty()) {
      return null;
    }
    int winnerIndex = (resigningIndex + 1) % state.players().size();
    return state.players().get(winnerIndex).name();
  }
}
