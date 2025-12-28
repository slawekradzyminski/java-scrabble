import type { EventLogEntry } from '../types';

interface HistoryPanelProps {
  eventLog: EventLogEntry[];
  lastEventAt: number | null;
  expanded: boolean;
  onToggle: () => void;
}

export default function HistoryPanel({ eventLog, lastEventAt, expanded, onToggle }: HistoryPanelProps) {
  return (
    <div className="info-section">
      <div className="info-header">
        <h3>Live history</h3>
        <button
          type="button"
          className="history-toggle"
          onClick={onToggle}
        >
          {expanded ? 'Hide' : 'Show'}
        </button>
      </div>
      {expanded ? (
        <>
          {lastEventAt ? (
            <p className="muted">Last update: {new Date(lastEventAt).toLocaleTimeString()}</p>
          ) : null}
          <div className="event-log">
            {eventLog.length === 0 ? (
              <p className="muted">No events yet.</p>
            ) : (
              <ul>
                {eventLog.map((entry) => (
                  <li key={entry.id} className={`event-log__entry event-log__entry--${entry.type.toLowerCase()}`}>
                    <span className="event-log__time">{entry.time}</span>
                    <span className="event-log__summary">{entry.summary}</span>
                  </li>
                ))}
              </ul>
            )}
          </div>
        </>
      ) : (
        <p className="muted">History hidden ({eventLog.length} events).</p>
      )}
    </div>
  );
}
