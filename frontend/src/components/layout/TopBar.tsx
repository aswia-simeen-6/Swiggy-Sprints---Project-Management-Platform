import { useState, useRef, useEffect, useCallback } from 'react';
import { useParams } from 'react-router-dom';
import { motion, AnimatePresence } from 'framer-motion';
import { Bell, Search, X } from 'lucide-react';
import { useNotificationStore } from '@/store/notificationStore';
import { usePresenceStore } from '@/store/presenceStore';
import { searchApi } from '@/services/api';
import PresenceAvatars from '@/components/common/PresenceAvatars';
import type { Issue } from '@/types';

export default function TopBar() {
  const { projectId } = useParams<{ projectId: string }>();
  const unreadCount = useNotificationStore((s) => s.unreadCount);
  const toggle = useNotificationStore((s) => s.toggle);
  const presenceUsers = usePresenceStore((s) => s.users);

  const [query, setQuery] = useState('');
  const [results, setResults] = useState<Issue[]>([]);
  const [isSearchOpen, setIsSearchOpen] = useState(false);
  const [isSearching, setIsSearching] = useState(false);
  const searchRef = useRef<HTMLDivElement>(null);
  const debounceRef = useRef<ReturnType<typeof setTimeout>>();

  // Close dropdown on click outside
  useEffect(() => {
    const handler = (e: MouseEvent) => {
      if (searchRef.current && !searchRef.current.contains(e.target as Node)) {
        setIsSearchOpen(false);
      }
    };
    document.addEventListener('mousedown', handler);
    return () => document.removeEventListener('mousedown', handler);
  }, []);

  const handleSearch = useCallback(
    (value: string) => {
      setQuery(value);
      if (!projectId || value.trim().length < 2) {
        setResults([]);
        setIsSearchOpen(false);
        return;
      }
      clearTimeout(debounceRef.current);
      debounceRef.current = setTimeout(async () => {
        setIsSearching(true);
        try {
          const page = await searchApi.search(projectId, { q: value.trim(), limit: 8 });
          setResults(page.items);
          setIsSearchOpen(true);
        } catch {
          setResults([]);
        } finally {
          setIsSearching(false);
        }
      }, 300);
    },
    [projectId],
  );

  const clearSearch = () => {
    setQuery('');
    setResults([]);
    setIsSearchOpen(false);
  };

  const priorityColor: Record<string, string> = {
    CRITICAL: 'text-priority-critical',
    HIGH: 'text-priority-high',
    MEDIUM: 'text-priority-medium',
    LOW: 'text-priority-low',
  };

  return (
    <header className="h-14 bg-surface-secondary/80 backdrop-blur-xl border-b border-surface-border flex items-center justify-between px-6 shrink-0">
      {/* Left: breadcrumb area - will be populated by page */}
      <div className="flex items-center gap-3">
        <div id="topbar-left" />
      </div>

      {/* Right: search + presence + notifications */}
      <div className="flex items-center gap-4">
        {/* Search bar (only on board pages) */}
        {projectId && (
          <div ref={searchRef} className="relative">
            <div className="flex items-center gap-2 bg-surface-primary border border-surface-border rounded-xl px-3 py-1.5 w-64 focus-within:ring-2 focus-within:ring-brand-400/30 focus-within:border-brand-400/50 transition-all">
              <Search className="w-4 h-4 text-text-muted shrink-0" />
              <input
                type="text"
                value={query}
                onChange={(e) => handleSearch(e.target.value)}
                placeholder="Search issues…"
                className="bg-transparent text-sm text-text-primary placeholder:text-text-muted outline-none flex-1"
              />
              {query && (
                <button onClick={clearSearch} className="text-text-muted hover:text-text-primary">
                  <X className="w-3.5 h-3.5" />
                </button>
              )}
            </div>

            {/* Search results dropdown */}
            <AnimatePresence>
              {isSearchOpen && (
                <motion.div
                  initial={{ opacity: 0, y: -4 }}
                  animate={{ opacity: 1, y: 0 }}
                  exit={{ opacity: 0, y: -4 }}
                  className="absolute right-0 top-full mt-2 w-96 bg-surface-secondary border border-surface-border rounded-2xl shadow-float overflow-hidden z-50"
                >
                  {isSearching ? (
                    <div className="p-4 text-center text-text-muted text-sm">Searching…</div>
                  ) : results.length === 0 ? (
                    <div className="p-4 text-center text-text-muted text-sm">No results found</div>
                  ) : (
                    <ul className="max-h-80 overflow-y-auto divide-y divide-surface-border">
                      {results.map((issue) => (
                        <li
                          key={issue.id}
                          className="px-4 py-3 hover:bg-surface-hover cursor-pointer transition-colors"
                          onClick={() => {
                            clearSearch();
                            // Could navigate to issue detail in future
                          }}
                        >
                          <div className="flex items-center gap-2">
                            <span className="text-xs font-mono text-text-muted">{issue.issueKey}</span>
                            <span className={`text-2xs font-semibold uppercase ${priorityColor[issue.priority] ?? 'text-text-muted'}`}>
                              {issue.priority}
                            </span>
                            <span className="text-2xs text-text-muted uppercase">{issue.issueType}</span>
                          </div>
                          <p className="text-sm text-text-primary mt-0.5 truncate">{issue.title}</p>
                        </li>
                      ))}
                    </ul>
                  )}
                </motion.div>
              )}
            </AnimatePresence>
          </div>
        )}

        {/* Live presence avatars */}
        {presenceUsers.length > 0 && <PresenceAvatars users={presenceUsers} />}

        {/* Notification bell */}
        <motion.button
          whileHover={{ scale: 1.05 }}
          whileTap={{ scale: 0.95 }}
          onClick={toggle}
          className="relative w-9 h-9 rounded-xl flex items-center justify-center text-text-muted hover:text-text-primary hover:bg-surface-hover transition-all duration-200"
        >
          <Bell className="w-5 h-5" />
          <AnimatePresence>
            {unreadCount > 0 && (
              <motion.span
                initial={{ scale: 0 }}
                animate={{ scale: 1 }}
                exit={{ scale: 0 }}
                className="absolute -top-0.5 -right-0.5 w-5 h-5 bg-brand-400 text-white text-2xs font-bold rounded-full flex items-center justify-center shadow-glow-sm"
              >
                {unreadCount > 9 ? '9+' : unreadCount}
              </motion.span>
            )}
          </AnimatePresence>
        </motion.button>
      </div>
    </header>
  );
}