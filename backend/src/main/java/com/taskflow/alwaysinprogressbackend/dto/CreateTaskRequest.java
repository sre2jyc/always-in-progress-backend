package com.taskflow.alwaysinprogressbackend.dto;

import com.taskflow.alwaysinprogressbackend.model.TaskPriority;
import com.taskflow.alwaysinprogressbackend.model.TaskStatus;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class CreateTaskRequest {

    @NotBlank(message = "Title is required")
    private String title;

    private String description;

    private TaskStatus status;
    private TaskPriority priority;

    private UUID assigneeId;
    private LocalDateTime dueDate;
}