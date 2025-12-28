import { fireEvent, render, screen, waitFor } from '@testing-library/react';

vi.mock('./pages/Lobby', () => ({
  default: ({ player, onPlayerChange, onJoinRoom }: {
    player: string;
    onPlayerChange: (name: string) => void;
    onJoinRoom: (roomId: string, roomName?: string) => void;
  }) => (
    <div data-testid="lobby">
      <span data-testid="lobby-player">{player}</span>
      <input
        data-testid="player-input"
        value={player}
        onChange={(e) => onPlayerChange(e.target.value)}
      />
      <button data-testid="join-btn" onClick={() => onJoinRoom('1', 'Test Room')}>
        Join Room
      </button>
    </div>
  )
}));

vi.mock('./pages/Game', () => ({
  default: ({ roomId, player, onLeave }: {
    roomId: string;
    player: string;
    onLeave: () => void;
  }) => (
    <div data-testid="game">
      <span data-testid="game-room">{roomId}</span>
      <span data-testid="game-player">{player}</span>
      <button data-testid="leave-btn" onClick={onLeave}>Leave</button>
    </div>
  )
}));

import App from './App';

describe('App', () => {
  beforeEach(() => {
    window.localStorage.clear();
    window.history.pushState({}, '', '/');
  });

  it('renders Lobby when no room or player', () => {
    render(<App />);

    expect(screen.getByTestId('lobby')).toBeInTheDocument();
    expect(screen.queryByTestId('game')).not.toBeInTheDocument();
  });

  it('renders Lobby when player is set but no room', () => {
    window.localStorage.setItem('scrabble.player', 'Alice');
    render(<App />);

    expect(screen.getByTestId('lobby')).toBeInTheDocument();
    expect(screen.getByTestId('lobby-player')).toHaveTextContent('Alice');
  });

  it('renders Game when room and player are set', () => {
    window.localStorage.setItem('scrabble.player', 'Alice');
    window.history.pushState({}, '', '/room/1-test-room');
    render(<App />);

    expect(screen.queryByTestId('lobby')).not.toBeInTheDocument();
    expect(screen.getByTestId('game')).toBeInTheDocument();
    expect(screen.getByTestId('game-room')).toHaveTextContent('1');
    expect(screen.getByTestId('game-player')).toHaveTextContent('Alice');
  });

  it('navigates to Game when joining room from Lobby', async () => {
    render(<App />);

    fireEvent.change(screen.getByTestId('player-input'), { target: { value: 'Bob' } });
    fireEvent.click(screen.getByTestId('join-btn'));

    await waitFor(() => {
      expect(screen.getByTestId('game')).toBeInTheDocument();
      expect(screen.getByTestId('game-room')).toHaveTextContent('1');
      expect(screen.getByTestId('game-player')).toHaveTextContent('Bob');
    });
    expect(window.location.pathname).toBe('/room/1-test-room');
  });

  it('navigates back to Lobby when leaving Game', async () => {
    window.localStorage.setItem('scrabble.player', 'Alice');
    window.history.pushState({}, '', '/room/1-test');
    render(<App />);

    expect(screen.getByTestId('game')).toBeInTheDocument();

    fireEvent.click(screen.getByTestId('leave-btn'));

    await waitFor(() => {
      expect(screen.getByTestId('lobby')).toBeInTheDocument();
      expect(screen.queryByTestId('game')).not.toBeInTheDocument();
    });
    expect(window.location.pathname).toBe('/');
  });

  it('persists player name to localStorage', () => {
    render(<App />);

    fireEvent.change(screen.getByTestId('player-input'), { target: { value: 'Charlie' } });

    expect(window.localStorage.getItem('scrabble.player')).toBe('Charlie');
  });

  it('loads player name from localStorage', () => {
    window.localStorage.setItem('scrabble.player', 'Dana');
    render(<App />);

    expect(screen.getByTestId('lobby-player')).toHaveTextContent('Dana');
  });

  it('handles browser back navigation from Game to Lobby', async () => {
    window.localStorage.setItem('scrabble.player', 'Eve');
    render(<App />);

    fireEvent.click(screen.getByTestId('join-btn'));
    await waitFor(() => expect(screen.getByTestId('game')).toBeInTheDocument());

    window.history.back();
    window.dispatchEvent(new PopStateEvent('popstate'));

    await waitFor(() => {
      expect(screen.getByTestId('lobby')).toBeInTheDocument();
    });
  });

  it('parses room ID from URL with slug', () => {
    window.localStorage.setItem('scrabble.player', 'Frank');
    window.history.pushState({}, '', '/room/123-my-cool-room');
    render(<App />);

    expect(screen.getByTestId('game-room')).toHaveTextContent('123');
  });

  it('stays on Lobby for invalid room URL', () => {
    window.localStorage.setItem('scrabble.player', 'Grace');
    window.history.pushState({}, '', '/invalid/path');
    render(<App />);

    expect(screen.getByTestId('lobby')).toBeInTheDocument();
  });

  it('removes player from localStorage when cleared', () => {
    window.localStorage.setItem('scrabble.player', 'Henry');
    render(<App />);

    fireEvent.change(screen.getByTestId('player-input'), { target: { value: '' } });

    expect(window.localStorage.getItem('scrabble.player')).toBeNull();
  });
});
