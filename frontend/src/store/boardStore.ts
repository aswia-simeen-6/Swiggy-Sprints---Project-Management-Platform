import { create } from 'zustand';
import type { Board, Issue, BoardEvent, UUID } from '@/types';
import { boardApi, issueApi } from '@/services/api';

interface BoardState {
  board: Board | null;
  isLoading: boolean;
  error: string | null;
  fetchBoard: (projectId: string) => Promise<void>;
  applyEvent: (event: BoardEvent) => void;
  moveIssue: (issueId: UUID, fromColumnId: UUID, toColumnId: UUID) => void;
  optimisticTransition: (issueId: UUID, targetStatusId: UUID) => Promise<void>;
}

export const useBoardStore = create<BoardState>()((set, get) => ({
  board: null,
  isLoading: false,
  error: null,

  fetchBoard: async (projectId) => {
    set({ isLoading: true, error: null });
    try {
      const board = await boardApi.get(projectId);
      set({ board, isLoading: false });
    } catch (err) {
      set({ error: 'Failed to load board', isLoading: false });
    }
  },

  // Optimistic drag-and-drop: move issue between columns locally
  moveIssue: (issueId, fromColumnId, toColumnId) => {
    const { board } = get();
    if (!board) return;

    const columns = board.columns.map((col) => ({ ...col, issues: [...col.issues] }));
    const fromCol = columns.find((c) => c.statusId === fromColumnId);
    const toCol = columns.find((c) => c.statusId === toColumnId);
    if (!fromCol || !toCol) return;

    const issueIdx = fromCol.issues.findIndex((i) => i.id === issueId);
    if (issueIdx === -1) return;

    const [issue] = fromCol.issues.splice(issueIdx, 1);
    const moved = { ...issue, statusId: toColumnId, statusName: toCol.name };
    toCol.issues.push(moved);

    fromCol.issueCount = fromCol.issues.length;
    toCol.issueCount = toCol.issues.length;

    set({ board: { ...board, columns } });
  },

  // Transition via API after optimistic update
  optimisticTransition: async (issueId, targetStatusId) => {
    const { board } = get();
    if (!board) return;

    const issue = board.columns.flatMap((c) => c.issues).find((i) => i.id === issueId);
    if (!issue) return;

    const fromColumnId = issue.statusId;

    // Optimistic: move locally first
    get().moveIssue(issueId, fromColumnId, targetStatusId);

    try {
      await issueApi.transition(issueId, { targetStatusId });
    } catch {
      // Rollback: move back
      get().moveIssue(issueId, targetStatusId, fromColumnId);
    }
  },

  // Apply a WebSocket event to the board state
  applyEvent: (event) => {
    const { board } = get();
    if (!board || event.projectId !== board.projectId) return;

    const payload = event.payload;

    switch (event.type) {
      case 'ISSUE_TRANSITIONED': {
        const issueId = payload.issueId as UUID;
        const newStatusId = payload.statusId as UUID;
        const columns = board.columns.map((col) => ({ ...col, issues: [...col.issues] }));

        // Find and move the issue
        for (const col of columns) {
          const idx = col.issues.findIndex((i) => i.id === issueId);
          if (idx !== -1) {
            const [issue] = col.issues.splice(idx, 1);
            const targetCol = columns.find((c) => c.statusId === newStatusId);
            if (targetCol) {
              targetCol.issues.push({
                ...issue,
                statusId: newStatusId,
                statusName: targetCol.name,
              });
            }
            break;
          }
        }

        columns.forEach((c) => { c.issueCount = c.issues.length; });
        set({ board: { ...board, columns } });
        break;
      }

      case 'ISSUE_CREATED': {
        // Refetch to get the full issue — simpler and reliable
        get().fetchBoard(board.projectId);
        break;
      }

      case 'ISSUE_UPDATED': {
        const issueId = payload.issueId as UUID;
        const columns = board.columns.map((col) => ({
          ...col,
          issues: col.issues.map((i) =>
            i.id === issueId ? { ...i, ...(payload.changes as Partial<Issue>) } : i,
          ),
        }));
        set({ board: { ...board, columns } });
        break;
      }

      default:
        // For sprint events etc., refetch
        get().fetchBoard(board.projectId);
    }
  },
}));
