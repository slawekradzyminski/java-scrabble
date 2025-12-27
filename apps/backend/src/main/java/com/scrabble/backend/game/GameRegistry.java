package com.scrabble.backend.game;

import com.scrabble.engine.GameState;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class GameRegistry {
  private final Map<String, GameSession> sessions = new ConcurrentHashMap<>();

  public Optional<GameSession> find(String roomId) {
    return Optional.ofNullable(sessions.get(roomId));
  }

  public GameSession create(String roomId, GameState state, Set<String> botPlayers) {
    GameSession session = new GameSession(roomId, state, Instant.now(), "active", botPlayers);
    sessions.put(roomId, session);
    return session;
  }
}
