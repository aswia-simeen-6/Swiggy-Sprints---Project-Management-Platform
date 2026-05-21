import { useState, useEffect, useCallback } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import {
  ChevronDown, ChevronUp, Plus, Play, CheckCircle2, Calendar,
  Target, Zap, X, Loader2, LayoutList,
} from 'lucide-react';
import { sprintApi, searchApi } from '@/services/api';
import { toast } from 'sonner';
import type { Sprint, Issue, CreateSprintRequest, SprintCompletionResult } from '@/types';

interface SprintPanelProps {
  projectId: string;
  onSprintChanged: () => void;
  onIssueClick?: (issue: Issue) => void;
}

export default function SprintPanel({ projectId, onSprintChanged, onIssueClick }: SprintPanelProps) {
  const [sprints, setSprints] = useState<Sprint[]>([]);
  const [loading, setLoading] = useState(true);
  const [expanded, setExpanded] = useState(true);
  const [showCreate, setShowCreate] = useState(false);
  const [completionResult, setCompletionResult] = useState<SprintCompletionResult | null>(null);

  const fetchSprints = useCallback(async () => {
    try {
      const data = await sprintApi.list(projectId);
      setSprints(data);
    } catch {
      toast.error('Failed to load sprints');
    } finally {
      setLoading(false);
    }
  }, [projectId]);

  useEffect(() => {
    fetchSprints();
  }, [fetchSprints]);

  const activeSprint = sprints.find((s) => s.status === 'ACTIVE');
  const plannedSprints = sprints.filter((s) => s.status === 'PLANNED');
  const completedSprints = sprints.filter((s) => s.status === 'COMPLETED');

  const handleStart = async (id: string) => {
    try {
      await sprintApi.start(id);
      toast.success('Sprint started');
      fetchSprints();
      onSprintChanged();
    } catch {
      toast.error('Failed to start sprint');
    }
  };

  const handleComplete = async (id: string) => {
    try {
      const result = await sprintApi.complete(id, { carryOverIssueIds: [] });
      setCompletionResult(result);
      toast.success('Sprint completed');
      fetchSprints();
      onSprintChanged();
    } catch {
      toast.error('Failed to complete sprint');
    }
  };

  return (
    <div className="bg-surface-secondary border border-surface-border rounded-2xl overflow-hidden">
      {/* Header */}
      <button
        onClick={() => setExpanded(!expanded)}
        className="w-full flex items-center justify-between px-4 py-3 hover:bg-surface-tertiary/50 transition-colors"
      >
        <div className="flex items-center gap-2">
          <Zap className="w-4 h-4 text-brand-400" />
          <span className="text-sm font-semibold text-text-primary">Sprints</span>
          <span className="text-xs text-text-muted">({sprints.length})</span>
        </div>
        {expanded ? (
          <ChevronUp className="w-4 h-4 text-text-muted" />
        ) : (
          <ChevronDown className="w-4 h-4 text-text-muted" />
        )}
      </button>

      <AnimatePresence>
        {expanded && (
          <motion.div
            initial={{ height: 0, opacity: 0 }}
            animate={{ height: 'auto', opacity: 1 }}
            exit={{ height: 0, opacity: 0 }}
            transition={{ duration: 0.2 }}
            className="overflow-hidden"
          >
            <div className="px-4 pb-4 space-y-3">
              {loading ? (
                <div className="flex items-center justify-center py-6">
                  <Loader2 className="w-5 h-5 animate-spin text-text-muted" />
                </div>
              ) : (
                <>
                  {/* Active Sprint */}
                  {activeSprint && (
                    <SprintCard
                      sprint={activeSprint}
                      projectId={projectId}
                      onComplete={() => handleComplete(activeSprint.id)}
                      onIssueClick={onIssueClick}
                    />
                  )}

                  {/* Planned Sprints */}
                  {plannedSprints.length > 0 && (
                    <div className="space-y-2">
                      <p className="text-[10px] font-semibold uppercase text-text-muted tracking-wider">
                        Planned
                      </p>
                      {plannedSprints.map((s) => (
                        <SprintCard
                          key={s.id}
                          sprint={s}
                          projectId={projectId}
                          onStart={!activeSprint ? () => handleStart(s.id) : undefined}
                          onIssueClick={onIssueClick}
                        />
                      ))}
                    </div>
                  )}

                  {/* Completed (collapsed) */}
                  {completedSprints.length > 0 && (
                    <CompletedSection sprints={completedSprints} projectId={projectId} onIssueClick={onIssueClick} />
                  )}

                  {sprints.length === 0 && (
                    <p className="text-xs text-text-muted text-center py-4">
                      No sprints yet
                    </p>
                  )}

                  {/* Create Sprint button */}
                  <button
                    onClick={() => setShowCreate(true)}
                    className="w-full flex items-center justify-center gap-1.5 text-xs text-brand-400 hover:text-brand-300 py-2 border border-dashed border-surface-border rounded-xl hover:border-brand-400/40 transition-colors"
                  >
                    <Plus className="w-3.5 h-3.5" />
                    New Sprint
                  </button>
                </>
              )}
            </div>
          </motion.div>
        )}
      </AnimatePresence>

      {/* Create Sprint Modal */}
      <AnimatePresence>
        {showCreate && (
          <CreateSprintModal
            projectId={projectId}
            onClose={() => setShowCreate(false)}
            onCreated={() => {
              setShowCreate(false);
              fetchSprints();
            }}
          />
        )}
      </AnimatePresence>

      {/* Completion Result Modal */}
      <AnimatePresence>
        {completionResult && (
          <CompletionResultModal
            result={completionResult}
            onClose={() => setCompletionResult(null)}
          />
        )}
      </AnimatePresence>
    </div>
  );
}

