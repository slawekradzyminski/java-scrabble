package com.scrabble.backend.game;

import java.util.Random;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GameConfig {

  @Bean
  public Random random() {
    return new Random();
  }
}
