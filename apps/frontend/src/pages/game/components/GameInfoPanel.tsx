import type { GameSnapshot } from '../../../types';

interface GameInfoPanelProps {
  snapshot: GameSnapshot;
  player: string;
  error: string | null;
  busy: boolean;
  isSocketReady: boolean;
  onStartGame: () => void;
  onOpenBag: () => void;
}

export default function GameInfoPanel({
  snapshot,
  player,
  error,
  busy,
  isSocketReady,
  onStartGame,
  onOpenBag
}: GameInfoPanelProps) {
  return (
    <div className="info-section">
      <div className="info-header">
        <h2>Game</h2>
        {snapshot.status === 'not_started' && (
          <button type="button" onClick={onStartGame} disabled={busy || !isSocketReady}>
            Start
          </button>
        )}
      </div>
      {error && <p className="error">{error}</p>}
      <div className="info-meta">
        <div>
          <span>Player</span>
          <strong>{player || '—'}</strong>
        </div>
        <div>
          <span>Bag</span>
          <strong>
            <button
              type="button"
              className="bag-button"
              onClick={onOpenBag}
              disabled={!isSocketReady}
            >
              {snapshot.bagCount}
            </button>
          </strong>
        </div>
        <div className="debug-meta">
          <span>Debug</span>
          <strong>
            v{snapshot.stateVersion ?? 0} · tiles {snapshot.boardTiles}
          </strong>
        </div>
        {snapshot.winner && (
          <div>
            <span>Winner</span>
            <strong>{snapshot.winner}</strong>
          </div>
        )}
      </div>
    </div>
  );
}
