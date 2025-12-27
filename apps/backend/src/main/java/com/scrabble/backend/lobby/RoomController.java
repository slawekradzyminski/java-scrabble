package com.scrabble.backend.lobby;

import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/rooms")
public class RoomController {
  private final RoomService roomService;

  public RoomController(RoomService roomService) {
    this.roomService = roomService;
  }

  @GetMapping
  public List<RoomResponse> listRooms() {
    return roomService.list().stream().map(RoomResponse::from).toList();
  }

  @PostMapping
  public RoomResponse create(@RequestBody CreateRoomRequest request) {
    boolean addAi = Boolean.TRUE.equals(request.ai());
    Room room = roomService.create(request.name(), request.owner(), addAi);
    return RoomResponse.from(room);
  }

  @PostMapping("/{roomId}/join")
  public RoomResponse join(@PathVariable String roomId, @RequestBody JoinRoomRequest request) {
    Room room = roomService.join(roomId, request.player());
    return RoomResponse.from(room);
  }

  public record CreateRoomRequest(String name, String owner, Boolean ai) { }

  public record JoinRoomRequest(String player) { }

  public record RoomResponse(String id, String name, List<String> players) {
    public static RoomResponse from(Room room) {
      return new RoomResponse(room.id(), room.name(), room.players());
    }
  }
}
