import { fireEvent, render, screen } from '@testing-library/react';
import HistoryPanel from './HistoryPanel';
import type { EventLogEntry } from '../types';

const sampleEvents: EventLogEntry[] = [
  { id: 1, time: '10:00:00', type: 'MOVE_ACCEPTED', summary: 'Move accepted for Alice (10 pts)' }
];

describe('HistoryPanel', () => {
  it('renders expanded history with events', () => {
    // given
    const onToggle = vi.fn();

    // when
    render(
      <HistoryPanel
        eventLog={sampleEvents}
        lastEventAt={Date.now()}
        expanded
        onToggle={onToggle}
      />
    );

    // then
    expect(screen.getByText('Live history')).toBeInTheDocument();
    expect(screen.getByText('Move accepted for Alice (10 pts)')).toBeInTheDocument();
  });

  it('renders collapsed history and toggles', () => {
    // given
    const onToggle = vi.fn();

    // when
    render(
      <HistoryPanel
        eventLog={sampleEvents}
        lastEventAt={null}
        expanded={false}
        onToggle={onToggle}
      />
    );
    fireEvent.click(screen.getByRole('button', { name: 'Show' }));

    // then
    expect(screen.getByText('History hidden (1 events).')).toBeInTheDocument();
    expect(onToggle).toHaveBeenCalled();
  });
});
