package com.scrabble.backend.game;

import static org.assertj.core.api.Assertions.assertThat;

import com.scrabble.backend.ws.WsMessage;
import com.scrabble.engine.Coordinate;
import com.scrabble.engine.PlacedTile;
import com.scrabble.engine.Player;
import com.scrabble.engine.ScoringResult;
import com.scrabble.engine.Tile;
import com.scrabble.engine.Word;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class GameMessageFactoryTest {

  @Test
  void buildsMoveAcceptedPayload() {
    // given
    GameMessageFactory factory = new GameMessageFactory();
    Player player = new Player("Alice");
    Word word = new Word("HI", List.of(new Coordinate(7, 7), new Coordinate(7, 8)));
    ScoringResult scoring = new ScoringResult(12, List.of(word));
    Map<Coordinate, PlacedTile> placements = Map.of(
        new Coordinate(7, 7), new PlacedTile(Tile.of('H', 4), 'H'),
        new Coordinate(7, 8), new PlacedTile(Tile.of('I', 1), 'I'));

    // when
    WsMessage message = factory.moveAccepted(player, scoring, placements, 3);

    // then
    assertThat(message.type().name()).isEqualTo("MOVE_ACCEPTED");
    assertThat(message.payload()).containsEntry("player", "Alice");
    assertThat(message.payload()).containsEntry("score", 12);
    assertThat(message.payload()).containsEntry("stateVersion", 3);
    assertThat(message.payload().get("placements")).isInstanceOf(List.class);
    assertThat(message.payload().get("words")).isInstanceOf(List.class);
  }
}
