package com.scrabble.dictionary.tools;

import com.scrabble.dictionary.compile.DictionaryCompiler;
import java.nio.file.Path;

public final class DictionaryCli {
  public static void main(String[] args) throws Exception {
    if (args.length == 0 || "--help".equals(args[0]) || "-h".equals(args[0])) {
      printHelp();
      return;
    }

    if (!"compile".equals(args[0])) {
      System.err.println("Unknown command: " + args[0]);
      printHelp();
      System.exit(2);
    }

    Args parsed = Args.parse(args);
    DictionaryCompiler compiler = new DictionaryCompiler();
    compiler.compile(parsed.inputPath, parsed.outputPath);
  }

  private static void printHelp() {
    System.out.println("Usage:");
    System.out.println("  dictionary-cli compile --input <wordlist> --output <fst>");
  }

  private record Args(Path inputPath, Path outputPath) {
    private static Args parse(String[] args) {
      Path input = null;
      Path output = null;

      for (int i = 1; i < args.length; i++) {
        String arg = args[i];
        if ("--input".equals(arg) && i + 1 < args.length) {
          input = Path.of(args[++i]);
        } else if ("--output".equals(arg) && i + 1 < args.length) {
          output = Path.of(args[++i]);
        } else {
          throw new IllegalArgumentException("Unexpected argument: " + arg);
        }
      }

      if (input == null || output == null) {
        throw new IllegalArgumentException("--input and --output are required");
      }

      return new Args(input, output);
    }
  }
}
