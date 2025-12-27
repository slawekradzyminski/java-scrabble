package com.scrabble.backend.game;

import com.scrabble.backend.lobby.Room;
import com.scrabble.backend.lobby.RoomService;
import com.scrabble.backend.ws.GameCommandException;
import com.scrabble.backend.ws.GameCommandResult;
import com.scrabble.backend.ws.WsMessage;
import com.scrabble.backend.ws.WsMessageType;
import com.scrabble.dictionary.Dictionary;
import com.scrabble.engine.Board;
import com.scrabble.engine.BoardState;
import com.scrabble.engine.Coordinate;
import com.scrabble.engine.GameState;
import com.scrabble.engine.MovePlacement;
import com.scrabble.engine.MoveValidator;
import com.scrabble.engine.PendingMove;
import com.scrabble.engine.PlacedTile;
import com.scrabble.engine.Player;
import com.scrabble.engine.Rack;
import com.scrabble.engine.ScoringResult;
import com.scrabble.engine.Scorer;
import com.scrabble.engine.Tile;
import com.scrabble.engine.TileBag;
import com.scrabble.engine.Word;
import com.scrabble.engine.ai.AiMove;
import com.scrabble.engine.ai.AiMoveGenerator;
import com.scrabble.engine.ai.WordDictionary;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class GameService {
  private static final int MAX_CONSECUTIVE_PASSES = 4;
  private static final int MAX_EXCHANGES_PER_PLAYER = 3;
  private static final int MIN_EXCHANGE_BAG_SIZE = 7;
  private final RoomService roomService;
  private final Dictionary dictionary;
  private final Random random;
  private final WordDictionary wordDictionary;
  private final AiMoveGenerator aiMoveGenerator = new AiMoveGenerator();
  private final GameRegistry registry = new GameRegistry();

  public GameService(RoomService roomService, Dictionary dictionary, Random random) {
    this.roomService = roomService;
    this.dictionary = dictionary;
    this.random = random;
    this.wordDictionary = new WordDictionary() {
      @Override
      public boolean contains(String word) {
        return dictionary.contains(word);
      }

      @Override
      public boolean containsPrefix(String prefix) {
        return dictionary.containsPrefix(prefix);
      }
    };
  }

  public GameSession start(String roomId) {
    return registry.find(roomId).orElseGet(() -> {
      Room room = roomService.find(roomId)
          .orElseThrow(() -> new IllegalArgumentException("Room not found: " + roomId));

      List<Player> players = room.players().stream()
          .map(Player::new)
          .collect(Collectors.toList());

      TileBag bag = TileBag.standard(random);
      for (Player player : players) {
        player.rack().addAll(bag.draw(Rack.CAPACITY));
      }

      GameState state = new GameState(BoardState.empty(), players, bag);
      GameSession session = registry.create(roomId, state, room.botPlayers());
      applyAiTurns(session, new ArrayList<>());
      return session;
    });
  }

  public GameSnapshot snapshot(String roomId) {
    return snapshotForPlayer(roomId, null);
  }

  public GameSnapshot snapshotForPlayer(String roomId, String player) {
    return registry.find(roomId)
        .map(session -> GameSnapshot.from(session, session.status(), player))
        .orElseGet(() -> GameSnapshot.missing(roomId));
  }

  public GameCommandResult playTiles(String roomId, String playerName, Map<Coordinate, PlacedTile> placements) {
    GameSession session = requireSession(roomId);
    synchronized (session) {
      ensureActive(session);
      GameState state = session.state();
      ensureNoPendingMove(state);
      int playerIndex = requirePlayerIndex(state, playerName);
      ensureCurrentPlayer(state, playerIndex);

      Player player = state.players().get(playerIndex);
      List<Tile> removed = new ArrayList<>();
      try {
        takePlacedTilesFromRack(player.rack(), placements.values(), removed);
        MovePlacement move = new MovePlacement(placements);
        MoveValidator.validatePlacement(state.board(), move);
        ScoringResult scoring = Scorer.score(state.board(), move, Board.standard());
        state.applyPendingMove(move, scoring);
        WsMessage proposed = new WsMessage(WsMessageType.MOVE_PROPOSED, Map.of(
            "player", player.name(),
            "score", scoring.totalScore(),
            "words", wordsToPayload(scoring.words()),
            "placements", placementsToPayload(placements)));
        WsMessage snapshot = new WsMessage(WsMessageType.STATE_SNAPSHOT,
            GameSnapshot.from(session, session.status()).toPayload());
        GameCommandResult base = GameCommandResult.broadcastOnly(proposed, snapshot);
        return withAiTurns(session, base);
      } catch (RuntimeException e) {
        restoreTiles(player.rack(), removed);
        if (e instanceof GameCommandException) {
          throw e;
        }
        throw rejected("invalid_move");
      }
    }
  }

  public GameCommandResult challenge(String roomId, String playerName) {
    GameSession session = requireSession(roomId);
    synchronized (session) {
      ensureActive(session);
      GameState state = session.state();
      requirePlayerIndex(state, playerName);
      PendingMove pending = state.pendingMove();
      if (pending == null) {
        throw rejected("no_pending_move");
      }
      int moverIndex = state.currentPlayerIndex();
      Player mover = state.players().get(moverIndex);

      List<String> invalidWords = pending.scoringResult().words().stream()
          .map(Word::text)
          .filter(word -> !dictionary.contains(word))
          .collect(Collectors.toList());

      boolean valid = invalidWords.isEmpty();
      if (!valid) {
        restoreTiles(mover.rack(), pending.placement().placements().values().stream()
            .map(PlacedTile::tile)
            .collect(Collectors.toList()));
      }

      state.resolveChallenge(valid);
      if (valid) {
        refillRack(mover.rack(), state.bag());
        session.resetPasses();
      }

      WsMessage result = valid
          ? new WsMessage(WsMessageType.MOVE_ACCEPTED, Map.of(
              "player", mover.name(),
              "score", pending.scoringResult().totalScore(),
              "words", wordsToPayload(pending.scoringResult().words())))
          : new WsMessage(WsMessageType.MOVE_REJECTED, Map.of(
              "player", mover.name(),
              "reason", "invalid_words",
              "invalidWords", invalidWords));

      WsMessage turn = new WsMessage(WsMessageType.TURN_ADVANCED, currentTurnPayload(state));
      WsMessage snapshot = new WsMessage(WsMessageType.STATE_SNAPSHOT,
          GameSnapshot.from(session, session.status()).toPayload());
      List<WsMessage> broadcast = new ArrayList<>(List.of(result, turn, snapshot));
      if (valid) {
        checkOutEndgame(session, mover, broadcast);
      }
      GameCommandResult base = new GameCommandResult(broadcast, List.of());
      return withAiTurns(session, base);
    }
  }

  public GameCommandResult pass(String roomId, String playerName) {
    GameSession session = requireSession(roomId);
    synchronized (session) {
      ensureActive(session);
      GameState state = session.state();
      ensureNoPendingMove(state);
      int playerIndex = requirePlayerIndex(state, playerName);
      ensureCurrentPlayer(state, playerIndex);
      state.advanceTurn();
      session.incrementPasses();
      WsMessage turn = new WsMessage(WsMessageType.TURN_ADVANCED, currentTurnPayload(state));
      WsMessage snapshot = new WsMessage(WsMessageType.STATE_SNAPSHOT,
          GameSnapshot.from(session, session.status()).toPayload());
      List<WsMessage> broadcast = new ArrayList<>(List.of(turn, snapshot));
      checkPassEndgame(session, broadcast);
      GameCommandResult base = new GameCommandResult(broadcast, List.of());
      return withAiTurns(session, base);
    }
  }

  public GameCommandResult exchange(String roomId, String playerName, List<Tile> tiles) {
    GameSession session = requireSession(roomId);
    synchronized (session) {
      ensureActive(session);
      GameState state = session.state();
      ensureNoPendingMove(state);
      int playerIndex = requirePlayerIndex(state, playerName);
      ensureCurrentPlayer(state, playerIndex);
      if (tiles.isEmpty()) {
        throw rejected("empty_exchange");
      }
      if (session.exchangesUsed(playerName) >= MAX_EXCHANGES_PER_PLAYER) {
        throw rejected("exchange_limit_reached");
      }
      if (state.bag().size() < MIN_EXCHANGE_BAG_SIZE) {
        throw rejected("bag_too_small");
      }
      if (state.bag().size() < tiles.size()) {
        throw rejected("bag_too_small");
      }
      Player player = state.players().get(playerIndex);
      List<Tile> removed = new ArrayList<>();
      try {
        takeTilesFromRack(player.rack(), tiles, removed);
        state.bag().addAll(removed, random);
        List<Tile> drawn = state.bag().draw(tiles.size());
        player.rack().addAll(drawn);
        session.incrementExchanges(playerName);
        state.advanceTurn();
        session.incrementPasses();
        WsMessage turn = new WsMessage(WsMessageType.TURN_ADVANCED, currentTurnPayload(state));
        WsMessage snapshot = new WsMessage(WsMessageType.STATE_SNAPSHOT,
            GameSnapshot.from(session, session.status()).toPayload());
        List<WsMessage> broadcast = new ArrayList<>(List.of(turn, snapshot));
        checkPassEndgame(session, broadcast);
        GameCommandResult base = new GameCommandResult(broadcast, List.of());
        return withAiTurns(session, base);
      } catch (RuntimeException e) {
        restoreTiles(player.rack(), removed);
        if (e instanceof GameCommandException) {
          throw e;
        }
        throw rejected("exchange_failed");
      }
    }
  }

  public GameCommandResult resign(String roomId, String playerName) {
    GameSession session = requireSession(roomId);
    synchronized (session) {
      ensureActive(session);
      GameState state = session.state();
      int playerIndex = requirePlayerIndex(state, playerName);
      session.setStatus("ended");
      session.setWinner(determineWinner(state, playerIndex));
      WsMessage ended = new WsMessage(WsMessageType.GAME_ENDED, Map.of(
          "winner", session.winner(),
          "resigned", playerName));
      WsMessage snapshot = new WsMessage(WsMessageType.STATE_SNAPSHOT,
          GameSnapshot.from(session, session.status()).toPayload());
      return GameCommandResult.broadcastOnly(ended, snapshot);
    }
  }

  private GameCommandResult withAiTurns(GameSession session, GameCommandResult base) {
    List<WsMessage> broadcast = new ArrayList<>(base.broadcast());
    applyAiTurns(session, broadcast);
    return new GameCommandResult(broadcast, base.direct());
  }

  private void applyAiTurns(GameSession session, List<WsMessage> broadcast) {
    if (applyAiResponseToPendingMove(session, broadcast)) {
      return;
    }
    int safety = 0;
    while ("active".equals(session.status()) && session.state().pendingMove() == null) {
      GameState state = session.state();
      String current = state.players().get(state.currentPlayerIndex()).name();
      if (!session.isBot(current)) {
        return;
      }
      if (safety++ > 4) {
        return;
      }

      Player bot = state.players().get(state.currentPlayerIndex());
      Optional<AiMove> move = aiMoveGenerator.bestMove(state.board(), bot, Board.standard(), wordDictionary);
      if (move.isEmpty()) {
        state.advanceTurn();
        session.incrementPasses();
        broadcast.add(new WsMessage(WsMessageType.TURN_ADVANCED, currentTurnPayload(state)));
        broadcast.add(snapshotMessage(session));
        if (checkPassEndgame(session, broadcast)) {
          return;
        }
        continue;
      }

      AiMove aiMove = move.get();
      List<Tile> removed = new ArrayList<>();
      try {
        takePlacedTilesFromRack(bot.rack(), aiMove.placement().placements().values(), removed);
        state.applyPendingMove(aiMove.placement(), aiMove.scoringResult());
        WsMessage proposed = new WsMessage(WsMessageType.MOVE_PROPOSED, Map.of(
            "player", bot.name(),
            "score", aiMove.scoringResult().totalScore(),
            "words", wordsToPayload(aiMove.scoringResult().words()),
            "placements", placementsToPayload(aiMove.placement().placements())));
        state.resolveChallenge(true);
        refillRack(bot.rack(), state.bag());
        session.resetPasses();
        WsMessage accepted = new WsMessage(WsMessageType.MOVE_ACCEPTED, Map.of(
            "player", bot.name(),
            "score", aiMove.scoringResult().totalScore(),
            "words", wordsToPayload(aiMove.scoringResult().words())));
        WsMessage turn = new WsMessage(WsMessageType.TURN_ADVANCED, currentTurnPayload(state));
        broadcast.add(proposed);
        broadcast.add(accepted);
        broadcast.add(turn);
        broadcast.add(snapshotMessage(session));
        if (checkOutEndgame(session, bot, broadcast)) {
          return;
        }
      } catch (RuntimeException e) {
        restoreTiles(bot.rack(), removed);
        state.advanceTurn();
        session.incrementPasses();
        broadcast.add(new WsMessage(WsMessageType.TURN_ADVANCED, currentTurnPayload(state)));
        broadcast.add(snapshotMessage(session));
        if (checkPassEndgame(session, broadcast)) {
          return;
        }
      }
    }
  }

  private boolean applyAiResponseToPendingMove(GameSession session, List<WsMessage> broadcast) {
    GameState state = session.state();
    PendingMove pending = state.pendingMove();
    if (pending == null) {
      return false;
    }
    int moverIndex = state.currentPlayerIndex();
    int challengerIndex = (moverIndex + 1) % state.players().size();
    String challengerName = state.players().get(challengerIndex).name();
    if (!session.isBot(challengerName)) {
      return false;
    }

    Player mover = state.players().get(moverIndex);
    List<String> invalidWords = pending.scoringResult().words().stream()
        .map(Word::text)
        .filter(word -> !dictionary.contains(word))
        .collect(Collectors.toList());
    boolean valid = invalidWords.isEmpty();
    if (!valid) {
      restoreTiles(mover.rack(), pending.placement().placements().values().stream()
          .map(PlacedTile::tile)
          .collect(Collectors.toList()));
    }

    state.resolveChallenge(valid);
    if (valid) {
      refillRack(mover.rack(), state.bag());
      session.resetPasses();
    }

    WsMessage result = valid
        ? new WsMessage(WsMessageType.MOVE_ACCEPTED, Map.of(
            "player", mover.name(),
            "score", pending.scoringResult().totalScore(),
            "words", wordsToPayload(pending.scoringResult().words())))
        : new WsMessage(WsMessageType.MOVE_REJECTED, Map.of(
            "player", mover.name(),
            "reason", "invalid_words",
            "invalidWords", invalidWords));
    WsMessage turn = new WsMessage(WsMessageType.TURN_ADVANCED, currentTurnPayload(state));
    broadcast.add(result);
    broadcast.add(turn);
    broadcast.add(snapshotMessage(session));
    if (valid) {
      return checkOutEndgame(session, mover, broadcast);
    }
    return false;
  }

  private GameSession requireSession(String roomId) {
    return registry.find(roomId)
        .orElseThrow(() -> rejected("game_not_started"));
  }

  private void ensureActive(GameSession session) {
    if (!"active".equals(session.status())) {
      throw rejected("game_ended");
    }
  }

  private void ensureNoPendingMove(GameState state) {
    if (state.pendingMove() != null) {
      throw rejected("pending_move");
    }
  }

  private int requirePlayerIndex(GameState state, String playerName) {
    for (int i = 0; i < state.players().size(); i++) {
      if (state.players().get(i).name().equals(playerName)) {
        return i;
      }
    }
    throw rejected("unknown_player");
  }

  private void ensureCurrentPlayer(GameState state, int playerIndex) {
    if (state.currentPlayerIndex() != playerIndex) {
      throw rejected("not_your_turn");
    }
  }

  private GameCommandException rejected(String reason) {
    return new GameCommandException(WsMessageType.MOVE_REJECTED, Map.of("reason", reason));
  }

  private void takePlacedTilesFromRack(Rack rack, Iterable<PlacedTile> placements, List<Tile> removed) {
    for (PlacedTile placed : placements) {
      removed.add(removeTileFromRack(rack, placed.tile()));
    }
  }

  private void takeTilesFromRack(Rack rack, Iterable<Tile> tiles, List<Tile> removed) {
    for (Tile tile : tiles) {
      removed.add(removeTileFromRack(rack, tile));
    }
  }

  private Tile removeTileFromRack(Rack rack, Tile needed) {
    for (Tile tile : rack.tiles()) {
      if (tile.blank() == needed.blank()
          && tile.letter() == needed.letter()
          && tile.points() == needed.points()) {
        rack.remove(tile);
        return tile;
      }
    }
    throw rejected("tile_not_in_rack");
  }

  private void restoreTiles(Rack rack, List<Tile> tiles) {
    for (Tile tile : tiles) {
      rack.add(tile);
    }
  }

  private void refillRack(Rack rack, TileBag bag) {
    int missing = rack.remainingCapacity();
    if (missing > 0) {
      rack.addAll(bag.draw(missing));
    }
  }

  private boolean checkOutEndgame(GameSession session, Player mover, List<WsMessage> broadcast) {
    GameState state = session.state();
    if (!state.bag().isEmpty()) {
      return false;
    }
    if (mover.rack().size() != 0) {
      return false;
    }
    applyOutBonus(state, mover);
    session.setStatus("ended");
    session.setWinner(mover.name());
    broadcast.add(new WsMessage(WsMessageType.GAME_ENDED, Map.of("winner", mover.name())));
    broadcast.add(snapshotMessage(session));
    return true;
  }

  private boolean checkPassEndgame(GameSession session, List<WsMessage> broadcast) {
    if (session.consecutivePasses() < MAX_CONSECUTIVE_PASSES) {
      return false;
    }
    applyPassPenalties(session.state());
    session.setStatus("ended");
    session.setWinner(determineWinnerByScore(session.state()));
    broadcast.add(new WsMessage(WsMessageType.GAME_ENDED, Map.of("winner", session.winner())));
    broadcast.add(snapshotMessage(session));
    return true;
  }

  private void applyOutBonus(GameState state, Player winner) {
    int bonus = 0;
    for (Player player : state.players()) {
      if (player == winner) {
        continue;
      }
      int penalty = rackPoints(player);
      if (penalty > 0) {
        player.addScore(-penalty);
        bonus += penalty;
      }
    }
    if (bonus > 0) {
      winner.addScore(bonus);
    }
  }

  private void applyPassPenalties(GameState state) {
    for (Player player : state.players()) {
      int penalty = rackPoints(player);
      if (penalty > 0) {
        player.addScore(-penalty);
      }
    }
  }

  private int rackPoints(Player player) {
    int total = 0;
    for (Tile tile : player.rack().tiles()) {
      total += tile.points();
    }
    return total;
  }

  private String determineWinnerByScore(GameState state) {
    String winner = null;
    int best = Integer.MIN_VALUE;
    for (Player player : state.players()) {
      if (player.score() > best) {
        best = player.score();
        winner = player.name();
      }
    }
    return winner;
  }

  private Map<String, Object> currentTurnPayload(GameState state) {
    int index = state.currentPlayerIndex();
    String name = state.players().get(index).name();
    return Map.of(
        "currentPlayerIndex", index,
        "currentPlayer", name);
  }

  private List<Map<String, Object>> placementsToPayload(Map<Coordinate, PlacedTile> placements) {
    return placements.entrySet().stream()
        .map(entry -> Map.<String, Object>of(
            "coordinate", entry.getKey().format(),
            "assignedLetter", Character.toString(entry.getValue().assignedLetter())))
        .collect(Collectors.toList());
  }

  private List<Map<String, Object>> wordsToPayload(List<Word> words) {
    return words.stream()
        .map(word -> Map.<String, Object>of(
            "text", word.text(),
            "coordinates", word.coordinates().stream()
                .map(Coordinate::format)
                .collect(Collectors.toList())))
        .collect(Collectors.toList());
  }

  private WsMessage snapshotMessage(GameSession session) {
    return new WsMessage(WsMessageType.STATE_SNAPSHOT,
        GameSnapshot.from(session, session.status()).toPayload());
  }

  private String determineWinner(GameState state, int resigningIndex) {
    if (state.players().isEmpty()) {
      return null;
    }
    int winnerIndex = (resigningIndex + 1) % state.players().size();
    return state.players().get(winnerIndex).name();
  }
}
