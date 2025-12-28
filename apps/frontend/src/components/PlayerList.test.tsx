import { render, screen } from '@testing-library/react';
import PlayerList from './PlayerList';

describe('PlayerList', () => {
  it('renders players and highlights current', () => {
    // given
    const players = [
      { name: 'Alice', score: 12, rackSize: 7, rackCapacity: 7, rack: [] },
      { name: 'Bob', score: 5, rackSize: 6, rackCapacity: 7, rack: [] }
    ];

    // when
    const { container } = render(<PlayerList players={players} currentIndex={1} />);

    // then
    expect(screen.getByText('Alice')).toBeInTheDocument();
    expect(screen.getByText('Bob')).toBeInTheDocument();
    const active = container.querySelector('.player-card--active');
    expect(active).toBeTruthy();
    expect(active?.textContent).toContain('Bob');
  });
});
