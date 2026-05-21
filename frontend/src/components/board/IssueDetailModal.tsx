import { useState, useEffect, type FormEvent } from 'react';
import { motion } from 'framer-motion';
import { X, Save, Eye, EyeOff, Send, Trash2, AlertTriangle } from 'lucide-react';
import { issueApi, projectApi, commentApi, activityApi, sprintApi } from '@/services/api';
import { useAuthStore } from '@/store/authStore';
import { toast } from 'sonner';
import type { Issue, Priority, ProjectMember, Comment, Activity, Sprint } from '@/types';

type Tab = 'details' | 'comments' | 'activity' | 'subtasks';

interface Props {
  issue: Issue;
  onClose: () => void;
  onUpdated: () => void;
}

export default function IssueDetailModal({ issue, onClose, onUpdated }: Props) {
  const [activeTab, setActiveTab] = useState<Tab>('details');
  const [title, setTitle] = useState(issue.title);
  const [description, setDescription] = useState(issue.description ?? '');
  const [priority, setPriority] = useState<Priority>(issue.priority);
  const [assigneeId, setAssigneeId] = useState(issue.assigneeId ?? '');
  const [storyPoints, setStoryPoints] = useState(issue.storyPoints?.toString() ?? '');
  const [sprintId, setSprintId] = useState(issue.sprintId ?? '');
  const [loading, setLoading] = useState(false);
  const [members, setMembers] = useState<ProjectMember[]>([]);
  const [sprints, setSprints] = useState<Sprint[]>([]);

  // Watch state
  const [watching, setWatching] = useState(false);
  const [watchLoading, setWatchLoading] = useState(false);

  // Comments state
  const [comments, setComments] = useState<Comment[]>([]);
  const [newComment, setNewComment] = useState('');
  const [commentLoading, setCommentLoading] = useState(false);

  // Activity state
  const [activities, setActivities] = useState<Activity[]>([]);

  // Subtasks state
  const [subtasks, setSubtasks] = useState<Issue[]>([]);

  // Delete state
  const [showDeleteConfirm, setShowDeleteConfirm] = useState(false);
  const [deleteLoading, setDeleteLoading] = useState(false);

  const currentUserId = useAuthStore((s) => s.user?.id);

  useEffect(() => {
    projectApi.members(issue.projectId).then(setMembers).catch(() => {});
    sprintApi.list(issue.projectId).then(setSprints).catch(() => {});
  }, [issue.projectId]);

  // Load tab data when tab changes
  useEffect(() => {
    if (activeTab === 'comments') {
      commentApi.list(issue.id).then(setComments).catch(() => {});
    } else if (activeTab === 'activity') {
      activityApi.issueActivity(issue.id).then(setActivities).catch(() => {});
    } else if (activeTab === 'subtasks') {
      issueApi.children(issue.id).then(setSubtasks).catch(() => {});
    }
  }, [activeTab, issue.id]);

  const hasChanges =
    title !== issue.title ||
    description !== (issue.description ?? '') ||
    priority !== issue.priority ||
    assigneeId !== (issue.assigneeId ?? '') ||
    storyPoints !== (issue.storyPoints?.toString() ?? '') ||
    sprintId !== (issue.sprintId ?? '');

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault();
    if (!hasChanges) return;
    setLoading(true);
    try {
      await issueApi.update(issue.id, {
        title,
        description: description || undefined,
        priority,
        assigneeId: assigneeId || undefined,
        sprintId: sprintId || undefined,
        storyPoints: storyPoints ? parseInt(storyPoints, 10) : undefined,
        version: issue.version,
      });
      toast.success('Issue updated');
      onUpdated();
    } catch {
      toast.error('Failed to update issue');
    } finally {
      setLoading(false);
    }
  };

  const toggleWatch = async () => {
    setWatchLoading(true);
    try {
      if (watching) {
        await issueApi.unwatch(issue.id);
        setWatching(false);
        toast.success('Unwatched');
      } else {
        await issueApi.watch(issue.id);
        setWatching(true);
        toast.success('Watching this issue');
      }
    } catch {
      toast.error('Failed to update watch status');
    } finally {
      setWatchLoading(false);
    }
  };

  const handleAddComment = async () => {
    if (!newComment.trim()) return;
    setCommentLoading(true);
    try {
      const comment = await commentApi.add(issue.id, { body: newComment });
      setComments((prev) => [comment, ...prev]);
      setNewComment('');
    } catch {
      toast.error('Failed to add comment');
    } finally {
      setCommentLoading(false);
    }
  };

  const handleDeleteComment = async (commentId: string) => {
    try {
      await commentApi.delete(issue.id, commentId);
      setComments((prev) => prev.filter((c) => c.id !== commentId));
      toast.success('Comment deleted');
    } catch {
      toast.error('Failed to delete comment');
    }
  };

  const handleDeleteIssue = async () => {
    setDeleteLoading(true);
    try {
      await issueApi.delete(issue.id);
      toast.success('Issue deleted');
      onUpdated();
    } catch {
      toast.error('Failed to delete issue');
    } finally {
      setDeleteLoading(false);
    }
  };

  const tabs: { key: Tab; label: string }[] = [
    { key: 'details', label: 'Details' },
    { key: 'comments', label: 'Comments' },
    { key: 'activity', label: 'Activity' },
    { key: 'subtasks', label: 'Subtasks' },
  ];

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
        <div
          className="card p-8 w-full max-w-2xl shadow-float max-h-[90vh] overflow-y-auto"
          onClick={(e) => e.stopPropagation()}
        >
          {/* Header */}
          <div className="flex items-center justify-between mb-4">
            <div className="flex items-center gap-3">
              <span className="text-xs font-mono text-text-muted bg-surface-tertiary px-2 py-1 rounded">
                {issue.issueKey}
              </span>
              <span className="text-xs text-text-muted uppercase">{issue.issueType}</span>
              {issue.statusName && (
                <span className="text-xs text-brand-400 bg-brand-400/10 px-2 py-0.5 rounded-md font-medium">
                  {issue.statusName}
                </span>
              )}
            </div>
            <div className="flex items-center gap-2">
              <button
                onClick={toggleWatch}
                disabled={watchLoading}
                className={`p-1.5 rounded-lg transition-colors ${
                  watching
                    ? 'text-brand-400 bg-brand-400/10'
                    : 'text-text-muted hover:text-text-primary hover:bg-surface-hover'
                }`}
                title={watching ? 'Unwatch' : 'Watch'}
              >
                {watching ? <EyeOff className="w-4 h-4" /> : <Eye className="w-4 h-4" />}
              </button>
              <button onClick={onClose} className="text-text-muted hover:text-text-primary transition-colors">
                <X className="w-5 h-5" />
              </button>
            </div>
          </div>

          {/* Tabs */}
          <div className="flex gap-1 mb-5 border-b border-surface-border">
            {tabs.map((tab) => (
              <button
                key={tab.key}
                onClick={() => setActiveTab(tab.key)}
                className={`px-4 py-2 text-sm font-medium transition-colors border-b-2 -mb-px ${
                  activeTab === tab.key
                    ? 'text-brand-400 border-brand-400'
                    : 'text-text-muted border-transparent hover:text-text-primary'
                }`}
              >
                {tab.label}
              </button>
            ))}
          </div>

          {/* Details Tab */}
          {activeTab === 'details' && (
            <form onSubmit={handleSubmit} className="space-y-5">
              <div className="flex gap-6">
                {/* Left — Main content */}
                <div className="flex-1 min-w-0 space-y-4">
                  <div>
                    <label className="block text-xs font-medium text-text-muted mb-1.5">Title</label>
                    <input
                      value={title}
                      onChange={(e) => setTitle(e.target.value)}
                      className="input-field"
                      required
                    />
                  </div>

                  <div>
                    <label className="block text-xs font-medium text-text-muted mb-1.5">Description</label>
                    <textarea
                      value={description}
                      onChange={(e) => setDescription(e.target.value)}
                      className="input-field resize-none h-36"
                      placeholder="Add details…"
                    />
                  </div>

                  {issue.labels.length > 0 && (
                    <div>
                      <label className="block text-xs font-medium text-text-muted mb-1.5">Labels</label>
                      <div className="flex flex-wrap gap-1.5">
                        {issue.labels.map((label) => (
                          <span key={label} className="text-xs text-text-muted bg-surface-tertiary px-2 py-1 rounded">
                            {label}
                          </span>
                        ))}
                      </div>
                    </div>
                  )}

                  <div className="text-xs text-text-muted pt-3 border-t border-surface-border flex gap-6">
                    <span>Created {new Date(issue.createdAt).toLocaleDateString()}</span>
                    <span>Updated {new Date(issue.updatedAt).toLocaleDateString()}</span>
                    <span>v{issue.version}</span>
                  </div>
                </div>

                {/* Right — Metadata sidebar */}
                <div className="w-48 shrink-0 space-y-3">
                  <div>
                    <label className="block text-[10px] font-semibold uppercase text-text-muted tracking-wider mb-1">Assignee</label>
                    <select
                      value={assigneeId}
                      onChange={(e) => setAssigneeId(e.target.value)}
                      className="input-field text-sm py-1.5"
                    >
                      <option value="">Unassigned</option>
                      {members.map((m) => (
                        <option key={m.userId} value={m.userId}>
                          {m.displayName}
                        </option>
                      ))}
                    </select>
                  </div>

                  <div>
                    <label className="block text-[10px] font-semibold uppercase text-text-muted tracking-wider mb-1">Priority</label>
                    <select
                      value={priority}
                      onChange={(e) => setPriority(e.target.value as Priority)}
                      className="input-field text-sm py-1.5"
                    >
                      <option value="CRITICAL">Critical</option>
                      <option value="HIGH">High</option>
                      <option value="MEDIUM">Medium</option>
                      <option value="LOW">Low</option>
                    </select>
                  </div>

                  <div>
                    <label className="block text-[10px] font-semibold uppercase text-text-muted tracking-wider mb-1">Sprint</label>
                    <select
                      value={sprintId}
                      onChange={(e) => setSprintId(e.target.value)}
                      className="input-field text-sm py-1.5"
                    >
                      <option value="">Backlog</option>
                      {sprints
                        .filter((s) => s.status !== 'COMPLETED')
                        .map((s) => (
                          <option key={s.id} value={s.id}>
                            {s.name}
                          </option>
                        ))}
                    </select>
                  </div>

                  <div>
                    <label className="block text-[10px] font-semibold uppercase text-text-muted tracking-wider mb-1">Story Points</label>
                    <input
                      type="number"
                      min="0"
                      value={storyPoints}
                      onChange={(e) => setStoryPoints(e.target.value)}
                      className="input-field text-sm py-1.5"
                      placeholder="—"
                    />
                  </div>
                </div>
              </div>

              <div className="flex gap-3 pt-2">
                <button type="button" onClick={onClose} className="btn-ghost flex-1">
                  Close
                </button>
                <motion.button
                  type="submit"
                  disabled={loading || !hasChanges}
                  whileTap={{ scale: 0.98 }}
                  className="btn-brand flex-1"
                >
                  {loading ? (
                    <div className="w-5 h-5 border-2 border-white/30 border-t-white rounded-full animate-spin" />
                  ) : (
                    <>
                      <Save className="w-4 h-4" />
                      Save Changes
                    </>
                  )}
                </motion.button>
              </div>

              {/* Delete issue */}
              <div className="pt-3 border-t border-surface-border">
                {!showDeleteConfirm ? (
                  <button
                    type="button"
                    onClick={() => setShowDeleteConfirm(true)}
                    className="text-xs text-text-muted hover:text-status-error transition-colors flex items-center gap-1"
                  >
                    <Trash2 className="w-3.5 h-3.5" />
                    Delete issue
                  </button>
                ) : (
                  <div className="flex items-center gap-3 bg-status-error/5 border border-status-error/20 rounded-xl px-4 py-3">
                    <AlertTriangle className="w-4 h-4 text-status-error shrink-0" />
                    <span className="text-sm text-text-secondary flex-1">This cannot be undone.</span>
                    <button
                      type="button"
                      onClick={() => setShowDeleteConfirm(false)}
                      className="text-xs text-text-muted hover:text-text-primary"
                    >
                      Cancel
                    </button>
                    <button
                      type="button"
                      onClick={handleDeleteIssue}
                      disabled={deleteLoading}
                      className="text-xs text-white bg-status-error hover:bg-status-error/80 px-3 py-1.5 rounded-lg font-medium transition-colors"
                    >
                      {deleteLoading ? 'Deleting…' : 'Confirm Delete'}
                    </button>
                  </div>
                )}
              </div>
            </form>
          )}

          {/* Comments Tab */}
          {activeTab === 'comments' && (
            <div className="space-y-4">
              {/* Add comment */}
              <div className="flex gap-2">
                <textarea
                  value={newComment}
                  onChange={(e) => setNewComment(e.target.value)}
                  className="input-field resize-none h-20 flex-1"
                  placeholder="Write a comment…"
                />
                <button
                  onClick={handleAddComment}
                  disabled={commentLoading || !newComment.trim()}
                  className="btn-brand self-end px-3"
                >
                  <Send className="w-4 h-4" />
                </button>
              </div>

              {/* Comment list */}
              {comments.length === 0 && (
                <p className="text-text-muted text-sm text-center py-8">No comments yet</p>
              )}
              {comments.map((c) => (
                <div key={c.id} className="bg-surface-secondary rounded-xl p-4 border border-surface-border">
                  <div className="flex items-center justify-between mb-2">
                    <div className="flex items-center gap-2">
                      <div className="w-6 h-6 rounded-full bg-brand-400/20 flex items-center justify-center text-brand-400 text-xs font-bold">
                        {c.authorName?.charAt(0).toUpperCase() ?? '?'}
                      </div>
                      <span className="text-sm font-medium text-text-primary">{c.authorName}</span>
                      <span className="text-xs text-text-muted">
                        {new Date(c.createdAt).toLocaleString()}
                      </span>
                    </div>
                    {c.authorId === currentUserId && (
                      <button
                        onClick={() => handleDeleteComment(c.id)}
                        className="text-text-muted hover:text-status-error transition-colors"
                      >
                        <Trash2 className="w-3.5 h-3.5" />
                      </button>
                    )}
                  </div>
                  <p className="text-sm text-text-secondary whitespace-pre-wrap">{c.body}</p>
                  {/* Replies */}
                  {c.replies?.length > 0 && (
                    <div className="mt-3 pl-4 border-l-2 border-surface-border space-y-3">
                      {c.replies.map((r) => (
                        <div key={r.id}>
                          <div className="flex items-center gap-2 mb-1">
                            <span className="text-xs font-medium text-text-primary">{r.authorName}</span>
                            <span className="text-xs text-text-muted">
                              {new Date(r.createdAt).toLocaleString()}
                            </span>
                          </div>
                          <p className="text-sm text-text-secondary whitespace-pre-wrap">{r.body}</p>
                        </div>
                      ))}
                    </div>
                  )}
                </div>
              ))}
            </div>
          )}

          {/* Activity Tab */}
          {activeTab === 'activity' && (
            <div className="space-y-3">
              {activities.length === 0 && (
                <p className="text-text-muted text-sm text-center py-8">No activity recorded</p>
              )}
              {activities.map((a) => (
                <div
                  key={a.id}
                  className="flex gap-3 py-2 border-b border-surface-border last:border-0"
                >
                  <div className="w-6 h-6 rounded-full bg-surface-tertiary flex items-center justify-center text-text-muted text-xs font-bold shrink-0 mt-0.5">
                    {a.userName?.charAt(0).toUpperCase() ?? '?'}
                  </div>
                  <div className="flex-1 min-w-0">
                    <p className="text-sm text-text-primary">
                      <span className="font-medium">{a.userName}</span>{' '}
                      <span className="text-text-muted">
                        {a.action.replace(/_/g, ' ').toLowerCase()}
                      </span>
                    </p>
                    {a.changes && Object.keys(a.changes).length > 0 && (
                      <div className="mt-1 text-xs text-text-muted bg-surface-tertiary rounded-lg px-3 py-2">
                        {Object.entries(a.changes).map(([key, val]) => (
                          <div key={key}>
                            <span className="font-medium">{key}</span>: {JSON.stringify(val)}
                          </div>
                        ))}
                      </div>
                    )}
                    <span className="text-xs text-text-muted">
                      {new Date(a.createdAt).toLocaleString()}
                    </span>
                  </div>
                </div>
              ))}
            </div>
          )}

          {/* Subtasks Tab */}
          {activeTab === 'subtasks' && (
            <div className="space-y-2">
              {subtasks.length === 0 && (
                <p className="text-text-muted text-sm text-center py-8">No subtasks</p>
              )}
              {subtasks.map((s) => (
                <div
                  key={s.id}
                  className="flex items-center justify-between bg-surface-secondary rounded-xl px-4 py-3 border border-surface-border"
                >
                  <div className="flex items-center gap-3 min-w-0">
                    <span className="text-xs font-mono text-text-muted shrink-0">{s.issueKey}</span>
                    <span className="text-sm text-text-primary truncate">{s.title}</span>
                  </div>
                  <div className="flex items-center gap-2 shrink-0">
                    {s.assigneeName && (
                      <span className="text-xs text-text-muted">{s.assigneeName}</span>
                    )}
                    {s.statusName && (
                      <span className="text-xs text-brand-400 bg-brand-400/10 px-2 py-0.5 rounded-md">
                        {s.statusName}
                      </span>
                    )}
                    <span className={`text-xs px-2 py-0.5 rounded-md font-medium ${
                      s.priority === 'CRITICAL' ? 'text-priority-critical bg-priority-critical/10' :
                      s.priority === 'HIGH' ? 'text-priority-high bg-priority-high/10' :
                      s.priority === 'MEDIUM' ? 'text-priority-medium bg-priority-medium/10' :
                      'text-priority-low bg-priority-low/10'
                    }`}>
                      {s.priority}
                    </span>
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>
      </motion.div>
    </>
  );
}