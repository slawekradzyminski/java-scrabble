package com.scrabble.backend.lobby;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class Room {
  private final String id;
  private final String name;
  private final Instant createdAt;
  private final List<String> players = new ArrayList<>();

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

  public void addPlayer(String playerName) {
    players.add(playerName);
  }
}
