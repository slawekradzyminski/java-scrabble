package com.scrabble.dictionary;

import java.text.Normalizer;
import java.util.Locale;

public final class DictionaryNormalizer {
  public static final String POLICY = "NFC_UPPERCASE_PL_TRIM";
  private static final Locale POLISH = Locale.forLanguageTag("pl-PL");

  public String normalize(String word) {
    if (word == null) {
      return "";
    }
    String trimmed = word.trim();
    if (trimmed.isEmpty()) {
      return "";
    }
    String nfc = Normalizer.normalize(trimmed, Normalizer.Form.NFC);
    return nfc.toUpperCase(POLISH);
  }
}
