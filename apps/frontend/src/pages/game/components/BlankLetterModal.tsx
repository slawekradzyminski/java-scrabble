import { useMemo } from 'react';

interface BlankLetterModalProps {
  onSelect: (letter: string) => void;
  onCancel: () => void;
}

export default function BlankLetterModal({ onSelect, onCancel }: BlankLetterModalProps) {
  const letters = useMemo(() => Array.from({ length: 26 }, (_, i) => String.fromCharCode(65 + i)), []);

  return (
    <div className="modal-backdrop" role="presentation" onClick={onCancel}>
      <div
        className="modal modal--compact"
        role="dialog"
        aria-modal="true"
        aria-label="Choose blank letter"
        onClick={(event) => event.stopPropagation()}
      >
        <div className="modal-header">
          <h2>Choose a letter</h2>
          <button type="button" className="modal-close" onClick={onCancel}>
            Close
          </button>
        </div>
        <p className="muted">Blank tile counts as any letter (0 points).</p>
        <div className="blank-grid">
          {letters.map((letter) => (
            <button
              key={letter}
              type="button"
              className="blank-letter"
              onClick={() => onSelect(letter)}
            >
              {letter}
            </button>
          ))}
        </div>
      </div>
    </div>
  );
}