// ─── Sprint Card ─────────────────────────────────────────────────────────────

function SprintCard({
  sprint,
  projectId,
  onStart,
  onComplete,
  onIssueClick,
}: {
  sprint: Sprint;
  projectId: string;
  onStart?: () => void;
  onComplete?: () => void;
  onIssueClick?: (issue: Issue) => void;
}) {
  const [showIssues, setShowIssues] = useState(false);
  const [issues, setIssues] = useState<Issue[]>([]);
  const [issuesLoading, setIssuesLoading] = useState(false);

  const fetchIssues = useCallback(async () => {
    setIssuesLoading(true);
    try {
      const result = await searchApi.search(projectId, { sprintId: sprint.id, limit: 50 });
      setIssues(result.items);
    } catch {
      toast.error('Failed to load sprint issues');
    } finally {
      setIssuesLoading(false);
    }
  }, [projectId, sprint.id]);

  const handleToggleIssues = () => {
    const next = !showIssues;
    setShowIssues(next);
    if (next && issues.length === 0) {
      fetchIssues();
    }
  };

  const statusColors = {
    ACTIVE: 'border-status-success/30 bg-status-success/5',
    PLANNED: 'border-surface-border bg-surface-tertiary/30',
    COMPLETED: 'border-surface-border bg-surface-tertiary/20',
  };

  const priorityColors: Record<string, string> = {
    CRITICAL: 'text-priority-critical',
    HIGH: 'text-priority-high',
    MEDIUM: 'text-priority-medium',
    LOW: 'text-priority-low',
  };

  return (
    <div className={`border rounded-xl p-3 space-y-2 ${statusColors[sprint.status]}`}>
      <div className="flex items-start justify-between gap-2">
        <div className="flex-1 min-w-0">
          <div className="flex items-center gap-2">
            <span className="text-sm font-medium text-text-primary truncate">{sprint.name}</span>
            <span
              className={`text-[10px] font-semibold uppercase px-2 py-0.5 rounded-full ${
                sprint.status === 'ACTIVE'
                  ? 'bg-status-success/20 text-status-success'
                  : sprint.status === 'PLANNED'
                  ? 'bg-status-info/20 text-status-info'
                  : 'bg-text-muted/20 text-text-muted'
              }`}
            >
              {sprint.status}
            </span>
          </div>
          {sprint.goal && (
            <p className="text-xs text-text-muted mt-0.5 line-clamp-2">{sprint.goal}</p>
          )}
        </div>
      </div>

      {/* Dates */}
      {(sprint.startDate || sprint.endDate) && (
        <div className="flex items-center gap-1.5 text-[11px] text-text-muted">
          <Calendar className="w-3 h-3" />
          {sprint.startDate && <span>{sprint.startDate}</span>}
          {sprint.startDate && sprint.endDate && <span>→</span>}
          {sprint.endDate && <span>{sprint.endDate}</span>}
        </div>
      )}

      {/* Points (for active/completed) */}
      {sprint.status !== 'PLANNED' && (
        <div className="flex items-center gap-3 text-[11px]">
          {sprint.completedPoints != null && (
            <div className="flex items-center gap-1">
              <CheckCircle2 className="w-3 h-3 text-status-success" />
              <span className="text-text-secondary">{sprint.completedPoints} pts done</span>
            </div>
          )}
          {sprint.totalPoints != null && (
            <div className="flex items-center gap-1">
              <Target className="w-3 h-3 text-text-muted" />
              <span className="text-text-secondary">{sprint.totalPoints} total</span>
            </div>
          )}
          {sprint.velocity != null && (
            <div className="flex items-center gap-1">
              <Zap className="w-3 h-3 text-brand-400" />
              <span className="text-text-secondary">v{sprint.velocity}</span>
            </div>
          )}
        </div>
      )}

      {/* Actions */}
      <div className="flex items-center gap-2 flex-wrap">
        {onStart && (
          <button
            onClick={onStart}
            className="text-[11px] font-medium text-status-info hover:text-status-info/80 flex items-center gap-1 transition-colors"
          >
            <Play className="w-3 h-3" />
            Start Sprint
          </button>
        )}
        {onComplete && (
          <button
            onClick={onComplete}
            className="text-[11px] font-medium text-status-success hover:text-status-success/80 flex items-center gap-1 transition-colors"
          >
            <CheckCircle2 className="w-3 h-3" />
            Complete Sprint
          </button>
        )}
        <button
          onClick={handleToggleIssues}
          className="text-[11px] font-medium text-text-muted hover:text-text-primary flex items-center gap-1 transition-colors ml-auto"
        >
          <LayoutList className="w-3 h-3" />
          Issues
          {showIssues ? <ChevronUp className="w-3 h-3" /> : <ChevronDown className="w-3 h-3" />}
        </button>
      </div>

      {/* Expandable issue list */}
      <AnimatePresence>
        {showIssues && (
          <motion.div
            initial={{ height: 0, opacity: 0 }}
            animate={{ height: 'auto', opacity: 1 }}
            exit={{ height: 0, opacity: 0 }}
            transition={{ duration: 0.15 }}
            className="overflow-hidden"
          >
            <div className="pt-2 border-t border-surface-border/50 space-y-1">
              {issuesLoading ? (
                <div className="flex items-center justify-center py-3">
                  <Loader2 className="w-4 h-4 animate-spin text-text-muted" />
                </div>
              ) : issues.length === 0 ? (
                <p className="text-[11px] text-text-muted text-center py-3">No issues in this sprint</p>
              ) : (
                issues.map((issue) => (
                  <div
                    key={issue.id}
                    onClick={() => onIssueClick?.(issue)}
                    className="flex items-center gap-2 px-2 py-1.5 rounded-lg hover:bg-surface-tertiary/50 transition-colors cursor-pointer"
                  >
                    <span className={`w-1.5 h-1.5 rounded-full shrink-0 ${priorityColors[issue.priority]}`}
                      style={{ backgroundColor: 'currentColor' }}
                    />
                    <span className="text-[10px] font-mono text-text-muted shrink-0">{issue.issueKey}</span>
                    <span className="text-[11px] text-text-primary truncate flex-1">{issue.title}</span>
                    {issue.storyPoints != null && (
                      <span className="text-[10px] text-text-muted bg-surface-tertiary px-1.5 py-0.5 rounded-md shrink-0">
                        {issue.storyPoints}
                      </span>
                    )}
                  </div>
                ))
              )}
            </div>
          </motion.div>
        )}
      </AnimatePresence>
    </div>
  );
}

