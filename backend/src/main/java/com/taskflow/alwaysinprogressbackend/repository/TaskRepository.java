package com.taskflow.alwaysinprogressbackend.repository;

import com.taskflow.alwaysinprogressbackend.model.Task;
import com.taskflow.alwaysinprogressbackend.model.TaskStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface TaskRepository extends JpaRepository<Task, UUID> {

    // Existing paginated task listing
    Page<Task> findByProjectId(UUID projectId, Pageable pageable);

    Page<Task> findByProjectIdAndStatus(UUID projectId, TaskStatus status, Pageable pageable);

    Page<Task> findByProjectIdAndAssigneeId(UUID projectId, UUID assigneeId, Pageable pageable);

    Page<Task> findByProjectIdAndStatusAndAssigneeId(
            UUID projectId,
            TaskStatus status,
            UUID assigneeId,
            Pageable pageable
    );

    // Needed for GET /projects/:id → project + tasks
    List<Task> findByProjectId(UUID projectId);

    // Needed for GET /projects → list projects + all tasks
    List<Task> findByProjectIdIn(List<UUID> projectIds);

    // Stats APIs
    @Query("SELECT t.status, COUNT(t) FROM Task t WHERE t.projectId = :projectId GROUP BY t.status")
    List<Object[]> countTasksByStatus(UUID projectId);

    @Query("SELECT t.assigneeId, COUNT(t) FROM Task t WHERE t.projectId = :projectId GROUP BY t.assigneeId")
    List<Object[]> countTasksByAssignee(UUID projectId);

    // For "projects user owns or has tasks in"
    @Query("SELECT DISTINCT t.projectId FROM Task t WHERE t.assigneeId = :userId")
    List<UUID> findDistinctProjectIdsByAssigneeId(UUID userId);
}