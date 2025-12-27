package com.scrabble.dictionary;

import java.nio.file.Path;

final class TestWordlists {
  private TestWordlists() { }

  static Path loadResourceToTempFile(String resourceName) throws java.io.IOException {
    try (java.io.InputStream input = TestWordlists.class.getResourceAsStream("/" + resourceName)) {
      if (input == null) {
        throw new IllegalStateException("Test resource not found: " + resourceName);
      }
      Path tempFile = java.nio.file.Files.createTempFile("dictionary-resource-", "-" + resourceName);
      java.nio.file.Files.copy(input, tempFile, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
      return tempFile;
    }
  }
}
