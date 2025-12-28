package com.scrabble.backend.game;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.scrabble.backend.BackendApplication;
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
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.boot.test.context.TestConfiguration;

@SpringBootTest(
    classes = {BackendApplication.class, GameServiceTest.TestConfig.class},
    properties = "spring.main.allow-bean-definition-overriding=true")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class GameServiceTest {

  @Autowired
  private GameService gameService;

  @Autowired
  private RoomService roomService;

  @Autowired
  private MutableDictionary dictionary;

  @BeforeEach
  void allowAllWords() {
    dictionary.allowAll();
  }

  @Test
  void autoAcceptsValidMoveAndAdvancesTurn() {
    // given
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
    assertThat(afterPlay.isPendingMove()).isFalse();
    assertThat(afterPlay.getCurrentPlayerIndex()).isEqualTo(1);
    assertThat(afterPlay.getBoardTiles()).isEqualTo(2);
    assertThat(alice.rack().size()).isEqualTo(7);
    assertThat(alice.score()).isPositive();
  }

  @Test
  void autoRejectsInvalidMoveAndRestoresRack() {
    // given
    dictionary.rejectAll();
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
    assertThat(afterPlay.isPendingMove()).isFalse();
    assertThat(afterPlay.getCurrentPlayerIndex()).isEqualTo(1);
    assertThat(afterPlay.getBoardTiles()).isEqualTo(0);
    assertThat(alice.rack().size()).isEqualTo(7);
  }

  @Test
  void passAdvancesTurn() {
    // given
    Room room = roomService.create("Room", "Alice");
    roomService.join(room.id(), "Bob");
    gameService.start(room.id());

    // when
    gameService.pass(room.id(), "Alice");

    // then
    GameSnapshot snapshot = gameService.snapshot(room.id());
    assertThat(snapshot.getCurrentPlayerIndex()).isEqualTo(1);
  }

  @Test
  void aiOpponentPlaysAfterHumanTurn() {
    // given
    Room room = roomService.create("Room AI", "Alice", true);
    gameService.start(room.id());

    // when
    gameService.pass(room.id(), "Alice");

    // then
    GameSnapshot snapshot = gameService.snapshot(room.id());
    assertThat(snapshot.getCurrentPlayerIndex()).isEqualTo(0);
    assertThat(snapshot.isPendingMove()).isFalse();
  }

  @Test
  void autoResolvesMoveWithoutChallenge() {
    // given
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
    assertThat(snapshot.isPendingMove()).isFalse();
    assertThat(alice.rack().size()).isEqualTo(7);
    assertThat(alice.score()).isPositive();
  }

  @Test
  void exchangeSwapsTilesAndAdvancesTurn() {
    // given
    Room room = roomService.create("Room", "Alice");
    roomService.join(room.id(), "Bob");
    GameSession session = gameService.start(room.id());
    Player alice = session.state().players().get(0);

    List<Tile> exchangeTiles = List.of(alice.rack().tiles().get(0));
    // when
    gameService.exchange(room.id(), "Alice", exchangeTiles);

    // then
    GameSnapshot snapshot = gameService.snapshot(room.id());
    assertThat(snapshot.getCurrentPlayerIndex()).isEqualTo(1);
    assertThat(alice.rack().size()).isEqualTo(7);
  }

  @Test
  void exchangeRejectsWhenBagHasFewerThanSevenTiles() {
    // given
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
          assertThat(exception.getPayload().get("reason")).isEqualTo(GameCommandReasons.BAG_TOO_SMALL);
        });
  }

  @Test
  void exchangeRejectsAfterThreeExchangesBySamePlayer() {
    // given
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
          assertThat(exception.getPayload().get("reason")).isEqualTo(GameCommandReasons.EXCHANGE_LIMIT_REACHED);
        });
  }

  @Test
  void resignEndsGameAndSetsWinner() {
    // given
    Room room = roomService.create("Room", "Alice");
    roomService.join(room.id(), "Bob");
    gameService.start(room.id());

    // when
    gameService.resign(room.id(), "Alice");

    // then
    GameSnapshot snapshot = gameService.snapshot(room.id());
    assertThat(snapshot.getStatus()).isEqualTo("ended");
    assertThat(snapshot.getWinner()).isEqualTo("Bob");
  }

  @Test
  void outMoveEndsGameAndAppliesRackBonus() {
    // given
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
    assertThat(snapshot.getStatus()).isEqualTo("ended");
    assertThat(snapshot.getWinner()).isEqualTo("Alice");
    assertThat(alice.score()).isGreaterThan(bob.score());
  }

  @Test
  void fourPassesEndGameWithRackPenalties() {
    // given
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
    assertThat(snapshot.getStatus()).isEqualTo("ended");
    assertThat(snapshot.getWinner()).isNotNull();
    assertThat(snapshot.getPlayers()).allSatisfy(player ->
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

  @TestConfiguration
  static class TestConfig {
    @Bean
    @Primary
    public MutableDictionary dictionary() {
      return new MutableDictionary();
    }

    @Bean
    @Primary
    public Random random() {
      return new Random(7);
    }
  }

  static final class MutableDictionary implements Dictionary {
    private final AtomicReference<Predicate<String>> predicate =
        new AtomicReference<>(word -> true);

    void allowAll() {
      predicate.set(word -> true);
    }

    void rejectAll() {
      predicate.set(word -> false);
    }

    @Override
    public boolean contains(String word) {
      return predicate.get().test(word);
    }

    @Override
    public boolean containsPrefix(String prefix) {
      return predicate.get().test(prefix);
    }
  }
}
