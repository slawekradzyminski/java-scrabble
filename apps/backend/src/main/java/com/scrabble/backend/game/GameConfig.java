package com.scrabble.backend.game;

import com.scrabble.dictionary.Dictionary;
import com.scrabble.engine.ai.WordDictionary;
import java.util.Random;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GameConfig {

  @Bean
  public Random random() {
    return new Random();
  }

  @Bean
  public WordDictionary wordDictionary(Dictionary dictionary) {
    return new WordDictionary() {
      @Override
      public boolean contains(String word) {
        return dictionary.contains(word);
      }

      @Override
      public boolean containsPrefix(String prefix) {
        return dictionary.containsPrefix(prefix);
      }
    };
  }

  @Bean
  public GameAiSettings gameAiSettings(@Value("${scrabble.ai.maxTurns:4}") int maxTurns) {
    return new GameAiSettings(maxTurns);
  }
}
