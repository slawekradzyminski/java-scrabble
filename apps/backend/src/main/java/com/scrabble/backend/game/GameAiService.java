package com.scrabble.backend.game;

import com.scrabble.backend.ws.WsMessage;
import com.scrabble.engine.Board;
import com.scrabble.engine.GameState;
import com.scrabble.engine.Player;
import com.scrabble.engine.Tile;
import com.scrabble.engine.ai.AiMove;
import com.scrabble.engine.ai.AiMoveGenerator;
import com.scrabble.engine.ai.WordDictionary;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public final class GameAiService {
  private final AiMoveGenerator aiMoveGenerator = new AiMoveGenerator();
  private final WordDictionary wordDictionary;
  private final GameRackManager rackManager;
  private final GameEndgameService endgameService;
  private final GameMessageFactory messageFactory;
  private final GameAiSettings settings;

  void applyAiTurns(GameSession session, List<WsMessage> broadcast) {
    int safety = 0;
    while ("active".equals(session.status()) && session.state().pendingMove() == null) {
      GameState state = session.state();
      String current = state.players().get(state.currentPlayerIndex()).name();
      if (!session.isBot(current)) {
        return;
      }
      if (safety++ >= settings.getMaxTurns()) {
        return;
      }

      Player bot = state.players().get(state.currentPlayerIndex());
      Optional<AiMove> move = aiMoveGenerator.bestMove(state.board(), bot, Board.standard(), wordDictionary);
      if (move.isEmpty()) {
        state.advanceTurn();
        session.incrementPasses();
        session.bumpStateVersion();
        broadcast.add(messageFactory.turnAdvanced(state, session.stateVersion()));
        broadcast.add(messageFactory.snapshot(session));
        if (endgameService.checkPassEndgame(session, broadcast)) {
          return;
        }
        continue;
      }

      AiMove aiMove = move.get();
      List<Tile> removed = new ArrayList<>();
      try {
        rackManager.takePlacedTilesFromRack(bot.rack(), aiMove.placement().placements().values(), removed);
        state.applyPendingMove(aiMove.placement(), aiMove.scoringResult());
        state.resolveChallenge(true);
        rackManager.refillRack(bot.rack(), state.bag());
        session.resetPasses();
        session.bumpStateVersion();
        broadcast.add(messageFactory.moveAccepted(
            bot, aiMove.scoringResult(), aiMove.placement().placements(), session.stateVersion()));
        broadcast.add(messageFactory.turnAdvanced(state, session.stateVersion()));
        broadcast.add(messageFactory.snapshot(session));
        if (endgameService.checkOutEndgame(session, bot, broadcast)) {
          return;
        }
      } catch (RuntimeException e) {
        rackManager.restoreTiles(bot.rack(), removed);
        state.advanceTurn();
        session.incrementPasses();
        session.bumpStateVersion();
        broadcast.add(messageFactory.turnAdvanced(state, session.stateVersion()));
        broadcast.add(messageFactory.snapshot(session));
        if (endgameService.checkPassEndgame(session, broadcast)) {
          return;
        }
      }
    }
  }
}
