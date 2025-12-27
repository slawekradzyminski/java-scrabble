package com.scrabble.backend.dictionary;

import com.scrabble.dictionary.Dictionary;
import com.scrabble.dictionary.FstDictionary;
import java.io.IOException;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(DictionaryProperties.class)
public class DictionaryConfig {

  @Bean
  public Dictionary dictionary(DictionaryProperties properties) throws IOException {
    return FstDictionary.load(properties.getFstPath(), properties.getMetaPath());
  }
}
