package com.scrabble.dictionary.format;

import java.nio.file.Path;

public final class DictionaryPaths {
  private DictionaryPaths() { }

  public static Path metaPathFor(Path fstPath) {
    return fstPath.resolveSibling(fstPath.getFileName() + ".meta.json");
  }
}
