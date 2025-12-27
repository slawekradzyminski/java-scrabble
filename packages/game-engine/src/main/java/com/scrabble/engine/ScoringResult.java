package com.scrabble.engine;

import java.util.List;

public record ScoringResult(int totalScore, List<Word> words) { }
