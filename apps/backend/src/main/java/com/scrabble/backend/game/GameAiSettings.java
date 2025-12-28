package com.scrabble.backend.game;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
@EqualsAndHashCode
public class GameAiSettings {
  private final int maxTurns;

  @Builder
  public GameAiSettings(int maxTurns) {
    if (maxTurns < 1) {
      throw new IllegalArgumentException("maxTurns must be >= 1");
    }
    this.maxTurns = maxTurns;
  }
}
