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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FstDictionaryTest {

  @Test
  void findsWordsFromWordlist() throws Exception {
    // given
    Path tempDir = Files.createTempDirectory("fst-test");
    Path fstPath = tempDir.resolve("osps.fst");

    Path input = TestWordlists.loadResourceToTempFile("osps_shortened.txt");
    new DictionaryCompiler().compile(input, fstPath);

    FstDictionary dictionary = FstDictionary.load(fstPath, DictionaryPaths.metaPathFor(fstPath));

    // when
    assertThat(dictionary.contains("zajawiałeś")).isTrue();
    assertThat(dictionary.contains("ZAJAWIAŁEŚ")).isTrue();

    // then
    assertThat(dictionary.contains("półroczniakach")).isTrue();
  }

  @Test
  void rejectsUnknownWords() throws Exception {
    // given
    Path tempDir = Files.createTempDirectory("fst-test-miss");
    Path fstPath = tempDir.resolve("osps.fst");

    Path input = TestWordlists.loadResourceToTempFile("osps_shortened.txt");
    new DictionaryCompiler().compile(input, fstPath);

    // when
    FstDictionary dictionary = FstDictionary.load(fstPath, DictionaryPaths.metaPathFor(fstPath));

    // then
    assertThat(dictionary.contains("nieistniejaceslowo")).isFalse();
  }

  @Test
  void keepsDiacriticsDuringNormalisation() throws Exception {
    // given
    Path tempDir = Files.createTempDirectory("fst-test-dia");
    Path fstPath = tempDir.resolve("osps.fst");

    Path input = TestWordlists.loadResourceToTempFile("osps_shortened.txt");
    new DictionaryCompiler().compile(input, fstPath);

    // when
    FstDictionary dictionary = FstDictionary.load(fstPath, DictionaryPaths.metaPathFor(fstPath));

    // then
    assertThat(dictionary.contains("PÓŁROCZNIAKACH")).isTrue();
    assertThat(dictionary.contains("POLROCZNIAKACH")).isFalse();
  }

  @Test
  void rejectsMismatchedFormatVersion() throws Exception {
    // given
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

    // when + then
    assertThatThrownBy(() -> FstDictionary.load(fstPath, metaPath))
        .isInstanceOf(IllegalStateException.class);
  }

  @Test
  void rejectsMismatchedNormalisation() throws Exception {
    // given
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

    // when + then
    assertThatThrownBy(() -> FstDictionary.load(fstPath, metaPath))
        .isInstanceOf(IllegalStateException.class);
  }
}
