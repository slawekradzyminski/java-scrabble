## Monorepo layout (one repository)

### Progress notes
- 2025-12-27: Ran AI simulator tests; lowered default batch min score to 600 and sanitized simulation console output to ASCII to avoid test report encoding failures.
- 2025-12-28: Added backend event IDs + events endpoint, and frontend delta history sync on reconnect (tests updated).
- 2025-12-28: Refactored Game page into smaller components and hooks, added event-driven snapshot fallback on WS events, and introduced shared bag/placement utilities with new frontend tests.
- 2025-12-28: Tweaked sidebar info-section styling (last move + preview) for a more polished look.
- 2025-12-28: Strengthened GPU-safe styling by flattening gradients/shadows on board, rack, and panels.
- 2025-12-28: Included event payloads for REST-hydrated history so last-move summaries can show score/words.
- 2025-12-28: Removed debug panel row and aligned board/rack to the top of the game column.
- 2025-12-28: Added frontend coverage run to regression script to catch CI coverage failures.
- 2025-12-28: Expanded usePlacements hook tests to lift branch coverage above thresholds.

### Top-level structure
- `apps/backend/` – Spring Boot WebFlux app (REST + WebSocket)
- `apps/frontend/` – React + TS
- `packages/game-engine/` – pure Java module (rules, move validation, scoring) no Spring
- `packages/dictionary-tools/` – CLI tool to compile `osps.txt` → `osps.fst`
- `packages/dictionary-runtime/` – runtime loader + lookup API used by backend
- `data/` – not committed (local dev): raw `osps.txt` (huge), optional local outputs
- `artifacts/` – optionally committed or released: `osps.fst` + metadata

### Why split dictionary-tools vs dictionary-runtime?
- Tools can depend on heavyweight build-time libs; runtime stays lean.
- You can enforce "same-format contract" via versioned metadata.

## Key early decisions (so you don't repaint later)

### Dictionary source vs artefact in Git
Because OSPS is huge (and may be licence-sensitive), you usually do:
- Do not commit `osps.txt` (put it in `data/` and `.gitignore`).
- Do commit either:
  - `artifacts/osps.fst` (if allowed), or
  - nothing, and instead build `osps.fst` in CI from a secure input (private CI secret/artifact store).

Even in a single repo, that's still "one repository"; it just avoids checking in a 2.4M-line file.

### Normalisation policy (must match build + runtime)
Your sample includes Polish diacritics and mixed casing is possible.

Pick one canonical representation, e.g.:
- Unicode NFC
- Uppercase (Polish locale-safe)
- One word per line, trimmed

Store this policy in `osps.fst.meta.json` and validate on startup.

## Phased implementation plan

### Phase 0 — Repository bootstrap (1–2 sessions)
Goal: buildable skeleton + shared conventions.

Deliverables:
- Gradle multi-module build (or Maven; Gradle tends to be nicer for multi-modules).
- Code style + static analysis (Spotless, Checkstyle, ErrorProne optional).
- Test stack:
  - JUnit 5 for engine/runtime
  - Spring Boot Test + WebFlux test for backend
  - Frontend: Vitest + React Testing Library
- Basic CI workflow:
  - build + unit tests
  - backend integration tests
  - frontend lint/test/build

Acceptance checks:
- `./gradlew test` green
- `apps/backend` starts and serves a health endpoint.

### Phase 1 — Dictionary pipeline (`osps.txt` → `osps.fst`) (core)
Goal: fast membership checks with a stable binary format.

Deliverables:
- `dictionary-tools` CLI
  - `compile --input data/osps.txt --output artifacts/osps.fst`
  - produces:
    - `artifacts/osps.fst`
    - `artifacts/osps.fst.meta.json` (format version, normalisation, wordcount, sha256 of input)
- `dictionary-runtime`
  - Dictionary interface: `boolean contains(String word)`
  - `FstDictionary` implementation: loads `osps.fst` once, thread-safe lookups

Tests:
- Use `osps_shortened.txt` in repo as test input to generate a tiny FST and verify lookups (including diacritics).

