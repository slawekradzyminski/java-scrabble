package com.scrabble.engine;

import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class EndgameScoringTest {

  @Test
  void appliesWentOutBonus() {
    Player a = new Player("A");
    Player b = new Player("B");
    a.rack().add(Tile.of('A', 1));
    b.rack().add(Tile.of('B', 3));
    b.rack().add(Tile.of('C', 2));

    EndgameScoring.applyFinalAdjustments(List.of(a, b), 0);

    assertEquals(5, a.score());
    assertEquals(-5, b.score());
  }

  @Test
  void appliesRackPenaltiesWithoutWinner() {
    Player a = new Player("A");
    Player b = new Player("B");
    a.rack().add(Tile.of('A', 1));
    b.rack().add(Tile.of('B', 3));

    EndgameScoring.applyFinalAdjustments(List.of(a, b), null);

    assertEquals(-1, a.score());
    assertEquals(-3, b.score());
  }
}
