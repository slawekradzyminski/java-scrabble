package com.scrabble.backend;

import com.scrabble.dictionary.Dictionary;
import java.util.Locale;
import java.util.Set;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

@TestConfiguration
public class TestDictionaryConfig {
  private static final Set<String> WORDS = Set.of("ZAJAWIAŁEŚ", "PÓŁROCZNIAKACH");

  @Bean
  @Primary
  public Dictionary dictionary() {
    return word -> WORDS.contains(word.toUpperCase(Locale.forLanguageTag("pl-PL")));
  }
}
