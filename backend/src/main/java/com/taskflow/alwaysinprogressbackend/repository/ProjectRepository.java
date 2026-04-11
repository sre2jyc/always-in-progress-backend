package com.taskflow.alwaysinprogressbackend.repository;

import com.taskflow.alwaysinprogressbackend.model.Project;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

public interface ProjectRepository extends JpaRepository<Project, UUID> {

    Page<Project> findByOwnerId(UUID ownerId, Pageable pageable);

    Page<Project> findByOwnerIdOrIdIn(UUID ownerId, List<UUID> ids, Pageable pageable);
}