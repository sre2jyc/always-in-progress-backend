# TaskFlow — Production-Grade Task Management Backend

> **A battle-tested, horizontally scalable RESTful backend for collaborative project and task management.**
> Built with **Spring Boot 3**, **Java 17**, **PostgreSQL 15**, **Flyway**, **JWT**, **SSE**, and **Docker** —
> engineered with a focus on clean architecture, zero-trust security, and operational observability.

---

## Table of Contents

- [Overview](#overview)
- [Tech Stack](#tech-stack)
- [System Architecture](#system-architecture)
- [Authentication & Security](#authentication--security)
- [Data Model](#data-model)
- [API Design](#api-design)
- [Real-Time Engine (SSE)](#real-time-engine-sse)
- [Concurrency & Consistency](#concurrency--consistency)
- [Database Migrations](#database-migrations)
- [Error Handling](#error-handling)
- [Observability & Logging](#observability--logging)
- [Running Locally](#running-locally)
- [Testing](#testing)
- [Engineering Tradeoffs & Future Roadmap](#engineering-tradeoffs--future-roadmap)

---

## Overview

TaskFlow is a **production-grade RESTful API** designed as a backend system for real-world collaborative task management. It models the kind of domain logic you'd find in tools like Jira, Linear, or Asana — project ownership, task lifecycle management, real-time notifications, and role-scoped access control — built with engineering rigour from day one.

The system was designed around three core tenets:

- **Correctness** — optimistic locking, transactional writes, and migration-first schema management prevent data races and schema drift
- **Observability** — structured logging throughout every service layer enables operational debugging without code spelunking
- **Scalability** — stateless JWT auth, paginated list APIs, and a Dockerized setup make horizontal scaling a natural next step

---

## Tech Stack

| Layer | Technology | Rationale |
|---|---|---|
| **Runtime** | Java 17 | LTS, stable JVM, mature ecosystem |
| **Framework** | Spring Boot 3 | Convention-over-configuration, mature ecosystem, battle-tested |
| **ORM** | Spring Data JPA / Hibernate | Reduces boilerplate; repository pattern aligns with clean arch |
| **Security** | Spring Security + JWT (HS256) | Stateless, horizontally scalable, widely adopted |
| **Database** | PostgreSQL 15 | ACID guarantees, strong JSON support, production-proven |
| **Migrations** | Flyway | Versioned, deterministic schema evolution with rollback support |
| **Real-Time** | Server-Sent Events (SSE) | Lightweight, browser-native; appropriate complexity for push-only notifications |
| **Containerisation** | Docker + Docker Compose | Reproducible builds, environment parity, single-command setup |
| **Logging** | SLF4J + Logback | Structured output, log-level control, integration-ready |
| **Build** | Maven | Dependency management, lifecycle hooks, CI-friendly |
| **Utilities** | Lombok | Boilerplate elimination (builders, constructors, logging annotations) |

---

## System Architecture

TaskFlow follows a **strict layered architecture** — no cross-layer leakage, no fat controllers, no anemic services.

```
┌──────────────────────────────────────────────┐
│                  Client Layer                │
│           (REST consumers / browsers)        │
└───────────────────────┬──────────────────────┘
                        │ HTTP / SSE
┌───────────────────────▼──────────────────────┐
│              Controller Layer                │
│   • HTTP request/response handling           │
│   • DTO validation (@Valid, @NotBlank)       │
│   • DTO ↔ Domain mapping                    │
│   • No business logic                        │
└───────────────────────┬──────────────────────┘
                        │
┌───────────────────────▼──────────────────────┐
│               Service Layer                  │
│   • Core business logic                      │
│   • Authorization enforcement                │
│   • SSE event publishing                     │
│   • Transaction boundary ownership           │
└───────────────────────┬──────────────────────┘
                        │
┌───────────────────────▼──────────────────────┐
│             Repository Layer                 │
│   • JPA repositories (Spring Data)           │
│   • Pagination & dynamic filtering           │
│   • Aggregations for stats endpoints         │
└───────────────────────┬──────────────────────┘
                        │
┌───────────────────────▼──────────────────────┐
│              PostgreSQL 15                   │
│   • Managed via Flyway versioned migrations  │
│   • UUID primary keys for distributed safety │
└──────────────────────────────────────────────┘
```

### Design Principles

- **Single Responsibility per Layer** — Controllers own HTTP semantics. Services own business invariants. Repositories own data access. Cross-cutting concerns (auth, logging) are handled by filters and AOP — not smeared across services.
- **DTO Boundary Discipline** — Request DTOs are used for input validation, while some response endpoints currently still return JPA entities directly. This is a pragmatic boundary that may be tightened in later iterations.
- **No God Classes** — every class has a clearly scoped responsibility. This makes unit testing deterministic and refactoring surgical.

---

## Authentication & Security

### JWT-Based Stateless Authentication

TaskFlow implements **zero-session, token-based auth** using JWTs — making each API node completely stateless and independently deployable.

```
POST /auth/register
  ├── Validate payload (name, email, password)
  ├── Hash password with BCrypt (strength 12)
  └── Persist user entity

POST /auth/login
  ├── Load user by email
  ├── Verify password against BCrypt hash
  └── Issue signed JWT (HS256, 24h TTL)
      ├── Claim: userId (UUID)
      └── Claim: email

All protected endpoints:
  ├── Extract Bearer token from Authorization header
  ├── Validate signature (HS256) + expiry
  └── Inject userId into Spring Security context
```

### JWT Specification

| Property | Value |
|---|---|
| Algorithm | HS256 |
| Expiry | 86,400,000 ms (24 hours) |
| Claims | `userId`, `email` |
| Password hashing | BCrypt, strength 12 |
| Auth header | `Authorization: Bearer <token>` |

### Security Design Rationale

**Why JWT over session-based auth?**

Stateless tokens eliminate the need for a shared session store, making horizontal scaling trivial. There's no session affinity requirement, no Redis dependency for auth state, and no sticky routing needed at the load balancer — each node can independently verify any request.

**Why BCrypt strength 12?**

BCrypt's adaptive cost factor makes brute-force attacks computationally infeasible even as hardware improves. Strength 12 provides a solid balance between security and acceptable registration latency (< 500ms on commodity hardware).

### Known Tradeoffs & Planned Improvements

| Current Limitation | Mitigation Path |
|---|---|
| No token revocation | Introduce short-lived tokens + Redis-backed token blacklist |
| No refresh token flow | Add `POST /auth/refresh` with rotating refresh tokens |
| No MFA support | TOTP layer via Google Authenticator / Authy |
| Single signing secret | Rotate to asymmetric RS256 (public/private keypair) |

---

## Data Model

```
┌───────────────────────────────────────┐
│                users                  │
│  id (UUID PK)                         │
│  name (VARCHAR)                       │
│  email (VARCHAR UNIQUE)               │
│  password_hash (VARCHAR)              │
│  created_at (TIMESTAMP)               │
└───────────────┬───────────────────────┘
                │ 1:N (owner)
┌───────────────▼───────────────────────┐
│              projects                 │
│  id (UUID PK)                         │
│  name (VARCHAR)                       │
│  description (TEXT)                   │
│  owner_id (UUID FK → users)           │
│  created_at (TIMESTAMP)               │
└───────────────┬───────────────────────┘
                │ 1:N (cascade delete)
┌───────────────▼───────────────────────┐
│               tasks                   │
│  id (UUID PK)                         │
│  project_id (UUID FK → projects)      │
│  creator_id (UUID FK → users)         │
│  assignee_id (UUID FK → users)        │
│  title (VARCHAR)                      │
│  description (TEXT)                   │
│  status (ENUM: TODO/IN_PROGRESS/DONE) │
│  priority (ENUM: LOW/MEDIUM/HIGH)     │
│  due_date (DATE)                      │
│  version (BIGINT) ← optimistic lock   │
│  updated_at (TIMESTAMP)               │
│  created_at (TIMESTAMP)               │
└───────────────────────────────────────┘
```

### Schema Design Decisions

- **UUID primary keys** — eliminates sequential ID enumeration attacks, distributed-safe without coordination
- **ENUM for status/priority** — enforces valid states at the DB layer, not just application layer
- **Cascade delete on project → tasks** — referential integrity enforced by the database, not application code
- **`version` + `updated_at` on tasks** — enables optimistic locking and audit tracing simultaneously

---

## API Design

### Authentication

```
POST   /auth/register      Register a new user
POST   /auth/login         Authenticate and receive JWT
```

### Projects

```
GET    /projects           List projects (owned + assigned) — paginated
POST   /projects           Create a new project
GET    /projects/{id}      Get project details + embedded tasks
PATCH  /projects/{id}      Partial update (owner only)
DELETE /projects/{id}      Delete project + cascade tasks (owner only)
GET    /projects/{id}/stats   Analytics: task counts by status and assignee
GET    /projects/{id}/events  SSE stream for real-time task events
```

### Tasks

```
GET    /projects/{id}/tasks        List tasks — paginated, filterable
POST   /projects/{id}/tasks        Create a task
PATCH  /tasks/{id}                 Partial update (PATCH semantics)
DELETE /tasks/{id}                 Delete task (owner or creator only)
```

### Task Query Parameters

```
GET /projects/{id}/tasks?page=0&limit=10&status=TODO&assignee=<uuid>
```

| Param | Type | Description |
|---|---|---|
| `page` | integer | Zero-indexed page number |
| `limit` | integer | Page size (default: 10) |
| `status` | enum | Filter by `TODO`, `IN_PROGRESS`, `DONE` |
| `assignee` | UUID | Filter by assignee user ID |

### API Design Rationale

- **PATCH over PUT for updates** — partial updates reduce client coupling and avoid over-posting vulnerabilities. Clients send only the fields they intend to change.
- **Project-scoped task routes** — `POST /projects/{id}/tasks` reinforces the ownership model and makes authorization straightforward: if you can access the project, you can operate on its tasks.
- **Paginated list endpoints by default** — unbounded list queries are a production outage waiting to happen. Pagination is a first-class concern, not an afterthought.

---

## Real-Time Engine (SSE)

### Overview

TaskFlow delivers live task lifecycle events to subscribed clients via **Server-Sent Events (SSE)** — a unidirectional, HTTP-native push mechanism that works out of the box with browsers and does not require WebSocket infrastructure.

### Endpoint

```
GET /projects/{id}/events
Accept: text/event-stream
Authorization: Bearer <token>
```

### Event Types

| Event | Trigger |
|---|---|
| `TASK_CREATED` | A new task is created in the project |
| `TASK_UPDATED` | Any field on a task is modified |
| `TASK_DELETED` | A task is deleted from the project |

### Architecture

```
Client subscribes to /projects/{id}/events
        │
        ▼
SseEmitter registered in-memory emitter registry
        │
        ▼
Task mutation in Service Layer
        │
        ▼
EventPublisher.publish(projectId, eventType, payload)
        │
        ▼
All emitters for projectId receive the event
```

### Why SSE over WebSockets?

SSE is a deliberate, right-sized choice for this use case:

- **Unidirectional push-only** — clients don't need to send data over the event stream; SSE is the correct abstraction
- **Zero client library dependency** — `EventSource` is a native browser API
- **HTTP/2 multiplexing** — SSE scales efficiently over HTTP/2 without per-connection TCP overhead
- **Simpler infrastructure** — no WebSocket upgrade handshake, no protocol negotiation, no proxy configuration headaches

### Current Limitations & Scale Path

The current implementation uses **in-memory emitter registration**, which bounds the system to a single instance.

For multi-node horizontal scale, the path is:

```
Current: In-memory emitter map (single node)
    ↓
Phase 1: Redis Pub/Sub fanout
    - Task mutation publishes to Redis channel: project:{id}:events
    - All API nodes subscribe and fan out to local SSE emitters
    ↓
Phase 2: Event replay support
    - Store last N events per project in Redis stream
    - Reconnecting clients request events since last-event-id
    - Eliminates missed events on reconnect
```

---

## Concurrency & Consistency

### Optimistic Locking

TaskFlow handles concurrent task updates using **JPA optimistic locking** — a non-blocking strategy that prevents lost updates without the throughput cost of pessimistic database locks.

```java
@Entity
public class Task {
    @Version
    private Long version;

    private Timestamp updatedAt;
    // ...
}
```

**Conflict semantics:**

```
Client A reads task (version=3)
Client B reads task (version=3)

Client A writes update → version incremented to 4 ✓
Client B writes update → version mismatch detected
                       → 409 Conflict returned ✗

Client B must re-read, re-apply changes, and retry
```

### Why Optimistic Over Pessimistic Locking?

- **Higher throughput** — no blocking DB locks held between read and write
- **Correct for low-contention workloads** — task edits are rarely concurrent on the same record; optimistic locking avoids unnecessary serialisation
- **Failure is explicit** — the 409 response surface the conflict to the client cleanly, enabling retry logic

---

## Database Migrations

Schema evolution is managed by **Flyway** — a versioned, migration-first approach that treats the database schema as code.

### Migration Strategy

| File Pattern | Purpose |
|---|---|
| `V{n}__{description}.sql` | Forward migration |
| `U{n}__{description}.sql` | Undo/rollback migration |

### Included Migrations

1. `V1__create_users.sql` — Users table with unique email constraint
2. `V2__create_projects.sql` — Projects table with owner FK
3. `V3__create_tasks.sql` — Tasks table with optimistic lock columns
4. `V4__seed_data.sql` — Local development seed data
5. `V5__add_version_updated_at.sql` — Optimistic locking columns

Flyway runs **automatically at application startup** — no manual migration step, no schema drift between environments.

### Seed Users (Local Testing)

```
Email:    test@example.com       Password: password123
Email:    assignee@example.com   Password: password123
```

---

## Error Handling

All error responses follow a consistent envelope schema, enforced by a **global exception handler** (`@RestControllerAdvice`):

```json
{
  "error": "validation failed",
  "fields": {
    "email": "is required",
    "title": "must not be blank"
  }
}
```

### HTTP Status Map

| Scenario | Status |
|---|---|
| Payload validation failure | `400 Bad Request` |
| Missing or invalid JWT | `401 Unauthorized` |
| Access denied (not owner/creator) | `403 Forbidden` |
| Entity not found | `404 Not Found` |
| Optimistic lock conflict | `409 Conflict` |

---

## Observability & Logging

TaskFlow uses **structured SLF4J/Logback logging** throughout the service layer — every significant domain event emits a log entry with contextual metadata, not free-form strings.

### Logged Events

| Category | Events |
|---|---|
| **Auth** | Registration, login success/failure |
| **Projects** | Created, updated, deleted, fetched |
| **Tasks** | Created, updated, deleted, conflict detected |
| **SSE** | Subscription opened, event dispatched, emitter closed |

### Why Structured Logging?

Structured logs (key=value or JSON) are machine-parseable — they flow naturally into log aggregation pipelines (ELK, Loki, Datadog) without regex-based parsing. When debugging a production incident, grep-friendly logs collapse investigation time from hours to minutes.

### Production Observability Roadmap

| Capability | Tooling |
|---|---|
| Metrics (latency, error rate, DB pool) | Micrometer + Prometheus |
| Distributed tracing | OpenTelemetry + Jaeger |
| Dashboards + alerting | Grafana + PagerDuty |

---

## Running Locally

### Prerequisites

- Docker ≥ 24.x
- Docker Compose ≥ 2.x

### One-Command Setup

```bash
git clone https://github.com/sre2jyc/taskflow-sreejit-chaudhury.git
cd taskflow-sreejit-chaudhury

cp .env.example .env

docker compose up --build
```

### Services

| Service | Address |
|---|---|
| REST API | `http://localhost:8080` |
| PostgreSQL | `localhost:5433` |

### Environment Variables

```env
DB_USERNAME=taskflow
DB_PASSWORD=taskflow
DB_NAME=taskflow

JWT_SECRET=taskflow-super-secret-key-for-jwt-signing-123456
JWT_EXPIRATION=86400000

API_PORT=8080
```

> In a real production deployment, secrets would be injected via a secrets manager (AWS SSM, HashiCorp Vault) — never committed to version control.

---

## Testing

### Integration Tests (Maven)

```bash
cd backend
./mvnw test
```

### End-to-End Shell Suite

```bash
chmod +x test_taskflow.sh
./test_taskflow.sh
```

### Postman Collection

A reusable Postman collection is available in `postman/TaskFlow.postman_collection.json` with a corresponding environment file at `postman/TaskFlow.postman_environment.json`.

### Coverage

| Metric | Value |
|---|---|
| Instruction coverage | **83%** |
| Branch coverage | **67%** |
| Integration tests passing | **13 / 13** |

### Testing Philosophy

Integration tests are run against a real embedded database — not mocked repositories. This validates the full stack (controller → service → repository → DB) including constraint enforcement, cascade behaviour, and migration correctness. Mocking the DB layer provides false confidence; integration tests catch the bugs that matter.

---

## Engineering Tradeoffs & Future Roadmap

### Current Scope Decisions

| Decision | Rationale |
|---|---|
| Single project owner | Keeps auth model simple and predictable; collaboration via RBAC is a Phase 2 concern |
| In-memory SSE emitters | Correct for single-instance deployment; Redis Pub/Sub is a well-understood scale path |
| No refresh token flow | JWT 24h TTL is appropriate for the assignment scope; refresh + blacklist adds ~2 days of complexity |
| No soft delete | Simplifies queries; audit requirements would drive this decision in production |

### Phase 2 Roadmap

#### 1. Collaboration & RBAC

Introduce a `project_members` join table with role-scoped permissions:

```sql
CREATE TABLE project_members (
  project_id  UUID REFERENCES projects(id),
  user_id     UUID REFERENCES users(id),
  role        VARCHAR CHECK (role IN ('OWNER', 'ADMIN', 'EDITOR', 'VIEWER')),
  PRIMARY KEY (project_id, user_id)
);
```

This enables shared team workspaces with granular permission inheritance — task assignment, comment, status update — gated per role.

#### 2. Async Notification Pipeline

Replace synchronous SSE with a durable async event pipeline for email, push, and in-app alerts:

```
Task mutation → Kafka topic → Notification worker
                                  ├── Email (SES / SendGrid)
                                  ├── Push (FCM / APNs)
                                  └── In-app (SSE / WebSocket)
```

This decouples the write path from notification delivery — mutations complete in microseconds regardless of notification fanout cost.

#### 3. Distributed Real-Time Infrastructure

```
Phase 1 (current): In-memory emitter registry
Phase 2: Redis Pub/Sub fanout across nodes
Phase 3: WebSocket upgrade for bidirectional collaboration (comments, live cursors)
Phase 4: Event replay via Redis Streams (reliable reconnect)
```

#### 4. Full Audit Trail

Append-only task activity log for accountability and rollback visibility:

```sql
CREATE TABLE task_activity_logs (
  id           UUID PRIMARY KEY,
  task_id      UUID REFERENCES tasks(id),
  user_id      UUID REFERENCES users(id),
  action_type  VARCHAR,   -- STATUS_CHANGED, ASSIGNEE_CHANGED, etc.
  old_value    JSONB,
  new_value    JSONB,
  created_at   TIMESTAMP
);
```

#### 5. Production Observability Stack

- **Micrometer + Prometheus** for request latency, error rates, and DB pool metrics
- **OpenTelemetry + Jaeger** for distributed trace propagation
- **Grafana dashboards** for SLO monitoring and capacity planning alerts

---

## Highlights

```
✔  JWT stateless auth — horizontally scalable, session-free
✔  BCrypt password hashing
✔  Clean layered architecture — no cross-layer leakage
✔  Project + task CRUD with ownership enforcement
✔  SSE real-time push for TASK_CREATED / UPDATED / DELETED
✔  Optimistic locking with 409 Conflict on concurrent writes
✔  Paginated, filterable list APIs — production-safe by design
✔  Project analytics endpoint (by status, by assignee)
✔  Flyway versioned migrations with rollback support
✔  Docker Compose one-command local setup
✔  Structured SLF4J logging throughout service layer
✔  Global exception handler with consistent error envelope
✔  83% instruction coverage · 13 integration tests passing
```

---

## Author

**Sreejit Chaudhury**

Engineered with focus on production readiness, clean architecture, operational observability, and systems thinking.
