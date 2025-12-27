package com.scrabble.engine;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public final class MovePlacement {
  private final Map<Coordinate, PlacedTile> placements;

  public MovePlacement(Map<Coordinate, PlacedTile> placements) {
    if (placements == null || placements.isEmpty()) {
      throw new IllegalArgumentException("Move must place at least one tile");
    }
    this.placements = Map.copyOf(new HashMap<>(placements));
  }

  public Map<Coordinate, PlacedTile> placements() {
    return Collections.unmodifiableMap(placements);
  }

  public int size() {
    return placements.size();
  }
}
