import { useDroppable } from '@dnd-kit/core';
import { motion } from 'framer-motion';
import type { BoardColumn as BoardColumnType, Issue } from '@/types';
import IssueCard from './IssueCard';

interface Props {
  column: BoardColumnType;
  index: number;
  onIssueClick?: (issue: Issue) => void;
}

const categoryColors: Record<string, string> = {
  TODO: 'bg-text-muted',
  IN_PROGRESS: 'bg-brand-400',
  DONE: 'bg-status-success',
};

export default function BoardColumn({ column, index, onIssueClick }: Props) {
  const { setNodeRef, isOver } = useDroppable({ id: column.statusId });

  return (
    <motion.div
      initial={{ opacity: 0, y: 20 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ delay: index * 0.08, type: 'spring', stiffness: 300, damping: 25 }}
      className="w-[220px] flex-1 flex flex-col"
    >
      {/* Column header */}
      <div className="flex items-center justify-between mb-3 px-1">
        <div className="flex items-center gap-2.5">
          <span className={`status-dot ${categoryColors[column.category] ?? 'bg-text-muted'}`} />
          <h3 className="text-sm font-semibold text-text-primary">{column.name}</h3>
          <span className="text-xs text-text-muted bg-surface-tertiary px-1.5 py-0.5 rounded-md font-medium">
            {column.issueCount}
          </span>
        </div>
        {column.totalStoryPoints > 0 && (
          <span className="text-xs text-text-muted font-medium">
            {column.totalStoryPoints} pts
          </span>
        )}
      </div>

      {/* Drop zone */}
      <div
        ref={setNodeRef}
        className={`
          flex-1 rounded-2xl p-2 space-y-2 min-h-[200px] transition-all duration-200 overflow-y-auto
          ${isOver
            ? 'bg-brand-400/8 border-2 border-dashed border-brand-400/30 shadow-glow-sm'
            : 'bg-surface-primary/50 border-2 border-transparent'
          }
        `}
      >
        {column.issues.map((issue, i) => (
          <IssueCard key={issue.id} issue={issue} index={i} onClick={() => onIssueClick?.(issue)} />
        ))}

        {/* Empty column state */}
        {column.issues.length === 0 && (
          <div className="flex items-center justify-center h-32 text-text-muted text-sm">
            {isOver ? (
              <motion.span
                initial={{ opacity: 0, scale: 0.9 }}
                animate={{ opacity: 1, scale: 1 }}
                className="text-brand-400 font-medium"
              >
                Drop here
              </motion.span>
            ) : (
              'No issues'
            )}
          </div>
        )}
      </div>
    </motion.div>
  );
}