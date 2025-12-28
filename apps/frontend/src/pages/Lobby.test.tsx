import { fireEvent, render, screen, waitFor } from '@testing-library/react';

const listRoomsMock = vi.fn();
const createRoomMock = vi.fn();

vi.mock('../api', () => ({
  listRooms: () => listRoomsMock(),
  createRoom: (...args: unknown[]) => createRoomMock(...args)
}));

import Lobby from './Lobby';

describe('Lobby', () => {
  const defaultProps = {
    player: '',
    onPlayerChange: vi.fn(),
    onJoinRoom: vi.fn()
  };

  beforeEach(() => {
    vi.clearAllMocks();
    listRoomsMock.mockResolvedValue([
      { id: 'room-1', name: 'Test Room', players: ['Alice'] }
    ]);
    createRoomMock.mockResolvedValue({ id: 'room-2', name: 'New Room', players: ['Bob'] });
  });

  it('renders lobby header and player input', async () => {
    render(<Lobby {...defaultProps} />);

    expect(screen.getByText('Lobby')).toBeInTheDocument();
    expect(screen.getByPlaceholderText('Player name')).toBeInTheDocument();
    await waitFor(() => expect(listRoomsMock).toHaveBeenCalled());
  });

  it('calls onPlayerChange when typing in player input', async () => {
    const onPlayerChange = vi.fn();
    render(<Lobby {...defaultProps} onPlayerChange={onPlayerChange} />);

    const input = screen.getByPlaceholderText('Player name');
    fireEvent.change(input, { target: { value: 'Alice' } });

    expect(onPlayerChange).toHaveBeenCalledWith('Alice');
  });

  it('loads and displays rooms on mount', async () => {
    render(<Lobby {...defaultProps} player="Alice" />);

    await waitFor(() => expect(screen.getByText('Test Room')).toBeInTheDocument());
    expect(screen.getByText('#room-1 · 1 players')).toBeInTheDocument();
  });

  it('refreshes rooms when clicking Refresh button', async () => {
    render(<Lobby {...defaultProps} player="Alice" />);
    await waitFor(() => expect(listRoomsMock).toHaveBeenCalledTimes(1));

    fireEvent.click(screen.getByRole('button', { name: 'Refresh' }));

    await waitFor(() => expect(listRoomsMock).toHaveBeenCalledTimes(2));
  });

  it('filters rooms by search query', async () => {
    listRoomsMock.mockResolvedValue([
      { id: 'room-1', name: 'Alpha Room', players: [] },
      { id: 'room-2', name: 'Beta Room', players: [] }
    ]);
    render(<Lobby {...defaultProps} player="Alice" />);
    await waitFor(() => expect(screen.getByText('Alpha Room')).toBeInTheDocument());

    const searchInput = screen.getByPlaceholderText('Filter by name or id');
    fireEvent.change(searchInput, { target: { value: 'beta' } });

    expect(screen.queryByText('Alpha Room')).not.toBeInTheDocument();
    expect(screen.getByText('Beta Room')).toBeInTheDocument();
  });

  it('disables Join button when no player name', async () => {
    render(<Lobby {...defaultProps} player="" />);
    await waitFor(() => expect(screen.getByText('Test Room')).toBeInTheDocument());

    const joinButton = screen.getByRole('button', { name: 'Join' });
    expect(joinButton).toBeDisabled();
  });

  it('calls onJoinRoom when clicking Join', async () => {
    const onJoinRoom = vi.fn();
    render(<Lobby {...defaultProps} player="Alice" onJoinRoom={onJoinRoom} />);
    await waitFor(() => expect(screen.getByText('Test Room')).toBeInTheDocument());

    fireEvent.click(screen.getByRole('button', { name: 'Join' }));

    expect(onJoinRoom).toHaveBeenCalledWith('room-1', 'Test Room');
  });

  it('shows error when trying to join without player name', async () => {
    const onJoinRoom = vi.fn();
    render(<Lobby {...defaultProps} player="Alice" onJoinRoom={onJoinRoom} />);
    await waitFor(() => expect(screen.getByText('Test Room')).toBeInTheDocument());

    fireEvent.click(screen.getByRole('button', { name: 'Join' }));

    expect(onJoinRoom).toHaveBeenCalledWith('room-1', 'Test Room');
  });

  it('disables Create button when no room name or player', async () => {
    render(<Lobby {...defaultProps} player="" />);

    const createButton = screen.getByRole('button', { name: 'Create & connect' });
    expect(createButton).toBeDisabled();
  });

  it('creates room and calls onJoinRoom', async () => {
    const onJoinRoom = vi.fn();
    render(<Lobby {...defaultProps} player="Bob" onJoinRoom={onJoinRoom} />);

    const roomNameInput = screen.getByPlaceholderText('Room name');
    fireEvent.change(roomNameInput, { target: { value: 'New Room' } });

    fireEvent.click(screen.getByRole('button', { name: 'Create & connect' }));

    await waitFor(() => {
      expect(createRoomMock).toHaveBeenCalledWith('New Room', 'Bob', false);
      expect(onJoinRoom).toHaveBeenCalledWith('room-2', 'New Room');
    });
  });

  it('creates room with AI opponent when checkbox is checked', async () => {
    const onJoinRoom = vi.fn();
    render(<Lobby {...defaultProps} player="Bob" onJoinRoom={onJoinRoom} />);

    fireEvent.change(screen.getByPlaceholderText('Room name'), { target: { value: 'AI Room' } });
    fireEvent.click(screen.getByLabelText('Add computer opponent'));
    fireEvent.click(screen.getByRole('button', { name: 'Create & connect' }));

    await waitFor(() => {
      expect(createRoomMock).toHaveBeenCalledWith('AI Room', 'Bob', true);
    });
  });

  it('shows error when room creation fails', async () => {
    createRoomMock.mockRejectedValue(new Error('Server error'));
    render(<Lobby {...defaultProps} player="Bob" />);

    fireEvent.change(screen.getByPlaceholderText('Room name'), { target: { value: 'Fail Room' } });
    fireEvent.click(screen.getByRole('button', { name: 'Create & connect' }));

    await waitFor(() => {
      expect(screen.getAllByText('Server error').length).toBeGreaterThan(0);
    });
  });

  it('shows error when room list fails to load', async () => {
    listRoomsMock.mockRejectedValue(new Error('Network error'));
    render(<Lobby {...defaultProps} player="Alice" />);

    fireEvent.click(screen.getByRole('button', { name: 'Refresh' }));

    await waitFor(() => {
      expect(screen.getAllByText('Network error').length).toBeGreaterThan(0);
    });
  });

  it('shows "No rooms yet" when room list is empty', async () => {
    listRoomsMock.mockResolvedValue([]);
    render(<Lobby {...defaultProps} player="Alice" />);

    await waitFor(() => {
      expect(screen.getByText('No rooms yet.')).toBeInTheDocument();
    });
  });
});

