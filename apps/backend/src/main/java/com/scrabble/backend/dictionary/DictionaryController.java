package com.scrabble.backend.dictionary;

import com.scrabble.dictionary.Dictionary;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dictionary")
public class DictionaryController {
  private final Dictionary dictionary;

  public DictionaryController(Dictionary dictionary) {
    this.dictionary = dictionary;
  }

  @GetMapping("/contains")
  public DictionaryResponse contains(@RequestParam("word") String word) {
    return new DictionaryResponse(word, dictionary.contains(word));
  }

  public record DictionaryResponse(String word, boolean contains) { }
}
