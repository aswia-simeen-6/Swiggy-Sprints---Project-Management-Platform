import { useDraggable } from '@dnd-kit/core';
import { motion } from 'framer-motion';
import { Bug, BookOpen, Zap, Target, Layers } from 'lucide-react';
import type { Issue, IssueType, Priority } from '@/types';

interface Props {
  issue: Issue;
  index?: number;
  isDragOverlay?: boolean;
  onClick?: () => void;
}

const issueTypeIcons: Record<IssueType, typeof Bug> = {
  BUG: Bug,
  STORY: BookOpen,
  TASK: Zap,
  EPIC: Target,
  SUBTASK: Layers,
};

const issueTypeColors: Record<IssueType, string> = {
  BUG: 'text-status-error',
  STORY: 'text-status-success',
  TASK: 'text-brand-400',
  EPIC: 'text-purple-400',
  SUBTASK: 'text-status-info',
};

const priorityConfig: Record<Priority, { color: string; bg: string }> = {
  CRITICAL: { color: 'text-priority-critical', bg: 'bg-priority-critical/10' },
  HIGH:     { color: 'text-priority-high', bg: 'bg-priority-high/10' },
  MEDIUM:   { color: 'text-priority-medium', bg: 'bg-priority-medium/10' },
  LOW:      { color: 'text-priority-low', bg: 'bg-priority-low/10' },
};

export default function IssueCard({ issue, index = 0, isDragOverlay = false, onClick }: Props) {
  const { attributes, listeners, setNodeRef, isDragging } = useDraggable({
    id: issue.id,
    disabled: isDragOverlay,
  });

  const TypeIcon = issueTypeIcons[issue.issueType] ?? Zap;
  const priority = priorityConfig[issue.priority];

  return (
    <motion.div
      ref={setNodeRef}
      {...attributes}
      {...listeners}
      initial={!isDragOverlay ? { opacity: 0, y: 8 } : false}
      animate={!isDragOverlay ? { opacity: 1, y: 0 } : undefined}
      transition={{ delay: index * 0.03, type: 'spring', stiffness: 400, damping: 25 }}
      onClick={() => !isDragging && onClick?.()}
      className={`
        card-interactive p-3.5 group select-none cursor-grab active:cursor-grabbing
        ${isDragging ? 'opacity-30 shadow-none' : ''}
        ${isDragOverlay ? 'shadow-float ring-2 ring-brand-400/40' : ''}
      `}
    >
      {/* Issue key */}
      <div className="flex items-center gap-2 mb-2">
        <TypeIcon className={`w-3.5 h-3.5 ${issueTypeColors[issue.issueType]}`} />

        <span className="text-xs font-mono text-text-muted">{issue.issueKey}</span>

        {/* Priority badge */}
        <span className={`priority-badge ml-auto ${priority.color} ${priority.bg}`}>
          {issue.priority}
        </span>
      </div>

      {/* Title */}
      <h4 className="text-sm font-medium text-text-primary leading-snug mb-2 line-clamp-2">
        {issue.title}
      </h4>

      {/* Footer: assignee + labels + story points */}
      <div className="flex items-center justify-between">
        <div className="flex items-center gap-1.5 overflow-hidden">
          {issue.assigneeName && (
            <span
              className="w-5 h-5 rounded-full bg-brand-400/20 text-brand-400 flex items-center justify-center text-2xs font-bold shrink-0"
              title={issue.assigneeName}
            >
              {issue.assigneeName.charAt(0).toUpperCase()}
            </span>
          )}
          {issue.labels.slice(0, 2).map((label) => (
            <span
              key={label}
              className="text-2xs text-text-muted bg-surface-tertiary px-1.5 py-0.5 rounded truncate max-w-[80px]"
            >
              {label}
            </span>
          ))}
          {issue.labels.length > 2 && (
            <span className="text-2xs text-text-muted">+{issue.labels.length - 2}</span>
          )}
        </div>

        {issue.storyPoints != null && issue.storyPoints > 0 && (
          <span className="w-6 h-6 bg-surface-tertiary rounded-md flex items-center justify-center text-2xs font-bold text-text-secondary">
            {issue.storyPoints}
          </span>
        )}
      </div>
    </motion.div>
  );
}