import type { GameSnapshot, RoomSummary, WsMessage } from './types';

export type SnapshotListener = (snapshot: GameSnapshot) => void;
export type EventListener = (message: WsMessage) => void;

const baseUrl = () => import.meta.env.VITE_BACKEND_URL ?? 'http://localhost:8080';

type FetchInit = Parameters<typeof fetch>[1];

async function fetchJson<T>(url: string, init?: FetchInit): Promise<T> {
  const response = await fetch(url, init);
  if (!response.ok) {
    const message = await response.text();
    throw new Error(message || `Request failed: ${response.status}`);
  }
  return response.json() as Promise<T>;
}

export async function listRooms(): Promise<RoomSummary[]> {
  return fetchJson<RoomSummary[]>(`${baseUrl()}/api/rooms`);
}

export async function createRoom(name: string, owner: string, ai?: boolean): Promise<RoomSummary> {
  return fetchJson<RoomSummary>(`${baseUrl()}/api/rooms`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ name, owner, ai: Boolean(ai) })
  });
}

export async function joinRoom(roomId: string, player: string): Promise<RoomSummary> {
  return fetchJson<RoomSummary>(`${baseUrl()}/api/rooms/${roomId}/join`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ player })
  });
}

export async function startGame(roomId: string): Promise<GameSnapshot> {
  return fetchJson<GameSnapshot>(`${baseUrl()}/api/rooms/${roomId}/game/start`, {
    method: 'POST'
  });
}

export class GameSocket {
  private socket: WebSocket | null = null;
  private snapshotListener: SnapshotListener | null = null;
  private eventListener: EventListener | null = null;
  private heartbeat: number | null = null;

  connect(roomId: string, player: string) {
    const base = new URL(baseUrl());
    base.protocol = base.protocol === 'https:' ? 'wss:' : 'ws:';
    const url = new URL('/ws', base);
    url.searchParams.set('roomId', roomId);
    url.searchParams.set('player', player);
    this.socket = new WebSocket(url.toString());

    this.socket.onmessage = (event) => {
      const parsed = JSON.parse(event.data) as WsMessage;
      if (parsed.type === 'STATE_SNAPSHOT') {
        this.snapshotListener?.(parsed.payload as unknown as GameSnapshot);
      }
      this.eventListener?.(parsed);
    };

    this.socket.onopen = () => {
      this.send({ type: 'SYNC', payload: { player } });
      this.heartbeat = window.setInterval(() => this.send({ type: 'PING', payload: {} }), 15000);
    };

    this.socket.onclose = () => {
      if (this.heartbeat) {
        window.clearInterval(this.heartbeat);
        this.heartbeat = null;
      }
    };
  }

  send(message: { type: string; payload: Record<string, unknown> }) {
    if (!this.socket || this.socket.readyState !== WebSocket.OPEN) {
      return;
    }
    this.socket.send(JSON.stringify(message));
  }

  onSnapshot(listener: SnapshotListener) {
    this.snapshotListener = listener;
  }

  onEvent(listener: EventListener) {
    this.eventListener = listener;
  }

  disconnect() {
    if (this.socket) {
      this.socket.close();
    }
  }
}
