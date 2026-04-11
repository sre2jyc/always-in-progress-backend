maverick00@air taskflow-sreejit-chaudhury % ./test_taskflow.sh
========================================
0. Unauthorized check (should fail)
========================================
HTTP/1.1 401 
X-Content-Type-Options: nosniff
X-XSS-Protection: 0
Cache-Control: no-cache, no-store, max-age=0, must-revalidate
Pragma: no-cache
Expires: 0
X-Frame-Options: DENY
Content-Type: application/json;charset=ISO-8859-1
Content-Length: 30
Date: Sat, 11 Apr 2026 11:45:08 GMT

{
  "error": "unauthorized"
}


========================================
1. Register User
========================================
{"error":"user already exists"}

========================================
2. Login User
========================================
{"token":"eyJhbGciOiJIUzI1NiJ9.eyJ1c2VySWQiOiIxMTExMTExMS0xMTExLTExMTEtMTExMS0xMTExMTExMTExMTEiLCJlbWFpbCI6InRlc3RAZXhhbXBsZS5jb20iLCJzdWIiOiJ0ZXN0QGV4YW1wbGUuY29tIiwiaWF0IjoxNzc1OTA3OTA5LCJleHAiOjE3NzU5OTQzMDl9.OqqTmYgOFezBzt27mArGsgbYWxrU0jXz12SlPEFfplA"}

TOKEN:
eyJhbGciOiJIUzI1NiJ9.eyJ1c2VySWQiOiIxMTExMTExMS0xMTExLTExMTEtMTExMS0xMTExMTExMTExMTEiLCJlbWFpbCI6InRlc3RAZXhhbXBsZS5jb20iLCJzdWIiOiJ0ZXN0QGV4YW1wbGUuY29tIiwiaWF0IjoxNzc1OTA3OTA5LCJleHAiOjE3NzU5OTQzMDl9.OqqTmYgOFezBzt27mArGsgbYWxrU0jXz12SlPEFfplA


========================================
3. Create Project
========================================
{"id":"1d4b8d9a-8898-4d27-be8c-7c32d36517d9","name":"Project Alpha","description":"Testing project","ownerId":"11111111-1111-1111-1111-111111111111","createdAt":"2026-04-11T11:45:09.145147844"}

PROJECT_ID:
1d4b8d9a-8898-4d27-be8c-7c32d36517d9


========================================
4. List Projects (with tasks)
========================================
[{"id":"33333333-3333-3333-3333-333333333333","name":"Seed Project","description":"Project created by the seed migration for local testing.","ownerId":"11111111-1111-1111-1111-111111111111","createdAt":"2026-04-11T10:26:47.038838","tasks":[{"id":"44444444-4444-4444-4444-444444444444","title":"Review product brief","description":"Seed task in TODO status.","status":"TODO","priority":"HIGH","assigneeId":"11111111-1111-1111-1111-111111111111","projectId":"33333333-3333-3333-3333-333333333333","createdBy":"11111111-1111-1111-1111-111111111111","dueDate":"2026-04-15T09:00:00","createdAt":"2026-04-11T10:26:47.038838","updatedAt":"2026-04-11T10:26:47.038838","version":0},{"id":"55555555-5555-5555-5555-555555555555","title":"Build task API","description":"Seed task in IN_PROGRESS status.","status":"IN_PROGRESS","priority":"MEDIUM","assigneeId":"22222222-2222-2222-2222-222222222222","projectId":"33333333-3333-3333-3333-333333333333","createdBy":"11111111-1111-1111-1111-111111111111","dueDate":"2026-04-17T09:00:00","createdAt":"2026-04-11T10:26:47.038838","updatedAt":"2026-04-11T10:26:47.038838","version":0},{"id":"66666666-6666-6666-6666-666666666666","title":"Document API examples","description":"Seed task in DONE status.","status":"DONE","priority":"LOW","assigneeId":null,"projectId":"33333333-3333-3333-3333-333333333333","createdBy":"11111111-1111-1111-1111-111111111111","dueDate":"2026-04-13T09:00:00","createdAt":"2026-04-11T10:26:47.038838","updatedAt":"2026-04-11T10:26:47.038838","version":0}]},{"id":"1d4b8d9a-8898-4d27-be8c-7c32d36517d9","name":"Project Alpha","description":"Testing project","ownerId":"11111111-1111-1111-1111-111111111111","createdAt":"2026-04-11T11:45:09.145148","tasks":[]}]

========================================
5. Get Single Project (with tasks)
========================================
{"id":"1d4b8d9a-8898-4d27-be8c-7c32d36517d9","name":"Project Alpha","description":"Testing project","ownerId":"11111111-1111-1111-1111-111111111111","createdAt":"2026-04-11T11:45:09.145148","tasks":[]}

========================================
6. Update Project
========================================
{"id":"1d4b8d9a-8898-4d27-be8c-7c32d36517d9","name":"Project Alpha Updated","description":"Updated description","ownerId":"11111111-1111-1111-1111-111111111111","createdAt":"2026-04-11T11:45:09.145148"}

========================================
7. SSE Setup Instructions
========================================
👉 Open another terminal and run:
curl -N "http://localhost:8080/projects/1d4b8d9a-8898-4d27-be8c-7c32d36517d9/events" -H "Authorization: Bearer eyJhbGciOiJIUzI1NiJ9.eyJ1c2VySWQiOiIxMTExMTExMS0xMTExLTExMTEtMTExMS0xMTExMTExMTExMTEiLCJlbWFpbCI6InRlc3RAZXhhbXBsZS5jb20iLCJzdWIiOiJ0ZXN0QGV4YW1wbGUuY29tIiwiaWF0IjoxNzc1OTA3OTA5LCJleHAiOjE3NzU5OTQzMDl9.OqqTmYgOFezBzt27mArGsgbYWxrU0jXz12SlPEFfplA"


