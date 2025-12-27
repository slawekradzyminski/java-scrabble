import React from 'react';
import { act, fireEvent, render, screen, waitFor } from '@testing-library/react';
import type { ConnectionState } from '../api';

const connectMock = vi.fn();
const sendMock = vi.fn();
const disconnectMock = vi.fn();
const requestSyncMock = vi.fn();
const joinRoomMock = vi.fn();
const startGameMock = vi.fn();

let snapshotHandler: ((snapshot: unknown) => void) | null = null;
let connectionStateHandler: ((state: ConnectionState) => void) | null = null;
let isConnectedValue = false;

vi.mock('../api', () => ({
  joinRoom: (...args: unknown[]) => joinRoomMock(...args),
  startGame: (...args: unknown[]) => startGameMock(...args),
  GameSocket: class {
    onSnapshot(handler: (snapshot: unknown) => void) {
      snapshotHandler = handler;
    }
    onEvent() {}
    onConnectionState(handler: (state: ConnectionState) => void) {
      connectionStateHandler = handler;
    }
    disconnect() {
      disconnectMock();
      isConnectedValue = false;
    }
    connect(roomId: string, player: string) {
      connectMock(roomId, player);
      connectionStateHandler?.('connecting');
      setTimeout(() => {
        connectionStateHandler?.('connected');
        isConnectedValue = true;
      }, 0);
    }
    send(message: unknown) {
      sendMock(message);
    }
    isConnected() {
      return isConnectedValue;
    }
    requestSync() {
      requestSyncMock();
    }
  }
}));

import Game from './Game';

