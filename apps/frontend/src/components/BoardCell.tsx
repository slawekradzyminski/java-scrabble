import React from 'react';

interface BoardCellProps {
  id: string;
  label?: string;
  premium?: string;
  children?: React.ReactNode;
  highlighted?: boolean;
}

export default function BoardCell({ id, label, premium, children, highlighted }: BoardCellProps) {
  return (
    <div className={`board-cell ${premium ? `board-cell--${premium}` : ''} ${highlighted ? 'board-cell--highlight' : ''}`} data-cell-id={id}>
      <span className="board-cell__label">{label}</span>
      {premium && <span className="board-cell__premium">{premium}</span>}
      {children}
    </div>
  );
}
