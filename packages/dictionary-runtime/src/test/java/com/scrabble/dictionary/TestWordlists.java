package com.scrabble.dictionary;

import java.nio.file.Path;

final class TestWordlists {
  private TestWordlists() { }

  static Path findRepoFile(String filename) {
    Path current = Path.of("").toAbsolutePath();
    while (current != null) {
      Path candidate = current.resolve(filename);
      if (candidate.toFile().exists()) {
        return candidate;
      }
      current = current.getParent();
    }
    throw new IllegalStateException("Unable to locate " + filename + " from " + Path.of("").toAbsolutePath());
  }
}