describe('Game', () => {
  const defaultProps = {
    roomId: 'room-1',
    player: 'Alice',
    onLeave: vi.fn()
  };

  beforeEach(() => {
    vi.clearAllMocks();
    isConnectedValue = false;
    snapshotHandler = null;
    connectionStateHandler = null;
    joinRoomMock.mockResolvedValue({ id: 'room-1', name: 'Test Room', players: ['Alice'] });
    startGameMock.mockResolvedValue({});
  });

  const waitForConnection = async () => {
    await waitFor(() => expect(connectMock).toHaveBeenCalled());
    await act(async () => {
      await new Promise((resolve) => setTimeout(resolve, 10));
    });
  };

  const sendSnapshot = (overrides = {}) => {
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
        winner: null,
        ...overrides
      });
    });
  };

  it('joins room and connects socket on mount', async () => {
    render(<Game {...defaultProps} />);

    await waitFor(() => {
      expect(joinRoomMock).toHaveBeenCalledWith('room-1', 'Alice');
      expect(connectMock).toHaveBeenCalledWith('room-1', 'Alice');
    });
  });

  it('shows connecting state before sync', async () => {
    render(<Game {...defaultProps} />);

    await waitFor(() => expect(joinRoomMock).toHaveBeenCalled());
    await waitForConnection();
    sendSnapshot({ status: 'not_started' });

    await waitFor(() => {
      expect(screen.getByText('not_started')).toBeInTheDocument();
    });
  });

  it('renders board after receiving snapshot', async () => {
    render(<Game {...defaultProps} />);
    await waitForConnection();
    sendSnapshot();

    await waitFor(() => {
      expect(screen.queryByText(/connecting/i)).not.toBeInTheDocument();
    });
    expect(screen.getByText('active')).toBeInTheDocument();
  });

  it('displays player info from snapshot', async () => {
    render(<Game {...defaultProps} />);
    await waitForConnection();
    sendSnapshot();

    expect(screen.getAllByText('Alice').length).toBeGreaterThan(0);
    expect(screen.getByText('95')).toBeInTheDocument();
  });

  it('calls onLeave when Leave Room button is clicked', async () => {
    const onLeave = vi.fn();
    render(<Game {...defaultProps} onLeave={onLeave} />);
    await waitForConnection();
    sendSnapshot();

    fireEvent.click(screen.getByRole('button', { name: 'Leave Room' }));

    expect(onLeave).toHaveBeenCalled();
  });

  it('disconnects socket on unmount', async () => {
    const { unmount } = render(<Game {...defaultProps} />);
    await waitForConnection();

    unmount();

    expect(disconnectMock).toHaveBeenCalled();
  });

  it('sends PASS command when Pass button is clicked', async () => {
    render(<Game {...defaultProps} />);
    await waitForConnection();
    sendSnapshot();

    fireEvent.click(screen.getByRole('button', { name: 'Pass' }));

    expect(sendMock).toHaveBeenCalledWith({ type: 'PASS', payload: { player: 'Alice' } });
  });

  it('sends CHALLENGE command when Challenge button is clicked', async () => {
    render(<Game {...defaultProps} />);
    await waitForConnection();
    sendSnapshot();

    fireEvent.click(screen.getByRole('button', { name: 'Challenge' }));

    expect(sendMock).toHaveBeenCalledWith({ type: 'CHALLENGE', payload: { player: 'Alice' } });
  });

  it('sends RESIGN command when Resign button is clicked', async () => {
    render(<Game {...defaultProps} />);
    await waitForConnection();
    sendSnapshot();

    fireEvent.click(screen.getByRole('button', { name: 'Resign' }));

    expect(sendMock).toHaveBeenCalledWith({ type: 'RESIGN', payload: { player: 'Alice' } });
  });

  it('sends PLAY_TILES when placing tile and clicking Play tiles', async () => {
    render(<Game {...defaultProps} />);
    await waitForConnection();
    sendSnapshot();

    fireEvent.click(screen.getByRole('button', { name: 'Test add placement' }));
    fireEvent.click(screen.getByRole('button', { name: 'Play tiles' }));

    expect(sendMock).toHaveBeenCalledWith({
      type: 'PLAY_TILES',
      payload: { player: 'Alice', placements: [{ coordinate: 'H8', letter: 'A', blank: false }] }
    });
  });

  it('clears placements when Reset is clicked', async () => {
    render(<Game {...defaultProps} />);
    await waitForConnection();
    sendSnapshot();

    fireEvent.click(screen.getByRole('button', { name: 'Test add placement' }));
    expect(screen.getByRole('button', { name: 'Reset' })).toBeEnabled();

    fireEvent.click(screen.getByRole('button', { name: 'Reset' }));
    expect(screen.getByRole('button', { name: 'Reset' })).toBeDisabled();
  });

  it('starts game and requests sync', async () => {
    render(<Game {...defaultProps} />);
    await waitForConnection();
    sendSnapshot({ status: 'not_started' });

    fireEvent.click(screen.getByRole('button', { name: 'Start' }));

    await waitFor(() => {
      expect(startGameMock).toHaveBeenCalledWith('room-1');
      expect(requestSyncMock).toHaveBeenCalled();
    });
  });

  it('shows error when start game fails', async () => {
    startGameMock.mockRejectedValue(new Error('Start failed'));
    render(<Game {...defaultProps} />);
    await waitForConnection();
    sendSnapshot({ status: 'not_started' });

    fireEvent.click(screen.getByRole('button', { name: 'Start' }));

    await waitFor(() => {
      expect(screen.getByText('Start failed')).toBeInTheDocument();
    });
  });

  it('displays winner when game ends', async () => {
    render(<Game {...defaultProps} />);
    await waitForConnection();
    sendSnapshot({ status: 'ended', winner: 'Alice' });

    expect(screen.getByText('Winner')).toBeInTheDocument();
    expect(screen.getAllByText('Alice').length).toBeGreaterThan(0);
  });

  it('displays pending move details', async () => {
    render(<Game {...defaultProps} />);
    await waitForConnection();
    sendSnapshot({
      pendingMove: true,
      pending: {
        playerIndex: 0,
        score: 15,
        words: [{ text: 'HELLO', coordinates: [] }],
        placements: []
      }
    });

    expect(screen.getByText('Score: 15')).toBeInTheDocument();
    expect(screen.getByText('Words: HELLO')).toBeInTheDocument();
  });

  it('handles blank tile with prompt', async () => {
    const promptSpy = vi.spyOn(window, 'prompt').mockReturnValue('Z');
    render(<Game {...defaultProps} />);
    await waitForConnection();
    sendSnapshot();

    fireEvent.click(screen.getByRole('button', { name: 'Test drop blank' }));

    expect(promptSpy).toHaveBeenCalledWith('Blank tile: choose a letter (A-Z)');
    promptSpy.mockRestore();
  });

  it('cancels blank tile placement when prompt is empty', async () => {
    const promptSpy = vi.spyOn(window, 'prompt').mockReturnValue('');
    render(<Game {...defaultProps} />);
    await waitForConnection();
    sendSnapshot();

    fireEvent.click(screen.getByRole('button', { name: 'Test drop blank' }));

    expect(screen.getByRole('button', { name: 'Play tiles' })).toBeDisabled();
    promptSpy.mockRestore();
  });

  it('shows error when join room fails', async () => {
    joinRoomMock.mockRejectedValue(new Error('Room not found'));
    render(<Game {...defaultProps} />);

    await waitFor(() => {
      expect(screen.getByText('Room not found')).toBeInTheDocument();
    });
  });

  it('disables action buttons before socket is ready', async () => {
    render(<Game {...defaultProps} />);

    expect(screen.getByRole('button', { name: 'Pass' })).toBeDisabled();
    expect(screen.getByRole('button', { name: 'Challenge' })).toBeDisabled();
    expect(screen.getByRole('button', { name: 'Resign' })).toBeDisabled();
    expect(screen.getByRole('button', { name: 'Start' })).toBeDisabled();
  });
});
