package com.taskflow.alwaysinprogressbackend.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taskflow.alwaysinprogressbackend.model.Outbox;
import com.taskflow.alwaysinprogressbackend.repository.OutboxRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class OutboxService {

    private final OutboxRepository outboxRepository;
    private final ObjectMapper objectMapper;

    public void publishTaskEvent(
            String eventType,
            UUID taskId,
            UUID projectId,
            UUID actorId,
            String actorType,
            Object oldValue,
            Object newValue,
            List<String> changedFields
    ) {
        try {
            Map<String, Object> payload = Map.of(
                    "eventType", eventType,
                    "taskId", taskId.toString(),
                    "projectId", projectId.toString(),
                    "actorId", actorId.toString(),
                    "actorType", actorType,
                    "oldValue", oldValue != null ? oldValue : Map.of(),
                    "newValue", newValue != null ? newValue : Map.of(),
                    "changedFields", changedFields != null ? changedFields : List.of()
            );

            String json = objectMapper.writeValueAsString(payload);

            Outbox entry = Outbox.builder()
                    .eventType(eventType)
                    .payload(json)
                    .build();

            outboxRepository.save(entry);

        } catch (Exception e) {
            log.error("Failed to write outbox entry | eventType={} taskId={}", eventType, taskId, e);
        }
    }
}
