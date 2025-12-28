import { useMemo } from 'react';
import type { GameSnapshot } from '../../../types';
import type { PlacementState } from '../types';

export function useRackTiles(snapshot: GameSnapshot, player: string, placements: PlacementState) {
  return useMemo(() => {
    const currentPlayer = snapshot.players.find(p => p.name === player);
    const serverRackTiles = currentPlayer?.rack ?? [];
    const filled = serverRackTiles.map((tile, index) => {
      const staging = placements[`rack-${index}`];
      return staging ? { letter: null, points: 0, blank: false } : tile;
    });
    while (filled.length < 7) {
      filled.push({ letter: null, points: 0, blank: false });
    }
    return filled.slice(0, 7);
  }, [snapshot.players, player, placements]);
}
