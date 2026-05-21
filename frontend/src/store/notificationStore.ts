import { create } from 'zustand';
import { notificationApi } from '@/services/api';
import type { Notification } from '@/types';

interface NotificationState {
  notifications: Notification[];
  unreadCount: number;
  isOpen: boolean;
  fetch: () => Promise<void>;
  fetchUnreadCount: () => Promise<void>;
  markRead: (id: string) => Promise<void>;
  markAllRead: () => Promise<void>;
  addRealtime: (n: Partial<Notification>) => void;
  toggle: () => void;
  close: () => void;
}

export const useNotificationStore = create<NotificationState>()((set) => ({
  notifications: [],
  unreadCount: 0,
  isOpen: false,

  fetch: async () => {
    const notifications = await notificationApi.list();
    set({ notifications });
  },

  fetchUnreadCount: async () => {
    const unreadCount = await notificationApi.unreadCount();
    set({ unreadCount });
  },

  markRead: async (id) => {
    await notificationApi.markRead(id);
    set((s) => ({
      notifications: s.notifications.map((n) =>
        n.id === id ? { ...n, isRead: true } : n,
      ),
      unreadCount: Math.max(0, s.unreadCount - 1),
    }));
  },

  markAllRead: async () => {
    await notificationApi.markAllRead();
    set((s) => ({
      notifications: s.notifications.map((n) => ({ ...n, isRead: true })),
      unreadCount: 0,
    }));
  },

  addRealtime: (data) => {
    const n: Notification = {
      id: crypto.randomUUID(),
      type: (data.type as string) ?? 'INFO',
      title: (data.title as string) ?? 'Notification',
      message: (data.message as string) ?? '',
      resourceType: null,
      resourceId: null,
      isRead: false,
      createdAt: new Date().toISOString(),
      ...data,
    };
    set((s) => ({
      notifications: [n, ...s.notifications],
      unreadCount: s.unreadCount + 1,
    }));
  },

  toggle: () => set((s) => ({ isOpen: !s.isOpen })),
  close: () => set({ isOpen: false }),
}));
