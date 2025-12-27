package com.scrabble.engine;

import java.util.Objects;

public record PlacedTile(Tile tile, char assignedLetter) {
  public PlacedTile {
    Objects.requireNonNull(tile, "tile");
    if (tile.blank()) {
      if (assignedLetter == '\0') {
        throw new IllegalArgumentException("Blank tile must have an assigned letter");
      }
    } else if (tile.letter() != assignedLetter) {
      throw new IllegalArgumentException("Assigned letter must match tile letter");
    }
  }

  public static PlacedTile fromTile(Tile tile) {
    if (tile.blank()) {
      throw new IllegalArgumentException("Blank tile requires an assigned letter");
    }
    return new PlacedTile(tile, tile.letter());
  }
}
