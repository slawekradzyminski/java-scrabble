package com.scrabble.dictionary.tools;

import com.scrabble.dictionary.FstDictionary;
import com.scrabble.dictionary.format.DictionaryPaths;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DictionaryCliTest {

  @Test
  void compilesWordlistIntoFst() throws Exception {
    // given
    Path tempDir = Files.createTempDirectory("dictionary-cli");
    Path wordlist = tempDir.resolve("wordlist.txt");
    Path fstPath = tempDir.resolve("osps.fst");

    Files.writeString(wordlist, String.join(System.lineSeparator(),
        "zajawiałeś",
        "półroczniakach"));

    // when
    int exitCode = DictionaryCli.run(new String[] {
        "compile",
        "--input", wordlist.toString(),
        "--output", fstPath.toString()
    }, new PrintStream(new ByteArrayOutputStream()), new PrintStream(new ByteArrayOutputStream()));
    assertThat(exitCode).isZero();

    FstDictionary dictionary = FstDictionary.load(fstPath, DictionaryPaths.metaPathFor(fstPath));
    // then
    assertThat(dictionary.contains("ZAJAWIAŁEŚ")).isTrue();
  }

  @Test
  void printsHelpForNoArgs() {
    // given
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    // when
    int exitCode = DictionaryCli.run(new String[0], new PrintStream(out), new PrintStream(new ByteArrayOutputStream()));
    // then
    assertThat(exitCode).isZero();
    assertThat(out.toString()).contains("Usage:");
  }

  @Test
  void rejectsUnknownCommand() {
    // given
    ByteArrayOutputStream err = new ByteArrayOutputStream();
    // when
    int exitCode = DictionaryCli.run(new String[] {"unknown"}, new PrintStream(new ByteArrayOutputStream()), new PrintStream(err));
    // then
    assertThat(exitCode).isEqualTo(2);
    assertThat(err.toString()).contains("Unknown command");
  }

  @Test
  void rejectsMissingArgs() {
    // given
    ByteArrayOutputStream err = new ByteArrayOutputStream();
    // when
    int exitCode = DictionaryCli.run(new String[] {"compile", "--input", "foo.txt"}, new PrintStream(new ByteArrayOutputStream()), new PrintStream(err));
    // then
    assertThat(exitCode).isEqualTo(2);
    assertThat(err.toString()).contains("--input and --output are required");
  }
}
