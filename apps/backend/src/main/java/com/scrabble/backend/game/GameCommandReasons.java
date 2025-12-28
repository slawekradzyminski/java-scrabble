package com.scrabble.backend.game;

final class GameCommandReasons {
  static final String GAME_NOT_STARTED = "game_not_started";
  static final String GAME_ENDED = "game_ended";
  static final String PENDING_MOVE = "pending_move";
  static final String UNKNOWN_PLAYER = "unknown_player";
  static final String NOT_YOUR_TURN = "not_your_turn";
  static final String INVALID_MOVE = "invalid_move";
  static final String INVALID_WORDS = "invalid_words";
  static final String TILE_NOT_IN_RACK = "tile_not_in_rack";
  static final String EMPTY_EXCHANGE = "empty_exchange";
  static final String EXCHANGE_LIMIT_REACHED = "exchange_limit_reached";
  static final String BAG_TOO_SMALL = "bag_too_small";
  static final String EXCHANGE_FAILED = "exchange_failed";

  private GameCommandReasons() { }
}