// ─── Completed Section (collapsible) ─────────────────────────────────────────

function CompletedSection({ sprints, projectId, onIssueClick }: { sprints: Sprint[]; projectId: string; onIssueClick?: (issue: Issue) => void }) {
  const [open, setOpen] = useState(false);

  return (
    <div>
      <button
        onClick={() => setOpen(!open)}
        className="flex items-center gap-1.5 text-[10px] font-semibold uppercase text-text-muted tracking-wider hover:text-text-secondary transition-colors"
      >
        {open ? <ChevronUp className="w-3 h-3" /> : <ChevronDown className="w-3 h-3" />}
        Completed ({sprints.length})
      </button>
      <AnimatePresence>
        {open && (
          <motion.div
            initial={{ height: 0, opacity: 0 }}
            animate={{ height: 'auto', opacity: 1 }}
            exit={{ height: 0, opacity: 0 }}
            className="overflow-hidden mt-2 space-y-2"
          >
            {sprints.map((s) => (
              <SprintCard key={s.id} sprint={s} projectId={projectId} onIssueClick={onIssueClick} />
            ))}
          </motion.div>
        )}
      </AnimatePresence>
    </div>
  );
}

// ─── Create Sprint Modal ─────────────────────────────────────────────────────

function CreateSprintModal({
  projectId,
  onClose,
  onCreated,
}: {
  projectId: string;
  onClose: () => void;
  onCreated: () => void;
}) {
  const [name, setName] = useState('');
  const [goal, setGoal] = useState('');
  const [startDate, setStartDate] = useState('');
  const [endDate, setEndDate] = useState('');
  const [loading, setLoading] = useState(false);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!name.trim()) return;

    setLoading(true);
    try {
      const data: CreateSprintRequest = {
        name: name.trim(),
        goal: goal.trim() || undefined,
        startDate: startDate || undefined,
        endDate: endDate || undefined,
      };
      await sprintApi.create(projectId, data);
      toast.success('Sprint created');
      onCreated();
    } catch {
      toast.error('Failed to create sprint');
    } finally {
      setLoading(false);
    }
  };

  return (
    <motion.div
      initial={{ opacity: 0 }}
      animate={{ opacity: 1 }}
      exit={{ opacity: 0 }}
      className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 backdrop-blur-sm"
      onClick={onClose}
    >
      <motion.div
        initial={{ scale: 0.95, opacity: 0 }}
        animate={{ scale: 1, opacity: 1 }}
        exit={{ scale: 0.95, opacity: 0 }}
        onClick={(e) => e.stopPropagation()}
        className="bg-surface-primary border border-surface-border rounded-2xl p-6 w-full max-w-md shadow-2xl"
      >
        <div className="flex items-center justify-between mb-4">
          <h3 className="text-lg font-semibold text-text-primary">New Sprint</h3>
          <button onClick={onClose} className="text-text-muted hover:text-text-primary">
            <X className="w-5 h-5" />
          </button>
        </div>

        <form onSubmit={handleSubmit} className="space-y-4">
          <div>
            <label className="text-xs font-medium text-text-secondary block mb-1">Name *</label>
            <input
              type="text"
              value={name}
              onChange={(e) => setName(e.target.value)}
              placeholder="Sprint 1"
              className="input-field w-full"
              autoFocus
            />
          </div>

          <div>
            <label className="text-xs font-medium text-text-secondary block mb-1">Goal</label>
            <textarea
              value={goal}
              onChange={(e) => setGoal(e.target.value)}
              placeholder="What do you want to achieve?"
              rows={2}
              className="input-field w-full resize-none"
            />
          </div>

          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className="text-xs font-medium text-text-secondary block mb-1">Start Date</label>
              <input
                type="date"
                value={startDate}
                onChange={(e) => setStartDate(e.target.value)}
                className="input-field w-full"
              />
            </div>
            <div>
              <label className="text-xs font-medium text-text-secondary block mb-1">End Date</label>
              <input
                type="date"
                value={endDate}
                onChange={(e) => setEndDate(e.target.value)}
                className="input-field w-full"
              />
            </div>
          </div>

          <div className="flex gap-3 pt-2">
            <button type="button" onClick={onClose} className="btn-ghost flex-1">
              Cancel
            </button>
            <button type="submit" disabled={loading || !name.trim()} className="btn-brand flex-1">
              {loading ? (
                <Loader2 className="w-4 h-4 animate-spin" />
              ) : (
                'Create Sprint'
              )}
            </button>
          </div>
        </form>
      </motion.div>
    </motion.div>
  );
}

