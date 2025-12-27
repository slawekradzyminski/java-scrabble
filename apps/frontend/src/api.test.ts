import { GameSocket } from './api';

class MockWebSocket {
  static OPEN = 1;
  readyState = MockWebSocket.OPEN;
  onmessage: ((event: { data: string }) => void) | null = null;
  onopen: (() => void) | null = null;
  onclose: (() => void) | null = null;
  sent: string[] = [];

  constructor(public url: string) {
    setTimeout(() => this.onopen?.(), 0);
  }

  send(data: string) {
    this.sent.push(data);
  }

  close() {
    this.onclose?.();
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
    // given
    const socket = new GameSocket();
    const snapshots: unknown[] = [];
    socket.onSnapshot((snapshot) => snapshots.push(snapshot));

    // when
    socket.connect('room-1', 'Alice');

    // then
    await new Promise((resolve) => setTimeout(resolve, 10));
    const ws = (socket as unknown as { socket: MockWebSocket }).socket;
    expect(ws.url).toContain('roomId=room-1');
    expect(ws.url).toContain('player=Alice');
    expect(ws.sent.join('')).toContain('SYNC');
  });

  it('routes snapshot messages to listener', async () => {
    // given
    const socket = new GameSocket();
    const snapshots: unknown[] = [];
    socket.onSnapshot((snapshot) => snapshots.push(snapshot));

    // when
    socket.connect('room-2', 'Bob');
    await new Promise((resolve) => setTimeout(resolve, 10));
    const ws = (socket as unknown as { socket: MockWebSocket }).socket;
    ws.onmessage?.({ data: JSON.stringify({ type: 'STATE_SNAPSHOT', payload: { roomId: 'room-2' } }) });

    // then
    expect(snapshots).toHaveLength(1);
  });

  it('calls event listener for non-snapshot messages', async () => {
    // given
    const socket = new GameSocket();
    const events: unknown[] = [];
    socket.onEvent((message) => events.push(message));

    // when
    socket.connect('room-3', 'Cara');
    await new Promise((resolve) => setTimeout(resolve, 10));
    const ws = (socket as unknown as { socket: MockWebSocket }).socket;
    ws.onmessage?.({ data: JSON.stringify({ type: 'MOVE_PROPOSED', payload: { score: 5 } }) });

    // then
    expect(events).toHaveLength(1);
  });

  it('sends heartbeat ping', () => {
    // given
    vi.useFakeTimers();
    const socket = new GameSocket();

    // when
    socket.connect('room-4', 'Dana');
    vi.runOnlyPendingTimers();
    vi.advanceTimersByTime(16000);
    const ws = (socket as unknown as { socket: MockWebSocket }).socket;

    // then
    expect(ws.sent.join('')).toContain('PING');
    vi.useRealTimers();
  });

  it('disconnects safely without open socket', () => {
    // given
    const socket = new GameSocket();

    // when
    socket.disconnect();

    // then
    expect(true).toBe(true);
  });
});
