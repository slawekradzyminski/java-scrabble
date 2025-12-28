package com.scrabble.backend.game;

import com.scrabble.backend.lobby.Room;
import com.scrabble.backend.lobby.RoomService;
import com.scrabble.backend.ws.GameCommandResult;
import com.scrabble.backend.ws.WsMessage;
import com.scrabble.dictionary.Dictionary;
import com.scrabble.engine.Board;
import com.scrabble.engine.BoardState;
import com.scrabble.engine.Coordinate;
import com.scrabble.engine.GameState;
import com.scrabble.engine.MovePlacement;
import com.scrabble.engine.MoveValidator;
import com.scrabble.engine.PlacedTile;
import com.scrabble.engine.Player;
import com.scrabble.engine.Rack;
import com.scrabble.engine.ScoringResult;
import com.scrabble.engine.Scorer;
import com.scrabble.engine.Tile;
import com.scrabble.engine.TileBag;
import com.scrabble.engine.Word;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class GameService {
  private static final int MAX_EXCHANGES_PER_PLAYER = 3;
  private static final int MIN_EXCHANGE_BAG_SIZE = 7;
  private final RoomService roomService;
  private final Dictionary dictionary;
  private final Random random;
  private final GameRegistry registry = new GameRegistry();
  private final GameCommandValidator validator;
  private final GameMessageFactory messageFactory;
  private final GameRackManager rackManager;
  private final GameEndgameService endgameService;
  private final GameAiService aiService;

  public GameService(
      RoomService roomService,
      Dictionary dictionary,
      Random random,
      GameCommandValidator validator,
      GameMessageFactory messageFactory,
      GameRackManager rackManager,
      GameEndgameService endgameService,
      GameAiService aiService) {
    this.roomService = roomService;
    this.dictionary = dictionary;
    this.random = random;
    this.validator = validator;
    this.messageFactory = messageFactory;
    this.rackManager = rackManager;
    this.endgameService = endgameService;
    this.aiService = aiService;
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
      session.bumpStateVersion();
      aiService.applyAiTurns(session, new ArrayList<>());
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

  public GameEventPage eventsAfter(String roomId, long afterEventId, int limit) {
    return registry.find(roomId)
        .map(session -> new GameEventPage(
            session.eventsAfter(afterEventId, limit).stream()
                .map(GameEvent::toMap)
                .collect(Collectors.toList()),
            session.lastEventId()))
        .orElseGet(() -> new GameEventPage(List.of(), 0));
  }

  public GameCommandResult playTiles(String roomId, String playerName, Map<Coordinate, PlacedTile> placements) {
    GameSession session = requireSession(roomId);
    synchronized (session) {
      validator.ensureActive(session);
      GameState state = session.state();
      validator.ensureNoPendingMove(state);
      int playerIndex = validator.requirePlayerIndex(state, playerName);
      validator.ensureCurrentPlayer(state, playerIndex);

      Player player = state.players().get(playerIndex);
      List<Tile> removed = new ArrayList<>();
      try {
        rackManager.takePlacedTilesFromRack(player.rack(), placements.values(), removed);
        MovePlacement move = new MovePlacement(placements);
        MoveValidator.validatePlacement(state.board(), move);
        ScoringResult scoring = Scorer.score(state.board(), move, Board.standard());
        state.applyPendingMove(move, scoring);
        List<String> invalidWords = scoring.words().stream()
            .map(Word::text)
            .filter(word -> !dictionary.contains(word))
            .collect(Collectors.toList());
        boolean valid = invalidWords.isEmpty();
        if (!valid) {
          rackManager.restoreTiles(player.rack(), placements.values().stream()
              .map(PlacedTile::tile)
              .collect(Collectors.toList()));
        }
        state.resolveChallenge(valid);
        List<WsMessage> broadcast = new ArrayList<>();
        if (valid) {
          rackManager.refillRack(player.rack(), state.bag());
          session.resetPasses();
          session.bumpStateVersion();
          broadcast.add(messageFactory.moveAccepted(
              player, scoring, placements, session.stateVersion()));
        } else {
          session.bumpStateVersion();
          broadcast.add(messageFactory.moveRejected(
              player.name(), GameCommandReasons.INVALID_WORDS, invalidWords, session.stateVersion()));
        }
        broadcast.add(messageFactory.turnAdvanced(state, session.stateVersion()));
        broadcast.add(messageFactory.snapshot(session));
        if (valid) {
          endgameService.checkOutEndgame(session, player, broadcast);
        }
        GameCommandResult base = new GameCommandResult(broadcast, List.of());
        return withAiTurns(session, base);
      } catch (RuntimeException e) {
        rackManager.restoreTiles(player.rack(), removed);
        if (e instanceof com.scrabble.backend.ws.GameCommandException) {
          throw e;
        }
        throw GameCommandErrors.rejected(GameCommandReasons.INVALID_MOVE);
      }
    }
  }

  public GameCommandResult pass(String roomId, String playerName) {
    GameSession session = requireSession(roomId);
    synchronized (session) {
      validator.ensureActive(session);
      GameState state = session.state();
      validator.ensureNoPendingMove(state);
      int playerIndex = validator.requirePlayerIndex(state, playerName);
      validator.ensureCurrentPlayer(state, playerIndex);
      state.advanceTurn();
      session.incrementPasses();
      session.bumpStateVersion();
      List<WsMessage> broadcast = new ArrayList<>(List.of(
          messageFactory.pass(playerName, session.stateVersion()),
          messageFactory.turnAdvanced(state, session.stateVersion()),
          messageFactory.snapshot(session)));
      endgameService.checkPassEndgame(session, broadcast);
      GameCommandResult base = new GameCommandResult(broadcast, List.of());
      return withAiTurns(session, base);
    }
  }

  public GameCommandResult exchange(String roomId, String playerName, List<Tile> tiles) {
    GameSession session = requireSession(roomId);
    synchronized (session) {
      validator.ensureActive(session);
      GameState state = session.state();
      validator.ensureNoPendingMove(state);
      int playerIndex = validator.requirePlayerIndex(state, playerName);
      validator.ensureCurrentPlayer(state, playerIndex);
      if (tiles.isEmpty()) {
        throw GameCommandErrors.rejected(GameCommandReasons.EMPTY_EXCHANGE);
      }
      if (session.exchangesUsed(playerName) >= MAX_EXCHANGES_PER_PLAYER) {
        throw GameCommandErrors.rejected(GameCommandReasons.EXCHANGE_LIMIT_REACHED);
      }
      if (state.bag().size() < MIN_EXCHANGE_BAG_SIZE) {
        throw GameCommandErrors.rejected(GameCommandReasons.BAG_TOO_SMALL);
      }
      if (state.bag().size() < tiles.size()) {
        throw GameCommandErrors.rejected(GameCommandReasons.BAG_TOO_SMALL);
      }
      Player player = state.players().get(playerIndex);
      List<Tile> removed = new ArrayList<>();
      try {
        rackManager.takeTilesFromRack(player.rack(), tiles, removed);
        state.bag().addAll(removed, random);
        List<Tile> drawn = state.bag().draw(tiles.size());
        player.rack().addAll(drawn);
        session.incrementExchanges(playerName);
        state.advanceTurn();
        session.incrementPasses();
        session.bumpStateVersion();
        List<WsMessage> broadcast = new ArrayList<>(List.of(
            messageFactory.exchange(playerName, tiles.size(), session.stateVersion()),
            messageFactory.turnAdvanced(state, session.stateVersion()),
            messageFactory.snapshot(session)));
        endgameService.checkPassEndgame(session, broadcast);
        GameCommandResult base = new GameCommandResult(broadcast, List.of());
        return withAiTurns(session, base);
      } catch (RuntimeException e) {
        rackManager.restoreTiles(player.rack(), removed);
        if (e instanceof com.scrabble.backend.ws.GameCommandException) {
          throw e;
        }
        throw GameCommandErrors.rejected(GameCommandReasons.EXCHANGE_FAILED);
      }
    }
  }

  public GameCommandResult resign(String roomId, String playerName) {
    GameSession session = requireSession(roomId);
    synchronized (session) {
      validator.ensureActive(session);
      GameState state = session.state();
      int playerIndex = validator.requirePlayerIndex(state, playerName);
      session.setStatus("ended");
      session.setWinner(validator.determineWinner(state, playerIndex));
      session.bumpStateVersion();
      WsMessage ended = messageFactory.gameEndedByResign(session.winner(), playerName, session.stateVersion());
      WsMessage snapshot = messageFactory.snapshot(session);
      session.recordHistory(ended);
      return GameCommandResult.broadcastOnly(ended, snapshot);
    }
  }

  private GameCommandResult withAiTurns(GameSession session, GameCommandResult base) {
    List<WsMessage> broadcast = new ArrayList<>(base.broadcast());
    aiService.applyAiTurns(session, broadcast);
    recordHistory(session, broadcast);
    return new GameCommandResult(broadcast, base.direct());
  }

  private GameSession requireSession(String roomId) {
    return registry.find(roomId)
        .orElseThrow(() -> GameCommandErrors.rejected(GameCommandReasons.GAME_NOT_STARTED));
  }

  private void recordHistory(GameSession session, List<WsMessage> messages) {
    for (WsMessage message : messages) {
      session.recordHistory(message);
    }
  }
}
