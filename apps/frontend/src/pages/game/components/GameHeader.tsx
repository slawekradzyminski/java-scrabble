
interface GameHeaderProps {
  expanded: boolean;
  onToggle: () => void;
  onLeave: () => void;
}

export default function GameHeader({ expanded, onToggle, onLeave }: GameHeaderProps) {
  if (expanded) {
    return (
      <header className="topbar topbar--expanded">
        <div>
          <div className="eyebrow">SCRABBLE LIVE</div>
          <h1>Realtime Board</h1>
        </div>
        <div className="topbar-actions">
          <button type="button" className="topbar-toggle" onClick={onToggle}>
            Hide header
          </button>
          <button type="button" className="leave-btn" onClick={onLeave}>
            Leave Room
          </button>
        </div>
      </header>
    );
  }

  return (
    <div className="compact-header">
      <button type="button" className="topbar-toggle" onClick={onToggle}>
        Show header
      </button>
      <button type="button" className="leave-btn" onClick={onLeave}>
        Leave Room
      </button>
    </div>
  );
}
