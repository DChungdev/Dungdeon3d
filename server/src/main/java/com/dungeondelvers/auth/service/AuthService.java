package com.dungeondelvers.auth.service;

import com.dungeondelvers.app.properties.DdoProperties;
import com.dungeondelvers.auth.dto.AuthTokensDto;
import com.dungeondelvers.auth.dto.LoginRequest;
import com.dungeondelvers.auth.dto.RegisterRequest;
import com.dungeondelvers.auth.jwt.JwtService;
import com.dungeondelvers.auth.jwt.RefreshTokenStore;
import com.dungeondelvers.common.exception.ErrorCode;
import com.dungeondelvers.common.exception.GameException;
import com.dungeondelvers.persistence.entity.Account;
import com.dungeondelvers.persistence.repository.AccountRepository;
import java.util.UUID;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final AccountRepository accountRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RefreshTokenStore refreshTokenStore;
    private final long refreshTokenTtlSec;

    public AuthService(
            AccountRepository accountRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            RefreshTokenStore refreshTokenStore,
            DdoProperties properties
    ) {
        this.accountRepository = accountRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.refreshTokenStore = refreshTokenStore;
        this.refreshTokenTtlSec = properties.jwt().refreshTokenTtlSec();
    }

    @Transactional
    public AuthTokensDto register(RegisterRequest request) {
        if (accountRepository.existsByEmailIgnoreCase(request.email())) {
            throw GameException.conflict(ErrorCode.EMAIL_EXISTS, "Email already exists");
        }

        Account account = new Account();
        account.setEmail(request.email().trim().toLowerCase());
        account.setPasswordHash(passwordEncoder.encode(request.password()));
        account.setStatus("ACTIVE");
        accountRepository.save(account);

        return issueTokens(account.getId());
    }

    @Transactional(readOnly = true)
    public AuthTokensDto login(LoginRequest request) {
        Account account = accountRepository.findByEmailIgnoreCase(request.email().trim())
                .orElseThrow(() -> GameException.unauthorized("Invalid email or password"));

        if (!"ACTIVE".equals(account.getStatus())) {
            throw GameException.forbidden("Account is not active");
        }
        if (account.getPasswordHash() == null
                || !passwordEncoder.matches(request.password(), account.getPasswordHash())) {
            throw GameException.unauthorized("Invalid email or password");
        }

        return issueTokens(account.getId());
    }

    @Transactional(readOnly = true)
    public AuthTokensDto refresh(String refreshToken) {
        UUID accountId = refreshTokenStore.consume(refreshToken)
                .orElseThrow(() -> GameException.unauthorized("Invalid or expired refresh token"));

        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> GameException.unauthorized("Account not found"));

        if (!"ACTIVE".equals(account.getStatus())) {
            throw GameException.forbidden("Account is not active");
        }

        return issueTokens(accountId);
    }

    private AuthTokensDto issueTokens(UUID accountId) {
        String accessToken = jwtService.createAccessToken(accountId);
        String refreshToken = refreshTokenStore.issue(accountId, refreshTokenTtlSec);
        return new AuthTokensDto(
                accessToken,
                refreshToken,
                jwtService.getAccessTokenTtlSec(),
                accountId
        );
    }
}
