import { act, fireEvent, render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';

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
    // given
    const onPlayerChange = vi.fn();
    render(<Lobby {...defaultProps} onPlayerChange={onPlayerChange} />);
    await waitFor(() => expect(listRoomsMock).toHaveBeenCalled());

    const input = screen.getByPlaceholderText('Player name');
    act(() => {
      fireEvent.change(input, { target: { value: 'Alice' } });
    });

    expect(onPlayerChange).toHaveBeenLastCalledWith('Alice');
  });

  it('loads and displays rooms on mount', async () => {
    render(<Lobby {...defaultProps} player="Alice" />);

    await waitFor(() => expect(screen.getByText('Test Room')).toBeInTheDocument());
    expect(screen.getByText('#room-1 · 1 players')).toBeInTheDocument();
  });

  it('refreshes rooms when clicking Refresh button', async () => {
    // given
    render(<Lobby {...defaultProps} player="Alice" />);
    await waitFor(() => expect(listRoomsMock).toHaveBeenCalledTimes(1));

    // when
    const user = userEvent.setup();
    await user.click(screen.getByRole('button', { name: 'Refresh' }));

    await waitFor(() => expect(listRoomsMock).toHaveBeenCalledTimes(2));
  });

  it('filters rooms by search query', async () => {
    // given
    listRoomsMock.mockResolvedValue([
      { id: 'room-1', name: 'Alpha Room', players: [] },
      { id: 'room-2', name: 'Beta Room', players: [] }
    ]);
    render(<Lobby {...defaultProps} player="Alice" />);
    await waitFor(() => expect(screen.getByText('Alpha Room')).toBeInTheDocument());

    const searchInput = screen.getByPlaceholderText('Filter by name or id');
    // when
    const user = userEvent.setup();
    await user.type(searchInput, 'beta');

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
    // given
    const onJoinRoom = vi.fn();
    render(<Lobby {...defaultProps} player="Alice" onJoinRoom={onJoinRoom} />);
    await waitFor(() => expect(screen.getByText('Test Room')).toBeInTheDocument());

    // when
    const user = userEvent.setup();
    await user.click(screen.getByRole('button', { name: 'Join' }));

    expect(onJoinRoom).toHaveBeenCalledWith('room-1', 'Test Room');
  });

  it('shows error when trying to join without player name', async () => {
    // given
    const onJoinRoom = vi.fn();
    render(<Lobby {...defaultProps} player="Alice" onJoinRoom={onJoinRoom} />);
    await waitFor(() => expect(screen.getByText('Test Room')).toBeInTheDocument());

    // when
    const user = userEvent.setup();
    await user.click(screen.getByRole('button', { name: 'Join' }));

    expect(onJoinRoom).toHaveBeenCalledWith('room-1', 'Test Room');
  });

  it('disables Create button when no room name or player', async () => {
    render(<Lobby {...defaultProps} player="" />);
    await waitFor(() => expect(listRoomsMock).toHaveBeenCalled());

    const createButton = screen.getByRole('button', { name: 'Create & connect' });
    expect(createButton).toBeDisabled();
  });

  it('creates room and calls onJoinRoom', async () => {
    // given
    const onJoinRoom = vi.fn();
    render(<Lobby {...defaultProps} player="Bob" onJoinRoom={onJoinRoom} />);
    const user = userEvent.setup();

    const roomNameInput = screen.getByPlaceholderText('Room name');
    await user.type(roomNameInput, 'New Room');

    // when
    await user.click(screen.getByRole('button', { name: 'Create & connect' }));

    await waitFor(() => {
      expect(createRoomMock).toHaveBeenCalledWith('New Room', 'Bob', false);
      expect(onJoinRoom).toHaveBeenCalledWith('room-2', 'New Room');
    });
  });

  it('creates room with AI opponent when checkbox is checked', async () => {
    // given
    const onJoinRoom = vi.fn();
    render(<Lobby {...defaultProps} player="Bob" onJoinRoom={onJoinRoom} />);
    const user = userEvent.setup();

    await user.type(screen.getByPlaceholderText('Room name'), 'AI Room');
    await user.click(screen.getByLabelText('Add computer opponent'));
    await user.click(screen.getByRole('button', { name: 'Create & connect' }));

    await waitFor(() => {
      expect(createRoomMock).toHaveBeenCalledWith('AI Room', 'Bob', true);
    });
  });

  it('shows error when room creation fails', async () => {
    // given
    createRoomMock.mockRejectedValue(new Error('Server error'));
    render(<Lobby {...defaultProps} player="Bob" />);
    const user = userEvent.setup();

    await user.type(screen.getByPlaceholderText('Room name'), 'Fail Room');
    await user.click(screen.getByRole('button', { name: 'Create & connect' }));

    await waitFor(() => {
      expect(screen.getAllByText('Server error').length).toBeGreaterThan(0);
    });
  });

  it('shows error when room list fails to load', async () => {
    // given
    listRoomsMock.mockRejectedValue(new Error('Network error'));
    render(<Lobby {...defaultProps} player="Alice" />);

    // when
    const user = userEvent.setup();
    await user.click(screen.getByRole('button', { name: 'Refresh' }));

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
