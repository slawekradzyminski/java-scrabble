package com.scrabble.engine.ai;

import static org.assertj.core.api.Assertions.assertThat;

import com.scrabble.engine.Board;
import com.scrabble.engine.BoardState;
import com.scrabble.engine.LetterTile;
import com.scrabble.engine.MoveValidator;
import com.scrabble.engine.Player;
import java.util.Set;
import org.junit.jupiter.api.Test;

class AiMoveGeneratorTest {

  @Test
  void findsValidMoveOnEmptyBoard() {
    // given
    Player player = new Player("Bot");
    player.rack().add(LetterTile.C.toTile());
    player.rack().add(LetterTile.A.toTile());
    player.rack().add(LetterTile.T.toTile());
    WordDictionary dictionary = word -> Set.of("CAT").contains(word);
    AiMoveGenerator generator = new AiMoveGenerator();

    // when
    AiMove move = generator.bestMove(BoardState.empty(), player, Board.standard(), dictionary)
        .orElseThrow();

    // then
    MoveValidator.validatePlacement(BoardState.empty(), move.placement());
    assertThat(move.scoringResult().words().get(0).text()).isEqualTo("CAT");
  }

  @Test
  void prefersHigherScoringMove() {
    // given
    Player player = new Player("Bot");
    player.rack().add(LetterTile.Ź.toTile());
    player.rack().add(LetterTile.Ź.toTile());
    player.rack().add(LetterTile.A.toTile());
    player.rack().add(LetterTile.A.toTile());
    WordDictionary dictionary = word -> Set.of("ŹŹ", "AA").contains(word);
    AiMoveGenerator generator = new AiMoveGenerator();

    // when
    AiMove move = generator.bestMove(BoardState.empty(), player, Board.standard(), dictionary)
        .orElseThrow();

    // then
    assertThat(move.scoringResult().words())
        .anyMatch(word -> word.text().equals("ŹŹ"));
    assertThat(move.scoringResult().totalScore()).isGreaterThan(2);
  }
}
