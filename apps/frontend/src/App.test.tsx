import React from 'react';
import { act, fireEvent, render, screen, waitFor } from '@testing-library/react';
let App: typeof import('./App').default;

const connectMock = vi.fn();
const sendMock = vi.fn();
const listRoomsMock = vi.fn();
const joinRoomMock = vi.fn();
const createRoomMock = vi.fn();
const startGameMock = vi.fn();
let snapshotHandler: ((snapshot: unknown) => void) | null = null;

const waitForLobbyReady = async () => {
  await waitFor(() => expect(listRoomsMock).toHaveBeenCalled());
  await waitFor(() => {
    expect(screen.getByRole('button', { name: 'Refresh' })).toBeEnabled();
  });
};

vi.mock('./api', () => {
  return {
    __esModule: true,
    listRooms: () => listRoomsMock(),
    joinRoom: (...args: unknown[]) => joinRoomMock(...args),
    createRoom: (...args: unknown[]) => createRoomMock(...args),
    startGame: (...args: unknown[]) => startGameMock(...args),
    GameSocket: class {
      onSnapshot(handler: (snapshot: unknown) => void) {
        snapshotHandler = handler;
      }
      onEvent() {}
      disconnect() {}
      connect(roomId: string, player: string) {
        connectMock(roomId, player);
      }
      send(message: unknown) {
        sendMock(message);
      }
    }
  };
});

