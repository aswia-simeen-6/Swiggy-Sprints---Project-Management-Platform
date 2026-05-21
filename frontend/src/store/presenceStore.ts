import { create } from 'zustand';
import type { PresenceUser } from '@/types';

interface PresenceState {
  users: PresenceUser[];
  count: number;
  setPresence: (users: PresenceUser[], count: number) => void;
  clear: () => void;
}

export const usePresenceStore = create<PresenceState>()((set) => ({
  users: [],
  count: 0,
  setPresence: (users, count) => set({ users, count }),
  clear: () => set({ users: [], count: 0 }),
}));
