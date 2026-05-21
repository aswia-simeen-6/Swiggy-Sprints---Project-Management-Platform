import { useEffect, useState, type FormEvent } from 'react';
import { useNavigate } from 'react-router-dom';
import { motion, AnimatePresence } from 'framer-motion';
import { Plus, FolderKanban, ArrowRight, X, UserPlus } from 'lucide-react';
import { projectApi, authApi } from '@/services/api';
import type { Project, CreateProjectRequest } from '@/types';
import { toast } from 'sonner';

const container = {
  hidden: { opacity: 0 },
  show: { opacity: 1, transition: { staggerChildren: 0.06 } },
};
const item = {
  hidden: { opacity: 0, y: 16 },
  show: { opacity: 1, y: 0, transition: { type: 'spring', stiffness: 300, damping: 24 } },
};

export default function ProjectsPage() {
  const [projects, setProjects] = useState<Project[]>([]);
  const [loading, setLoading] = useState(true);
  const [showCreate, setShowCreate] = useState(false);
  const [inviteProject, setInviteProject] = useState<Project | null>(null);
  const navigate = useNavigate();

  useEffect(() => {
    projectApi.list()
      .then(setProjects)
      .catch(() => toast.error('Failed to load projects'))
      .finally(() => setLoading(false));
  }, []);

  const handleCreate = async (data: CreateProjectRequest) => {
    try {
      const project = await projectApi.create(data);
      setProjects((prev) => [...prev, project]);
      setShowCreate(false);
      toast.success(`Project "${project.name}" created!`);
    } catch {
      toast.error('Failed to create project');
    }
  };

  return (
    <div className="p-8 max-w-6xl mx-auto">
      {/* Header */}
      <div className="flex items-center justify-between mb-8">
        <div>
          <h1 className="text-2xl font-bold text-text-primary">Projects</h1>
          <p className="text-text-secondary text-sm mt-1">Manage your project boards</p>
        </div>
        <motion.button
          whileHover={{ scale: 1.02 }}
          whileTap={{ scale: 0.98 }}
          onClick={() => setShowCreate(true)}
          className="btn-brand"
        >
          <Plus className="w-4 h-4" />
          New Project
        </motion.button>
      </div>

      {/* Loading skeleton */}
      {loading && (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-5">
          {[1, 2, 3].map((i) => (
            <div key={i} className="card p-6 space-y-3">
              <div className="skeleton h-6 w-3/4" />
              <div className="skeleton h-4 w-1/2" />
              <div className="skeleton h-4 w-full" />
            </div>
          ))}
        </div>
      )}

      {/* Projects grid */}
      {!loading && (
        <motion.div
          variants={container}
          initial="hidden"
          animate="show"
          className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-5"
        >
          {projects.map((project) => (
            <motion.div
              key={project.id}
              variants={item}
              onClick={() => navigate(`/board/${project.id}`)}
              className="card-interactive p-6 group"
            >
              <div className="flex items-start justify-between mb-3">
                <div className="w-10 h-10 bg-brand-400/10 rounded-xl flex items-center justify-center">
                  <FolderKanban className="w-5 h-5 text-brand-400" />
                </div>
                <div className="flex items-center gap-1">
                  <motion.button
                    whileHover={{ scale: 1.1 }}
                    whileTap={{ scale: 0.9 }}
                    onClick={(e) => { e.stopPropagation(); setInviteProject(project); }}
                    className="w-8 h-8 rounded-lg flex items-center justify-center text-text-muted hover:text-brand-400 hover:bg-brand-400/10 transition-colors opacity-0 group-hover:opacity-100"
                    title="Add member"
                  >
                    <UserPlus className="w-4 h-4" />
                  </motion.button>
                  <motion.div
                    initial={{ opacity: 0, x: -4 }}
                    whileHover={{ opacity: 1, x: 0 }}
                    className="opacity-0 group-hover:opacity-100 transition-opacity"
                  >
                    <ArrowRight className="w-5 h-5 text-text-muted" />
                  </motion.div>
                </div>
              </div>

              <h3 className="text-lg font-semibold text-text-primary mb-1">{project.name}</h3>
              <span className="inline-block px-2 py-0.5 bg-surface-tertiary text-text-muted text-xs font-mono rounded-md mb-2">
                {project.key}
              </span>
              {project.description && (
                <p className="text-text-secondary text-sm line-clamp-2">{project.description}</p>
              )}
            </motion.div>
          ))}

          {/* Empty state */}
          {projects.length === 0 && (
            <motion.div
              variants={item}
              className="col-span-full text-center py-20"
            >
              <div className="w-16 h-16 bg-surface-tertiary rounded-2xl flex items-center justify-center mx-auto mb-4">
                <FolderKanban className="w-8 h-8 text-text-muted" />
              </div>
              <h3 className="text-lg font-medium text-text-primary mb-2">No projects yet</h3>
              <p className="text-text-secondary text-sm mb-6">Create your first project to get started</p>
              <button onClick={() => setShowCreate(true)} className="btn-brand">
                <Plus className="w-4 h-4" />
                Create Project
              </button>
            </motion.div>
          )}
        </motion.div>
      )}

      {/* Create Project Modal */}
      <AnimatePresence>
        {showCreate && (
          <CreateProjectModal onClose={() => setShowCreate(false)} onCreate={handleCreate} />
        )}
      </AnimatePresence>

      {/* Invite Member Modal */}
      <AnimatePresence>
        {inviteProject && (
          <InviteMemberModal
            project={inviteProject}
            onClose={() => setInviteProject(null)}
          />
        )}
      </AnimatePresence>
    </div>
  );
}

