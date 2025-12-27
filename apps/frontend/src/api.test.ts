import { createRoom, fetchSnapshot, GameSocket, joinRoom, listRooms, startGame } from './api';

class MockWebSocket {
  static OPEN = 1;
  static CONNECTING = 0;
  static CLOSED = 3;
  readyState = MockWebSocket.OPEN;
  onmessage: ((event: { data: string }) => void) | null = null;
  onopen: (() => void) | null = null;
  onclose: ((event: { code: number; reason: string }) => void) | null = null;
  onerror: ((error: unknown) => void) | null = null;
  sent: string[] = [];

  constructor(public url: string) {
    setTimeout(() => this.onopen?.(), 0);
  }

  send(data: string) {
    this.sent.push(data);
  }

  close() {
    this.readyState = MockWebSocket.CLOSED;
    this.onclose?.({ code: 1000, reason: 'normal' });
  }
}

describe('GameSocket', () => {
  const originalWebSocket = window.WebSocket;

  beforeEach(() => {
    window.WebSocket = MockWebSocket as unknown as typeof WebSocket;
  });

  afterEach(() => {
    window.WebSocket = originalWebSocket;
  });

  it('connects and sends sync', async () => {
    const socket = new GameSocket();
    const snapshots: unknown[] = [];
    socket.onSnapshot((snapshot) => snapshots.push(snapshot));

    socket.connect('room-1', 'Alice');

    await new Promise((resolve) => setTimeout(resolve, 10));
    const ws = (socket as unknown as { socket: MockWebSocket }).socket;
    expect(ws.url).toContain('roomId=room-1');
    expect(ws.url).toContain('player=Alice');
    expect(ws.sent.join('')).toContain('SYNC');
  });

  it('routes snapshot messages to listener', async () => {
    const socket = new GameSocket();
    const snapshots: unknown[] = [];
    socket.onSnapshot((snapshot) => snapshots.push(snapshot));

    socket.connect('room-2', 'Bob');
    await new Promise((resolve) => setTimeout(resolve, 10));
    const ws = (socket as unknown as { socket: MockWebSocket }).socket;
    ws.onmessage?.({ data: JSON.stringify({ type: 'STATE_SNAPSHOT', payload: { roomId: 'room-2' } }) });

    expect(snapshots).toHaveLength(1);
  });

  it('calls event listener for non-snapshot messages', async () => {
    const socket = new GameSocket();
    const events: unknown[] = [];
    socket.onEvent((message) => events.push(message));

    socket.connect('room-3', 'Cara');
    await new Promise((resolve) => setTimeout(resolve, 10));
    const ws = (socket as unknown as { socket: MockWebSocket }).socket;
    ws.onmessage?.({ data: JSON.stringify({ type: 'MOVE_PROPOSED', payload: { score: 5 } }) });

    expect(events).toHaveLength(1);
  });

  it('sends heartbeat ping', () => {
    vi.useFakeTimers();
    const socket = new GameSocket();

    socket.connect('room-4', 'Dana');
    vi.runOnlyPendingTimers();
    vi.advanceTimersByTime(16000);
    const ws = (socket as unknown as { socket: MockWebSocket }).socket;

    expect(ws.sent.join('')).toContain('PING');
    vi.useRealTimers();
  });

  it('disconnects safely without open socket', () => {
    const socket = new GameSocket();

    socket.disconnect();

    expect(true).toBe(true);
  });

  it('tracks connection state', async () => {
    const socket = new GameSocket();
    const states: string[] = [];
    socket.onConnectionState((state) => states.push(state));

    socket.connect('room-5', 'Eve');
    await new Promise((resolve) => setTimeout(resolve, 10));

    expect(states).toContain('connecting');
    expect(states).toContain('connected');
    expect(socket.isConnected()).toBe(true);
  });

  it('exposes current room and player', async () => {
    const socket = new GameSocket();
    socket.connect('room-6', 'Frank');
    await new Promise((resolve) => setTimeout(resolve, 10));

    expect(socket.getCurrentRoomId()).toBe('room-6');
    expect(socket.getCurrentPlayer()).toBe('Frank');
  });

  it('clears state on disconnect', async () => {
    const socket = new GameSocket();
    socket.connect('room-7', 'Grace');
    await new Promise((resolve) => setTimeout(resolve, 10));

    socket.disconnect();

    expect(socket.getCurrentRoomId()).toBeNull();
    expect(socket.getCurrentPlayer()).toBeNull();
    expect(socket.isConnected()).toBe(false);
  });

  it('allows manual sync request', async () => {
    const socket = new GameSocket();
    socket.connect('room-8', 'Henry');
    await new Promise((resolve) => setTimeout(resolve, 10));
    const ws = (socket as unknown as { socket: MockWebSocket }).socket;
    const initialSent = ws.sent.length;

    socket.requestSync();

    expect(ws.sent.length).toBeGreaterThan(initialSent);
    expect(ws.sent[ws.sent.length - 1]).toContain('SYNC');
  });
});

