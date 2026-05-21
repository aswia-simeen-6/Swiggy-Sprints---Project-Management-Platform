# Project Management Platform

A full-featured, Jira-like project management platform built for engineering teams to plan, track, and deliver software collaboratively. Features a robust Spring Boot backend with a configurable workflow engine, real-time collaboration via WebSockets, and a modern React frontend with drag-and-drop Kanban boards.

> **Live Demo:** `<HOSTED_URL>`  
> **API Docs (Swagger):** `<HOSTED_URL>/swagger-ui.html`

---

## Table of Contents

- [Architecture Overview](#architecture-overview)
- [Tech Stack](#tech-stack)
- [Features](#features)
- [Database Schema (ERD)](#database-schema-erd)
- [API Documentation](#api-documentation)
- [Setup Instructions](#setup-instructions)
- [Project Structure](#project-structure)
- [Design Decisions & Trade-offs](#design-decisions--trade-offs)
- [Sample Scenarios](#sample-scenarios)
- [What I'd Do With More Time](#what-id-do-with-more-time)

---

## Architecture Overview

```
┌─────────────────────────────────────────────────────────────────┐
│                        React Frontend                           │
│  (Vite + TypeScript + Tailwind + Zustand + dnd-kit + STOMP)    │
└──────────────┬─────────────────────────┬────────────────────────┘
               │ REST (HTTP)             │ WebSocket (STOMP/SockJS)
               ▼                         ▼
┌─────────────────────────────────────────────────────────────────┐
│                    Spring Boot 3.4 Backend                       │
│                       (Java 21 + Virtual Threads)                │
│  ┌──────────┐  ┌──────────────┐  ┌────────────┐  ┌───────────┐ │
│  │   REST   │  │  Workflow    │  │  WebSocket  │  │  Search   │ │
│  │   APIs   │  │  Engine      │  │  Broker     │  │  (FTS)    │ │
│  └────┬─────┘  └──────┬───────┘  └──────┬─────┘  └─────┬─────┘ │
│       │               │                 │               │       │
│  ┌────▼───────────────▼─────────────────▼───────────────▼─────┐ │
│  │              Service Layer (Domain-Driven)                  │ │
│  │   (Issue · Sprint · Workflow · Comment · Notification ·     │ │
│  │    Activity · Search · Project · User)                      │ │
│  └────────────────────┬────────────────────────────────────────┘ │
│                       │  Spring Data JPA                         │
└───────────────────────┼─────────────────────────────────────────┘
               ┌────────┴────────┐
               ▼                 ▼
        ┌─────────────┐   ┌───────────┐
        │ PostgreSQL   │   │   Redis   │
        │    16        │   │     7     │
        │ (Data + FTS) │   │ (Rate     │
        │              │   │  Limit +  │
        │              │   │  Presence)│
        └─────────────┘   └───────────┘
```

The application follows a **domain-driven, layered architecture**:

- **Controller Layer** — REST endpoints + WebSocket message handlers
- **Service Layer** — Business logic, workflow orchestration, event publishing
- **Repository Layer** — Spring Data JPA with custom queries, pessimistic locking
- **Domain Events** — Async event-driven communication (Spring `@EventListener`) for WebSocket broadcasts, notifications, and activity logging

---

## Tech Stack

| Layer | Technology |
|-------|-----------|
| **Language** | Java 21 (Virtual Threads, Preview Features) |
| **Framework** | Spring Boot 3.4.1 |
| **Database** | PostgreSQL 16 |
| **Cache / Pub-Sub** | Redis 7 (Lettuce) |
| **ORM** | Spring Data JPA / Hibernate 6.3 |
| **Migrations** | Flyway (14 versioned migrations) |
| **Auth** | JWT (jjwt 0.12) + Spring Security 6 |
| **Real-Time** | WebSocket (STOMP over SockJS) |
| **Search** | PostgreSQL Full-Text Search (tsvector + GIN index) |
| **API Docs** | SpringDoc OpenAPI 2.7 (Swagger UI) |
| **Mapping** | MapStruct 1.6 |
| **Build** | Gradle (Kotlin DSL) |
| **Containerization** | Docker (multi-stage, Temurin 21, ZGC) |
| **Testing** | JUnit 5, Testcontainers, AssertJ |
| **Frontend** | React 18, TypeScript, Vite, Tailwind CSS, Zustand, dnd-kit, Framer Motion |

---

## Features

### Core Requirements (All Implemented ✅)

#### 1. Data Model & Storage Layer
- ✅ Relational schema for **Projects, Issues, Sprints, Users, Comments, Activity Log**
- ✅ Five issue types: **Epic, Story, Task, Bug, Sub-task**
- ✅ Parent-child hierarchy (Epic → Story → Sub-task) with `parent_id` foreign key
- ✅ Full audit trail — immutable, append-only `activity_log` table with triggers preventing UPDATE/DELETE
- ✅ **Custom fields** per project (Text, Number, Dropdown, Date, Checkbox, URL) stored as JSONB
- ✅ High throughput — connection pooling (HikariCP, 20 max), virtual threads, batch inserts/updates

#### 2. Issue & Workflow Engine
- ✅ Configurable status columns per project (To Do → In Progress → In Review → Done)
- ✅ **Transition rules** — database-driven allowed transitions with validation
- ✅ **Automatic actions** on transitions (e.g., assign reviewer via `SetFieldAction`, notify watchers via `NotifyWatchersAction`)
- ✅ **Validation hooks / Conditions** — pluggable condition pipeline (`FieldRequiredCondition`, `StoryPointsSetCondition`) prevents transitions when conditions are unmet
- ✅ Sprint CRUD with date ranges and status lifecycle (Planned → Active → Completed)
- ✅ Move issues between backlog and active sprint
- ✅ **Sprint completion** with selective carry-over of incomplete items
- ✅ **Sprint velocity tracking** (story points completed per sprint, materialized view)

#### 3. Collaboration APIs
- ✅ Threaded comments with `parent_id` for replies
- ✅ **@mentions** extracted from comment body (stored as `mentions` array)
- ✅ Activity feed — paginated, filterable event stream per project and per issue
- ✅ **Notification system** — in-app notifications for assignments, mentions, status changes, comments
- ✅ Unread count endpoint + mark-read / mark-all-read
- ✅ **Watcher subscription** — watch/unwatch issues

#### 4. Real-Time Sync
- ✅ WebSocket server (STOMP over SockJS) broadcasting board state changes
- ✅ Event types: `ISSUE_CREATED`, `ISSUE_UPDATED`, `ISSUE_TRANSITIONED`, `COMMENT_ADDED`, `SPRINT_UPDATED`
- ✅ **Presence tracking** — who is viewing a board (Redis-backed with TTL + heartbeat)
- ✅ Authenticated WebSocket connections (JWT in STOMP CONNECT frame)
- ✅ User-specific notification channel (`/user/{id}/queue/notifications`)

#### 5. Search & Filtering
- ✅ **Full-text search** across issue titles, descriptions, and comments (PostgreSQL `tsvector` with weighted ranking: title=A, description=B, comments=C)
- ✅ Structured query filters: status, assignee, priority, issue type, sprint, labels
- ✅ GIN index for performant FTS queries
- ✅ **Cursor-based pagination** with `CursorPage` response model
- ✅ Auto-maintained search vectors via database triggers (updates on issue/comment changes)

### Beyond Requirements (Extra Features) ⭐

| Feature | Description |
|---------|-------------|
| **Optimistic Locking** | `@Version` on all entities with `409 CONFLICT` response for concurrent update detection |
| **Pessimistic Locking** | `SELECT FOR UPDATE` on workflow transitions to prevent race conditions |
| **Rate Limiting** | Redis-based token bucket algorithm (Lua script) — 100 req/s, 150 burst capacity |
| **Request Idempotency** | `IdempotencyFilter` prevents duplicate processing of retried requests |
| **Request Tracing** | `X-Request-ID` header injected on every request for distributed tracing |
| **ETag Support** | Conditional responses for bandwidth optimization |
| **UUIDv7** | Time-ordered UUIDs for better index locality and natural ordering |
| **Soft Deletes** | All entities use `deleted_at` with partial indexes (`WHERE deleted_at IS NULL`) |
| **Virtual Threads** | Java 21 virtual threads enabled for high-concurrency I/O workloads |
| **ZGC** | Z Garbage Collector configured in Docker for low-latency GC pauses |
| **Materialized Views** | `mv_board_summary` (column stats) and `mv_sprint_velocity` (per-sprint metrics) with concurrent refresh |
| **Non-Root Docker** | Multi-stage Dockerfile running as non-root `appuser` for security |
| **Health Checks** | Docker + Spring Actuator health probes on all services |
| **Pluggable Workflow Engine** | Registry-based condition/action pipeline — extensible without code changes to core engine |
| **React Frontend** | Full Kanban board UI with drag-and-drop, real-time updates, presence indicators, and dark theme |

---

## Database Schema (ERD)

```
┌──────────────┐       ┌───────────────────┐       ┌─────────────────┐
│    users     │       │     projects      │       │ project_members  │
├──────────────┤       ├───────────────────┤       ├─────────────────┤
│ id (PK)      │◄──┐   │ id (PK)           │◄──────│ project_id (FK) │
│ email        │   │   │ name              │       │ user_id (FK)    │
│ display_name │   │   │ key (unique)      │       │ role            │
│ password_hash│   │   │ owner_id (FK)─────┼───►   │ joined_at       │
│ role         │   │   │ issue_counter     │       └─────────────────┘
│ version      │   │   │ version           │
│ deleted_at   │   │   │ deleted_at        │
└──────────────┘   │   └───────────────────┘
       ▲           │            ▲
       │           │            │
       │           │   ┌────────┴──────────────────────────────┐
       │           │   │              issues                    │
       │           │   ├───────────────────────────────────────┤
       │           │   │ id (PK)                                │
       │           │   │ issue_key (unique, e.g., PROJ-123)     │
       │           │   │ project_id (FK) ──────────────────►    │
       │           ├───│ assignee_id (FK)                       │
       │           ├───│ reporter_id (FK)                       │
       │           │   │ sprint_id (FK) ──────────► sprints     │
       │           │   │ parent_id (FK, self-ref) ──► issues    │
       │           │   │ status_id (FK) ──► workflow_statuses   │
       │           │   │ issue_type (ENUM)                      │
       │           │   │ priority (ENUM)                        │
       │           │   │ title, description                     │
       │           │   │ story_points                           │
       │           │   │ labels (TEXT[])                         │
       │           │   │ custom_fields (JSONB)                  │
       │           │   │ search_vector (TSVECTOR)               │
       │           │   │ version, deleted_at                    │
       │           │   └──────┬───────────┬────────────────────┘
       │           │          │           │
       │           │   ┌──────▼──────┐   ┌▼──────────────┐
       │           │   │  comments   │   │ issue_watchers │
       │           │   ├─────────────┤   ├───────────────┤
       │           │   │ id (PK)     │   │ issue_id (FK) │
       │           └───│ author_id   │   │ user_id (FK)  │
       │               │ issue_id    │   └───────────────┘
       │               │ parent_id   │
       │               │ body        │
       │               │ mentions[]  │
       │               └─────────────┘
       │
┌──────┴──────────────┐    ┌──────────────────────┐
│   activity_log      │    │    notifications      │
├─────────────────────┤    ├──────────────────────┤
│ id (PK)             │    │ id (PK)              │
│ project_id (FK)     │    │ user_id (FK)         │
│ issue_id (FK)       │    │ type                 │
│ user_id (FK)        │    │ title, message       │
│ action (ENUM)       │    │ resource_type/id     │
│ changes (JSONB)     │    │ read                 │
│ metadata (JSONB)    │    └──────────────────────┘
│ created_at          │
│ (immutable — no     │
│  UPDATE/DELETE)     │
└─────────────────────┘

┌───────────────────┐    ┌─────────────────────┐    ┌──────────────────────┐
│ workflow_statuses │    │ workflow_transitions │    │ transition_conditions│
├───────────────────┤    ├─────────────────────┤    │ transition_actions   │
│ id (PK)           │◄───│ from_status_id (FK) │    ├──────────────────────┤
│ project_id (FK)   │    │ to_status_id (FK)───┼►   │ transition_id (FK)   │
│ name              │    │ project_id (FK)     │    │ type (ENUM)          │
│ category          │    └─────────────────────┘    │ config (JSONB)       │
│ display_order     │                               └──────────────────────┘
└───────────────────┘

┌──────────────────┐    ┌──────────────────────────┐
│    sprints       │    │ custom_field_definitions  │
├──────────────────┤    ├──────────────────────────┤
│ id (PK)          │    │ id (PK)                  │
│ project_id (FK)  │    │ project_id (FK)          │
│ name             │    │ name, field_type         │
│ goal             │    │ required, options (JSONB) │
│ start/end_date   │    └──────────────────────────┘
│ status (ENUM)    │
│ velocity         │
│ completed_points │
│ total_points     │
└──────────────────┘
```

**Key Design Choices:**
- **UUIDv7 primary keys** — time-ordered for B-tree locality, no sequential scanning
- **JSONB for custom fields** — flexible schema without EAV anti-pattern
- **PostgreSQL arrays** for labels — efficient with GIN indexes
- **tsvector + GIN** for full-text search — no external search engine needed
- **Partial indexes** on `deleted_at IS NULL` — soft deletes don't bloat index scans
- **Immutable activity log** — database-level triggers prevent tampering

---

## API Documentation

Interactive Swagger UI is available at `/swagger-ui.html` when the application is running.

### Endpoint Summary

| Method | Endpoint | Description |
|--------|----------|-------------|
| **Auth** | | |
| POST | `/api/auth/register` | Register a new user |
| POST | `/api/auth/login` | Login and receive JWT |
| GET | `/api/auth/me` | Get current user profile |
| GET | `/api/auth/search` | Search users by name/email |
| **Projects** | | |
| POST | `/api/projects` | Create a project |
| GET | `/api/projects` | List user's projects |
| GET | `/api/projects/{id}` | Get project details |
| POST | `/api/projects/{id}/members` | Add member to project |
| GET | `/api/projects/{id}/members` | List project members |
| **Issues** | | |
| POST | `/api/projects/{projectId}/issues` | Create issue |
| GET | `/api/projects/{projectId}/board` | Get board state (columns + issues) |
| GET | `/api/issues/{id}` | Get issue by ID |
| GET | `/api/issues/key/{issueKey}` | Get issue by key (e.g., PROJ-123) |
| PATCH | `/api/issues/{id}` | Update issue fields (optimistic lock) |
| DELETE | `/api/issues/{id}` | Soft-delete issue |
| POST | `/api/issues/{id}/transitions` | Transition issue status |
| GET | `/api/issues/{id}/transitions` | Get allowed transitions |
| POST | `/api/issues/{id}/watch` | Watch an issue |
| DELETE | `/api/issues/{id}/watch` | Unwatch an issue |
| GET | `/api/issues/{id}/children` | Get child issues |
| **Sprints** | | |
| POST | `/api/projects/{projectId}/sprints` | Create sprint |
| GET | `/api/projects/{projectId}/sprints` | List sprints |
| GET | `/api/sprints/{id}` | Get sprint details |
| POST | `/api/sprints/{id}/start` | Start a sprint |
| POST | `/api/sprints/{id}/complete` | Complete sprint (with carry-over) |
| GET | `/api/projects/{projectId}/velocity` | Get sprint velocity history |
| **Comments** | | |
| POST | `/api/issues/{issueId}/comments` | Add comment (supports @mentions) |
| GET | `/api/issues/{issueId}/comments` | List comments (threaded) |
| DELETE | `/api/issues/{issueId}/comments/{id}` | Delete comment |
| **Activity** | | |
| GET | `/api/projects/{projectId}/activity` | Project activity feed (paginated) |
| GET | `/api/issues/{issueId}/activity` | Issue activity history |
| **Notifications** | | |
| GET | `/api/notifications` | List user notifications |
| GET | `/api/notifications/unread-count` | Get unread count |
| PATCH | `/api/notifications/{id}/read` | Mark notification as read |
| POST | `/api/notifications/read-all` | Mark all as read |
| **Workflow** | | |
| GET | `/api/projects/{projectId}/workflow/statuses` | List workflow statuses |
| GET | `/api/projects/{projectId}/workflow/transitions` | List transition rules |
| **Search** | | |
| GET | `/api/search` | Full-text + structured search |
| **Health** | | |
| GET | `/health` | Application health check |

### WebSocket Topics

| Destination | Direction | Purpose |
|-------------|-----------|---------|
| `/ws` | Connect | SockJS endpoint |
| `/app/board/{projectId}/join` | Client → Server | Join board (presence) |
| `/app/board/{projectId}/leave` | Client → Server | Leave board |
| `/app/board/{projectId}/heartbeat` | Client → Server | Keep presence alive |
| `/app/board/{projectId}/cursor` | Client → Server | Share cursor position |
| `/topic/board/{projectId}/updates` | Server → Client | Board state changes |
| `/user/{userId}/queue/notifications` | Server → Client | Personal notifications |

### Authentication

All endpoints (except `/api/auth/register`, `/api/auth/login`, `/health`, `/swagger-ui/**`) require a JWT Bearer token:

```
Authorization: Bearer <jwt_token>
```

---

## Setup Instructions

### Prerequisites

- **Docker** & **Docker Compose** (recommended)
- OR: Java 21, PostgreSQL 16, Redis 7, Node.js 18+

### Option 1: Docker Compose (Recommended)

```bash
# Clone the repository
git clone https://github.com/<your-username>/project-mgmt-platform.git
cd project-mgmt-platform

# Start all services (backend + PostgreSQL + Redis)
docker-compose up --build -d

# The app starts at http://localhost:8080
# Swagger UI: http://localhost:8080/swagger-ui.html
```

### Option 2: Local Development

```bash
# 1. Start PostgreSQL and Redis
docker-compose up db redis -d

# 2. Run the backend
./gradlew bootRun

# 3. Run the frontend (optional — for UI)
cd frontend
npm install
npm run dev
# Frontend runs at http://localhost:5173
```

### Seed Data

The application auto-seeds on first run (via `V12__seed_default_data.sql`):

| User | Email | Password | Role |
|------|-------|----------|------|
| Admin User | admin@example.com | `password123` | ADMIN |
| Jane Smith | jane@example.com | `password123` | MEMBER |
| Bob Chen | bob@example.com | `password123` | MEMBER |

**Seeded Project:** "Project Alpha" (key: `ALPHA`) with 4 workflow statuses and 5 transition rules.

### Environment Variables

| Variable | Default | Description |
|----------|---------|-------------|
| `SPRING_DATASOURCE_URL` | `jdbc:postgresql://localhost:5432/projectmgmt` | Database URL |
| `SPRING_DATASOURCE_USERNAME` | `app` | Database user |
| `SPRING_DATASOURCE_PASSWORD` | `changeme` | Database password |
| `SPRING_DATA_REDIS_HOST` | `localhost` | Redis host |
| `SPRING_DATA_REDIS_PORT` | `6379` | Redis port |
| `JWT_SECRET` | (dev default) | JWT signing secret (change in production!) |
| `JWT_EXPIRATION_MS` | `86400000` (24h) | Token expiration |
| `SERVER_PORT` | `8080` | Application port |
| `SPRING_PROFILES_ACTIVE` | — | `dev` / `prod` |

---

## Project Structure

```
project-mgmt-platform/
├── src/main/java/com/projectmgmt/
│   ├── ProjectMgmtApplication.java          # Entry point
│   ├── common/                              # Cross-cutting concerns
│   │   ├── exception/                       #   Global exception handling
│   │   ├── filter/                          #   JWT, Rate Limit, Idempotency, Request ID
│   │   ├── model/                           #   ApiResponse, CursorPage, ErrorResponse
│   │   └── util/                            #   JWT, ETag, Cursor, UUIDv7 utilities
│   ├── config/                              # Spring configuration
│   │   ├── SecurityConfig.java              #   Spring Security + CORS
│   │   ├── WebSocketConfig.java             #   STOMP/SockJS configuration
│   │   ├── RedisConfig.java                 #   Redis / Lettuce pool
│   │   └── OpenApiConfig.java               #   Swagger UI setup
│   ├── event/                               # Domain events (async decoupling)
│   │   ├── DomainEvent.java                 #   Base event
│   │   ├── DomainEventListener.java         #   Activity log + notification generation
│   │   └── Issue/Sprint/Comment events      #   Typed event classes
│   └── domain/                              # Business domains
│       ├── activity/                        #   Activity log (immutable audit trail)
│       ├── comment/                         #   Threaded comments + @mentions
│       ├── common/                          #   BaseEntity, WebSocket broadcasting, Presence
│       ├── customfield/                     #   Custom field definitions
│       ├── issue/                           #   Issues (CRUD, board, watchers)
│       ├── notification/                    #   In-app notifications
│       ├── project/                         #   Projects + members
│       ├── search/                          #   Full-text + structured search
│       ├── sprint/                          #   Sprint lifecycle + velocity
│       ├── user/                            #   Auth (register/login), user management
│       └── workflow/                        #   Workflow engine (statuses, transitions,
│           └── engine/                      #     conditions, actions — pluggable registry)
├── src/main/resources/
│   ├── application.yml                      # Main config
│   ├── application-dev.yml                  # Dev profile (debug SQL)
│   ├── application-prod.yml                 # Prod profile
│   └── db/migration/                        # Flyway migrations (V1–V14)
├── src/test/                                # Integration tests (Testcontainers)
├── frontend/                                # React SPA
│   └── src/
│       ├── components/board/                #   Kanban board (drag-and-drop)
│       ├── pages/                           #   Board, Login, Register, Projects
│       ├── services/                        #   API client, WebSocket client
│       ├── store/                           #   Zustand stores (auth, board, notifications)
│       └── types/                           #   TypeScript interfaces
├── Dockerfile                               # Multi-stage (builder + runtime)
├── docker-compose.yml                       # App + PostgreSQL 16 + Redis 7
└── build.gradle.kts                         # Gradle build (Kotlin DSL)
```

---

## Design Decisions & Trade-offs

### 1. PostgreSQL Full-Text Search vs. Elasticsearch
**Chose:** PostgreSQL `tsvector` with GIN indexes.  
**Why:** Eliminates operational complexity of a separate search cluster. PostgreSQL FTS with weighted ranking (title=A, description=B, comments=C) is sufficient for the scale of a project management tool. Triggers auto-maintain search vectors — zero application-level sync needed.  
**Trade-off:** Less flexible ranking/fuzzy matching than Elasticsearch. Acceptable for structured project data.

### 2. Domain Events vs. Direct Service Calls
**Chose:** Spring `ApplicationEventPublisher` with `@EventListener`.  
**Why:** Decouples core business logic from side effects (WebSocket broadcasts, notifications, activity logging). Adding new side effects requires no changes to service code.  
**Trade-off:** Slightly harder to trace execution flow. Mitigated by clear event naming and a single `DomainEventListener` entry point.

### 3. Optimistic + Pessimistic Locking (Dual Strategy)
**Chose:** `@Version` for general CRUD (optimistic), `SELECT FOR UPDATE` for workflow transitions (pessimistic).  
**Why:** General updates benefit from optimistic locking's zero-contention fast path. Workflow transitions require strict serialization to prevent invalid state.  
**Trade-off:** Pessimistic locks hold database connections longer. Acceptable since transitions are infrequent operations.

### 4. Redis for Rate Limiting + Presence (Not Sessions)
**Chose:** Redis for rate limiting (Lua-scripted token bucket) and WebSocket presence tracking only.  
**Why:** These are inherently ephemeral, high-frequency operations that benefit from in-memory speed. Sessions use stateless JWT — no server-side session storage needed.  
**Trade-off:** Redis is an additional operational dependency. Mitigated by graceful fallback (rate limiter passes through if Redis is down).

### 5. JSONB for Custom Fields vs. EAV Pattern
**Chose:** JSONB column on `issues` table.  
**Why:** Simpler queries, no multi-table joins for field lookups, native PostgreSQL indexing support.  
**Trade-off:** Less type-safe than dedicated columns. Mitigated by `custom_field_definitions` table that defines schemas per project.

### 6. Cursor-Based Pagination vs. Offset
**Chose:** Cursor-based pagination throughout.  
**Why:** Stable pagination under concurrent writes (no skipped/duplicated items). O(1) seek performance vs. O(n) offset scanning.  
**Trade-off:** Slightly more complex client implementation. Handled by the `CursorPage` response wrapper.

### 7. Virtual Threads (Java 21)
**Chose:** Enabled `spring.threads.virtual.enabled: true`.  
**Why:** Dramatically improves throughput for I/O-bound workloads (database queries, Redis calls) without thread pool tuning. Each request gets its own lightweight virtual thread.  
**Trade-off:** Requires careful avoidance of `synchronized` blocks (pinning). Codebase uses `ReentrantLock` where needed.

### 8. Monolith with Clear Domain Boundaries
**Chose:** Single deployable with domain-package structure over microservices.  
**Why:** Simpler deployment, no inter-service communication overhead, transactional consistency across domains. Package structure is microservice-ready if scaling demands it.  
**Trade-off:** Scales vertically only. Sufficient for the target scale (~500 concurrent users).

---

## Sample Scenarios

### Scenario 1: Concurrent Issue Updates ✅

Two users simultaneously update the same issue.

**How it's handled:**
1. Both users load the issue (both see `version: 3`)
2. User A updates the assignee → succeeds, version becomes `4`
3. User B updates the priority with stale `version: 3` → JPA detects mismatch → `409 CONFLICT`
4. User B refreshes (version `4`), retries → succeeds, version becomes `5`
5. Both users receive `ISSUE_UPDATED` WebSocket events reflecting the final state

**Key components:** `@Version` on `BaseEntity`, `GlobalExceptionHandler` → `409 CONFLICT`, `BoardEventBroadcaster`

### Scenario 2: Sprint Completion with Carry-Over ✅

A sprint ends with 3 incomplete stories (8 story points).

**How it's handled:**
1. `POST /api/sprints/{id}/complete` with `carryOverIssueIds` and `targetSprintId`
2. Service partitions issues into completed (status category = DONE) vs incomplete
3. Velocity calculated = sum of story points on completed issues
4. Selected incomplete issues move to target sprint; others return to backlog
5. `ActivityLog` entries created for each moved issue (`ISSUE_MOVED_TO_SPRINT` / `ISSUE_MOVED_TO_BACKLOG`)
6. Response includes `completedIssueCount`, `incompleteIssueCount`, `carriedOverCount`, `velocity`

**Key components:** `SprintService.completeSprint()`, `SprintCompletionResult` DTO, pessimistic lock on sprint

### Scenario 3: Workflow Violation ✅

User attempts to move an issue from "To Do" directly to "Done".

**How it's handled:**
1. `POST /api/issues/{id}/transitions` with `targetStatusId` = Done
2. `WorkflowEngine` queries `workflow_transitions` for (To Do → Done) — not found
3. Engine queries allowed transitions from "To Do" → returns `["In Progress"]`
4. Throws `WorkflowViolationException` → `GlobalExceptionHandler` returns:

```json
{
  "status": 422,
  "code": "WORKFLOW_TRANSITION_NOT_ALLOWED",
  "message": "Cannot transition from 'To Do' to 'Done'",
  "details": {
    "currentStatus": "To Do",
    "targetStatus": "Done",
    "allowedTransitions": ["In Progress"]
  }
}
```

**Key components:** `WorkflowEngine`, `WorkflowViolationException`, `GlobalExceptionHandler` → `422`

---

## Testing

Run the integration test suite (requires Docker for Testcontainers):

```bash
./gradlew test
```

Tests cover:
- Authentication flow (register, login, JWT validation)
- Project CRUD and member management
- Issue lifecycle (create, update, transition, delete)
- Workflow engine (valid transitions, violation handling, conditions)
- Sprint management (create, start, complete with carry-over)
- Comment CRUD with threading
- Health endpoint

---

## What I'd Do With More Time

- **WebSocket reconnection & missed event replay** — event sequence numbers with server-side buffer for gap detection
- **Role-based access control (RBAC)** — project-level roles (Admin, Maintainer, Developer, Viewer) with permission checks
- **Bulk operations** — multi-select issues for batch status transitions, assignment, or sprint moves
- **File attachments** — S3-compatible object storage integration
- **Email notifications** — async email delivery for @mentions and assignments via message queue
- **Burndown charts** — daily snapshot tracking for sprint burndown visualization
- **Custom workflow editor UI** — drag-and-drop workflow builder in the frontend
- **Audit log export** — CSV/JSON export of activity history for compliance
- **API rate limiting per user tier** — differentiated limits for free vs. paid plans
- **Horizontal scaling** — Redis pub/sub for WebSocket message fan-out across multiple instances

---

## License

MIT