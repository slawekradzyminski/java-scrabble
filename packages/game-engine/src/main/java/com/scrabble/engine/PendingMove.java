package com.scrabble.engine;

public record PendingMove(
    BoardState before,
    BoardState after,
    MovePlacement placement,
    ScoringResult scoringResult) { }
