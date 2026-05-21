// ─── Domain Types ────────────────────────────────────────────────────────────

export type UUID = string;

export type IssueType = 'EPIC' | 'STORY' | 'TASK' | 'BUG' | 'SUBTASK';
export type Priority = 'CRITICAL' | 'HIGH' | 'MEDIUM' | 'LOW';
export type StatusCategory = 'TODO' | 'IN_PROGRESS' | 'DONE';
export type SprintStatus = 'PLANNED' | 'ACTIVE' | 'COMPLETED';
export type UserRole = 'ADMIN' | 'MEMBER' | 'VIEWER';

export interface User {
  id: UUID;
  email: string;
  displayName: string;
  avatarUrl: string | null;
  role: UserRole;
  createdAt: string;
}

export interface Project {
  id: UUID;
  name: string;
  key: string;
  description: string | null;
  ownerId: UUID;
  createdAt: string;
}

export interface Issue {
  id: UUID;
  projectId: UUID;
  issueKey: string;
  issueType: IssueType;
  title: string;
  description: string | null;
  statusId: UUID;
  statusName: string | null;
  statusCategory: StatusCategory | null;
  priority: Priority;
  assigneeId: UUID | null;
  assigneeName: string | null;
  reporterId: UUID;
  sprintId: UUID | null;
  parentId: UUID | null;
  storyPoints: number | null;
  labels: string[];
  customFields: Record<string, unknown>;
  createdAt: string;
  updatedAt: string;
  version: number;
}

export interface BoardColumn {
  statusId: UUID;
  name: string;
  category: StatusCategory;
  color: string | null;
  position: number;
  issues: Issue[];
  issueCount: number;
  totalStoryPoints: number;
}

export interface Board {
  projectId: UUID;
  projectName: string;
  projectKey: string;
  columns: BoardColumn[];
  activeSprintId: UUID | null;
  activeSprintName: string | null;
}

export interface Sprint {
  id: UUID;
  projectId: UUID;
  name: string;
  goal: string | null;
  status: SprintStatus;
  startDate: string | null;
  endDate: string | null;
  velocity: number | null;
  completedPoints: number | null;
  totalPoints: number | null;
}

export interface CreateSprintRequest {
  name: string;
  goal?: string;
  startDate?: string;
  endDate?: string;
}

export interface SprintCompleteRequest {
  carryOverIssueIds?: UUID[];
  targetSprintId?: UUID;
}

export interface SprintCompletionResult {
  sprint: Sprint;
  completedIssueCount: number;
  incompleteIssueCount: number;
  carriedOverCount: number;
  velocity: number;
}

export interface WorkflowStatus {
  id: UUID;
  projectId: UUID;
  name: string;
  category: StatusCategory;
  position: number;
  color: string | null;
}

export interface Notification {
  id: UUID;
  type: string;
  title: string;
  message: string;
  resourceType: string | null;
  resourceId: UUID | null;
  isRead: boolean;
  createdAt: string;
}

// ─── API Response Wrappers ───────────────────────────────────────────────────

export interface ApiResponse<T> {
  success: boolean;
  data: T;
  message?: string;
}

export interface ApiError {
  errorCode: string;
  message: string;
  details?: Record<string, unknown>;
  requestId?: string;
  timestamp?: string;
}

// ─── Request DTOs ────────────────────────────────────────────────────────────

export interface LoginRequest {
  email: string;
  password: string;
}

export interface RegisterRequest {
  email: string;
  password: string;
  displayName: string;
}

export interface AuthResponse {
  token: string;
  tokenType: string;
  user: User;
}

export interface CreateProjectRequest {
  name: string;
  key: string;
  description?: string;
}

export interface CreateIssueRequest {
  issueType: IssueType;
  title: string;
  description?: string;
  priority?: Priority;
  assigneeId?: UUID;
  sprintId?: UUID;
  parentId?: UUID;
  storyPoints?: number;
  labels?: string[];
}

export interface UpdateIssueRequest {
  title?: string;
  description?: string;
  priority?: Priority;
  assigneeId?: UUID;
  sprintId?: UUID;
  storyPoints?: number;
  labels?: string[];
  version: number;
}

export interface TransitionRequest {
  targetStatusId: UUID;
}

// ─── WebSocket Event Types ───────────────────────────────────────────────────

export type BoardEventType =
  | 'ISSUE_CREATED'
  | 'ISSUE_UPDATED'
  | 'ISSUE_TRANSITIONED'
  | 'COMMENT_ADDED'
  | 'SPRINT_CREATED'
  | 'SPRINT_STARTED'
  | 'SPRINT_COMPLETED';

export interface BoardEvent {
  type: BoardEventType;
  projectId: UUID;
  payload: Record<string, unknown>;
  timestamp: string;
}

export interface PresenceUser {
  userId: UUID;
  displayName: string;
  avatarUrl: string | null;
}

export interface PresenceUpdate {
  users: PresenceUser[];
  count: number;
  timestamp: string;
}

export interface ProjectMember {
  userId: UUID;
  displayName: string;
  avatarUrl: string | null;
  role: string;
}

export interface CursorPage<T> {
  items: T[];
  nextCursor: string | null;
  hasMore: boolean;
  totalCount: number;
}

// ─── Comment Types ───────────────────────────────────────────────────────────

export interface Comment {
  id: UUID;
  issueId: UUID;
  authorId: UUID;
  authorName: string;
  parentId: UUID | null;
  body: string;
  mentions: UUID[];
  replies: Comment[];
  createdAt: string;
  updatedAt: string;
  version: number;
}

export interface CreateCommentRequest {
  body: string;
  parentId?: UUID;
}

// ─── Activity Types ──────────────────────────────────────────────────────────

export interface Activity {
  id: UUID;
  projectId: UUID;
  issueId: UUID | null;
  userId: UUID;
  userName: string;
  action: string;
  changes: Record<string, unknown>;
  metadata: Record<string, unknown>;
  createdAt: string;
}