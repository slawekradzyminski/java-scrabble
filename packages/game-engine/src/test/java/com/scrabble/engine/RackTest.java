package com.scrabble.engine;

import java.util.List;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RackTest {

  @Test
  void enforcesCapacity() {
    // given
    Rack rack = new Rack();
    for (int i = 0; i < Rack.CAPACITY; i++) {
      rack.add(Tile.of('A', 1));
    }
    // when
    assertThat(rack.size()).isEqualTo(Rack.CAPACITY);
    // then
    assertThatThrownBy(() -> rack.add(Tile.of('B', 3)))
        .isInstanceOf(IllegalStateException.class);
  }

  @Test
  void addAllRespectsOrder() {
    // given
    Rack rack = new Rack();
    // when
    rack.addAll(List.of(Tile.of('A', 1), Tile.of('B', 3)));
    // then
    assertThat(rack.size()).isEqualTo(2);
  }
}
