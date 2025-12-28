package com.scrabble.backend.game;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.scrabble.backend.ws.GameCommandException;
import com.scrabble.engine.BoardState;
import com.scrabble.engine.GameState;
import com.scrabble.engine.Player;
import com.scrabble.engine.TileBag;
import java.util.List;
import org.junit.jupiter.api.Test;

class GameCommandValidatorTest {

  @Test
  void rejectsUnknownPlayer() {
    // given
    GameCommandValidator validator = new GameCommandValidator();
    GameState state = new GameState(BoardState.empty(), List.of(new Player("Alice")), TileBag.standard(new java.util.Random(1)));

    // when / then
    assertThatThrownBy(() -> validator.requirePlayerIndex(state, "Bob"))
        .isInstanceOf(GameCommandException.class)
        .satisfies(error -> {
          GameCommandException exception = (GameCommandException) error;
          assertThat(exception.getPayload().get("reason")).isEqualTo(GameCommandReasons.UNKNOWN_PLAYER);
        });
  }

  @Test
  void rejectsWrongTurn() {
    // given
    GameCommandValidator validator = new GameCommandValidator();
    GameState state = new GameState(BoardState.empty(), List.of(new Player("Alice"), new Player("Bob")), TileBag.standard(new java.util.Random(1)));
    int bobIndex = 1;

    // when / then
    assertThatThrownBy(() -> validator.ensureCurrentPlayer(state, bobIndex))
        .isInstanceOf(GameCommandException.class)
        .satisfies(error -> {
          GameCommandException exception = (GameCommandException) error;
          assertThat(exception.getPayload().get("reason")).isEqualTo(GameCommandReasons.NOT_YOUR_TURN);
        });
  }
}
