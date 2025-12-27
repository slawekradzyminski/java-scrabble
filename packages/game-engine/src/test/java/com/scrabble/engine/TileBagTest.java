package com.scrabble.engine;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TileBagTest {

  @Test
  void standardBagHasCorrectCounts() {
    TileBag bag = TileBag.standard(new Random(123));
    Map<String, Integer> counts = new HashMap<>();

    for (Tile tile : bag.draw(LetterTile.totalTiles())) {
      String key = tile.blank() ? "BLANK" : Character.toString(tile.letter());
      counts.merge(key, 1, Integer::sum);
    }

    assertEquals(100, counts.values().stream().mapToInt(Integer::intValue).sum());
    assertEquals(2, counts.getOrDefault("BLANK", 0));
    assertEquals(9, counts.getOrDefault("A", 0));
    assertEquals(1, counts.getOrDefault("Ą", 0));
    assertEquals(1, counts.getOrDefault("Ź", 0));
    assertEquals(6, counts.getOrDefault("O", 0));
  }

  @Test
  void drawNeverExceedsAvailableTiles() {
    TileBag bag = TileBag.standard(new Random(1));
    assertEquals(100, bag.draw(200).size());
    assertTrue(bag.isEmpty());
  }

  @Test
  void addAllRestoresTilesToBag() {
    Random random = new Random(4);
    TileBag bag = TileBag.standard(random);
    List<Tile> drawn = bag.draw(3);
    int remaining = bag.size();

    bag.addAll(drawn, random);

    assertEquals(remaining + 3, bag.size());
  }
}
