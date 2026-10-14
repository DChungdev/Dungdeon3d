package com.dungeondelvers.auth.dto;

import java.util.UUID;

public record AuthTokensDto(
        String accessToken,
        String refreshToken,
        long expiresIn,
        UUID accountId
) {}
