#!/bin/bash

# ==============================
# TaskFlow Full E2E Test Script
# Covers:
# - Auth (register/login)
# - Unauthorized access
# - Create / list / get / update / delete project
# - Create / list / filter / update / delete tasks
# - Stats API
# - Pagination
# - SSE realtime events
# ==============================

set -e

BASE_URL="http://localhost:8080"
TEST_EMAIL="test@example.com"
TEST_PASSWORD="password123"

echo "========================================"
echo "0. Unauthorized check (should fail)"
echo "========================================"
curl -i "$BASE_URL/projects" || true
echo -e "\n"

echo "========================================"
echo "1. Register User"
echo "========================================"
curl -s -X POST "$BASE_URL/auth/register" \
  -H "Content-Type: application/json" \
  -d "{
    \"name\": \"Test User\",
    \"email\": \"$TEST_EMAIL\",
    \"password\": \"$TEST_PASSWORD\"
  }"
echo -e "\n"

echo "========================================"
echo "2. Login User"
echo "========================================"
LOGIN_RESPONSE=$(curl -s -X POST "$BASE_URL/auth/login" \
  -H "Content-Type: application/json" \
  -d "{
    \"email\": \"$TEST_EMAIL\",
    \"password\": \"$TEST_PASSWORD\"
  }")

echo "$LOGIN_RESPONSE"

TOKEN=$(echo "$LOGIN_RESPONSE" | sed -E 's/.*"token":"([^"]+)".*/\1/')

if [ -z "$TOKEN" ]; then
  echo "❌ Failed to get token"
  exit 1
fi

echo -e "\nTOKEN:"
echo "$TOKEN"
echo -e "\n"

echo "========================================"
echo "3. Create Project"
echo "========================================"
PROJECT_RESPONSE=$(curl -s -X POST "$BASE_URL/projects" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Project Alpha",
    "description": "Testing project"
  }')

echo "$PROJECT_RESPONSE"

PROJECT_ID=$(echo "$PROJECT_RESPONSE" | sed -E 's/.*"id":"([^"]+)".*/\1/')

if [ -z "$PROJECT_ID" ]; then
  echo "❌ Failed to get project ID"
  exit 1
fi

echo -e "\nPROJECT_ID:"
echo "$PROJECT_ID"
echo -e "\n"

echo "========================================"
echo "4. List Projects (with tasks)"
echo "========================================"
curl -s "$BASE_URL/projects?page=0&limit=10" \
  -H "Authorization: Bearer $TOKEN"
echo -e "\n"

echo "========================================"
echo "5. Get Single Project (with tasks)"
echo "========================================"
curl -s "$BASE_URL/projects/$PROJECT_ID" \
  -H "Authorization: Bearer $TOKEN"
echo -e "\n"

echo "========================================"
echo "6. Update Project"
echo "========================================"
curl -s -X PATCH "$BASE_URL/projects/$PROJECT_ID" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Project Alpha Updated",
    "description": "Updated description"
  }'
echo -e "\n"

echo "========================================"
echo "7. SSE Setup Instructions"
echo "========================================"
echo "👉 Open another terminal and run:"
echo "curl -N \"$BASE_URL/projects/$PROJECT_ID/events\" -H \"Authorization: Bearer $TOKEN\""
echo -e "\n"

echo "========================================"
echo "8. Create Task #1"
echo "========================================"
TASK_RESPONSE=$(curl -s -X POST "$BASE_URL/projects/$PROJECT_ID/tasks" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "title": "Task 1",
    "description": "First task",
    "status": "TODO",
    "priority": "HIGH"
  }')

echo "$TASK_RESPONSE"

TASK_ID=$(echo "$TASK_RESPONSE" | sed -E 's/.*"id":"([^"]+)".*/\1/')

if [ -z "$TASK_ID" ]; then
  echo "❌ Failed to get task ID"
  exit 1
fi

echo -e "\nTASK_ID:"
echo "$TASK_ID"
echo -e "\n"

echo "========================================"
echo "9. Create Task #2"
echo "========================================"
curl -s -X POST "$BASE_URL/projects/$PROJECT_ID/tasks" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "title": "Task 2",
    "description": "Second task",
    "status": "IN_PROGRESS",
    "priority": "MEDIUM"
  }'
echo -e "\n"

echo "========================================"
echo "10. List Tasks (paginated)"
echo "========================================"
curl -s "$BASE_URL/projects/$PROJECT_ID/tasks?page=0&limit=10" \
  -H "Authorization: Bearer $TOKEN"
echo -e "\n"

echo "========================================"
echo "11. Filter Tasks by Status"
echo "========================================"
curl -s "$BASE_URL/projects/$PROJECT_ID/tasks?status=TODO&page=0&limit=10" \
  -H "Authorization: Bearer $TOKEN"
echo -e "\n"

echo "========================================"
echo "12. Filter Tasks by Assignee (none expected)"
echo "========================================"
curl -s "$BASE_URL/projects/$PROJECT_ID/tasks?assignee=$(uuidgen)&page=0&limit=10" \
  -H "Authorization: Bearer $TOKEN"
echo -e "\n"

echo "========================================"
echo "13. Update Task"
echo "========================================"
curl -s -X PATCH "$BASE_URL/tasks/$TASK_ID" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "status": "DONE",
    "priority": "LOW"
  }'
echo -e "\n"

echo "========================================"
echo "14. Get Project Stats"
echo "========================================"
curl -s "$BASE_URL/projects/$PROJECT_ID/stats" \
  -H "Authorization: Bearer $TOKEN"
echo -e "\n"

echo "========================================"
echo "15. Get Single Project Again (verify tasks embedded)"
echo "========================================"
curl -s "$BASE_URL/projects/$PROJECT_ID" \
  -H "Authorization: Bearer $TOKEN"
echo -e "\n"

echo "========================================"
echo "16. Delete Task"
echo "========================================"
curl -s -X DELETE "$BASE_URL/tasks/$TASK_ID" \
  -H "Authorization: Bearer $TOKEN"
echo -e "\n"

echo "========================================"
echo "17. Delete Project (cascade tasks)"
echo "========================================"
curl -s -X DELETE "$BASE_URL/projects/$PROJECT_ID" \
  -H "Authorization: Bearer $TOKEN"
echo -e "\n"

echo "========================================"
echo "18. Invalid Login Test (should fail)"
echo "========================================"
curl -s -X POST "$BASE_URL/auth/login" \
  -H "Content-Type: application/json" \
  -d "{
    \"email\": \"$TEST_EMAIL\",
    \"password\": \"wrongpass\"
  }"
echo -e "\n"

echo "========================================"
echo "19. Invalid UUID Test (should fail)"
echo "========================================"
curl -s "$BASE_URL/projects/invalid-id" \
  -H "Authorization: Bearer $TOKEN"
echo -e "\n"

echo "========================================"
echo "✅ FULL E2E TEST SUITE COMPLETED"
echo "========================================"