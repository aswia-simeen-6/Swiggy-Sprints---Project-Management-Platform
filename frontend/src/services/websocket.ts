import { Client, IMessage } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import type { BoardEvent, PresenceUpdate } from '@/types';

type Unsubscribe = () => void;

class WebSocketService {
  private client: Client | null = null;
  private reconnectAttempts = 0;
  private maxReconnectAttempts = 10;

  connect(token: string): Promise<void> {
    return new Promise((resolve, reject) => {
      this.client = new Client({
        webSocketFactory: () => new SockJS('/ws'),
        connectHeaders: { Authorization: `Bearer ${token}` },
        reconnectDelay: 3000,
        heartbeatIncoming: 10000,
        heartbeatOutgoing: 10000,
        onConnect: () => {
          this.reconnectAttempts = 0;
          resolve();
        },
        onStompError: (frame) => {
          console.error('STOMP error:', frame.headers['message']);
          reject(new Error(frame.headers['message']));
        },
        onWebSocketClose: () => {
          this.reconnectAttempts++;
          if (this.reconnectAttempts >= this.maxReconnectAttempts) {
            console.error('Max reconnect attempts reached');
          }
        },
      });

      this.client.activate();
    });
  }

  disconnect(): void {
    this.client?.deactivate();
    this.client = null;
    this.reconnectAttempts = 0;
  }

  // Subscribe to real-time board updates
  subscribeToBoardUpdates(projectId: string, callback: (event: BoardEvent) => void): Unsubscribe {
    if (!this.client?.connected) return () => {};

    const sub = this.client.subscribe(
      `/topic/board/${projectId}/updates`,
      (message: IMessage) => {
        const event: BoardEvent = JSON.parse(message.body);
        callback(event);
      },
    );

    return () => sub.unsubscribe();
  }

  // Subscribe to presence changes on a board
  subscribeToPresence(projectId: string, callback: (update: PresenceUpdate) => void): Unsubscribe {
    if (!this.client?.connected) return () => {};

    const sub = this.client.subscribe(
      `/topic/board/${projectId}/presence`,
      (message: IMessage) => {
        const update: PresenceUpdate = JSON.parse(message.body);
        callback(update);
      },
    );

    return () => sub.unsubscribe();
  }

  // Subscribe to personal notifications
  subscribeToNotifications(callback: (data: Record<string, unknown>) => void): Unsubscribe {
    if (!this.client?.connected) return () => {};

    const sub = this.client.subscribe(
      '/user/queue/notifications',
      (message: IMessage) => {
        callback(JSON.parse(message.body));
      },
    );

    return () => sub.unsubscribe();
  }

  // Tell server we joined a board
  joinBoard(projectId: string): void {
    this.client?.publish({ destination: `/app/board/${projectId}/join`, body: '' });
  }

  // Tell server we left a board
  leaveBoard(projectId: string): void {
    this.client?.publish({ destination: `/app/board/${projectId}/leave`, body: '' });
  }

  // Keep-alive heartbeat (call every 30s)
  sendHeartbeat(projectId: string): void {
    this.client?.publish({ destination: `/app/board/${projectId}/heartbeat`, body: '' });
  }

  get isConnected(): boolean {
    return this.client?.connected ?? false;
  }
}

export const wsService = new WebSocketService();
