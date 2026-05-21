import { motion } from 'framer-motion';
import type { PresenceUser } from '@/types';

interface Props {
  users: PresenceUser[];
  max?: number;
}

export default function PresenceAvatars({ users, max = 5 }: Props) {
  const visible = users.slice(0, max);
  const overflow = users.length - max;

  return (
    <div className="flex items-center">
      <div className="flex -space-x-2">
        {visible.map((user, i) => (
          <motion.div
            key={user.userId}
            initial={{ opacity: 0, scale: 0.5, x: -10 }}
            animate={{ opacity: 1, scale: 1, x: 0 }}
            transition={{ delay: i * 0.05, type: 'spring', stiffness: 300 }}
            title={`${user.displayName} (online)`}
            className="relative"
          >
            {user.avatarUrl ? (
              <img
                src={user.avatarUrl}
                alt={user.displayName}
                className="w-8 h-8 rounded-full border-2 border-surface-primary presence-ring object-cover"
              />
            ) : (
              <div className="w-8 h-8 rounded-full border-2 border-surface-primary presence-ring bg-surface-tertiary flex items-center justify-center text-text-primary text-xs font-bold">
                {user.displayName.charAt(0).toUpperCase()}
              </div>
            )}
            {/* Green dot */}
            <span className="absolute bottom-0 right-0 w-2.5 h-2.5 bg-status-success rounded-full border-2 border-surface-primary animate-pulse-dot" />
          </motion.div>
        ))}
      </div>

      {overflow > 0 && (
        <motion.span
          initial={{ opacity: 0 }}
          animate={{ opacity: 1 }}
          className="ml-2 text-text-muted text-xs font-medium"
        >
          +{overflow}
        </motion.span>
      )}

      <span className="ml-3 text-text-muted text-xs">
        {users.length} online
      </span>
    </div>
  );
}