Acceptance checks:
- Building an FST from `osps_shortened.txt` works in CI.
- Runtime lookup is deterministic and fast for repeated calls.
- Startup fails fast if meta/version/normalisation mismatches.

Notes:
Your rules mention OSPS as authoritative and dictionary usage typically happens at challenge/check time.
That means you can support two modes later:
- strict validation at play time, or
- pending move + challenge window (more authentic).

### Phase 2 — Pure game engine (no networking yet)
Goal: implement the rules correctly, with heavy unit coverage.

Deliverables in `packages/game-engine`:
- Domain model
  - Board 15×15 with premium squares using your coordinate system (A–O, 1–15) and exact premium coordinates.
  - Tiles for Polish distribution + points (incl. blanks=0).
  - Bag, Rack, Player, GameState, PendingMove
- Move validation
  - First move covers H8 and uses ≥2 tiles.
  - Subsequent moves must connect (adjacent/cross) and be collinear/contiguous.
  - Premium squares apply only on newly placed tiles.
  - Build main word + cross-words; require newly formed words length ≥2.
- Scoring
  - Apply DL/TL per word for new tiles; DW/TW multiply cumulatively; blank stays 0 even on DL/TL.
  - +50 for using all 7 rack tiles.
- Challenge + pending move
  - "Move is pending until accepted/recorded; invalid challenge undoes move and player loses turn."
- Endgame scoring
  - Rack penalties; "went out" bonus.

Testing focus (this is where quality is won):
- Golden tests for:
  - multipliers with cross-words,
  - blank interactions (0 points but DW/TW applies to whole word),
  - first move constraints,
  - challenge undo correctness.
- Property-style tests:
  - undo returns state exactly,
  - score never changes without a legal committed move.

Acceptance checks:
- A suite of ~150+ unit tests around scoring/validation.
- Engine can run a full simulated game deterministically.

### Phase 3 — Backend (Spring Boot WebFlux) + realtime protocol
 
## Progress notes
- 2025-12-27: Enforced exchange limits (max 3 per player) and 7-tile bag minimum for exchanges; added backend tests.
- 2025-12-27: Switched to automatic move validation (no manual challenge) and removed challenge UI/tests.
- 2025-12-27: Reverted periodic/live syncs; relying on WS snapshots only with placement-safe resets.
Goal: authoritative server, real-time turns, reconnection.

Deliverables in `apps/backend`:
- WebFlux REST (non-realtime)
  - auth (can start simple: guest accounts, then JWT)
  - lobby: create/join room, list rooms
- WebSocket (realtime)
  - single WebSocket endpoint per server (or per game), with:
    - Commands: `PLAY_TILES`, `EXCHANGE`, `PASS`, `CHALLENGE`, `RESIGN`, `SYNC`
    - Events: `STATE_SNAPSHOT`, `MOVE_PROPOSED`, `MOVE_ACCEPTED`, `MOVE_REJECTED`, `TURN_ADVANCED`, `GAME_ENDED`
- State management
  - In-memory game registry for MVP
  - "snapshot on reconnect" support
- Dictionary integration
  - backend uses `dictionary-runtime` (`FstDictionary`) to validate during challenge/commit

Acceptance checks:
- Two browser clients can play a full game in one room.
- Refresh/reconnect restores game state.
- Server rejects illegal moves (never trust client).

### Phase 4 — Frontend (React + TS) playable MVP
Goal: solid UX for board play and challenges.

Deliverables in `apps/frontend`:
- Board UI (15×15 grid, premium colours/labels)
- Rack UI (drag/drop + click-to-place)
- Move preview (tiles placed, words formed, provisional score)
- Pending move / challenge banner (time window or "until next action", per your rules model)
- Lobby screens
- WebSocket client with reconnect + resync

Acceptance checks:
- Full playable loop (create room → join → play → challenge → game end).
- No desync: server snapshot always wins.

### Phase 5 — Persistence, scaling, and "real product" features
Goal: survive real users.

