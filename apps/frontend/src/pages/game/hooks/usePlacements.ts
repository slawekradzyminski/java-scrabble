import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import type { DragEndEvent, DragStartEvent } from '@dnd-kit/core';
import type { BoardTile, RackTile } from '../../../types';
import { cellId } from '../../../utils';
import type { ActiveTile, PlacementState } from '../types';

interface UsePlacementsOptions {
  getRackTile: (index: number) => RackTile | undefined;
}

export function usePlacements({ getRackTile }: UsePlacementsOptions) {
  const [placements, setPlacements] = useState<PlacementState>({});
  const [activeTile, setActiveTile] = useState<ActiveTile | null>(null);
  const [activeTileSource, setActiveTileSource] = useState<string | null>(null);
  const [activeTileLabel, setActiveTileLabel] = useState<string | undefined>(undefined);
  const [pendingBlank, setPendingBlank] = useState<{
    tile: ActiveTile;
    coordinate: string;
    source: string | null;
  } | null>(null);

  const placementsRef = useRef<PlacementState>({});

  useEffect(() => {
    placementsRef.current = placements;
  }, [placements]);

  const resetLocalState = useCallback(() => {
    setPlacements({});
    setActiveTile(null);
    setActiveTileSource(null);
    setActiveTileLabel(undefined);
    setPendingBlank(null);
  }, []);

  const commitPlacement = useCallback((tile: ActiveTile, coordinate: string, source: string | null, assignedLetterOverride?: string) => {
    const assignedLetter = assignedLetterOverride
      ?? (source && placements[source]?.assignedLetter)
      ?? tile.letter
      ?? '';

    const boardTile: BoardTile = {
      coordinate,
      letter: tile.letter,
      points: tile.points,
      blank: tile.blank,
      assignedLetter
    };
    setPlacements((prev) => {
      const next = { ...prev };
      if (source && source !== coordinate) {
        delete next[source];
      }
      const rackKey = tile.rackIndex >= 0 ? `rack-${tile.rackIndex}` : null;
      if (rackKey) {
        Object.keys(next).forEach((key) => {
          if (next[key].coordinate === rackKey) {
            delete next[key];
          }
        });
        next[rackKey] = { ...boardTile, coordinate: rackKey, rackIndex: tile.rackIndex };
      }
      next[coordinate] = { ...boardTile, rackIndex: tile.rackIndex };
      return next;
    });
  }, [placements]);

  const applyDrop = useCallback((tile: ActiveTile | null, dropTarget: string | null) => {
    if (!tile || !dropTarget) {
      setActiveTile(null);
      setActiveTileSource(null);
      setActiveTileLabel(undefined);
      return;
    }

    const dropId = dropTarget.toString();

    if (dropId === 'rack-drop') {
      if (activeTileSource) {
        setPlacements((prev) => {
          const next = { ...prev };
          const rackIndex = next[activeTileSource]?.rackIndex;
          if (typeof rackIndex === 'number' && rackIndex >= 0) {
            delete next[`rack-${rackIndex}`];
          }
          delete next[activeTileSource];
          return next;
        });
      }
      setActiveTile(null);
      setActiveTileSource(null);
      setActiveTileLabel(undefined);
      return;
    }

    if (dropId.startsWith('rack-')) {
      setActiveTile(null);
      setActiveTileSource(null);
      setActiveTileLabel(undefined);
      return;
    }

    if (!dropId.startsWith('cell-')) {
      setActiveTile(null);
      setActiveTileSource(null);
      setActiveTileLabel(undefined);
      return;
    }

    const coordinate = dropId.replace('cell-', '');

    if (activeTileSource && activeTileSource !== coordinate) {
      setPlacements((prev) => {
        const next = { ...prev };
        delete next[activeTileSource];
        const boardTile: BoardTile = {
          coordinate,
          letter: tile.letter,
          points: tile.points,
          blank: tile.blank,
          assignedLetter: prev[activeTileSource]?.assignedLetter ?? tile.letter ?? ''
        };
        next[coordinate] = boardTile;
        return next;
      });
      setActiveTile(null);
      setActiveTileSource(null);
      setActiveTileLabel(undefined);
      return;
    }

    if (placements[coordinate] && !activeTileSource) {
      setPlacements((prev) => {
        const next = { ...prev };
        const rackIndex = next[coordinate]?.rackIndex;
        if (typeof rackIndex === 'number' && rackIndex >= 0) {
          delete next[`rack-${rackIndex}`];
        }
        delete next[coordinate];
        return next;
      });
      setActiveTile(null);
      setActiveTileSource(null);
      setActiveTileLabel(undefined);
      return;
    }

    if (tile.blank && !placements[coordinate]) {
      const existingPlacement = activeTileSource ? placements[activeTileSource] : null;
      if (!existingPlacement) {
        setPendingBlank({ tile, coordinate, source: activeTileSource });
        setActiveTile(null);
        setActiveTileSource(null);
        setActiveTileLabel(undefined);
        return;
      }
      commitPlacement(tile, coordinate, activeTileSource, existingPlacement.assignedLetter);
    } else {
      commitPlacement(tile, coordinate, activeTileSource);
    }
    setActiveTile(null);
    setActiveTileSource(null);
    setActiveTileLabel(undefined);
  }, [activeTileSource, commitPlacement, placements]);

  const confirmBlank = useCallback((letter: string) => {
    if (!pendingBlank) {
      return;
    }
    commitPlacement(pendingBlank.tile, pendingBlank.coordinate, pendingBlank.source, letter);
    setPendingBlank(null);
  }, [commitPlacement, pendingBlank]);

  const cancelBlank = useCallback(() => {
    setPendingBlank(null);
  }, []);

  const handleDragStart = useCallback((event: DragStartEvent) => {
    const activeId = String(event.active.id);
    if (activeId.startsWith('rack-')) {
      const rackIndex = parseInt(activeId.replace('rack-', ''), 10);
      const tile = getRackTile(rackIndex);
      if (tile && placements[`rack-${rackIndex}`]) {
        setActiveTile(null);
        return;
      }
      setActiveTile(tile ? { ...tile, rackIndex } : null);
      setActiveTileSource(null);
      setActiveTileLabel(undefined);
    } else if (activeId.startsWith('placement-')) {
      const coordinate = activeId.replace('placement-', '');
      const placement = placements[coordinate];
      if (placement) {
        setActiveTile({
          letter: placement.letter,
          points: placement.points,
          blank: placement.blank,
          rackIndex: placement.rackIndex ?? -1
        });
        setActiveTileSource(coordinate);
        setActiveTileLabel(placement.assignedLetter);
      }
    }
  }, [getRackTile, placements]);

  const handleDragEnd = useCallback((event: DragEndEvent) => {
    applyDrop(activeTile, event.over?.id?.toString() ?? null);
  }, [activeTile, applyDrop]);

  const handleTileSelect = useCallback((tile: RackTile, index: number) => {
    if (placements[`rack-${index}`]) {
      return;
    }
    setActiveTile({ ...tile, rackIndex: index });
  }, [placements]);

  const handleCellClick = useCallback((id: string) => {
    applyDrop(activeTile, cellId(id));
  }, [activeTile, applyDrop]);

  const hasBoardPlacements = useMemo(() => (
    Object.values(placements).some((tile) => !tile.coordinate.startsWith('rack-'))
  ), [placements]);

  const placementsPayload = useMemo(() => (
    Object.values(placements)
      .filter((tile) => !tile.coordinate.startsWith('rack-'))
      .map((tile) => ({
        coordinate: tile.coordinate,
        letter: tile.assignedLetter,
        blank: tile.blank
      }))
  ), [placements]);

  const getHasPlacements = useCallback(() => Object.keys(placementsRef.current).length > 0, []);

  return {
    placements,
    setPlacements,
    placementsPayload,
    hasBoardPlacements,
    activeTile,
    activeTileLabel,
    pendingBlank,
    confirmBlank,
    cancelBlank,
    handleDragStart,
    handleDragEnd,
    handleTileSelect,
    handleCellClick,
    applyDrop,
    resetLocalState,
    getHasPlacements
  };
}
