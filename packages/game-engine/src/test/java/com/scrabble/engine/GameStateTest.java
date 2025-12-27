package com.scrabble.engine;

import java.util.List;
import java.util.Map;
import java.util.Random;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class GameStateTest {

  @Test
  void acceptsValidChallengeAndAppliesScore() {
    BoardState board = BoardState.empty();
    List<Player> players = List.of(new Player("A"), new Player("B"));
    TileBag bag = TileBag.standard(new Random(1));
    GameState state = new GameState(board, players, bag);

    MovePlacement move = new MovePlacement(Map.of(
        Coordinate.parse("H8"), PlacedTile.fromTile(Tile.of('A', 1)),
        Coordinate.parse("H9"), PlacedTile.fromTile(Tile.of('B', 3))));
    ScoringResult scoring = Scorer.score(board, move, Board.standard());

    state.applyPendingMove(move, scoring);
    assertNotNull(state.pendingMove());

    state.resolveChallenge(true);
    assertEquals(8, players.get(0).score());
    assertEquals(1, state.currentPlayerIndex());
  }

  @Test
  void rejectsInvalidChallengeAndSkipsScore() {
    BoardState board = BoardState.empty();
    List<Player> players = List.of(new Player("A"), new Player("B"));
    TileBag bag = TileBag.standard(new Random(1));
    GameState state = new GameState(board, players, bag);

    MovePlacement move = new MovePlacement(Map.of(
        Coordinate.parse("H8"), PlacedTile.fromTile(Tile.of('A', 1)),
        Coordinate.parse("H9"), PlacedTile.fromTile(Tile.of('B', 3))));
    ScoringResult scoring = Scorer.score(board, move, Board.standard());

    state.applyPendingMove(move, scoring);
    state.resolveChallenge(false);

    assertEquals(0, players.get(0).score());
    assertEquals(1, state.currentPlayerIndex());
    assertEquals(true, state.board().isEmpty());
  }
}
