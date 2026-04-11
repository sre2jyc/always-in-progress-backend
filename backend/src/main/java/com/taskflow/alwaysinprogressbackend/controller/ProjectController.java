package com.taskflow.alwaysinprogressbackend.controller;

import com.taskflow.alwaysinprogressbackend.dto.CreateProjectRequest;
import com.taskflow.alwaysinprogressbackend.dto.ProjectDetailsResponse;
import com.taskflow.alwaysinprogressbackend.model.Project;
import com.taskflow.alwaysinprogressbackend.service.ProjectService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/projects")
@RequiredArgsConstructor
public class ProjectController {

    private final ProjectService projectService;

    @PostMapping
    public Project createProject(@Valid @RequestBody CreateProjectRequest request) {

        UUID userId = getCurrentUserId();
        return projectService.createProject(request, userId);
    }

    @GetMapping
    public List<ProjectDetailsResponse> getProjects(@RequestParam(defaultValue = "0")int page, @RequestParam(defaultValue = "10")int limit) {

        UUID userId = getCurrentUserId();
        return projectService.getProjects(userId, page, limit);
    }

    @GetMapping("/{id}")
    public ProjectDetailsResponse getProject(@PathVariable UUID id) {
        return projectService.getProject(id);
    }

    @PatchMapping("/{id}")
    public Project updateProject(@PathVariable UUID id,
                                 @Valid @RequestBody CreateProjectRequest request) {

        UUID userId = getCurrentUserId();
        return projectService.updateProject(id, request, userId);
    }

    @DeleteMapping("/{id}")
    public String deleteProject(@PathVariable UUID id) {

        UUID userId = getCurrentUserId();
        projectService.deleteProject(id, userId);

        return "Project deleted";
    }

    @GetMapping("/{id}/stats")
    public Map<String, Object> getProjectStats(@PathVariable UUID id) {
        return projectService.getProjectStats(id);
    }

    // 🔥 Helper
    private UUID getCurrentUserId() {
        return (UUID) SecurityContextHolder
                .getContext()
                .getAuthentication()
                .getPrincipal();
    }
}