# Frontend (Phase 4)

## Stack choices
- Vite + React + TypeScript for fast iteration and minimal config.
- `@dnd-kit/core` for drag-and-drop (lightweight, no HTML5 drag quirks).
- Styling via plain CSS to keep bundle size small and allow Scrabble-like theming.

## Run
From repo root:
```
cd apps/frontend
npm install
npm run dev
```

Optional backend URL override:
```
VITE_BACKEND_URL=http://localhost:8080 npm run dev
```

## Checks
```
npm run lint
npm run test
npm run test:coverage
```

## UX notes
- Lobby is a dedicated view for create/join/room selection (no board shown).
- After connecting, the game view fits on a single screen with board + rack stacked and a compact sidebar.
- Start games from the Room panel after you are connected.
- Rack lives in the sidebar; drag tiles from rack to board cells.
- Filter rooms by name or id in the lobby list, then join directly.
- Game URLs include the room id and a name slug (ex: `/room/12-scrabble-night`).
- Use action buttons to play, pass, challenge, or resign.

## Drag & drop model
- Rack tiles are draggable via `@dnd-kit/core`.
- Board cells are droppable targets keyed by coordinate (e.g. `H8`).
- Local placements are staged client-side until “Play tiles”.
- Blank tiles prompt for a letter on drop.

## Next improvements
- Click-to-place fallback for touch.
- Move preview score and word list.
- Better reconnection UI and error toasts.
