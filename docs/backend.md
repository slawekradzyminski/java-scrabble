# Backend summary (Phase 3)

## Services
- `RoomService` manages lobby rooms (create/list/join).
- `GameService` manages game sessions, state, and applies engine rules.
- `GameRegistry` stores in-memory sessions keyed by room id.

## REST endpoints
- `POST /api/rooms` create a room (optional `ai: true` adds a computer player).
- `GET /api/rooms` list rooms.
- `POST /api/rooms/{roomId}/join` join a room.
- `POST /api/rooms/{roomId}/game/start` start a game.
- `GET /api/rooms/{roomId}/game/state?player=Name` snapshot (player-scoped rack).
- `POST /api/rooms/{roomId}/game/command` play, pass, challenge, exchange, resign.

## WebSocket
- Endpoint: `ws://host/ws?roomId=...&player=...`
- Commands: `PLAY_TILES`, `EXCHANGE`, `PASS`, `CHALLENGE`, `RESIGN`, `SYNC`, `PING`.
- Events: `STATE_SNAPSHOT`, `MOVE_PROPOSED`, `MOVE_ACCEPTED`, `MOVE_REJECTED`, `TURN_ADVANCED`, `GAME_ENDED`.

## Snapshot model
- Each snapshot contains board tiles, scores, pending move, bag count.
- Rack tiles are included only for the requesting player.

## Dictionary use
- `FstDictionary` validates words during challenges.
- Invalid word challenges reject the pending move and restore tiles.

## MVP rules
- In-memory only, no persistence.
- Basic resign ends the game immediately.
- Exchange requires enough tiles in bag.
- Endgame triggers on 4 consecutive pass/exchange turns, or when a player goes out with an empty bag.
- Rack penalties are applied on endgame; going-out player gains opponents' rack points.
- CORS allows local frontend dev origins (localhost:5173, 127.0.0.1:5173).
- AI turns auto-resolve as accepted (no challenge window).

## AI self-play simulation
Run a 2-AI simulation with the full FST dictionary from `artifacts/osps.fst`, logging each move to `reports/`:
```
./gradlew :packages:game-engine:test --tests com.scrabble.engine.ai.AiSelfPlaySimulationTest -Dai.simulation=true
```

Optional tuning:
- `-Dai.simulation.maxCandidates=900`
- `-Dai.simulation.maxTurns=400`
- `-Dai.simulation.games=10`
- `-Dai.simulation.minScore=700`
- `-Dai.simulation.seed=12345`
- `-Dai.report.dir=reports`