// ─── Completion Result Modal ─────────────────────────────────────────────────

function CompletionResultModal({
  result,
  onClose,
}: {
  result: SprintCompletionResult;
  onClose: () => void;
}) {
  return (
    <motion.div
      initial={{ opacity: 0 }}
      animate={{ opacity: 1 }}
      exit={{ opacity: 0 }}
      className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 backdrop-blur-sm"
      onClick={onClose}
    >
      <motion.div
        initial={{ scale: 0.95, opacity: 0 }}
        animate={{ scale: 1, opacity: 1 }}
        exit={{ scale: 0.95, opacity: 0 }}
        onClick={(e) => e.stopPropagation()}
        className="bg-surface-primary border border-surface-border rounded-2xl p-6 w-full max-w-sm shadow-2xl"
      >
        <div className="flex items-center justify-between mb-4">
          <h3 className="text-lg font-semibold text-text-primary">Sprint Completed</h3>
          <button onClick={onClose} className="text-text-muted hover:text-text-primary">
            <X className="w-5 h-5" />
          </button>
        </div>

        <div className="space-y-3">
          <div className="text-center p-4 bg-status-success/10 rounded-xl">
            <Zap className="w-8 h-8 text-status-success mx-auto mb-2" />
            <p className="text-2xl font-bold text-text-primary">{result.velocity}</p>
            <p className="text-xs text-text-muted">Velocity</p>
          </div>

          <div className="grid grid-cols-3 gap-2 text-center">
            <div className="bg-surface-tertiary rounded-xl p-3">
              <p className="text-lg font-semibold text-status-success">{result.completedIssueCount}</p>
              <p className="text-[10px] text-text-muted">Completed</p>
            </div>
            <div className="bg-surface-tertiary rounded-xl p-3">
              <p className="text-lg font-semibold text-status-warning">{result.incompleteIssueCount}</p>
              <p className="text-[10px] text-text-muted">Incomplete</p>
            </div>
            <div className="bg-surface-tertiary rounded-xl p-3">
              <p className="text-lg font-semibold text-status-info">{result.carriedOverCount}</p>
              <p className="text-[10px] text-text-muted">Carried Over</p>
            </div>
          </div>

          <button onClick={onClose} className="btn-brand w-full mt-2">
            Done
          </button>
        </div>
      </motion.div>
    </motion.div>
  );
}