import { create } from 'zustand';
import { persist } from 'zustand/middleware';
import type { User } from '@/types';
import { authApi } from '@/services/api';
import { wsService } from '@/services/websocket';

interface AuthState {
  token: string | null;
  user: User | null;
  isLoading: boolean;
  login: (email: string, password: string) => Promise<void>;
  register: (email: string, password: string, displayName: string) => Promise<void>;
  logout: () => void;
  hydrate: () => Promise<void>;
}

export const useAuthStore = create<AuthState>()(
  persist(
    (set, get) => ({
      token: null,
      user: null,
      isLoading: false,

      login: async (email, password) => {
        set({ isLoading: true });
        try {
          const res = await authApi.login({ email, password });
          set({ token: res.token, user: res.user, isLoading: false });
          wsService.connect(res.token).catch((err) =>
            console.warn('WebSocket connection failed:', err),
          );
        } catch (err) {
          set({ isLoading: false });
          throw err;
        }
      },

      register: async (email, password, displayName) => {
        set({ isLoading: true });
        try {
          const res = await authApi.register({ email, password, displayName });
          set({ token: res.token, user: res.user, isLoading: false });
          wsService.connect(res.token).catch((err) =>
            console.warn('WebSocket connection failed:', err),
          );
        } catch (err) {
          set({ isLoading: false });
          throw err;
        }
      },

      logout: () => {
        wsService.disconnect();
        set({ token: null, user: null });
      },

      hydrate: async () => {
        const { token } = get();
        if (!token) return;
        try {
          const user = await authApi.me();
          set({ user });
          if (!wsService.isConnected) {
            wsService.connect(token).catch((err) =>
              console.warn('WebSocket reconnect failed:', err),
            );
          }
        } catch {
          set({ token: null, user: null });
        }
      },
    }),
    {
      name: 'pf-auth',
      partialize: (state) => ({ token: state.token, user: state.user }),
    },
  ),
);