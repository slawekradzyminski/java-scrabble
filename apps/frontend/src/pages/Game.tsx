import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { closestCenter, DndContext, PointerSensor, useSensor, useSensors } from '@dnd-kit/core';
import type { RackTile } from '../types';
import { joinRoom, startGame } from '../api';
import BoardSection from './game/components/BoardSection';
import BagModal from './game/components/BagModal';
import BlankLetterModal from './game/components/BlankLetterModal';
import GameActions from './game/components/GameActions';
import GameHeader from './game/components/GameHeader';
import GameInfoPanel from './game/components/GameInfoPanel';
import HistoryPanel from './game/components/HistoryPanel';
import PlayersPanel from './game/components/PlayersPanel';
import { useGameSocket } from './game/hooks/useGameSocket';
import { usePlacements } from './game/hooks/usePlacements';
import { useRackTiles } from './game/hooks/useRackTiles';
import { BAG_TILES, buildUsedTileCounts } from './game/utils/bagTiles';
import { scoreMove } from './game/utils/scoreMove';

interface GameProps {
  roomId: string;
  player: string;
  onLeave: () => void;
}

export default function Game({ roomId, player, onLeave }: GameProps) {
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);
  const [historyExpanded, setHistoryExpanded] = useState(false);
  const [headerExpanded, setHeaderExpanded] = useState(false);
  const [bagModalOpen, setBagModalOpen] = useState(false);

  const rackTilesRef = useRef<RackTile[]>([]);

  const {
    placements,
    placementsPayload,
    hasBoardPlacements,
    activeTile,
    activeTileLabel,
    handleDragStart,
    handleDragEnd,
    handleTileSelect,
    handleCellClick,
    applyDrop,
    resetLocalState,
    getHasPlacements,
    pendingBlank,
    confirmBlank,
    cancelBlank
  } = usePlacements({
    getRackTile: (index) => rackTilesRef.current[index]
  });

  const {
    snapshot,
    connectionState,
    hasSynced,
    eventLog,
    lastEventAt,
    connect,
    disconnect,
    requestSync,
    socket
  } = useGameSocket({
    onResetLocal: resetLocalState,
    hasLocalPlacements: getHasPlacements
  });

  const rackTiles = useRackTiles(snapshot, player, placements);

  useEffect(() => {
    rackTilesRef.current = rackTiles;
  }, [rackTiles]);

  const sensors = useSensors(useSensor(PointerSensor, { activationConstraint: { distance: 8 } }));

  useEffect(() => {
    if (!roomId || !player) {
      return;
    }

    let active = true;
    setError(null);

    joinRoom(roomId, player)
      .then(() => {
        if (!active) {
          return;
        }
        connect(roomId, player);
      })
      .catch((err) => {
        if (!active) {
          return;
        }
        setError(err instanceof Error ? err.message : 'Failed to join room');
      });

    return () => {
      active = false;
    };
  }, [roomId, player, connect]);

  useEffect(() => {
    return () => {
      disconnect();
    };
  }, [disconnect]);

  useEffect(() => {
    document.body.dataset.gpu = 'safe';
    return () => {
      delete document.body.dataset.gpu;
    };
  }, []);

  const handleStartGame = async () => {
    if (!roomId) {
      return;
    }
    setBusy(true);
    setError(null);
    try {
      await startGame(roomId);
      if (player) {
        requestSync();
      }
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to start game');
    } finally {
      setBusy(false);
    }
  };

  const commitMove = useCallback(() => {
    const socketInstance = socket.current;
    if (!socketInstance?.isConnected() || !roomId) {
      return;
    }
    socketInstance.send({
      type: 'PLAY_TILES',
      payload: {
        player,
        placements: placementsPayload
      }
    });
  }, [placementsPayload, player, roomId, socket]);

  const sendPass = useCallback(() => {
    socket.current?.send({ type: 'PASS', payload: { player } });
  }, [player, socket]);

  const sendResign = useCallback(() => {
    socket.current?.send({ type: 'RESIGN', payload: { player } });
  }, [player, socket]);

  const isSocketReady = connectionState === 'connected' && hasSynced;
  const isConnecting = (connectionState === 'connecting' || connectionState === 'reconnecting') && !hasSynced;
  const connectionLabel = connectionState === 'reconnecting' ? 'Reconnecting…' : 'Connecting…';

  const usedTiles = useMemo(() => (
    buildUsedTileCounts(snapshot.board, placements, rackTiles)
  ), [snapshot.board, placements, rackTiles]);

  const opponentRackSize = snapshot.players.find(p => p.name !== player)?.rackSize ?? 0;

  const lastMove = useMemo(() => {
    const entry = eventLog.find((item) => item.type === 'MOVE_ACCEPTED');
    if (!entry || !entry.payload) {
      return entry?.summary ?? null;
    }
    const playerName = typeof entry.payload.player === 'string' ? entry.payload.player : null;
    const score = typeof entry.payload.score === 'number' ? entry.payload.score : null;
    const wordsPayload = Array.isArray(entry.payload.words) ? entry.payload.words : [];
    const words = wordsPayload.map((word: { text?: string }) => word.text).filter(Boolean) as string[];
    if (words.length === 0 && score === null) {
      return entry.summary;
    }
    const nameLabel = playerName ?? 'Unknown';
    const wordsLabel = words.length > 0 ? words.join(', ') : 'word';
    const scoreLabel = score !== null ? ` (+${score} pts)` : '';
    return `${nameLabel} played ${wordsLabel}${scoreLabel}`;
  }, [eventLog]);

  const movePreview = useMemo(() => scoreMove(snapshot.board, placements), [snapshot.board, placements]);
  const previewText = movePreview ? `Preview: ${movePreview.words.join(', ')} (${movePreview.score} pts)` : undefined;

  return (
    <>
      <GameHeader
        expanded={headerExpanded}
        onToggle={() => setHeaderExpanded((prev) => !prev)}
        onLeave={onLeave}
      />

      <DndContext
        sensors={sensors}
        collisionDetection={closestCenter}
        onDragStart={handleDragStart}
        onDragEnd={handleDragEnd}
      >
        <main className="layout">
          <BoardSection
            isConnecting={isConnecting}
            connectionLabel={connectionLabel}
            board={snapshot.board}
            placements={placements}
            activeTile={activeTile}
            activeTileLabel={activeTileLabel}
            rackTiles={rackTiles}
            onCellClick={handleCellClick}
            onTileSelect={handleTileSelect}
            applyDrop={applyDrop}
          />

          <aside className="sidebar">
            <div className="panel game-info">
              <GameInfoPanel
                snapshot={snapshot}
                player={player}
                error={error}
                busy={busy}
                isSocketReady={isSocketReady}
                onStartGame={handleStartGame}
                onOpenBag={() => setBagModalOpen(true)}
              />

              <PlayersPanel
                players={snapshot.players}
                currentIndex={snapshot.currentPlayerIndex}
              />

              {lastMove ? (
                <div className="info-section last-move">
                  <h3>Last move</h3>
                  <p className="last-move__text">{lastMove}</p>
                </div>
              ) : null}

              <HistoryPanel
                eventLog={eventLog}
                lastEventAt={lastEventAt}
                expanded={historyExpanded}
                onToggle={() => setHistoryExpanded((prev) => !prev)}
              />

              <GameActions
                canPlay={hasBoardPlacements}
                canReset={Object.keys(placements).length > 0}
                isSocketReady={isSocketReady}
                onPlay={commitMove}
                onReset={resetLocalState}
                onPass={sendPass}
                onResign={sendResign}
                previewText={previewText}
              />
            </div>
          </aside>
        </main>
      </DndContext>

      {bagModalOpen && (
        <BagModal
          bagCount={snapshot.bagCount}
          opponentRackSize={opponentRackSize}
          bagTiles={BAG_TILES}
          usedTiles={usedTiles}
          onClose={() => setBagModalOpen(false)}
        />
      )}
      {pendingBlank && (
        <BlankLetterModal
          onSelect={confirmBlank}
          onCancel={cancelBlank}
        />
      )}
    </>
  );
}
