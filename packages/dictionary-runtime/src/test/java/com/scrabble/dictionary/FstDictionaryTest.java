package com.scrabble.dictionary;

import com.scrabble.dictionary.compile.DictionaryCompiler;
import com.scrabble.dictionary.format.DictionaryPaths;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FstDictionaryTest {

  @Test
  void findsWordsFromWordlist() throws Exception {
    Path tempDir = Files.createTempDirectory("fst-test");
    Path fstPath = tempDir.resolve("osps.fst");

    Path input = TestWordlists.loadResourceToTempFile("osps_shortened.txt");
    new DictionaryCompiler().compile(input, fstPath);

    FstDictionary dictionary = FstDictionary.load(fstPath, DictionaryPaths.metaPathFor(fstPath));

    assertTrue(dictionary.contains("zajawiałeś"));
    assertTrue(dictionary.contains("ZAJAWIAŁEŚ"));
    assertTrue(dictionary.contains("półroczniakach"));
  }

  @Test
  void rejectsUnknownWords() throws Exception {
    Path tempDir = Files.createTempDirectory("fst-test-miss");
    Path fstPath = tempDir.resolve("osps.fst");

    Path input = TestWordlists.loadResourceToTempFile("osps_shortened.txt");
    new DictionaryCompiler().compile(input, fstPath);

    FstDictionary dictionary = FstDictionary.load(fstPath, DictionaryPaths.metaPathFor(fstPath));

    assertFalse(dictionary.contains("nieistniejaceslowo"));
  }

  @Test
  void keepsDiacriticsDuringNormalisation() throws Exception {
    Path tempDir = Files.createTempDirectory("fst-test-dia");
    Path fstPath = tempDir.resolve("osps.fst");

    Path input = TestWordlists.loadResourceToTempFile("osps_shortened.txt");
    new DictionaryCompiler().compile(input, fstPath);

    FstDictionary dictionary = FstDictionary.load(fstPath, DictionaryPaths.metaPathFor(fstPath));

    assertTrue(dictionary.contains("PÓŁROCZNIAKACH"));
    assertFalse(dictionary.contains("POLROCZNIAKACH"));
  }
}
