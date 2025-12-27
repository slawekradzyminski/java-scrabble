package com.scrabble.dictionary;

import com.scrabble.dictionary.compile.DictionaryCompiler;
import com.scrabble.dictionary.format.DictionaryPaths;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.concurrent.TimeUnit;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;

@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
public class FstDictionaryBenchmark {

  @State(Scope.Benchmark)
  public static class DictionaryState {
    private FstDictionary dictionary;

    @Setup
    public void setUp() throws IOException {
      String fstPathProperty = System.getProperty("dictionary.fstPath");
      if (fstPathProperty != null && !fstPathProperty.isBlank()) {
        Path fstPath = Path.of(fstPathProperty);
        dictionary = FstDictionary.load(fstPath, DictionaryPaths.metaPathFor(fstPath));
        return;
      }

      Path tempDir = Files.createTempDirectory("fst-bench");
      Path fstPath = tempDir.resolve("osps.fst");

      String wordlistProperty = System.getProperty("dictionary.wordlistPath");
      Path input = (wordlistProperty == null || wordlistProperty.isBlank())
          ? loadResourceToTempFile("osps_shortened.txt")
          : Path.of(wordlistProperty);
      new DictionaryCompiler().compile(input, fstPath);

      dictionary = FstDictionary.load(fstPath, DictionaryPaths.metaPathFor(fstPath));
    }

    private static Path loadResourceToTempFile(String resourceName) throws IOException {
      try (InputStream input = DictionaryState.class.getResourceAsStream("/" + resourceName)) {
        if (input == null) {
          throw new IllegalStateException("Benchmark resource not found: " + resourceName);
        }
        Path tempFile = Files.createTempFile("dictionary-resource-", "-" + resourceName);
        Files.copy(input, tempFile, StandardCopyOption.REPLACE_EXISTING);
        return tempFile;
      }
    }
  }

  @Benchmark
  public boolean containsKnownWord(DictionaryState state) {
    return state.dictionary.contains("ZAJAWIAŁEŚ");
  }

  @Benchmark
  public boolean containsUnknownWord(DictionaryState state) {
    return state.dictionary.contains("NIEISTNIEJACE");
  }
}
