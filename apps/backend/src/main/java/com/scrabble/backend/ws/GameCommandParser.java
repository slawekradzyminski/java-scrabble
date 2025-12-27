package com.scrabble.backend.ws;

import com.fasterxml.jackson.databind.JsonNode;
import com.scrabble.engine.Coordinate;
import com.scrabble.engine.LetterTile;
import com.scrabble.engine.PlacedTile;
import com.scrabble.engine.Tile;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class GameCommandParser {

  public Map<Coordinate, PlacedTile> parsePlacements(JsonNode payload) {
    JsonNode placementsNode = payload.path("placements");
    if (!placementsNode.isArray()) {
      throw new GameCommandException(WsMessageType.MOVE_REJECTED, Map.of("reason", "missing_placements"));
    }
    Map<Coordinate, PlacedTile> placements = new HashMap<>();
    for (JsonNode node : placementsNode) {
      String coordinateText = node.path("coordinate").asText(null);
      String letterText = node.path("letter").asText(null);
      boolean blank = node.path("blank").asBoolean(false);
      if (coordinateText == null || letterText == null || letterText.isBlank()) {
        throw new GameCommandException(WsMessageType.MOVE_REJECTED, Map.of("reason", "invalid_placement"));
      }
      char letter = normalizeLetter(letterText);
      Tile tile = blank ? Tile.blankTile() : LetterTile.fromLetter(letter).toTile();
      PlacedTile placed = new PlacedTile(tile, letter);
      Coordinate coordinate = Coordinate.parse(coordinateText);
      if (placements.putIfAbsent(coordinate, placed) != null) {
        throw new GameCommandException(WsMessageType.MOVE_REJECTED, Map.of("reason", "duplicate_coordinate"));
      }
    }
    return placements;
  }

  public List<Tile> parseTiles(JsonNode payload) {
    JsonNode tilesNode = payload.path("tiles");
    if (!tilesNode.isArray()) {
      throw new GameCommandException(WsMessageType.MOVE_REJECTED, Map.of("reason", "missing_tiles"));
    }
    List<Tile> tiles = new ArrayList<>();
    for (JsonNode node : tilesNode) {
      String letterText = node.path("letter").asText(null);
      boolean blank = node.path("blank").asBoolean(false);
      if (blank) {
        tiles.add(Tile.blankTile());
        continue;
      }
      if (letterText == null || letterText.isBlank()) {
        throw new GameCommandException(WsMessageType.MOVE_REJECTED, Map.of("reason", "invalid_tile"));
      }
      char letter = normalizeLetter(letterText);
      tiles.add(LetterTile.fromLetter(letter).toTile());
    }
    return tiles;
  }

  private char normalizeLetter(String text) {
    String trimmed = text.trim();
    if (trimmed.length() != 1) {
      throw new GameCommandException(WsMessageType.MOVE_REJECTED, Map.of("reason", "invalid_letter"));
    }
    return trimmed.toUpperCase(Locale.forLanguageTag("pl-PL")).charAt(0);
  }
}
