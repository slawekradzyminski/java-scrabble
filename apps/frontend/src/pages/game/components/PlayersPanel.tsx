import PlayerList from '../../../components/PlayerList';
import type { PlayerSnapshot } from '../../../types';

interface PlayersPanelProps {
  players: PlayerSnapshot[];
  currentIndex: number | null;
}

export default function PlayersPanel({ players, currentIndex }: PlayersPanelProps) {
  return (
    <div className="info-section">
      <h3>Players</h3>
      <PlayerList players={players} currentIndex={currentIndex} />
    </div>
  );
}
