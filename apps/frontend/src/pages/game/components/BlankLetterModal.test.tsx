import { fireEvent, render, screen } from '@testing-library/react';
import BlankLetterModal from './BlankLetterModal';

describe('BlankLetterModal', () => {
  it('selects a letter', () => {
    // given
    const onSelect = vi.fn();
    const onCancel = vi.fn();

    // when
    render(<BlankLetterModal onSelect={onSelect} onCancel={onCancel} />);
    fireEvent.click(screen.getByRole('button', { name: 'A' }));

    // then
    expect(onSelect).toHaveBeenCalledWith('A');
  });

  it('closes modal', () => {
    // given
    const onSelect = vi.fn();
    const onCancel = vi.fn();

    // when
    render(<BlankLetterModal onSelect={onSelect} onCancel={onCancel} />);
    fireEvent.click(screen.getByRole('button', { name: 'Close' }));

    // then
    expect(onCancel).toHaveBeenCalled();
  });
});
