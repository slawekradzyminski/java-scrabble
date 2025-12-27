package com.scrabble.dictionary;

import com.scrabble.dictionary.format.DictionaryFormat;
import com.scrabble.dictionary.format.DictionaryMeta;
import com.scrabble.dictionary.format.DictionaryMetaIO;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Objects;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.fst.FST.Arc;
import org.apache.lucene.util.fst.FST.BytesReader;
import org.apache.lucene.util.fst.FST;
import org.apache.lucene.util.fst.NoOutputs;
import org.apache.lucene.util.fst.Util;

public final class FstDictionary implements Dictionary {
  private final DictionaryNormalizer normalizer;
  private final FST<Object> fst;

  private FstDictionary(DictionaryNormalizer normalizer, FST<Object> fst) {
    this.normalizer = normalizer;
    this.fst = fst;
  }

  public static FstDictionary load(Path fstPath, Path metaPath) throws IOException {
    Objects.requireNonNull(fstPath, "fstPath");
    Objects.requireNonNull(metaPath, "metaPath");

    DictionaryMeta meta = DictionaryMetaIO.read(metaPath);
    validateMeta(meta);

    NoOutputs outputs = NoOutputs.getSingleton();
    FST<Object> fst = FST.read(fstPath, outputs);
    return new FstDictionary(new DictionaryNormalizer(), fst);
  }

  @Override
  public boolean contains(String word) {
    String normalized = normalizer.normalize(word);
    if (normalized.isEmpty()) {
      return false;
    }
    try {
      return Util.get(fst, new BytesRef(normalized)) != null;
    } catch (IOException e) {
      throw new IllegalStateException("Failed to read FST", e);
    }
  }

  @Override
  public boolean containsPrefix(String prefix) {
    String normalized = normalizer.normalize(prefix);
    if (normalized.isEmpty()) {
      return true;
    }
    BytesRef bytes = new BytesRef(normalized);
    Arc<Object> arc = fst.getFirstArc(new Arc<>());
    BytesReader reader = fst.getBytesReader();
    try {
      for (int i = 0; i < bytes.length; i++) {
        int label = bytes.bytes[bytes.offset + i] & 0xFF;
        if (fst.findTargetArc(label, arc, arc, reader) == null) {
          return false;
        }
      }
      return true;
    } catch (IOException e) {
      throw new IllegalStateException("Failed to read FST", e);
    }
  }

  private static void validateMeta(DictionaryMeta meta) {
    if (meta.formatVersion() != DictionaryFormat.FORMAT_VERSION) {
      throw new IllegalStateException(
          "Unsupported dictionary format: " + meta.formatVersion());
    }
    if (!DictionaryFormat.NORMALISATION.equals(meta.normalisation())) {
      throw new IllegalStateException(
          "Unsupported normalization: " + meta.normalisation());
    }
  }
}
