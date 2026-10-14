package com.dungeondelvers.game.ws;

import com.dungeondelvers.auth.jwt.JwtService;
import com.dungeondelvers.persistence.entity.CharacterEntity;
import com.dungeondelvers.persistence.repository.CharacterRepository;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * Validates {@code ?token=<accessToken>} on the WS upgrade and resolves the character
 * to play. {@code ?characterId=} is optional; without it the account's first character
 * is used (Phase 0 has no character select screen).
 */
@Component
public class JwtHandshakeInterceptor implements HandshakeInterceptor {

    public static final String ATTR_ACCOUNT_ID = "accountId";
    public static final String ATTR_CHARACTER_ID = "characterId";
    public static final String ATTR_CHARACTER_NAME = "characterName";

    private static final Logger log = LoggerFactory.getLogger(JwtHandshakeInterceptor.class);

    private final JwtService jwtService;
    private final CharacterRepository characterRepository;

    public JwtHandshakeInterceptor(JwtService jwtService, CharacterRepository characterRepository) {
        this.jwtService = jwtService;
        this.characterRepository = characterRepository;
    }

    @Override
    public boolean beforeHandshake(
            ServerHttpRequest request,
            ServerHttpResponse response,
            WebSocketHandler wsHandler,
            Map<String, Object> attributes
    ) {
        var params = UriComponentsBuilder.fromUri(request.getURI()).build().getQueryParams();
        String token = params.getFirst("token");
        if (token == null || token.isBlank()) {
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            return false;
        }

        UUID accountId;
        try {
            accountId = jwtService.parseAccountId(token);
        } catch (Exception ex) {
            log.debug("WS handshake rejected: {}", ex.getMessage());
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            return false;
        }

        Optional<CharacterEntity> character = resolveCharacter(accountId, params.getFirst("characterId"));
        if (character.isEmpty()) {
            response.setStatusCode(HttpStatus.FORBIDDEN);
            return false;
        }

        attributes.put(ATTR_ACCOUNT_ID, accountId);
        attributes.put(ATTR_CHARACTER_ID, character.get().getId());
        attributes.put(ATTR_CHARACTER_NAME, character.get().getName());
        return true;
    }

    private Optional<CharacterEntity> resolveCharacter(UUID accountId, String characterIdParam) {
        if (characterIdParam == null || characterIdParam.isBlank()) {
            List<CharacterEntity> characters = characterRepository.findByAccountIdOrderByCreatedAtAsc(accountId);
            return characters.stream().findFirst();
        }
        try {
            return characterRepository.findByIdAndAccountId(UUID.fromString(characterIdParam), accountId);
        } catch (IllegalArgumentException ex) {
            return Optional.empty();
        }
    }

    @Override
    public void afterHandshake(
            ServerHttpRequest request,
            ServerHttpResponse response,
            WebSocketHandler wsHandler,
            Exception exception
    ) {
    }
}
