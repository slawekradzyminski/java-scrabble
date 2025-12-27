package com.scrabble.backend.game;

import com.scrabble.engine.GameState;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public final class GameSession {
  private final String roomId;
  private final GameState state;
  private final Instant createdAt;
  private final Set<String> botPlayers;
  private final Map<String, Integer> exchangesByPlayer = new HashMap<>();
  private String status;
  private String winner;
  private int consecutivePasses;

  GameSession(String roomId, GameState state, Instant createdAt, String status, Set<String> botPlayers) {
    this.roomId = roomId;
    this.state = state;
    this.createdAt = createdAt;
    this.status = status;
    this.botPlayers = Set.copyOf(botPlayers);
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

  public boolean isBot(String playerName) {
    return botPlayers.contains(playerName);
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

  public int consecutivePasses() {
    return consecutivePasses;
  }

  public void resetPasses() {
    consecutivePasses = 0;
  }

  public void incrementPasses() {
    consecutivePasses += 1;
  }

  public int exchangesUsed(String playerName) {
    return exchangesByPlayer.getOrDefault(playerName, 0);
  }

  public void incrementExchanges(String playerName) {
    exchangesByPlayer.merge(playerName, 1, Integer::sum);
  }
}
