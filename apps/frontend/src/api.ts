import type { GameSnapshot, RoomSummary, WsMessage } from './types';

export type SnapshotListener = (snapshot: GameSnapshot) => void;
export type EventListener = (message: WsMessage) => void;
export type ConnectionStateListener = (state: ConnectionState) => void;

export type ConnectionState = 'disconnected' | 'connecting' | 'connected' | 'reconnecting';

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

export async function fetchSnapshot(roomId: string, player?: string): Promise<GameSnapshot> {
  const params = new URLSearchParams();
  if (player) {
    params.set('player', player);
  }
  const query = params.toString();
  const url = `${baseUrl()}/api/rooms/${roomId}/game/state${query ? `?${query}` : ''}`;
  return fetchJson<GameSnapshot>(url);
}

interface GameSocketConfig {
  reconnectDelay?: number;
  maxReconnectDelay?: number;
  reconnectBackoffMultiplier?: number;
  syncTimeoutMs?: number;
}

const DEFAULT_CONFIG: Required<GameSocketConfig> = {
  reconnectDelay: 1000,
  maxReconnectDelay: 30000,
  reconnectBackoffMultiplier: 1.5,
  syncTimeoutMs: 5000
};

export class GameSocket {
  private socket: WebSocket | null = null;
  private snapshotListener: SnapshotListener | null = null;
  private eventListener: EventListener | null = null;
  private connectionStateListener: ConnectionStateListener | null = null;
  private heartbeat: number | null = null;
  private reconnectTimeout: number | null = null;
  private syncFallbackTimeout: number | null = null;
  private reconnectSnapshotTimeout: number | null = null;

  private roomId: string | null = null;
  private player: string | null = null;
  private connectionState: ConnectionState = 'disconnected';
  private reconnectAttempts = 0;
  private shouldReconnect = false;
  private hasSyncedOnce = false;

  private config: Required<GameSocketConfig>;

  constructor(config?: GameSocketConfig) {
    this.config = { ...DEFAULT_CONFIG, ...config };
  }

  connect(roomId: string, player: string) {
    this.roomId = roomId;
    this.player = player;
    this.shouldReconnect = true;
    this.reconnectAttempts = 0;
    this.hasSyncedOnce = false;
    this.doConnect();
  }

  private doConnect() {
    if (!this.roomId || !this.player) {
      return;
    }

    this.clearReconnectTimeout();
    this.clearReconnectSnapshotTimeout();
    this.closeSocket();

    this.hasSyncedOnce = false;
    this.setConnectionState(this.reconnectAttempts > 0 ? 'reconnecting' : 'connecting');

    const base = new URL(baseUrl());
    base.protocol = base.protocol === 'https:' ? 'wss:' : 'ws:';
    const url = new URL('/ws', base);
    url.searchParams.set('roomId', this.roomId);
    url.searchParams.set('player', this.player);

    if (import.meta.env.DEV) {
      console.info('WS connect', url.toString(), `attempt=${this.reconnectAttempts}`);
    }

    this.socket = new WebSocket(url.toString());

    this.socket.onmessage = (event) => {
      const parsed = JSON.parse(event.data) as WsMessage;
      if (import.meta.env.DEV) {
        console.info('WS message', parsed.type);
      }
      if (parsed.type === 'STATE_SNAPSHOT') {
        this.clearSyncFallbackTimeout();
        this.hasSyncedOnce = true;
        this.snapshotListener?.(parsed.payload as unknown as GameSnapshot);
      }
      this.eventListener?.(parsed);
    };

    this.socket.onopen = () => {
      if (import.meta.env.DEV) {
        console.info('WS open');
      }
      this.reconnectAttempts = 0;
      this.setConnectionState('connected');
      this.requestSync();
      this.startHeartbeat();
      this.scheduleSyncFallback();
      this.scheduleReconnectSnapshot();
    };

    this.socket.onclose = (event) => {
      if (import.meta.env.DEV) {
        console.info('WS close', event.code, event.reason);
      }
      this.stopHeartbeat();
      this.clearSyncFallbackTimeout();
      this.clearReconnectSnapshotTimeout();

      if (this.shouldReconnect) {
        this.setConnectionState('reconnecting');
        this.scheduleReconnect();
      } else {
        this.setConnectionState('disconnected');
      }
    };

    this.socket.onerror = (error) => {
      if (import.meta.env.DEV) {
        console.info('WS error', error);
      }
    };
  }

  requestSync() {
    if (!this.player) {
      return;
    }
    this.send({ type: 'SYNC', payload: { player: this.player } });
  }

