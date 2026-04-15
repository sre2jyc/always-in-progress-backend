package com.taskflow.alwaysinprogressbackend.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class RedisEventSubscriber implements MessageListener {

    private final SseService sseService;
    private final ObjectMapper objectMapper;

    @Override
    public void onMessage(Message message, byte[] pattern) {
        try {
            Map<String, Object> payload = objectMapper.readValue(message.getBody(), Map.class);

            UUID projectId = UUID.fromString((String) payload.get("projectId"));
            String eventName = (String) payload.get("eventName");
            Object data = payload.get("data");

            sseService.send(projectId, eventName, data);

        } catch (Exception e) {
            log.error("Failed to process Redis event", e);
        }
    }
}
