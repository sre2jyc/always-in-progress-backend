package com.taskflow.alwaysinprogressbackend.repository;

import com.taskflow.alwaysinprogressbackend.model.TaskActivityLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface TaskActivityLogRepository extends JpaRepository<TaskActivityLog, UUID> {

    List<TaskActivityLog> findByTaskIdOrderByCreatedAtAsc(UUID taskId);

    List<TaskActivityLog> findByProjectIdOrderByCreatedAtDesc(UUID projectId);

    List<TaskActivityLog> findByProjectIdAndActorTypeOrderByCreatedAtDesc(UUID projectId, String actorType);

    List<TaskActivityLog> findByProjectIdAndActorIdOrderByCreatedAtDesc(UUID projectId, UUID actorId);
}
