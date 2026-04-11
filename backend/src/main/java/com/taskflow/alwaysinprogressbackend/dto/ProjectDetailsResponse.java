package com.taskflow.alwaysinprogressbackend.dto;

import com.taskflow.alwaysinprogressbackend.model.Task;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProjectDetailsResponse {

    private UUID id;
    private String name;
    private String description;
    private UUID ownerId;
    private LocalDateTime createdAt;

    private List<Task> tasks;
}