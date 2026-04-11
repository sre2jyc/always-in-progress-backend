## Task Management Design

TaskFlow uses a **project-scoped task management model** designed for clarity, flexibility, and safe concurrent updates.

### How it works

#### 1. Task Creation

* Tasks are created via `POST /projects/{id}/tasks`.
* Every task belongs to a project.
* The authenticated user is stored as the **task creator**.
* Tasks support:

  * title
  * description
  * status (`TODO`, `IN_PROGRESS`, `DONE`)
  * priority (`LOW`, `MEDIUM`, `HIGH`)
  * assignee
  * due date

Before creation:

* project existence is validated
* optional assignee is validated

This ensures:

```text id="task1"
all tasks are tied to a valid project and user context
```

---

#### 2. Task Listing

* `GET /projects/{id}/tasks`

Supports:

* pagination (`page`, `limit`)
* filtering by:

  * status
  * assignee

Examples:

```text id="task2"
/projects/{id}/tasks?page=0&limit=10
/projects/{id}/tasks?status=TODO
/projects/{id}/tasks?assignee=<uuid>
```

This allows:

```text id="task3"
efficient task browsing even for larger projects
```

---

#### 3. Task Updates

* `PATCH /tasks/{id}`

Supports partial updates for:

* title
* description
* status
* priority
* assignee
* due date

Only provided fields are updated.

Why:

```text id="task4"
prevents accidental overwrites and supports PATCH semantics cleanly
```

---

#### 4. Task Deletion

* `DELETE /tasks/{id}`

Allowed for:

* project owner
* task creator

Why:

```text id="task5"
balances ownership control with creator accountability
```

---

## Real-Time Task Updates (SSE)

To improve collaboration:

* task create / update / delete operations emit real-time events

### Endpoint:

```text id="task6"
GET /projects/{id}/events
```

### Events:

* `TASK_CREATED`
* `TASK_UPDATED`
* `TASK_DELETED`

Why SSE:

* lightweight for one-way updates
* simple browser support
* easier than WebSockets for assignment scope

---

## Concurrency Handling

### Optimistic Locking

Implemented using:

```text id="task7"
@Version
```

Each task stores:

* `version`
* `updatedAt`

This prevents:

```text id="task8"
lost updates when multiple users edit the same task simultaneously
```

If concurrent update conflict occurs:

* API returns `409 Conflict`

Why:

* safe concurrent writes
* simple implementation
* production-friendly

---

## Why this approach

### Chosen because:

* project-scoped tasks are intuitive
* PATCH updates are flexible
* pagination and filters improve scalability
* optimistic locking improves data safety
* SSE adds meaningful realtime value

---

## Architecture Decisions

### 1. Explicit foreign key design

Used UUID foreign keys:

* `project_id`
* `assignee_id`
* `created_by`

Why:

* predictable SQL
* avoids ORM complexity

---

### 2. Service-layer authorization

Task permissions handled in service layer.

Why:

```text id="task9"
keeps controller clean and business rules centralized
```

---

### 3. Event-driven UX enhancement

SSE events are triggered after successful DB operations.

Why:

* better user experience
* clear event lifecycle

---

## Tradeoffs / Limitations

### Current tradeoffs

* **No comments / activity thread**
* **No attachments**
* **No task labels / tags**
* **No drag-and-drop ordering**
* **No recurring tasks**
* **No bulk actions**

---

## Better options / future improvements

### 1. Rich collaboration

* comments
* mentions
* file uploads

### 2. Better workflow support

* subtasks
* dependencies
* recurring tasks

### 3. Scalable real-time infra

* WebSockets
* Redis Pub/Sub

### 4. Audit Trail

Track:

* status transitions
* assignee history

### 5. Notifications

* due date reminders
* assignment alerts

---