========================================
8. Create Task #1
========================================
{"id":"6f4a74af-4be7-403e-8f48-63d017c01d28","title":"Task 1","description":"First task","status":"TODO","priority":"HIGH","assigneeId":null,"projectId":"1d4b8d9a-8898-4d27-be8c-7c32d36517d9","createdBy":"11111111-1111-1111-1111-111111111111","dueDate":null,"createdAt":"2026-04-11T11:45:09.312039011","updatedAt":"2026-04-11T11:45:09.312052802","version":0}

TASK_ID:
6f4a74af-4be7-403e-8f48-63d017c01d28


========================================
9. Create Task #2
========================================
{"id":"914953ca-13b5-48c5-b5b3-819abe057f6b","title":"Task 2","description":"Second task","status":"IN_PROGRESS","priority":"MEDIUM","assigneeId":null,"projectId":"1d4b8d9a-8898-4d27-be8c-7c32d36517d9","createdBy":"11111111-1111-1111-1111-111111111111","dueDate":null,"createdAt":"2026-04-11T11:45:09.336861469","updatedAt":"2026-04-11T11:45:09.336874677","version":0}

========================================
10. List Tasks (paginated)
========================================
[{"id":"6f4a74af-4be7-403e-8f48-63d017c01d28","title":"Task 1","description":"First task","status":"TODO","priority":"HIGH","assigneeId":null,"projectId":"1d4b8d9a-8898-4d27-be8c-7c32d36517d9","createdBy":"11111111-1111-1111-1111-111111111111","dueDate":null,"createdAt":"2026-04-11T11:45:09.312039","updatedAt":"2026-04-11T11:45:09.312053","version":0},{"id":"914953ca-13b5-48c5-b5b3-819abe057f6b","title":"Task 2","description":"Second task","status":"IN_PROGRESS","priority":"MEDIUM","assigneeId":null,"projectId":"1d4b8d9a-8898-4d27-be8c-7c32d36517d9","createdBy":"11111111-1111-1111-1111-111111111111","dueDate":null,"createdAt":"2026-04-11T11:45:09.336861","updatedAt":"2026-04-11T11:45:09.336875","version":0}]

========================================
11. Filter Tasks by Status
========================================
[{"id":"6f4a74af-4be7-403e-8f48-63d017c01d28","title":"Task 1","description":"First task","status":"TODO","priority":"HIGH","assigneeId":null,"projectId":"1d4b8d9a-8898-4d27-be8c-7c32d36517d9","createdBy":"11111111-1111-1111-1111-111111111111","dueDate":null,"createdAt":"2026-04-11T11:45:09.312039","updatedAt":"2026-04-11T11:45:09.312053","version":0}]

========================================
12. Filter Tasks by Assignee (none expected)
========================================
[]

========================================
13. Update Task
========================================
{"id":"6f4a74af-4be7-403e-8f48-63d017c01d28","title":"Task 1","description":"First task","status":"DONE","priority":"LOW","assigneeId":null,"projectId":"1d4b8d9a-8898-4d27-be8c-7c32d36517d9","createdBy":"11111111-1111-1111-1111-111111111111","dueDate":null,"createdAt":"2026-04-11T11:45:09.312039","updatedAt":"2026-04-11T11:45:09.456383261","version":1}

========================================
14. Get Project Stats
========================================
{"statusCounts":{"IN_PROGRESS":1,"DONE":1},"assigneeCounts":{"UNASSIGNED":2}}

========================================
15. Get Single Project Again (verify tasks embedded)
========================================
{"id":"1d4b8d9a-8898-4d27-be8c-7c32d36517d9","name":"Project Alpha Updated","description":"Updated description","ownerId":"11111111-1111-1111-1111-111111111111","createdAt":"2026-04-11T11:45:09.145148","tasks":[{"id":"914953ca-13b5-48c5-b5b3-819abe057f6b","title":"Task 2","description":"Second task","status":"IN_PROGRESS","priority":"MEDIUM","assigneeId":null,"projectId":"1d4b8d9a-8898-4d27-be8c-7c32d36517d9","createdBy":"11111111-1111-1111-1111-111111111111","dueDate":null,"createdAt":"2026-04-11T11:45:09.336861","updatedAt":"2026-04-11T11:45:09.336875","version":0},{"id":"6f4a74af-4be7-403e-8f48-63d017c01d28","title":"Task 1","description":"First task","status":"DONE","priority":"LOW","assigneeId":null,"projectId":"1d4b8d9a-8898-4d27-be8c-7c32d36517d9","createdBy":"11111111-1111-1111-1111-111111111111","dueDate":null,"createdAt":"2026-04-11T11:45:09.312039","updatedAt":"2026-04-11T11:45:09.456383","version":1}]}

========================================
16. Delete Task
========================================
{"message":"Task deleted successfully"}

========================================
17. Delete Project (cascade tasks)
========================================
Project deleted

========================================
18. Invalid Login Test (should fail)
========================================
{"error":"invalid credentials"}

========================================
19. Invalid UUID Test (should fail)
========================================
{"message":"id must be a valid UUID","error":"invalid parameter"}

========================================
✅ FULL E2E TEST SUITE COMPLETED
========================================
maverick00@air taskflow-sreejit-chaudhury % 