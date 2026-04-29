package com.storystream.api.controller;

import com.storystream.api.service.GeminiContextService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/debug/gemini")
public class DebugController {

    private final GeminiContextService geminiContextService;

    public DebugController(GeminiContextService geminiContextService) {
        this.geminiContextService = geminiContextService;
    }

    @GetMapping(value = "/last-request", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> lastRequest() {
        String allow = System.getenv("ALLOW_GEMINI_DEBUG");
        if (allow == null || !allow.equalsIgnoreCase("true")) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("{\"error\":\"debug disabled\"}");
        }

        String body = geminiContextService.getLastRequestBody();
        if (body == null) {
            return ResponseEntity.status(HttpStatus.NO_CONTENT).body("");
        }

        return ResponseEntity.ok().body(body);
    }
}

