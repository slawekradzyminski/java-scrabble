package com.scrabble.backend.game;

import java.util.List;
import java.util.Map;

public record GameEventPage(List<Map<String, Object>> events, long lastEventId) { }
