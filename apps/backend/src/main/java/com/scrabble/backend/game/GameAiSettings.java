package com.scrabble.backend.game;

public record GameAiSettings(int maxTurns) {
  public GameAiSettings {
    if (maxTurns < 1) {
      throw new IllegalArgumentException("maxTurns must be >= 1");
    }
  }
}
