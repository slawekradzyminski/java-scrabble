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
- Connect with room id + player name.
- Drag tiles from rack to board cells.
- Use action buttons to play, pass, challenge, or resign.

## Drag & drop model
- Rack tiles are draggable via `@dnd-kit/core`.
- Board cells are droppable targets keyed by coordinate (e.g. `H8`).
- Local placements are staged client-side until “Play tiles”.
- Blank tiles prompt for a letter on drop.

## Next improvements
- Blank tile letter picker.
- Click-to-place fallback for touch.
- Move preview score and word list.
- Better reconnection UI and error toasts.
