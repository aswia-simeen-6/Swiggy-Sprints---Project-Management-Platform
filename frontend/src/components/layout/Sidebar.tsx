import { NavLink, useNavigate } from 'react-router-dom';
import { motion } from 'framer-motion';
import { FolderKanban, Zap, LogOut } from 'lucide-react';
import { useAuthStore } from '@/store/authStore';

const navItems = [
  { to: '/projects', icon: FolderKanban, label: 'Projects' },
];

export default function Sidebar() {
  const user = useAuthStore((s) => s.user);
  const logout = useAuthStore((s) => s.logout);
  const navigate = useNavigate();

  const handleLogout = () => {
    logout();
    navigate('/login');
  };

  return (
    <aside className="w-[72px] bg-surface-secondary border-r border-surface-border flex flex-col items-center py-4 shrink-0">
      {/* Logo */}
      <motion.div
        whileHover={{ scale: 1.05 }}
        whileTap={{ scale: 0.95 }}
        className="w-11 h-11 bg-gradient-brand rounded-xl flex items-center justify-center shadow-glow-sm mb-8 cursor-pointer"
        onClick={() => navigate('/projects')}
      >
        <Zap className="w-5 h-5 text-white" />
      </motion.div>

      {/* Nav Items */}
      <nav className="flex-1 flex flex-col items-center gap-2">
        {navItems.map((item) => (
          <NavLink key={item.to} to={item.to}>
            {({ isActive }) => (
              <motion.div
                whileHover={{ scale: 1.08 }}
                whileTap={{ scale: 0.92 }}
                className={`
                  w-11 h-11 rounded-xl flex items-center justify-center transition-all duration-200
                  ${isActive
                    ? 'bg-brand-400/15 text-brand-400 shadow-glow-sm'
                    : 'text-text-muted hover:text-text-primary hover:bg-surface-hover'
                  }
                `}
                title={item.label}
              >
                <item.icon className="w-5 h-5" />
              </motion.div>
            )}
          </NavLink>
        ))}
      </nav>

      {/* Bottom: User avatar + logout */}
      <div className="flex flex-col items-center gap-3">
        <motion.button
          whileHover={{ scale: 1.08 }}
          whileTap={{ scale: 0.92 }}
          onClick={handleLogout}
          className="w-11 h-11 rounded-xl flex items-center justify-center text-text-muted hover:text-status-error hover:bg-status-error/10 transition-all duration-200"
          title="Logout"
        >
          <LogOut className="w-5 h-5" />
        </motion.button>

        {/* Avatar */}
        <div className="w-9 h-9 rounded-full bg-gradient-brand flex items-center justify-center text-white text-sm font-bold">
          {user?.displayName?.charAt(0).toUpperCase() ?? '?'}
        </div>
      </div>
    </aside>
  );
}
