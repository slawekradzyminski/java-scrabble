package com.scrabble.engine;

import java.util.List;
import java.util.Objects;

public record Word(String text, List<Coordinate> coordinates) {
  public Word {
    Objects.requireNonNull(text, "text");
    Objects.requireNonNull(coordinates, "coordinates");
  }

  public int length() {
    return text.length();
  }
}
