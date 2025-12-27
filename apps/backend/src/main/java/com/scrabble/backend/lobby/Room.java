package com.scrabble.backend.lobby;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.LinkedHashSet;

public final class Room {
  private final String id;
  private final String name;
  private final Instant createdAt;
  private final List<String> players = new ArrayList<>();
  private final Set<String> botPlayers = new LinkedHashSet<>();

  Room(String id, String name, Instant createdAt) {
    this.id = id;
    this.name = name;
    this.createdAt = createdAt;
  }

  public String id() {
    return id;
  }

  public String name() {
    return name;
  }

  public Instant createdAt() {
    return createdAt;
  }

  public List<String> players() {
    return Collections.unmodifiableList(players);
  }

  public Set<String> botPlayers() {
    return Collections.unmodifiableSet(botPlayers);
  }

  public void addPlayer(String playerName, boolean bot) {
    if (players.contains(playerName)) {
      return;
    }
    players.add(playerName);
    if (bot) {
      botPlayers.add(playerName);
    }
  }

  public void addPlayer(String playerName) {
    addPlayer(playerName, false);
  }
}
