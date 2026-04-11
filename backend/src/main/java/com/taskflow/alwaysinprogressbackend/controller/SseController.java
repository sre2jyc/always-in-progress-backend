package com.taskflow.alwaysinprogressbackend.controller;

import com.taskflow.alwaysinprogressbackend.service.SseService;
import lombok.RequiredArgsConstructor;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class SseController {

    private final SseService sseService;

    @GetMapping(value="/projects/{projectId}/events",produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribe(@PathVariable UUID projectId) {
        return sseService.subscribe(projectId);
    }
}