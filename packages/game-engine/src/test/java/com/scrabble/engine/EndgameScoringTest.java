package com.scrabble.engine;

import java.util.List;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class EndgameScoringTest {

  @Test
  void appliesWentOutBonus() {
    // given
    Player a = new Player("A");
    Player b = new Player("B");
    a.rack().add(Tile.of('A', 1));
    b.rack().add(Tile.of('B', 3));
    b.rack().add(Tile.of('C', 2));

    // when
    EndgameScoring.applyFinalAdjustments(List.of(a, b), 0);

    // then
    assertThat(a.score()).isEqualTo(5);
    assertThat(b.score()).isEqualTo(-5);
  }

  @Test
  void appliesRackPenaltiesWithoutWinner() {
    // given
    Player a = new Player("A");
    Player b = new Player("B");
    a.rack().add(Tile.of('A', 1));
    b.rack().add(Tile.of('B', 3));

    // when
    EndgameScoring.applyFinalAdjustments(List.of(a, b), null);

    // then
    assertThat(a.score()).isEqualTo(-1);
    assertThat(b.score()).isEqualTo(-3);
  }
}