describe('App', () => {
  beforeEach(async () => {
    await vi.resetModules();
    window.localStorage.clear();
    connectMock.mockClear();
    sendMock.mockClear();
    listRoomsMock.mockReset().mockImplementation(() => new Promise((resolve) => {
      setTimeout(() => resolve([{ id: 'room-1', name: 'Room 1', players: [] }]), 0);
    }));
    joinRoomMock.mockReset().mockResolvedValue({ id: 'room-1', name: 'Room', players: ['Alice'] });
    createRoomMock.mockReset().mockResolvedValue({ id: 'room-9', name: 'Room', players: ['Alice'] });
    startGameMock.mockReset().mockResolvedValue({});
    App = (await import('./App')).default;
  });

  it('renders connect fields and calls connect', async () => {
    // given
    render(<App />);
    await waitForLobbyReady();
    const playerInput = screen.getByPlaceholderText('Player name');
    const joinButton = screen.getByRole('button', { name: 'Join' });

    // when
    fireEvent.change(playerInput, { target: { value: 'Alice' } });
    await waitFor(() => expect(playerInput).toHaveValue('Alice'));
    await waitFor(() => expect(joinButton).toBeEnabled());
    fireEvent.click(joinButton);

    // then
    await waitFor(() => {
      expect(joinRoomMock).toHaveBeenCalledWith('room-1', 'Alice');
      expect(connectMock).toHaveBeenCalledWith('room-1', 'Alice');
    });
  });

  it('does not connect without room or player', async () => {
    // given
    render(<App />);
    await waitForLobbyReady();

    // when
    const joinButton = screen.getByRole('button', { name: 'Join' });

    // then
    expect(joinButton).toBeDisabled();
    expect(connectMock).not.toHaveBeenCalled();
    expect(joinRoomMock).not.toHaveBeenCalled();
  });

  it('ignores play tiles when not connected', async () => {
    // given
    render(<App />);
    await waitForLobbyReady();

    // when
    const playButton = screen.queryByRole('button', { name: 'Play tiles' });

    // then
    expect(playButton).not.toBeInTheDocument();
    expect(sendMock).not.toHaveBeenCalled();
  });

  it('sends commands from action buttons', async () => {
    // given
    render(<App />);
    await waitForLobbyReady();
    fireEvent.change(screen.getByPlaceholderText('Player name'), { target: { value: 'Alice' } });
    await waitFor(() => expect(screen.getByPlaceholderText('Player name')).toHaveValue('Alice'));
    await waitFor(() => expect(screen.getByRole('button', { name: 'Join' })).toBeEnabled());
    fireEvent.click(screen.getByRole('button', { name: 'Join' }));
    await waitFor(() => expect(connectMock).toHaveBeenCalled());
    act(() => {
      snapshotHandler?.({
        roomId: 'room-1',
        status: 'active',
        players: [
          {
            name: 'Alice',
            score: 0,
            rackSize: 1,
            rackCapacity: 7,
            rack: [{ letter: 'A', points: 1, blank: false }]
          }
        ],
        bagCount: 95,
        boardTiles: 0,
        board: [],
        currentPlayerIndex: 0,
        pendingMove: false,
        pending: null,
        winner: null
      });
    });

    // when
    fireEvent.click(screen.getByRole('button', { name: 'Pass' }));
    fireEvent.click(screen.getByRole('button', { name: 'Challenge' }));
    fireEvent.click(screen.getByRole('button', { name: 'Resign' }));
    act(() => {
      fireEvent.click(screen.getByRole('button', { name: 'Test drop empty' }));
      fireEvent.click(screen.getByRole('button', { name: 'Test drop invalid target' }));
      fireEvent.click(screen.getByRole('button', { name: 'Test add placement' }));
    });
    act(() => {
      fireEvent.click(screen.getByRole('button', { name: 'Play tiles' }));
    });

    // then
    expect(sendMock).toHaveBeenCalledWith({ type: 'PASS', payload: { player: 'Alice' } });
    expect(sendMock).toHaveBeenCalledWith({ type: 'CHALLENGE', payload: { player: 'Alice' } });
    expect(sendMock).toHaveBeenCalledWith({ type: 'RESIGN', payload: { player: 'Alice' } });
    expect(sendMock).toHaveBeenCalledWith({
      type: 'PLAY_TILES',
      payload: { player: 'Alice', placements: [{ coordinate: 'H8', letter: 'A', blank: false }] }
    });
  });

  it('renders pending move and winner details', async () => {
    // given
    render(<App />);
    await waitForLobbyReady();
    fireEvent.change(screen.getByPlaceholderText('Player name'), { target: { value: 'Bob' } });
    await waitFor(() => expect(screen.getByPlaceholderText('Player name')).toHaveValue('Bob'));
    await waitFor(() => expect(screen.getByRole('button', { name: 'Join' })).toBeEnabled());
    fireEvent.click(screen.getByRole('button', { name: 'Join' }));
    await waitFor(() => expect(connectMock).toHaveBeenCalled());

    // when
    act(() => {
      snapshotHandler?.({
        roomId: 'room-2',
        status: 'ended',
        players: [],
        bagCount: 0,
        boardTiles: 0,
        board: [],
        currentPlayerIndex: null,
        pendingMove: true,
        pending: { playerIndex: 0, score: 12, words: [{ text: 'TEST', coordinates: [] }], placements: [] },
        winner: 'Bob'
      });
    });

    // then
    expect(screen.getByText('Winner')).toBeInTheDocument();
    expect(screen.getAllByText('Bob').length).toBeGreaterThan(0);
    expect(screen.getByText('Score: 12')).toBeInTheDocument();
    expect(screen.getByText('Words: TEST')).toBeInTheDocument();
  });

  it('handles blank tile prompt rejection', async () => {
    // given
    const promptSpy = vi.spyOn(window, 'prompt').mockReturnValueOnce('').mockReturnValueOnce('Z');
    render(<App />);
    await waitForLobbyReady();
    fireEvent.change(screen.getByPlaceholderText('Player name'), { target: { value: 'Cara' } });
    await waitFor(() => expect(screen.getByPlaceholderText('Player name')).toHaveValue('Cara'));
    await waitFor(() => expect(screen.getByRole('button', { name: 'Join' })).toBeEnabled());
    fireEvent.click(screen.getByRole('button', { name: 'Join' }));
    await waitFor(() => expect(connectMock).toHaveBeenCalled());
    act(() => {
      snapshotHandler?.({
        roomId: 'room-3',
        status: 'active',
        players: [
          { name: 'Cara', score: 0, rackSize: 1, rackCapacity: 7, rack: [{ letter: 'A', points: 1, blank: false }] }
        ],
        bagCount: 0,
        boardTiles: 0,
        board: [],
        currentPlayerIndex: 0,
        pendingMove: false,
        pending: null,
        winner: null
      });
    });

    // when
    act(() => {
      fireEvent.click(screen.getByRole('button', { name: 'Test drop blank' }));
      fireEvent.click(screen.getByRole('button', { name: 'Test drop blank' }));
    });

    // then
    expect(promptSpy).toHaveBeenCalled();
    promptSpy.mockRestore();
  });
});
