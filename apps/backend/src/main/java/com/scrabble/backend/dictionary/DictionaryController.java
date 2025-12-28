package com.scrabble.backend.dictionary;

import com.scrabble.dictionary.Dictionary;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dictionary")
@RequiredArgsConstructor
public class DictionaryController {
  private final Dictionary dictionary;

  @GetMapping("/contains")
  public DictionaryResponse contains(@RequestParam("word") String word) {
    return new DictionaryResponse(word, dictionary.contains(word));
  }

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  public static class DictionaryResponse {
    private String word;
    private boolean contains;
  }
}
