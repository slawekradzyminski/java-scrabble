package com.scrabble.backend.lobby;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.stereotype.Service;

@Service
public class RoomService {
  private final AtomicLong counter = new AtomicLong();
  private final Map<String, Room> rooms = new ConcurrentHashMap<>();

  public Room create(String name, String owner) {
    return create(name, owner, false);
  }

  public Room create(String name, String owner, boolean addAi) {
    String id = Long.toString(counter.incrementAndGet());
    Room room = new Room(id, name, Instant.now());
    room.addPlayer(owner);
    if (addAi) {
      room.addPlayer(uniqueBotName(room), true);
    }
    rooms.put(id, room);
    return room;
  }

  public Optional<Room> find(String id) {
    return Optional.ofNullable(rooms.get(id));
  }

  public List<Room> list() {
    List<Room> list = new ArrayList<>(rooms.values());
    list.sort(Comparator.comparing(Room::createdAt));
    return list;
  }

  public Room join(String id, String playerName) {
    Room room = rooms.get(id);
    if (room == null) {
      throw new IllegalArgumentException("Room not found: " + id);
    }
    room.addPlayer(playerName);
    return room;
  }

  private String uniqueBotName(Room room) {
    String base = "Computer";
    if (!room.players().contains(base)) {
      return base;
    }
    int index = 2;
    while (room.players().contains(base + " " + index)) {
      index++;
    }
    return base + " " + index;
  }
}
