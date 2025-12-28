package com.scrabble.backend.lobby;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/rooms")
@RequiredArgsConstructor
public class RoomController {
  private final RoomService roomService;

  @GetMapping
  public List<RoomResponse> listRooms() {
    return roomService.list().stream().map(RoomResponse::from).toList();
  }

  @PostMapping
  public RoomResponse create(@RequestBody CreateRoomRequest request) {
    boolean addAi = Boolean.TRUE.equals(request.getAi());
    Room room = roomService.create(request.getName(), request.getOwner(), addAi);
    return RoomResponse.from(room);
  }

  @PostMapping("/{roomId}/join")
  public RoomResponse join(@PathVariable String roomId, @RequestBody JoinRoomRequest request) {
    Room room = roomService.join(roomId, request.getPlayer());
    return RoomResponse.from(room);
  }

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  public static class CreateRoomRequest {
    private String name;
    private String owner;
    private Boolean ai;
  }

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  public static class JoinRoomRequest {
    private String player;
  }

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  public static class RoomResponse {
    private String id;
    private String name;
    private List<String> players;

    public static RoomResponse from(Room room) {
      return new RoomResponse(room.id(), room.name(), room.players());
    }
  }
}
