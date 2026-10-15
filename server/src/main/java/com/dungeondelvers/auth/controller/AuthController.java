package com.dungeondelvers.auth.controller;

import com.dungeondelvers.auth.dto.AuthTokensDto;
import com.dungeondelvers.auth.dto.LoginRequest;
import com.dungeondelvers.auth.dto.RefreshTokenRequest;
import com.dungeondelvers.auth.dto.RegisterRequest;
import com.dungeondelvers.auth.service.AuthService;
import com.dungeondelvers.common.dto.ApiResponse;
import com.dungeondelvers.common.exception.GameException;
import com.dungeondelvers.common.ratelimit.KeyedRateLimiter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.Locale;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    /** Phase 0 checklist: login 5/min (per client IP + email, so one user can't lock out others). */
    static final int LOGIN_ATTEMPTS_PER_MINUTE = 5;

    private final AuthService authService;
    private final KeyedRateLimiter loginLimiter = new KeyedRateLimiter(LOGIN_ATTEMPTS_PER_MINUTE, 60_000);

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<AuthTokensDto> register(@Valid @RequestBody RegisterRequest request) {
        return ApiResponse.ok(authService.register(request));
    }

    @PostMapping("/login")
    public ApiResponse<AuthTokensDto> login(@Valid @RequestBody LoginRequest request, HttpServletRequest http) {
        // Remote address, not X-Forwarded-For: behind a proxy this must change to a trusted header.
        String key = http.getRemoteAddr() + "|" + request.email().trim().toLowerCase(Locale.ROOT);
        if (!loginLimiter.tryAcquire(key, System.currentTimeMillis())) {
            throw GameException.tooManyRequests("Too many login attempts, try again in a minute");
        }
        return ApiResponse.ok(authService.login(request));
    }

    @PostMapping("/refresh")
    public ApiResponse<AuthTokensDto> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return ApiResponse.ok(authService.refresh(request.refreshToken()));
    }
}
