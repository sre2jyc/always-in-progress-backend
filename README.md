# TaskFlow — Production-Grade Task Management Backend

> **A battle-tested, horizontally scalable RESTful backend for collaborative project and task management.**
> Built with **Spring Boot 3**, **Java 17**, **PostgreSQL 15**, **Redis**, **Flyway**, **JWT**, **SSE**, and **Docker** —
> engineered with a focus on clean architecture, zero-trust security, distributed real-time infrastructure, and a guaranteed-delivery audit trail.

---

## Table of Contents

- [Overview](#overview)
- [Tech Stack](#tech-stack)
- [System Architecture](#system-architecture)
- [Authentication & Security](#authentication--security)
- [Data Model](#data-model)
- [API Design](#api-design)
- [Real-Time Engine (SSE + Redis Pub/Sub)](#real-time-engine-sse--redis-pubsub)
- [Audit Logging (Transactional Outbox)](#audit-logging-transactional-outbox)
- [Concurrency & Consistency](#concurrency--consistency)
- [Database Migrations](#database-migrations)
- [Error Handling](#error-handling)
- [Observability & Logging](#observability--logging)
- [Running Locally](#running-locally)
- [Testing](#testing)
- [Engineering Tradeoffs & Future Roadmap](#engineering-tradeoffs--future-roadmap)

---

## Overview

TaskFlow is a **production-grade RESTful API** designed as a backend system for real-world collaborative task management. It models the kind of domain logic you'd find in tools like Jira, Linear, or Asana — project ownership, task lifecycle management, distributed real-time notifications, and a complete audit trail — built with engineering rigour from day one.

The system was designed around four core tenets:

- **Correctness** — optimistic locking, transactional writes, and migration-first schema management prevent data races and schema drift
- **Observability** — structured logging and a full append-only audit log enable operational debugging and accountability
- **Scalability** — stateless JWT auth, Redis Pub/Sub SSE fanout, and a Dockerized setup make horizontal scaling a natural next step
- **Reliability** — the Transactional Outbox pattern guarantees zero audit message loss even under failure conditions

---

## Tech Stack

| Layer | Technology | Rationale |
|---|---|---|
| **Runtime** | Java 17 | LTS, stable JVM, mature ecosystem |
| **Framework** | Spring Boot 3 | Convention-over-configuration, mature ecosystem, battle-tested |
| **ORM** | Spring Data JPA / Hibernate | Reduces boilerplate; repository pattern aligns with clean arch |
| **Security** | Spring Security + JWT (HS256) | Stateless, horizontally scalable, widely adopted |
| **Database** | PostgreSQL 15 | ACID guarantees, strong JSONB support, production-proven |
| **Message Broker** | Redis 7 (Pub/Sub) | Distributed SSE fanout across API instances; Lettuce client via Spring Data Redis |
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
│   • No business logic                        │
└───────────────────────┬──────────────────────┘
                        │
┌───────────────────────▼──────────────────────┐
│               Service Layer                  │
│   • Core business logic                      │
│   • Authorization enforcement                │
│   • Outbox write (atomic with task save)     │
│   • Redis Pub/Sub event publishing           │
│   • Transaction boundary ownership           │
└──────────┬────────────────────────┬──────────┘
           │                        │
┌──────────▼──────────┐  ┌─────────▼──────────┐
│   Repository Layer  │  │ RedisEventPublisher │
│   JPA / Spring Data │  │ publishes to Redis  │
└──────────┬──────────┘  └─────────┬──────────┘
           │                        │
┌──────────▼──────────┐  ┌─────────▼──────────┐
│   PostgreSQL 15     │  │     Redis 7         │
│   tasks             │  │  taskflow:          │
│   outbox            │  │  task-events        │
│   task_activity_logs│  │  channel            │
└─────────────────────┘  └─────────┬──────────┘
                                    │
                         ┌──────────▼──────────┐
                         │ RedisEventSubscriber │
                         │ fans out to local    │
                         │ SSE emitters         │
                         └─────────────────────┘
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

┌───────────────────────────────────────┐
│               outbox                  │
│  id (UUID PK)                         │
│  event_type (VARCHAR)                 │
│  payload (JSONB)                      │
│  processed (BOOLEAN)                  │
│  created_at (TIMESTAMP)               │
│  processed_at (TIMESTAMP)             │
└───────────────────────────────────────┘

┌───────────────────────────────────────┐
│         task_activity_logs            │
│  id (UUID PK)                         │
│  task_id (UUID — no FK intentional)   │
│  project_id (UUID FK → projects)      │
│  actor_id (UUID FK → users)           │
│  actor_type (USER/AI_AGENT/SYSTEM)    │
│  action_type (CREATED/UPDATED/etc.)   │
│  old_value (JSONB)                    │
│  new_value (JSONB)                    │
│  changed_fields (TEXT[])              │
│  conversation_id (UUID nullable)      │
│  metadata (JSONB nullable)            │
│  created_at (TIMESTAMP)               │
└───────────────────────────────────────┘
```

### Schema Design Decisions

- **UUID primary keys** — eliminates sequential ID enumeration attacks, distributed-safe without coordination
- **ENUM for status/priority** — enforces valid states at the DB layer, not just application layer
- **Cascade delete on project → tasks** — referential integrity enforced by the database, not application code
- **`version` + `updated_at` on tasks** — enables optimistic locking and audit tracing simultaneously
- **`task_id` has no FK on audit logs** — audit records must outlive the task. Deleting a task preserves its full mutation history
- **Transactional Outbox** — the task save and outbox insert are atomic; a background worker drains outbox entries into `task_activity_logs` asynchronously, decoupling the write path from audit persistence

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

### Audit & Activity

```
GET    /tasks/{id}/history                           Full mutation history for a task
GET    /projects/{id}/activity                       All activity across a project
GET    /projects/{id}/activity?actor_type=AI_AGENT   Filter by actor type
GET    /projects/{id}/activity?actor_id={uuid}       Filter by specific user
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

## Real-Time Engine (SSE + Redis Pub/Sub)

### Overview

TaskFlow delivers live task lifecycle events via **Server-Sent Events (SSE)** backed by **Redis Pub/Sub** — enabling real-time fanout across multiple API instances. Any server that receives a task mutation publishes to a shared Redis channel; all instances receive the message and fan out to their local SSE connections.

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
Task mutation in TaskService
        │
        ├── RedisEventPublisher.publish(projectId, eventName, data)
        │         │
        │         ▼
        │   Redis channel: taskflow:task-events
        │         │
        │         ▼  (all API instances subscribed)
        │   RedisEventSubscriber.onMessage()
        │         │
        │         ▼
        │   SseService.send(projectId, eventName, data)
        │         │
        │         ▼
        │   All local SSE emitters for projectId notified
        │
        └── OutboxService.publishTaskEvent() [within same transaction]
```

### Why Redis Pub/Sub for SSE fanout?

The previous implementation stored SSE connections in a `ConcurrentHashMap` — correct for a single instance but broken under horizontal scale. When a mutation hits Instance 2, Instance 1's connected clients would never receive the event.

Redis Pub/Sub solves this: every instance subscribes to the same channel. Any instance receiving a mutation publishes once; all instances fan out to their local connections.

**Why not Kafka or RabbitMQ?**

SSE notifications are best-effort — a missed notification is acceptable (client can refresh). Redis Pub/Sub is the right complexity for this use case. Kafka and RabbitMQ add significant operational overhead for a problem that doesn't require guaranteed delivery on the SSE path.

**Note:** SSE fanout is intentionally fire-and-forget. Audit logging uses the Transactional Outbox pattern for guaranteed delivery — these are separate concerns with different reliability requirements.

---

## Audit Logging (Transactional Outbox)

### Overview

Every task mutation is captured in an **append-only audit log** (`task_activity_logs`) with full field-level change detection, actor attribution, and old/new value snapshots. Writes are guaranteed via the **Transactional Outbox Pattern** — zero message loss even under consumer failures.

### Why Transactional Outbox instead of synchronous audit writes?

Two naive approaches and why they fail:

| Approach | Problem |
|---|---|
| Write audit log in same transaction | Overloads the write path, increases latency, couples audit storage to main DB performance |
| Write to Redis Pub/Sub then consume | Redis Pub/Sub is fire-and-forget — consumer downtime = permanent audit gaps |

The Transactional Outbox solves both:

```
BEGIN TRANSACTION
  ├── UPDATE tasks SET ...          ← main write
  └── INSERT INTO outbox (payload)  ← atomic guarantee
COMMIT

OutboxWorker (@Scheduled every 1s):
  ├── SELECT unprocessed outbox entries
  ├── INSERT into task_activity_logs
  └── Mark outbox entry processed
      (on failure: log + skip → retry next tick)
```

The task save and outbox insert are in the **same database transaction** — both commit or both roll back. The worker drains asynchronously, keeping the request path fast while guaranteeing no audit entry is ever lost.

### What gets captured

| Field | Description |
|---|---|
| `actor_type` | `USER`, `AI_AGENT`, or `SYSTEM` |
| `action_type` | `CREATED`, `UPDATED`, `DELETED`, `STATUS_CHANGED`, `ASSIGNED` |
| `old_value` | Full task snapshot before mutation (JSONB) |
| `new_value` | Full task snapshot after mutation (JSONB) |
| `changed_fields` | Array of field names that actually changed |
| `conversation_id` | Phase 2: groups all mutations from one AI agent session |
| `metadata` | Phase 2: agent tool call context, reasoning trace |

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
- **Failure is explicit** — the 409 response surfaces the conflict to the client cleanly, enabling retry logic

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
6. `V6__create_outbox.sql` — Transactional outbox table for async audit delivery
7. `V7__create_task_activity_logs.sql` — Append-only audit log with field-level change tracking

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
| **Tasks** | Created, updated, deleted — with changed fields |
| **SSE** | Subscription opened, event dispatched, emitter closed |
| **Redis** | Publish failures, subscriber errors |
| **Outbox Worker** | Processing errors, retry attempts |

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
| Redis | `localhost:6379` |

### Environment Variables

```env
DB_USERNAME=taskflow
DB_PASSWORD=taskflow
DB_NAME=taskflow

JWT_SECRET=taskflow-super-secret-key-for-jwt-signing-123456
JWT_EXPIRATION=86400000

API_PORT=8080

REDIS_HOST=localhost
REDIS_PORT=6379
```

> In a real production deployment, secrets would be injected via a secrets manager (AWS SSM, HashiCorp Vault) — never committed to version control.

---

## Testing

### Integration Tests (Maven)

```bash
cd backend
./mvnw test
```

| Metric | Value |
|---|---|
| Instruction coverage | **83%** |
| Branch coverage | **67%** |
| Integration tests passing | **13 / 13** |

### End-to-End Shell Suite

```bash
chmod +x test_taskflow.sh
./test_taskflow.sh
```

### Postman Collection

A reusable Postman collection is available in `postman/TaskFlow.postman_collection.json` with a corresponding environment file at `postman/TaskFlow.postman_environment.json`.

### Postman Testing Screenshots

Screenshots demonstrating Postman API testing are available in the `screenshot_testing/` folder.

### Testing Philosophy

Integration tests are run against a real embedded database — not mocked repositories. This validates the full stack (controller → service → repository → DB) including constraint enforcement, cascade behaviour, and migration correctness. Mocking the DB layer provides false confidence; integration tests catch the bugs that matter.

---

## Engineering Tradeoffs & Future Roadmap

### Phase 1 — Completed

| Decision | Rationale |
|---|---|
| Redis Pub/Sub for SSE fanout | Distributed real-time across multiple instances; fire-and-forget is acceptable for notifications |
| Transactional Outbox over sync audit writes | Decouples audit persistence from the write path; guarantees delivery without blocking the request |
| Outbox worker polls every 1s with `fixedDelay` | `fixedDelay` prevents worker overlap; 1s lag is acceptable for audit use cases |
| `task_id` without FK in audit logs | Audit records must outlive the task — history persists even after deletion |
| Same PostgreSQL for audit logs | Enables atomic outbox+task writes; pgvector in Phase 3 extends it naturally; Phase 4 ETL pipes it to Redshift for analytics |

### Phase 2 Roadmap — AI Agent + MCP Server

A Claude-powered agent that drives the REST backend through natural language, with every action attributed in the audit log under `actor_type=AI_AGENT`. Exposed as an MCP server for ecosystem interoperability (Claude Desktop, Cursor, any AI client).

### Phase 3 Roadmap — pgvector + Semantic Search

pgvector extends PostgreSQL for semantic task search (embedding-based, no keyword matching) and cross-session agent memory via similarity search.

### Phase 4 Roadmap — AWS Deployment + Analytical Data Layer

ECS Fargate + RDS + ElastiCache + Redis on AWS via Terraform, GitHub Actions CI/CD with zero-downtime rolling deploys, and a Redshift + AWS Glue ETL pipeline pulling audit logs for analytical queries.

---

## Highlights

```
✔  JWT stateless auth — horizontally scalable, session-free
✔  BCrypt password hashing (strength 12)
✔  Clean layered architecture — no cross-layer leakage
✔  Project + task CRUD with ownership enforcement
✔  Redis Pub/Sub distributed SSE — real-time fanout across multiple API instances
✔  Transactional Outbox Pattern — guaranteed async audit delivery, zero message loss
✔  Append-only audit log with field-level change detection and actor attribution
✔  Audit history endpoints — queryable by task, project, actor type, actor ID
✔  Optimistic locking with 409 Conflict on concurrent writes
✔  Paginated, filterable list APIs — production-safe by design
✔  Project analytics endpoint (by status, by assignee)
✔  Flyway versioned migrations with rollback support (V1–V7)
✔  Docker Compose one-command local setup (Postgres + Redis + API)
✔  Structured SLF4J logging throughout service layer
✔  Global exception handler with consistent error envelope
✔  83% instruction coverage · 13 integration tests passing
```

---

## Author

**Sreejit Chaudhury**

Engineered with focus on production readiness, clean architecture, distributed systems thinking, and operational observability.
