package com.scrabble.dictionary.compile;

import com.scrabble.dictionary.DictionaryNormalizer;
import com.scrabble.dictionary.format.DictionaryFormat;
import com.scrabble.dictionary.format.DictionaryMeta;
import com.scrabble.dictionary.format.DictionaryMetaIO;
import com.scrabble.dictionary.format.DictionaryPaths;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Set;
import java.util.TreeSet;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.IntsRefBuilder;
import org.apache.lucene.util.fst.FSTCompiler;
import org.apache.lucene.util.fst.FST;
import org.apache.lucene.util.fst.NoOutputs;
import org.apache.lucene.util.fst.Util;

public final class DictionaryCompiler {
  private static final HexFormat HEX = HexFormat.of();

  private final DictionaryNormalizer normalizer = new DictionaryNormalizer();

  public void compile(Path inputPath, Path fstOutputPath) throws IOException {
    Set<String> words = readNormalizedWords(inputPath);
    if (words.isEmpty()) {
      throw new IllegalArgumentException("No words found in input: " + inputPath);
    }

    if (fstOutputPath.getParent() != null) {
      Files.createDirectories(fstOutputPath.getParent());
    }

    writeFst(words, fstOutputPath);
    DictionaryMeta meta = new DictionaryMeta(
        DictionaryFormat.FORMAT_VERSION,
        DictionaryFormat.NORMALISATION,
        words.size(),
        sha256(inputPath),
        Instant.now());
    DictionaryMetaIO.write(DictionaryPaths.metaPathFor(fstOutputPath), meta);
  }

  private Set<String> readNormalizedWords(Path inputPath) throws IOException {
    Set<String> words = new TreeSet<>();
    try (BufferedReader reader = Files.newBufferedReader(inputPath, StandardCharsets.UTF_8)) {
      String line;
      while ((line = reader.readLine()) != null) {
        String normalized = normalizer.normalize(line);
        if (!normalized.isEmpty()) {
          words.add(normalized);
        }
      }
    }
    return words;
  }

  private void writeFst(Set<String> words, Path fstOutputPath) throws IOException {
    NoOutputs outputs = NoOutputs.getSingleton();
    FSTCompiler<Object> compiler =
        new FSTCompiler.Builder<>(FST.INPUT_TYPE.BYTE1, outputs).build();
    IntsRefBuilder scratch = new IntsRefBuilder();

    for (String word : words) {
      compiler.add(Util.toIntsRef(new BytesRef(word), scratch), outputs.getNoOutput());
    }

    FST.FSTMetadata<Object> metadata = compiler.compile();
    FST<Object> fst = FST.fromFSTReader(metadata, compiler.getFSTReader());
    fst.save(fstOutputPath);
  }

  private String sha256(Path inputPath) throws IOException {
    MessageDigest digest;
    try {
      digest = MessageDigest.getInstance("SHA-256");
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException("SHA-256 not available", e);
    }

    try (InputStream input = Files.newInputStream(inputPath);
        DigestInputStream digestStream = new DigestInputStream(input, digest)) {
      byte[] buffer = new byte[8192];
      while (digestStream.read(buffer) != -1) {
        // digest updates via stream
      }
    }

    return HEX.formatHex(digest.digest());
  }
}
