package com.scrabble.engine;

import java.util.List;
import java.util.Map;
import java.util.Random;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class GameStateTest {

  @Test
  void acceptsValidChallengeAndAppliesScore() {
    // given
    BoardState board = BoardState.empty();
    List<Player> players = List.of(new Player("A"), new Player("B"));
    TileBag bag = TileBag.standard(new Random(1));
    GameState state = new GameState(board, players, bag);

    MovePlacement move = new MovePlacement(Map.of(
        Coordinate.parse("H8"), PlacedTile.fromTile(Tile.of('A', 1)),
        Coordinate.parse("H9"), PlacedTile.fromTile(Tile.of('B', 3))));
    ScoringResult scoring = Scorer.score(board, move, Board.standard());

    // when
    state.applyPendingMove(move, scoring);
    assertThat(state.pendingMove()).isNotNull();

    state.resolveChallenge(true);
    // then
    assertThat(players.get(0).score()).isEqualTo(8);
    assertThat(state.currentPlayerIndex()).isEqualTo(1);
  }

  @Test
  void rejectsInvalidChallengeAndSkipsScore() {
    // given
    BoardState board = BoardState.empty();
    List<Player> players = List.of(new Player("A"), new Player("B"));
    TileBag bag = TileBag.standard(new Random(1));
    GameState state = new GameState(board, players, bag);

    MovePlacement move = new MovePlacement(Map.of(
        Coordinate.parse("H8"), PlacedTile.fromTile(Tile.of('A', 1)),
        Coordinate.parse("H9"), PlacedTile.fromTile(Tile.of('B', 3))));
    ScoringResult scoring = Scorer.score(board, move, Board.standard());

    // when
    state.applyPendingMove(move, scoring);
    state.resolveChallenge(false);

    // then
    assertThat(players.get(0).score()).isEqualTo(0);
    assertThat(state.currentPlayerIndex()).isEqualTo(1);
    assertThat(state.board().isEmpty()).isTrue();
  }
}
