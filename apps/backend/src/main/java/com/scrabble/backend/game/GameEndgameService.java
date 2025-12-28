package com.scrabble.backend.game;

import com.scrabble.backend.ws.WsMessage;
import com.scrabble.engine.GameState;
import com.scrabble.engine.Player;
import com.scrabble.engine.Tile;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public final class GameEndgameService {
  private static final int MAX_CONSECUTIVE_PASSES = 4;
  private final GameMessageFactory messageFactory;

  boolean checkOutEndgame(GameSession session, Player mover, List<WsMessage> broadcast) {
    GameState state = session.state();
    if (!state.bag().isEmpty()) {
      return false;
    }
    if (mover.rack().size() != 0) {
      return false;
    }
    applyOutBonus(state, mover);
    session.setStatus("ended");
    session.setWinner(mover.name());
    session.bumpStateVersion();
    broadcast.add(messageFactory.gameEnded(mover.name(), session.stateVersion()));
    broadcast.add(messageFactory.snapshot(session));
    return true;
  }

  boolean checkPassEndgame(GameSession session, List<WsMessage> broadcast) {
    if (session.consecutivePasses() < MAX_CONSECUTIVE_PASSES) {
      return false;
    }
    applyPassPenalties(session.state());
    session.setStatus("ended");
    session.setWinner(determineWinnerByScore(session.state()));
    session.bumpStateVersion();
    broadcast.add(messageFactory.gameEnded(session.winner(), session.stateVersion()));
    broadcast.add(messageFactory.snapshot(session));
    return true;
  }

  private void applyOutBonus(GameState state, Player winner) {
    int bonus = 0;
    for (Player player : state.players()) {
      if (player == winner) {
        continue;
      }
      int penalty = rackPoints(player);
      if (penalty > 0) {
        player.addScore(-penalty);
        bonus += penalty;
      }
    }
    if (bonus > 0) {
      winner.addScore(bonus);
    }
  }

  private void applyPassPenalties(GameState state) {
    for (Player player : state.players()) {
      int penalty = rackPoints(player);
      if (penalty > 0) {
        player.addScore(-penalty);
      }
    }
  }

  private int rackPoints(Player player) {
    int total = 0;
    for (Tile tile : player.rack().tiles()) {
      total += tile.points();
    }
    return total;
  }

  private String determineWinnerByScore(GameState state) {
    String winner = null;
    int best = Integer.MIN_VALUE;
    for (Player player : state.players()) {
      if (player.score() > best) {
        best = player.score();
        winner = player.name();
      }
    }
    return winner;
  }
}
