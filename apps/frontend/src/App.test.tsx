import React from 'react';
import { act, fireEvent, render, screen } from '@testing-library/react';
import App from './App';

const connectMock = vi.fn();
const sendMock = vi.fn();
let snapshotHandler: ((snapshot: unknown) => void) | null = null;

vi.mock('./api', () => {
  return {
    GameSocket: class {
      onSnapshot(handler: (snapshot: unknown) => void) {
        snapshotHandler = handler;
      }
      onEvent() {}
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
  beforeEach(() => {
    connectMock.mockClear();
    sendMock.mockClear();
  });

  it('renders connect fields and calls connect', () => {
    // given
    render(<App />);
    const roomInput = screen.getByPlaceholderText('Room id');
    const playerInput = screen.getByPlaceholderText('Player name');

    // when
    fireEvent.change(roomInput, { target: { value: 'room-1' } });
    fireEvent.change(playerInput, { target: { value: 'Alice' } });
    fireEvent.click(screen.getByRole('button', { name: 'Connect' }));

    // then
    expect(connectMock).toHaveBeenCalledWith('room-1', 'Alice');
  });

  it('does not connect without room or player', () => {
    // given
    render(<App />);

    // when
    fireEvent.click(screen.getByRole('button', { name: 'Connect' }));

    // then
    expect(connectMock).not.toHaveBeenCalled();
  });

  it('ignores play tiles when not connected', () => {
    // given
    render(<App />);

    // when
    fireEvent.click(screen.getByRole('button', { name: 'Play tiles' }));

    // then
    expect(sendMock).not.toHaveBeenCalled();
  });

  it('sends commands from action buttons', () => {
    // given
    render(<App />);
    fireEvent.change(screen.getByPlaceholderText('Room id'), { target: { value: 'room-1' } });
    fireEvent.change(screen.getByPlaceholderText('Player name'), { target: { value: 'Alice' } });
    fireEvent.click(screen.getByRole('button', { name: 'Connect' }));
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

  it('renders pending move and winner details', () => {
    // given
    render(<App />);
    fireEvent.change(screen.getByPlaceholderText('Room id'), { target: { value: 'room-2' } });
    fireEvent.change(screen.getByPlaceholderText('Player name'), { target: { value: 'Bob' } });
    fireEvent.click(screen.getByRole('button', { name: 'Connect' }));

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
    expect(screen.getByText('Winner:')).toBeInTheDocument();
    expect(screen.getByText('Bob')).toBeInTheDocument();
    expect(screen.getByText('Score: 12')).toBeInTheDocument();
    expect(screen.getByText('Words: TEST')).toBeInTheDocument();
  });

  it('handles blank tile prompt rejection', () => {
    // given
    const promptSpy = vi.spyOn(window, 'prompt').mockReturnValueOnce('').mockReturnValueOnce('Z');
    render(<App />);
    fireEvent.change(screen.getByPlaceholderText('Room id'), { target: { value: 'room-3' } });
    fireEvent.change(screen.getByPlaceholderText('Player name'), { target: { value: 'Cara' } });
    fireEvent.click(screen.getByRole('button', { name: 'Connect' }));
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
