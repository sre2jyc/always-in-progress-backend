# 🚀 TaskFlow Backend

A production-grade backend system for managing **Projects and Tasks** with authentication, authorization, pagination, and real-time extensibility.

---

# 🧠 Overview

TaskFlow is a RESTful backend built using:

* **Java + Spring Boot**
* **PostgreSQL (Dockerized)**
* **JWT Authentication**
* **Flyway Migrations**

It supports:

```text
✔ User Authentication (JWT)
✔ Project Management
✔ Task Management
✔ Filtering + Pagination
✔ Aggregation (Stats API)
✔ Proper Error Handling
✔ Secure ENV-based configuration
```

---

# 🏗️ Architecture

```text
Controller → Service → Repository → Database
```

* **Controller** → Handles HTTP layer
* **Service** → Business logic + authorization
* **Repository** → Database interaction (JPA)
* **Security** → JWT-based stateless authentication

---

# 🔐 Authentication

| Method | Endpoint       | Description               |
| ------ | -------------- | ------------------------- |
| POST   | /auth/register | Register new user         |
| POST   | /auth/login    | Login → returns JWT token |

---

## 🔑 JWT Details

* Algorithm: HS256
* Expiry: 24 hours
* Claims:

  * `userId`
  * `email`

---

# 📦 Project APIs

| Method | Endpoint             | Description                    |
| ------ | -------------------- | ------------------------------ |
| GET    | /projects            | List user projects (paginated) |
| POST   | /projects            | Create project                 |
| GET    | /projects/{id}       | Get project                    |
| PATCH  | /projects/{id}       | Update project (owner only)    |
| DELETE | /projects/{id}       | Delete project (owner only)    |
| GET    | /projects/{id}/stats | Task stats (status + assignee) |

---

# 📝 Task APIs

| Method | Endpoint             | Description                       |
| ------ | -------------------- | --------------------------------- |
| GET    | /projects/{id}/tasks | List tasks (filters + pagination) |
| POST   | /projects/{id}/tasks | Create task                       |
| PATCH  | /tasks/{id}          | Update task                       |
| DELETE | /tasks/{id}          | Delete task                       |

---

# 🔍 Query Parameters

### Pagination

```text
?page=0&limit=10
```

---

### Task Filters

```text
?status=TODO
?assignee=<UUID>
```

---

# 📊 Stats API Example

```json
{
  "statusCounts": {
    "TODO": 3,
    "DONE": 2
  },
  "assigneeCounts": {
    "userId1": 2,
    "UNASSIGNED": 1
  }
}
```

---

# ⚠️ Error Handling

| Case             | Status | Response                                            |
| ---------------- | ------ | --------------------------------------------------- |
| Validation Error | 400    | `{ "error": "validation failed", "fields": {...} }` |
| Unauthenticated  | 401    | `{ "error": "unauthorized" }`                       |
| Forbidden        | 403    | `{ "error": "forbidden" }`                          |
| Not Found        | 404    | `{ "error": "not found" }`                          |

---

# 🔧 Environment Variables

Create a `.env` file:

```env
DB_URL=jdbc:postgresql://localhost:5433/taskflow
DB_USERNAME=taskflow
DB_PASSWORD=taskflow

JWT_SECRET=taskflow-super-secret-key-for-jwt-signing-123456
JWT_EXPIRATION=86400000
```

---

# 🐳 Running with Docker

```bash
cp .env.example .env
docker compose up -d
```

This starts:

* PostgreSQL on `localhost:5433`
* TaskFlow API on `http://localhost:8080`

Flyway runs automatically when the API starts. The seed migration creates test data for immediate login and API testing.

Test credentials:

```text
Email:    test@example.com
Password: password123
```

---

# ▶️ Run Application

```bash
export DB_URL=jdbc:postgresql://localhost:5433/taskflow
export DB_USERNAME=taskflow
export DB_PASSWORD=taskflow
export JWT_SECRET=taskflow-super-secret-key-for-jwt-signing-123456
export JWT_EXPIRATION=86400000

./mvnw spring-boot:run
```

---

# 🧪 Sample Flow

```bash
# Register
POST /auth/register

# Login → get token
POST /auth/login

# Use token
Authorization: Bearer <token>

# Create project
POST /projects

# Create task
POST /projects/{id}/tasks
```

Seeded project/task data is also available after running migrations:

```text
Seed user:      test@example.com / password123
Seed project:   Seed Project
Seed tasks:     TODO, IN_PROGRESS, DONE
```

---

# 🧪 Tests and Coverage

The backend includes integration tests that run through the real Spring Boot HTTP layer using `MockMvc`. Tests start the application context, apply Flyway migrations against an in-memory H2 database in PostgreSQL compatibility mode, and exercise controllers, services, repositories, validation, and JWT security together.

The integration suite covers:

* Registration, login, JWT generation, and password hashing
* Missing-token authentication failure (`401`)
* Validation errors with structured field responses (`400`)
* Duplicate registration and invalid login
* Forbidden project updates (`403`)
* Not-found and invalid-parameter responses
* Task creation with invalid assignee handling
* Project/task filtering, pagination, and stats
* Task update/delete and project delete cascade behavior

Run all tests:

```bash
./mvnw test
```

Run only the integration test suite:

```bash
./mvnw test -Dtest=TaskFlowIntegrationTests
```

JaCoCo coverage is generated during the Maven test phase.

```bash
./mvnw test
open target/site/jacoco/index.html
```

Current coverage from the latest local run:

```text
Instruction coverage: 83%
Branch coverage:      67%
Classes covered:      17 / 17
Integration tests:    13 passing
```

---

# 🧠 Design Decisions

* **Stateless Auth (JWT)** → scalable, no session storage
* **Layered Architecture** → clean separation of concerns
* **Global Exception Handling** → consistent API responses
* **Pagination** → scalable APIs
* **Aggregation Queries** → efficient stats computation
* **ENV Config** → secure and flexible deployment

---

# 🚀 Future Improvements

```text
✔ Project Members (multi-user collaboration)
✔ SSE (real-time updates)
✔ Optimistic Locking (concurrency control)
✔ Email notifications
✔ Role-based access control (RBAC)
```

---

# 💥 Highlights

```text
✔ Production-ready backend
✔ Secure (no hardcoded secrets)
✔ Clean architecture
✔ Scalable APIs
✔ Strong error handling
```

---

# 🙌 Author

Built as part of a backend engineering assignment with focus on:

* system design
* clean code
* production practices
