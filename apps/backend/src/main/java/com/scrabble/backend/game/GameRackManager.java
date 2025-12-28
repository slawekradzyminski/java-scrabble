package com.scrabble.backend.game;

import com.scrabble.engine.PlacedTile;
import com.scrabble.engine.Rack;
import com.scrabble.engine.Tile;
import com.scrabble.engine.TileBag;
import java.util.List;

final class GameRackManager {

  void takePlacedTilesFromRack(Rack rack, Iterable<PlacedTile> placements, List<Tile> removed) {
    for (PlacedTile placed : placements) {
      removed.add(removeTileFromRack(rack, placed.tile()));
    }
  }

  void takeTilesFromRack(Rack rack, Iterable<Tile> tiles, List<Tile> removed) {
    for (Tile tile : tiles) {
      removed.add(removeTileFromRack(rack, tile));
    }
  }

  void restoreTiles(Rack rack, List<Tile> tiles) {
    for (Tile tile : tiles) {
      rack.add(tile);
    }
  }

  void refillRack(Rack rack, TileBag bag) {
    int missing = rack.remainingCapacity();
    if (missing > 0) {
      rack.addAll(bag.draw(missing));
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
    throw GameCommandErrors.rejected("tile_not_in_rack");
  }
}
