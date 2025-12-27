package com.scrabble.engine;

import java.util.List;

public final class EndgameScoring {
  private EndgameScoring() { }

  public static void applyFinalAdjustments(List<Player> players, Integer wentOutIndex) {
    int totalRackSum = 0;
    int[] rackSums = new int[players.size()];

    for (int i = 0; i < players.size(); i++) {
      int sum = players.get(i).rack().tiles().stream().mapToInt(Tile::points).sum();
      rackSums[i] = sum;
      totalRackSum += sum;
    }

    for (int i = 0; i < players.size(); i++) {
      players.get(i).addScore(-rackSums[i]);
    }

    if (wentOutIndex != null) {
      players.get(wentOutIndex).addScore(totalRackSum);
    }
  }
}
