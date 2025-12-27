package com.scrabble.backend.ws;

import java.util.Map;

public record WsMessage(String type, Map<String, Object> payload) { }
