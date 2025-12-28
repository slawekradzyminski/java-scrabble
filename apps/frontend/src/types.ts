export type WsMessageType =
  | 'STATE_SNAPSHOT'
  | 'MOVE_PROPOSED'
  | 'MOVE_ACCEPTED'
  | 'MOVE_REJECTED'
  | 'TURN_ADVANCED'
  | 'PASS'
  | 'EXCHANGE'
  | 'GAME_ENDED'
  | 'ERROR'
  | 'PONG';

export interface WsMessage {
  type: WsMessageType;
  payload: Record<string, unknown>;
}

export interface RoomSummary {
  id: string;
  name: string;
  players: string[];
}

export interface RackTile {
  letter: string | null;
  points: number;
  blank: boolean;
}

export interface PlayerSnapshot {
  name: string;
  score: number;
  rackSize: number;
  rackCapacity: number;
  rack: RackTile[];
}

export interface BoardTile {
  coordinate: string;
  letter: string | null;
  points: number;
  blank: boolean;
  assignedLetter: string;
}

export interface PendingMove {
  playerIndex: number;
  score: number;
  words: Array<{ text: string; coordinates: string[] }>;
  placements: Array<{ coordinate: string; assignedLetter: string }>;
}

export interface HistoryEntry {
  eventId: number;
  type: WsMessageType;
  payload: Record<string, unknown>;
  time: string;
}

export interface GameSnapshot {
  roomId: string;
  status: string;
  players: PlayerSnapshot[];
  bagCount: number;
  boardTiles: number;
  board: BoardTile[];
  currentPlayerIndex: number | null;
  pendingMove: boolean;
  pending: PendingMove | null;
  winner: string | null;
  history?: HistoryEntry[];
  stateVersion?: number;
  lastEventId?: number;
  serverTime?: string;
}

export interface GameEventPage {
  events: HistoryEntry[];
  lastEventId: number;
}
