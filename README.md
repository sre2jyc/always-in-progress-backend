# 🚀 TaskFlow — Production-Ready Real-Time Task Management Backend

> **A robust, scalable backend system for collaborative project and task management**
> Built with **Spring Boot**, **PostgreSQL**, **Docker**, **Flyway**, and **Server-Sent Events (SSE)**.
> Designed with production-grade practices: clean architecture, secure auth, migrations, optimistic locking, and structured logging.  

---

# 📌 Overview

TaskFlow is a production-grade RESTful backend for managing:

* **Users**
* **Projects**
* **Tasks**
* **Task assignments**
* **Real-time task updates**

It was built as part of an engineering take-home assignment with focus on:

```text id="rm1"
✔ clean architecture
✔ secure authentication
✔ scalable APIs
✔ Docker-first setup
✔ database migrations
✔ production engineering best practices
```

---

# 🏗️ Tech Stack

## Backend

* **Java 21**
* **Spring Boot 3**
* **Spring Security**
* **Spring Data JPA / Hibernate**
* **Flyway migrations**

## Database

* **PostgreSQL 15**

## Infrastructure

* **Docker**
* **Docker Compose**

## Security

* **JWT Authentication**
* **BCrypt Password Hashing**

## Real-Time

* **Server-Sent Events (SSE)**

## Utilities

* **SLF4J / Logback structured logging**
* **Lombok**
* **Maven**

---

# 🧠 Architecture

TaskFlow follows a clean layered architecture:

```text id="rm2"
Controller → Service → Repository → Database
```

### Layer Responsibilities

### Controller

* Handles HTTP requests / responses
* Request validation
* DTO mapping

### Service

* Business logic
* Authorization checks
* Real-time event publishing

### Repository

* JPA queries
* pagination
* aggregations

### Security

* JWT filter
* stateless auth

### Why this structure?

* maintainable
* testable
* avoids god classes
* clean separation of concerns

---

# 🔐 Authentication Design

TaskFlow uses **JWT-based stateless authentication** with **Spring Security**, **BCrypt**, and a custom JWT filter.

---

## Authentication Flow

### 1. Register

```text id="rm3"
POST /auth/register
```

* User registers with:

  * name
  * email
  * password
* Password is hashed using **BCrypt (strength 12)** before saving.

---

### 2. Login

```text id="rm4"
POST /auth/login
```

* Email + password validated
* JWT token returned

---

### JWT Details

* Algorithm: **HS256**
* Expiry: **24 hours**
* Claims:

  * `userId`
  * `email`

---

### 3. Protected APIs

All non-auth APIs require:

```text id="rm5"
Authorization: Bearer <token>
```

JWT filter:

* extracts token
* validates signature + expiry
* loads userId into Spring Security context

---

## Why JWT?

Chosen because:

* stateless → horizontally scalable
* simple for REST APIs
* clean for Docker deployments

---

## Tradeoffs

* no refresh token flow
* no token revocation yet
* no MFA

---

## Future Improvements

* refresh tokens
* token blacklist
* OAuth / SSO
* MFA

---

# 📁 Project Management Design

TaskFlow uses a **project-centric ownership model**.

---

## Features

### Supported APIs

| Method | Endpoint               | Description                    |
| ------ | ---------------------- | ------------------------------ |
| GET    | `/projects`            | List owned / assigned projects |
| POST   | `/projects`            | Create project                 |
| GET    | `/projects/{id}`       | Project details + tasks        |
| PATCH  | `/projects/{id}`       | Update project                 |
| DELETE | `/projects/{id}`       | Delete project                 |
| GET    | `/projects/{id}/stats` | Project analytics              |

---

## Project Flow

### Create Project

* authenticated user becomes owner

### List Projects

Returns:

* owned projects
* projects where user is task assignee

Implemented efficiently using:

* batched task lookup
* pagination

### Get Project

Returns:

* project metadata
* embedded tasks

### Update / Delete

Restricted to:

* owner only

Cascade delete ensures:

```text id="rm6"
project deletion also deletes all tasks
```

---

## Design Decisions

* UUID-based FK design
* DTO aggregation for project + tasks
* no heavy ORM relations

---

## Tradeoffs

* single owner only
* no project members yet
* no soft delete

---

## Future Improvements

* project collaboration
* RBAC
* audit logs

---

# 📝 Task Management Design

TaskFlow uses a **project-scoped task model**.

---

## Supported APIs

| Method | Endpoint               | Description |
| ------ | ---------------------- | ----------- |
| GET    | `/projects/{id}/tasks` | List tasks  |
| POST   | `/projects/{id}/tasks` | Create task |
| PATCH  | `/tasks/{id}`          | Update task |
| DELETE | `/tasks/{id}`          | Delete task |

---

## Task Features

### Create Task

Supports:

* title
* description
* status
* priority
* assignee
* due date

Validations:

* project must exist
* assignee must exist

---

### List Tasks

Supports:

* pagination
* filters

Examples:

```text id="rm7"
?page=0&limit=10
?status=TODO
?assignee=<uuid>
```

---

### Update Task

Supports PATCH semantics:

* only provided fields update

---

### Delete Task

Allowed for:

* project owner
* task creator

---

## Why this design?

* intuitive project scoping
* flexible updates
* scalable listing

---

