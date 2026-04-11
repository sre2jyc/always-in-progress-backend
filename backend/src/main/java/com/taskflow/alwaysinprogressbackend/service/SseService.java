package com.taskflow.alwaysinprogressbackend.service;

import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
public class SseService {

    private final Map<UUID, List<SseEmitter>> emitters = new ConcurrentHashMap<>();

    // Subscribe
    public SseEmitter subscribe(UUID projectId) {

        SseEmitter emitter = new SseEmitter(0L);

        emitters.computeIfAbsent(projectId, key -> new CopyOnWriteArrayList<>())
                .add(emitter);

        emitter.onCompletion(() -> remove(projectId, emitter));
        emitter.onTimeout(() -> remove(projectId, emitter));
        emitter.onError((e) -> remove(projectId, emitter));

        try {
            emitter.send(SseEmitter.event()
                    .name("CONNECTED")
                    .data("Subscribed successfully"));
        } catch (IOException e) {
            emitter.complete();
        }

        return emitter;
    }

    // Send event
    public void send(UUID projectId, String event, Object data) {

        List<SseEmitter> list = emitters.get(projectId);
        if (list == null) return;

        for (SseEmitter emitter : list) {
            try {
                emitter.send(
                    SseEmitter.event()
                        .name(event)
                        .data(data)
                );
            } catch (IOException e) {
                remove(projectId, emitter);
            }
        }
    }

    private void remove(UUID projectId, SseEmitter emitter) {
        List<SseEmitter> list = emitters.get(projectId);
        if (list != null) {
            list.remove(emitter);
        }
    }
}