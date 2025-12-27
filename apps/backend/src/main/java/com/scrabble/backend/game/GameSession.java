package com.scrabble.backend.game;

import com.scrabble.engine.GameState;
import java.time.Instant;

public final class GameSession {
  private final String roomId;
  private final GameState state;
  private final Instant createdAt;
  private String status;
  private String winner;

  GameSession(String roomId, GameState state, Instant createdAt, String status) {
    this.roomId = roomId;
    this.state = state;
    this.createdAt = createdAt;
    this.status = status;
  }

  public String roomId() {
    return roomId;
  }

  public GameState state() {
    return state;
  }

  public Instant createdAt() {
    return createdAt;
  }

  public String status() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public String winner() {
    return winner;
  }

  public void setWinner(String winner) {
    this.winner = winner;
  }
}