  private scheduleSyncFallback() {
    this.clearSyncFallbackTimeout();
    if (!this.roomId || !this.player) {
      return;
    }

    const roomId = this.roomId;
    const player = this.player;

    this.syncFallbackTimeout = window.setTimeout(async () => {
      if (this.hasSyncedOnce) {
        return;
      }
      if (import.meta.env.DEV) {
        console.info('WS sync fallback: fetching REST snapshot');
      }
      try {
        const snapshot = await fetchSnapshot(roomId, player);
        if (!this.hasSyncedOnce && this.roomId === roomId) {
          this.hasSyncedOnce = true;
          this.snapshotListener?.(snapshot);
        }
      } catch (error) {
        if (import.meta.env.DEV) {
          console.warn('REST snapshot fallback failed', error);
        }
      }
    }, this.config.syncTimeoutMs);
  }

  private clearSyncFallbackTimeout() {
    if (this.syncFallbackTimeout !== null) {
      window.clearTimeout(this.syncFallbackTimeout);
      this.syncFallbackTimeout = null;
    }
  }

  private scheduleReconnectSnapshot() {
    this.clearReconnectSnapshotTimeout();
    if (this.connectionState !== 'reconnecting') {
      return;
    }
    const roomId = this.roomId;
    const player = this.player;
    this.reconnectSnapshotTimeout = window.setTimeout(async () => {
      if (this.hasSyncedOnce || !roomId || !player) {
        return;
      }
      if (import.meta.env.DEV) {
        console.info('WS reconnect snapshot: fetching REST snapshot');
      }
      try {
        const snapshot = await fetchSnapshot(roomId, player);
        if (!this.hasSyncedOnce && this.roomId === roomId) {
          this.hasSyncedOnce = true;
          this.snapshotListener?.(snapshot);
        }
      } catch (error) {
        if (import.meta.env.DEV) {
          console.warn('Reconnect snapshot failed', error);
        }
      }
    }, 800);
  }

  private clearReconnectSnapshotTimeout() {
    if (this.reconnectSnapshotTimeout !== null) {
      window.clearTimeout(this.reconnectSnapshotTimeout);
      this.reconnectSnapshotTimeout = null;
    }
  }

  private scheduleReconnect() {
    this.clearReconnectTimeout();

    const delay = Math.min(
      this.config.reconnectDelay * Math.pow(this.config.reconnectBackoffMultiplier, this.reconnectAttempts),
      this.config.maxReconnectDelay
    );

    if (import.meta.env.DEV) {
      console.info(`WS reconnect scheduled in ${delay}ms`);
    }

    this.reconnectTimeout = window.setTimeout(() => {
      this.reconnectAttempts++;
      this.doConnect();
    }, delay);
  }

  private clearReconnectTimeout() {
    if (this.reconnectTimeout !== null) {
      window.clearTimeout(this.reconnectTimeout);
      this.reconnectTimeout = null;
    }
  }

  private startHeartbeat() {
    this.stopHeartbeat();
    this.heartbeat = window.setInterval(() => {
      this.send({ type: 'PING', payload: {} });
    }, 15000);
  }

  private stopHeartbeat() {
    if (this.heartbeat !== null) {
      window.clearInterval(this.heartbeat);
      this.heartbeat = null;
    }
  }

  private closeSocket() {
    if (this.socket) {
      this.socket.onopen = null;
      this.socket.onclose = null;
      this.socket.onerror = null;
      this.socket.onmessage = null;
      if (this.socket.readyState === WebSocket.OPEN || this.socket.readyState === WebSocket.CONNECTING) {
        this.socket.close();
      }
      this.socket = null;
    }
  }

  private setConnectionState(state: ConnectionState) {
    if (this.connectionState !== state) {
      this.connectionState = state;
      this.connectionStateListener?.(state);
    }
  }

  send(message: { type: string; payload: Record<string, unknown> }) {
    if (!this.socket || this.socket.readyState !== WebSocket.OPEN) {
      return;
    }
    if (import.meta.env.DEV) {
      console.info('WS send', message.type);
    }
    this.socket.send(JSON.stringify(message));
  }

  onSnapshot(listener: SnapshotListener) {
    this.snapshotListener = listener;
  }

  onEvent(listener: EventListener) {
    this.eventListener = listener;
  }

  onConnectionState(listener: ConnectionStateListener) {
    this.connectionStateListener = listener;
  }

  getConnectionState(): ConnectionState {
    return this.connectionState;
  }

  isConnected(): boolean {
    return this.connectionState === 'connected';
  }

  getCurrentRoomId(): string | null {
    return this.roomId;
  }

  getCurrentPlayer(): string | null {
    return this.player;
  }

  disconnect() {
    this.shouldReconnect = false;
    this.clearReconnectTimeout();
    this.clearSyncFallbackTimeout();
    this.clearReconnectSnapshotTimeout();
    this.stopHeartbeat();
    this.closeSocket();
    this.setConnectionState('disconnected');
    this.roomId = null;
    this.player = null;
    this.hasSyncedOnce = false;
  }
}
