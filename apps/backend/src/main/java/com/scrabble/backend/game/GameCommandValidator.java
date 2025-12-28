package com.scrabble.backend.game;

import com.scrabble.engine.GameState;

final class GameCommandValidator {

  void ensureActive(GameSession session) {
    if (!"active".equals(session.status())) {
      throw GameCommandErrors.rejected("game_ended");
    }
  }

  void ensureNoPendingMove(GameState state) {
    if (state.pendingMove() != null) {
      throw GameCommandErrors.rejected("pending_move");
    }
  }

  int requirePlayerIndex(GameState state, String playerName) {
    for (int i = 0; i < state.players().size(); i++) {
      if (state.players().get(i).name().equals(playerName)) {
        return i;
      }
    }
    throw GameCommandErrors.rejected("unknown_player");
  }

  void ensureCurrentPlayer(GameState state, int playerIndex) {
    if (state.currentPlayerIndex() != playerIndex) {
      throw GameCommandErrors.rejected("not_your_turn");
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
