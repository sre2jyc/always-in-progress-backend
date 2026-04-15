package com.taskflow.alwaysinprogressbackend.controller;

import com.taskflow.alwaysinprogressbackend.model.TaskActivityLog;
import com.taskflow.alwaysinprogressbackend.service.ActivityService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class ActivityController {

    private final ActivityService activityService;

    @GetMapping("/tasks/{taskId}/history")
    public List<TaskActivityLog> getTaskHistory(@PathVariable UUID taskId) {
        return activityService.getTaskHistory(taskId);
    }

    @GetMapping("/projects/{projectId}/activity")
    public List<TaskActivityLog> getProjectActivity(
            @PathVariable UUID projectId,
            @RequestParam(required = false) String actor_type,
            @RequestParam(required = false) UUID actor_id) {

        return activityService.getProjectActivity(projectId, actor_type, actor_id);
    }
}
