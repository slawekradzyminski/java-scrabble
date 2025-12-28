import type { BoardTile, RackTile, WsMessage } from '../../types';

export type PlacementTile = BoardTile & { rackIndex?: number };
export type PlacementState = Record<string, PlacementTile>;

export type ActiveTile = RackTile & { rackIndex: number };

export type EventLogEntry = {
  id: number;
  time: string;
  type: WsMessage['type'];
  summary: string;
};
