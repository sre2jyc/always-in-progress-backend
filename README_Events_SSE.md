## ⚡ Real-Time Updates with SSE

To improve collaboration and make task changes visible instantly, TaskFlow supports **Server-Sent Events (SSE)** for live project updates.

Unlike traditional polling (where clients repeatedly ask the server for updates), SSE allows the backend to **push events to connected clients in real time** whenever something changes.

---

### Why SSE was added

Task management is inherently collaborative:

* project owners need visibility into task progress
* assignees need instant awareness of status changes
* dashboards should reflect live project state

Without SSE:

```text id="sse1"
clients would need to keep polling the server repeatedly,
causing unnecessary load and delayed updates
```

With SSE:

```text id="sse2"
the server pushes updates immediately after task operations succeed
```

This creates:

* better UX
* lower latency
* reduced network overhead

---

## How SSE Works in TaskFlow

TaskFlow implements **project-scoped event streams**.

This means:

```text id="sse3"
clients subscribe only to events for a specific project
```

So users only receive updates relevant to the project they are currently viewing.

---

### 1. Client subscribes to event stream

Frontend / client opens a long-lived HTTP connection:

```text id="sse4"
GET /projects/{projectId}/events
```

Example:

```bash id="sse5"
curl -N http://localhost:8081/projects/<projectId>/events \
-H "Authorization: Bearer <token>"
```

What happens:

* request passes JWT auth
* backend validates user access
* backend creates an `SseEmitter`
* emitter is stored in memory for that project

At this point:

```text id="sse6"
connection stays open and listens for future updates
```

---

### 2. Task APIs trigger events

Whenever these APIs succeed:

```text id="sse7"
POST   /projects/{id}/tasks
PATCH  /tasks/{id}
DELETE /tasks/{id}
```

the backend:

* completes DB transaction first
* saves / updates / deletes task
* publishes event to all subscribers of that project

---

### 3. Backend broadcasts to all subscribers

TaskFlow maintains:

```text id="sse8"
Map<projectId, List<SseEmitter>>
```

Flow:

* find all emitters for project
* send event payload
* remove dead / disconnected emitters

Example events:

```text id="sse9"
event: TASK_CREATED
data: { taskId: "...", title: "Build API" }

event: TASK_UPDATED
data: { taskId: "...", status: "DONE" }

event: TASK_DELETED
data: { taskId: "..." }
```

---

### 4. Client updates UI instantly

Frontend can:

* append new task
* update task status
* remove deleted task

without:

```text id="sse10"
manual refresh or polling
```

---

## Why SSE was chosen over WebSockets

For this assignment, SSE was the right tradeoff.

---

### Benefits of SSE here

### 1. Simpler implementation

* native HTTP support
* easy Spring Boot support via `SseEmitter`

### 2. Perfect for one-way events

TaskFlow only needs:

```text id="sse11"
server → client updates
```

No chat / bidirectional messaging needed.

### 3. Browser friendly

* supported natively by EventSource API

### 4. Lightweight

* lower operational complexity

---

## Tradeoffs / Limitations

Current implementation is intentionally simple:

---

### 1. In-memory subscribers

Current:

```text id="sse12"
Map<projectId, emitters>
```

Limitation:

* works only in single instance deployment
* does not scale horizontally

---

### 2. No persistent event replay

If client disconnects:

* missed events are lost

---

### 3. Basic reconnect support

SSE browser auto reconnect exists,
but advanced recovery is not implemented.

---

## How this can be extended in production

---

### 1. Redis Pub/Sub / Message Broker

For multi-instance deployment:

```text id="sse13"
API node A publishes → Redis → all nodes broadcast
```

Benefits:

* horizontal scalability
* shared event bus

---

### 2. WebSockets

For richer collaboration:

* comments
* typing indicators
* presence

---

### 3. Event persistence / replay

Store:

* event logs
* last event ID

So reconnecting clients can:

```text id="sse14"
catch up on missed changes
```

---

### 4. Fine-grained subscriptions

Future:

* per task subscription
* user-specific notifications
* due date alerts

---

## Engineering value added

SSE was added intentionally to go beyond baseline CRUD and demonstrate:

```text id="sse15"
✔ real-time system thinking
✔ event-driven architecture concepts
✔ better UX design
✔ production extensibility awareness
```

---

