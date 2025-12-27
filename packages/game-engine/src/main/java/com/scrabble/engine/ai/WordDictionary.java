package com.scrabble.engine.ai;

public interface WordDictionary {
  boolean contains(String word);

  default boolean containsPrefix(String prefix) {
    return true;
  }
}
