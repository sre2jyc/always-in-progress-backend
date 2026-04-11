package com.taskflow.alwaysinprogressbackend.controller;

import com.taskflow.alwaysinprogressbackend.dto.CreateTaskRequest;
import com.taskflow.alwaysinprogressbackend.dto.UpdateTaskRequest;
import com.taskflow.alwaysinprogressbackend.model.Task;
import com.taskflow.alwaysinprogressbackend.model.TaskStatus;
import com.taskflow.alwaysinprogressbackend.service.TaskService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class TaskController {

    private final TaskService taskService;

    // CREATE TASK
    @PostMapping("/projects/{projectId}/tasks")
    public Task createTask(@PathVariable UUID projectId,
                           @Valid @RequestBody CreateTaskRequest request) {

        UUID userId = getCurrentUserId();
        return taskService.createTask(projectId, request, userId);
    }

    // GET TASKS (WITH FILTERS)
    @GetMapping("/projects/{projectId}/tasks")
    public List<Task> getTasks(
            @PathVariable UUID projectId,
            @RequestParam(required = false) TaskStatus status,
            @RequestParam(required = false) UUID assignee,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int limit
        ) {

        return taskService.getTasks(projectId, status, assignee, page, limit);
    }

    // UPDATE TASK
    @PatchMapping("/tasks/{taskId}")
    public Task updateTask(@PathVariable UUID taskId,
                           @Valid @RequestBody UpdateTaskRequest request) {

        UUID userId = getCurrentUserId();
        return taskService.updateTask(taskId, request, userId);
    }

    // DELETE TASK
    @DeleteMapping("/tasks/{taskId}")
    public Map<String, String> deleteTask(@PathVariable UUID taskId) {

        UUID userId = getCurrentUserId();
        taskService.deleteTask(taskId, userId);

        return Map.of("message", "Task deleted successfully");
    }

    // Helper
    private UUID getCurrentUserId() {
        return (UUID) SecurityContextHolder
                .getContext()
                .getAuthentication()
                .getPrincipal();
    }
}