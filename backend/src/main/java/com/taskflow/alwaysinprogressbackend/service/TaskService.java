package com.taskflow.alwaysinprogressbackend.service;

import com.taskflow.alwaysinprogressbackend.dto.CreateTaskRequest;
import com.taskflow.alwaysinprogressbackend.dto.UpdateTaskRequest;
import com.taskflow.alwaysinprogressbackend.model.Project;
import com.taskflow.alwaysinprogressbackend.model.Task;
import com.taskflow.alwaysinprogressbackend.model.TaskStatus;
import com.taskflow.alwaysinprogressbackend.repository.ProjectRepository;
import com.taskflow.alwaysinprogressbackend.repository.TaskRepository;
import com.taskflow.alwaysinprogressbackend.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class TaskService {

    private final TaskRepository taskRepository;
    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final OutboxService outboxService;
    private final RedisEventPublisher redisEventPublisher;

    // CREATE TASK
    @Transactional
    public Task createTask(UUID projectId, CreateTaskRequest request, UUID userId) {

        if (request.getAssigneeId() != null) {
            validateAssignee(request.getAssigneeId());
        }

        Task task = Task.builder()
                .id(UUID.randomUUID())
                .title(request.getTitle())
                .description(request.getDescription())
                .status(request.getStatus())
                .priority(request.getPriority())
                .assigneeId(request.getAssigneeId())
                .projectId(projectId)
                .createdBy(userId)
                .dueDate(request.getDueDate())
                .build();

        Task savedTask = taskRepository.save(task);

        outboxService.publishTaskEvent(
                "TASK_CREATED",
                savedTask.getId(),
                projectId,
                userId,
                "USER",
                null,
                savedTask,
                null
        );

        redisEventPublisher.publish(projectId, "TASK_CREATED", savedTask);

        log.info("Task created | taskId={} projectId={} createdBy={} assigneeId={}",
                savedTask.getId(), projectId, userId, savedTask.getAssigneeId());

        return savedTask;
    }

    // GET TASKS WITH FILTERS
    public List<Task> getTasks(UUID projectId, TaskStatus status, UUID assignee, int page, int limit) {

        projectRepository.findById(projectId)
                .orElseThrow(() -> new RuntimeException("NOT_FOUND"));

        Pageable pageable = PageRequest.of(page, limit);

        if (status != null && assignee != null) {
            return taskRepository.findByProjectIdAndStatusAndAssigneeId(projectId, status, assignee, pageable).getContent();
        }
        if (status != null) {
            return taskRepository.findByProjectIdAndStatus(projectId, status, pageable).getContent();
        }
        if (assignee != null) {
            return taskRepository.findByProjectIdAndAssigneeId(projectId, assignee, pageable).getContent();
        }
        return taskRepository.findByProjectId(projectId, pageable).getContent();
    }

    // UPDATE TASK
    @Transactional
    public Task updateTask(UUID taskId, UpdateTaskRequest request, UUID userId) {

        Task task = getTaskOrThrow(taskId);
        Project project = getProjectOrThrow(task.getProjectId());

        validateAccess(task, project, userId);

        // capture old state before mutations
        Task oldSnapshot = cloneTask(task);
        List<String> changedFields = new ArrayList<>();

        if (request.getTitle() != null && !request.getTitle().equals(task.getTitle())) {
            changedFields.add("title");
            task.setTitle(request.getTitle());
        }
        if (request.getDescription() != null && !request.getDescription().equals(task.getDescription())) {
            changedFields.add("description");
            task.setDescription(request.getDescription());
        }
        if (request.getStatus() != null && !request.getStatus().equals(task.getStatus())) {
            changedFields.add("status");
            task.setStatus(request.getStatus());
        }
        if (request.getPriority() != null && !request.getPriority().equals(task.getPriority())) {
            changedFields.add("priority");
            task.setPriority(request.getPriority());
        }
        if (request.getAssigneeId() != null && !request.getAssigneeId().equals(task.getAssigneeId())) {
            validateAssignee(request.getAssigneeId());
            changedFields.add("assigneeId");
            task.setAssigneeId(request.getAssigneeId());
        }
        if (request.getDueDate() != null && !request.getDueDate().equals(task.getDueDate())) {
            changedFields.add("dueDate");
            task.setDueDate(request.getDueDate());
        }

        Task updated = taskRepository.save(task);

        outboxService.publishTaskEvent(
                "TASK_UPDATED",
                updated.getId(),
                updated.getProjectId(),
                userId,
                "USER",
                oldSnapshot,
                updated,
                changedFields
        );

        redisEventPublisher.publish(task.getProjectId(), "TASK_UPDATED", updated);

        log.info("Task updated | taskId={} updatedBy={} changedFields={}", task.getId(), userId, changedFields);

        return updated;
    }

    // DELETE TASK
    @Transactional
    public void deleteTask(UUID taskId, UUID userId) {

        Task task = getTaskOrThrow(taskId);
        Project project = getProjectOrThrow(task.getProjectId());

        validateDeleteAccess(task, project, userId);

        UUID projectId = task.getProjectId();


        taskRepository.delete(task);

        outboxService.publishTaskEvent(
                "TASK_DELETED",
                taskId,
                projectId,
                userId,
                "USER",
                task,
                null,
                null
        );

        redisEventPublisher.publish(projectId, "TASK_DELETED", Map.of("taskId", taskId));

        log.info("Task deleted | taskId={} deletedBy={}", taskId, userId);
    }

    // HELPERS

    private Task cloneTask(Task task) {
        return Task.builder()
                .id(task.getId())
                .title(task.getTitle())
                .description(task.getDescription())
                .status(task.getStatus())
                .priority(task.getPriority())
                .assigneeId(task.getAssigneeId())
                .projectId(task.getProjectId())
                .createdBy(task.getCreatedBy())
                .dueDate(task.getDueDate())
                .createdAt(task.getCreatedAt())
                .updatedAt(task.getUpdatedAt())
                .version(task.getVersion())
                .build();
    }

    private Task getTaskOrThrow(UUID taskId) {
        return taskRepository.findById(taskId)
                .orElseThrow(() -> new RuntimeException("NOT_FOUND"));
    }

    private Project getProjectOrThrow(UUID projectId) {
        return projectRepository.findById(projectId)
                .orElseThrow(() -> new RuntimeException("NOT_FOUND"));
    }

    private void validateAccess(Task task, Project project, UUID userId) {
        boolean isOwner = project.getOwnerId().equals(userId);
        boolean isCreator = task.getCreatedBy().equals(userId);
        boolean isAssignee = userId.equals(task.getAssigneeId());
        if (!isOwner && !isCreator && !isAssignee) {
            throw new RuntimeException("FORBIDDEN");
        }
    }

    private void validateDeleteAccess(Task task, Project project, UUID userId) {
        boolean isOwner = project.getOwnerId().equals(userId);
        boolean isCreator = task.getCreatedBy().equals(userId);
        if (!isOwner && !isCreator) {
            throw new RuntimeException("FORBIDDEN");
        }
    }

    private void validateAssignee(UUID assigneeId) {
        if (!userRepository.existsById(assigneeId)) {
            throw new RuntimeException("INVALID_ASSIGNEE");
        }
    }
}
