import axios, { InternalAxiosRequestConfig } from 'axios';
import { useAuthStore } from '@/store/authStore';
import type {
  ApiResponse, AuthResponse, LoginRequest, RegisterRequest,
  Project, CreateProjectRequest, Board, Issue,
  CreateIssueRequest, UpdateIssueRequest, TransitionRequest,
  Sprint, WorkflowStatus, Notification, ProjectMember, CursorPage,
  Comment, CreateCommentRequest, Activity, User,
  CreateSprintRequest, SprintCompleteRequest, SprintCompletionResult,
} from '@/types';

const api = axios.create({
  baseURL: '/api',
  headers: { 'Content-Type': 'application/json' },
});

// Inject JWT token on every request
api.interceptors.request.use((config: InternalAxiosRequestConfig) => {
  const token = useAuthStore.getState().token;
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

// Auto-logout on 401
api.interceptors.response.use(
  (res) => res,
  (err) => {
    if (err.response?.status === 401 && useAuthStore.getState().token) {
      useAuthStore.getState().logout();
      window.location.href = '/login';
    }
    return Promise.reject(err);
  },
);

// ─── Auth ────────────────────────────────────────────────────────────────────

export const authApi = {
  login: (data: LoginRequest) =>
    api.post<ApiResponse<AuthResponse>>('/auth/login', data).then((r) => r.data.data),
  register: (data: RegisterRequest) =>
    api.post<ApiResponse<AuthResponse>>('/auth/register', data).then((r) => r.data.data),
  me: () =>
    api.get<ApiResponse<AuthResponse['user']>>('/auth/me').then((r) => r.data.data),
  searchUser: (email: string) =>
    api.get<ApiResponse<User>>('/auth/search', { params: { email } }).then((r) => r.data.data),
};

// ─── Projects ────────────────────────────────────────────────────────────────

export const projectApi = {
  list: () =>
    api.get<ApiResponse<Project[]>>('/projects').then((r) => r.data.data),
  get: (id: string) =>
    api.get<ApiResponse<Project>>(`/projects/${id}`).then((r) => r.data.data),
  create: (data: CreateProjectRequest) =>
    api.post<ApiResponse<Project>>('/projects', data).then((r) => r.data.data),
  members: (projectId: string) =>
    api.get<ApiResponse<ProjectMember[]>>(`/projects/${projectId}/members`).then((r) => r.data.data),
  addMember: (projectId: string, userId: string, role?: string) =>
    api.post<ApiResponse<void>>(`/projects/${projectId}/members`, null, { params: { userId, role } }).then((r) => r.data),
};

// ─── Board / Issues ──────────────────────────────────────────────────────────

export const boardApi = {
  get: (projectId: string) =>
    api.get<Board>(`/projects/${projectId}/board`).then((r) => r.data),
};

export const issueApi = {
  create: (projectId: string, data: CreateIssueRequest) =>
    api.post<ApiResponse<Issue>>(`/projects/${projectId}/issues`, data).then((r) => r.data.data),
  get: (id: string) =>
    api.get<ApiResponse<Issue>>(`/issues/${id}`).then((r) => r.data.data),
  update: (id: string, data: UpdateIssueRequest) =>
    api.patch<ApiResponse<Issue>>(`/issues/${id}`, data).then((r) => r.data.data),
  delete: (id: string) =>
    api.delete(`/issues/${id}`),
  transition: (id: string, data: TransitionRequest) =>
    api.post<ApiResponse<Issue>>(`/issues/${id}/transitions`, data).then((r) => r.data.data),
  getAllowedTransitions: (id: string) =>
    api.get<ApiResponse<WorkflowStatus[]>>(`/issues/${id}/transitions`).then((r) => r.data.data),
  watch: (id: string) =>
    api.post<ApiResponse<void>>(`/issues/${id}/watch`),
  unwatch: (id: string) =>
    api.delete(`/issues/${id}/watch`),
  children: (id: string) =>
    api.get<ApiResponse<Issue[]>>(`/issues/${id}/children`).then((r) => r.data.data),
};

// ─── Sprints ─────────────────────────────────────────────────────────────────

export const sprintApi = {
  list: (projectId: string) =>
    api.get<ApiResponse<Sprint[]>>(`/projects/${projectId}/sprints`).then((r) => r.data.data),
  get: (id: string) =>
    api.get<ApiResponse<Sprint>>(`/sprints/${id}`).then((r) => r.data.data),
  create: (projectId: string, data: CreateSprintRequest) =>
    api.post<ApiResponse<Sprint>>(`/projects/${projectId}/sprints`, data).then((r) => r.data.data),
  start: (id: string) =>
    api.post<ApiResponse<Sprint>>(`/sprints/${id}/start`).then((r) => r.data.data),
  complete: (id: string, data: SprintCompleteRequest) =>
    api.post<ApiResponse<SprintCompletionResult>>(`/sprints/${id}/complete`, data).then((r) => r.data.data),
  velocity: (projectId: string) =>
    api.get<ApiResponse<Sprint[]>>(`/projects/${projectId}/velocity`).then((r) => r.data.data),
};

// ─── Notifications ───────────────────────────────────────────────────────────

export const notificationApi = {
  list: (unreadOnly = false) =>
    api.get<ApiResponse<Notification[]>>('/notifications', { params: { unreadOnly } })
      .then((r) => r.data.data),
  unreadCount: () =>
    api.get<ApiResponse<{ count: number }>>('/notifications/unread-count')
      .then((r) => r.data.data.count),
  markRead: (id: string) =>
    api.patch(`/notifications/${id}/read`),
  markAllRead: () =>
    api.post('/notifications/read-all'),
};

export default api;

// ─── Comments ─────────────────────────────────────────────────────────────────

export const commentApi = {
  list: (issueId: string, page = 0, size = 20) =>
    api.get<ApiResponse<Comment[]>>(`/issues/${issueId}/comments`, { params: { page, size } })
      .then((r) => r.data.data),
  add: (issueId: string, data: CreateCommentRequest) =>
    api.post<ApiResponse<Comment>>(`/issues/${issueId}/comments`, data).then((r) => r.data.data),
  delete: (issueId: string, commentId: string) =>
    api.delete(`/issues/${issueId}/comments/${commentId}`),
};

// ─── Activity ─────────────────────────────────────────────────────────────────

export const activityApi = {
  issueActivity: (issueId: string, page = 0, size = 20) =>
    api.get<ApiResponse<Activity[]>>(`/issues/${issueId}/activity`, { params: { page, size } })
      .then((r) => r.data.data),
};

// ─── Search ───────────────────────────────────────────────────────────────────

export const searchApi = {
  search: (projectId: string, params: { q?: string; limit?: number; cursor?: string; sprintId?: string }) =>
    api.get<ApiResponse<CursorPage<Issue>>>('/search', { params: { projectId, ...params } })
      .then((r) => r.data.data),
};