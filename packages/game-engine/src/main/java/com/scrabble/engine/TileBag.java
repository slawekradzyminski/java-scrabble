package com.scrabble.engine;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public final class TileBag {
  private final List<Tile> tiles;

  private TileBag(List<Tile> tiles) {
    this.tiles = tiles;
  }

  public static TileBag standard(Random random) {
    List<Tile> tiles = new ArrayList<>(LetterTile.totalTiles());
    for (LetterTile letter : LetterTile.values()) {
      for (int i = 0; i < letter.count(); i++) {
        tiles.add(letter.toTile());
      }
    }
    Collections.shuffle(tiles, random);
    return new TileBag(tiles);
  }

  public int size() {
    return tiles.size();
  }

  public boolean isEmpty() {
    return tiles.isEmpty();
  }

  public List<Tile> draw(int count) {
    if (count <= 0) {
      return List.of();
    }
    int actual = Math.min(count, tiles.size());
    List<Tile> drawn = new ArrayList<>(actual);
    for (int i = 0; i < actual; i++) {
      drawn.add(tiles.remove(tiles.size() - 1));
    }
    return drawn;
  }

  public void addAll(List<Tile> tilesToAdd, Random random) {
    if (tilesToAdd == null || tilesToAdd.isEmpty()) {
      return;
    }
    tiles.addAll(tilesToAdd);
    Collections.shuffle(tiles, random);
  }
}
