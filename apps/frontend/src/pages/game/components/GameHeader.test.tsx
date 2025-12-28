import { fireEvent, render, screen } from '@testing-library/react';
import GameHeader from './GameHeader';

describe('GameHeader', () => {
  it('renders expanded header and handles actions', () => {
    // given
    const onToggle = vi.fn();
    const onLeave = vi.fn();

    // when
    render(<GameHeader expanded onToggle={onToggle} onLeave={onLeave} />);
    fireEvent.click(screen.getByRole('button', { name: 'Hide header' }));
    fireEvent.click(screen.getByRole('button', { name: 'Leave Room' }));

    // then
    expect(screen.getByText('Realtime Board')).toBeInTheDocument();
    expect(onToggle).toHaveBeenCalled();
    expect(onLeave).toHaveBeenCalled();
  });

  it('renders compact header and toggles', () => {
    // given
    const onToggle = vi.fn();

    // when
    render(<GameHeader expanded={false} onToggle={onToggle} onLeave={vi.fn()} />);
    fireEvent.click(screen.getByRole('button', { name: 'Show header' }));

    // then
    expect(onToggle).toHaveBeenCalled();
  });
});
