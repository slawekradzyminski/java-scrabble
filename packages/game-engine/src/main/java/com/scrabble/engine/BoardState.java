package com.scrabble.engine;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public final class BoardState {
  private final Map<Coordinate, PlacedTile> tiles;

  private BoardState(Map<Coordinate, PlacedTile> tiles) {
    this.tiles = Map.copyOf(tiles);
  }

  public static BoardState empty() {
    return new BoardState(Map.of());
  }

  public boolean isEmpty() {
    return tiles.isEmpty();
  }

  public Optional<PlacedTile> tileAt(Coordinate coordinate) {
    return Optional.ofNullable(tiles.get(coordinate));
  }

  public boolean hasTile(Coordinate coordinate) {
    return tiles.containsKey(coordinate);
  }

  public Map<Coordinate, PlacedTile> tiles() {
    return Collections.unmodifiableMap(tiles);
  }

  public BoardState withPlaced(Map<Coordinate, PlacedTile> placements) {
    Map<Coordinate, PlacedTile> next = new HashMap<>(tiles);
    for (Map.Entry<Coordinate, PlacedTile> entry : placements.entrySet()) {
      if (next.containsKey(entry.getKey())) {
        throw new IllegalArgumentException("Square already occupied: " + entry.getKey());
      }
      next.put(entry.getKey(), entry.getValue());
    }
    return new BoardState(next);
  }
}
