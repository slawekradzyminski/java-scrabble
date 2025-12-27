package com.scrabble.backend.ws;

import java.util.Map;

public record WsMessage(WsMessageType type, Map<String, Object> payload) { }
