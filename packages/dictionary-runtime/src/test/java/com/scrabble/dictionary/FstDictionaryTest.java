package com.scrabble.dictionary;

import com.scrabble.dictionary.compile.DictionaryCompiler;
import com.scrabble.dictionary.format.DictionaryFormat;
import com.scrabble.dictionary.format.DictionaryMeta;
import com.scrabble.dictionary.format.DictionaryMetaIO;
import com.scrabble.dictionary.format.DictionaryPaths;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
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

  @Test
  void rejectsMismatchedFormatVersion() throws Exception {
    Path tempDir = Files.createTempDirectory("fst-test-version");
    Path fstPath = tempDir.resolve("osps.fst");
    Path metaPath = DictionaryPaths.metaPathFor(fstPath);

    Path input = TestWordlists.loadResourceToTempFile("osps_shortened.txt");
    new DictionaryCompiler().compile(input, fstPath);

    DictionaryMeta meta = DictionaryMetaIO.read(metaPath);
    DictionaryMeta invalid = new DictionaryMeta(
        DictionaryFormat.FORMAT_VERSION + 1,
        meta.normalisation(),
        meta.wordCount(),
        meta.sourceSha256(),
        Instant.now());
    DictionaryMetaIO.write(metaPath, invalid);

    assertThrows(IllegalStateException.class, () -> FstDictionary.load(fstPath, metaPath));
  }

  @Test
  void rejectsMismatchedNormalisation() throws Exception {
    Path tempDir = Files.createTempDirectory("fst-test-normalisation");
    Path fstPath = tempDir.resolve("osps.fst");
    Path metaPath = DictionaryPaths.metaPathFor(fstPath);

    Path input = TestWordlists.loadResourceToTempFile("osps_shortened.txt");
    new DictionaryCompiler().compile(input, fstPath);

    DictionaryMeta meta = DictionaryMetaIO.read(metaPath);
    DictionaryMeta invalid = new DictionaryMeta(
        meta.formatVersion(),
        "NFD_LOWERCASE",
        meta.wordCount(),
        meta.sourceSha256(),
        Instant.now());
    DictionaryMetaIO.write(metaPath, invalid);

    assertThrows(IllegalStateException.class, () -> FstDictionary.load(fstPath, metaPath));
  }
}
