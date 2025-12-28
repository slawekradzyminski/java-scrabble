package com.scrabble.backend.game;

import static org.assertj.core.api.Assertions.assertThat;

import com.scrabble.engine.BoardState;
import com.scrabble.engine.GameState;
import com.scrabble.engine.Player;
import com.scrabble.engine.TileBag;
import com.scrabble.engine.ai.WordDictionary;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class GameAiServiceTest {

  @Test
  void stopsAfterConfiguredMaxTurns() {
    // given
    WordDictionary dictionary = new WordDictionary() {
      @Override
      public boolean contains(String word) {
        return false;
      }

      @Override
      public boolean containsPrefix(String prefix) {
        return false;
      }
    };
    GameMessageFactory messageFactory = new GameMessageFactory();
    GameRackManager rackManager = new GameRackManager();
    GameEndgameService endgameService = new GameEndgameService(messageFactory);
    GameAiSettings settings = new GameAiSettings(1);
    GameAiService aiService = new GameAiService(dictionary, rackManager, endgameService, messageFactory, settings);

    Player bot = new Player("Bot");
    GameState state = new GameState(BoardState.empty(), List.of(bot), TileBag.standard(new java.util.Random(1)));
    GameSession session = new GameSession("room-1", state, java.time.Instant.now(), "active", Set.of("Bot"));

    // when
    aiService.applyAiTurns(session, new ArrayList<>());

    // then
    assertThat(session.consecutivePasses()).isEqualTo(1);
  }
}
