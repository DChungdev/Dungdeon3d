package com.dungeondelvers.api.rest;

import com.dungeondelvers.api.rest.dto.HealthResponse;
import com.dungeondelvers.game.session.SessionManager;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthController {

    private final SessionManager sessionManager;

    @Value("${spring.application.name:dungeon-delvers-server}")
    private String applicationName;

    public HealthController(SessionManager sessionManager) {
        this.sessionManager = sessionManager;
    }

    @GetMapping("/health")
    public HealthResponse health() {
        return new HealthResponse("UP", sessionManager.connectedCount(), applicationName);
    }
}
