## Project Management Design

TaskFlow uses a **project-centric collaboration model** where each project acts as the top-level container for tasks, ownership, and access control.

### How it works

#### 1. Project Creation

* Users create projects via `POST /projects`.
* The authenticated user is automatically set as the **project owner**.
* Each project stores:

  * `id`
  * `name`
  * `description`
  * `ownerId`
  * `createdAt`

This ensures:

```text id="pm1"
every project has a clear ownership model
```

---

#### 2. Project Listing

* `GET /projects` returns:

  * projects owned by the current user
  * projects where the user is assigned tasks

This improves usability by allowing users to see:

```text id="pm2"
both projects they own and projects they actively contribute to
```

To keep this efficient:

* task project IDs are fetched in batch
* projects are queried using pagination
* tasks are aggregated without N+1 query issues

---

#### 3. Project Details

* `GET /projects/{id}` returns:

  * project metadata
  * all tasks belonging to the project

This provides:

```text id="pm3"
single-call project detail view for clients
```

---

#### 4. Project Updates

* `PATCH /projects/{id}`
* Only the **project owner** can:

  * rename project
  * update description

---

#### 5. Project Deletion

* `DELETE /projects/{id}`
* Only the **project owner** can delete.

Deletion is cascaded:

* all associated tasks are automatically deleted

This keeps:

```text id="pm4"
referential integrity and cleanup simple
```

---

## Why this approach

### Chosen because:

* simple and intuitive ownership model
* aligns with common SaaS collaboration tools
* easy to reason about permissions
* avoids over-engineering for take-home scope

---

## Architecture Decisions

### 1. UUID-based relational design

Used explicit foreign keys (`project_id`, `owner_id`) instead of heavy ORM relationships.

Why:

* better query control
* avoids lazy loading pitfalls
* prevents serialization recursion issues

---

### 2. DTO aggregation for project details

Instead of exposing entities directly:

* project detail responses embed task lists via DTOs

Why:

* cleaner API contracts
* flexible response shaping
* avoids leaking internal model details

---

### 3. Batched task lookup

For project listing:

* tasks are fetched using batched `IN` queries
* grouped in memory

Why:

```text id="pm5"
prevents N+1 database query problems
```

---

## Tradeoffs / Limitations

### Current tradeoffs

* **Single owner model only**
  No project members / collaborators yet.

* **No role hierarchy**
  Cannot distinguish editor vs viewer.

* **No soft delete**
  Project deletion is permanent.

* **No activity history**
  Changes are not audited.

* **Task ordering is basic**
  No drag-and-drop / kanban support.

---

## Better options / future improvements

### 1. Project Members / Collaboration

Add:

* team members
* invite flow
* shared projects

### 2. Role-Based Access

Support:

* owner
* editor
* viewer

### 3. Audit Trail

Track:

* task status history
* project changes

### 4. Soft Delete / Restore

Safer project recovery

### 5. Notifications

* assignment alerts
* due date reminders

### 6. Search / Labels / Tags

Better task discovery

---