## Tradeoffs

* no labels
* no comments
* no attachments

---

## Future Improvements

* subtasks
* labels
* file uploads

---

# ⚡ Additional Features

---

# 🔄 Real-Time Updates (SSE)

TaskFlow supports live task updates.

---

## Endpoint

```text id="rm8"
GET /projects/{id}/events
```

---

## Events

* TASK_CREATED
* TASK_UPDATED
* TASK_DELETED

---

## Why SSE?

* lightweight
* browser-friendly
* simpler than WebSockets

---

## Tradeoffs

* in-memory emitters → single instance only

---

## Better future options

* Redis Pub/Sub
* WebSockets

---

# 📊 Project Stats API

Endpoint:

```text id="rm9"
GET /projects/{id}/stats
```

Returns:

* count by status
* count by assignee

---

# 📄 Pagination / Filtering

Supported across:

* project listing
* task listing

Benefits:

* scalable APIs
* better UX

---

# 🔄 Concurrency Handling

Implemented optimistic locking using:

```text id="rm10"
@Version
```

Task stores:

* version
* updatedAt

Benefits:

* prevents lost updates
* safe concurrent edits

Conflict returns:

```text id="rm11"
409 Conflict
```

---

# 🧱 Structured Logging

Implemented structured logging for:

* auth events
* project lifecycle
* task lifecycle
* SSE subscriptions

Why:

* easier debugging
* operational visibility

---

# 🗄️ Database & Migrations

Database schema is managed using **Flyway**.

---

## Included migrations

* users table
* projects table
* tasks table
* seed data
* version / updatedAt support

---

## Rollback support

Every migration includes:

* Up migration (`V*.sql`)
* Down migration (`U*.sql`)

---

# 🌱 Seed Data

Included for local testing.

---

## Seed users

```text id="rm12"
Email: test@example.com
Password: password123
```

```text id="rm13"
Email: assignee@example.com
Password: password123
```

---

## Seed project

* Seed Project

## Seed tasks

* TODO
* IN_PROGRESS
* DONE

---

# ⚠️ Error Handling

Consistent API responses via global exception handling.

| Case              | Status |
| ----------------- | ------ |
| Validation failed | 400    |
| Unauthorized      | 401    |
| Forbidden         | 403    |
| Not found         | 404    |
| Conflict          | 409    |

Example:

```json id="rm14"
{
  "error": "validation failed",
  "fields": {
    "email": "is required"
  }
}
```

---

# 🐳 Running Locally

---

## Prerequisites

* Docker
* Docker Compose

---

## Setup

```bash id="rm15"
git clone https://github.com/<your-username>/taskflow-sreejit-chaudhury.git
cd taskflow-sreejit-chaudhury

cp .env.example .env

docker compose up --build
```

---

## Services

### API:

```text id="rm16"
http://localhost:8081
```

### PostgreSQL:

```text id="rm17"
localhost:5433
```

---

# 🔧 Environment Variables

```env id="rm18"
DB_USERNAME=taskflow
DB_PASSWORD=taskflow
DB_NAME=taskflow

JWT_SECRET=taskflow-super-secret-key-for-jwt-signing-123456
JWT_EXPIRATION=86400000

API_PORT=8081
```

---

# 🗃️ Running Migrations

Flyway runs automatically on startup.

No manual migration step required.

---

# 🧪 Testing

TaskFlow includes:

* integration tests
* end-to-end shell script suite

---

## Run tests

### Maven Integration tests

```bash id="rm19"
cd backend
./mvnw test
```

---

### E2E Script

```bash id="rm20"
chmod +x scripts/test_taskflow.sh
./scripts/test_taskflow.sh
```

---

## Coverage

Current local coverage:

```text id="rm21"
Instruction coverage: 83%
Branch coverage: 67%
13 integration tests passing
```

---

# 📚 API Reference

---

## Auth

```text id="rm22"
POST /auth/register
POST /auth/login
```

---

## Projects

```text id="rm23"
GET    /projects
POST   /projects
GET    /projects/{id}
PATCH  /projects/{id}
DELETE /projects/{id}
GET    /projects/{id}/stats
GET    /projects/{id}/events
```

---

## Tasks

```text id="rm24"
GET    /projects/{id}/tasks?page=&limit=&status=&assignee=
POST   /projects/{id}/tasks
PATCH  /tasks/{id}
DELETE /tasks/{id}
```

---

# 🚀 What I’d Improve With More Time

---

## Planned Enhancements

### Collaboration / RBAC

* project members
* owner / editor / viewer roles

### Notifications

* due date alerts
* email reminders

### Audit Trail

* status history
* activity logs

### Richer workflows

* subtasks
* labels
* comments

### Scalable realtime

* Redis + WebSockets

### Observability

* metrics
* tracing
* dashboards

---

# 💥 Highlights

```text id="rm25"
✔ JWT-based stateless auth
✔ secure BCrypt password hashing
✔ project / task CRUD
✔ pagination + filters
✔ project analytics
✔ optimistic locking
✔ SSE real-time updates
✔ Docker-first setup
✔ Flyway migrations
✔ integration tests
✔ structured logging
```

---

# 👨‍💻 Author

**Sreejit Chaudhury**

Built with focus on:

* production readiness
* clean code
* developer experience
* scalable system design
