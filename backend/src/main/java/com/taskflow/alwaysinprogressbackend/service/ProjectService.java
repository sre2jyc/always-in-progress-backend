package com.taskflow.alwaysinprogressbackend.service;

import com.taskflow.alwaysinprogressbackend.dto.CreateProjectRequest;
import com.taskflow.alwaysinprogressbackend.dto.ProjectDetailsResponse;
import com.taskflow.alwaysinprogressbackend.model.Project;
import com.taskflow.alwaysinprogressbackend.model.Task;
import com.taskflow.alwaysinprogressbackend.repository.ProjectRepository;
import com.taskflow.alwaysinprogressbackend.repository.TaskRepository;
import lombok.extern.slf4j.Slf4j;


// import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final TaskRepository taskRepository;

    // CREATE PROJECT
    public Project createProject(CreateProjectRequest request, UUID userId) {

        Project project = Project.builder()
                .id(UUID.randomUUID())
                .name(request.getName())
                .description(request.getDescription())
                .ownerId(userId)
                .build();

        Project projectCreated = projectRepository.save(project);

        log.info(
            "Project created | projectId={} ownerId={}",
            project.getId(),
            userId
        );

        return projectCreated;
    }

    // GET PROJECTS
    public List<ProjectDetailsResponse> getProjects(UUID userId, int page, int limit) {

        Pageable pageable = PageRequest.of(page, limit);

        List<UUID> taskProjectIds =
                taskRepository.findDistinctProjectIdsByAssigneeId(userId);

        List<Project> projects =
                projectRepository.findByOwnerIdOrIdIn(
                        userId,
                        taskProjectIds.isEmpty() ? List.of(UUID.randomUUID()) : taskProjectIds,
                        pageable
                ).getContent();

        List<UUID> projectIds = projects.stream()
                .map(Project::getId)
                .toList();

        List<Task> allTasks = taskRepository.findByProjectIdIn(projectIds);

        Map<UUID, List<Task>> taskMap = allTasks.stream()
                .collect(Collectors.groupingBy(Task::getProjectId));

        return projects.stream()
                .map(project -> ProjectDetailsResponse.builder()
                        .id(project.getId())
                        .name(project.getName())
                        .description(project.getDescription())
                        .ownerId(project.getOwnerId())
                        .createdAt(project.getCreatedAt())
                        .tasks(taskMap.getOrDefault(project.getId(), List.of()))
                        .build())
                .toList();
    }

    // GET SINGLE PROJECT
    public ProjectDetailsResponse getProject(UUID id) {

        Project project = getProjectOrThrow(id);

        List<Task> tasks = taskRepository.findByProjectId(id);

        return ProjectDetailsResponse.builder()
                .id(project.getId())
                .name(project.getName())
                .description(project.getDescription())
                .ownerId(project.getOwnerId())
                .createdAt(project.getCreatedAt())
                .tasks(tasks)
                .build();
    }

    // UPDATE PROJECT
    public Project updateProject(UUID id, CreateProjectRequest request, UUID userId) {

        Project project = getProjectOrThrow(id);

        validateOwner(project, userId);

        project.setName(request.getName());
        project.setDescription(request.getDescription());

        return projectRepository.save(project);
    }

    // DELETE PROJECT
    public void deleteProject(UUID id, UUID userId) {

        Project project = getProjectOrThrow(id);

        validateOwner(project, userId);

        projectRepository.delete(project);

        log.info(
            "Project deleted | projectId={} deletedBy={}",
            project.getId(),
            userId
        );
    }

    public Map<String, Object> getProjectStats(UUID projectId) {

    getProjectOrThrow(projectId);

    List<Object[]> statusCounts = taskRepository.countTasksByStatus(projectId);
    List<Object[]> assigneeCounts = taskRepository.countTasksByAssignee(projectId);

    Map<String, Long> statusMap = new HashMap<>();
    for (Object[] row : statusCounts) {
        statusMap.put(row[0].toString(), (Long) row[1]);
    }

    Map<String, Long> assigneeMap = new HashMap<>();
    for (Object[] row : assigneeCounts) {
        assigneeMap.put(
                row[0] != null ? row[0].toString() : "UNASSIGNED",
                (Long) row[1]
        );
    }

    return Map.of(
            "statusCounts", statusMap,
            "assigneeCounts", assigneeMap
    );
}

    // HELPERS (CLEAN CODE)

    private Project getProjectOrThrow(UUID id) {
        return projectRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("NOT_FOUND"));
    }

    private void validateOwner(Project project, UUID userId) {
        if (!project.getOwnerId().equals(userId)) {
            throw new RuntimeException("FORBIDDEN");
        }
    }
}