Deliverables:
- Postgres persistence:
  - users, finished games, moves/events (optional), ratings
- Redis (optional) for:
  - distributed game registry (if you ever run >1 backend instance)
  - pub/sub for game rooms
- Observability:
  - structured logs, metrics, tracing
- Anti-cheat basics:
  - server-side rack enforcement
  - rate limiting on WS commands
- Configurable rules toggles:
  - exchange allowed only if ≥7 tiles remain (toggle mentioned as common variant).

Acceptance checks:
- You can restart the backend and keep finished games.
- You can scale to multiple instances (if needed) without breaking rooms.

### Phase 6 — AI readiness (post-MVP)
Goal: enable strong computer opponents once the game loop is stable.

Deliverables:
- Dictionary traversal API (prefix walking) to support move generation.
- Move generator based on anchors + cross-checks + rack permutations.
- Scoring heuristics (rack leave values, board control).
- Search strategy (iterative deepening, move ordering, pruning).
- AI integration in backend (play vs AI mode).

Acceptance checks:
- AI can generate legal moves quickly for mid-game positions.
- AI strength can be tuned by depth/time budget.

## Progress updates
- 2025-12-27: Phase 0 completed (Gradle multi-module, tests, Checkstyle, CI, health endpoint).
- 2025-12-27: Coverage enforcement (min 75% line) and docs/quality added.
- 2025-12-27: Phase 1 tests expanded to validate dictionary metadata compatibility.
- 2025-12-27: Dictionary CLI tests added; JMH benchmark added for runtime lookups.
- 2025-12-27: Phase 1 completed (dictionary pipeline + tests + benchmark).
- 2025-12-27: Phase 2 completed (engine scaffolding + move validation + scoring + challenge flow).
- 2025-12-27: Phase 3 started (backend lobby and WebSocket scaffolding).
- 2025-12-27: Phase 3: lobby REST + WebSocket placeholder implemented with tests.
- 2025-12-27: Phase 3: WebSocket handler parsing + start/stop scripts + e2e smoke added.
- 2025-12-27: Phase 3: WebSocket commands wired to game engine (play/pass/exchange/challenge/resign), room broadcast hub, expanded snapshots, and backend coverage restored.
- 2025-12-27: Phase 3: REST command endpoint added for curl-based gameplay; parser shared with WebSocket; e2e curl flow verified.
- 2025-12-27: Phase 3: Per-player snapshots (rack visibility gated by player) and enum-based WS message types introduced; test structure standardized.
- 2025-12-27: Phase 4 started (Vite + React + TS scaffold, drag-and-drop board + rack, docs updated with frontend/back-end summary).
- 2025-12-27: Phase 4: frontend scripts added, lint + Vitest coverage configured, RTL component tests added, docs/quality updated.
- 2025-12-27: Phase 4: frontend test coverage enforced, App/api tests added, CI updated to run frontend lint/tests/coverage.
- 2025-12-27: Phase 4: frontend build verified and CI updated to run frontend build.
- 2025-12-27: Phase 4: staged tiles list added with removal control; Playwright manual check verified live UI connection.
- 2025-12-27: Phase 4: lobby UI added (room list/create/join/start) with REST wiring and tests; docs refreshed.
- 2025-12-27: Phase 4: lobby split into separate view, board layout squared, and drag/drop context fixed.
- 2025-12-27: Phase 4: lobby UX polished and game layout tightened to fit in one screen.
- 2025-12-27: Phase 6: AI move generator added with backend integration and lobby toggle for computer opponent.
- 2025-12-27: Phase 6: AI self-play simulation test added (opt-in via system property).
- 2025-12-27: Phase 3/6: AI now auto-challenges pending moves and resolves them during WS flow (tests added).

## Practical build/CI workflow in one repo

### Local dev
- `./gradlew :packages:dictionary-tools:compileOsps -PospsInput=...`
- `./gradlew :apps:backend:bootRun`
- `pnpm --filter frontend dev`

### CI (typical)
- Build dictionary artefact from `osps_shortened.txt` (fast, committed) for tests
