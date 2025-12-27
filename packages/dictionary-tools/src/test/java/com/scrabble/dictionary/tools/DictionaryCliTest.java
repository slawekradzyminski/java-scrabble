package com.scrabble.dictionary.tools;

import com.scrabble.dictionary.FstDictionary;
import com.scrabble.dictionary.format.DictionaryPaths;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DictionaryCliTest {

  @Test
  void compilesWordlistIntoFst() throws Exception {
    Path tempDir = Files.createTempDirectory("dictionary-cli");
    Path wordlist = tempDir.resolve("wordlist.txt");
    Path fstPath = tempDir.resolve("osps.fst");

    Files.writeString(wordlist, String.join(System.lineSeparator(),
        "zajawiałeś",
        "półroczniakach"));

    int exitCode = DictionaryCli.run(new String[] {
        "compile",
        "--input", wordlist.toString(),
        "--output", fstPath.toString()
    }, new PrintStream(new ByteArrayOutputStream()), new PrintStream(new ByteArrayOutputStream()));
    assertEquals(0, exitCode);

    FstDictionary dictionary = FstDictionary.load(fstPath, DictionaryPaths.metaPathFor(fstPath));
    assertTrue(dictionary.contains("ZAJAWIAŁEŚ"));
  }

  @Test
  void printsHelpForNoArgs() {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    int exitCode = DictionaryCli.run(new String[0], new PrintStream(out), new PrintStream(new ByteArrayOutputStream()));
    assertEquals(0, exitCode);
    assertTrue(out.toString().contains("Usage:"));
  }

  @Test
  void rejectsUnknownCommand() {
    ByteArrayOutputStream err = new ByteArrayOutputStream();
    int exitCode = DictionaryCli.run(new String[] {"unknown"}, new PrintStream(new ByteArrayOutputStream()), new PrintStream(err));
    assertEquals(2, exitCode);
    assertTrue(err.toString().contains("Unknown command"));
  }

  @Test
  void rejectsMissingArgs() {
    ByteArrayOutputStream err = new ByteArrayOutputStream();
    int exitCode = DictionaryCli.run(new String[] {"compile", "--input", "foo.txt"}, new PrintStream(new ByteArrayOutputStream()), new PrintStream(err));
    assertEquals(2, exitCode);
    assertTrue(err.toString().contains("--input and --output are required"));
  }
}
