package com.scrabble.dictionary.tools;

import com.scrabble.dictionary.compile.DictionaryCompiler;
import java.io.PrintStream;
import java.nio.file.Path;

public final class DictionaryCli {
  public static void main(String[] args) throws Exception {
    int exitCode = run(args, System.out, System.err);
    if (exitCode != 0) {
      System.exit(exitCode);
    }
  }

  static int run(String[] args, PrintStream out, PrintStream err) {
    if (args.length == 0 || "--help".equals(args[0]) || "-h".equals(args[0])) {
      printHelp(out);
      return 0;
    }

    if (!"compile".equals(args[0])) {
      err.println("Unknown command: " + args[0]);
      printHelp(out);
      return 2;
    }

    Args parsed;
    try {
      parsed = Args.parse(args);
    } catch (IllegalArgumentException e) {
      err.println(e.getMessage());
      printHelp(out);
      return 2;
    }

    DictionaryCompiler compiler = new DictionaryCompiler();
    try {
      compiler.compile(parsed.inputPath, parsed.outputPath);
      return 0;
    } catch (Exception e) {
      err.println("Compilation failed: " + e.getMessage());
      return 1;
    }
  }

  private static void printHelp(PrintStream out) {
    out.println("Usage:");
    out.println("  dictionary-cli compile --input <wordlist> --output <fst>");
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
