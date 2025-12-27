package com.scrabble.backend.game;

import com.scrabble.backend.lobby.Room;
import com.scrabble.backend.lobby.RoomService;
import com.scrabble.dictionary.Dictionary;
import com.scrabble.engine.Coordinate;
import com.scrabble.engine.PlacedTile;
import com.scrabble.engine.Player;
import com.scrabble.engine.Tile;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GameServiceTest {

  @Test
  void challengeAcceptsValidMoveAndAdvancesTurn() {
    RoomService roomService = new RoomService();
    Dictionary dictionary = word -> true;
    GameService gameService = new GameService(roomService, dictionary, new Random(7));

    Room room = roomService.create("Room", "Alice");
    roomService.join(room.id(), "Bob");
    GameSession session = gameService.start(room.id());

    Player alice = session.state().players().get(0);
    List<Tile> tiles = pickNonBlankTiles(alice.rack().tiles(), 2);

    Map<Coordinate, PlacedTile> placements = Map.of(
        Coordinate.parse("H8"), PlacedTile.fromTile(tiles.get(0)),
        Coordinate.parse("H9"), PlacedTile.fromTile(tiles.get(1)));

    gameService.playTiles(room.id(), "Alice", placements);
    GameSnapshot afterPlay = gameService.snapshot(room.id());
    assertTrue(afterPlay.pendingMove());

    gameService.challenge(room.id(), "Bob");

    GameSnapshot afterChallenge = gameService.snapshot(room.id());
    assertFalse(afterChallenge.pendingMove());
    assertEquals(1, afterChallenge.currentPlayerIndex());
    assertEquals(2, afterChallenge.boardTiles());
    assertEquals(7, alice.rack().size());
    assertTrue(alice.score() > 0);
  }

  @Test
  void challengeRejectsInvalidMoveAndRestoresRack() {
    RoomService roomService = new RoomService();
    Dictionary dictionary = word -> false;
    GameService gameService = new GameService(roomService, dictionary, new Random(3));

    Room room = roomService.create("Room", "Alice");
    roomService.join(room.id(), "Bob");
    GameSession session = gameService.start(room.id());

    Player alice = session.state().players().get(0);
    List<Tile> tiles = pickNonBlankTiles(alice.rack().tiles(), 2);

    Map<Coordinate, PlacedTile> placements = Map.of(
        Coordinate.parse("H8"), PlacedTile.fromTile(tiles.get(0)),
        Coordinate.parse("H9"), PlacedTile.fromTile(tiles.get(1)));

    gameService.playTiles(room.id(), "Alice", placements);
    assertEquals(5, alice.rack().size());

    gameService.challenge(room.id(), "Bob");

    GameSnapshot afterChallenge = gameService.snapshot(room.id());
    assertFalse(afterChallenge.pendingMove());
    assertEquals(0, afterChallenge.boardTiles());
    assertEquals(7, alice.rack().size());
  }

  @Test
  void passAdvancesTurn() {
    RoomService roomService = new RoomService();
    Dictionary dictionary = word -> true;
    GameService gameService = new GameService(roomService, dictionary, new Random(5));

    Room room = roomService.create("Room", "Alice");
    roomService.join(room.id(), "Bob");
    gameService.start(room.id());

    gameService.pass(room.id(), "Alice");

    GameSnapshot snapshot = gameService.snapshot(room.id());
    assertEquals(1, snapshot.currentPlayerIndex());
  }

  @Test
  void exchangeSwapsTilesAndAdvancesTurn() {
    RoomService roomService = new RoomService();
    Dictionary dictionary = word -> true;
    GameService gameService = new GameService(roomService, dictionary, new Random(9));

    Room room = roomService.create("Room", "Alice");
    roomService.join(room.id(), "Bob");
    GameSession session = gameService.start(room.id());
    Player alice = session.state().players().get(0);

    List<Tile> exchangeTiles = List.of(alice.rack().tiles().get(0));
    gameService.exchange(room.id(), "Alice", exchangeTiles);

    GameSnapshot snapshot = gameService.snapshot(room.id());
    assertEquals(1, snapshot.currentPlayerIndex());
    assertEquals(7, alice.rack().size());
  }

  @Test
  void resignEndsGameAndSetsWinner() {
    RoomService roomService = new RoomService();
    Dictionary dictionary = word -> true;
    GameService gameService = new GameService(roomService, dictionary, new Random(11));

    Room room = roomService.create("Room", "Alice");
    roomService.join(room.id(), "Bob");
    gameService.start(room.id());

    gameService.resign(room.id(), "Alice");

    GameSnapshot snapshot = gameService.snapshot(room.id());
    assertEquals("ended", snapshot.status());
    assertEquals("Bob", snapshot.winner());
  }

  private List<Tile> pickNonBlankTiles(List<Tile> tiles, int count) {
    List<Tile> picked = new ArrayList<>();
    for (Tile tile : tiles) {
      if (!tile.blank()) {
        picked.add(tile);
      }
      if (picked.size() == count) {
        return picked;
      }
    }
    throw new IllegalStateException("Not enough non-blank tiles");
  }
}
