import { render, screen } from '@testing-library/react';
import PlayersPanel from './PlayersPanel';

describe('PlayersPanel', () => {
  it('renders player list', () => {
    // given
    const players = [
      { name: 'Alice', score: 10, rackSize: 7, rackCapacity: 7, rack: [] }
    ];

    // when
    render(<PlayersPanel players={players} currentIndex={0} />);

    // then
    expect(screen.getByText('Alice')).toBeInTheDocument();
  });
});
