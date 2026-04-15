package com.taskflow.alwaysinprogressbackend.service;

import com.taskflow.alwaysinprogressbackend.model.TaskActivityLog;
import com.taskflow.alwaysinprogressbackend.repository.TaskActivityLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ActivityService {

    private final TaskActivityLogRepository taskActivityLogRepository;

    public List<TaskActivityLog> getTaskHistory(UUID taskId) {
        return taskActivityLogRepository.findByTaskIdOrderByCreatedAtAsc(taskId);
    }

    public List<TaskActivityLog> getProjectActivity(UUID projectId, String actorType, UUID actorId) {
        if (actorType != null) {
            return taskActivityLogRepository.findByProjectIdAndActorTypeOrderByCreatedAtDesc(projectId, actorType);
        }
        if (actorId != null) {
            return taskActivityLogRepository.findByProjectIdAndActorIdOrderByCreatedAtDesc(projectId, actorId);
        }
        return taskActivityLogRepository.findByProjectIdOrderByCreatedAtDesc(projectId);
    }
}
