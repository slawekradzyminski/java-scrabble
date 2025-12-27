package com.scrabble.backend.game;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.scrabble.backend.lobby.Room;
import com.scrabble.backend.lobby.RoomService;
import com.scrabble.backend.ws.GameCommandException;
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
  void autoAcceptsValidMoveAndAdvancesTurn() {
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

    // then
    GameSnapshot afterPlay = gameService.snapshot(room.id());
    assertThat(afterPlay.pendingMove()).isFalse();
    assertThat(afterPlay.currentPlayerIndex()).isEqualTo(1);
    assertThat(afterPlay.boardTiles()).isEqualTo(2);
    assertThat(alice.rack().size()).isEqualTo(7);
    assertThat(alice.score()).isPositive();
  }

  @Test
  void autoRejectsInvalidMoveAndRestoresRack() {
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

    // then
    GameSnapshot afterPlay = gameService.snapshot(room.id());
    assertThat(afterPlay.pendingMove()).isFalse();
    assertThat(afterPlay.currentPlayerIndex()).isEqualTo(1);
    assertThat(afterPlay.boardTiles()).isEqualTo(0);
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
  void autoResolvesMoveWithoutChallenge() {
    // given
    RoomService roomService = new RoomService();
    Dictionary dictionary = word -> true;
    GameService gameService = new GameService(roomService, dictionary, new Random(15));

    Room room = roomService.create("Room AI", "Alice", true);
    GameSession session = gameService.start(room.id());
    Player alice = session.state().players().get(0);
    List<Tile> tiles = pickNonBlankTiles(alice.rack().tiles(), 2);

    Map<Coordinate, PlacedTile> placements = Map.of(
        Coordinate.parse("H8"), PlacedTile.fromTile(tiles.get(0)),
        Coordinate.parse("H9"), PlacedTile.fromTile(tiles.get(1)));

    // when
    gameService.playTiles(room.id(), "Alice", placements);

    // then
    GameSnapshot snapshot = gameService.snapshot(room.id());
    assertThat(snapshot.pendingMove()).isFalse();
    assertThat(alice.rack().size()).isEqualTo(7);
    assertThat(alice.score()).isPositive();
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
  void exchangeRejectsWhenBagHasFewerThanSevenTiles() {
    // given
    RoomService roomService = new RoomService();
    Dictionary dictionary = word -> true;
    GameService gameService = new GameService(roomService, dictionary, new Random(5));

    Room room = roomService.create("Room", "Alice");
    GameSession session = gameService.start(room.id());
    Player alice = session.state().players().get(0);

    session.state().bag().draw(session.state().bag().size() - 6);
    List<Tile> exchangeTiles = List.of(alice.rack().tiles().get(0));

    // when / then
    assertThatThrownBy(() -> gameService.exchange(room.id(), "Alice", exchangeTiles))
        .isInstanceOf(GameCommandException.class)
        .satisfies(error -> {
          GameCommandException exception = (GameCommandException) error;
          assertThat(exception.payload().get("reason")).isEqualTo("bag_too_small");
        });
  }

  @Test
  void exchangeRejectsAfterThreeExchangesBySamePlayer() {
    // given
    RoomService roomService = new RoomService();
    Dictionary dictionary = word -> true;
    GameService gameService = new GameService(roomService, dictionary, new Random(13));

    Room room = roomService.create("Room", "Alice");
    roomService.join(room.id(), "Bob");
    GameSession session = gameService.start(room.id());
    Player alice = session.state().players().get(0);

    for (int i = 0; i < 3; i += 1) {
      // when
      List<Tile> exchangeTiles = List.of(alice.rack().tiles().get(0));
      gameService.exchange(room.id(), "Alice", exchangeTiles);
      session.resetPasses();
      gameService.pass(room.id(), "Bob");
      session.resetPasses();
    }

    List<Tile> exchangeTiles = List.of(alice.rack().tiles().get(0));

    // then
    assertThatThrownBy(() -> gameService.exchange(room.id(), "Alice", exchangeTiles))
        .isInstanceOf(GameCommandException.class)
        .satisfies(error -> {
          GameCommandException exception = (GameCommandException) error;
          assertThat(exception.payload().get("reason")).isEqualTo("exchange_limit_reached");
        });
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

  @Test
  void outMoveEndsGameAndAppliesRackBonus() {
    // given
    RoomService roomService = new RoomService();
    Dictionary dictionary = word -> true;
    GameService gameService = new GameService(roomService, dictionary, new Random(17));

    Room room = roomService.create("Room", "Alice");
    roomService.join(room.id(), "Bob");
    GameSession session = gameService.start(room.id());
    Player alice = session.state().players().get(0);
    Player bob = session.state().players().get(1);

    session.state().bag().draw(session.state().bag().size());

    List<Tile> rackTiles = new ArrayList<>(alice.rack().tiles());
    List<Tile> playTiles = pickNonBlankTiles(rackTiles, 2);
    List<Tile> keep = new ArrayList<>(playTiles);
    for (Tile tile : new ArrayList<>(alice.rack().tiles())) {
      if (keep.contains(tile)) {
        keep.remove(tile);
        continue;
      }
      alice.rack().remove(tile);
    }

    Map<Coordinate, PlacedTile> placements = Map.of(
        Coordinate.parse("H8"), PlacedTile.fromTile(playTiles.get(0)),
        Coordinate.parse("H9"), PlacedTile.fromTile(playTiles.get(1)));

    // when
    gameService.playTiles(room.id(), "Alice", placements);

    // then
    GameSnapshot snapshot = gameService.snapshot(room.id());
    assertThat(snapshot.status()).isEqualTo("ended");
    assertThat(snapshot.winner()).isEqualTo("Alice");
    assertThat(alice.score()).isGreaterThan(bob.score());
  }

  @Test
  void fourPassesEndGameWithRackPenalties() {
    // given
    RoomService roomService = new RoomService();
    Dictionary dictionary = word -> true;
    GameService gameService = new GameService(roomService, dictionary, new Random(21));

    Room room = roomService.create("Room", "Alice");
    roomService.join(room.id(), "Bob");
    gameService.start(room.id());

    // when
    gameService.pass(room.id(), "Alice");
    gameService.pass(room.id(), "Bob");
    gameService.pass(room.id(), "Alice");
    gameService.pass(room.id(), "Bob");

    // then
    GameSnapshot snapshot = gameService.snapshot(room.id());
    assertThat(snapshot.status()).isEqualTo("ended");
    assertThat(snapshot.winner()).isNotNull();
    assertThat(snapshot.players()).allSatisfy(player ->
        assertThat(((Number) player.get("score")).intValue()).isLessThanOrEqualTo(0));
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
