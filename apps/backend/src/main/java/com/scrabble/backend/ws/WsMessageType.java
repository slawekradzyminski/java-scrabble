package com.scrabble.backend.ws;

public enum WsMessageType {
  STATE_SNAPSHOT,
  MOVE_PROPOSED,
  MOVE_ACCEPTED,
  MOVE_REJECTED,
  TURN_ADVANCED,
  GAME_ENDED,
  ERROR,
  PONG
}
