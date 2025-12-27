package com.scrabble.backend.game;

import static org.assertj.core.api.Assertions.assertThat;

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

class GameServiceTest {

  @Test
  void challengeAcceptsValidMoveAndAdvancesTurn() {
    // given
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

    // when
    gameService.playTiles(room.id(), "Alice", placements);
    GameSnapshot afterPlay = gameService.snapshot(room.id());
    assertThat(afterPlay.pendingMove()).isTrue();

    gameService.challenge(room.id(), "Bob");

    // then
    GameSnapshot afterChallenge = gameService.snapshot(room.id());
    assertThat(afterChallenge.pendingMove()).isFalse();
    assertThat(afterChallenge.currentPlayerIndex()).isEqualTo(1);
    assertThat(afterChallenge.boardTiles()).isEqualTo(2);
    assertThat(alice.rack().size()).isEqualTo(7);
    assertThat(alice.score()).isPositive();
  }

  @Test
  void challengeRejectsInvalidMoveAndRestoresRack() {
    // given
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

    // when
    gameService.playTiles(room.id(), "Alice", placements);
    assertThat(alice.rack().size()).isEqualTo(5);

    gameService.challenge(room.id(), "Bob");

    // then
    GameSnapshot afterChallenge = gameService.snapshot(room.id());
    assertThat(afterChallenge.pendingMove()).isFalse();
    assertThat(afterChallenge.boardTiles()).isEqualTo(0);
    assertThat(alice.rack().size()).isEqualTo(7);
  }

  @Test
  void passAdvancesTurn() {
    // given
    RoomService roomService = new RoomService();
    Dictionary dictionary = word -> true;
    GameService gameService = new GameService(roomService, dictionary, new Random(5));

    Room room = roomService.create("Room", "Alice");
    roomService.join(room.id(), "Bob");
    gameService.start(room.id());

    // when
    gameService.pass(room.id(), "Alice");

    // then
    GameSnapshot snapshot = gameService.snapshot(room.id());
    assertThat(snapshot.currentPlayerIndex()).isEqualTo(1);
  }

  @Test
  void aiOpponentPlaysAfterHumanTurn() {
    // given
    RoomService roomService = new RoomService();
    Dictionary dictionary = word -> true;
    GameService gameService = new GameService(roomService, dictionary, new Random(13));

    Room room = roomService.create("Room AI", "Alice", true);
    gameService.start(room.id());

    // when
    gameService.pass(room.id(), "Alice");

    // then
    GameSnapshot snapshot = gameService.snapshot(room.id());
    assertThat(snapshot.currentPlayerIndex()).isEqualTo(0);
    assertThat(snapshot.pendingMove()).isFalse();
  }

  @Test
  void exchangeSwapsTilesAndAdvancesTurn() {
    // given
    RoomService roomService = new RoomService();
    Dictionary dictionary = word -> true;
    GameService gameService = new GameService(roomService, dictionary, new Random(9));

    Room room = roomService.create("Room", "Alice");
    roomService.join(room.id(), "Bob");
    GameSession session = gameService.start(room.id());
    Player alice = session.state().players().get(0);

    List<Tile> exchangeTiles = List.of(alice.rack().tiles().get(0));
    // when
    gameService.exchange(room.id(), "Alice", exchangeTiles);

    // then
    GameSnapshot snapshot = gameService.snapshot(room.id());
    assertThat(snapshot.currentPlayerIndex()).isEqualTo(1);
    assertThat(alice.rack().size()).isEqualTo(7);
  }

  @Test
  void resignEndsGameAndSetsWinner() {
    // given
    RoomService roomService = new RoomService();
    Dictionary dictionary = word -> true;
    GameService gameService = new GameService(roomService, dictionary, new Random(11));

    Room room = roomService.create("Room", "Alice");
    roomService.join(room.id(), "Bob");
    gameService.start(room.id());

    // when
    gameService.resign(room.id(), "Alice");

    // then
    GameSnapshot snapshot = gameService.snapshot(room.id());
    assertThat(snapshot.status()).isEqualTo("ended");
    assertThat(snapshot.winner()).isEqualTo("Bob");
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
