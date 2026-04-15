package com.taskflow.alwaysinprogressbackend.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "task_activity_logs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TaskActivityLog {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(name = "task_id")
    private UUID taskId;

    @Column(name = "project_id")
    private UUID projectId;

    @Column(name = "actor_id")
    private UUID actorId;

    @Column(name = "actor_type")
    private String actorType;

    @Column(name = "action_type")
    private String actionType;

    @Column(name = "old_value", columnDefinition = "jsonb")
    private String oldValue;

    @Column(name = "new_value", columnDefinition = "jsonb")
    private String newValue;

    @Column(name = "changed_fields", columnDefinition = "text[]")
    private String[] changedFields;

    @Column(name = "conversation_id")
    private UUID conversationId;

    @Column(columnDefinition = "jsonb")
    private String metadata;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
