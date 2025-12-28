
import { render, screen } from '@testing-library/react';
import BoardCell from './BoardCell';

describe('BoardCell', () => {
  it('renders label and premium', () => {
    // given
    const id = 'H8';

    // when
    render(
      <BoardCell id={id} label={id} premium="dw">
        <div>child</div>
      </BoardCell>
    );

    // then
    expect(screen.getByText('H8')).toBeInTheDocument();
    expect(screen.getByText('dw')).toBeInTheDocument();
    expect(screen.getByText('child')).toBeInTheDocument();
  });
});
