package com.scrabble.engine;

import java.util.List;
import java.util.Objects;

public final class GameState {
  private BoardState board;
  private final List<Player> players;
  private final TileBag bag;
  private int currentPlayerIndex;
  private PendingMove pendingMove;

  public GameState(BoardState board, List<Player> players, TileBag bag) {
    this.board = Objects.requireNonNull(board, "board");
    this.players = List.copyOf(players);
    this.bag = Objects.requireNonNull(bag, "bag");
    this.currentPlayerIndex = 0;
  }

  public BoardState board() {
    return board;
  }

  public List<Player> players() {
    return players;
  }

  public TileBag bag() {
    return bag;
  }

  public int currentPlayerIndex() {
    return currentPlayerIndex;
  }

  public PendingMove pendingMove() {
    return pendingMove;
  }

  public void applyPendingMove(MovePlacement placement, ScoringResult scoringResult) {
    if (pendingMove != null) {
      throw new IllegalStateException("Pending move already exists");
    }
    BoardState after = board.withPlaced(placement.placements());
    pendingMove = new PendingMove(board, after, placement, scoringResult);
  }

  public void resolveChallenge(boolean valid) {
    if (pendingMove == null) {
      throw new IllegalStateException("No pending move to resolve");
    }
    if (valid) {
      board = pendingMove.after();
      players.get(currentPlayerIndex).addScore(pendingMove.scoringResult().totalScore());
    }
    pendingMove = null;
    advanceTurn();
  }

  public void advanceTurn() {
    currentPlayerIndex = (currentPlayerIndex + 1) % players.size();
  }
}
