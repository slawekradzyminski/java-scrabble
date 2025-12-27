package com.scrabble.engine;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public final class Board {
  public static final int SIZE = Coordinate.SIZE;

  private static final Board STANDARD = new Board(buildPremiums());

  private final Map<Coordinate, Premium> premiums;

  private Board(Map<Coordinate, Premium> premiums) {
    this.premiums = Map.copyOf(premiums);
  }

  public static Board standard() {
    return STANDARD;
  }

  public Optional<Premium> premiumAt(Coordinate coordinate) {
    return Optional.ofNullable(premiums.get(coordinate));
  }

  public boolean isInside(Coordinate coordinate) {
    return coordinate.rowIndex() >= 0 && coordinate.rowIndex() < SIZE
        && coordinate.colIndex() >= 0 && coordinate.colIndex() < SIZE;
  }

  private static Map<Coordinate, Premium> buildPremiums() {
    Map<Coordinate, Premium> map = new HashMap<>();

    addAll(map, Premium.TW, "A1", "A8", "A15", "H1", "H15", "O1", "O8", "O15");
    addAll(map, Premium.DW, "B2", "B14", "C3", "C13", "D4", "D12", "E5", "E11",
        "H8", "K5", "K11", "L4", "L12", "M3", "M13", "N2", "N14");
    addAll(map, Premium.TL, "B6", "B10", "F2", "F6", "F10", "F14", "J2", "J6",
        "J10", "J14", "N6", "N10");
    addAll(map, Premium.DL, "A4", "A12", "C7", "C9", "D1", "D8", "D15", "G3",
        "G7", "G9", "G13", "H4", "H12", "I3", "I7", "I9", "I13", "L1", "L8",
        "L15", "M7", "M9", "O4", "O12");

    return map;
  }

  private static void addAll(Map<Coordinate, Premium> map, Premium premium, String... coords) {
    for (String coord : coords) {
      Coordinate parsed = Coordinate.parse(coord);
      map.put(parsed, premium);
    }
  }
}
