package com.taskflow.alwaysinprogressbackend.service;

import com.taskflow.alwaysinprogressbackend.dto.CreateTaskRequest;
import com.taskflow.alwaysinprogressbackend.dto.UpdateTaskRequest;
import com.taskflow.alwaysinprogressbackend.model.Project;
import com.taskflow.alwaysinprogressbackend.model.Task;
import com.taskflow.alwaysinprogressbackend.model.TaskStatus;
import com.taskflow.alwaysinprogressbackend.repository.ProjectRepository;
import com.taskflow.alwaysinprogressbackend.repository.TaskRepository;
import com.taskflow.alwaysinprogressbackend.repository.UserRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;
import lombok.extern.slf4j.Slf4j;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
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
    private final SseService sseService;

    // CREATE TASK
    public Task createTask(UUID projectId, CreateTaskRequest request, UUID userId) {

        // validate assignee
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

        sseService.send(
        task.getProjectId(),
        "TASK_CREATED",
        savedTask
        );

        log.info(
            "Task created | taskId={} projectId={} createdBy={} assigneeId={}",
            savedTask.getId(),
            projectId,
            userId,
            savedTask.getAssigneeId()
        );

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
    public Task updateTask(UUID taskId, UpdateTaskRequest request, UUID userId) {

        Task task = getTaskOrThrow(taskId);
        Project project = getProjectOrThrow(task.getProjectId());

        validateAccess(task, project, userId);

        if (request.getTitle() != null) task.setTitle(request.getTitle());
        if (request.getDescription() != null) task.setDescription(request.getDescription());
        if (request.getStatus() != null) task.setStatus(request.getStatus());
        if (request.getPriority() != null) task.setPriority(request.getPriority());

        if (request.getAssigneeId() != null) {
            validateAssignee(request.getAssigneeId());
            task.setAssigneeId(request.getAssigneeId());
        }

        if (request.getDueDate() != null) {
            task.setDueDate(request.getDueDate());
        }

        // if (request.getVersion() != null && !request.getVersion().equals(task.getVersion())) {
        //     throw new RuntimeException("CONFLICT");
        // }

        Task updated =  taskRepository.save(task);

        sseService.send(
        task.getProjectId(),
        "TASK_UPDATED",
        updated
        );

        log.info(
            "Task updated | taskId={} updatedBy={}",
            task.getId(),
            userId
        );

        return updated;
    }

    // DELETE TASK
    public void deleteTask(UUID taskId, UUID userId) {

        Task task = getTaskOrThrow(taskId);
        Project project = getProjectOrThrow(task.getProjectId());

        validateDeleteAccess(task, project, userId);

        taskRepository.delete(task);

        log.info(
            "Task deleted | taskId={} deletedBy={}",
            taskId,
            userId
        );

        sseService.send(
            task.getProjectId(),
            "TASK_DELETED",
            Map.of("taskId", taskId)
        );
    }

    // HELPERS

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
