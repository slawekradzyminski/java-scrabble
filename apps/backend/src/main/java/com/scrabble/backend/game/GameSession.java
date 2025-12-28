package com.scrabble.backend.game;

import com.scrabble.backend.ws.WsMessage;
import com.scrabble.backend.ws.WsMessageType;
import com.scrabble.engine.GameState;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class GameSession {
  private static final int HISTORY_LIMIT = 50;
  private final String roomId;
  private final GameState state;
  private final Instant createdAt;
  private final Set<String> botPlayers;
  private final Map<String, Integer> exchangesByPlayer = new HashMap<>();
  private final Deque<Map<String, Object>> history = new ArrayDeque<>();
  private String status;
  private String winner;
  private int consecutivePasses;
  private int stateVersion;

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

  public int stateVersion() {
    return stateVersion;
  }

  public void bumpStateVersion() {
    stateVersion += 1;
  }

  public void recordHistory(WsMessage message) {
    if (message.type() == WsMessageType.STATE_SNAPSHOT) {
      return;
    }
    Map<String, Object> entry = new LinkedHashMap<>();
    entry.put("type", message.type().name());
    entry.put("payload", message.payload());
    entry.put("time", Instant.now().toString());
    history.addLast(entry);
    while (history.size() > HISTORY_LIMIT) {
      history.removeFirst();
    }
  }

  public List<Map<String, Object>> history() {
    return List.copyOf(history);
  }
}