function CreateProjectModal({
  onClose,
  onCreate,
}: {
  onClose: () => void;
  onCreate: (data: CreateProjectRequest) => void;
}) {
  const [name, setName] = useState('');
  const [key, setKey] = useState('');
  const [description, setDescription] = useState('');

  // Auto-generate key from name
  const handleNameChange = (val: string) => {
    setName(val);
    if (!key || key === autoKey(name)) {
      setKey(autoKey(val));
    }
  };

  const autoKey = (n: string) =>
    n.replace(/[^a-zA-Z0-9]/g, '').toUpperCase().slice(0, 5);

  return (
    <>
      {/* Backdrop */}
      <motion.div
        initial={{ opacity: 0 }}
        animate={{ opacity: 1 }}
        exit={{ opacity: 0 }}
        onClick={onClose}
        className="fixed inset-0 bg-black/60 backdrop-blur-sm z-40"
      />

      {/* Modal */}
      <motion.div
        initial={{ opacity: 0, scale: 0.95, y: 20 }}
        animate={{ opacity: 1, scale: 1, y: 0 }}
        exit={{ opacity: 0, scale: 0.95, y: 20 }}
        transition={{ type: 'spring', stiffness: 300, damping: 25 }}
        className="fixed inset-0 flex items-center justify-center z-50 p-4"
      >
        <div className="card p-8 w-full max-w-md shadow-float" onClick={(e) => e.stopPropagation()}>
          <div className="flex items-center justify-between mb-6">
            <h2 className="text-xl font-bold text-text-primary">New Project</h2>
            <button onClick={onClose} className="text-text-muted hover:text-text-primary transition-colors">
              <X className="w-5 h-5" />
            </button>
          </div>

          <form
            onSubmit={(e) => {
              e.preventDefault();
              onCreate({ name, key, description: description || undefined });
            }}
            className="space-y-5"
          >
            <div>
              <label className="block text-sm font-medium text-text-secondary mb-2">Project Name</label>
              <input
                value={name}
                onChange={(e) => handleNameChange(e.target.value)}
                className="input-field"
                placeholder="My Awesome Project"
                required
                autoFocus
              />
            </div>
            <div>
              <label className="block text-sm font-medium text-text-secondary mb-2">Key</label>
              <input
                value={key}
                onChange={(e) => setKey(e.target.value.toUpperCase().replace(/[^A-Z0-9]/g, ''))}
                className="input-field font-mono"
                placeholder="MAP"
                required
                maxLength={10}
              />
              <p className="text-text-muted text-xs mt-1">Used in issue keys like {key || 'KEY'}-1</p>
            </div>
            <div>
              <label className="block text-sm font-medium text-text-secondary mb-2">Description</label>
              <textarea
                value={description}
                onChange={(e) => setDescription(e.target.value)}
                className="input-field resize-none h-20"
                placeholder="Optional description..."
              />
            </div>
            <div className="flex gap-3 pt-2">
              <button type="button" onClick={onClose} className="btn-ghost flex-1">
                Cancel
              </button>
              <motion.button type="submit" whileTap={{ scale: 0.98 }} className="btn-brand flex-1">
                Create Project
              </motion.button>
            </div>
          </form>
        </div>
      </motion.div>
    </>
  );
}

function InviteMemberModal({
  project,
  onClose,
}: {
  project: Project;
  onClose: () => void;
}) {
  const [email, setEmail] = useState('');
  const [role, setRole] = useState('MEMBER');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  const handleInvite = async (e: FormEvent) => {
    e.preventDefault();
    setError('');
    setLoading(true);
    try {
      const user = await authApi.searchUser(email);
      await projectApi.addMember(project.id, user.id, role);
      toast.success(`${user.displayName} added to ${project.name}`);
      onClose();
    } catch (err: unknown) {
      const msg =
        err && typeof err === 'object' && 'response' in err
          ? (err as { response?: { data?: { message?: string } } }).response?.data?.message
          : undefined;
      setError(msg ?? 'User not found or already a member');
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
        <div className="card p-8 w-full max-w-md shadow-float" onClick={(e) => e.stopPropagation()}>
          <div className="flex items-center justify-between mb-6">
            <h2 className="text-xl font-bold text-text-primary">Add Member to {project.name}</h2>
            <button onClick={onClose} className="text-text-muted hover:text-text-primary transition-colors">
              <X className="w-5 h-5" />
            </button>
          </div>

          <form onSubmit={handleInvite} className="space-y-5">
            <div>
              <label className="block text-sm font-medium text-text-secondary mb-2">User Email</label>
              <input
                type="email"
                value={email}
                onChange={(e) => { setEmail(e.target.value); setError(''); }}
                className="input-field"
                placeholder="user@example.com"
                required
                autoFocus
              />
            </div>
            <div>
              <label className="block text-sm font-medium text-text-secondary mb-2">Role</label>
              <select value={role} onChange={(e) => setRole(e.target.value)} className="input-field">
                <option value="MEMBER">Member</option>
                <option value="ADMIN">Admin</option>
                <option value="VIEWER">Viewer</option>
              </select>
            </div>
            {error && (
              <p className="text-sm text-status-error">{error}</p>
            )}
            <div className="flex gap-3 pt-2">
              <button type="button" onClick={onClose} className="btn-ghost flex-1">
                Cancel
              </button>
              <motion.button type="submit" disabled={loading} whileTap={{ scale: 0.98 }} className="btn-brand flex-1">
                {loading ? (
                  <div className="w-5 h-5 border-2 border-white/30 border-t-white rounded-full animate-spin" />
                ) : (
                  <>
                    <UserPlus className="w-4 h-4" />
                    Add Member
                  </>
                )}
              </motion.button>
            </div>
          </form>
        </div>
      </motion.div>
    </>
  );
}