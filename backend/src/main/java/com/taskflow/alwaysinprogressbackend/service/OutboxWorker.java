package com.taskflow.alwaysinprogressbackend.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taskflow.alwaysinprogressbackend.model.Outbox;
import com.taskflow.alwaysinprogressbackend.model.TaskActivityLog;
import com.taskflow.alwaysinprogressbackend.repository.OutboxRepository;
import com.taskflow.alwaysinprogressbackend.repository.TaskActivityLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class OutboxWorker {

    private final OutboxRepository outboxRepository;
    private final TaskActivityLogRepository taskActivityLogRepository;
    private final ObjectMapper objectMapper;

    //waits 1s after the previous run finishes. Safer, no overlap.
    @Scheduled(fixedDelay = 1000)
    public void process() {
        List<Outbox> pending = outboxRepository.findByProcessedFalseOrderByCreatedAtAsc();

        for (Outbox entry : pending) {
            try {
                Map<String, Object> payload = objectMapper.readValue(entry.getPayload(), Map.class);

                TaskActivityLog log = TaskActivityLog.builder()
                        .taskId(UUID.fromString((String) payload.get("taskId")))
                        .projectId(UUID.fromString((String) payload.get("projectId")))
                        .actorId(UUID.fromString((String) payload.get("actorId")))
                        .actorType((String) payload.get("actorType"))
                        .actionType((String) payload.get("eventType"))
                        .oldValue(objectMapper.writeValueAsString(payload.get("oldValue")))
                        .newValue(objectMapper.writeValueAsString(payload.get("newValue")))
                        .changedFields(((List<String>) payload.get("changedFields")).toArray(new String[0]))
                        .build();

                taskActivityLogRepository.save(log);

                entry.setProcessed(true);
                entry.setProcessedAt(LocalDateTime.now());
                outboxRepository.save(entry);

            } catch (Exception e) {
                log.error("Failed to process outbox entry | id={}", entry.getId(), e);
                // do NOT mark as processed — will retry on next tick
            }
        }
    }
}
