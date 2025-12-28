
interface GameActionsProps {
  canPlay: boolean;
  canReset: boolean;
  isSocketReady: boolean;
  onPlay: () => void;
  onReset: () => void;
  onPass: () => void;
  onResign: () => void;
}

export default function GameActions({
  canPlay,
  canReset,
  isSocketReady,
  onPlay,
  onReset,
  onPass,
  onResign
}: GameActionsProps) {
  return (
    <div className="info-section actions">
      <button type="button" onClick={onPlay} disabled={!canPlay || !isSocketReady}>Play tiles</button>
      <button type="button" onClick={onReset} disabled={!canReset}>Reset</button>
      <button type="button" onClick={onPass} disabled={!isSocketReady}>Pass</button>
      <button type="button" onClick={onResign} disabled={!isSocketReady}>Resign</button>
    </div>
  );
}
