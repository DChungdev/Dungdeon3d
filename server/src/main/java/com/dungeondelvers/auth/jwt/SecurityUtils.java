package com.dungeondelvers.auth.jwt;

import java.util.UUID;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

public final class SecurityUtils {

    private SecurityUtils() {}

    public static UUID currentAccountId() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth instanceof UsernamePasswordAuthenticationToken token
                && token.getPrincipal() instanceof UUID accountId) {
            return accountId;
        }
        throw new IllegalStateException("No authenticated account");
    }
}
