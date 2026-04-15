package com.taskflow.alwaysinprogressbackend.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taskflow.alwaysinprogressbackend.config.RedisConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class RedisEventPublisher {

    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;

    public void publish(UUID projectId, String eventName, Object data) {
        try {
            Map<String, Object> message = Map.of(
                    "projectId", projectId.toString(),
                    "eventName", eventName,
                    "data", data
            );

            String json = objectMapper.writeValueAsString(message);
            redisTemplate.convertAndSend(RedisConfig.TASK_EVENTS_CHANNEL, json);

        } catch (Exception e) {
            log.error("Failed to publish Redis event | projectId={} eventName={}", projectId, eventName, e);
        }
    }
}
