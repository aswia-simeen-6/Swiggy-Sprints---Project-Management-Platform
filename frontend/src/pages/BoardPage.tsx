import { useEffect, useRef, useCallback, useState } from 'react';
import { useParams } from 'react-router-dom';
import { DndContext, DragOverlay, closestCorners, PointerSensor, useSensor, useSensors, type DragStartEvent, type DragEndEvent } from '@dnd-kit/core';
import { motion, AnimatePresence } from 'framer-motion';
import { Plus, PanelLeftClose, PanelLeft } from 'lucide-react';
import { useBoardStore } from '@/store/boardStore';
import { usePresenceStore } from '@/store/presenceStore';
import { wsService } from '@/services/websocket';
import { toast } from 'sonner';
import BoardColumn from '@/components/board/BoardColumn';
import IssueCard from '@/components/board/IssueCard';
import SkeletonBoard from '@/components/board/SkeletonBoard';
import CreateIssueModal from '@/components/board/CreateIssueModal';
import IssueDetailModal from '@/components/board/IssueDetailModal';
import SprintPanel from '@/components/board/SprintPanel';
import type { Issue, BoardEvent, PresenceUpdate } from '@/types';

export default function BoardPage() {
  const { projectId } = useParams<{ projectId: string }>();
  const { board, isLoading, fetchBoard, applyEvent, optimisticTransition } = useBoardStore();
  const setPresence = usePresenceStore((s) => s.setPresence);
  const clearPresence = usePresenceStore((s) => s.clear);
  const [activeIssue, setActiveIssue] = useState<Issue | null>(null);
  const [showCreateIssue, setShowCreateIssue] = useState(false);
  const [selectedIssue, setSelectedIssue] = useState<Issue | null>(null);
  const [sprintPanelOpen, setSprintPanelOpen] = useState(true);
  const heartbeatRef = useRef<ReturnType<typeof setInterval>>();

  // Drag sensors with activation constraint to avoid accidental drags
  const sensors = useSensors(
    useSensor(PointerSensor, { activationConstraint: { distance: 8 } }),
  );

  // Fetch board + set up WebSocket subscriptions
  useEffect(() => {
    if (!projectId) return;

    fetchBoard(projectId);

    // WebSocket subscriptions
    const unsubs: (() => void)[] = [];

    if (wsService.isConnected) {
      wsService.joinBoard(projectId);

      unsubs.push(
        wsService.subscribeToBoardUpdates(projectId, (event: BoardEvent) => {
          applyEvent(event);
          // Show toast for others' changes
          if (event.type === 'ISSUE_TRANSITIONED') {
            toast.info(`${event.payload.issueKey} moved to ${event.payload.toStatus}`, {
              duration: 3000,
            });
          }
        }),
      );

      unsubs.push(
        wsService.subscribeToPresence(projectId, (update: PresenceUpdate) => {
          setPresence(update.users, update.count);
        }),
      );

      // Heartbeat every 30s
      heartbeatRef.current = setInterval(() => {
        wsService.sendHeartbeat(projectId);
      }, 30000);
    }

    return () => {
      unsubs.forEach((unsub) => unsub());
      if (heartbeatRef.current) clearInterval(heartbeatRef.current);
      if (projectId && wsService.isConnected) {
        wsService.leaveBoard(projectId);
      }
      clearPresence();
    };
  }, [projectId]);

  // Drag handlers
  const handleDragStart = useCallback((event: DragStartEvent) => {
    const issue = board?.columns
      .flatMap((c) => c.issues)
      .find((i) => i.id === event.active.id);
    setActiveIssue(issue ?? null);
  }, [board]);

  const handleDragEnd = useCallback((event: DragEndEvent) => {
    setActiveIssue(null);
    const { active, over } = event;
    if (!over || !board) return;

    const issueId = active.id as string;
    const targetColumnId = over.id as string;

    // Find which column the issue currently belongs to
    const sourceCol = board.columns.find((c) =>
      c.issues.some((i) => i.id === issueId),
    );
    if (!sourceCol || sourceCol.statusId === targetColumnId) return;

    // Optimistic transition
    optimisticTransition(issueId, targetColumnId);
  }, [board, optimisticTransition]);

  if (isLoading || !board) return <SkeletonBoard />;

  return (
    <div className="h-full flex flex-col">
      {/* Board header */}
      <div className="px-6 py-4 flex items-center justify-between shrink-0">
        <div>
          <h1 className="text-xl font-bold text-text-primary">{board.projectName}</h1>
          <div className="flex items-center gap-3 mt-1">
            <span className="text-xs font-mono text-text-muted bg-surface-tertiary px-2 py-0.5 rounded">
              {board.projectKey}
            </span>
            {board.activeSprintName && (
              <span className="text-xs text-brand-400 font-medium">
                {board.activeSprintName}
              </span>
            )}
          </div>
        </div>
        <motion.button
          whileHover={{ scale: 1.02 }}
          whileTap={{ scale: 0.98 }}
          onClick={() => setShowCreateIssue(true)}
          className="btn-brand text-sm"
        >
          <Plus className="w-4 h-4" />
          Create Issue
        </motion.button>
      </div>

      {/* Board area: sidebar + columns */}
      <div className="flex-1 flex min-h-0">
        {/* Sprint sidebar */}
        {projectId && (
          <AnimatePresence initial={false}>
            {sprintPanelOpen && (
              <motion.div
                initial={{ width: 0, opacity: 0 }}
                animate={{ width: 256, opacity: 1 }}
                exit={{ width: 0, opacity: 0 }}
                transition={{ duration: 0.2, ease: 'easeInOut' }}
                className="shrink-0 overflow-hidden border-r border-surface-border"
              >
                <div className="w-64 h-full overflow-y-auto px-3 py-3">
                  <SprintPanel projectId={projectId} onSprintChanged={() => projectId && fetchBoard(projectId)} onIssueClick={(issue) => setSelectedIssue(issue)} />
                </div>
              </motion.div>
            )}
          </AnimatePresence>
        )}

        {/* Sprint toggle + Board columns */}
        <div className="flex-1 flex flex-col min-w-0">
          {/* Toggle button */}
          {projectId && (
            <div className="px-4 pt-2">
              <button
                onClick={() => setSprintPanelOpen(!sprintPanelOpen)}
                className="text-text-muted hover:text-text-primary transition-colors p-1 rounded-lg hover:bg-surface-hover"
                title={sprintPanelOpen ? 'Hide sprints' : 'Show sprints'}
              >
                {sprintPanelOpen ? <PanelLeftClose className="w-4 h-4" /> : <PanelLeft className="w-4 h-4" />}
              </button>
            </div>
          )}

          <DndContext
            sensors={sensors}
            collisionDetection={closestCorners}
            onDragStart={handleDragStart}
            onDragEnd={handleDragEnd}
          >
            <div className="flex-1 flex gap-4 px-6 pb-6 pt-2">
              {board.columns.map((column, i) => (
                <BoardColumn
                  key={column.statusId}
                  column={column}
                  index={i}
                  onIssueClick={(issue) => setSelectedIssue(issue)}
                />
              ))}
            </div>

            {/* Drag overlay — the ghost card that follows the cursor */}
            <DragOverlay dropAnimation={{
              duration: 200,
              easing: 'cubic-bezier(0.16, 1, 0.3, 1)',
            }}>
              {activeIssue && (
                <div className="rotate-[2deg] scale-105">
                  <IssueCard issue={activeIssue} isDragOverlay />
                </div>
              )}
            </DragOverlay>
          </DndContext>
        </div>
      </div>

      {/* Create Issue Modal */}
      <AnimatePresence>
        {showCreateIssue && projectId && (
          <CreateIssueModal
            projectId={projectId}
            onClose={() => setShowCreateIssue(false)}
            onCreated={() => {
              setShowCreateIssue(false);
              if (projectId) fetchBoard(projectId);
            }}
          />
        )}
      </AnimatePresence>

      {/* Issue Detail Modal */}
      <AnimatePresence>
        {selectedIssue && (
          <IssueDetailModal
            issue={selectedIssue}
            onClose={() => setSelectedIssue(null)}
            onUpdated={() => {
              setSelectedIssue(null);
              if (projectId) fetchBoard(projectId);
            }}
          />
        )}
      </AnimatePresence>
    </div>
  );
}