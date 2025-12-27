package com.scrabble.engine;

import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RackTest {

  @Test
  void enforcesCapacity() {
    Rack rack = new Rack();
    for (int i = 0; i < Rack.CAPACITY; i++) {
      rack.add(Tile.of('A', 1));
    }
    assertEquals(Rack.CAPACITY, rack.size());
    assertThrows(IllegalStateException.class, () -> rack.add(Tile.of('B', 3)));
  }

  @Test
  void addAllRespectsOrder() {
    Rack rack = new Rack();
    rack.addAll(List.of(Tile.of('A', 1), Tile.of('B', 3)));
    assertEquals(2, rack.size());
  }
}
