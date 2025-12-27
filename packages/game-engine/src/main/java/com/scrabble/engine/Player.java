package com.scrabble.engine;

import java.util.Objects;

public final class Player {
  private final String name;
  private final Rack rack;
  private int score;

  public Player(String name) {
    this.name = Objects.requireNonNull(name, "name");
    this.rack = new Rack();
  }

  public String name() {
    return name;
  }

  public Rack rack() {
    return rack;
  }

  public int score() {
    return score;
  }

  public void addScore(int delta) {
    score += delta;
  }
}
