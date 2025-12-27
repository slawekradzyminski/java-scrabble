import React from 'react';
import { render, screen } from '@testing-library/react';
import Tile from './Tile';

describe('Tile', () => {
  it('renders letter and points', () => {
    // given
    const tile = { letter: 'A', points: 1, blank: false };

    // when
    render(<Tile tile={tile} />);

    // then
    expect(screen.getByText('A')).toBeInTheDocument();
    expect(screen.getByText('1')).toBeInTheDocument();
  });

  it('hides points for blank tiles', () => {
    // given
    const tile = { letter: null, points: 0, blank: true };

    // when
    render(<Tile tile={tile} label="A" />);

    // then
    expect(screen.getByText('A')).toBeInTheDocument();
    expect(screen.queryByText('0')).not.toBeInTheDocument();
  });
});
