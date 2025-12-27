package com.scrabble.engine.ai;

import static org.assertj.core.api.Assertions.assertThat;

import com.scrabble.engine.Board;
import com.scrabble.engine.BoardState;
import com.scrabble.engine.Coordinate;
import com.scrabble.engine.MovePlacement;
import com.scrabble.engine.MoveValidator;
import com.scrabble.engine.Player;
import com.scrabble.engine.ScoringResult;
import com.scrabble.engine.Scorer;
import com.scrabble.engine.LetterTile;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;

class AiMoveSimulationTest {

  @Test
  void simulatesSingleMoveOnEmptyBoard() {
    // given
    WordDictionary dictionary = new TestDictionary(Set.of("KOT"));
    BoardState board = BoardState.empty();
    Player player = new Player("Bot");
    player.rack().addAll(List.of(
        LetterTile.K.toTile(),
        LetterTile.O.toTile(),
        LetterTile.T.toTile()));
    AiMoveGenerator generator = new AiMoveGenerator();

    // when
    Optional<AiMove> move = generator.bestMove(board, player, Board.standard(), dictionary, 200);

    // then
    assertThat(move).isPresent();
    MovePlacement placement = move.get().placement();
    MoveValidator.validatePlacement(board, placement);
    ScoringResult scoring = Scorer.score(board, placement, Board.standard());
    assertThat(scoring.words()).anySatisfy(word -> assertThat(word.text()).isEqualTo("KOT"));
    assertThat(placement.placements().keySet())
        .anySatisfy(coord -> assertThat(coord).isEqualTo(new Coordinate(7, 7)));
    assertThat(placement.size()).isEqualTo(3);
  }

  private static final class TestDictionary implements WordDictionary {
    private final Set<String> words;
    private final Set<String> prefixes;

    private TestDictionary(Set<String> words) {
      this.words = Set.copyOf(words);
      this.prefixes = buildPrefixes(words);
    }

    @Override
    public boolean contains(String word) {
      return words.contains(word);
    }

    @Override
    public boolean containsPrefix(String prefix) {
      return prefixes.contains(prefix);
    }

    private static Set<String> buildPrefixes(Set<String> words) {
      Set<String> prefixSet = new HashSet<>();
      prefixSet.add("");
      for (String word : words) {
        for (int i = 1; i <= word.length(); i++) {
          prefixSet.add(word.substring(0, i));
        }
      }
      return prefixSet;
    }
  }
}
