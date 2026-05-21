import { useState, useEffect, type FormEvent } from 'react';
import { motion } from 'framer-motion';
import { X } from 'lucide-react';
import { issueApi, projectApi } from '@/services/api';
import { useBoardStore } from '@/store/boardStore';
import { toast } from 'sonner';
import type { IssueType, Priority, ProjectMember } from '@/types';

interface Props {
  projectId: string;
  onClose: () => void;
  onCreated: () => void;
}

export default function CreateIssueModal({ projectId, onClose, onCreated }: Props) {
  const [title, setTitle] = useState('');
  const [description, setDescription] = useState('');
  const [issueType, setIssueType] = useState<IssueType>('TASK');
  const [priority, setPriority] = useState<Priority>('MEDIUM');
  const [parentId, setParentId] = useState('');
  const [assigneeId, setAssigneeId] = useState('');
  const [members, setMembers] = useState<ProjectMember[]>([]);
  const [loading, setLoading] = useState(false);

  // Get all non-subtask issues from the board for parent selection
  const board = useBoardStore((s) => s.board);
  const parentCandidates = board?.columns
    .flatMap((c) => c.issues)
    .filter((i) => i.issueType !== 'SUBTASK') ?? [];

  // Fetch project members for assignee picker
  useEffect(() => {
    projectApi.members(projectId).then(setMembers).catch(() => {});
  }, [projectId]);

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault();
    setLoading(true);
    try {
      await issueApi.create(projectId, {
        title,
        description: description || undefined,
        issueType,
        priority,
        parentId: issueType === 'SUBTASK' && parentId ? parentId : undefined,
        assigneeId: assigneeId || undefined,
      });
      toast.success('Issue created');
      onCreated();
    } catch {
      toast.error('Failed to create issue');
    } finally {
      setLoading(false);
    }
  };

  return (
    <>
      <motion.div
        initial={{ opacity: 0 }}
        animate={{ opacity: 1 }}
        exit={{ opacity: 0 }}
        onClick={onClose}
        className="fixed inset-0 bg-black/60 backdrop-blur-sm z-40"
      />

      <motion.div
        initial={{ opacity: 0, scale: 0.95, y: 20 }}
        animate={{ opacity: 1, scale: 1, y: 0 }}
        exit={{ opacity: 0, scale: 0.95, y: 20 }}
        transition={{ type: 'spring', stiffness: 300, damping: 25 }}
        className="fixed inset-0 flex items-center justify-center z-50 p-4"
      >
        <div className="card p-8 w-full max-w-lg shadow-float" onClick={(e) => e.stopPropagation()}>
          <div className="flex items-center justify-between mb-6">
            <h2 className="text-xl font-bold text-text-primary">Create Issue</h2>
            <button onClick={onClose} className="text-text-muted hover:text-text-primary transition-colors">
              <X className="w-5 h-5" />
            </button>
          </div>

          <form onSubmit={handleSubmit} className="space-y-5">
            {/* Type + Priority row */}
            <div className="grid grid-cols-2 gap-4">
              <div>
                <label className="block text-sm font-medium text-text-secondary mb-2">Type</label>
                <select
                  value={issueType}
                  onChange={(e) => setIssueType(e.target.value as IssueType)}
                  className="input-field"
                >
                  <option value="TASK">Task</option>
                  <option value="BUG">Bug</option>
                  <option value="STORY">Story</option>
                  <option value="EPIC">Epic</option>
                  <option value="SUBTASK">Subtask</option>
                </select>
              </div>
              <div>
                <label className="block text-sm font-medium text-text-secondary mb-2">Priority</label>
                <select
                  value={priority}
                  onChange={(e) => setPriority(e.target.value as Priority)}
                  className="input-field"
                >
                  <option value="CRITICAL">Critical</option>
                  <option value="HIGH">High</option>
                  <option value="MEDIUM">Medium</option>
                  <option value="LOW">Low</option>
                </select>
              </div>
            </div>

            {/* Assignee */}
            <div>
              <label className="block text-sm font-medium text-text-secondary mb-2">Assignee</label>
              <select
                value={assigneeId}
                onChange={(e) => setAssigneeId(e.target.value)}
                className="input-field"
              >
                <option value="">Unassigned</option>
                {members.map((m) => (
                  <option key={m.userId} value={m.userId}>
                    {m.displayName}
                  </option>
                ))}
              </select>
            </div>

            {/* Parent issue (for Subtasks) */}
            {issueType === 'SUBTASK' && (
              <div>
                <label className="block text-sm font-medium text-text-secondary mb-2">Parent Issue</label>
                <select
                  value={parentId}
                  onChange={(e) => setParentId(e.target.value)}
                  className="input-field"
                  required
                >
                  <option value="">Select parent issue…</option>
                  {parentCandidates.map((issue) => (
                    <option key={issue.id} value={issue.id}>
                      {issue.issueKey} — {issue.title}
                    </option>
                  ))}
                </select>
              </div>
            )}

            <div>
              <label className="block text-sm font-medium text-text-secondary mb-2">Title</label>
              <input
                value={title}
                onChange={(e) => setTitle(e.target.value)}
                className="input-field"
                placeholder="What needs to be done?"
                required
                autoFocus
              />
            </div>

            <div>
              <label className="block text-sm font-medium text-text-secondary mb-2">Description</label>
              <textarea
                value={description}
                onChange={(e) => setDescription(e.target.value)}
                className="input-field resize-none h-28"
                placeholder="Add details, context, or acceptance criteria..."
              />
            </div>

            <div className="flex gap-3 pt-2">
              <button type="button" onClick={onClose} className="btn-ghost flex-1">
                Cancel
              </button>
              <motion.button
                type="submit"
                disabled={loading}
                whileTap={{ scale: 0.98 }}
                className="btn-brand flex-1"
              >
                {loading ? (
                  <div className="w-5 h-5 border-2 border-white/30 border-t-white rounded-full animate-spin" />
                ) : (
                  'Create Issue'
                )}
              </motion.button>
            </div>
          </form>
        </div>
      </motion.div>
    </>
  );
}