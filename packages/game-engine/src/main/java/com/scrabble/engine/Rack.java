package com.scrabble.engine;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class Rack {
  public static final int CAPACITY = 7;

  private final List<Tile> tiles = new ArrayList<>();

  public List<Tile> tiles() {
    return Collections.unmodifiableList(tiles);
  }

  public int size() {
    return tiles.size();
  }

  public int remainingCapacity() {
    return CAPACITY - tiles.size();
  }

  public void add(Tile tile) {
    if (tiles.size() >= CAPACITY) {
      throw new IllegalStateException("Rack is full");
    }
    tiles.add(tile);
  }

  public void addAll(List<Tile> tilesToAdd) {
    for (Tile tile : tilesToAdd) {
      add(tile);
    }
  }

  public boolean remove(Tile tile) {
    return tiles.remove(tile);
  }

  public void clear() {
    tiles.clear();
  }
}
