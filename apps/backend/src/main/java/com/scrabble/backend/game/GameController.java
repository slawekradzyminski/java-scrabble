package com.scrabble.backend.game;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/rooms/{roomId}/game")
public class GameController {
  private final GameService gameService;

  public GameController(GameService gameService) {
    this.gameService = gameService;
  }

  @PostMapping("/start")
  public GameSnapshot start(@PathVariable String roomId) {
    return GameSnapshot.from(gameService.start(roomId), "active");
  }

  @GetMapping("/state")
  public GameSnapshot state(@PathVariable String roomId) {
    return gameService.snapshot(roomId);
  }
}