describe('lobby api', () => {
  const originalFetch = globalThis.fetch;

  beforeEach(() => {
    globalThis.fetch = vi.fn();
  });

  afterEach(() => {
    globalThis.fetch = originalFetch;
  });

  it('lists rooms', async () => {
    const response = [{ id: '1', name: 'Room 1', players: [] }];
    const fetchMock = globalThis.fetch as unknown as ReturnType<typeof vi.fn>;
    fetchMock.mockResolvedValue({
      ok: true,
      json: async () => response
    });

    const rooms = await listRooms();

    expect(rooms).toEqual(response);
  });

  it('creates a room', async () => {
    const response = { id: '2', name: 'Room 2', players: ['Alice'] };
    const fetchMock = globalThis.fetch as unknown as ReturnType<typeof vi.fn>;
    fetchMock.mockResolvedValue({
      ok: true,
      json: async () => response
    });

    const created = await createRoom('Room 2', 'Alice');

    expect(created).toEqual(response);
    expect(fetchMock).toHaveBeenCalledWith(expect.stringContaining('/api/rooms'), expect.objectContaining({
      method: 'POST',
      body: JSON.stringify({ name: 'Room 2', owner: 'Alice', ai: false })
    }));
  });

  it('creates a room with ai enabled', async () => {
    const response = { id: '5', name: 'Room 5', players: ['Alice', 'Computer'] };
    const fetchMock = globalThis.fetch as unknown as ReturnType<typeof vi.fn>;
    fetchMock.mockResolvedValue({
      ok: true,
      json: async () => response
    });

    const created = await createRoom('Room 5', 'Alice', true);

    expect(created).toEqual(response);
    expect(fetchMock).toHaveBeenCalledWith(expect.stringContaining('/api/rooms'), expect.objectContaining({
      method: 'POST',
      body: JSON.stringify({ name: 'Room 5', owner: 'Alice', ai: true })
    }));
  });

  it('joins a room', async () => {
    const response = { id: '3', name: 'Room 3', players: ['Bob'] };
    const fetchMock = globalThis.fetch as unknown as ReturnType<typeof vi.fn>;
    fetchMock.mockResolvedValue({
      ok: true,
      json: async () => response
    });

    const joined = await joinRoom('3', 'Bob');

    expect(joined).toEqual(response);
    expect(fetchMock).toHaveBeenCalledWith(expect.stringContaining('/api/rooms/3/join'), expect.objectContaining({
      method: 'POST'
    }));
  });

  it('starts a game', async () => {
    const response = { roomId: '4', status: 'active' };
    const fetchMock = globalThis.fetch as unknown as ReturnType<typeof vi.fn>;
    fetchMock.mockResolvedValue({
      ok: true,
      json: async () => response
    });

    const snapshot = await startGame('4');

    expect(snapshot).toEqual(response);
    expect(fetchMock).toHaveBeenCalledWith(expect.stringContaining('/api/rooms/4/game/start'), expect.objectContaining({
      method: 'POST'
    }));
  });

  it('fetches snapshot', async () => {
    const response = { roomId: '6', status: 'active', players: [] };
    const fetchMock = globalThis.fetch as unknown as ReturnType<typeof vi.fn>;
    fetchMock.mockResolvedValue({
      ok: true,
      json: async () => response
    });

    const snapshot = await fetchSnapshot('6', 'Alice');

    expect(snapshot).toEqual(response);
    const calledUrl = fetchMock.mock.calls[0][0] as string;
    expect(calledUrl).toContain('/api/rooms/6/game/state');
    expect(calledUrl).toContain('player=Alice');
  });

  it('fetches snapshot without player', async () => {
    const response = { roomId: '7', status: 'active', players: [] };
    const fetchMock = globalThis.fetch as unknown as ReturnType<typeof vi.fn>;
    fetchMock.mockResolvedValue({
      ok: true,
      json: async () => response
    });

    const snapshot = await fetchSnapshot('7');

    expect(snapshot).toEqual(response);
    const calledUrl = fetchMock.mock.calls[0][0] as string;
    expect(calledUrl).toContain('/api/rooms/7/game/state');
    expect(calledUrl).not.toContain('player=');
  });

  it('throws when response is not ok', async () => {
    const fetchMock = globalThis.fetch as unknown as ReturnType<typeof vi.fn>;
    fetchMock.mockResolvedValue({
      ok: false,
      status: 500,
      text: async () => 'boom'
    });

    const action = () => listRooms();

    await expect(action()).rejects.toThrow('boom');
  });
});
