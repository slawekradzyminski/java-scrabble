package com.scrabble.engine.ai;

import com.scrabble.engine.MovePlacement;
import com.scrabble.engine.ScoringResult;

public record AiMove(MovePlacement placement, ScoringResult scoringResult) { }
