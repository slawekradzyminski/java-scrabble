package com.scrabble.engine;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TileBagTest {

  @Test
  void standardBagHasCorrectCounts() {
    // given
    TileBag bag = TileBag.standard(new Random(123));
    Map<String, Integer> counts = new HashMap<>();

    // when
    for (Tile tile : bag.draw(LetterTile.totalTiles())) {
      String key = tile.blank() ? "BLANK" : Character.toString(tile.letter());
      counts.merge(key, 1, Integer::sum);
    }

    // then
    assertThat(counts.values().stream().mapToInt(Integer::intValue).sum()).isEqualTo(100);
    assertThat(counts.getOrDefault("BLANK", 0)).isEqualTo(2);
    assertThat(counts.getOrDefault("A", 0)).isEqualTo(9);
    assertThat(counts.getOrDefault("Ą", 0)).isEqualTo(1);
    assertThat(counts.getOrDefault("Ź", 0)).isEqualTo(1);
    assertThat(counts.getOrDefault("O", 0)).isEqualTo(6);
  }

  @Test
  void drawNeverExceedsAvailableTiles() {
    // given
    TileBag bag = TileBag.standard(new Random(1));
    // when
    assertThat(bag.draw(200).size()).isEqualTo(100);
    // then
    assertThat(bag.isEmpty()).isTrue();
  }

  @Test
  void addAllRestoresTilesToBag() {
    // given
    Random random = new Random(4);
    TileBag bag = TileBag.standard(random);
    List<Tile> drawn = bag.draw(3);
    int remaining = bag.size();

    // when
    bag.addAll(drawn, random);

    // then
    assertThat(bag.size()).isEqualTo(remaining + 3);
  }
}
